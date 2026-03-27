package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class SettingsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void profileShouldExposeDefaultPreferredLocale() throws Exception {
        String token = register("v79-settings-default");

        JsonNode profile = getProfile(token);

        assertThat(profile.path("preferredLocale").asText()).isEqualTo("en");
        assertThat(profile.path("mailAddressMode").asText()).isEqualTo("PROTON_ADDRESS");
        assertThat(profile.path("timezone").asText()).isEqualTo("UTC");
    }

    @Test
    void updateProfileShouldPersistPreferredLocale() throws Exception {
        String token = register("v79-settings-update");

        mockMvc.perform(put("/api/v1/settings/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Locale Tester",
                                  "signature": "Ni hao",
                                  "timezone": "Asia/Shanghai",
                                  "preferredLocale": "zh-TW",
                                  "mailAddressMode": "EXTERNAL_ACCOUNT",
                                  "autoSaveSeconds": 30,
                                  "undoSendSeconds": 20,
                                  "driveVersionRetentionCount": 80,
                                  "driveVersionRetentionDays": 730
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.preferredLocale").value("zh-TW"))
                .andExpect(jsonPath("$.data.mailAddressMode").value("EXTERNAL_ACCOUNT"))
                .andExpect(jsonPath("$.data.displayName").value("Locale Tester"));

        JsonNode reopened = getProfile(token);
        assertThat(reopened.path("preferredLocale").asText()).isEqualTo("zh-TW");
        assertThat(reopened.path("mailAddressMode").asText()).isEqualTo("EXTERNAL_ACCOUNT");
        assertThat(reopened.path("autoSaveSeconds").asInt()).isEqualTo(30);
        assertThat(reopened.path("driveVersionRetentionCount").asInt()).isEqualTo(80);
    }

    private JsonNode getProfile(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/settings/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String register(String prefix) throws Exception {
        String email = prefix + "-" + System.nanoTime() + "@mmmail.local";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password@123",
                                  "displayName": "Locale Tester"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }
}
