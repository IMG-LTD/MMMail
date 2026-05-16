package com.mmmail.server.commercial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.model.vo.CommercialLicenseStatusVo;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommercialLicenseStatusMapper {

    private static final String DEFAULT_EDITION = "FREE";
    private static final String EXTERNAL_BILLING_STATUS = "LICENSE_KEY";
    private static final String NOOP_ACTION = "none";
    private static final String UPLOAD_ACTION = "upload-license";

    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public CommercialLicenseStatusMapper(ObjectMapper objectMapper) {
        this(objectMapper, Clock.systemUTC());
    }

    public CommercialLicenseStatusMapper(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public CommercialLicenseStatusVo missing(long orgId) {
        return new CommercialLicenseStatusVo(
                String.valueOf(orgId),
                CommercialLicenseStatusVo.State.MISSING,
                DEFAULT_EDITION,
                List.of(),
                EXTERNAL_BILLING_STATUS,
                null,
                null,
                UPLOAD_ACTION,
                "license is not uploaded"
        );
    }

    public CommercialLicenseStatusVo fromState(LicenseState state) {
        JsonNode claims = readClaims(state.claimsJson());
        CommercialLicenseStatusVo.State currentState = currentState(state);
        return new CommercialLicenseStatusVo(
                String.valueOf(state.orgId()),
                currentState,
                text(claims, "edition"),
                features(claims),
                EXTERNAL_BILLING_STATUS,
                state.expiresAt(),
                state.syncedAt(),
                requiredAction(currentState),
                message(currentState)
        );
    }

    private CommercialLicenseStatusVo.State currentState(LicenseState state) {
        if (state.status() == LicenseStatus.ACTIVE && !state.expiresAt().isAfter(clock.instant())) {
            return CommercialLicenseStatusVo.State.EXPIRED;
        }
        return CommercialLicenseStatusVo.State.valueOf(state.status().name());
    }

    private JsonNode readClaims(String claimsJson) {
        try {
            JsonNode claims = objectMapper.readTree(claimsJson);
            if (!claims.isObject()) {
                throw new IllegalStateException("Stored license claims must be a JSON object");
            }
            return claims;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored license claims are invalid", ex);
        }
    }

    private List<String> features(JsonNode claims) {
        JsonNode featuresNode = claims.get("features");
        if (featuresNode == null || !featuresNode.isArray()) {
            throw new IllegalStateException("Stored license features are invalid");
        }
        List<String> values = new ArrayList<>();
        featuresNode.forEach(node -> values.add(node.asText()));
        return values.stream().sorted(Comparator.naturalOrder()).toList();
    }

    private String text(JsonNode claims, String fieldName) {
        JsonNode node = claims.get(fieldName);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw new IllegalStateException("Stored license claim is invalid: " + fieldName);
        }
        return node.asText();
    }

    private String requiredAction(CommercialLicenseStatusVo.State state) {
        return state == CommercialLicenseStatusVo.State.ACTIVE ? NOOP_ACTION : UPLOAD_ACTION;
    }

    private String message(CommercialLicenseStatusVo.State state) {
        return switch (state) {
            case ACTIVE -> "license is active";
            case EXPIRED -> "license has expired";
            case INVALID -> "license is invalid";
            case MISSING -> "license is not uploaded";
        };
    }
}
