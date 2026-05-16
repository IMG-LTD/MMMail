package com.mmmail.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BackendV212CiGateContractTest {

    @Test
    void ciShouldRunBackendV212RegressionGate() throws Exception {
        String workflow = Files.readString(repoRoot().resolve(".github/workflows/ci.yml"));

        assertThat(workflow).contains(
                "Backend v2.1.2 runtime regression",
                "DevSeedV212StartupIntegrationTest",
                "WebSocketLoadTestScriptContractTest",
                "SuiteNotificationSyncWebSocketReplayTest",
                "RequestObservationServiceTest"
        );
    }

    private Path repoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.exists(current.resolve(".github/workflows/ci.yml"))) {
            current = current.getParent();
        }
        assertThat(current).isNotNull();
        return current;
    }
}
