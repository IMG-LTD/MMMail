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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SimpleLoginRelayPolicyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void relayPolicyShouldExposeCrudOverviewReadinessAndSearch() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String email = "v76-simplelogin-%s@mmmail.local".formatted(suffix);
        String token = register(email, "Password@123", "V76 SimpleLogin User");

        JsonNode mailboxes = getPassMailboxes(token);
        String mailboxId = mailboxes.get(0).path("id").asText();
        String mailboxEmail = mailboxes.get(0).path("mailboxEmail").asText();

        String orgId = createOrganization(token, "V76 Org " + suffix);
        String domainName = "relay-" + suffix + ".test";
        String domainId = createDomain(token, orgId, domainName);
        verifyDomain(token, orgId, domainId);

        JsonNode created = createRelayPolicy(token, orgId, domainId, mailboxId, true, "ANY_PREFIX", "Primary relay route");
        String policyId = created.path("id").asText();
        assertThat(created.path("domain").asText()).isEqualTo(domainName);
        assertThat(created.path("catchAllEnabled").asBoolean()).isTrue();
        assertThat(created.path("defaultMailboxEmail").asText()).isEqualTo(mailboxEmail);

        JsonNode policies = listRelayPolicies(token, orgId);
        assertThat(policies).hasSize(1);
        assertThat(policies.get(0).path("subdomainMode").asText()).isEqualTo("ANY_PREFIX");

        JsonNode overview = getSimpleLoginOverview(token, orgId);
        assertThat(overview.path("relayPolicyCount").asInt()).isEqualTo(1);
        assertThat(overview.path("catchAllDomainCount").asInt()).isEqualTo(1);
        assertThat(overview.path("subdomainPolicyCount").asInt()).isEqualTo(1);
        assertThat(overview.path("defaultRelayMailboxEmail").asText()).isEqualTo(mailboxEmail);

        JsonNode updated = updateRelayPolicy(token, orgId, policyId, mailboxId, false, "DISABLED", "Updated route");
        assertThat(updated.path("catchAllEnabled").asBoolean()).isFalse();
        assertThat(updated.path("subdomainMode").asText()).isEqualTo("DISABLED");
        assertThat(updated.path("note").asText()).isEqualTo("Updated route");

        JsonNode searchItems = getUnifiedSearchItems(token, domainName);
        JsonNode matched = findByProductAndType(searchItems, "SIMPLELOGIN", "POLICY");
        assertThat(matched).isNotNull();
        assertThat(matched.path("entityId").asText()).isEqualTo(policyId);
        assertThat(matched.path("routePath").asText()).isEqualTo("/simplelogin?orgId=" + orgId);

        JsonNode readiness = getReadinessItems(token);
        JsonNode simpleLoginItem = findByCode(readiness, "SIMPLELOGIN");
        assertThat(simpleLoginItem).isNotNull();
        assertThat(findSignalValue(simpleLoginItem.path("signals"), "relay_policy_count")).isEqualTo(1);
        assertThat(findSignalValue(simpleLoginItem.path("signals"), "catch_all_domain_count")).isEqualTo(0);

        removeRelayPolicy(token, orgId, policyId);
        JsonNode deletedPolicies = listRelayPolicies(token, orgId);
        assertThat(deletedPolicies).isEmpty();
        JsonNode finalOverview = getSimpleLoginOverview(token, orgId);
        assertThat(finalOverview.path("relayPolicyCount").asInt()).isZero();
        assertThat(finalOverview.path("catchAllDomainCount").asInt()).isZero();
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

    private JsonNode createRelayPolicy(
            String token,
            String orgId,
            String domainId,
            String mailboxId,
            boolean catchAllEnabled,
            String subdomainMode,
            String note
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/simplelogin/orgs/" + orgId + "/relay-policies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customDomainId": %s,
                                  "catchAllEnabled": %s,
                                  "subdomainMode": "%s",
                                  "defaultMailboxId": %s,
                                  "note": "%s"
                                }
                                """.formatted(domainId, catchAllEnabled, subdomainMode, mailboxId, note)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode updateRelayPolicy(
            String token,
            String orgId,
            String policyId,
            String mailboxId,
            boolean catchAllEnabled,
            String subdomainMode,
            String note
    ) throws Exception {
        MvcResult result = mockMvc.perform(put("/api/v1/simplelogin/orgs/" + orgId + "/relay-policies/" + policyId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "catchAllEnabled": %s,
                                  "subdomainMode": "%s",
                                  "defaultMailboxId": %s,
                                  "note": "%s"
                                }
                                """.formatted(catchAllEnabled, subdomainMode, mailboxId, note)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode listRelayPolicies(String token, String orgId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/simplelogin/orgs/" + orgId + "/relay-policies")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private void removeRelayPolicy(String token, String orgId, String policyId) throws Exception {
        mockMvc.perform(delete("/api/v1/simplelogin/orgs/" + orgId + "/relay-policies/" + policyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private JsonNode getSimpleLoginOverview(String token, String orgId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/simplelogin/overview")
                        .header("Authorization", "Bearer " + token)
                        .param("orgId", orgId))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode getUnifiedSearchItems(String token, String keyword) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/suite/unified-search")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", keyword))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("items");
    }

    private JsonNode getReadinessItems(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/suite/readiness")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("items");
    }

    private JsonNode findByProductAndType(JsonNode rows, String productCode, String itemType) {
        for (JsonNode row : rows) {
            if (productCode.equals(row.path("productCode").asText()) && itemType.equals(row.path("itemType").asText())) {
                return row;
            }
        }
        return null;
    }

    private JsonNode findByCode(JsonNode rows, String productCode) {
        for (JsonNode row : rows) {
            if (productCode.equals(row.path("productCode").asText())) {
                return row;
            }
        }
        return null;
    }

    private int findSignalValue(JsonNode signals, String key) {
        for (JsonNode signal : signals) {
            if (key.equals(signal.path("key").asText())) {
                return signal.path("value").asInt();
            }
        }
        return -1;
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
