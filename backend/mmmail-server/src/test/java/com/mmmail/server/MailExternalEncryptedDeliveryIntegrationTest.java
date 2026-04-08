package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.service.MailOutboundDeliveryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MailExternalEncryptedDeliveryIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final String SENDER_FINGERPRINT = "111122223333444455556666777788889999AAAA";
    private static final String ENCRYPTED_BODY = "-----BEGIN PGP MESSAGE-----MMMAIL-EXTERNAL-----END PGP MESSAGE-----";
    private static final byte[] ENCRYPTED_ATTACHMENT = "encrypted-attachment-payload".getBytes(StandardCharsets.UTF_8);
    private static final Pattern SECURE_LINK_PATTERN = Pattern.compile("/share/mail/([A-Za-z0-9]+)");

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
    void passwordProtectedExternalSendShouldDispatchSecureLinkAndExposePublicCiphertext() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderEmail = "mail-external-sender-" + suffix + "@mmmail.local";
        String externalEmail = "external-" + suffix + "@example.net";
        String senderToken = register(senderEmail, "External Mail Sender");
        disableUndoSend(senderToken);
        saveKeyProfile(senderToken, SENDER_FINGERPRINT);

        mockMvc.perform(sendExternalEncryptedMail(senderToken, senderEmail, externalEmail, suffix, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<MailOutboundDeliveryGateway.MailOutboundRequest> requestCaptor = ArgumentCaptor.forClass(
                MailOutboundDeliveryGateway.MailOutboundRequest.class
        );
        verify(mailOutboundDeliveryGateway, times(1)).send(requestCaptor.capture());
        MailOutboundDeliveryGateway.MailOutboundRequest outboundRequest = requestCaptor.getValue();
        assertThat(outboundRequest.subject()).isEqualTo("[MMMail Secure Mail] External secure subject");
        assertThat(outboundRequest.body()).contains("Password hint: shared out-of-band");
        assertThat(outboundRequest.body()).doesNotContain(ENCRYPTED_BODY);

        String secureToken = extractSecureToken(outboundRequest.body());
        MvcResult secureLinkResult = mockMvc.perform(get("/api/v1/public/mail/secure-links/" + secureToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.subject").value("External secure subject"))
                .andExpect(jsonPath("$.data.senderEmail").value(senderEmail))
                .andExpect(jsonPath("$.data.recipientEmail").value(externalEmail))
                .andExpect(jsonPath("$.data.bodyCiphertext").value(ENCRYPTED_BODY))
                .andExpect(jsonPath("$.data.algorithm").value("openpgp"))
                .andExpect(jsonPath("$.data.passwordHint").value("shared out-of-band"))
                .andReturn();

        JsonNode secureLink = readJson(secureLinkResult).path("data");
        assertThat(secureLink.path("expiresAt").asText()).startsWith("2026-04-21T12:00:00");

        mockMvc.perform(get("/api/v1/mails/sent")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].preview").value("Mail E2EE encrypted body"));
    }

    @Test
    void passwordProtectedExternalSendShouldRejectInternalRecipient() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderEmail = "mail-external-internal-sender-" + suffix + "@mmmail.local";
        String receiverEmail = "mail-external-internal-receiver-" + suffix + "@mmmail.local";
        String senderToken = register(senderEmail, "External Mail Sender");
        register(receiverEmail, "Internal Receiver");
        disableUndoSend(senderToken);
        saveKeyProfile(senderToken, SENDER_FINGERPRINT);

        mockMvc.perform(sendExternalEncryptedMail(senderToken, senderEmail, receiverEmail, suffix, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()))
                .andExpect(jsonPath("$.message").value("Password-protected external Mail E2EE requires SMTP outbound recipients"));

        verify(mailOutboundDeliveryGateway, never()).send(any());
    }

    @Test
    void passwordProtectedExternalDraftShouldPersistAndExposeEncryptedAttachments() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderEmail = "mail-external-draft-sender-" + suffix + "@mmmail.local";
        String externalEmail = "mail-external-draft-" + suffix + "@example.net";
        String senderToken = register(senderEmail, "External Mail Sender");
        disableUndoSend(senderToken);
        saveKeyProfile(senderToken, SENDER_FINGERPRINT);
        String draftId = saveExternalDraft(senderToken, senderEmail, externalEmail);
        String attachmentId = uploadEncryptedAttachment(senderToken, draftId);

        mockMvc.perform(get("/api/v1/mails/" + draftId)
                        .header("Authorization", "Bearer " + senderToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDraft").value(true))
                .andExpect(jsonPath("$.data.body").value(ENCRYPTED_BODY))
                .andExpect(jsonPath("$.data.e2ee.enabled").value(true))
                .andExpect(jsonPath("$.data.e2ee.externalAccess.mode").value("PASSWORD_PROTECTED"))
                .andExpect(jsonPath("$.data.e2ee.externalAccess.passwordHint").value("shared out-of-band"))
                .andExpect(jsonPath("$.data.attachments[0].id").value(attachmentId))
                .andExpect(jsonPath("$.data.attachments[0].e2ee.enabled").value(true))
                .andExpect(jsonPath("$.data.attachments[0].e2ee.algorithm").value("openpgp"));

        mockMvc.perform(sendExternalEncryptedMail(senderToken, senderEmail, externalEmail, suffix, draftId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        ArgumentCaptor<MailOutboundDeliveryGateway.MailOutboundRequest> requestCaptor = ArgumentCaptor.forClass(
                MailOutboundDeliveryGateway.MailOutboundRequest.class
        );
        verify(mailOutboundDeliveryGateway, times(1)).send(requestCaptor.capture());
        String secureToken = extractSecureToken(requestCaptor.getValue().body());

        mockMvc.perform(get("/api/v1/public/mail/secure-links/" + secureToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments[0].id").value(attachmentId))
                .andExpect(jsonPath("$.data.attachments[0].fileName").value("roadmap.pdf"))
                .andExpect(jsonPath("$.data.attachments[0].contentType").value("application/pdf"))
                .andExpect(jsonPath("$.data.attachments[0].algorithm").value("openpgp"));

        mockMvc.perform(get("/api/v1/public/mail/secure-links/" + secureToken + "/attachments/" + attachmentId + "/download"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(ENCRYPTED_ATTACHMENT));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder sendExternalEncryptedMail(
            String token,
            String senderEmail,
            String recipientEmail,
            String suffix,
            String draftId
    ) {
        String draftFragment = draftId == null ? "" : """
                          "draftId": %s,
                        """.formatted(draftId);
        return post("/api/v1/mails/send")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                        %s
                          "fromEmail": "%s",
                          "toEmail": "%s",
                          "subject": "External secure subject",
                          "labels": [],
                          "idempotencyKey": "mail-external-secure-%s",
                          "e2ee": {
                            "encryptedBody": "%s",
                            "algorithm": "openpgp",
                            "recipientFingerprints": ["%s"],
                            "externalAccess": {
                              "mode": "PASSWORD_PROTECTED",
                              "passwordHint": "shared out-of-band",
                              "expiresAt": "2026-04-21T12:00:00"
                            }
                          }
                        }
                        """.formatted(draftFragment, senderEmail, recipientEmail, suffix, ENCRYPTED_BODY, SENDER_FINGERPRINT));
    }

    private String saveExternalDraft(String token, String senderEmail, String recipientEmail) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mails/drafts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toEmail": "%s",
                                  "fromEmail": "%s",
                                  "subject": "Draft secure subject",
                                  "e2ee": {
                                    "encryptedBody": "%s",
                                    "algorithm": "openpgp",
                                    "recipientFingerprints": ["%s"],
                                    "externalAccess": {
                                      "mode": "PASSWORD_PROTECTED",
                                      "passwordHint": "shared out-of-band",
                                      "expiresAt": "2026-04-21T12:00:00"
                                    }
                                  }
                                }
                                """.formatted(recipientEmail, senderEmail, ENCRYPTED_BODY, SENDER_FINGERPRINT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readJson(result).at("/data/draftId").asText();
    }

    private String uploadEncryptedAttachment(String token, String draftId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "roadmap.pdf.pgp",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                ENCRYPTED_ATTACHMENT
        );
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(
                                "/api/v1/mails/drafts/" + draftId + "/attachments"
                        )
                        .file(file)
                        .param("fileName", "roadmap.pdf")
                        .param("contentType", "application/pdf")
                        .param("fileSize", "22")
                        .param("e2eeEnabled", "true")
                        .param("e2eeAlgorithm", "openpgp")
                        .param("e2eeRecipientFingerprintsJson", "[\"" + SENDER_FINGERPRINT + "\"]")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readJson(result).at("/data/attachment/id").asText();
    }

    private String extractSecureToken(String notificationBody) {
        Matcher matcher = SECURE_LINK_PATTERN.matcher(notificationBody);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
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

    private void saveKeyProfile(String token, String fingerprint) throws Exception {
        mockMvc.perform(put("/api/v1/settings/mail-e2ee")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "publicKeyArmored": "-----BEGIN PGP PUBLIC KEY BLOCK-----%s-----END PGP PUBLIC KEY BLOCK-----",
                                  "encryptedPrivateKeyArmored": "-----BEGIN PGP PRIVATE KEY BLOCK-----%s-----END PGP PRIVATE KEY BLOCK-----",
                                  "fingerprint": "%s",
                                  "algorithm": "curve25519Legacy",
                                  "keyCreatedAt": "2026-04-01T21:00:00"
                                }
                                """.formatted(fingerprint, fingerprint, fingerprint)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
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
