package com.mmmail.server;

import com.mmmail.server.mapper.AuditEventMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.entity.AuditEvent;
import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.service.AuditEventRegistry;
import com.mmmail.server.service.AuditEventSpec;
import com.mmmail.server.service.AuditService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditV212ContractTest {

    private static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));

    private static final Map<String, String> REQUIRED_EVENTS = Map.ofEntries(
            Map.entry("wallet.tx.send", "high"),
            Map.entry("wallet.tx.sign", "critical"),
            Map.entry("wallet.account.recover", "critical"),
            Map.entry("meet.host.transfer", "medium"),
            Map.entry("domain.add", "high"),
            Map.entry("domain.delete", "high"),
            Map.entry("totp.entry.add", "medium"),
            Map.entry("totp.security.update", "high"),
            Map.entry("community.post.delete", "medium"),
            Map.entry("community.report.actioned", "medium"),
            Map.entry("auth.login.high_risk", "high"),
            Map.entry("billing.subscription.action", "high"),
            Map.entry("webpush.subscription.delete", "low")
    );

    @Test
    void v212AuditRegistryShouldDeclareRequiredEventsWithSeverityAndTarget() {
        Map<String, AuditEventSpec> specs = AuditEventRegistry.registeredSpecsByType();

        assertThat(specs.keySet()).containsAll(REQUIRED_EVENTS.keySet());
        REQUIRED_EVENTS.forEach((eventType, severity) -> {
            AuditEventSpec spec = specs.get(eventType);
            assertThat(spec.severity()).isEqualTo(severity);
            assertThat(spec.targetType()).isNotBlank();
        });
    }

    @Test
    void auditEventTableShouldPersistTargetAndSeverityFields() throws Exception {
        String schema = Files.readString(PROJECT_ROOT.resolve("src/main/resources/schema.sql"));
        String migration = Files.readString(PROJECT_ROOT.resolve(
                "src/main/resources/db/migration/V31__v212_audit_event_metadata.sql"
        ));

        assertThat(schema).contains(
                "target_type varchar(64)",
                "target_id varchar(128)",
                "severity varchar(16) not null default 'low'"
        );
        assertThat(migration).contains(
                "add column target_type varchar(64)",
                "add column target_id varchar(128)",
                "add column severity varchar(16) not null default 'low'",
                "idx_audit_event_type_severity_created"
        );
    }

    @Test
    void recordShouldStoreRegisteredMetadataAndIncrementAuditCounter() {
        AuditEventMapper auditEventMapper = mock(AuditEventMapper.class);
        UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);

        when(auditEventMapper.insert(any(AuditEvent.class))).thenAnswer(invocation -> {
            AuditEvent event = invocation.getArgument(0);
            event.setId(123L);
            return 1;
        });

        AuditService service = new AuditService(auditEventMapper, userAccountMapper, meterRegistry);
        AuditEventVo result = service.recordRegisteredEvent(
                7L,
                "wallet.tx.send",
                "tx_42",
                "chain=eth,amountWei=1,recipient=0xabc",
                "127.0.0.1"
        );

        org.mockito.Mockito.verify(auditEventMapper).insert(captor.capture());
        AuditEvent event = captor.getValue();
        assertThat(event.getActorId()).isEqualTo(7L);
        assertThat(event.getEventType()).isEqualTo("wallet.tx.send");
        assertThat(event.getTargetType()).isEqualTo("txId");
        assertThat(event.getTargetId()).isEqualTo("tx_42");
        assertThat(event.getSeverity()).isEqualTo("high");
        assertThat(result.targetId()).isEqualTo("tx_42");
        assertThat(result.severity()).isEqualTo("high");

        assertThat(meterRegistry.get("audit_event_total")
                .tag("type", "wallet.tx.send")
                .tag("severity", "high")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    void legacyAuditNamesShouldResolveRegisteredMetadataWithoutChangingStoredEventType() {
        AuditEventMapper auditEventMapper = mock(AuditEventMapper.class);
        UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);

        when(auditEventMapper.insert(any(AuditEvent.class))).thenAnswer(invocation -> {
            AuditEvent event = invocation.getArgument(0);
            event.setId(124L);
            return 1;
        });

        AuditService service = new AuditService(auditEventMapper, userAccountMapper, meterRegistry);
        service.record(7L, "MEET_HOST_TRANSFER", "roomId=22,targetParticipantId=99", "127.0.0.1");

        org.mockito.Mockito.verify(auditEventMapper).insert(captor.capture());
        AuditEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo("MEET_HOST_TRANSFER");
        assertThat(event.getTargetType()).isEqualTo("roomId");
        assertThat(event.getTargetId()).isEqualTo("22");
        assertThat(event.getSeverity()).isEqualTo("medium");
        assertThat(meterRegistry.get("audit_event_total")
                .tag("type", "meet.host.transfer")
                .tag("severity", "medium")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    void businessEventsShouldExposeModuleSpecificCountersFromAuditPaths() {
        AuditEventMapper auditEventMapper = mock(AuditEventMapper.class);
        UserAccountMapper userAccountMapper = mock(UserAccountMapper.class);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        when(auditEventMapper.insert(any(AuditEvent.class))).thenReturn(1);

        AuditService service = new AuditService(auditEventMapper, userAccountMapper, meterRegistry);
        service.record(7L, "WALLET_TX_CONFIRM", "transactionId=tx_42", "127.0.0.1");
        service.record(7L, "COMMUNITY_REPORT_CREATE", "reportId=rp_1", "127.0.0.1");

        assertThat(meterRegistry.get("wallet_business_event_total")
                .tag("event", "wallet.tx.confirm")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("community_business_event_total")
                .tag("event", "community.report.opened")
                .counter()
                .count()).isEqualTo(1.0);
    }
}
