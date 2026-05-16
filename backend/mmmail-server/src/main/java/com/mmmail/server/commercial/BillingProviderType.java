package com.mmmail.server.commercial;

import java.util.Arrays;

public enum BillingProviderType {
    NONE("none"),
    WEBHOOK("webhook");

    private final String wireValue;

    BillingProviderType(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static BillingProviderType fromWireValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.wireValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown billing provider: " + value));
    }
}
