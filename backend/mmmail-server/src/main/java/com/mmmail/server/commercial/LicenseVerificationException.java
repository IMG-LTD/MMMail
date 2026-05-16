package com.mmmail.server.commercial;

public class LicenseVerificationException extends RuntimeException {

    private final LicenseFailureReason reason;

    public LicenseVerificationException(LicenseFailureReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public LicenseFailureReason reason() {
        return reason;
    }
}
