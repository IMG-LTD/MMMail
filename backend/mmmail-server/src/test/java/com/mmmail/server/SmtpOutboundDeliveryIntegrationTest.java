package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.service.MailOutboundDeliveryGateway;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SmtpOutboundDeliveryIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final String EXTERNAL_EMAIL = "external-recipient@example.net";
    private static final String SMTP_OUTBOUND_SUBJECT = "SMTP outbound";
    private static final String SMTP_OUTBOUND_BODY = "External delivery payload";
    private static final String SMTP_CONFIG_ERROR_MESSAGE = "SMTP outbound configuration is incomplete";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MailOutboundDeliveryGateway mailOutboundDeliveryGateway;

    @BeforeEach
    void setUpGateway() {
        when(mailOutboundDeliveryGateway.isConfigured()).thenReturn(true);
        when(mailOutboundDeliveryGateway.configurationMessage()).thenReturn(null);
        when(mailOutboundDeliveryGateway.send(any())).thenReturn(
                new MailOutboundDeliveryGateway.MailOutboundDeliveryResult(true, "SMTP outbound delivered")
        );
    }

    @Test
    void externalRecipientShouldDispatchThroughSmtpGateway() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderEmail = "smtp-sender-" + suffix + "@mmmail.local";
        String senderToken = register(senderEmail, "SMTP Sender");
        disableUndoSend(senderToken);

        mockMvc.perform(get("/api/v1/mails/e2ee-recipient-status")
                        .header("Authorization", "Bearer " + senderToken)
                        .param("toEmail", EXTERNAL_EMAIL)
                        .param("fromEmail", senderEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deliverable").value(true))
                .andExpect(jsonPath("$.data.encryptionReady").value(false))
                .andExpect(jsonPath("$.data.readiness").value("NOT_READY"));

        mockMvc.perform(buildExternalSendRequest(senderToken, senderEmail, suffix))
                .andExpect(status().isOk());

        ArgumentCaptor<MailOutboundDeliveryGateway.MailOutboundRequest> requestCaptor = ArgumentCaptor.forClass(
                MailOutboundDeliveryGateway.MailOutboundRequest.class
        );
        verify(mailOutboundDeliveryGateway, times(1)).send(requestCaptor.capture());
        assertThat(requestCaptor.getValue().fromEmail()).isEqualTo(senderEmail);
        assertThat(requestCaptor.getValue().toEmail()).isEqualTo(EXTERNAL_EMAIL);
        assertThat(requestCaptor.getValue().subject()).isEqualTo(SMTP_OUTBOUND_SUBJECT);
        assertThat(requestCaptor.getValue().body()).isEqualTo(SMTP_OUTBOUND_BODY);

        mockMvc.perform(get("/api/v1/mails/outbox")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        MvcResult sentResult = mockMvc.perform(get("/api/v1/mails/sent")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andReturn();
        assertThat(readJson(sentResult).at("/data/items/0/peerEmail").asText()).isEqualTo(EXTERNAL_EMAIL);
    }

    @Test
    void externalRecipientShouldFailExplicitlyWhenSmtpOutboundIsNotConfigured() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderEmail = "smtp-misconfigured-" + suffix + "@mmmail.local";
        String senderToken = register(senderEmail, "SMTP Sender");
        disableUndoSend(senderToken);
        when(mailOutboundDeliveryGateway.isConfigured()).thenReturn(false);
        when(mailOutboundDeliveryGateway.configurationMessage()).thenReturn(SMTP_CONFIG_ERROR_MESSAGE);

        mockMvc.perform(buildExternalSendRequest(senderToken, senderEmail, suffix))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(90000))
                .andExpect(jsonPath("$.message").value(SMTP_CONFIG_ERROR_MESSAGE));

        verify(mailOutboundDeliveryGateway, never()).send(any());
        assertFolderTotal(senderToken, "/api/v1/mails/outbox", 0);
        assertFolderTotal(senderToken, "/api/v1/mails/sent", 0);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder buildExternalSendRequest(
            String token,
            String senderEmail,
            String suffix
    ) {
        return post("/api/v1/mails/send")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "fromEmail": "%s",
                          "toEmail": "%s",
                          "subject": "%s",
                          "body": "%s",
                          "labels": [],
                          "idempotencyKey": "smtp-outbound-%s"
                        }
                        """.formatted(senderEmail, EXTERNAL_EMAIL, SMTP_OUTBOUND_SUBJECT, SMTP_OUTBOUND_BODY, suffix));
    }

    private void assertFolderTotal(String token, String path, int total) throws Exception {
        mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(total));
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
