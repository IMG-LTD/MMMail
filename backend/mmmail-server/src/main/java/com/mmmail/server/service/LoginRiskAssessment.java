package com.mmmail.server.service;

import java.util.List;

public record LoginRiskAssessment(
        String risk,
        List<String> reasons,
        boolean secondFactorRequired,
        Long securityEventId
) {
    public static LoginRiskAssessment low() {
        return new LoginRiskAssessment("low", List.of(), false, null);
    }

    public LoginRiskAssessment withSecurityEventId(Long eventId) {
        return new LoginRiskAssessment(risk, reasons, secondFactorRequired, eventId);
    }
}
