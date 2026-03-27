package com.mmmail.server.migration;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SqlScriptMigrationSupportTest {

    @Test
    void shouldNormalizeConditionalAddColumnWhenColumnIsMissing() {
        List<String> executedSql = new ArrayList<>();
        Connection connection = fakeConnection(Set.of(), executedSql);

        SqlScriptMigrationSupport.execute(connection, "inline.sql", """
                alter table mail_message add column if not exists delivery_targets_json text;
                update system_release_metadata set schema_version = '4';
                """);

        assertThat(executedSql).containsExactly(
                "alter table mail_message add column delivery_targets_json text",
                "update system_release_metadata set schema_version = '4'"
        );
    }

    @Test
    void shouldSkipConditionalAddColumnWhenColumnAlreadyExists() {
        List<String> executedSql = new ArrayList<>();
        Connection connection = fakeConnection(Set.of("mail_message.delivery_targets_json"), executedSql);

        SqlScriptMigrationSupport.execute(connection, "inline.sql", """
                alter table mail_message add column if not exists delivery_targets_json text;
                """);

        assertThat(executedSql).isEmpty();
    }

    private Connection fakeConnection(Set<String> existingColumns, List<String> executedSql) {
        DatabaseMetaData metadata = fakeMetadata(existingColumns);
        Statement statement = fakeStatement(executedSql);
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getMetaData" -> metadata;
                    case "createStatement" -> statement;
                    case "getCatalog" -> "mmmail";
                    case "close" -> null;
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private DatabaseMetaData fakeMetadata(Set<String> existingColumns) {
        return (DatabaseMetaData) Proxy.newProxyInstance(
                DatabaseMetaData.class.getClassLoader(),
                new Class[]{DatabaseMetaData.class},
                (proxy, method, args) -> {
                    if (!"getColumns".equals(method.getName())) {
                        return defaultValue(method.getReturnType());
                    }
                    String tableName = String.valueOf(args[2]);
                    String columnName = String.valueOf(args[3]);
                    boolean present = existingColumns.contains(tableName + "." + columnName);
                    return fakeResultSet(present);
                }
        );
    }

    private Statement fakeStatement(List<String> executedSql) {
        return (Statement) Proxy.newProxyInstance(
                Statement.class.getClassLoader(),
                new Class[]{Statement.class},
                (proxy, method, args) -> {
                    if ("execute".equals(method.getName())) {
                        executedSql.add(String.valueOf(args[0]).trim());
                        return true;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private ResultSet fakeResultSet(boolean present) {
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class[]{ResultSet.class},
                new java.lang.reflect.InvocationHandler() {
                    private boolean unread = present;

                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        if ("next".equals(method.getName())) {
                            if (!unread) {
                                return false;
                            }
                            unread = false;
                            return true;
                        }
                        return defaultValue(method.getReturnType());
                    }
                }
        );
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}
