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
class AiMcpCapabilityIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void aiCapabilitiesShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v2/ai-platform/capabilities"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mcpRegistryShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v2/mcp/registry"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aiAndMcpCapabilitiesShouldExposeTheFrozenFlagsAfterAuthentication() throws Exception {
        String adminToken = login("admin@mmmail.local", PASSWORD);

        mockMvc.perform(get("/api/v2/ai-platform/capabilities")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runStates.length()").value(6))
                .andExpect(jsonPath("$.data.runStates[0]").value("queued"))
                .andExpect(jsonPath("$.data.runStates[1]").value("running"))
                .andExpect(jsonPath("$.data.runStates[2]").value("waiting-approval"))
                .andExpect(jsonPath("$.data.runStates[3]").value("succeeded"))
                .andExpect(jsonPath("$.data.runStates[4]").value("failed"))
                .andExpect(jsonPath("$.data.runStates[5]").value("retryable"))
                .andExpect(jsonPath("$.data.supportsPreview").value(true))
                .andExpect(jsonPath("$.data.supportsApproval").value(true))
                .andExpect(jsonPath("$.data.supportsAudit").value(true));

        mockMvc.perform(get("/api/v2/mcp/registry")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supportsGrantMatrix").value(true))
                .andExpect(jsonPath("$.data.supportsHealthChecks").value(true))
                .andExpect(jsonPath("$.data.supportsSecretMasking").value(true))
                .andExpect(jsonPath("$.data.supportsAudit").value(true));
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
