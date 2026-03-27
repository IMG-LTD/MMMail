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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class MailAttachmentIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void draftAttachmentsShouldUploadPersistSendAndDownload() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderToken = register("mail-attachment-sender-" + suffix + "@mmmail.local", "Attachment Sender");
        String receiverEmail = "mail-attachment-receiver-" + suffix + "@mmmail.local";
        String receiverToken = register(receiverEmail, "Attachment Receiver");
        setUndoSendSeconds(senderToken, "Attachment Sender", 0);

        String draftId = saveDraft(senderToken, null, receiverEmail, "Attachment subject", "Attachment body");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "mail attachment payload".getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/mails/drafts/" + draftId + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.draftId").value(draftId))
                .andExpect(jsonPath("$.data.attachment.fileName").value("report.txt"))
                .andReturn();

        String attachmentId = readJson(uploadResult).at("/data/attachment/id").asText();

        mockMvc.perform(get("/api/v1/mails/" + draftId)
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments[0].id").value(attachmentId))
                .andExpect(jsonPath("$.data.attachments[0].fileName").value("report.txt"));

        sendMail(senderToken, draftId, receiverEmail, "Attachment subject", "Attachment body", "mail-attachment-send-" + suffix);

        long sentMailId = latestMailId(senderToken, "/api/v1/mails/sent");
        mockMvc.perform(get("/api/v1/mails/" + sentMailId + "/attachments/" + attachmentId + "/download")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("report.txt")))
                .andExpect(content().bytes("mail attachment payload".getBytes(StandardCharsets.UTF_8)));

        long inboxMailId = latestMailId(receiverToken, "/api/v1/mails/inbox");
        MvcResult inboxDetail = mockMvc.perform(get("/api/v1/mails/" + inboxMailId)
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments[0].fileName").value("report.txt"))
                .andReturn();

        String inboundAttachmentId = readJson(inboxDetail).at("/data/attachments/0/id").asText();
        mockMvc.perform(get("/api/v1/mails/" + inboxMailId + "/attachments/" + inboundAttachmentId + "/download")
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes("mail attachment payload".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void attachmentDeletionAndAccessBoundaryShouldBeEnforced() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerToken = register("mail-attachment-owner-" + suffix + "@mmmail.local", "Attachment Owner");
        String outsiderToken = register("mail-attachment-outsider-" + suffix + "@mmmail.local", "Attachment Outsider");

        String draftId = saveDraft(ownerToken, null, "target-" + suffix + "@example.com", "Delete subject", "Delete body");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "remove me".getBytes(StandardCharsets.UTF_8)
        );
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/mails/drafts/" + draftId + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        String attachmentId = readJson(uploadResult).at("/data/attachment/id").asText();

        mockMvc.perform(get("/api/v1/mails/" + draftId + "/attachments/" + attachmentId + "/download")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(30001));

        mockMvc.perform(delete("/api/v1/mails/drafts/" + draftId + "/attachments/" + attachmentId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/mails/" + draftId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attachments").isEmpty());
    }

    @Test
    void dangerousAttachmentTypesShouldBeRejected() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerToken = register("mail-attachment-danger-" + suffix + "@mmmail.local", "Attachment Danger");
        String draftId = saveDraft(ownerToken, null, "target-" + suffix + "@example.com", "Danger subject", "Danger body");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "danger.exe",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "danger".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/mails/drafts/" + draftId + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Attachment type is not allowed"));
    }

    @Test
    void oversizedAttachmentsShouldBeRejected() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerToken = register("mail-attachment-large-" + suffix + "@mmmail.local", "Attachment Large");
        String draftId = saveDraft(ownerToken, null, "target-" + suffix + "@example.com", "Large subject", "Large body");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "oversized.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[21 * 1024 * 1024]
        );

        mockMvc.perform(multipart("/api/v1/mails/drafts/" + draftId + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Attachment exceeds 20MB limit"));
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

    private String saveDraft(String token, String draftId, String toEmail, String subject, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mails/drafts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(draftPayload(draftId, toEmail, subject, body)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/draftId").asText();
    }

    private void sendMail(String token, String draftId, String toEmail, String subject, String body, String idempotencyKey) throws Exception {
        mockMvc.perform(post("/api/v1/mails/send")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "draftId": %s,
                                  "toEmail": "%s",
                                  "subject": "%s",
                                  "body": "%s",
                                  "idempotencyKey": "%s",
                                  "labels": []
                                }
                                """.formatted(draftId, toEmail, subject, body, idempotencyKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private long latestMailId(String token, String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").exists())
                .andReturn();
        return readJson(result).at("/data/items/0/id").asLong();
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

    private String draftPayload(String draftId, String toEmail, String subject, String body) {
        String draftFragment = draftId == null ? "" : """
                  "draftId": %s,
                """.formatted(draftId);
        return """
                {
                %s
                  "toEmail": "%s",
                  "subject": "%s",
                  "body": "%s"
                }
                """.formatted(draftFragment, toEmail, subject, body);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
