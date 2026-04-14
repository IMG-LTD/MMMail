package com.mmmail.server;

import jakarta.mail.Session;
import org.apache.catalina.util.ServerInfo;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jose4j.jwt.JwtClaims;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyVersionGuardTest {

    @Test
    void jose4jRuntimeShouldStayOnPatchedVersionForSecurityGate() throws Exception {
        String version = readRuntimeVersion(JwtClaims.class, "META-INF/maven/org.bitbucket.b_c/jose4j/pom.properties");

        assertThat(versionAtLeast(version, "0.9.6"))
                .as("resolved jose4j version %s should be at least 0.9.6 to clear the CI security gate", version)
                .isTrue();
    }

    @Test
    void jakartaMailRuntimeShouldStayOnPatchedVersionForSecurityGate() throws Exception {
        String version = readRuntimeVersion(Session.class, "META-INF/maven/org.eclipse.angus/jakarta.mail/pom.properties");

        assertThat(versionAtLeast(version, "2.0.4"))
                .as("resolved jakarta.mail version %s should be at least 2.0.4 to clear the CI security gate", version)
                .isTrue();
    }

    @Test
    void angusMailRuntimeShouldStayOnPatchedVersionForSecurityGate() throws Exception {
        String version = readRuntimeVersion(Session.class, "META-INF/maven/org.eclipse.angus/angus-mail/pom.properties");

        assertThat(versionAtLeast(version, "2.0.4"))
                .as("resolved angus-mail version %s should be at least 2.0.4 to clear the CI security gate", version)
                .isTrue();
    }

    @Test
    void angusCoreRuntimeShouldStayOnPatchedVersionForSecurityGate() throws Exception {
        String version = readRuntimeVersion(Session.class, "META-INF/maven/org.eclipse.angus/angus-core/pom.properties");

        assertThat(versionAtLeast(version, "2.0.4"))
                .as("resolved angus-core version %s should be at least 2.0.4 to clear the CI security gate", version)
                .isTrue();
    }

    @Test
    void tomcatRuntimeShouldStayOnPatchedVersionForSecurityGate() {
        String version = ServerInfo.getServerNumber().trim();

        assertThat(versionAtLeast(version, "10.1.53.0"))
                .as("resolved tomcat-embed-core version %s should be at least 10.1.53.0 to clear the CI security gate", version)
                .isTrue();
    }

    @Test
    void swaggerUiRuntimeShouldStayOnPatchedVersionForSecurityGate() throws Exception {
        String version = readRuntimeVersion(DependencyVersionGuardTest.class, "META-INF/maven/org.webjars/swagger-ui/pom.properties");

        assertThat(versionAtLeast(version, "5.32.1"))
                .as("resolved swagger-ui version %s should be at least 5.32.1 to clear the CI security gate", version)
                .isTrue();
    }

    @Test
    void validateSecurityScriptShouldKeepWebPushCompatibilityGuard() throws IOException {
        String validateSecurityScript = Files.readString(Path.of("..", "..", "scripts", "validate-security.sh"));

        assertThat(validateSecurityScript)
                .as("validate-security.sh backend regression list")
                .contains("DependencyVersionGuardTest")
                .contains("VapidWebPushDeliveryGatewayConfigurationTest");
    }

    @Test
    void validateRc1ContainerScriptShouldCheckLatestAppliedFlywayVersion() throws IOException {
        String validateRc1ContainerScript = Files.readString(Path.of("..", "..", "scripts", "validate-rc1-container.sh"));

        assertThat(validateRc1ContainerScript)
                .as("validate-rc1-container.sh should compare release metadata to the latest applied Flyway version")
                .contains("select coalesce((select version from flyway_schema_history where success = 1 and version is not null order by installed_rank desc limit 1), 'none')")
                .doesNotContain("[[ \"$schema_version\" != \"5\" ]]");
    }

    @Test
    void validateBatch3ScriptShouldRunFlywayUniquenessGuard() throws IOException {
        String validateBatch3Script = Files.readString(Path.of("..", "..", "scripts", "validate-batch3.sh"));

        assertThat(validateBatch3Script)
                .as("validate-batch3.sh should keep the Flyway uniqueness regression test")
                .contains("FlywayMigrationVersionUniquenessTest");
    }

    @Test
    void baselineMigrationShouldUseFrozenCommunityV1SchemaSnapshot() throws IOException {
        String baselineMigration = Files.readString(Path.of("src", "main", "java", "db", "migration", "V1__baseline_schema.java"));

        assertThat(baselineMigration)
                .as("V1 baseline migration should use a frozen community v1 snapshot instead of the mutable schema.sql")
                .contains("db/baseline/community-v1-schema.sql")
                .doesNotContain("execute(context.getConnection(), \"schema.sql\")");
    }

    @Test
    void frozenCommunityV1SchemaSnapshotShouldExcludePostBaselineObjects() throws IOException {
        String currentSchema = Files.readString(Path.of("src", "main", "resources", "schema.sql"));
        Path baselineSnapshotPath = Path.of("src", "main", "resources", "db", "baseline", "community-v1-schema.sql");

        assertThat(currentSchema)
                .as("current schema.sql should still reflect later product additions")
                .contains("web_push_subscription")
                .contains("source varchar(24) not null default 'MANUAL'")
                .contains("readable_e2ee_enabled tinyint not null default 0")
                .contains("body_e2ee_external_access_json")
                .contains("mail_external_secure_link");
        assertThat(Files.exists(baselineSnapshotPath))
                .as("frozen community v1 baseline snapshot should exist")
                .isTrue();
        if (!Files.exists(baselineSnapshotPath)) {
            return;
        }

        String baselineSnapshot = Files.readString(baselineSnapshotPath);
        assertThat(baselineSnapshot)
                .as("community v1 baseline snapshot should stop before later V8+ additions")
                .doesNotContain("web_push_subscription")
                .doesNotContain("source varchar(24) not null default 'MANUAL'")
                .doesNotContain("readable_e2ee_enabled tinyint not null default 0")
                .doesNotContain("body_e2ee_external_access_json")
                .doesNotContain("mail_external_secure_link");
    }

    @Test
    void flywayMigrationIntegrationTestShouldSeedLegacySchemaFromFrozenSnapshot() throws IOException {
        String flywayMigrationIntegrationTest = Files.readString(Path.of("src", "test", "java", "com", "mmmail", "server", "FlywayMigrationIntegrationTest.java"));

        assertThat(flywayMigrationIntegrationTest)
                .as("legacy schema upgrade test should use the frozen community v1 snapshot")
                .contains("db/baseline/community-v1-schema.sql")
                .doesNotContain("executeLegacyScript(\"schema.sql\")");
    }

    @Test
    void mailE2eeRecoveryColumnsShouldHaveDedicatedPostBaselineMigration() throws IOException {
        String currentSchema = Files.readString(Path.of("src", "main", "resources", "schema.sql"));
        String baselineSnapshot = Files.readString(Path.of("src", "main", "resources", "db", "baseline", "community-v1-schema.sql"));
        Path recoveryMigrationPath = Path.of("src", "main", "java", "db", "migration", "V13__user_preference_mail_e2ee_recovery_columns.java");

        assertThat(currentSchema)
                .as("current schema should still expose Mail E2EE recovery columns")
                .contains("mail_e2ee_recovery_private_key_encrypted")
                .contains("mail_e2ee_recovery_updated_at");
        assertThat(baselineSnapshot)
                .as("frozen v1 baseline should stop before Mail E2EE recovery columns")
                .doesNotContain("mail_e2ee_recovery_private_key_encrypted")
                .doesNotContain("mail_e2ee_recovery_updated_at");
        assertThat(Files.exists(recoveryMigrationPath))
                .as("a dedicated V13 migration should restore Mail E2EE recovery columns after the frozen baseline")
                .isTrue();
        if (!Files.exists(recoveryMigrationPath)) {
            return;
        }

        String recoveryMigration = Files.readString(recoveryMigrationPath);
        assertThat(recoveryMigration)
                .as("Mail E2EE recovery migration should add the missing columns idempotently")
                .contains("\"mail_e2ee_recovery_private_key_encrypted\"")
                .contains("\"longtext null\"")
                .contains("\"mail_e2ee_recovery_updated_at\"")
                .contains("\"timestamp null\"")
                .contains("set schema_version = '13'");
    }

    @Test
    void flywayUpgradeProbeShouldStayAfterLatestProductionMigration() {
        Path productionMigration = Path.of("src", "main", "java", "db", "migration", "V13__user_preference_mail_e2ee_recovery_columns.java");
        Path testProbeMigration = Path.of("src", "test", "resources", "db", "testmigration", "V14__upgrade_probe.sql");
        Path staleProbeMigration = Path.of("src", "test", "resources", "db", "testmigration", "V13__upgrade_probe.sql");

        assertThat(Files.exists(productionMigration))
                .as("V13 should be reserved for the production recovery migration")
                .isTrue();
        assertThat(Files.exists(testProbeMigration))
                .as("upgrade probe test migration should move after the latest production migration")
                .isTrue();
        assertThat(Files.exists(staleProbeMigration))
                .as("test migration should not keep the old conflicting V13 slot")
                .isFalse();
    }

    @Test
    void prereleaseVersionShouldNotCountAsStableBaseline() {
        assertThat(versionAtLeast("0.9.6-rc1", "0.9.6"))
                .as("release candidates must not satisfy the stable jose4j baseline")
                .isFalse();
    }

    private String readRuntimeVersion(Class<?> anchor, String resourcePath) throws Exception {
        try (InputStream inputStream = anchor.getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(inputStream)
                    .as("runtime metadata %s should be present on the classpath", resourcePath)
                    .isNotNull();
            Properties properties = new Properties();
            properties.load(inputStream);
            String version = properties.getProperty("version");
            assertThat(version)
                    .as("runtime metadata %s should contain a version", resourcePath)
                    .isNotBlank();
            return version.trim();
        }
    }

    private boolean versionAtLeast(String actual, String minimum) {
        return new ComparableVersion(actual).compareTo(new ComparableVersion(minimum)) >= 0;
    }
}
