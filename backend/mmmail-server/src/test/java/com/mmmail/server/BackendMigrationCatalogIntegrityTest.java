package com.mmmail.server;

import com.mmmail.server.migration.MigrationDefaults;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class BackendMigrationCatalogIntegrityTest {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^V(\\d+)__.+\\.(?:java|sql)$");
    private static final String FLYWAY_CATALOG_JDBC_URL = "jdbc:h2:mem:flyway_catalog;MODE=MySQL;DB_CLOSE_DELAY=-1";
    private static final int MAX_MIGRATIONS_PER_VERSION = 1;
    private static final List<String> PRODUCTION_MIGRATION_DIRS = List.of(
            "src/main/java/db/migration",
            "src/main/resources/db/migration"
    );

    @Test
    void productionMigrationsShouldUseUniqueVersionsAcrossJavaAndSql() throws Exception {
        Map<Integer, List<Path>> migrationsByVersion = productionMigrationsByVersion();
        Map<Integer, List<Path>> duplicates = new TreeMap<>();

        migrationsByVersion.forEach((version, paths) -> {
            if (paths.size() > MAX_MIGRATIONS_PER_VERSION) {
                duplicates.put(version, paths);
            }
        });

        assertThat(duplicates).isEmpty();
    }

    @Test
    void flywayShouldResolveProductionCatalogWithoutDuplicateVersionFailure() {
        Flyway flyway = MigrationDefaults.apply(Flyway.configure()
                .dataSource(FLYWAY_CATALOG_JDBC_URL, "sa", ""))
                .load();

        assertThatCode(flyway::info).doesNotThrowAnyException();
    }

    private static Map<Integer, List<Path>> productionMigrationsByVersion() throws IOException {
        Path moduleRoot = resolveModuleRoot();
        Map<Integer, List<Path>> migrationsByVersion = new TreeMap<>();
        for (String directory : PRODUCTION_MIGRATION_DIRS) {
            collectMigrations(moduleRoot.resolve(directory), migrationsByVersion);
        }
        return migrationsByVersion;
    }

    private static void collectMigrations(Path directory, Map<Integer, List<Path>> migrationsByVersion) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path file : stream) {
                versionOf(file.getFileName().toString())
                        .ifPresent(version -> migrationsByVersion
                                .computeIfAbsent(version, ignored -> new ArrayList<>())
                                .add(file));
            }
        }
    }

    private static java.util.Optional<Integer> versionOf(String filename) {
        Matcher matcher = VERSION_PATTERN.matcher(filename);
        if (!matcher.matches()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Integer.parseInt(matcher.group(1)));
    }

    private static Path resolveModuleRoot() {
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
