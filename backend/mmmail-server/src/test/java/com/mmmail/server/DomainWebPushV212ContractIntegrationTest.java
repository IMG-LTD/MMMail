package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.service.DomainDnsLookupService;
import com.mmmail.server.service.WebPushDeliveryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DomainWebPushV212ContractIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final String ORG_HEADER = "X-Org-Id";
    private static final String ENDPOINT = "https://push.example.test/subscriptions/v212";
    private static final String P256DH = "BOr7vJH3xpP9Jm7hM7W9l0lmZg0b4YdOq0sVQxv5V4w";
    private static final String AUTH = "f3dJ6l7Qf8k2m1z9";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private DomainDnsLookupService domainDnsLookupService;
    @MockBean
    private WebPushDeliveryGateway webPushDeliveryGateway;

    @BeforeEach
    void setUp() {
        when(webPushDeliveryGateway.isConfigured()).thenReturn(true);
        when(webPushDeliveryGateway.publicKey()).thenReturn("BG6kxmQZCeU6xxlW4THvdTZLKmnxMR9GFzLEkNrk5-dkwl28YnBMDaqM3zNYnkcqojxB_z1ZUEZNk-y7qrsC5Ug");
        when(webPushDeliveryGateway.send(any())).thenReturn(
                new WebPushDeliveryGateway.WebPushDeliveryResult(true, false, "HTTP/1.1 201 Created")
        );
    }

    @Test
    void domainControllerShouldExposeCurrentOrgDnsLifecycle() throws Exception {
        UserOrg userOrg = createUserOrg("v212-domain");
        String domainName = "v212-" + userOrg.suffix() + ".example.com";
        MvcResult createdResult = mockMvc.perform(post("/api/v1/domains")
                        .header("Authorization", "Bearer " + userOrg.token())
                        .header(ORG_HEADER, userOrg.orgId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain\":\"" + domainName + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.domain").value(domainName))
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"))
                .andReturn();
        JsonNode created = readJson(createdResult).path("data");
        String domainId = created.path("id").asText();
        String token = created.path("verificationToken").asText();

        mockMvc.perform(get("/api/v1/domains")
                        .header("Authorization", "Bearer " + userOrg.token())
                        .header(ORG_HEADER, userOrg.orgId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(domainId));
        mockMvc.perform(get("/api/v1/domains/" + domainId + "/dns-records")
                        .header("Authorization", "Bearer " + userOrg.token())
                        .header(ORG_HEADER, userOrg.orgId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].type").value("TXT"))
                .andExpect(jsonPath("$.data.records[0].expected").value("mmmail-verify=" + token));

        when(domainDnsLookupService.resolveTxt(eq("_mmmail-verify." + domainName))).thenReturn(List.of("mmmail-verify=" + token));
        when(domainDnsLookupService.resolveTxt(eq(domainName))).thenReturn(List.of("v=spf1 include:_spf.mmmail.com ~all"));
        when(domainDnsLookupService.resolveTxt(eq("_dmarc." + domainName))).thenReturn(List.of("v=DMARC1; p=quarantine; rua=mailto:dmarc@" + domainName));
        when(domainDnsLookupService.resolveCname(eq("mm._domainkey." + domainName))).thenReturn(List.of("mm.dkim.mmmail.com"));
        when(domainDnsLookupService.resolveMx(eq(domainName))).thenReturn(List.of("10 inbound.mmmail.com"));

        mockMvc.perform(get("/api/v1/domains/" + domainId + "/diagnostics")
                        .header("Authorization", "Bearer " + userOrg.token())
                        .header(ORG_HEADER, userOrg.orgId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));
        mockMvc.perform(post("/api/v1/domains/" + domainId + "/verify")
                        .header("Authorization", "Bearer " + userOrg.token())
                        .header(ORG_HEADER, userOrg.orgId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
    }

    @Test
    void webPushControllerShouldExposeVapidSubscriptionsAndTestDelivery() throws Exception {
        String token = register("v212-webpush-" + System.nanoTime() + "@mmmail.local", "V212 WebPush");

        mockMvc.perform(get("/api/v1/web-push/vapid-public-key")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.publicKey").isNotEmpty());
        mockMvc.perform(get("/api/v1/web-push/subscriptions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        MvcResult subscriptionResult = mockMvc.perform(post("/api/v1/web-push/subscriptions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endpoint": "%s",
                                  "keys": { "p256dh": "%s", "auth": "%s" },
                                  "ua": "Vitest Browser",
                                  "label": "work laptop"
                                }
                                """.formatted(ENDPOINT, P256DH, AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subscriptionId").isNumber())
                .andExpect(jsonPath("$.data.label").value("work laptop"))
                .andReturn();
        long subscriptionId = readJson(subscriptionResult).at("/data/subscriptionId").asLong();

        mockMvc.perform(get("/api/v1/web-push/subscriptions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].subscriptionId").value(subscriptionId));
        mockMvc.perform(post("/api/v1/web-push/test")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subscriptionId\":" + subscriptionId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deliveryId").isNotEmpty());
        verify(webPushDeliveryGateway).send(any());

        mockMvc.perform(delete("/api/v1/web-push/subscriptions/" + subscriptionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    private UserOrg createUserOrg(String prefix) throws Exception {
        String suffix = prefix + "-" + System.nanoTime();
        String token = register(suffix + "@mmmail.local", "V212 Owner");
        MvcResult result = mockMvc.perform(post("/api/v1/orgs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"V212 Org " + suffix + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return new UserOrg(suffix, token, readJson(result).at("/data/id").asText());
    }

    private String register(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "%s"
                                }
                                """.formatted(email, PASSWORD, displayName)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.path("code").asInt()).isZero();
        return json;
    }

    private record UserOrg(String suffix, String token, String orgId) {
    }
}
