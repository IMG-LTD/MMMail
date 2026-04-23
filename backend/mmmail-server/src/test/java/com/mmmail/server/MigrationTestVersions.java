package com.mmmail.server;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

final class MigrationTestVersions {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^V(\\d+)__.+\\.(?:java|sql)$");

    private MigrationTestVersions() {
    }

    static int latestProductionVersion() throws IOException {
        Path moduleRoot = resolveModuleRoot();
        return Math.max(
                latestVersion(moduleRoot.resolve("src/main/java/db/migration"), "*.java"),
                latestVersion(moduleRoot.resolve("src/main/resources/db/migration"), "*.sql")
        );
    }

    static int productionMigrationCount() throws IOException {
        Path moduleRoot = resolveModuleRoot();
        return countVersionedMigrations(moduleRoot.resolve("src/main/java/db/migration"), "*.java")
                + countVersionedMigrations(moduleRoot.resolve("src/main/resources/db/migration"), "*.sql");
    }

    static int productionAndTestMigrationCount() throws IOException {
        Path moduleRoot = resolveModuleRoot();
        return productionMigrationCount()
                + countVersionedMigrations(moduleRoot.resolve("src/test/java/db/migration"), "*.java")
                + countVersionedMigrations(moduleRoot.resolve("src/test/resources/db/testmigration"), "*.sql");
    }

    private static int latestVersion(Path directory, String glob) throws IOException {
        int latest = 0;
        if (!Files.isDirectory(directory)) {
            return latest;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, glob)) {
            for (Path file : stream) {
                latest = Math.max(latest, versionOf(file.getFileName().toString()));
            }
        }
        return latest;
    }

    private static int countVersionedMigrations(Path directory, String glob) throws IOException {
        int count = 0;
        if (!Files.isDirectory(directory)) {
            return count;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, glob)) {
            for (Path file : stream) {
                if (versionOf(file.getFileName().toString()) > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int versionOf(String filename) {
        Matcher matcher = VERSION_PATTERN.matcher(filename);
        if (!matcher.matches()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
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
