package com.mmmail.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BackendV22CommercialSurfaceCoverageContractTest {

    @Test
    void currentCommercialSurfaceDocumentationClassifiesEveryV22Boundary() throws Exception {
        String surface = read("docs/commercial/edition-entitlement-surface.md");

        assertThat(surface).contains(
                "GET /api/v2/billing/license/status",
                "POST /api/v2/billing/license",
                "POST /api/v2/billing/webhook",
                "GET /api/v2/orgs/{orgId}/oidc/config",
                "PUT /api/v2/orgs/{orgId}/oidc/config",
                "POST /api/v2/auth/oidc/login",
                "GET /api/v2/auth/oidc/callback",
                "GET /api/v2/orgs/{orgId}/audit/events/export",
                "POST /api/v2/orgs/{orgId}/dsr/export",
                "POST /api/v2/orgs/{orgId}/dsr/erasure",
                "GET /api/v2/orgs/{orgId}/dsr/jobs/{jobId}"
        );
        assertThat(surface).contains("upgrade path", "External billing gateway callback", "Business `oidc.sso`");
        assertThat(surface).contains("When a new Pro or Business backend endpoint is added");
    }

    @Test
    void businessOnlyControllersEnforceFeatureGatesOnTheServerSide() throws Exception {
        String oidcController = read("backend/mmmail-server/src/main/java/com/mmmail/server/controller/OidcSsoController.java");
        String oidcService = read("backend/mmmail-server/src/main/java/com/mmmail/server/commercial/oidc/OidcSsoService.java");
        String auditController = read("backend/mmmail-server/src/main/java/com/mmmail/server/controller/OrgAuditExportController.java");
        String dsrController = read("backend/mmmail-server/src/main/java/com/mmmail/server/controller/DsrRequestController.java");

        assertThat(count(oidcController, "commercialAuthorizationGate.enforceFeature(request, orgId, FeatureCode.OIDC_SSO)")).isEqualTo(2);
        assertThat(oidcService).contains("featureGate().requireFeature", "FeatureCode.OIDC_SSO");
        assertThat(auditController).contains("commercialAuthorizationGate.enforceFeature(request, orgId, FeatureCode.AUDIT_EXPORT)");
        assertThat(count(dsrController, "commercialAuthorizationGate.enforceFeature(request, orgId, FeatureCode.DSR_REQUESTS)")).isEqualTo(3);
    }

    @Test
    void upgradeAndWebhookSurfacesDoNotSilentlyGrantPaidState() throws Exception {
        String licenseController = read("backend/mmmail-server/src/main/java/com/mmmail/server/controller/CommercialLicenseController.java");
        String webhookService = read("backend/mmmail-server/src/main/java/com/mmmail/server/commercial/BillingWebhookService.java");
        String noopProvider = read("backend/mmmail-server/src/main/java/com/mmmail/server/commercial/NoopBillingProvider.java");
        String verifier = read("backend/mmmail-server/src/main/java/com/mmmail/server/commercial/BillingWebhookVerifier.java");

        assertThat(licenseController).contains("activeOrgId(request)", "V2_ENTITLEMENT_REQUIRED");
        assertThat(licenseController).doesNotContain("FeatureCode.LICENSE_MANAGEMENT");
        assertThat(webhookService).contains("verifier.verify(event, signatureHeader, webhookSecret)");
        assertThat(webhookService).contains("Webhook billing provider cannot apply paid state");
        assertThat(noopProvider).contains("return false;");
        assertThat(verifier).contains("HmacSHA256", "requireTimestampInWindow", "MessageDigest.isEqual");
    }

    @Test
    void commercialSurfaceCoverageContractIsWiredIntoGovernanceGates() throws Exception {
        String validateLocal = read("scripts/validate-local.sh");
        String ci = read(".github/workflows/ci.yml");
        String spec = read("docs/v22-open-source-commercial-spec.md");

        assertThat(validateLocal).contains("BackendV22CommercialSurfaceCoverageContractTest");
        assertThat(ci).contains("BackendV22CommercialSurfaceCoverageContractTest");
        assertThat(spec).contains("BackendV22CommercialSurfaceCoverageContractTest");
        assertThat(spec).contains("COMM-04 | done |");
    }

    private String read(String path) throws IOException {
        return Files.readString(repoRoot().resolve(path));
    }

    private int count(String source, String needle) {
        return source.split(java.util.regex.Pattern.quote(needle), -1).length - 1;
    }

    private Path repoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.exists(current.resolve(".github/workflows/ci.yml"))) {
            current = current.getParent();
        }
        return current;
    }
}
