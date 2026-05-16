package com.mmmail.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.commercial.Edition;
import com.mmmail.server.commercial.FeatureCode;
import com.mmmail.server.commercial.LicenseClaims;
import com.mmmail.server.commercial.LicenseFailureReason;
import com.mmmail.server.commercial.LicenseKeyVerifier;
import com.mmmail.server.commercial.LicenseStateRepository;
import com.mmmail.server.commercial.LicenseSyncService;
import com.mmmail.server.commercial.LicenseVerificationException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackendV22LicenseVerifierContractTest {

    private static final Instant NOW = Instant.parse("2026-05-16T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void verifierAcceptsValidEd25519LicenseClaims() throws Exception {
        KeyPair keyPair = generateEd25519KeyPair();
        String licenseKey = signLicense(keyPair.getPrivate(), validClaims(42L, "PRO", NOW.plusSeconds(86400)));

        LicenseClaims claims = new LicenseKeyVerifier(OBJECT_MAPPER, FIXED_CLOCK)
                .verify(licenseKey, 42L, keyPair.getPublic());

        assertThat(claims.orgId()).isEqualTo(42L);
        assertThat(claims.edition()).isEqualTo(Edition.PRO);
        assertThat(claims.seats()).isEqualTo(12);
        assertThat(claims.features()).containsExactlyInAnyOrder(FeatureCode.LICENSE_MANAGEMENT, FeatureCode.BILLING_ADMIN);
        assertThat(claims.issuedAt()).isEqualTo(NOW.minusSeconds(60));
        assertThat(claims.expiresAt()).isEqualTo(NOW.plusSeconds(86400));
    }

    @Test
    void verifierRejectsTamperedExpiredMalformedOrgMismatchAndWrongKeyLicenses() throws Exception {
        KeyPair trustedKeyPair = generateEd25519KeyPair();
        KeyPair wrongKeyPair = generateEd25519KeyPair();
        LicenseKeyVerifier verifier = new LicenseKeyVerifier(OBJECT_MAPPER, FIXED_CLOCK);
        String validLicense = signLicense(trustedKeyPair.getPrivate(), validClaims(42L, "BUSINESS", NOW.plusSeconds(86400)));

        assertLicenseFailure(verifier, tamperSignature(validLicense), 42L, trustedKeyPair.getPublic(), LicenseFailureReason.SIGNATURE_INVALID);
        assertLicenseFailure(
                verifier,
                signLicense(trustedKeyPair.getPrivate(), validClaims(42L, "PRO", NOW.minusSeconds(1))),
                42L,
                trustedKeyPair.getPublic(),
                LicenseFailureReason.EXPIRED
        );
        assertLicenseFailure(verifier, validLicense, 99L, trustedKeyPair.getPublic(), LicenseFailureReason.ORG_MISMATCH);
        assertLicenseFailure(verifier, "not-a-license", 42L, trustedKeyPair.getPublic(), LicenseFailureReason.FORMAT_INVALID);
        assertLicenseFailure(verifier, validLicense, 42L, wrongKeyPair.getPublic(), LicenseFailureReason.SIGNATURE_INVALID);
    }

    @Test
    void comm02RepositoryServiceSchemaAndMigrationContractsExist() throws Exception {
        assertThat(LicenseStateRepository.class).isInterface();
        assertThat(LicenseSyncService.class).isNotNull();

        String schema = Files.readString(repoPath("contracts/license/license-claims.schema.json"));
        assertThat(schema).contains("\"orgId\"", "\"edition\"", "\"seats\"", "\"features\"", "\"issuedAt\"", "\"expiresAt\"");
        assertThat(schema).contains("\"FREE\"", "\"PRO\"", "\"BUSINESS\"");
        assertThat(schema).contains("license.management", "billing.admin", "oidc.sso", "audit.export", "dsr.requests");

        String migration = Files.readString(repoPath("backend/mmmail-server/src/main/resources/db/migration/V37__license_init.sql"));
        assertThat(migration).contains("-- DESCRIPTION:", "-- ROLLBACK:");
        assertThat(migration).contains("create table if not exists license_state");
        assertThat(migration).contains("org_id bigint not null primary key");
        assertThat(migration).contains("claims_json text not null");
        assertThat(migration).contains("status varchar(32) not null");
        assertThat(migration).contains("synced_at timestamp not null");
        assertThat(migration).contains("schema_version = '37'");
    }

    private static void assertLicenseFailure(
            LicenseKeyVerifier verifier,
            String licenseKey,
            long expectedOrgId,
            PublicKey publicKey,
            LicenseFailureReason reason
    ) {
        assertThatThrownBy(() -> verifier.verify(licenseKey, expectedOrgId, publicKey))
                .isInstanceOfSatisfying(LicenseVerificationException.class, ex -> {
                    LicenseVerificationException failure = (LicenseVerificationException) ex;
                    assertThat(failure.reason()).isEqualTo(reason);
                });
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

    private static Map<String, Object> validClaims(long orgId, String edition, Instant expiresAt) {
        return Map.of(
                "orgId", orgId,
                "edition", edition,
                "seats", 12,
                "features", List.of("license.management", "billing.admin"),
                "issuedAt", NOW.minusSeconds(60).toString(),
                "expiresAt", expiresAt.toString()
        );
    }

    private static String tamperSignature(String licenseKey) {
        int signatureIndex = licenseKey.lastIndexOf('.');
        byte[] signature = Base64.getUrlDecoder().decode(licenseKey.substring(signatureIndex + 1));
        signature[0] = (byte) (signature[0] ^ 0x01);
        return licenseKey.substring(0, signatureIndex + 1)
                + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    private static Path repoPath(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("contracts"))) {
            current = current.getParent();
        }
        return current.resolve(relativePath);
    }
}
