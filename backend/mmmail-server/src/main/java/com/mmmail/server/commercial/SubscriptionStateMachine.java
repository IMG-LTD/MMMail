package com.mmmail.server.commercial;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionStateMachine {

    private final BillingWebhookEventRepository eventRepository;
    private final SubscriptionStateRepository stateRepository;
    private final Clock clock;

    @Autowired
    public SubscriptionStateMachine(
            BillingWebhookEventRepository eventRepository,
            SubscriptionStateRepository stateRepository
    ) {
        this(eventRepository, stateRepository, Clock.systemUTC());
    }

    public SubscriptionStateMachine(
            BillingWebhookEventRepository eventRepository,
            SubscriptionStateRepository stateRepository,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.stateRepository = stateRepository;
        this.clock = clock;
    }

    public BillingWebhookApplyResult apply(BillingWebhookEvent event) {
        Instant processedAt = clock.instant();
        if (!eventRepository.markProcessed(event, processedAt)) {
            return BillingWebhookApplyResult.DUPLICATE;
        }
        stateRepository.save(new SubscriptionState(
                event.orgId(),
                event.plan(),
                event.status(),
                BillingProviderType.WEBHOOK,
                processedAt
        ));
        return BillingWebhookApplyResult.APPLIED;
    }
}
