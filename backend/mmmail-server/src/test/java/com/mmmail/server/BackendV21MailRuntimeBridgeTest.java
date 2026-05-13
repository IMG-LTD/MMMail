package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendV21MailRuntimeBridgeTest {

    private static final String PASSWORD = "Password@123";
    private static final String SUBJECT = "V21 Mail Runtime";
    private static final String BODY = "Updated v2 body";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void v21MailShouldUseRuntimeDraftSendAndFolderState() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        MailPair users = registerMailPair(suffix);
        setUndoSendSeconds(users.senderToken(), "V21 Mail Sender", 0);

        String draftId = createDraft(users.senderToken(), users.receiverEmail(), "V21 Draft", "Initial v2 body");
        patchDraft(users.senderToken(), draftId, users.receiverEmail());
        sendDraft(users.senderToken(), draftId, users.receiverEmail(), suffix);

        assertSentMail(users.senderToken(), users.receiverEmail());
        String receivedMailId = latestInboxMailId(users.receiverToken());
        assertThreadDetail(users.receiverToken(), receivedMailId);
    }

    @Test
    void v21MailShouldExposeContactsRecipientTrustBulkActionAndGates() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        MailPair users = registerMailPair(suffix);
        setUndoSendSeconds(users.senderToken(), "V21 Mail Sender", 0);
        String draftId = createDraft(users.senderToken(), users.receiverEmail(), "Bulk Target", "Bulk body");
        sendDraft(users.senderToken(), draftId, users.receiverEmail(), suffix);
        String receivedMailId = latestInboxMailId(users.receiverToken());

        assertSenderIdentities(users.senderToken(), users.senderEmail());
        assertRecipientTrust(users.senderToken(), users.senderEmail(), users.receiverEmail());
        archiveReceivedMail(users.receiverToken(), receivedMailId);
        assertInboxTotal(users.receiverToken(), 0);
        assertMissingFolderRejected(users.receiverToken());
        assertMailRulesRemainPremiumGated(users.receiverToken());
    }

    @Test
    void v21MailShouldRejectInvalidBulkIdsWithoutPartialAction() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        MailPair users = registerMailPair(suffix);
        setUndoSendSeconds(users.senderToken(), "V21 Mail Sender", 0);
        String draftId = createDraft(users.senderToken(), users.receiverEmail(), "Bulk Invalid", "Bulk invalid body");
        sendDraft(users.senderToken(), draftId, users.receiverEmail(), suffix);
        String receivedMailId = latestInboxMailId(users.receiverToken());

        mockMvc.perform(post("/api/v2/mail/messages/bulk-action")
                        .header("Authorization", "Bearer " + users.receiverToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageIds": ["%s", "999999999999"],
                                  "action": "MOVE_ARCHIVE"
                                }
                                """.formatted(receivedMailId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.MAIL_NOT_FOUND.getCode()));
        assertInboxTotal(users.receiverToken(), 1);
    }

    @Test
    void v21MailShouldRejectMalformedJsonPayloadsAsInvalidArguments() throws Exception {
        String token = register("v21-mail-json-" + System.nanoTime() + "@mmmail.local", "V21 Mail Json");

        mockMvc.perform(post("/api/v2/mail/send")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "draftId": "invalid",
                                  "toEmail": "receiver@mmmail.local",
                                  "subject": "Bad JSON type",
                                  "body": "Body",
                                  "idempotencyKey": "bad-json"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    private MailPair registerMailPair(String suffix) throws Exception {
        String senderEmail = "v21-mail-sender-" + suffix + "@mmmail.local";
        String receiverEmail = "v21-mail-receiver-" + suffix + "@mmmail.local";
        String senderToken = register(senderEmail, "V21 Mail Sender");
        String receiverToken = register(receiverEmail, "V21 Mail Receiver");
        return new MailPair(senderEmail, receiverEmail, senderToken, receiverToken);
    }

    private String createDraft(String token, String toEmail, String subject, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/mail/drafts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toEmail": "%s",
                                  "subject": "%s",
                                  "body": "%s"
                                }
                                """.formatted(toEmail, subject, body)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void patchDraft(String token, String draftId, String toEmail) throws Exception {
        mockMvc.perform(patch("/api/v2/mail/drafts/" + draftId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toEmail": "%s",
                                  "subject": "%s",
                                  "body": "%s"
                                }
                                """.formatted(toEmail, SUBJECT, BODY)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(draftId))
                .andExpect(jsonPath("$.data.subject").value(SUBJECT));
    }

    private void sendDraft(String token, String draftId, String toEmail, String suffix) throws Exception {
        mockMvc.perform(post("/api/v2/mail/send")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "draftId": %s,
                                  "toEmail": "%s",
                                  "subject": "%s",
                                  "body": "%s",
                                  "idempotencyKey": "v21-mail-%s",
                                  "labels": []
                                }
                                """.formatted(draftId, toEmail, SUBJECT, BODY, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void assertSentMail(String token, String receiverEmail) throws Exception {
        mockMvc.perform(get("/api/v2/mail/messages")
                        .header("Authorization", "Bearer " + token)
                        .param("folder", "sent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].subject").value(SUBJECT))
                .andExpect(jsonPath("$.data.items[0].peerEmail").value(receiverEmail));
    }

    private String latestInboxMailId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v2/mail/messages")
                        .header("Authorization", "Bearer " + token)
                        .param("folder", "inbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].subject").value(SUBJECT))
                .andReturn();
        return readJson(result).at("/data/items/0/id").asText();
    }

    private void assertThreadDetail(String token, String receivedMailId) throws Exception {
        mockMvc.perform(get("/api/v2/mail/threads/" + receivedMailId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(receivedMailId))
                .andExpect(jsonPath("$.data.subject").value(SUBJECT))
                .andExpect(jsonPath("$.data.body").value(BODY));
    }

    private void assertSenderIdentities(String token, String senderEmail) throws Exception {
        mockMvc.perform(get("/api/v2/mail/contacts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].emailAddress").value(senderEmail));
    }

    private void assertRecipientTrust(String token, String fromEmail, String toEmail) throws Exception {
        mockMvc.perform(get("/api/v2/mail/contacts")
                        .header("Authorization", "Bearer " + token)
                        .param("capability", "recipient-trust")
                        .param("fromEmail", fromEmail)
                        .param("toEmail", toEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deliverable").value(true))
                .andExpect(jsonPath("$.data.readiness").isNotEmpty());
    }

    private void archiveReceivedMail(String token, String receivedMailId) throws Exception {
        mockMvc.perform(post("/api/v2/mail/messages/bulk-action")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageIds": ["%s"],
                                  "action": "MOVE_ARCHIVE"
                                }
                                """.formatted(receivedMailId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(receivedMailId))
                .andExpect(jsonPath("$.data[0].folderType").value("ARCHIVE"));
    }

    private void assertInboxTotal(String token, int total) throws Exception {
        mockMvc.perform(get("/api/v2/mail/messages")
                        .header("Authorization", "Bearer " + token)
                        .param("folder", "inbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(total));
    }

    private void assertMissingFolderRejected(String token) throws Exception {
        mockMvc.perform(get("/api/v2/mail/messages")
                        .header("Authorization", "Bearer " + token)
                        .param("folder", "missing-folder"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    private void assertMailRulesRemainPremiumGated(String token) throws Exception {
        mockMvc.perform(get("/api/v2/mail/rules")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
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
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record MailPair(String senderEmail, String receiverEmail, String senderToken, String receiverToken) {
    }
}
