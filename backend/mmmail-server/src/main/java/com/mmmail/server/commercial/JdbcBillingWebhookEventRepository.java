package com.mmmail.server.commercial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.observability.RuntimeTraceService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBillingWebhookEventRepository implements BillingWebhookEventRepository {

    private static final String INSERT_SQL = """
            insert ignore into billing_webhook_event
                (event_id, provider, org_id, plan, status, occurred_at, processed_at, signature_version, payload_json)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RuntimeTraceService runtimeTraceService;

    public JdbcBillingWebhookEventRepository(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            RuntimeTraceService runtimeTraceService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.runtimeTraceService = runtimeTraceService;
    }

    @Override
    public boolean markProcessed(BillingWebhookEvent event, Instant processedAt) {
        int rows = runtimeTraceService.observe("mmmail.db.operation", Map.of(
                "component", "db",
                "table", "billing_webhook_event",
                "operation", "insert"
        ), () -> jdbcTemplate.update(
                    INSERT_SQL,
                    event.eventId(),
                    BillingProviderType.WEBHOOK.wireValue(),
                    event.orgId(),
                    event.plan().name(),
                    event.status().name(),
                    timestamp(event.occurredAt()),
                    timestamp(processedAt),
                    event.signatureVersion(),
                    payloadJson(event)
            ));
        return rows > 0;
    }

    private String payloadJson(BillingWebhookEvent event) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "eventId", event.eventId(),
                    "orgId", event.orgId(),
                    "plan", event.plan().name(),
                    "status", event.status().name(),
                    "occurredAt", event.occurredAt().toString(),
                    "signatureVersion", event.signatureVersion()
            ));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize billing webhook event", ex);
        }
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }
}
