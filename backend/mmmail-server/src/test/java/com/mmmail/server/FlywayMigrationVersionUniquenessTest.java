package com.mmmail.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationVersionUniquenessTest {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^V(\\d+)__.+\\.(?:class|sql)$");
    private static final Pattern V212_MIGRATION_PATTERN = Pattern.compile("^V(2[1-9]|3[0-9])__.+\\.sql$");

    @Test
    void sourceMigrationsShouldHaveUniqueVersionsAcrossJavaAndSql() throws Exception {
        Map<String, List<String>> migrationsByVersion = new TreeMap<>();
        Path repoRoot = resolveRepoRoot();

        collectVersions(migrationsByVersion, repoRoot.resolve("src/main/java/db/migration"), "db/migration", "*.java");
        collectVersions(migrationsByVersion, repoRoot.resolve("src/main/resources/db/migration"), "db/migration", "*.sql");
        collectVersions(migrationsByVersion, repoRoot.resolve("src/test/java/db/migration"), "db/testmigration", "*.java");
        collectVersions(migrationsByVersion, repoRoot.resolve("src/test/resources/db/testmigration"), "db/testmigration", "*.sql");

        Map<String, List<String>> duplicates = migrationsByVersion.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        assertThat(duplicates)
                .as("duplicate Flyway migration versions in source migrations")
                .isEmpty();
    }

    @Test
    void v212SqlMigrationsShouldDeclareRollbackNotes() throws Exception {
        Path migrationDirectory = resolveRepoRoot().resolve("src/main/resources/db/migration");
        List<String> missingRollbackNotes = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(migrationDirectory, "V*.sql")) {
            for (Path file : stream) {
                if (V212_MIGRATION_PATTERN.matcher(file.getFileName().toString()).matches()
                        && !Files.readString(file).contains("-- ROLLBACK:")) {
                    missingRollbackNotes.add(file.getFileName().toString());
                }
            }
        }

        assertThat(missingRollbackNotes)
                .as("v2.1.2 Flyway migrations must document rollback steps")
                .isEmpty();
    }

    @Test
    void boardPositionMigrationShouldBackfillLexorankFromCreatedAt() throws Exception {
        Path migration = resolveRepoRoot()
                .resolve("src/main/resources/db/migration/V22__v21_collaboration_board_positions.sql");
        String sql = Files.readString(migration).toLowerCase();

        assertThat(sql)
                .contains("row_number() over")
                .contains("partition by project_id, board_column")
                .contains("order by created_at, id")
                .contains("concat('0|i'")
                .contains("lpad")
                .contains("1000");
        assertThat(sql)
                .as("existing tasks should not all keep one static board position")
                .doesNotContain("position varchar(64) not null default '00000000000000001000'");
    }

    @Test
    void webPushSubscriptionMigrationShouldPreserveDeliveryStateColumns() throws Exception {
        Path migration = resolveRepoRoot()
                .resolve("src/main/resources/db/migration/V8__web_push_subscriptions.sql");
        String sql = Files.readString(migration).toLowerCase();

        assertThat(sql)
                .contains("endpoint_hash varchar(64) not null")
                .contains("p256dh_key varchar(255) not null")
                .contains("auth_key varchar(255) not null")
                .contains("last_success_at timestamp")
                .contains("last_failure_at timestamp")
                .contains("last_error_message varchar(255)");
    }

    private void collectVersions(Map<String, List<String>> migrationsByVersion, Path directory, String location, String glob) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, glob)) {
            for (Path file : stream) {
                String filename = file.getFileName().toString();
                Matcher matcher = VERSION_PATTERN.matcher(filename);
                if (!matcher.matches()) {
                    continue;
                }
                migrationsByVersion.computeIfAbsent(matcher.group(1), ignored -> new ArrayList<>()).add(location + "/" + filename);
            }
        }
    }

    private Path resolveRepoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.isDirectory(current.resolve("src/main/java/db/migration"))) {
            current = current.getParent();
        }
        assertThat(current)
                .as("module root containing src/main/java/db/migration should exist")
                .isNotNull();
        return current;
    }
}
