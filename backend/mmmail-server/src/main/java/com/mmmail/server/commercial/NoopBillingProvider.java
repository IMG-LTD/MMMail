package com.mmmail.server.commercial;

import org.springframework.stereotype.Component;

@Component
public class NoopBillingProvider implements BillingProvider {

    @Override
    public BillingProviderType type() {
        return BillingProviderType.NONE;
    }

    @Override
    public boolean supportsPaidState() {
        return false;
    }
}
