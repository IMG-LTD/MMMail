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
        Connection connection = fakeConnection(Set.of(), Set.of(), executedSql);

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
        Connection connection = fakeConnection(Set.of("mail_message.delivery_targets_json"), Set.of(), executedSql);

        SqlScriptMigrationSupport.execute(connection, "inline.sql", """
                alter table mail_message add column if not exists delivery_targets_json text;
                """);

        assertThat(executedSql).isEmpty();
    }

    @Test
    void shouldSkipPlainAddColumnWhenColumnAlreadyExists() {
        List<String> executedSql = new ArrayList<>();
        Connection connection = fakeConnection(Set.of("search_preset.is_pinned"), Set.of(), executedSql);

        SqlScriptMigrationSupport.execute(connection, "inline.sql", """
                alter table search_preset add column is_pinned tinyint not null default 0;
                """);

        assertThat(executedSql).isEmpty();
    }

    @Test
    void shouldSkipDropIndexWhenIndexIsMissing() {
        List<String> executedSql = new ArrayList<>();
        Connection connection = fakeConnection(Set.of(), Set.of(), executedSql);

        SqlScriptMigrationSupport.execute(connection, "inline.sql", """
                drop index uk_calendar_attendee_event_email on calendar_event_attendee;
                """);

        assertThat(executedSql).isEmpty();
    }

    @Test
    void shouldCreateIndexWhenIndexIsMissing() {
        List<String> executedSql = new ArrayList<>();
        Connection connection = fakeConnection(Set.of(), Set.of(), executedSql);

        SqlScriptMigrationSupport.createIndexIfMissing(
                connection,
                "mail_attachment",
                "idx_mail_attachment_owner_mail_created",
                "(owner_id, mail_id, created_at)"
        );

        assertThat(executedSql).containsExactly(
                "create index idx_mail_attachment_owner_mail_created on mail_attachment(owner_id, mail_id, created_at)"
        );
    }

    @Test
    void shouldSkipCreateIndexWhenIndexAlreadyExists() {
        List<String> executedSql = new ArrayList<>();
        Connection connection = fakeConnection(
                Set.of(),
                Set.of("mail_attachment.idx_mail_attachment_owner_mail_created"),
                executedSql
        );

        SqlScriptMigrationSupport.createIndexIfMissing(
                connection,
                "mail_attachment",
                "idx_mail_attachment_owner_mail_created",
                "(owner_id, mail_id, created_at)"
        );

        assertThat(executedSql).isEmpty();
    }

    private Connection fakeConnection(Set<String> existingColumns, Set<String> existingIndexes, List<String> executedSql) {
        DatabaseMetaData metadata = fakeMetadata(existingColumns, existingIndexes);
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

    private DatabaseMetaData fakeMetadata(Set<String> existingColumns, Set<String> existingIndexes) {
        return (DatabaseMetaData) Proxy.newProxyInstance(
                DatabaseMetaData.class.getClassLoader(),
                new Class[]{DatabaseMetaData.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "getColumns" -> {
                            String tableName = String.valueOf(args[2]);
                            String columnName = String.valueOf(args[3]);
                            boolean present = existingColumns.contains(tableName + "." + columnName);
                            yield fakeResultSet(present);
                        }
                        case "getIndexInfo" -> {
                            String tableName = String.valueOf(args[2]);
                            yield fakeIndexResultSet(tableName, existingIndexes);
                        }
                        default -> defaultValue(method.getReturnType());
                    };
                }
        );
    }

    private ResultSet fakeIndexResultSet(String tableName, Set<String> existingIndexes) {
        List<String> matches = existingIndexes.stream()
                .filter(index -> index.startsWith(tableName + "."))
                .map(index -> index.substring(tableName.length() + 1))
                .toList();
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class[]{ResultSet.class},
                new java.lang.reflect.InvocationHandler() {
                    private int position = -1;

                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        return switch (method.getName()) {
                            case "next" -> ++position < matches.size();
                            case "getString" -> "INDEX_NAME".equals(args[0]) ? matches.get(position) : null;
                            default -> defaultValue(method.getReturnType());
                        };
                    }
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
