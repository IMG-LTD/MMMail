package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MailE2eeAttachmentEncryptionIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final String FINGERPRINT = "ABCDEF0123456789ABCDEF0123456789ABCDEF01";
    private static final byte[] CIPHERTEXT = "encrypted-attachment-binary".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void encryptedAttachmentsShouldPersistMetadataAndDownloadCiphertext() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderToken = register("mail-attachment-e2ee-sender-" + suffix + "@mmmail.local", "Attachment Sender");
        String receiverEmail = "mail-attachment-e2ee-receiver-" + suffix + "@mmmail.local";
        String receiverToken = register(receiverEmail, "Attachment Receiver");
        saveKeyProfile(senderToken);
        setUndoSendSeconds(senderToken, "Attachment Sender", 0);

        String draftId = saveDraft(senderToken, receiverEmail);
        String attachmentId = uploadEncryptedAttachment(senderToken, draftId);

        mockMvc.perform(get("/api/v1/mails/" + draftId)
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments[0].id").value(attachmentId))
                .andExpect(jsonPath("$.data.attachments[0].fileName").value("report.txt"))
                .andExpect(jsonPath("$.data.attachments[0].contentType").value("text/plain"))
                .andExpect(jsonPath("$.data.attachments[0].fileSize").value(18))
                .andExpect(jsonPath("$.data.attachments[0].e2ee.enabled").value(true))
                .andExpect(jsonPath("$.data.attachments[0].e2ee.algorithm").value("openpgp"))
                .andExpect(jsonPath("$.data.attachments[0].e2ee.recipientFingerprints[0]").value(FINGERPRINT));

        sendMail(senderToken, draftId, receiverEmail, suffix);

        long sentMailId = latestMailId(senderToken, "/api/v1/mails/sent");
        mockMvc.perform(get("/api/v1/mails/" + sentMailId + "/attachments/" + attachmentId + "/download")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("report.txt")))
                .andExpect(content().bytes(CIPHERTEXT));

        long inboxMailId = latestMailId(receiverToken, "/api/v1/mails/inbox");
        MvcResult inboxDetail = mockMvc.perform(get("/api/v1/mails/" + inboxMailId)
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments[0].e2ee.enabled").value(true))
                .andExpect(jsonPath("$.data.attachments[0].e2ee.algorithm").value("openpgp"))
                .andReturn();

        String inboundAttachmentId = readJson(inboxDetail).at("/data/attachments/0/id").asText();
        mockMvc.perform(get("/api/v1/mails/" + inboxMailId + "/attachments/" + inboundAttachmentId + "/download")
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes(CIPHERTEXT));
    }

    @Test
    void encryptedDraftAttachmentShouldRejectForeignFingerprints() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderToken = register("mail-attachment-e2ee-invalid-" + suffix + "@mmmail.local", "Attachment Sender");
        saveKeyProfile(senderToken);
        String draftId = saveDraft(senderToken, "target-" + suffix + "@example.com");

        mockMvc.perform(multipart("/api/v1/mails/drafts/" + draftId + "/attachments")
                        .file(encryptedFile())
                        .param("fileName", "report.txt")
                        .param("contentType", "text/plain")
                        .param("fileSize", "18")
                        .param("e2eeEnabled", "true")
                        .param("e2eeAlgorithm", "openpgp")
                        .param("e2eeRecipientFingerprintsJson", "[\"FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF\"]")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mail E2EE draft must target only the current sender key"));
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

    private void saveKeyProfile(String token) throws Exception {
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
                                  "keyCreatedAt": "2026-04-02T16:10:00"
                                }
                                """.formatted(FINGERPRINT, FINGERPRINT, FINGERPRINT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void setUndoSendSeconds(String token, String displayName, int undoSeconds) throws Exception {
        mockMvc.perform(put("/api/v1/settings/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "%s",
                                  "signature": "",
                                  "timezone": "UTC",
                                  "autoSaveSeconds": 15,
                                  "undoSendSeconds": %d,
                                  "driveVersionRetentionCount": 50,
                                  "driveVersionRetentionDays": 365
                                }
                                """.formatted(displayName, undoSeconds)))
                .andExpect(status().isOk());
    }

    private String saveDraft(String token, String toEmail) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mails/drafts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toEmail": "%s",
                                  "subject": "Encrypted attachment",
                                  "body": "Plain draft body"
                                }
                                """.formatted(toEmail)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/draftId").asText();
    }

    private String uploadEncryptedAttachment(String token, String draftId) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/mails/drafts/" + draftId + "/attachments")
                        .file(encryptedFile())
                        .param("fileName", "report.txt")
                        .param("contentType", "text/plain")
                        .param("fileSize", "18")
                        .param("e2eeEnabled", "true")
                        .param("e2eeAlgorithm", "openpgp")
                        .param("e2eeRecipientFingerprintsJson", "[\"" + FINGERPRINT + "\"]")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachment.e2ee.enabled").value(true))
                .andReturn();
        return readJson(result).at("/data/attachment/id").asText();
    }

    private void sendMail(String token, String draftId, String toEmail, String suffix) throws Exception {
        mockMvc.perform(post("/api/v1/mails/send")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "draftId": %s,
                                  "toEmail": "%s",
                                  "subject": "Encrypted attachment",
                                  "body": "Plain draft body",
                                  "idempotencyKey": "mail-e2ee-attachment-%s",
                                  "labels": []
                                }
                                """.formatted(draftId, toEmail, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private long latestMailId(String token, String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/items/0/id").asLong();
    }

    private MockMultipartFile encryptedFile() {
        return new MockMultipartFile(
                "file",
                "report.txt.pgp",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                CIPHERTEXT
        );
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
