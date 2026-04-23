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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlatformCapabilityIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void capabilityEndpointShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v2/platform/capabilities"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void capabilityEndpointShouldExposeStablePayloadForAdmin() throws Exception {
        String adminToken = login("admin@mmmail.local", PASSWORD);

        mockMvc.perform(get("/api/v2/platform/capabilities")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.jobRunStates.length()").value(6))
                .andExpect(jsonPath("$.data.jobRunStates[0]").value("QUEUED"))
                .andExpect(jsonPath("$.data.jobRunStates[1]").value("RUNNING"))
                .andExpect(jsonPath("$.data.jobRunStates[2]").value("WAITING_APPROVAL"))
                .andExpect(jsonPath("$.data.jobRunStates[3]").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.jobRunStates[4]").value("FAILED"))
                .andExpect(jsonPath("$.data.jobRunStates[5]").value("RETRYABLE"))
                .andExpect(jsonPath("$.data.outboxStatuses.length()").value(4))
                .andExpect(jsonPath("$.data.outboxStatuses[0]").value("PENDING"))
                .andExpect(jsonPath("$.data.outboxStatuses[1]").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.outboxStatuses[2]").value("FAILED"))
                .andExpect(jsonPath("$.data.outboxStatuses[3]").value("DEAD_LETTER"))
                .andExpect(jsonPath("$.data.auditEventTypes.length()").value(5))
                .andExpect(jsonPath("$.data.auditEventTypes[0]").value("AUTH"))
                .andExpect(jsonPath("$.data.auditEventTypes[1]").value("GOVERNANCE"))
                .andExpect(jsonPath("$.data.auditEventTypes[2]").value("PUBLIC_SHARE"))
                .andExpect(jsonPath("$.data.auditEventTypes[3]").value("AUTOMATION"))
                .andExpect(jsonPath("$.data.auditEventTypes[4]").value("MCP"))
                .andExpect(jsonPath("$.data.softAuthSupported").value(true))
                .andExpect(jsonPath("$.data.scopeHeaders.length()").value(2))
                .andExpect(jsonPath("$.data.scopeHeaders[0]").value("X-MMMAIL-ORG-ID"))
                .andExpect(jsonPath("$.data.scopeHeaders[1]").value("X-MMMAIL-SCOPE-ID"));
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"email\": \"%s\",
                                  \"password\": \"%s\"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
