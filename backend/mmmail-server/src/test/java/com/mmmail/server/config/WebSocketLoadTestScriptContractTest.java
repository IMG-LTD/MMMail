package com.mmmail.server.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketLoadTestScriptContractTest {

    @Test
    void shouldProvideSpecLevelWsGatewayLoadTestScriptAndRunbook() throws Exception {
        String script = Files.readString(Path.of("..", "..", "ops", "ws-gateway-load-test.mjs"));
        String runbook = Files.readString(Path.of("..", "..", "docs", "deployment-runbook.md"));

        assertThat(script)
                .contains("DEFAULT_CONNECTIONS = 1000")
                .contains("DEFAULT_DURATION_MS = 30 * 60 * 1000")
                .contains("DEFAULT_CPU_MAX_PERCENT = 30")
                .contains("DEFAULT_MEMORY_MAX_BYTES = 1024 * 1024 * 1024")
                .contains("WS_TOKEN")
                .contains("WS_PROMETHEUS_URL")
                .contains("throttle")
                .contains("pong");
        assertThat(runbook)
                .contains("ops/ws-gateway-load-test.mjs")
                .contains("WS_CONNECTIONS=1000")
                .contains("WS_DURATION_MS=1800000");
    }
}
