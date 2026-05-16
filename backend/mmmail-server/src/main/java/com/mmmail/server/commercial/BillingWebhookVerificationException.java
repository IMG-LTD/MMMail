package com.mmmail.server.commercial;

public class BillingWebhookVerificationException extends RuntimeException {

    private final BillingWebhookVerificationFailureReason reason;

    public BillingWebhookVerificationException(BillingWebhookVerificationFailureReason reason) {
        super("Billing webhook verification failed: " + reason);
        this.reason = reason;
    }

    public BillingWebhookVerificationFailureReason reason() {
        return reason;
    }
}
