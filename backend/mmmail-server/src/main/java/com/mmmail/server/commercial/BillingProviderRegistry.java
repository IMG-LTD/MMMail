package com.mmmail.server.commercial;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BillingProviderRegistry {

    private final Map<BillingProviderType, BillingProvider> providers;

    public BillingProviderRegistry(Collection<BillingProvider> providers) {
        this.providers = index(providers);
    }

    public BillingProvider require(BillingProviderType type) {
        BillingProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalStateException("Billing provider is not registered: " + type);
        }
        return provider;
    }

    private static Map<BillingProviderType, BillingProvider> index(Collection<BillingProvider> providers) {
        EnumMap<BillingProviderType, BillingProvider> indexed = new EnumMap<>(BillingProviderType.class);
        for (BillingProvider provider : providers) {
            if (indexed.put(provider.type(), provider) != null) {
                throw new IllegalStateException("Duplicate billing provider: " + provider.type());
            }
        }
        return Map.copyOf(indexed);
    }
}
