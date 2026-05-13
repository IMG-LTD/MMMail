package com.mmmail.platform.access;

public enum AccessEntitlement {
    COMMUNITY("community"),
    PREMIUM("premium"),
    HOSTED("hosted"),
    ENTERPRISE_GOVERNANCE("enterprise-governance");

    private final String contractValue;

    AccessEntitlement(String contractValue) {
        this.contractValue = contractValue;
    }

    public String contractValue() {
        return contractValue;
    }

    public static AccessEntitlement fromContractValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Access entitlement is required");
        }
        String normalized = value.trim();
        for (AccessEntitlement entitlement : values()) {
            if (entitlement.contractValue.equals(normalized)) {
                return entitlement;
            }
        }
        throw new IllegalArgumentException("Unknown access entitlement: " + value);
    }
}
