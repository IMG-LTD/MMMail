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
class AuthPayloadV213ContractTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginShouldExposeCurrentOrgAndEnabledProductsForFrontendRouteGates() throws Exception {
        String email = "v213-auth-payload-" + System.nanoTime() + "@mmmail.local";
        String token = register(email);
        String orgId = createOrg(token);

        JsonNode login = login(email).path("data");

        assertThat(login.path("currentOrgId").asText()).isEqualTo(orgId);
        assertThat(textValues(login.path("entitlements"))).contains("WALLET", "MEET", "MAIL", "BUSINESS");
        assertThat(textValues(login.path("featureFlags"))).contains("feat.wallet.enabled", "feat.meet.enabled");
    }

    @Test
    void currentUserInfoShouldUseFrontendUserInfoShape() throws Exception {
        String email = "v213-auth-me-" + System.nanoTime() + "@mmmail.local";
        String token = register(email);
        String orgId = createOrg(token);

        mockMvc.perform(get("/api/v2/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").isNotEmpty())
                .andExpect(jsonPath("$.data.userName").value("V213 Auth Payload"))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"))
                .andExpect(jsonPath("$.data.buttons.length()").value(0))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.mailAddressMode").value("PROTON_ADDRESS"))
                .andExpect(jsonPath("$.data.currentOrgId").value(orgId))
                .andExpect(jsonPath("$.data.entitlements").isArray())
                .andExpect(jsonPath("$.data.featureFlags").isArray());
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "V213 Auth Payload"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private String createOrg(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orgs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"V213 Closure Workspace\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private JsonNode login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readJson(result);
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
