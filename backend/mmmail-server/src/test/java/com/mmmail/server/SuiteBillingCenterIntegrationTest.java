package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SuiteBillingCenterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void billingCenterShouldTrackPaymentMethodsAndDefaultSelection() throws Exception {
        SessionSeed owner = register("v122-billing-methods");

        getData(authorized(post("/api/v1/suite/billing/payment-methods"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "methodType": "CARD",
                          "displayLabel": "Visa •••• 4242",
                          "brand": "VISA",
                          "lastFour": "4242",
                          "expiresAt": "2028-12",
                          "makeDefault": true
                        }
                        """));

        JsonNode center = getData(authorized(post("/api/v1/suite/billing/payment-methods"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "methodType": "PAYPAL",
                          "displayLabel": "PayPal workspace@mmmail.local",
                          "makeDefault": true
                        }
                        """));

        assertThat(center.path("paymentMethods")).hasSize(2);
        assertThat(center.path("subscriptionSummary").path("defaultPaymentMethodLabel").asText())
                .isEqualTo("PayPal workspace@mmmail.local");
        assertThat(findDefaultMethod(center.path("paymentMethods")).path("methodType").asText()).isEqualTo("PAYPAL");
    }

    @Test
    void applyLatestDraftShouldCreatePendingInvoiceAndLifecycleSummary() throws Exception {
        SessionSeed owner = register("v122-billing-action");

        getData(authorized(post("/api/v1/suite/billing/payment-methods"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "methodType": "CARD",
                          "displayLabel": "Visa •••• 4242",
                          "brand": "VISA",
                          "lastFour": "4242",
                          "expiresAt": "2028-12",
                          "makeDefault": true
                        }
                        """));

        mockMvc.perform(authorized(post("/api/v1/suite/billing/checkout-draft"), owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "offerCode": "PASS_PLUS",
                                  "billingCycle": "ANNUAL",
                                  "seatCount": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        JsonNode center = getData(authorized(post("/api/v1/suite/billing/subscription-actions"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "actionCode": "APPLY_LATEST_DRAFT"
                        }
                        """));

        assertThat(center.path("subscriptionSummary").path("pendingActionCode").asText())
                .isEqualTo("APPLY_LATEST_DRAFT");
        assertThat(center.path("subscriptionSummary").path("pendingOfferCode").asText()).isEqualTo("PASS_PLUS");
        assertThat(center.path("invoices")).hasSize(1);
        assertThat(center.path("invoices").get(0).path("invoiceStatus").asText()).isEqualTo("PENDING");
        assertThat(center.path("invoices").get(0).path("offerCode").asText()).isEqualTo("PASS_PLUS");
    }

    @Test
    void paidSubscriptionShouldSupportAutoRenewToggle() throws Exception {
        SessionSeed owner = register("v122-auto-renew");

        mockMvc.perform(authorized(post("/api/v1/suite/subscription/change"), owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planCode": "UNLIMITED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        JsonNode disabled = getData(authorized(post("/api/v1/suite/billing/subscription-actions"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "actionCode": "CANCEL_AUTO_RENEW"
                        }
                        """));
        assertThat(disabled.path("subscriptionSummary").path("autoRenew").asBoolean()).isFalse();

        JsonNode resumed = getData(authorized(post("/api/v1/suite/billing/subscription-actions"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "actionCode": "RESUME_AUTO_RENEW"
                        }
                        """));
        assertThat(resumed.path("subscriptionSummary").path("autoRenew").asBoolean()).isTrue();
    }

    private JsonNode findDefaultMethod(JsonNode paymentMethods) {
        for (JsonNode item : paymentMethods) {
            if (item.path("defaultMethod").asBoolean()) {
                return item;
            }
        }
        throw new AssertionError("Default payment method not found");
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private JsonNode getData(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private SessionSeed register(String prefix) throws Exception {
        String email = prefix + "-" + System.nanoTime() + "@mmmail.local";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password@123",
                                  "displayName": "Suite Billing Center"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return new SessionSeed(data.path("accessToken").asText(), email);
    }

    private record SessionSeed(String token, String email) {
    }
}
