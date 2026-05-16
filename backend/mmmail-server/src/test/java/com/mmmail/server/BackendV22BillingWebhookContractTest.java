package com.mmmail.server;

import com.mmmail.server.commercial.BillingProvider;
import com.mmmail.server.commercial.BillingProviderRegistry;
import com.mmmail.server.commercial.BillingProviderType;
import com.mmmail.server.commercial.BillingWebhookApplyResult;
import com.mmmail.server.commercial.BillingWebhookEvent;
import com.mmmail.server.commercial.BillingWebhookEventRepository;
import com.mmmail.server.commercial.BillingWebhookVerificationException;
import com.mmmail.server.commercial.BillingWebhookVerifier;
import com.mmmail.server.commercial.BillingWebhookVerificationFailureReason;
import com.mmmail.server.commercial.NoopBillingProvider;
import com.mmmail.server.commercial.SubscriptionPlan;
import com.mmmail.server.commercial.SubscriptionState;
import com.mmmail.server.commercial.SubscriptionStateMachine;
import com.mmmail.server.commercial.SubscriptionStateRepository;
import com.mmmail.server.commercial.SubscriptionStatus;
import com.mmmail.server.controller.BillingWebhookController;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackendV22BillingWebhookContractTest {

    private static final Instant NOW = Instant.parse("2026-05-16T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void registryKeepsNoopProviderFromGrantingPaidState() {
        BillingProvider noop = new NoopBillingProvider();
        BillingProvider webhook = new WebhookProviderFixture();
        BillingProviderRegistry registry = new BillingProviderRegistry(Set.of(noop, webhook));

        assertThat(noop.type()).isEqualTo(BillingProviderType.NONE);
        assertThat(noop.supportsPaidState()).isFalse();
        assertThat(registry.require(BillingProviderType.NONE)).isSameAs(noop);
        assertThat(registry.require(BillingProviderType.WEBHOOK)).isSameAs(webhook);
    }

    @Test
    void verifierRequiresValidHmacSignatureSecretAndTimestampWindow() {
        BillingWebhookVerifier verifier = new BillingWebhookVerifier(FIXED_CLOCK, Duration.ofMinutes(5));
        BillingWebhookEvent event = event("evt-valid", NOW.minusSeconds(60), SubscriptionStatus.ACTIVE);
        String signature = sign("webhook-secret", canonical(event));

        verifier.verify(event, signature, "webhook-secret");

        assertWebhookFailure(verifier, event, "v1=bad", "webhook-secret", BillingWebhookVerificationFailureReason.SIGNATURE_INVALID);
        assertWebhookFailure(verifier, event, signature, "", BillingWebhookVerificationFailureReason.SECRET_MISSING);
        assertWebhookFailure(
                verifier,
                event("evt-stale", NOW.minusSeconds(600), SubscriptionStatus.ACTIVE),
                sign("webhook-secret", canonical(event("evt-stale", NOW.minusSeconds(600), SubscriptionStatus.ACTIVE))),
                "webhook-secret",
                BillingWebhookVerificationFailureReason.TIMESTAMP_OUT_OF_WINDOW
        );
    }

    @Test
    void stateMachineStoresWebhookEventOnceAndUpdatesSubscriptionState() {
        RecordingWebhookEventRepository eventRepository = new RecordingWebhookEventRepository();
        RecordingSubscriptionStateRepository stateRepository = new RecordingSubscriptionStateRepository();
        SubscriptionStateMachine stateMachine = new SubscriptionStateMachine(eventRepository, stateRepository, FIXED_CLOCK);
        BillingWebhookEvent event = event("evt-once", NOW, SubscriptionStatus.PAST_DUE);

        assertThat(stateMachine.apply(event)).isEqualTo(BillingWebhookApplyResult.APPLIED);
        assertThat(stateMachine.apply(event)).isEqualTo(BillingWebhookApplyResult.DUPLICATE);
        assertThat(eventRepository.processedCount()).isEqualTo(1);
        assertThat(stateRepository.saveCount()).isEqualTo(1);
        assertThat(stateRepository.findByOrgId(42L))
                .get()
                .extracting(SubscriptionState::plan, SubscriptionState::status)
                .containsExactly(SubscriptionPlan.PRO, SubscriptionStatus.PAST_DUE);
    }

    @Test
    void stateMachineAcceptsAllCommercialWebhookStatuses() {
        RecordingWebhookEventRepository eventRepository = new RecordingWebhookEventRepository();
        RecordingSubscriptionStateRepository stateRepository = new RecordingSubscriptionStateRepository();
        SubscriptionStateMachine stateMachine = new SubscriptionStateMachine(eventRepository, stateRepository, FIXED_CLOCK);

        for (SubscriptionStatus status : SubscriptionStatus.values()) {
            assertThat(stateMachine.apply(event("evt-" + status.name(), NOW, status)))
                    .isEqualTo(BillingWebhookApplyResult.APPLIED);
        }
    }

    @Test
    void comm03ControllerSchemaDocsAndMigrationContractsExist() throws Exception {
        assertThat(BillingWebhookController.class).isNotNull();

        String schema = Files.readString(repoPath("contracts/billing/webhook-event.schema.json"));
        assertThat(schema).contains("\"eventId\"", "\"orgId\"", "\"plan\"", "\"status\"", "\"occurredAt\"", "\"signatureVersion\"");
        assertThat(schema).contains("\"FREE\"", "\"PRO\"", "\"BUSINESS\"");
        assertThat(schema).contains("\"TRIAL\"", "\"ACTIVE\"", "\"PAST_DUE\"", "\"CANCELED\"", "\"EXPIRED\"");

        String signatureDoc = Files.readString(repoPath("docs/billing/webhook-signature.md"));
        assertThat(signatureDoc).contains("X-MMMail-Billing-Signature", "HMAC-SHA256", "v1=");

        String migration = Files.readString(repoPath("backend/mmmail-server/src/main/resources/db/migration/V38__billing_webhook_init.sql"));
        assertThat(migration).contains("-- DESCRIPTION:", "-- ROLLBACK:");
        assertThat(migration).contains("create table if not exists billing_webhook_event");
        assertThat(migration).contains("create table if not exists org_subscription_state");
        assertThat(migration).contains("event_id varchar(128) not null primary key");
        assertThat(migration).contains("schema_version = '38'");
    }

    private static void assertWebhookFailure(
            BillingWebhookVerifier verifier,
            BillingWebhookEvent event,
            String signature,
            String secret,
            BillingWebhookVerificationFailureReason reason
    ) {
        assertThatThrownBy(() -> verifier.verify(event, signature, secret))
                .isInstanceOfSatisfying(BillingWebhookVerificationException.class, ex -> {
                    BillingWebhookVerificationException failure = (BillingWebhookVerificationException) ex;
                    assertThat(failure.reason()).isEqualTo(reason);
                });
    }

    private static BillingWebhookEvent event(String id, Instant occurredAt, SubscriptionStatus status) {
        return new BillingWebhookEvent(id, 42L, SubscriptionPlan.PRO, status, occurredAt, "v1");
    }

    private static String canonical(BillingWebhookEvent event) {
        return String.join("\n",
                event.signatureVersion(),
                event.eventId(),
                String.valueOf(event.orgId()),
                event.plan().name(),
                event.status().name(),
                event.occurredAt().toString());
    }

    private static String sign(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "v1=" + bytesToHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign webhook fixture", ex);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }

    private static Path repoPath(String relativePath) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("contracts"))) {
            current = current.getParent();
        }
        return current.resolve(relativePath);
    }

    private static final class WebhookProviderFixture implements BillingProvider {
        @Override
        public BillingProviderType type() {
            return BillingProviderType.WEBHOOK;
        }

        @Override
        public boolean supportsPaidState() {
            return true;
        }
    }

    private static final class RecordingWebhookEventRepository implements BillingWebhookEventRepository {
        private final Set<String> processed = new HashSet<>();

        @Override
        public boolean markProcessed(BillingWebhookEvent event, Instant processedAt) {
            return processed.add(event.eventId());
        }

        int processedCount() {
            return processed.size();
        }
    }

    private static final class RecordingSubscriptionStateRepository implements SubscriptionStateRepository {
        private SubscriptionState state;
        private int saves;

        @Override
        public void save(SubscriptionState state) {
            this.state = state;
            saves++;
        }

        @Override
        public Optional<SubscriptionState> findByOrgId(long orgId) {
            return state != null && state.orgId() == orgId ? Optional.of(state) : Optional.empty();
        }

        int saveCount() {
            return saves;
        }
    }
}
