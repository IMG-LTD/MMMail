package com.mmmail.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BackendV2GateContractTest {

    @Test
    void localAndCiGatesShouldCoverTheRemainingBackendV2Slices() throws Exception {
        Path repoRoot = resolveRepoRoot();
        String validateLocal = Files.readString(repoRoot.resolve("scripts/validate-local.sh"));
        String ci = Files.readString(repoRoot.resolve(".github/workflows/ci.yml"));
        String runbook = Files.readString(repoRoot.resolve("docs/deployment-runbook.md"));

        assertThat(validateLocal)
                .contains("ContractCatalogRegressionTest")
                .contains("TenantScopeFoundationContractTest")
                .contains("BackendModuleExtractionContractTest")
                .contains("PublicShareTokenHashMigrationIntegrationTest")
                .contains("MailPublicShareTokenHashContractTest")
                .contains("PassPublicShareTokenHashContractTest")
                .contains("DrivePublicShareTokenHashContractTest");

        assertThat(ci)
                .contains("ContractCatalogRegressionTest")
                .contains("TenantScopeFoundationContractTest")
                .contains("BackendModuleExtractionContractTest")
                .contains("PublicShareTokenHashMigrationIntegrationTest")
                .contains("MailPublicShareTokenHashContractTest")
                .contains("PassPublicShareTokenHashContractTest")
                .contains("DrivePublicShareTokenHashContractTest");

        assertThat(runbook)
                .contains("token-hash public-share contract")
                .contains("tenant/scope foundation kernel")
                .contains("module kernel extraction");
    }

    private Path resolveRepoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.isDirectory(current.resolve("scripts"))) {
            current = current.getParent();
        }
        assertThat(current).isNotNull();
        return current;
    }
}
