package com.mmmail.server.commercial;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LicenseKeyVerifier {

    private static final String ALG = "EdDSA";
    private static final String TYP = "MMMail-License";

    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public LicenseKeyVerifier(ObjectMapper objectMapper) {
        this(objectMapper, Clock.systemUTC());
    }

    public LicenseKeyVerifier(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public LicenseClaims verify(String licenseKey, long expectedOrgId, PublicKey publicKey) {
        LicenseParts parts = split(licenseKey);
        verifyHeader(parts.headerJson());
        verifySignature(parts.signingInput(), parts.signature(), publicKey);
        LicenseClaims claims = parseClaims(parts.payloadJson());
        if (claims.orgId() != expectedOrgId) {
            throw failure(LicenseFailureReason.ORG_MISMATCH, "license org does not match requested org");
        }
        if (claims.isExpired(clock)) {
            throw failure(LicenseFailureReason.EXPIRED, "license has expired");
        }
        return claims;
    }

    private LicenseParts split(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            throw failure(LicenseFailureReason.FORMAT_INVALID, "license key is required");
        }
        String[] parts = licenseKey.split("\\.", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw failure(LicenseFailureReason.FORMAT_INVALID, "license key must use header.payload.signature");
        }
        return decodeParts(parts);
    }

    private LicenseParts decodeParts(String[] parts) {
        try {
            String signingInput = parts[0] + "." + parts[1];
            return new LicenseParts(signingInput, decodeText(parts[0]), decodeText(parts[1]), decodeBytes(parts[2]));
        } catch (IllegalArgumentException ex) {
            throw failure(LicenseFailureReason.FORMAT_INVALID, "license key contains invalid base64url");
        }
    }

    private void verifyHeader(String headerJson) {
        JsonNode header = readJson(headerJson, LicenseFailureReason.FORMAT_INVALID);
        if (!ALG.equals(text(header, "alg")) || !TYP.equals(text(header, "typ"))) {
            throw failure(LicenseFailureReason.FORMAT_INVALID, "license header must use EdDSA MMMail-License");
        }
    }

    private void verifySignature(String signingInput, byte[] signatureBytes, PublicKey publicKey) {
        if (publicKey == null) {
            throw failure(LicenseFailureReason.PUBLIC_KEY_INVALID, "license public key is required");
        }
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
            if (!signature.verify(signatureBytes)) {
                throw failure(LicenseFailureReason.SIGNATURE_INVALID, "license signature does not match claims");
            }
        } catch (LicenseVerificationException ex) {
            throw ex;
        } catch (InvalidKeyException | NoSuchAlgorithmException ex) {
            throw failure(LicenseFailureReason.PUBLIC_KEY_INVALID, "license public key cannot verify Ed25519");
        } catch (SignatureException ex) {
            throw failure(LicenseFailureReason.SIGNATURE_INVALID, "license signature cannot be verified");
        }
    }

    private LicenseClaims parseClaims(String payloadJson) {
        JsonNode payload = readJson(payloadJson, LicenseFailureReason.CLAIMS_INVALID);
        try {
            return new LicenseClaims(
                    number(payload, "orgId").longValue(),
                    Edition.valueOf(text(payload, "edition")),
                    number(payload, "seats").intValue(),
                    features(payload),
                    Instant.parse(text(payload, "issuedAt")),
                    Instant.parse(text(payload, "expiresAt"))
            );
        } catch (RuntimeException ex) {
            throw failure(LicenseFailureReason.CLAIMS_INVALID, "license claims are invalid");
        }
    }

    private Set<FeatureCode> features(JsonNode payload) {
        JsonNode node = payload.get("features");
        if (node == null || !node.isArray()) {
            throw failure(LicenseFailureReason.CLAIMS_INVALID, "license features must be an array");
        }
        Set<FeatureCode> features = new LinkedHashSet<>();
        node.forEach(feature -> features.add(FeatureCode.fromCode(feature.asText())));
        return features;
    }

    private JsonNode readJson(String json, LicenseFailureReason reason) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            throw failure(reason, "license json is invalid");
        }
    }

    private Number number(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        if (node == null || !node.isNumber()) {
            throw failure(LicenseFailureReason.CLAIMS_INVALID, "license claim is not numeric: " + field);
        }
        return node.numberValue();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw failure(LicenseFailureReason.CLAIMS_INVALID, "license claim is not text: " + field);
        }
        return value.asText();
    }

    private byte[] decodeBytes(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private String decodeText(String value) {
        return new String(decodeBytes(value), StandardCharsets.UTF_8);
    }

    private LicenseVerificationException failure(LicenseFailureReason reason, String message) {
        return new LicenseVerificationException(reason, message);
    }

    private record LicenseParts(String signingInput, String headerJson, String payloadJson, byte[] signature) {
    }
}
