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
