package com.mmmail.server.commercial;

import com.mmmail.server.observability.RuntimeTraceService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLicenseStateRepository implements LicenseStateRepository {

    private static final String UPSERT_SQL = """
            insert into license_state (org_id, claims_json, status, synced_at, expires_at)
            values (?, ?, ?, ?, ?)
            on duplicate key update
                claims_json = values(claims_json),
                status = values(status),
                synced_at = values(synced_at),
                expires_at = values(expires_at),
                updated_at = current_timestamp
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RuntimeTraceService runtimeTraceService;

    public JdbcLicenseStateRepository(JdbcTemplate jdbcTemplate, RuntimeTraceService runtimeTraceService) {
        this.jdbcTemplate = jdbcTemplate;
        this.runtimeTraceService = runtimeTraceService;
    }

    @Override
    public void save(LicenseState state) {
        runtimeTraceService.observeVoid("mmmail.db.operation", Map.of(
                "component", "db",
                "table", "license_state",
                "operation", "upsert"
        ), () -> jdbcTemplate.update(
                UPSERT_SQL,
                state.orgId(),
                state.claimsJson(),
                state.status().name(),
                timestamp(state.syncedAt()),
                timestamp(state.expiresAt())
        ));
    }

    @Override
    public Optional<LicenseState> findByOrgId(long orgId) {
        List<LicenseState> states = runtimeTraceService.observe("mmmail.db.operation", Map.of(
                "component", "db",
                "table", "license_state",
                "operation", "select"
        ), () -> jdbcTemplate.query(
                "select org_id, claims_json, status, synced_at, expires_at from license_state where org_id = ?",
                (rs, rowNum) -> new LicenseState(
                        rs.getLong("org_id"),
                        rs.getString("claims_json"),
                        LicenseStatus.valueOf(rs.getString("status")),
                        instant(rs.getTimestamp("synced_at")),
                        instant(rs.getTimestamp("expires_at"))
                ),
                orgId
        ));
        return states.stream().findFirst();
    }

    private static Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp.toInstant();
    }
}
