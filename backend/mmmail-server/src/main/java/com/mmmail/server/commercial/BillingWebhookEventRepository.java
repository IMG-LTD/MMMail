package com.mmmail.server.commercial;

import java.time.Instant;

public interface BillingWebhookEventRepository {

    boolean markProcessed(BillingWebhookEvent event, Instant processedAt);
}
