package com.mmmail.server;

import com.mmmail.server.migration.MigrationCli;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class MigrationCliWorkflowIntegrationTest {

    private static final String URL_PROPERTY = "mmmail.migration.datasource.url";
    private static final String USERNAME_PROPERTY = "mmmail.migration.datasource.username";
    private static final String PASSWORD_PROPERTY = "mmmail.migration.datasource.password";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("mmmail")
            .withUsername("mmmail_app")
            .withPassword("Password123!");

    @BeforeEach
    void resetDatabase() throws Exception {
        System.setProperty(URL_PROPERTY, MYSQL.getJdbcUrl());
        System.setProperty(USERNAME_PROPERTY, MYSQL.getUsername());
        System.setProperty(PASSWORD_PROPERTY, MYSQL.getPassword());
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS=0");
            for (String tableName : listTables(connection)) {
                statement.execute("DROP TABLE IF EXISTS `" + tableName + "`");
            }
            statement.execute("SET FOREIGN_KEY_CHECKS=1");
        }
    }

    @Test
    void cliShouldRunInfoValidateAndMigrate() throws Exception {
        String initialInfo = captureOutput(() -> MigrationCli.main(new String[]{"info"}));
        assertThat(initialInfo).contains("current=none");

        String validation = captureOutput(() -> MigrationCli.main(new String[]{"validate"}));
        assertThat(validation).contains("validation=ok");

        String migrate = captureOutput(() -> MigrationCli.main(new String[]{"migrate"}));
        assertThat(migrate).contains("migrations=3");

        String finalInfo = captureOutput(() -> MigrationCli.main(new String[]{"info"}));
        assertThat(finalInfo).contains("current=3");
        assertThat(queryForLong("select count(*) from system_release_metadata where schema_version = '3'")).isEqualTo(1);
    }

    private String captureOutput(ThrowingRunnable runnable) throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintStream stream = new PrintStream(output)) {
            System.setOut(stream);
            runnable.run();
        } finally {
            System.setOut(originalOut);
        }
        return output.toString();
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    private long queryForLong(String sql) throws Exception {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private List<String> listTables(Connection connection) throws Exception {
        List<String> tableNames = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     select table_name
                     from information_schema.tables
                     where table_schema = 'mmmail'
                     """)) {
            while (resultSet.next()) {
                tableNames.add(resultSet.getString(1));
            }
        }
        return tableNames;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
