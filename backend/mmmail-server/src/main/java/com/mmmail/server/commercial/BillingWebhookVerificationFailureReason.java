package com.mmmail.server.commercial;

public enum BillingWebhookVerificationFailureReason {
    SECRET_MISSING,
    SIGNATURE_INVALID,
    TIMESTAMP_OUT_OF_WINDOW,
    VERSION_UNSUPPORTED
}
