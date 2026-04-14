package com.mmmail.server;

import com.mmmail.server.migration.MigrationDefaults;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class BackupRestoreWorkflowIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("mmmail")
            .withUsername("mmmail_app")
            .withPassword("Password123!");

    @TempDir
    Path tempDir;

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
    void backupAndRestoreShouldRecoverDatabaseAndDrivePayload() throws Exception {
        MigrationDefaults.apply(Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())).load().migrate();

        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    insert into user_account (id, email, password_hash, display_name, role, status, token_version, created_at, updated_at, deleted)
                    values (99, 'backup-user@mmmail.local', 'hash', 'Backup User', 'USER', 1, 1, current_timestamp, current_timestamp, 0)
                    """);
        }

        Path driveRoot = tempDir.resolve("drive");
        Files.createDirectories(driveRoot);
        Files.writeString(driveRoot.resolve("note.txt"), "backup-original");

        Path backupDir = tempDir.resolve("backup");
        Path envFile = writeEnvFile(driveRoot);

        runScript("scripts/db-backup.sh", envFile.toString(), backupDir.toString());

        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("delete from user_account where id = 99");
        }
        Files.writeString(driveRoot.resolve("note.txt"), "mutated");

        runScript("scripts/db-restore.sh", envFile.toString(), backupDir.toString());

        assertThat(queryForLong("select count(*) from user_account where id = 99")).isEqualTo(1);
        assertThat(Files.readString(driveRoot.resolve("note.txt"))).isEqualTo("backup-original");
        assertThat(Files.exists(backupDir.resolve("manifest.txt"))).isTrue();
        assertThat(Files.readString(backupDir.resolve("manifest.txt"))).contains("schema_version=13");
    }

    @Test
    void rollbackShouldReuseBackupRestoreStrategy() throws Exception {
        MigrationDefaults.apply(Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())).load().migrate();

        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    insert into user_account (id, email, password_hash, display_name, role, status, token_version, created_at, updated_at, deleted)
                    values (101, 'rollback-user@mmmail.local', 'hash', 'Rollback User', 'USER', 1, 1, current_timestamp, current_timestamp, 0)
                    """);
        }

        Path driveRoot = tempDir.resolve("rollback-drive");
        Files.createDirectories(driveRoot);
        Files.writeString(driveRoot.resolve("rollback.txt"), "before-upgrade");

        Path backupDir = tempDir.resolve("rollback-backup");
        Path envFile = writeEnvFile(driveRoot);

        runScript("scripts/db-backup.sh", envFile.toString(), backupDir.toString());

        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("delete from user_account where id = 101");
        }
        Files.writeString(driveRoot.resolve("rollback.txt"), "after-upgrade");

        runScript("scripts/db-rollback.sh", envFile.toString(), backupDir.toString());

        assertThat(queryForLong("select count(*) from user_account where id = 101")).isEqualTo(1);
        assertThat(Files.readString(driveRoot.resolve("rollback.txt"))).isEqualTo("before-upgrade");
    }

    private Path writeEnvFile(Path driveRoot) throws Exception {
        Path envFile = tempDir.resolve("backup.env");
        Files.writeString(envFile, """
                SPRING_DATASOURCE_URL=%s
                SPRING_DATASOURCE_USERNAME=%s
                SPRING_DATASOURCE_PASSWORD=%s
                MMMAIL_DRIVE_STORAGE_ROOT=%s
                """.formatted(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword(), driveRoot));
        return envFile;
    }

    private void runScript(String scriptPath, String envFile, String backupDir) throws Exception {
        Path repoRoot = resolveRepoRoot();
        ProcessBuilder builder = new ProcessBuilder("bash", repoRoot.resolve(scriptPath).toString(), envFile, backupDir);
        builder.directory(repoRoot.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        assertThat(exitCode).withFailMessage(output).isZero();
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

    private Path resolveRepoRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("scripts/db-backup.sh"))) {
            current = current.getParent();
        }
        assertThat(current).as("repo root containing scripts/db-backup.sh").isNotNull();
        return current;
    }
}
