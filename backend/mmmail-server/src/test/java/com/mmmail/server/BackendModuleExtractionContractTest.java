package com.mmmail.server;

import com.mmmail.billing.BillingReadinessCapabilities;
import com.mmmail.identity.session.RefreshTokenHasher;
import com.mmmail.orggovernance.scope.OrgScopeAccessDecision;
import com.mmmail.workspace.WorkspaceAggregationCapabilities;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BackendModuleExtractionContractTest {

    @Test
    void backendModulesShouldExposeRealReusableKernels() throws Exception {
        RefreshTokenHasher hasher = new RefreshTokenHasher();
        assertThat(hasher.hash("refresh-token")).hasSize(64);

        OrgScopeAccessDecision decision = new OrgScopeAccessDecision(42L, Set.of("MAIL", "DOCS"));
        assertThat(decision.active()).isTrue();
        assertThat(decision.allows("MAIL")).isTrue();
        assertThat(decision.allows("PASS")).isFalse();

        assertThat(WorkspaceAggregationCapabilities.defaultCapabilities().toPayload())
                .containsKey("workspaceModules")
                .containsKey("storyGroups");
        assertThat(BillingReadinessCapabilities.defaultCapabilities().toPayload())
                .containsEntry("legacyExitReady", true);

        String authService = Files.readString(Path.of("src", "main", "java", "com", "mmmail", "server", "service", "AuthService.java"));
        String suiteOrgScopeService = Files.readString(Path.of("src", "main", "java", "com", "mmmail", "server", "service", "SuiteOrgScopeService.java"));
        String workspaceAggregationController = Files.readString(Path.of("src", "main", "java", "com", "mmmail", "server", "controller", "WorkspaceAggregationController.java"));
        String billingReadinessController = Files.readString(Path.of("src", "main", "java", "com", "mmmail", "server", "controller", "BillingReadinessController.java"));

        assertThat(authService).contains("RefreshTokenHasher");
        assertThat(suiteOrgScopeService).contains("OrgScopeAccessDecision");
        assertThat(workspaceAggregationController).contains("WorkspaceAggregationCapabilities");
        assertThat(billingReadinessController).contains("BillingReadinessCapabilities");
    }
}
