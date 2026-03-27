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
class MailGaIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void saveDraftShouldUpdateExistingDraft() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String token = register("mail-ga-draft-" + suffix + "@mmmail.local", "Mail GA Draft");

        String draftId = saveDraft(token, null, "first-" + suffix + "@example.com", "Initial subject", "Initial body");
        String updatedDraftId = saveDraft(token, draftId, "second-" + suffix + "@example.com", "Updated subject", "Updated body");

        assertThat(updatedDraftId).isEqualTo(draftId);

        mockMvc.perform(get("/api/v1/mails/" + draftId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDraft").value(true))
                .andExpect(jsonPath("$.data.peerEmail").value("second-" + suffix + "@example.com"))
                .andExpect(jsonPath("$.data.subject").value("Updated subject"))
                .andExpect(jsonPath("$.data.body").value("Updated body"));

        mockMvc.perform(get("/api/v1/mails/drafts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void searchShouldRespectPagination() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String senderToken = register("mail-ga-sender-" + suffix + "@mmmail.local", "Mail GA Sender");
        String receiverEmail = "mail-ga-receiver-" + suffix + "@mmmail.local";
        String receiverToken = register(receiverEmail, "Mail GA Receiver");
        setUndoSendSeconds(senderToken, "Mail GA Sender", 0);

        sendMail(senderToken, receiverEmail, "Batch5A keyword first", "Mail GA pagination first", "idemp-mail-ga-1-" + suffix);
        sendMail(senderToken, receiverEmail, "Batch5A keyword second", "Mail GA pagination second", "idemp-mail-ga-2-" + suffix);

        JsonNode firstPage = searchMail(receiverToken, "Batch5A keyword", 1, 1);
        JsonNode secondPage = searchMail(receiverToken, "Batch5A keyword", 2, 1);

        assertThat(firstPage.path("total").asInt()).isEqualTo(2);
        assertThat(firstPage.path("page").asInt()).isEqualTo(1);
        assertThat(firstPage.path("items")).hasSize(1);

        assertThat(secondPage.path("total").asInt()).isEqualTo(2);
        assertThat(secondPage.path("page").asInt()).isEqualTo(2);
        assertThat(secondPage.path("items")).hasSize(1);
        assertThat(firstPage.at("/items/0/id").asText()).isNotEqualTo(secondPage.at("/items/0/id").asText());
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
                .andExpect(jsonPath("$.data.draftId").exists())
                .andReturn();
        return readJson(result).at("/data/draftId").asText();
    }

    private JsonNode searchMail(String token, String keyword, int page, int size) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/mails/search")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", keyword)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
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

    private void setUndoSendSeconds(String token, String displayName, int undoSeconds) throws Exception {
        String payload = """
                {
                  "displayName": "%s",
                  "signature": "",
                  "timezone": "UTC",
                  "autoSaveSeconds": 15,
                  "undoSendSeconds": %d,
                  "driveVersionRetentionCount": 50,
                  "driveVersionRetentionDays": 365
                }
                """.formatted(displayName, undoSeconds);
        mockMvc.perform(put("/api/v1/settings/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
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
