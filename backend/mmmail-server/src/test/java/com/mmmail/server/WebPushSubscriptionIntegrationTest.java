package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.service.WebPushDeliveryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebPushSubscriptionIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final String ENDPOINT = "https://push.example.test/subscriptions/abc123";
    private static final String P256DH = "BOr7vJH3xpP9Jm7hM7W9l0lmZg0b4YdOq0sVQxv5V4w";
    private static final String AUTH = "f3dJ6l7Qf8k2m1z9";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WebPushDeliveryGateway webPushDeliveryGateway;

    @BeforeEach
    void setUpGateway() {
        when(webPushDeliveryGateway.isConfigured()).thenReturn(true);
        when(webPushDeliveryGateway.publicKey()).thenReturn(
                "BG6kxmQZCeU6xxlW4THvdTZLKmnxMR9GFzLEkNrk5-dkwl28YnBMDaqM3zNYnkcqojxB_z1ZUEZNk-y7qrsC5Ug"
        );
        when(webPushDeliveryGateway.configurationMessage()).thenReturn(null);
        when(webPushDeliveryGateway.send(any())).thenReturn(
                new WebPushDeliveryGateway.WebPushDeliveryResult(true, false, "HTTP/1.1 201 Created")
        );
    }

    @Test
    void webPushSubscriptionCrudAndMailDispatchShouldWork() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderEmail = "webpush-sender-" + suffix + "@mmmail.local";
        String recipientEmail = "webpush-recipient-" + suffix + "@mmmail.local";
        String senderToken = register(senderEmail, "Sender");
        String recipientToken = register(recipientEmail, "Recipient");
        disableUndoSend(senderToken);

        mockMvc.perform(get("/api/v1/suite/web-push")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.deliveryScope").value("MAIL_INBOX"))
                .andExpect(jsonPath("$.data.vapidPublicKey").isNotEmpty());

        mockMvc.perform(post("/api/v1/suite/web-push/subscriptions")
                        .header("Authorization", "Bearer " + recipientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endpoint": "%s",
                                  "p256dh": "%s",
                                  "auth": "%s",
                                  "contentEncoding": "aes128gcm",
                                  "userAgent": "Vitest Browser"
                                }
                                """.formatted(ENDPOINT, P256DH, AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subscriptionId").isNumber())
                .andExpect(jsonPath("$.data.endpointHash").isNotEmpty());

        mockMvc.perform(post("/api/v1/mails/send")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromEmail": "%s",
                                  "toEmail": "%s",
                                  "subject": "Web Push mail",
                                  "body": "Payload",
                                  "labels": [],
                                  "idempotencyKey": "web-push-%s"
                                }
                                """.formatted(senderEmail, recipientEmail, suffix)))
                .andExpect(status().isOk());

        MvcResult inboxResult = mockMvc.perform(get("/api/v1/mails/inbox")
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andReturn();
        String mailId = readJson(inboxResult).at("/data/items/0/id").asText();

        ArgumentCaptor<WebPushDeliveryGateway.WebPushDispatchRequest> requestCaptor = ArgumentCaptor.forClass(WebPushDeliveryGateway.WebPushDispatchRequest.class);
        verify(webPushDeliveryGateway, times(1)).send(requestCaptor.capture());
        assertThat(requestCaptor.getValue().endpoint()).isEqualTo(ENDPOINT);
        assertThat(requestCaptor.getValue().payload()).contains("\"kind\":\"mail-inbox\"");
        assertThat(requestCaptor.getValue().payload()).contains("\"mailId\":" + mailId);
        assertThat(requestCaptor.getValue().payload()).contains("\"routePath\":\"/mail/" + mailId + "\"");

        mockMvc.perform(delete("/api/v1/suite/web-push/subscriptions")
                        .header("Authorization", "Bearer " + recipientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endpoint": "%s"
                                }
                                """.formatted(ENDPOINT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
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

    private void disableUndoSend(String token) throws Exception {
        MvcResult profileResult = mockMvc.perform(get("/api/v1/settings/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode profile = readJson(profileResult).path("data");
        mockMvc.perform(put("/api/v1/settings/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "%s",
                                  "signature": %s,
                                  "timezone": "%s",
                                  "preferredLocale": "%s",
                                  "mailAddressMode": "%s",
                                  "autoSaveSeconds": %s,
                                  "undoSendSeconds": 0,
                                  "driveVersionRetentionCount": %s,
                                  "driveVersionRetentionDays": %s
                                }
                                """.formatted(
                                profile.path("displayName").asText(),
                                jsonStringOrNull(profile.get("signature")),
                                profile.path("timezone").asText(),
                                profile.path("preferredLocale").asText(),
                                profile.path("mailAddressMode").asText(),
                                profile.path("autoSaveSeconds").asInt(),
                                profile.path("driveVersionRetentionCount").asInt(),
                                profile.path("driveVersionRetentionDays").asInt()
                        )))
                .andExpect(status().isOk());
    }

    private String jsonStringOrNull(JsonNode value) throws Exception {
        if (value == null || value.isNull()) {
            return "null";
        }
        return objectMapper.writeValueAsString(value.asText());
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.at("/code").asInt()).isZero();
        return json;
    }
}
