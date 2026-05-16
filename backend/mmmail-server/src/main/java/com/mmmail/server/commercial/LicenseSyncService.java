package com.mmmail.server.commercial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.observability.RuntimeTraceService;
import java.security.PublicKey;
import java.time.Clock;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LicenseSyncService {

    private final LicenseKeyVerifier verifier;
    private final LicenseStateRepository repository;
    private final ObjectMapper objectMapper;
    private final RuntimeTraceService runtimeTraceService;
    private final Clock clock;

    @Autowired
    public LicenseSyncService(
            LicenseKeyVerifier verifier,
            LicenseStateRepository repository,
            ObjectMapper objectMapper,
            RuntimeTraceService runtimeTraceService
    ) {
        this(verifier, repository, objectMapper, runtimeTraceService, Clock.systemUTC());
    }

    LicenseSyncService(
            LicenseKeyVerifier verifier,
            LicenseStateRepository repository,
            ObjectMapper objectMapper,
            RuntimeTraceService runtimeTraceService,
            Clock clock
    ) {
        this.verifier = verifier;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.runtimeTraceService = runtimeTraceService;
        this.clock = clock;
    }

    public LicenseState syncLicense(String licenseKey, long orgId, PublicKey publicKey) {
        return runtimeTraceService.observe("mmmail.license.verify", Map.of(
                "component", "license",
                "operation", "sync"
        ), () -> {
            LicenseClaims claims = verifier.verify(licenseKey, orgId, publicKey);
            LicenseState state = new LicenseState(
                    orgId,
                    claimsJson(claims),
                    LicenseStatus.ACTIVE,
                    clock.instant(),
                    claims.expiresAt()
            );
            repository.save(state);
            return state;
        });
    }

    private String claimsJson(LicenseClaims claims) {
        try {
            return objectMapper.writeValueAsString(claimMap(claims));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize license claims", ex);
        }
    }

    private Map<String, Object> claimMap(LicenseClaims claims) {
        return Map.of(
                "orgId", claims.orgId(),
                "edition", claims.edition().name(),
                "seats", claims.seats(),
                "features", claims.features().stream().map(FeatureCode::code).sorted().toList(),
                "issuedAt", claims.issuedAt().toString(),
                "expiresAt", claims.expiresAt().toString()
        );
    }
}
