package com.mmmail.server.migration;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SqlScriptMigrationSupport {

    private static final Pattern ADD_COLUMN = Pattern.compile(
            "(?is)^alter\\s+table\\s+`?([\\w]+)`?\\s+add\\s+column(?:\\s+if\\s+not\\s+exists)?\\s+`?([\\w]+)`?\\s+(.+)$"
    );

    private SqlScriptMigrationSupport() {
    }

    public static void execute(Connection connection, String resourcePath) {
        execute(connection, resourcePath, readScript(resourcePath));
    }

    public static void addColumnIfMissing(Connection connection, String tableName, String columnName, String definition) {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }
        executeRaw(connection, "alter table " + tableName + " add column " + columnName + " " + definition, tableName + "." + columnName);
    }

    static void execute(Connection connection, String sourceName, String scriptContent) {
        for (String statement : splitStatements(scriptContent)) {
            executeStatement(connection, sourceName, statement);
        }
    }

    private static void executeStatement(Connection connection, String sourceName, String statement) {
        String sql = statement.trim();
        if (sql.isEmpty()) {
            return;
        }
        AddColumnStatement addColumn = parseAddColumn(sql);
        if (addColumn != null && columnExists(connection, addColumn.table(), addColumn.column())) {
            return;
        }
        String executable = addColumn == null ? sql : addColumn.toSql();
        executeRaw(connection, executable, sourceName);
    }

    private static AddColumnStatement parseAddColumn(String sql) {
        Matcher matcher = ADD_COLUMN.matcher(sql);
        if (!matcher.matches()) {
            return null;
        }
        return new AddColumnStatement(matcher.group(1), matcher.group(2), matcher.group(3).trim());
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) {
        try {
            DatabaseMetaData metadata = connection.getMetaData();
            String catalog = connection.getCatalog();
            for (String tableVariant : variants(tableName)) {
                for (String columnVariant : variants(columnName)) {
                    if (hasColumn(metadata, catalog, tableVariant, columnVariant)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect schema metadata for " + tableName + "." + columnName, exception);
        }
    }

    private static boolean hasColumn(DatabaseMetaData metadata, String catalog, String tableName, String columnName)
            throws SQLException {
        try (ResultSet resultSet = metadata.getColumns(catalog, null, tableName, columnName)) {
            return resultSet.next();
        }
    }

    private static List<String> variants(String value) {
        List<String> values = new ArrayList<>();
        values.add(value);
        values.add(value.toLowerCase(Locale.ROOT));
        values.add(value.toUpperCase(Locale.ROOT));
        return values;
    }

    private static String readScript(String resourcePath) {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        try {
            byte[] content = resource.getInputStream().readAllBytes();
            return new String(content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read SQL script: " + resourcePath, exception);
        }
    }

    private static List<String> splitStatements(String scriptContent) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean singleQuote = false;
        boolean doubleQuote = false;
        for (int index = 0; index < scriptContent.length(); index++) {
            char currentChar = scriptContent.charAt(index);
            if (currentChar == '\'' && !doubleQuote) {
                singleQuote = !singleQuote;
            } else if (currentChar == '"' && !singleQuote) {
                doubleQuote = !doubleQuote;
            }
            if (currentChar == ';' && !singleQuote && !doubleQuote) {
                statements.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(currentChar);
        }
        if (!current.isEmpty()) {
            statements.add(current.toString());
        }
        return statements;
    }

    private static void executeRaw(Connection connection, String sql, String sourceName) {
        try (Statement jdbcStatement = connection.createStatement()) {
            jdbcStatement.execute(sql);
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to execute SQL from " + sourceName + ": " + sql, exception);
        }
    }

    private record AddColumnStatement(String table, String column, String definition) {
        private String toSql() {
            return "alter table " + table + " add column " + column + " " + definition;
        }
    }
}
