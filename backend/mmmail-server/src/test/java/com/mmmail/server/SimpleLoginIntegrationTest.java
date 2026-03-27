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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SimpleLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void simpleLoginWorkspaceShouldExposeOverviewAndSuiteIntegration() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String email = "v74-simplelogin-%s@mmmail.local".formatted(suffix);
        String token = register(email, "Password@123", "V74 SimpleLogin User");

        JsonNode initialOverview = getSimpleLoginOverview(token, null);
        assertThat(initialOverview.path("aliasCount").asInt()).isZero();
        assertThat(initialOverview.path("mailboxCount").asInt()).isGreaterThanOrEqualTo(1);

        JsonNode mailboxes = getPassMailboxes(token);
        String defaultMailboxEmail = mailboxes.get(0).path("mailboxEmail").asText();
        String aliasTitle = "Relay Workspace " + suffix;
        JsonNode alias = createAlias(token, aliasTitle, defaultMailboxEmail, "relay-" + suffix);
        String aliasId = alias.path("id").asText();
        assertThat(alias.path("aliasEmail").asText()).contains("@");

        String orgId = createOrganization(token, "V74 Org " + suffix);
        String domainId = createDomain(token, orgId, "relay-" + suffix + ".test");
        verifyDomain(token, orgId, domainId);

        JsonNode scopedOverview = getSimpleLoginOverview(token, orgId);
        assertThat(scopedOverview.path("aliasCount").asInt()).isEqualTo(1);
        assertThat(scopedOverview.path("verifiedMailboxCount").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(scopedOverview.path("customDomainCount").asInt()).isEqualTo(1);
        assertThat(scopedOverview.path("verifiedCustomDomainCount").asInt()).isEqualTo(1);

        JsonNode products = getSuiteProducts(token);
        assertThat(containsProduct(products, "SIMPLELOGIN")).isTrue();

        JsonNode quickRoutes = getCommandCenterQuickRoutes(token);
        assertThat(containsQuickRoute(quickRoutes, "SIMPLELOGIN", "/simplelogin")).isTrue();

        JsonNode searchItems = getUnifiedSearchItems(token, suffix);
        JsonNode matched = findByProductCode(searchItems, "SIMPLELOGIN");
        assertThat(matched).isNotNull();
        assertThat(matched.path("entityId").asText()).isEqualTo(aliasId);
        assertThat(matched.path("routePath").asText()).isEqualTo("/simplelogin?aliasId=" + aliasId);
    }

    private String register(String email, String password, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "%s"
                                }
                                """.formatted(email, password, displayName)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private JsonNode getPassMailboxes(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/pass/mailboxes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode createAlias(String token, String title, String mailboxEmail, String prefix) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/pass/aliases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "forwardToEmails": ["%s"],
                                  "prefix": "%s"
                                }
                                """.formatted(title, mailboxEmail, prefix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(title))
                .andReturn();
        return readJson(result).path("data");
    }

    private String createOrganization(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orgs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String createDomain(String token, String orgId, String domain) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orgs/" + orgId + "/domains")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"domain\":\"" + domain + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.domain").value(domain))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void verifyDomain(String token, String orgId, String domainId) throws Exception {
        mockMvc.perform(post("/api/v1/orgs/" + orgId + "/domains/" + domainId + "/verify")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
    }

    private JsonNode getSimpleLoginOverview(String token, String orgId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/simplelogin/overview")
                        .header("Authorization", "Bearer " + token)
                        .param("orgId", orgId == null ? "" : orgId))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode getSuiteProducts(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/suite/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode getCommandCenterQuickRoutes(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/suite/command-center")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("quickRoutes");
    }

    private JsonNode getUnifiedSearchItems(String token, String keyword) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/suite/unified-search")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", keyword))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("items");
    }

    private boolean containsProduct(JsonNode rows, String productCode) {
        for (JsonNode row : rows) {
            if (productCode.equals(row.path("code").asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsQuickRoute(JsonNode rows, String productCode, String routePath) {
        for (JsonNode row : rows) {
            if (productCode.equals(row.path("productCode").asText())
                    && routePath.equals(row.path("routePath").asText())) {
                return true;
            }
        }
        return false;
    }

    private JsonNode findByProductCode(JsonNode rows, String productCode) {
        for (JsonNode row : rows) {
            if (productCode.equals(row.path("productCode").asText())) {
                return row;
            }
        }
        return null;
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
