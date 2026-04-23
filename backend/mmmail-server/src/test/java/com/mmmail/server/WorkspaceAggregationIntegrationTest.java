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
class WorkspaceAggregationIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void workspaceAggregationShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v2/workspace/aggregation"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void workspaceAggregationShouldExposeSurfaceAndStoryGroupsForAdmin() throws Exception {
        String adminToken = login("admin@mmmail.local", PASSWORD);

        mockMvc.perform(get("/api/v2/workspace/aggregation")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.surfaces.length()").value(3))
                .andExpect(jsonPath("$.data.surfaces[0]").value("collaboration"))
                .andExpect(jsonPath("$.data.surfaces[1]").value("command-center"))
                .andExpect(jsonPath("$.data.surfaces[2]").value("notifications"))
                .andExpect(jsonPath("$.data.storyGroups.length()").value(2))
                .andExpect(jsonPath("$.data.storyGroups[0]").value("onboarding"))
                .andExpect(jsonPath("$.data.storyGroups[1]").value("failure"))
                .andExpect(jsonPath("$.data.workspaceModules.length()").value(6))
                .andExpect(jsonPath("$.data.workspaceModules[0]").value("docs"))
                .andExpect(jsonPath("$.data.workspaceModules[1]").value("sheets"))
                .andExpect(jsonPath("$.data.workspaceModules[2]").value("mail"))
                .andExpect(jsonPath("$.data.workspaceModules[3]").value("calendar"))
                .andExpect(jsonPath("$.data.workspaceModules[4]").value("drive"))
                .andExpect(jsonPath("$.data.workspaceModules[5]").value("pass"));
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
