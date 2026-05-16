package com.mmmail.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.common.model.Result;
import com.mmmail.server.commercial.CommercialLicenseStatusMapper;
import com.mmmail.server.commercial.CommercialLicenseStatusReader;
import com.mmmail.server.commercial.CommercialLicenseUploadService;
import com.mmmail.server.commercial.LicenseKeyVerifier;
import com.mmmail.server.commercial.LicensePublicKeyProvider;
import com.mmmail.server.commercial.LicenseState;
import com.mmmail.server.commercial.LicenseStateRepository;
import com.mmmail.server.commercial.LicenseSyncService;
import com.mmmail.server.controller.CommercialLicenseController;
import com.mmmail.server.model.dto.CommercialLicenseUploadRequest;
import com.mmmail.server.model.vo.CommercialLicenseStatusVo;
import com.mmmail.server.observability.RuntimeTraceService;
import com.mmmail.server.service.OrgProductAccessGuardService;
import io.micrometer.observation.ObservationRegistry;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackendV22LicenseManagementApiContractTest {

    private static final long ORG_ID = 99L;
    private static final Instant NOW = Instant.parse("2026-05-16T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void statusEndpointReturnsMissingStateForOrgWithoutLicense() {
        CommercialLicenseController controller = controller(new InMemoryLicenseStateRepository(), "");
        MockHttpServletRequest request = request("/api/v2/billing/license/status", ORG_ID);

        Result<CommercialLicenseStatusVo> result = controller.readStatus(request);

        assertThat(result.data().orgId()).isEqualTo(String.valueOf(ORG_ID));
        assertThat(result.data().state()).isEqualTo(CommercialLicenseStatusVo.State.MISSING);
        assertThat(result.data().edition()).isEqualTo("FREE");
        assertThat(result.data().requiredAction()).isEqualTo("upload-license");
        assertThat(result.data().externalBillingStatus()).isEqualTo("LICENSE_KEY");
    }

    @Test
    void uploadEndpointVerifiesLicenseWithConfiguredPublicKeyAndPersistsState() throws Exception {
        KeyPair keyPair = generateEd25519KeyPair();
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        InMemoryLicenseStateRepository repository = new InMemoryLicenseStateRepository();
        CommercialLicenseController controller = controller(repository, publicKey);
        String licenseKey = signLicense(keyPair.getPrivate(), validClaims(ORG_ID, NOW.plusSeconds(86400)));

        Result<CommercialLicenseStatusVo> result = controller.uploadLicense(
                new CommercialLicenseUploadRequest(licenseKey),
                request("/api/v2/billing/license", ORG_ID)
        );

        assertThat(result.data().state()).isEqualTo(CommercialLicenseStatusVo.State.ACTIVE);
        assertThat(result.data().edition()).isEqualTo("PRO");
        assertThat(result.data().features()).containsExactly("billing.admin", "license.management");
        assertThat(repository.findByOrgId(ORG_ID)).isPresent();
    }

    @Test
    void licenseManagementRequiresOrgContextAndConfiguredPublicKey() {
        CommercialLicenseController noOrgController = controller(new InMemoryLicenseStateRepository(), "unused", null);
        CommercialLicenseController noKeyController = controller(new InMemoryLicenseStateRepository(), "");

        assertThatThrownBy(() -> noOrgController.readStatus(request("/api/v2/billing/license/status", null)))
                .isInstanceOfSatisfying(BizException.class, error -> {
                    BizException exception = (BizException) error;
                    assertThat(exception.getCode()).isEqualTo(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode());
                })
                .hasMessageContaining("upgradeAction=select-org");

        assertThatThrownBy(() -> noKeyController.uploadLicense(
                new CommercialLicenseUploadRequest("header.payload.signature"),
                request("/api/v2/billing/license", ORG_ID)
        ))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("MMMAIL_LICENSE_PUBLIC_KEY");
    }

    private CommercialLicenseController controller(LicenseStateRepository repository, String publicKey) {
        return controller(repository, publicKey, ORG_ID);
    }

    private CommercialLicenseController controller(
            LicenseStateRepository repository,
            String publicKey,
            Long activeOrgId
    ) {
        OrgProductAccessGuardService guardService = mock(OrgProductAccessGuardService.class);
        CommercialLicenseStatusMapper mapper = new CommercialLicenseStatusMapper(OBJECT_MAPPER, FIXED_CLOCK);
        LicenseSyncService syncService = new LicenseSyncService(
                new LicenseKeyVerifier(OBJECT_MAPPER, FIXED_CLOCK),
                repository,
                OBJECT_MAPPER,
                new RuntimeTraceService(ObservationRegistry.create())
        );
        when(guardService.resolveActiveOrgId(org.mockito.ArgumentMatchers.any())).thenReturn(activeOrgId);
        return new CommercialLicenseController(
                guardService,
                new CommercialLicenseStatusReader(repository, mapper),
                new CommercialLicenseUploadService(syncService, new LicensePublicKeyProvider(publicKey), mapper)
        );
    }

    private MockHttpServletRequest request(String path, Long orgId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (orgId != null) {
            request.addHeader(OrgProductAccessGuardService.ACTIVE_ORG_HEADER, String.valueOf(orgId));
        }
        return request;
    }

    private static KeyPair generateEd25519KeyPair() throws Exception {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
    }

    private static String signLicense(PrivateKey privateKey, Map<String, Object> claims) throws Exception {
        String header = encodeJson(Map.of("alg", "EdDSA", "typ", "MMMail-License"));
        String payload = encodeJson(claims);
        String signingInput = header + "." + payload;
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(privateKey);
        signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
        return signingInput + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
    }

    private static String encodeJson(Map<String, Object> value) throws Exception {
        byte[] json = OBJECT_MAPPER.writeValueAsBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
    }

    private static Map<String, Object> validClaims(long orgId, Instant expiresAt) {
        return Map.of(
                "orgId", orgId,
                "edition", "PRO",
                "seats", 12,
                "features", List.of("license.management", "billing.admin"),
                "issuedAt", NOW.minusSeconds(60).toString(),
                "expiresAt", expiresAt.toString()
        );
    }

    private static final class InMemoryLicenseStateRepository implements LicenseStateRepository {
        private final Map<Long, LicenseState> states = new ConcurrentHashMap<>();

        @Override
        public void save(LicenseState state) {
            states.put(state.orgId(), state);
        }

        @Override
        public Optional<LicenseState> findByOrgId(long orgId) {
            return Optional.ofNullable(states.get(orgId));
        }
    }
}
