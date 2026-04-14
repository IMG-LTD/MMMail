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
