package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PassAliasIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("Verification code: (\\d{6})");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void passAliasShouldRouteIncomingMailToMultipleVerifiedMailboxes() throws Exception {
        RoutingUsers users = createRoutingUsers("v71-mailbox");
        MailboxContext mailboxA = createMailbox(users.ownerToken(), users.mailboxAEmail());
        MailboxContext mailboxB = createMailbox(users.ownerToken(), users.mailboxBEmail());

        assertAliasCreateRejected(users.ownerToken(), List.of(users.mailboxAEmail(), users.mailboxBEmail()), 30028);

        verifyMailbox(users.ownerToken(), mailboxA.mailboxId(), extractVerificationCode(users.mailboxAToken()));
        verifyMailbox(users.ownerToken(), mailboxB.mailboxId(), extractVerificationCode(users.mailboxBToken()));
        setDefaultMailbox(users.ownerToken(), mailboxA.mailboxId());
        assertMailboxState(users.ownerToken(), users.mailboxAEmail(), "VERIFIED", true);
        assertMailboxState(users.ownerToken(), users.mailboxBEmail(), "VERIFIED", false);

        AliasContext alias = createAlias(
                users.ownerToken(),
                List.of(users.mailboxAEmail(), users.mailboxBEmail()),
                "Ops Relay",
                "ops"
        );
        assertAliasRoutes(users.ownerToken(), alias.aliasId(), Set.of(users.mailboxAEmail(), users.mailboxBEmail()));

        sendMail(
                users.senderToken(),
                alias.aliasEmail(),
                users.senderEmail(),
                "Inbound alias",
                "hello routing mailboxes",
                "idemp-v71-inbound"
        );
        assertLatestFolderMail(users.mailboxAToken(), "inbox", users.senderEmail(), users.senderEmail());
        assertLatestFolderMail(users.mailboxBToken(), "inbox", users.senderEmail(), users.senderEmail());

        assertMailboxDeleteRejected(users.ownerToken(), mailboxA.mailboxId(), 30030);
        updateAlias(users.ownerToken(), alias.aliasId(), List.of(users.ownerEmail(), users.mailboxBEmail()), "Ops Relay");
        deleteMailbox(users.ownerToken(), mailboxA.mailboxId());
        assertMailboxMissing(users.ownerToken(), users.mailboxAEmail());
        assertAliasRoutes(users.ownerToken(), alias.aliasId(), Set.of(users.ownerEmail(), users.mailboxBEmail()));
        assertOwnerAuditTrail(users.ownerToken(), Set.of(
                "PASS_MAILBOX_CREATE",
                "PASS_MAILBOX_VERIFY",
                "PASS_MAILBOX_SET_DEFAULT",
                "PASS_ALIAS_CREATE",
                "PASS_ALIAS_UPDATE",
                "MAIL_ALIAS_RELAY"
        ));
    }

    @Test
    void passAliasReverseAliasContactsShouldStillGateAliasSender() throws Exception {
        ReplyUsers users = createReplyUsers("v71-reply");
        AliasContext alias = createAlias(users.ownerToken(), List.of(users.ownerEmail()), "Acme Reply", "acme");
        AliasContactContext contact = createAliasContact(users.ownerToken(), alias.aliasId(), users.targetEmail());

        sendMail(users.targetToken(), alias.aliasEmail(), users.targetEmail(), "Inbound alias", "hello owner", "idemp-v71-owner-inbound");
        assertLatestFolderMail(users.ownerToken(), "inbox", users.targetEmail(), users.targetEmail());

        assertAliasSendRejected(users.ownerToken(), users.targetEmail(), alias.aliasEmail(), "idemp-v71-direct-send");
        sendMail(users.ownerToken(), contact.reverseAliasEmail(), alias.aliasEmail(), "Reply alias", "reply via reverse alias", "idemp-v71-reverse-send");
        assertLatestFolderMail(users.targetToken(), "inbox", alias.aliasEmail(), alias.aliasEmail());
        assertLatestFolderMail(users.ownerToken(), "sent", alias.aliasEmail(), contact.reverseAliasEmail());

        deleteAliasContact(users.ownerToken(), alias.aliasId(), contact.contactId());
        assertAliasSendRejected(users.ownerToken(), contact.reverseAliasEmail(), alias.aliasEmail(), "idemp-v71-after-delete");
        assertOwnerAuditTrail(users.ownerToken(), Set.of(
                "PASS_ALIAS_CREATE",
                "PASS_ALIAS_CONTACT_CREATE",
                "PASS_ALIAS_CONTACT_DELETE",
                "MAIL_ALIAS_REVERSE_ROUTE"
        ));
    }

    private RoutingUsers createRoutingUsers(String prefix) throws Exception {
        String suffix = prefix + '-' + System.nanoTime();
        String ownerEmail = suffix + "-owner@mmmail.local";
        String mailboxAEmail = suffix + "-mailbox-a@mmmail.local";
        String mailboxBEmail = suffix + "-mailbox-b@mmmail.local";
        String senderEmail = suffix + "-sender@mmmail.local";
        String ownerToken = register(ownerEmail, "V71 Owner");
        String mailboxAToken = register(mailboxAEmail, "V71 Mailbox A");
        String mailboxBToken = register(mailboxBEmail, "V71 Mailbox B");
        String senderToken = register(senderEmail, "V71 Sender");
        setUndoSendSeconds(ownerToken, "V71 Owner", 0);
        setUndoSendSeconds(mailboxAToken, "V71 Mailbox A", 0);
        setUndoSendSeconds(mailboxBToken, "V71 Mailbox B", 0);
        setUndoSendSeconds(senderToken, "V71 Sender", 0);
        return new RoutingUsers(ownerEmail, mailboxAEmail, mailboxBEmail, senderEmail, ownerToken, mailboxAToken, mailboxBToken, senderToken);
    }

    private ReplyUsers createReplyUsers(String prefix) throws Exception {
        String suffix = prefix + '-' + System.nanoTime();
        String ownerEmail = suffix + "-owner@mmmail.local";
        String targetEmail = suffix + "-target@mmmail.local";
        String ownerToken = register(ownerEmail, "V71 Reply Owner");
        String targetToken = register(targetEmail, "V71 Reply Target");
        setUndoSendSeconds(ownerToken, "V71 Reply Owner", 0);
        setUndoSendSeconds(targetToken, "V71 Reply Target", 0);
        return new ReplyUsers(ownerEmail, targetEmail, ownerToken, targetToken);
    }

    private MailboxContext createMailbox(String token, String mailboxEmail) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pass/mailboxes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of("mailboxEmail", mailboxEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mailboxEmail").value(mailboxEmail))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        JsonNode data = readJson(result).at("/data");
        return new MailboxContext(data.path("id").asText(), data.path("mailboxEmail").asText());
    }

    private void verifyMailbox(String token, String mailboxId, String verificationCode) throws Exception {
        mockMvc.perform(post("/api/v1/pass/mailboxes/" + mailboxId + "/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of("verificationCode", verificationCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
    }

    private void setDefaultMailbox(String token, String mailboxId) throws Exception {
        mockMvc.perform(post("/api/v1/pass/mailboxes/" + mailboxId + "/default")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultMailbox").value(true));
    }

    private void deleteMailbox(String token, String mailboxId) throws Exception {
        mockMvc.perform(delete("/api/v1/pass/mailboxes/" + mailboxId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private AliasContext createAlias(String token, List<String> forwardToEmails, String title, String prefix) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pass/aliases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "title", title,
                                "note", "v71 alias",
                                "forwardToEmails", forwardToEmails,
                                "prefix", prefix
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENABLED"))
                .andExpect(jsonPath("$.data.forwardToEmails.length()").value(forwardToEmails.size()))
                .andReturn();
        JsonNode data = readJson(result).at("/data");
        return new AliasContext(data.path("id").asText(), data.path("aliasEmail").asText());
    }

    private void updateAlias(String token, String aliasId, List<String> forwardToEmails, String title) throws Exception {
        mockMvc.perform(put("/api/v1/pass/aliases/" + aliasId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "title", title,
                                "note", "rerouted",
                                "forwardToEmails", forwardToEmails
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.forwardToEmails.length()").value(forwardToEmails.size()));
    }

    private void assertAliasCreateRejected(String token, List<String> forwardToEmails, int errorCode) throws Exception {
        mockMvc.perform(post("/api/v1/pass/aliases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "title", "Pending alias",
                                "note", "should fail",
                                "forwardToEmails", forwardToEmails,
                                "prefix", "pending"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(errorCode));
    }

    private void assertAliasRoutes(String token, String aliasId, Set<String> expectedRoutes) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/pass/aliases")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode alias = findById(readJson(result).at("/data"), aliasId);
        assertThat(alias).isNotNull();
        Set<String> routes = new HashSet<>();
        for (JsonNode route : alias.path("forwardToEmails")) {
            routes.add(route.asText());
        }
        assertThat(routes).isEqualTo(expectedRoutes);
    }

    private AliasContactContext createAliasContact(String token, String aliasId, String targetEmail) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pass/aliases/" + aliasId + "/contacts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "targetEmail", targetEmail,
                                "displayName", "Target User",
                                "note", "Primary reverse route"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetEmail").value(targetEmail))
                .andReturn();
        JsonNode data = readJson(result).at("/data");
        return new AliasContactContext(data.path("id").asText(), data.path("reverseAliasEmail").asText());
    }

    private void deleteAliasContact(String token, String aliasId, String contactId) throws Exception {
        mockMvc.perform(delete("/api/v1/pass/aliases/" + aliasId + "/contacts/" + contactId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void sendMail(String token, String toEmail, String fromEmail, String subject, String body, String idempotencyKey) throws Exception {
        mockMvc.perform(post("/api/v1/mails/send")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "toEmail", toEmail,
                                "fromEmail", fromEmail,
                                "subject", subject,
                                "body", body,
                                "idempotencyKey", idempotencyKey,
                                "labels", List.of()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void assertAliasSendRejected(String token, String toEmail, String fromEmail, String idempotencyKey) throws Exception {
        mockMvc.perform(post("/api/v1/mails/send")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "toEmail", toEmail,
                                "fromEmail", fromEmail,
                                "subject", "Rejected",
                                "body", "should reject",
                                "idempotencyKey", idempotencyKey,
                                "labels", List.of()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001));
    }

    private void assertMailboxDeleteRejected(String token, String mailboxId, int errorCode) throws Exception {
        mockMvc.perform(delete("/api/v1/pass/mailboxes/" + mailboxId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(errorCode));
    }

    private void assertMailboxState(String token, String mailboxEmail, String status, boolean defaultMailbox) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/pass/mailboxes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode mailbox = findByEmail(readJson(result).at("/data"), mailboxEmail);
        assertThat(mailbox).isNotNull();
        assertThat(mailbox.path("status").asText()).isEqualTo(status);
        assertThat(mailbox.path("defaultMailbox").asBoolean()).isEqualTo(defaultMailbox);
    }

    private void assertMailboxMissing(String token, String mailboxEmail) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/pass/mailboxes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(findByEmail(readJson(result).at("/data"), mailboxEmail)).isNull();
    }

    private void assertLatestFolderMail(String token, String folder, String senderEmail, String peerEmail) throws Exception {
        String mailId = latestFolderMailId(token, folder);
        mockMvc.perform(get("/api/v1/mails/" + mailId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.senderEmail").value(senderEmail))
                .andExpect(jsonPath("$.data.peerEmail").value(peerEmail));
    }

    private String extractVerificationCode(String token) throws Exception {
        String mailId = latestFolderMailId(token, "inbox");
        MvcResult result = mockMvc.perform(get("/api/v1/mails/" + mailId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Matcher matcher = VERIFICATION_CODE_PATTERN.matcher(readJson(result).at("/data/body").asText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String latestFolderMailId(String token, String folder) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/mails/" + folder)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/items/0/id").asText();
    }

    private String register(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "displayName", displayName
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private void setUndoSendSeconds(String token, String displayName, int undoSeconds) throws Exception {
        mockMvc.perform(put("/api/v1/settings/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeJson(Map.of(
                                "displayName", displayName,
                                "signature", "",
                                "timezone", "UTC",
                                "autoSaveSeconds", 15,
                                "undoSendSeconds", undoSeconds,
                                "driveVersionRetentionCount", 50,
                                "driveVersionRetentionDays", 365
                        ))))
                .andExpect(status().isOk());
    }

    private void assertOwnerAuditTrail(String token, Set<String> expectedTypes) throws Exception {
        assertThat(listAuditTypes(token)).containsAll(expectedTypes);
    }

    private Set<String> listAuditTypes(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/audit/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        Set<String> types = new HashSet<>();
        for (JsonNode item : readJson(result).at("/data")) {
            types.add(item.path("eventType").asText());
        }
        return types;
    }

    private JsonNode findByEmail(JsonNode items, String mailboxEmail) {
        for (JsonNode item : items) {
            if (mailboxEmail.equals(item.path("mailboxEmail").asText())) {
                return item;
            }
        }
        return null;
    }

    private JsonNode findById(JsonNode items, String id) {
        for (JsonNode item : items) {
            if (id.equals(item.path("id").asText())) {
                return item;
            }
        }
        return null;
    }

    private String writeJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record RoutingUsers(
            String ownerEmail,
            String mailboxAEmail,
            String mailboxBEmail,
            String senderEmail,
            String ownerToken,
            String mailboxAToken,
            String mailboxBToken,
            String senderToken
    ) {
    }

    private record ReplyUsers(String ownerEmail, String targetEmail, String ownerToken, String targetToken) {
    }

    private record MailboxContext(String mailboxId, String mailboxEmail) {
    }

    private record AliasContext(String aliasId, String aliasEmail) {
    }

    private record AliasContactContext(String contactId, String reverseAliasEmail) {
    }
}
