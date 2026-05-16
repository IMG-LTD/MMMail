package com.mmmail.server.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.Map;

public final class BusinessEventMetrics {

    private static final Map<String, BusinessEvent> AUDIT_EVENT_MAP = Map.ofEntries(
            Map.entry("WALLET_TX_BROADCAST", new BusinessEvent("wallet", "wallet.tx.broadcast")),
            Map.entry("WALLET_TX_CONFIRM", new BusinessEvent("wallet", "wallet.tx.confirm")),
            Map.entry("WALLET_TX_FAIL", new BusinessEvent("wallet", "wallet.tx.fail")),
            Map.entry("MEET_ROOM_CREATE", new BusinessEvent("meet", "meet.room.created")),
            Map.entry("MEET_GUEST_REQUEST_APPROVE", new BusinessEvent("meet", "meet.guest.approved")),
            Map.entry("meet.host.transfer", new BusinessEvent("meet", "meet.host.transfer")),
            Map.entry("COMMUNITY_POST_CREATE", new BusinessEvent("community", "community.post.published")),
            Map.entry("COMMUNITY_REPORT_CREATE", new BusinessEvent("community", "community.report.opened")),
            Map.entry("V21_COLLABORATION_PROJECT_CREATE", new BusinessEvent("collab", "collab.session.start")),
            Map.entry("V21_COLLABORATION_TASK_UPDATE", new BusinessEvent("collab", "collab.update.applied")),
            Map.entry("auth.login.high_risk", new BusinessEvent("security", "security.event.high")),
            Map.entry("LOGIN_BLOCKED", new BusinessEvent("security", "security.account.locked"))
    );

    private BusinessEventMetrics() {
    }

    public static void recordFromAudit(MeterRegistry meterRegistry, String eventType) {
        BusinessEvent event = AUDIT_EVENT_MAP.get(AuditEventRegistry.canonicalType(eventType));
        if (event == null) {
            return;
        }
        record(meterRegistry, event.module(), event.event());
    }

    public static void record(MeterRegistry meterRegistry, String module, String event) {
        Counter.builder(module + "_business_event_total")
                .tag("event", event)
                .register(meterRegistry)
                .increment();
    }

    private record BusinessEvent(String module, String event) {
    }
}
