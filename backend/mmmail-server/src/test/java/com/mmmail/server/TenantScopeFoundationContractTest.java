package com.mmmail.server;

import com.mmmail.foundation.tenant.TenantScopeContext;
import com.mmmail.foundation.tenant.TenantScopeContextHolder;
import com.mmmail.foundation.tenant.TenantScopeHeaders;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TenantScopeFoundationContractTest {

    @Test
    void foundationScopeKernelShouldBackTheServerBoundary() throws Exception {
        assertThat(TenantScopeHeaders.ORG_ID).isEqualTo("X-MMMAIL-ORG-ID");
        assertThat(TenantScopeHeaders.SCOPE_ID).isEqualTo("X-MMMAIL-SCOPE-ID");

        TenantScopeContextHolder.set(new TenantScopeContext("42", "mail"));
        assertThat(TenantScopeContextHolder.get()).isEqualTo(new TenantScopeContext("42", "mail"));
        TenantScopeContextHolder.clear();
        assertThat(TenantScopeContextHolder.get()).isNull();

        String tracing = Files.readString(Path.of("src", "main", "java", "com", "mmmail", "server", "observability", "RequestTracingFilter.java"));
        String security = Files.readString(Path.of("src", "main", "java", "com", "mmmail", "server", "config", "SecurityConfig.java"));
        String platform = Files.readString(Path.of("src", "main", "java", "com", "mmmail", "server", "controller", "PlatformCapabilityController.java"));

        assertThat(tracing)
                .contains("TenantScopeContextHolder.set")
                .contains("TenantScopeContextHolder.clear()")
                .contains("TenantScopeHeaders.ORG_ID")
                .contains("TenantScopeHeaders.SCOPE_ID");
        assertThat(security)
                .contains("TenantScopeHeaders.ORG_ID")
                .contains("TenantScopeHeaders.SCOPE_ID");
        assertThat(platform)
                .contains("TenantScopeHeaders.ORG_ID")
                .contains("TenantScopeHeaders.SCOPE_ID");
    }
}
