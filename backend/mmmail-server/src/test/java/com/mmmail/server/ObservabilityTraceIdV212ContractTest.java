package com.mmmail.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityTraceIdV212ContractTest {

    @Test
    void structuredLogsShouldExposeTraceIdAliasForRequestId() throws Exception {
        String traceContext = readCommonJava("observability/TraceContext.java");
        String requestTracingFilter = readServerJava("observability/RequestTracingFilter.java");

        assertThat(traceContext).contains(
                "TRACE_ID_MDC",
                "\"traceId\""
        );
        assertThat(requestTracingFilter).contains("MDC.put(TraceContext.TRACE_ID_MDC, requestId)");
    }

    private String readCommonJava(String path) throws Exception {
        return Files.readString(repoRoot().resolve("backend/mmmail-common/src/main/java/com/mmmail/common").resolve(path));
    }

    private String readServerJava(String path) throws Exception {
        return Files.readString(repoRoot().resolve("backend/mmmail-server/src/main/java/com/mmmail/server").resolve(path));
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
