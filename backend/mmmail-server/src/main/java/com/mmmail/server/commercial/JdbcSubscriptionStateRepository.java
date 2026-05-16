package com.mmmail.server.commercial;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSubscriptionStateRepository implements SubscriptionStateRepository {

    private static final String UPSERT_SQL = """
            insert into org_subscription_state (org_id, plan, status, provider, updated_at)
            values (?, ?, ?, ?, ?)
            on duplicate key update
                plan = values(plan),
                status = values(status),
                provider = values(provider),
                updated_at = values(updated_at)
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcSubscriptionStateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(SubscriptionState state) {
        jdbcTemplate.update(
                UPSERT_SQL,
                state.orgId(),
                state.plan().name(),
                state.status().name(),
                state.provider().wireValue(),
                timestamp(state.updatedAt())
        );
    }

    @Override
    public Optional<SubscriptionState> findByOrgId(long orgId) {
        List<SubscriptionState> states = jdbcTemplate.query(
                "select org_id, plan, status, provider, updated_at from org_subscription_state where org_id = ?",
                (rs, rowNum) -> new SubscriptionState(
                        rs.getLong("org_id"),
                        SubscriptionPlan.valueOf(rs.getString("plan")),
                        SubscriptionStatus.valueOf(rs.getString("status")),
                        BillingProviderType.fromWireValue(rs.getString("provider")),
                        instant(rs.getTimestamp("updated_at"))
                ),
                orgId
        );
        return states.stream().findFirst();
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp.toInstant();
    }
}
