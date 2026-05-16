package com.mmmail.server.commercial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcEditionContextResolver implements EditionContextResolver {

    private static final String WORKSPACE_EDITION_SQL = """
            select edition from org_workspace where id = ? and deleted = 0 limit 1
            """;

    private final SubscriptionStateRepository subscriptionStateRepository;
    private final LicenseStateRepository licenseStateRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public JdbcEditionContextResolver(
            SubscriptionStateRepository subscriptionStateRepository,
            LicenseStateRepository licenseStateRepository,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this(subscriptionStateRepository, licenseStateRepository, jdbcTemplate, objectMapper, Clock.systemUTC());
    }

    JdbcEditionContextResolver(
            SubscriptionStateRepository subscriptionStateRepository,
            LicenseStateRepository licenseStateRepository,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.subscriptionStateRepository = subscriptionStateRepository;
        this.licenseStateRepository = licenseStateRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public EditionContext resolve(Long orgId) {
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }
        return new EditionContext(orgId, resolveEdition(orgId));
    }

    private Edition resolveEdition(long orgId) {
        Optional<Edition> subscriptionEdition = subscriptionEdition(orgId);
        if (subscriptionEdition.isPresent()) {
            return subscriptionEdition.get();
        }
        return licenseEdition(orgId).orElseGet(() -> workspaceEdition(orgId));
    }

    private Optional<Edition> subscriptionEdition(long orgId) {
        return subscriptionStateRepository.findByOrgId(orgId)
                .map(state -> isPaidStatus(state.status()) ? planEdition(state.plan()) : Edition.FREE);
    }

    private Optional<Edition> licenseEdition(long orgId) {
        return licenseStateRepository.findByOrgId(orgId)
                .filter(state -> state.status() == LicenseStatus.ACTIVE)
                .filter(state -> state.expiresAt().isAfter(clock.instant()))
                .map(state -> parseLicenseEdition(state.claimsJson()));
    }

    private Edition workspaceEdition(long orgId) {
        List<Edition> editions = jdbcTemplate.query(
                WORKSPACE_EDITION_SQL,
                (rs, rowNum) -> Edition.valueOf(rs.getString("edition")),
                orgId
        );
        if (editions.isEmpty()) {
            throw new BizException(ErrorCode.ORG_NOT_FOUND, "Organization workspace not found: orgId=" + orgId);
        }
        return editions.getFirst();
    }

    private Edition parseLicenseEdition(String claimsJson) {
        try {
            return Edition.valueOf(objectMapper.readTree(claimsJson).path("edition").asText());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to parse license claims", ex);
        }
    }

    private boolean isPaidStatus(SubscriptionStatus status) {
        return status == SubscriptionStatus.TRIAL || status == SubscriptionStatus.ACTIVE;
    }

    private Edition planEdition(SubscriptionPlan plan) {
        return switch (plan) {
            case FREE -> Edition.FREE;
            case PRO -> Edition.PRO;
            case BUSINESS -> Edition.BUSINESS;
        };
    }
}
