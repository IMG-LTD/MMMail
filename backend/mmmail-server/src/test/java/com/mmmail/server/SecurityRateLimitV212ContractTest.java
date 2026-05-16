package com.mmmail.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityRateLimitV212ContractTest {

    private static final List<String> REQUIRED_SCOPES = List.of(
            "mail-send",
            "web-push-test",
            "command-run"
    );

    @Test
    void sensitiveWriteActionsShouldUseCentralRateLimitService() throws Exception {
        String service = readJava("security/SecurityRateLimitService.java");
        String filter = readJava("security/RequestActionRateLimitFilter.java");
        String securityConfig = readJava("config/SecurityConfig.java");

        assertThat(service).contains(
                "recordMailSendAttempt",
                "recordWebPushTestAttempt",
                "recordCommandRunAttempt"
        );
        REQUIRED_SCOPES.forEach(scope -> assertThat(service).contains("\"" + scope + "\""));
        assertThat(filter).contains(
                "POST",
                "/api/v1/mails/send",
                "/api/v2/mail/send",
                "/api/v1/web-push/test",
                "/api/v2/command-center/runs",
                "recordMailSendAttempt",
                "recordWebPushTestAttempt",
                "recordCommandRunAttempt"
        );
        assertThat(securityConfig).contains(
                "RequestActionRateLimitFilter",
                "addFilterAfter(requestActionRateLimitFilter, JwtAuthFilter.class)",
                "requestActionRateLimitFilterRegistration"
        );
    }

    @Test
    void actionRateLimitPoliciesShouldBeConfigurable() throws Exception {
        String runbook = readDoc("deployment-runbook.md");
        for (String configFile : List.of("application.yml", "application-local.yml")) {
            String yaml = readResource(configFile);
            assertThat(yaml).contains(
                    "mail-send:",
                    "web-push-test:",
                    "command-run:"
            );
        }

        String testYaml = readTestResource("application-test.yml");
        assertThat(testYaml).contains(
                "mail-send:",
                "web-push-test:",
                "command-run:"
        );
        assertThat(runbook).contains(
                "MMMAIL_SECURITY_MAIL_SEND_RATE_LIMIT_WINDOW_SECONDS",
                "MMMAIL_SECURITY_WEB_PUSH_TEST_RATE_LIMIT_MAX_EVENTS",
                "MMMAIL_SECURITY_COMMAND_RUN_RATE_LIMIT_MAX_EVENTS"
        );
    }

    private String readJava(String path) throws Exception {
        return Files.readString(moduleRoot().resolve("src/main/java/com/mmmail/server").resolve(path));
    }

    private String readResource(String path) throws Exception {
        return Files.readString(moduleRoot().resolve("src/main/resources").resolve(path));
    }

    private String readTestResource(String path) throws Exception {
        return Files.readString(moduleRoot().resolve("src/test/resources").resolve(path));
    }

    private String readDoc(String path) throws Exception {
        return Files.readString(repoRoot().resolve("docs").resolve(path));
    }

    private Path moduleRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.exists(current.resolve("pom.xml"))) {
            current = current.getParent();
        }
        assertThat(current).isNotNull();
        return current;
    }

    private Path repoRoot() {
        Path current = moduleRoot();
        while (current != null && !Files.exists(current.resolve(".github/workflows/ci.yml"))) {
            current = current.getParent();
        }
        assertThat(current).isNotNull();
        return current;
    }
}
