package com.mmmail.server.commercial;

import java.util.Arrays;

public enum FeatureCode {
    LICENSE_MANAGEMENT("license.management", Edition.PRO),
    BILLING_ADMIN("billing.admin", Edition.PRO),
    BILLING_WEBHOOK("billing.webhook", Edition.PRO),
    OIDC_SSO("oidc.sso", Edition.BUSINESS),
    AUDIT_EXPORT("audit.export", Edition.BUSINESS),
    DSR_REQUESTS("dsr.requests", Edition.BUSINESS);

    private final String code;
    private final Edition requiredEdition;

    FeatureCode(String code, Edition requiredEdition) {
        this.code = code;
        this.requiredEdition = requiredEdition;
    }

    public String code() {
        return code;
    }

    public Edition requiredEdition() {
        return requiredEdition;
    }

    public static FeatureCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(feature -> feature.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown feature code: " + code));
    }
}
