package com.mmmail.server.commercial;

import org.springframework.stereotype.Component;

@Component
public class WebhookBillingProvider implements BillingProvider {

    @Override
    public BillingProviderType type() {
        return BillingProviderType.WEBHOOK;
    }

    @Override
    public boolean supportsPaidState() {
        return true;
    }
}
