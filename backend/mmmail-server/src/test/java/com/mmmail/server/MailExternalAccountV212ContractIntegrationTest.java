package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.service.MailExternalAccountGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MailExternalAccountV212ContractIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private MailExternalAccountGateway mailExternalAccountGateway;

    @BeforeEach
    void setUp() {
        when(mailExternalAccountGateway.testConnection(any(), anyString()))
                .thenReturn(new MailExternalAccountGateway.ConnectionTestResult(true, true, 120, "ok"));
        when(mailExternalAccountGateway.syncInbox(any(), anyString(), any(Integer.class)))
                .thenReturn(new MailExternalAccountGateway.SyncFetchResult(
                        List.of(new MailExternalAccountGateway.ImportedMessage(
                                "uid-100",
                                "external@example.com",
                                "Imported Gmail",
                                "Hello from Gmail",
                                LocalDateTime.now().minusMinutes(5)
                        )),
                        "100"
                ));
    }

    @Test
    void externalMailAccountShouldHideSecretsTestSyncAndDelete() throws Exception {
        String token = register("v212-external-mail-" + System.nanoTime() + "@mmmail.local");
        JsonNode created = createAccount(token);
        String accountId = created.path("accountId").asText();

        assertThat(created.path("syncStatus").asText()).isEqualTo("INITIAL_SYNC");
        assertThat(created.has("password")).isFalse();
        assertThat(created.has("secretCiphertext")).isFalse();

        mockMvc.perform(get("/api/v1/mail/external-accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].accountId").value(accountId))
                .andExpect(jsonPath("$.data[0].password").doesNotExist());

        mockMvc.perform(get("/api/v1/mail/external-accounts/" + accountId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("alice.gmail@example.com"));

        mockMvc.perform(post("/api/v1/mail/external-accounts/" + accountId + "/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imapOk").value(true))
                .andExpect(jsonPath("$.data.smtpOk").value(true));

        mockMvc.perform(post("/api/v1/mail/external-accounts/" + accountId + "/sync")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(1))
                .andExpect(jsonPath("$.data.syncStatus").value("SYNCED"));

        mockMvc.perform(get("/api/v2/mail/messages")
                        .header("Authorization", "Bearer " + token)
                        .param("folder", "inbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].subject").value("Imported Gmail"));

        mockMvc.perform(patch("/api/v1/mail/external-accounts/" + accountId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authMode\":\"OAUTH2\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/mail/external-accounts/" + accountId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"GMAIL\",\"email\":\"alice.renamed@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("alice.renamed@example.com"));

        mockMvc.perform(delete("/api/v1/mail/external-accounts/" + accountId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private JsonNode createAccount(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/mail/external-accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "GMAIL",
                                  "authMode": "PASSWORD",
                                  "email": "alice.gmail@example.com",
                                  "username": "alice.gmail@example.com",
                                  "password": "app-password",
                                  "imap": { "host": "imap.gmail.com", "port": 993, "ssl": true },
                                  "smtp": { "host": "smtp.gmail.com", "port": 587, "starttls": true }
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return readData(result);
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "External Mail"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return readJson(result).path("data");
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
