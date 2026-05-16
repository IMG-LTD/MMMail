package com.mmmail.server.commercial.oidc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class JdbcOidcStateRepository implements OidcStateRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcOidcStateRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(OidcStateRecord record) {
        jdbcTemplate.update(
                """
                        insert into oidc_auth_state(
                          state, org_id, nonce, code_verifier, callback_uri,
                          post_login_redirect_uri, expires_at, created_at
                        )
                        values (?, ?, ?, ?, ?, ?, ?, current_timestamp)
                        """,
                record.state(),
                record.orgId(),
                record.nonce(),
                record.codeVerifier(),
                record.callbackUri(),
                record.postLoginRedirectUri(),
                Timestamp.from(record.expiresAt())
        );
    }

    @Override
    public Optional<OidcStateRecord> findActive(String state, Instant now) {
        return jdbcTemplate.query(
                """
                        select state, org_id, nonce, code_verifier, callback_uri,
                               post_login_redirect_uri, expires_at, consumed_at
                        from oidc_auth_state
                        where state = ?
                          and consumed_at is null
                          and expires_at > ?
                        limit 1
                        """,
                rs -> {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    Timestamp consumedAt = rs.getTimestamp("consumed_at");
                    return Optional.of(new OidcStateRecord(
                            rs.getString("state"),
                            rs.getLong("org_id"),
                            rs.getString("nonce"),
                            rs.getString("code_verifier"),
                            rs.getString("callback_uri"),
                            rs.getString("post_login_redirect_uri"),
                            rs.getTimestamp("expires_at").toInstant(),
                            consumedAt == null ? null : consumedAt.toInstant()
                    ));
                },
                state,
                Timestamp.from(now)
        );
    }

    @Override
    public void markConsumed(String state, Instant consumedAt) {
        jdbcTemplate.update(
                "update oidc_auth_state set consumed_at = ? where state = ? and consumed_at is null",
                Timestamp.from(consumedAt),
                state
        );
    }
}
