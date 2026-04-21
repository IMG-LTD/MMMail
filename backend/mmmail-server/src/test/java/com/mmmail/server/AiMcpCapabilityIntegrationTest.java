package com.mmmail.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiMcpCapabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
}
