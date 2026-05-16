package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.service.FeatureFlagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeatureFlagV212ContractIntegrationTest {

    private static final String ENABLED_FLAG = "feat.wallet.enabled";
    private static final String DISABLED_FLAG = "feat.vpn.enabled";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private FeatureFlagService featureFlagService;

    @Test
    void authAndSettingsShouldExposeFeatureFlagsFromFeatureFlagTable() throws Exception {
        upsertFlag(ENABLED_FLAG, true);
        upsertFlag(DISABLED_FLAG, false);
        featureFlagService.refresh();

        MvcResult registerResult = register("v212-feature-flags");
        JsonNode registerJson = readJson(registerResult);
        String token = registerJson.at("/data/accessToken").asText();

        assertThat(textValues(registerJson.at("/data/featureFlags"))).contains(ENABLED_FLAG);
        assertThat(textValues(registerJson.at("/data/featureFlags"))).doesNotContain(DISABLED_FLAG);

        JsonNode settingsFlags = getFeatureFlags(token);
        assertThat(textValues(settingsFlags)).contains(ENABLED_FLAG);
        assertThat(textValues(settingsFlags)).doesNotContain(DISABLED_FLAG);
    }

    private void upsertFlag(String flagKey, boolean enabled) {
        jdbcTemplate.update("delete from feature_flag where flag_key = ?", flagKey);
        jdbcTemplate.update(
                """
                        insert into feature_flag (flag_key, enabled, description, created_at, updated_at)
                        values (?, ?, ?, current_timestamp, current_timestamp)
                        """,
                flagKey,
                enabled ? 1 : 0,
                "v2.1.2 feature flag contract"
        );
    }

    private MvcResult register(String prefix) throws Exception {
        String email = prefix + "-" + System.nanoTime() + "@mmmail.local";
        return mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password@123",
                                  "displayName": "Feature Flag Tester"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
    }

    private JsonNode getFeatureFlags(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/settings/feature-flags")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private List<String> textValues(JsonNode arrayNode) {
        List<String> values = new ArrayList<>();
        arrayNode.forEach(value -> values.add(value.asText()));
        return values;
    }
}
