package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.commercial.BillingWebhookApplyResult;
import com.mmmail.server.commercial.BillingWebhookEvent;
import com.mmmail.server.commercial.BillingWebhookService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/billing")
public class BillingWebhookController {

    public static final String SIGNATURE_HEADER = "X-MMMail-Billing-Signature";

    private final BillingWebhookService service;

    public BillingWebhookController(BillingWebhookService service) {
        this.service = service;
    }

    @PostMapping("/webhook")
    public Result<Map<String, Object>> receiveWebhook(
            @RequestHeader(SIGNATURE_HEADER) String signature,
            @RequestBody BillingWebhookEvent event
    ) {
        BillingWebhookApplyResult result = service.process(event, signature);
        return Result.success(Map.of("result", result.name()));
    }
}
