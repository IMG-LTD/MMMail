package com.mmmail.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ContractCatalogRegressionTest {

    @Test
    void remainingBackendCatalogEntriesShouldResolveToFrozenContractIdentities() throws Exception {
        Path repoRoot = resolveRepoRoot();

        assertOpenApiContract(repoRoot, "contracts/openapi/identity.yaml", "MMMail Identity Contract",
                "paths:\n  /api/v1/auth/login:",
                "  /api/v1/auth/refresh:");
        assertOpenApiContract(repoRoot, "contracts/openapi/platform.yaml", "MMMail Platform Contract",
                "paths:\n  /api/v2/platform/capabilities:");
        assertOpenApiContract(repoRoot, "contracts/openapi/mail.yaml", "MMMail Mail Public Share Contract",
                "paths:\n  /api/v1/public/mail/secure-links/{token}:");
        assertTokenPathParameter(repoRoot, "contracts/openapi/mail.yaml",
                "/api/v1/public/mail/secure-links/{token}:\n" +
                        "    parameters:\n" +
                        "      - name: token\n" +
                        "        in: path\n" +
                        "        required: true\n" +
                        "        schema:\n" +
                        "          type: string");
        assertOpenApiContract(repoRoot, "contracts/openapi/pass.yaml", "MMMail Pass Public Share Contract",
                "paths:\n  /api/v1/public/pass/secure-links/{token}:");
        assertTokenPathParameter(repoRoot, "contracts/openapi/pass.yaml",
                "/api/v1/public/pass/secure-links/{token}:\n" +
                        "    parameters:\n" +
                        "      - name: token\n" +
                        "        in: path\n" +
                        "        required: true\n" +
                        "        schema:\n" +
                        "          type: string");
        assertOpenApiContract(repoRoot, "contracts/openapi/workspace.yaml", "MMMail Workspace Aggregation Contract",
                "paths:\n  /api/v2/workspace/aggregation:");
        assertOpenApiContract(repoRoot, "contracts/openapi/billing.yaml", "MMMail Billing Readiness Contract",
                "paths:\n  /api/v2/billing/readiness:");

        assertEventContract(repoRoot, "contracts/events/identity.yaml", "identity.session", "identity.session.issued");
        assertEventContract(repoRoot, "contracts/events/platform.yaml", "platform.outbox", "platform.outbox.published");
        assertEventContract(repoRoot, "contracts/events/mail.yaml", "mail.secure-link", "mail.secure-link.viewed");
        assertEventContract(repoRoot, "contracts/events/pass.yaml", "pass.secure-link", "pass.secure-link.viewed");
        assertEventContract(repoRoot, "contracts/events/workspace.yaml", "workspace.aggregation", "workspace.aggregation.requested");
        assertEventContract(repoRoot, "contracts/events/billing.yaml", "billing.readiness", "billing.readiness.queried");
    }

    private void assertOpenApiContract(Path repoRoot, String relativePath, String expectedTitle, String... expectedPathIdentities) throws Exception {
        String content = Files.readString(repoRoot.resolve(relativePath));

        assertThat(content).contains(
                "openapi: 3.1.0\n" +
                        "info:\n" +
                        "  title: " + expectedTitle + "\n" +
                        "  version: v2-freeze");
        for (String expectedPathIdentity : expectedPathIdentities) {
            assertThat(content).contains(expectedPathIdentity);
        }
    }

    private void assertTokenPathParameter(Path repoRoot, String relativePath, String expectedTokenParameterIdentity) throws Exception {
        assertThat(Files.readString(repoRoot.resolve(relativePath))).contains(expectedTokenParameterIdentity);
    }

    private void assertEventContract(Path repoRoot, String relativePath, String expectedFamily, String requiredExample) throws Exception {
        String content = Files.readString(repoRoot.resolve(relativePath));

        assertThat(content).contains(
                "version: v2-freeze\n" +
                        "family: " + expectedFamily);
        assertThat(content).contains(requiredExample);
    }

    private Path resolveRepoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.isDirectory(current.resolve("contracts"))) {
            current = current.getParent();
        }
        assertThat(current).isNotNull();
        return current;
    }
}
