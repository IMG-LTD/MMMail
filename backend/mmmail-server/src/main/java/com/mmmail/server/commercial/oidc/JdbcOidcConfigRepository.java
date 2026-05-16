package com.mmmail.server.commercial.oidc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public class JdbcOidcConfigRepository implements OidcConfigRepository {

    private static final TypeReference<Set<String>> STRING_SET = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcOidcConfigRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public java.util.Optional<OidcClientConfig> findByOrgId(Long orgId) {
        try {
            return java.util.Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                            select org_id, enabled, issuer_uri, client_id, client_secret_ref,
                                   callback_uri, scopes_json, allowed_post_login_redirect_uris_json
                            from org_oidc_config
                            where org_id = ?
                            limit 1
                            """,
                    (rs, rowNum) -> new OidcClientConfig(
                            rs.getLong("org_id"),
                            rs.getInt("enabled") == 1,
                            rs.getString("issuer_uri"),
                            rs.getString("client_id"),
                            rs.getString("client_secret_ref"),
                            rs.getString("callback_uri"),
                            readSet(rs.getString("scopes_json")),
                            readSet(rs.getString("allowed_post_login_redirect_uris_json"))
                    ),
                    orgId
            ));
        } catch (EmptyResultDataAccessException ex) {
            return java.util.Optional.empty();
        }
    }

    @Override
    public void save(OidcClientConfig config) {
        jdbcTemplate.update(
                """
                        insert into org_oidc_config(
                          org_id, enabled, issuer_uri, client_id, client_secret_ref,
                          callback_uri, scopes_json, allowed_post_login_redirect_uris_json,
                          created_at, updated_at
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                        on duplicate key update
                          enabled = values(enabled),
                          issuer_uri = values(issuer_uri),
                          client_id = values(client_id),
                          client_secret_ref = values(client_secret_ref),
                          callback_uri = values(callback_uri),
                          scopes_json = values(scopes_json),
                          allowed_post_login_redirect_uris_json = values(allowed_post_login_redirect_uris_json),
                          updated_at = current_timestamp
                        """,
                config.orgId(),
                config.enabled() ? 1 : 0,
                config.issuerUri(),
                config.clientId(),
                config.clientSecretRef(),
                config.callbackUri(),
                writeSet(config.scopes()),
                writeSet(config.allowedPostLoginRedirectUris())
        );
    }

    private Set<String> readSet(String json) {
        try {
            return objectMapper.readValue(json, STRING_SET);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse OIDC config JSON", ex);
        }
    }

    private String writeSet(Set<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to write OIDC config JSON", ex);
        }
    }
}
