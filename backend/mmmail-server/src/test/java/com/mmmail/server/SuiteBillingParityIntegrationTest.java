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
class SuiteBillingParityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void pricingOffersShouldExposeSingleProductAndSalesLedPaths() throws Exception {
        SessionSeed owner = register("v121-suite-offers");

        JsonNode offers = getData(authorized(get("/api/v1/suite/pricing/offers"), owner.token()));
        assertThat(offers).isNotEmpty();
        assertThat(findOffer(offers, "PASS_PLUS").path("checkoutMode").asText()).isEqualTo("SELF_SERVE");
        assertThat(findOffer(offers, "DRIVE_PLUS").path("priceValue").asText()).isEqualTo("$3.99");
        assertThat(findOffer(offers, "BUSINESS_SUITE").path("checkoutMode").asText()).isEqualTo("CONTACT_SALES");
    }

    @Test
    void quoteAndDraftShouldPersistLatestBillingSnapshot() throws Exception {
        SessionSeed owner = register("v121-suite-draft");

        JsonNode quote = getData(authorized(post("/api/v1/suite/billing/quote"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "offerCode": "PASS_PLUS",
                          "billingCycle": "ANNUAL",
                          "seatCount": 1
                        }
                        """));
        assertThat(quote.path("quoteStatus").asText()).isEqualTo("READY");
        assertThat(quote.path("currencyCode").asText()).isEqualTo("USD");
        assertThat(quote.path("invoiceSummary").path("discountCents").asLong()).isEqualTo(2400L);
        assertThat(quote.path("invoiceSummary").path("totalCents").asLong()).isEqualTo(3588L);

        JsonNode draft = getData(authorized(post("/api/v1/suite/billing/checkout-draft"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "offerCode": "DRIVE_PLUS",
                          "billingCycle": "ANNUAL",
                          "seatCount": 1,
                          "organizationName": "MMMail Personal",
                          "domainName": "drive.mmmail.local"
                        }
                        """));
        assertThat(draft.path("offerCode").asText()).isEqualTo("DRIVE_PLUS");
        assertThat(draft.path("invoiceSummary").path("totalCents").asLong()).isEqualTo(4788L);
        assertThat(draft.path("organizationName").asText()).isEqualTo("MMMail Personal");

        JsonNode overview = getData(authorized(get("/api/v1/suite/billing/overview"), owner.token()));
        assertThat(overview.path("activePlanCode").asText()).isEqualTo("FREE");
        assertThat(overview.path("latestDraft").path("offerCode").asText()).isEqualTo("DRIVE_PLUS");
        assertThat(overview.path("selfServeOfferCodes")).anyMatch(node -> "PASS_PLUS".equals(node.asText()));
    }

    @Test
    void businessQuoteShouldExposeContactSalesMode() throws Exception {
        SessionSeed owner = register("v121-suite-sales");

        JsonNode quote = getData(authorized(post("/api/v1/suite/billing/quote"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "offerCode": "BUSINESS_SUITE",
                          "billingCycle": "ANNUAL",
                          "seatCount": 5
                        }
                        """));
        assertThat(quote.path("quoteStatus").asText()).isEqualTo("CONTACT_SALES");
        assertThat(quote.path("invoiceSummary").isNull()).isTrue();
        assertThat(quote.path("onboardingSummary").path("organizationRequired").asBoolean()).isTrue();
        assertThat(quote.path("onboardingSummary").path("nextAction").asText()).isEqualTo("REQUEST_SALES_CONTACT");
    }

    private JsonNode findOffer(JsonNode offers, String offerCode) {
        for (JsonNode offer : offers) {
            if (offerCode.equals(offer.path("code").asText())) {
                return offer;
            }
        }
        throw new AssertionError("Offer not found: " + offerCode);
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
                                  "displayName": "Suite Billing"
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
