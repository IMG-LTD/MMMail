package com.mmmail.server;

import com.mmmail.platform.event.PlatformEvent;
import com.mmmail.platform.event.PlatformEventMetadata;
import com.mmmail.platform.event.PlatformEventType;
import com.mmmail.platform.outbox.OutboxDispatchResult;
import com.mmmail.platform.outbox.OutboxEventRecord;
import com.mmmail.platform.outbox.OutboxEventStatus;
import com.mmmail.platform.outbox.OutboxPublishRequest;
import com.mmmail.platform.outbox.OutboxPublishResult;
import com.mmmail.platform.outbox.OutboxPublisher;
import com.mmmail.server.outbox.InProcessOutboxDispatcher;
import com.mmmail.server.outbox.PlatformOutboxEvent;
import com.mmmail.server.outbox.PlatformOutboxEventMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class BackendV21EventOutboxFoundationTest {

    private static final Set<String> REQUIRED_EVENTS = Set.of(
            "identity.user.created",
            "identity.session.revoked",
            "workspace.activity.recorded",
            "mail.message.created",
            "mail.message.sent",
            "mail.rule.matched",
            "calendar.event.created",
            "calendar.booking.created",
            "drive.file.uploaded",
            "drive.file.shared",
            "docs.document.updated",
            "docs.version.created",
            "sheets.workbook.imported",
            "pass.item.updated",
            "pass.secure_link.created",
            "collaboration.task.updated",
            "command.run.requested",
            "command.run.completed",
            "notification.delivery.requested",
            "admin.audit.recorded",
            "billing.entitlement.changed",
            "labs.ai_job.requested"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private OutboxPublisher publisher;
    @Autowired
    private PlatformOutboxEventMapper mapper;
    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void cleanOutbox() {
        jdbcTemplate.update("delete from platform_outbox_event");
    }

    @Test
    void eventCatalogShouldCoverBackendV21ArchitectureEvents() {
        Set<String> names = Arrays.stream(PlatformEventType.values())
                .map(PlatformEventType::eventName)
                .collect(Collectors.toSet());

        assertThat(names).containsAll(REQUIRED_EVENTS);
        assertThat(PlatformEventType.MAIL_MESSAGE_SENT.ownerModule()).isEqualTo("mail");
        assertThat(PlatformEventType.BILLING_ENTITLEMENT_CHANGED.tenantRequired()).isTrue();
        assertThat(PlatformEventType.COMMAND_RUN_REQUESTED.replayable()).isTrue();
    }

    @Test
    void platformEventShouldRejectMissingRequiredMetadata() {
        PlatformEventMetadata metadata = new PlatformEventMetadata(
                "",
                "7",
                "req-1",
                "trace-1",
                "mail",
                "send",
                LocalDateTime.now()
        );

        assertThatThrownBy(() -> new PlatformEvent(
                PlatformEventType.MAIL_MESSAGE_SENT,
                "mail_message",
                "100",
                metadata,
                "{\"messageId\":\"100\"}",
                "mail-send-100"
        )).hasMessageContaining("tenantId is required");
    }

    @Test
    void outboxRecordShouldEnforceExplicitStatusTransitions() {
        OutboxEventRecord pending = OutboxEventRecord.pending(
                1L,
                event(),
                LocalDateTime.now()
        );

        OutboxEventRecord published = pending.markPublished(LocalDateTime.now());
        assertThat(published.status()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThatThrownBy(() -> published.markFailed("late failure", LocalDateTime.now()))
                .hasMessageContaining("Invalid outbox status transition");
    }

    @Test
    void publisherShouldPersistPendingEventsWithMetadataAndRejectIdempotencyMismatch() {
        OutboxPublishResult first = publisher.publish(new OutboxPublishRequest(event()));
        PlatformOutboxEvent saved = mapper.selectById(first.eventId());

        assertThat(first.status()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(first.duplicate()).isFalse();
        assertThat(saved.getEventType()).isEqualTo("mail.message.sent");
        assertThat(saved.getTenantId()).isEqualTo("tenant-1");
        assertThat(saved.getRequestId()).isEqualTo("req-1");
        assertThat(saved.getStatus()).isEqualTo("PENDING");

        OutboxPublishResult duplicate = publisher.publish(new OutboxPublishRequest(event()));
        assertThat(duplicate.eventId()).isEqualTo(first.eventId());
        assertThat(duplicate.duplicate()).isTrue();

        assertThatThrownBy(() -> publisher.publish(new OutboxPublishRequest(eventWithPayload("{\"messageId\":\"101\"}"))))
                .hasMessageContaining("idempotency key already belongs to a different event");
    }

    @Test
    void dispatcherShouldPublishSuccessfulDueEvents() {
        OutboxPublishResult result = publisher.publish(new OutboxPublishRequest(event()));
        InProcessOutboxDispatcher dispatcher = new InProcessOutboxDispatcher(
                mapper,
                new SimpleMeterRegistry(),
                new InProcessOutboxDispatcher.DispatcherOptions(eventRecord -> {
                }, 2, Duration.ZERO)
        );

        OutboxDispatchResult dispatchResult = dispatcher.dispatchDue(10, LocalDateTime.now());
        PlatformOutboxEvent saved = mapper.selectById(result.eventId());

        assertThat(dispatchResult.published()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo("PUBLISHED");
        assertThat(saved.getPublishedAt()).isNotNull();
    }

    @Test
    void dispatcherShouldDeadLetterRepeatedFailures() {
        OutboxPublishResult result = publisher.publish(new OutboxPublishRequest(event()));
        InProcessOutboxDispatcher dispatcher = new InProcessOutboxDispatcher(
                mapper,
                new SimpleMeterRegistry(),
                new InProcessOutboxDispatcher.DispatcherOptions(eventRecord -> {
                    throw new IllegalStateException("handler failed");
                }, 2, Duration.ZERO)
        );

        OutboxDispatchResult first = dispatcher.dispatchDue(10, LocalDateTime.now());
        OutboxDispatchResult second = dispatcher.dispatchDue(10, LocalDateTime.now());
        PlatformOutboxEvent saved = mapper.selectById(result.eventId());

        assertThat(first.failed()).isEqualTo(1);
        assertThat(second.deadLettered()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo("DEAD_LETTER");
        assertThat(saved.getAttempts()).isEqualTo(2);
    }

    @Test
    void migrationShouldFreezeOutboxTableAndIndexes() throws Exception {
        Path root = resolveRepoRoot();
        String migration = Files.readString(root.resolve("src/main/resources/db/migration/V11__platform_outbox_event.sql"));
        String schema = Files.readString(root.resolve("src/main/resources/schema.sql"));

        assertThat(migration).contains("create table if not exists platform_outbox_event");
        assertThat(migration).contains("uk_platform_outbox_idempotency");
        assertThat(migration).contains("idx_platform_outbox_status_next_attempt");
        assertThat(migration).contains("idx_platform_outbox_tenant_created");
        assertThat(schema).contains("create table if not exists platform_outbox_event");
    }

    @Test
    void communityTestProfileShouldUseInProcessOutboxWithoutExternalBrokerBeans() {
        assertThat(applicationContext.getBean(OutboxPublisher.class)).isNotNull();
        assertThat(applicationContext.getBean(InProcessOutboxDispatcher.class)).isNotNull();
        assertThat(Arrays.stream(applicationContext.getBeanDefinitionNames())
                .noneMatch(name -> name.toLowerCase().contains("kafka"))).isTrue();
    }

    private PlatformEvent event() {
        return eventWithPayload("{\"messageId\":\"100\"}");
    }

    private PlatformEvent eventWithPayload(String payloadJson) {
        return new PlatformEvent(
                PlatformEventType.MAIL_MESSAGE_SENT,
                "mail_message",
                "100",
                metadata(),
                payloadJson,
                "mail-send-100"
        );
    }

    private PlatformEventMetadata metadata() {
        return new PlatformEventMetadata(
                "tenant-1",
                "7",
                "req-1",
                "trace-1",
                "mail",
                "send",
                LocalDateTime.now()
        );
    }

    private Path resolveRepoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.isDirectory(current.resolve("src/main/resources/db/migration"))) {
            current = current.getParent();
        }
        assertThat(current).isNotNull();
        return current;
    }
}
