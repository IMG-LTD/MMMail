package com.mmmail.server.commercial;

import com.mmmail.server.observability.RuntimeTraceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BillingWebhookService {

    private final BillingProviderRegistry registry;
    private final BillingWebhookVerifier verifier;
    private final SubscriptionStateMachine stateMachine;
    private final RuntimeTraceService runtimeTraceService;
    private final String webhookSecret;

    public BillingWebhookService(
            BillingProviderRegistry registry,
            BillingWebhookVerifier verifier,
            SubscriptionStateMachine stateMachine,
            RuntimeTraceService runtimeTraceService,
            @Value("${mmmail.billing.webhook.secret:}") String webhookSecret
    ) {
        this.registry = registry;
        this.verifier = verifier;
        this.stateMachine = stateMachine;
        this.runtimeTraceService = runtimeTraceService;
        this.webhookSecret = webhookSecret;
    }

    public BillingWebhookApplyResult process(BillingWebhookEvent event, String signatureHeader) {
        return runtimeTraceService.observe("mmmail.billing.webhook", Map.of(
                "component", "billing",
                "provider", BillingProviderType.WEBHOOK.wireValue(),
                "status", event.status().name()
        ), () -> {
            BillingProvider provider = registry.require(BillingProviderType.WEBHOOK);
            if (!provider.supportsPaidState()) {
                throw new IllegalStateException("Webhook billing provider cannot apply paid state");
            }
            verifier.verify(event, signatureHeader, webhookSecret);
            return stateMachine.apply(event);
        });
    }
}
