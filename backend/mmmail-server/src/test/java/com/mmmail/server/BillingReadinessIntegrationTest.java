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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BillingReadinessIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void billingReadinessShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v2/billing/readiness"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void billingReadinessShouldKeepPanelsStableForAuthenticatedUsers() throws Exception {
        String token = register("billing-user-" + System.nanoTime() + "@mmmail.local", PASSWORD, "Billing User");

        mockMvc.perform(get("/api/v2/billing/readiness")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.data.panels.length()").value(4))
                .andExpect(jsonPath("$.data.panels[0]").value("plans"))
                .andExpect(jsonPath("$.data.panels[1]").value("billing"))
                .andExpect(jsonPath("$.data.panels[2]").value("operations"))
                .andExpect(jsonPath("$.data.panels[3]").value("boundary"))
                .andExpect(jsonPath("$.data.legacyExitReady").value(true));
    }

    private String register(String email, String password, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"email\": \"%s\",
                                  \"password\": \"%s\",
                                  \"displayName\": \"%s\"
                                }
                                """.formatted(email, password, displayName)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
