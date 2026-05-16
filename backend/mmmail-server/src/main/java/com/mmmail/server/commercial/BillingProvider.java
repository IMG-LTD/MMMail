package com.mmmail.server.commercial;

public interface BillingProvider {

    BillingProviderType type();

    boolean supportsPaidState();
}
