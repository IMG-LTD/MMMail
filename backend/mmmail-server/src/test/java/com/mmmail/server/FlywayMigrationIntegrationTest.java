package com.mmmail.server;

import com.mmmail.server.migration.MigrationDefaults;
import com.mmmail.server.migration.SqlScriptMigrationSupport;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class FlywayMigrationIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("mmmail")
            .withUsername("mmmail_app")
            .withPassword("Password123!");

    @BeforeEach
    void resetDatabase() throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS=0");
            for (String tableName : listTables(connection)) {
                statement.execute("DROP TABLE IF EXISTS `" + tableName + "`");
            }
            statement.execute("SET FOREIGN_KEY_CHECKS=1");
        }
    }

    @Test
    void freshInstallShouldApplyBaselineSchemaAndSeedData() throws Exception {
        int productionMigrationCount = MigrationTestVersions.productionMigrationCount();
        int latestProductionVersion = MigrationTestVersions.latestProductionVersion();
        Flyway flyway = defaultFlyway();

        MigrateResult result = flyway.migrate();

        assertThat(result.migrationsExecuted).isEqualTo(productionMigrationCount);
        assertThat(queryForLong("select count(*) from user_account")).isEqualTo(2);
        assertThat(queryForLong("select count(*) from user_preference")).isEqualTo(2);
        assertThat(queryForLong("select count(*) from mail_attachment")).isEqualTo(0);
        assertThat(queryForLong("select count(*) from information_schema.columns where table_schema = 'mmmail' and table_name = 'user_preference' and column_name = 'mail_e2ee_recovery_private_key_encrypted'")).isEqualTo(1);
        assertThat(queryForLong("select count(*) from information_schema.columns where table_schema = 'mmmail' and table_name = 'user_preference' and column_name = 'mail_e2ee_recovery_updated_at'")).isEqualTo(1);
        assertThat(queryForLong("select count(*) from system_release_metadata where schema_version = '" + latestProductionVersion + "'")).isEqualTo(1);
        assertThat(queryForLong("select count(*) from flyway_schema_history where version is not null and success = 1")).isEqualTo(productionMigrationCount);
    }

    @Test
    void existingSchemaShouldBaselineCurrentStateAndApplyUpgradeMigration() throws Exception {
        int latestProductionVersion = MigrationTestVersions.latestProductionVersion();
        int productionAndTestMigrationCount = MigrationTestVersions.productionAndTestMigrationCount();
        executeLegacyScript("db/baseline/community-v1-schema.sql");
        executeLegacyScript("data.sql");

        Flyway flyway = MigrationDefaults.apply(
                Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()),
                "classpath:db/migration",
                "classpath:db/testmigration"
        ).load();

        MigrateResult result = flyway.migrate();

        assertThat(result.migrationsExecuted).isEqualTo(productionAndTestMigrationCount - 1);
        assertThat(queryForLong("select count(*) from migration_upgrade_probe")).isEqualTo(1);
        assertThat(queryForLong("select count(*) from mail_attachment")).isEqualTo(0);
        assertThat(queryForLong("select count(*) from information_schema.columns where table_schema = 'mmmail' and table_name = 'user_preference' and column_name = 'mail_e2ee_recovery_private_key_encrypted'")).isEqualTo(1);
        assertThat(queryForLong("select count(*) from information_schema.columns where table_schema = 'mmmail' and table_name = 'user_preference' and column_name = 'mail_e2ee_recovery_updated_at'")).isEqualTo(1);
        assertThat(queryForLong("select count(*) from system_release_metadata where schema_version = '" + latestProductionVersion + "'")).isEqualTo(1);
        assertThat(queryForLong("select count(*) from flyway_schema_history where version = '1' and type = 'BASELINE'")).isEqualTo(1);
        assertThat(queryForLong("select count(*) from flyway_schema_history where version is not null and success = 1")).isEqualTo(productionAndTestMigrationCount);
        assertThat(queryForLong("select count(*) from flyway_schema_history where version = '14' and success = 1")).isEqualTo(1);
    }

    private Flyway defaultFlyway() {
        return MigrationDefaults.apply(Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())).load();
    }

    private void executeLegacyScript(String resourcePath) throws Exception {
        try (Connection connection = openConnection()) {
            SqlScriptMigrationSupport.execute(connection, resourcePath);
        }
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
}
