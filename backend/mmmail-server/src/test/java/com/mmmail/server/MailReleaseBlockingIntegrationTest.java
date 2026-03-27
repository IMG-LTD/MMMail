package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MailReleaseBlockingIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void filtersSearchPaginationAndBulkActionsShouldRemainStable() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderToken = register("mail-rb-sender-" + suffix + "@mmmail.local", "RB Sender");
        String receiverEmail = "mail-rb-receiver-" + suffix + "@mmmail.local";
        String receiverToken = register(receiverEmail, "RB Receiver");
        setUndoSendSeconds(senderToken, "RB Sender", 0);

        mockMvc.perform(post("/api/v1/labels")
                        .header("Authorization", "Bearer " + receiverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ops\",\"color\":\"#0F6E6E\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/mail-filters")
                        .header("Authorization", "Bearer " + receiverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ops archive %s",
                                  "senderContains": "mail-rb-sender-%s@mmmail.local",
                                  "subjectContains": "Ops",
                                  "keywordContains": "urgent",
                                  "targetFolder": "ARCHIVE",
                                  "labels": ["ops"],
                                  "markRead": true,
                                  "enabled": true
                                }
                                """.formatted(suffix, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));

        sendMail(senderToken, receiverEmail, "Ops incident", "urgent action required", "rb-filter-" + suffix);
        sendMail(senderToken, receiverEmail, "ReleaseBlocking pagination A", "body-A", "rb-search-a-" + suffix);
        sendMail(senderToken, receiverEmail, "ReleaseBlocking pagination B", "body-B", "rb-search-b-" + suffix);

        mockMvc.perform(get("/api/v1/mails/archive")
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].labels[0]").value("ops"));

        JsonNode firstPage = search(receiverToken, "ReleaseBlocking pagination", 1, 1);
        JsonNode secondPage = search(receiverToken, "ReleaseBlocking pagination", 2, 1);
        assertThat(firstPage.path("items")).hasSize(1);
        assertThat(secondPage.path("items")).hasSize(1);
        assertThat(firstPage.at("/items/0/id").asText()).isNotEqualTo(secondPage.at("/items/0/id").asText());

        long inboxMailId = latestMailId(receiverToken, "/api/v1/mails/inbox");
        mockMvc.perform(post("/api/v1/mails/actions/batch")
                        .header("Authorization", "Bearer " + receiverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mailIds": [%d],
                                  "action": "MOVE_ARCHIVE"
                                }
                                """.formatted(inboxMailId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affected").value(1));

        mockMvc.perform(get("/api/v1/mails/inbox")
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void batchActionsShouldNotCrossTenantBoundary() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderToken = register("mail-rb-owner-" + suffix + "@mmmail.local", "RB Owner");
        String receiverEmail = "mail-rb-target-" + suffix + "@mmmail.local";
        String receiverToken = register(receiverEmail, "RB Target");
        String outsiderToken = register("mail-rb-outsider-" + suffix + "@mmmail.local", "RB Outsider");
        setUndoSendSeconds(senderToken, "RB Owner", 0);

        sendMail(senderToken, receiverEmail, "Boundary subject", "Boundary body", "rb-boundary-" + suffix);
        long inboxMailId = latestMailId(receiverToken, "/api/v1/mails/inbox");

        mockMvc.perform(post("/api/v1/mails/actions/batch")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mailIds": [%d],
                                  "action": "MOVE_TRASH"
                                }
                                """.formatted(inboxMailId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affected").value(0));

        mockMvc.perform(get("/api/v1/mails/" + inboxMailId)
                        .header("Authorization", "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.folderType").value("INBOX"));
    }

    @Test
    void searchShouldNotLeakAnotherTenantMailbox() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderToken = register("mail-rb-search-owner-" + suffix + "@mmmail.local", "RB Search Owner");
        String receiverEmail = "mail-rb-search-target-" + suffix + "@mmmail.local";
        String receiverToken = register(receiverEmail, "RB Search Target");
        String outsiderToken = register("mail-rb-search-outsider-" + suffix + "@mmmail.local", "RB Search Outsider");
        setUndoSendSeconds(senderToken, "RB Search Owner", 0);

        sendMail(senderToken, receiverEmail, "Secret tenant keyword", "boundary body", "rb-search-boundary-" + suffix);

        mockMvc.perform(get("/api/v1/mails/search")
                        .header("Authorization", "Bearer " + receiverToken)
                        .param("keyword", "tenant keyword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(get("/api/v1/mails/search")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .param("keyword", "tenant keyword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void invalidSearchInputShouldReturnClearError() throws Exception {
        String token = register("mail-rb-search-" + System.nanoTime() + "@mmmail.local", "RB Search");

        mockMvc.perform(get("/api/v1/mails/search")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "bad-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid datetime for from"));
    }

    @Test
    void protectedMailEndpointsShouldRejectMissingSession() throws Exception {
        mockMvc.perform(get("/api/v1/mails/inbox"))
                .andExpect(status().isUnauthorized());
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

    private void sendMail(String token, String toEmail, String subject, String body, String idempotencyKey) throws Exception {
        mockMvc.perform(post("/api/v1/mails/send")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toEmail": "%s",
                                  "subject": "%s",
                                  "body": "%s",
                                  "idempotencyKey": "%s",
                                  "labels": []
                                }
                                """.formatted(toEmail, subject, body, idempotencyKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private JsonNode search(String token, String keyword, int page, int size) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/mails/search")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", keyword)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
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

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
