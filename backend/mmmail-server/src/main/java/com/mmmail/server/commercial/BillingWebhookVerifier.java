package com.mmmail.server.commercial;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class BillingWebhookVerifier {

    private static final String SIGNATURE_VERSION = "v1";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration DEFAULT_WINDOW = Duration.ofMinutes(5);

    private final Clock clock;
    private final Duration window;

    public BillingWebhookVerifier() {
        this(Clock.systemUTC(), DEFAULT_WINDOW);
    }

    public BillingWebhookVerifier(Clock clock, Duration window) {
        this.clock = clock;
        this.window = window;
    }

    public void verify(BillingWebhookEvent event, String signatureHeader, String secret) {
        requireSecret(secret);
        requireVersion(event);
        requireTimestampInWindow(event);
        requireSignature(event, signatureHeader, secret);
    }

    private static void requireSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new BillingWebhookVerificationException(BillingWebhookVerificationFailureReason.SECRET_MISSING);
        }
    }

    private static void requireVersion(BillingWebhookEvent event) {
        if (!SIGNATURE_VERSION.equals(event.signatureVersion())) {
            throw new BillingWebhookVerificationException(BillingWebhookVerificationFailureReason.VERSION_UNSUPPORTED);
        }
    }

    private void requireTimestampInWindow(BillingWebhookEvent event) {
        Duration age = Duration.between(event.occurredAt(), clock.instant()).abs();
        if (age.compareTo(window) > 0) {
            throw new BillingWebhookVerificationException(BillingWebhookVerificationFailureReason.TIMESTAMP_OUT_OF_WINDOW);
        }
    }

    private static void requireSignature(BillingWebhookEvent event, String header, String secret) {
        String expected = SIGNATURE_VERSION + "=" + hmacHex(secret, event.canonicalPayload());
        if (header == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), header.getBytes(StandardCharsets.UTF_8))) {
            throw new BillingWebhookVerificationException(BillingWebhookVerificationFailureReason.SIGNATURE_INVALID);
        }
    }

    private static String hmacHex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return bytesToHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to verify billing webhook signature", ex);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }
}
