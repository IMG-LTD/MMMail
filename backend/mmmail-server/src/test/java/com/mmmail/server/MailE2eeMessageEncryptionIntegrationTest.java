package com.mmmail.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MailE2eeMessageEncryptionIntegrationTest {

    private static final String ENCRYPTED_BODY = "-----BEGIN PGP MESSAGE-----MMMAIL-E2EE-----END PGP MESSAGE-----";
    private static final String SENDER_FINGERPRINT = "111122223333444455556666777788889999AAAA";
    private static final String RECEIVER_FINGERPRINT = "AAAABBBBCCCCDDDDEEEEFFFF0000111122223333";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void encryptedSendShouldPersistMetadataForSenderAndReceiver() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderEmail = "mail-e2ee-message-sender-%s@mmmail.local".formatted(suffix);
        String receiverEmail = "mail-e2ee-message-receiver-%s@mmmail.local".formatted(suffix);

        String senderToken = register(senderEmail);
        String receiverToken = register(receiverEmail);
        setUndoSendSeconds(senderToken, 0);
        saveKeyProfile(senderToken, SENDER_FINGERPRINT);
        saveKeyProfile(receiverToken, RECEIVER_FINGERPRINT);

        mockMvc.perform(post("/api/v1/mails/send")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toEmail": "%s",
                                  "subject": "Encrypted hello",
                                  "idempotencyKey": "mail-e2ee-send-%s",
                                  "labels": [],
                                  "e2ee": {
                                    "encryptedBody": "%s",
                                    "algorithm": "openpgp",
                                    "recipientFingerprints": ["%s", "%s"]
                                  }
                                }
                                """.formatted(receiverEmail, suffix, ENCRYPTED_BODY, RECEIVER_FINGERPRINT, SENDER_FINGERPRINT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String senderMailId = firstMailId(senderToken, "/api/v1/mails/sent");
        String receiverMailId = firstMailId(receiverToken, "/api/v1/mails/inbox");

        mockMvc.perform(get("/api/v1/mails/sent")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].preview").value("Mail E2EE encrypted body"));

        assertEncryptedDetail(senderToken, senderMailId);
        assertEncryptedDetail(receiverToken, receiverMailId);
    }

    @Test
    void encryptedSendShouldRejectNotReadyRecipient() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderEmail = "mail-e2ee-message-sender-%s@mmmail.local".formatted(suffix);
        String receiverEmail = "mail-e2ee-message-plain-%s@mmmail.local".formatted(suffix);

        String senderToken = register(senderEmail);
        register(receiverEmail);
        saveKeyProfile(senderToken, SENDER_FINGERPRINT);

        mockMvc.perform(post("/api/v1/mails/send")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toEmail": "%s",
                                  "subject": "Encrypted hello",
                                  "idempotencyKey": "mail-e2ee-send-fail-%s",
                                  "labels": [],
                                  "e2ee": {
                                    "encryptedBody": "%s",
                                    "algorithm": "openpgp",
                                    "recipientFingerprints": ["%s"]
                                  }
                                }
                                """.formatted(receiverEmail, suffix, ENCRYPTED_BODY, SENDER_FINGERPRINT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    private void assertEncryptedDetail(String token, String mailId) throws Exception {
        mockMvc.perform(get("/api/v1/mails/" + mailId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.body").value(ENCRYPTED_BODY))
                .andExpect(jsonPath("$.data.e2ee.enabled").value(true))
                .andExpect(jsonPath("$.data.e2ee.algorithm").value("openpgp"))
                .andExpect(jsonPath("$.data.e2ee.recipientFingerprints[0]").exists());
    }

    private String firstMailId(String token, String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("items")
                .get(0)
                .path("id")
                .asText();
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password@123",
                                  "displayName": "Mail E2EE Message"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
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

    private void setUndoSendSeconds(String token, int undoSeconds) throws Exception {
        mockMvc.perform(put("/api/v1/settings/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Mail E2EE Message",
                                  "signature": "",
                                  "timezone": "UTC",
                                  "autoSaveSeconds": 15,
                                  "undoSendSeconds": %d,
                                  "driveVersionRetentionCount": 50,
                                  "driveVersionRetentionDays": 365
                                }
                                """.formatted(undoSeconds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
