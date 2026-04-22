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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "mmmail.schema.preview-initializers.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SuiteOrgAccessIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final String MAIL_BASELINE_ACTION = "MAIL_ADD_BLOCKED_DOMAIN_BASELINE";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void suiteAggregationSurfacesShouldRespectOrgScopeProductAccess() throws Exception {
        OrgScenario scenario = createScenario("v97-suite");
        String keyword = "v97kw" + scenario.suffix().substring(Math.max(0, scenario.suffix().length() - 6));

        seedMailCommandCenterData(scenario, keyword);
        seedDocsCollaborationData(scenario, keyword);
        executeMailRemediation(scenario.memberToken());
        disableMemberProducts(scenario, "MAIL", "DISABLED", "DOCS", "DISABLED");

        JsonNode products = suiteGet(scenario.memberToken(), scenario.orgId(), "/api/v1/suite/products");
        JsonNode readiness = suiteGet(scenario.memberToken(), scenario.orgId(), "/api/v1/suite/readiness");
        JsonNode posture = suiteGet(scenario.memberToken(), scenario.orgId(), "/api/v1/suite/security-posture");
        JsonNode commandCenter = suiteGet(scenario.memberToken(), scenario.orgId(), "/api/v1/suite/command-center");
        JsonNode commandFeed = suiteGet(scenario.memberToken(), scenario.orgId(), "/api/v1/suite/command-feed", "limit", "20");
        JsonNode unifiedSearch = suiteGet(
                scenario.memberToken(),
                scenario.orgId(),
                "/api/v1/suite/unified-search",
                "keyword",
                keyword,
                "limit",
                "20"
        );
        JsonNode collaborationCenter = suiteGet(
                scenario.memberToken(),
                scenario.orgId(),
                "/api/v1/suite/collaboration-center",
                "limit",
                "20"
        );
        JsonNode notificationCenter = suiteGet(
                scenario.memberToken(),
                scenario.orgId(),
                "/api/v1/suite/notification-center",
                "limit",
                "20"
        );

        assertThat(collectFieldValues(products, "code")).doesNotContain("MAIL", "DOCS");
        assertThat(collectFieldValues(readiness.path("items"), "productCode")).doesNotContain("MAIL", "DOCS");
        assertThat(collectFieldValues(posture.path("recommendedActions"), "productCode")).doesNotContain("MAIL", "DOCS");
        assertThat(posture.path("blockedSenderCount").asInt()).isZero();
        assertThat(posture.path("blockedDomainCount").asInt()).isZero();

        assertThat(collectFieldValues(commandCenter.path("quickRoutes"), "productCode")).doesNotContain("MAIL", "DOCS");
        assertThat(collectFieldValues(commandCenter.path("recommendedActions"), "productCode")).doesNotContain("MAIL", "DOCS");
        assertThat(commandCenter.path("pinnedSearches").size()).isZero();
        assertThat(commandCenter.path("recentKeywords").size()).isZero();

        assertThat(collectFieldValues(commandFeed.path("items"), "productCode")).doesNotContain("MAIL", "DOCS");
        assertThat(collectFieldValues(commandFeed.path("items"), "eventType"))
                .doesNotContain(
                        "SUITE_PRODUCT_LIST",
                        "SUITE_SUBSCRIPTION_QUERY",
                        "SUITE_COMMAND_CENTER_QUERY",
                        "SUITE_COLLABORATION_CENTER_QUERY"
                );
        assertThat(collectFieldValues(commandFeed.path("items"), "detail"))
                .noneMatch(detail -> detail.startsWith("count=") || detail.startsWith("plan="));
        assertThat(collectFieldValues(unifiedSearch.path("items"), "productCode")).doesNotContain("MAIL", "DOCS");
        assertThat(collectFieldValues(collaborationCenter.path("items"), "productCode")).doesNotContain("DOCS");
        assertThat(collaborationCenter.path("productCounts").path("DOCS").asInt()).isZero();
        assertThat(collectFieldValues(notificationCenter.path("items"), "productCode")).doesNotContain("MAIL", "DOCS");
    }

    @Test
    void suiteRemediationActionsShouldRejectDisabledProductsInOrgScope() throws Exception {
        OrgScenario scenario = createScenario("v97-remediation");
        disableMemberProducts(scenario, "MAIL", "DISABLED");

        mockMvc.perform(post("/api/v1/suite/remediation-actions/execute")
                        .header("Authorization", "Bearer " + scenario.memberToken())
                        .header("X-MMMAIL-ORG-ID", scenario.orgId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionCode": "%s"
                                }
                                """.formatted(MAIL_BASELINE_ACTION)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(30045));

        mockMvc.perform(post("/api/v1/suite/remediation-actions/batch-execute")
                        .header("Authorization", "Bearer " + scenario.memberToken())
                        .header("X-MMMAIL-ORG-ID", scenario.orgId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionCodes": ["%s"]
                                }
                                """.formatted(MAIL_BASELINE_ACTION)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(30045));
    }

    private OrgScenario createScenario(String prefix) throws Exception {
        String suffix = prefix + "-" + System.nanoTime();
        String ownerEmail = suffix + "-owner@mmmail.local";
        String memberEmail = suffix + "-member@mmmail.local";
        String ownerToken = register(ownerEmail, "V97 Owner");
        String memberToken = register(memberEmail, "V97 Member");
        String orgId = createOrganization(ownerToken, "V97 Org " + suffix);
        inviteMember(ownerToken, orgId, memberEmail, "MEMBER");
        acceptFirstIncomingInvite(memberToken);
        String memberId = findMemberId(ownerToken, orgId, memberEmail);
        return new OrgScenario(suffix, orgId, ownerToken, memberToken, ownerEmail, memberEmail, memberId);
    }

    private void seedMailCommandCenterData(OrgScenario scenario, String keyword) throws Exception {
        sendMail(
                scenario.ownerToken(),
                scenario.memberEmail(),
                keyword + " seed",
                "seed for suite org scope",
                "mail-seed-" + scenario.suffix()
        );
        mockMvc.perform(get("/api/v1/mails/search")
                        .header("Authorization", "Bearer " + scenario.memberToken())
                        .param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        String presetId = createSearchPreset(scenario.memberToken(), keyword);
        mockMvc.perform(post("/api/v1/search-presets/" + presetId + "/pin")
                        .header("Authorization", "Bearer " + scenario.memberToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPinned").value(true));
    }

    private void seedDocsCollaborationData(OrgScenario scenario, String keyword) throws Exception {
        String noteId = createNote(scenario.memberToken(), keyword + " note", "docs scope seed");
        createDocsShare(scenario.memberToken(), noteId, scenario.ownerEmail(), "EDIT");
        createComment(scenario.ownerToken(), noteId, "scope", "docs collaboration seed");
    }

    private void executeMailRemediation(String token) throws Exception {
        mockMvc.perform(post("/api/v1/suite/remediation-actions/execute")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actionCode": "%s"
                                }
                                """.formatted(MAIL_BASELINE_ACTION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void disableMemberProducts(OrgScenario scenario, String... changes) throws Exception {
        mockMvc.perform(put("/api/v1/orgs/" + scenario.orgId() + "/admin-console/product-access/" + scenario.memberId())
                        .header("Authorization", "Bearer " + scenario.ownerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productAccessPayload(changes)))
                .andExpect(status().isOk());
    }

    private JsonNode suiteGet(String token, String orgId, String path, String... params) throws Exception {
        MockHttpServletRequestBuilder builder = get(path)
                .header("Authorization", "Bearer " + token)
                .header("X-MMMAIL-ORG-ID", orgId);
        for (int index = 0; index < params.length; index += 2) {
            builder.param(params[index], params[index + 1]);
        }
        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return readJson(result).path("data");
    }

    private Set<String> collectFieldValues(JsonNode items, String fieldName) {
        Set<String> values = new LinkedHashSet<>();
        if (items == null || !items.isArray()) {
            return values;
        }
        for (JsonNode item : items) {
            String value = item.path(fieldName).asText();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private String register(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "%s"
                                }
                                """.formatted(email, PASSWORD, displayName)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
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

    private void inviteMember(String token, String orgId, String email, String role) throws Exception {
        mockMvc.perform(post("/api/v1/orgs/" + orgId + "/invites")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "role": "%s"
                                }
                                """.formatted(email, role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private void acceptFirstIncomingInvite(String token) throws Exception {
        String inviteId = firstIncomingInviteId(token);
        mockMvc.perform(post("/api/v1/orgs/invites/" + inviteId + "/respond")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"response\":\"ACCEPT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    private String firstIncomingInviteId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/orgs/invites/incoming")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].inviteId").exists())
                .andReturn();
        return readJson(result).at("/data/0/inviteId").asText();
    }

    private String findMemberId(String token, String orgId, String email) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/orgs/" + orgId + "/members")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode item : readJson(result).at("/data")) {
            if (email.equals(item.path("email").asText()) || email.equals(item.path("userEmail").asText())) {
                return item.path("id").asText();
            }
        }
        throw new IllegalStateException("member not found: " + email);
    }

    private void sendMail(String token, String toEmail, String subject, String body, String idempotencyKey) throws Exception {
        mockMvc.perform(post("/api/v1/mails/send")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toEmail": "%s",
                                  "subject": "%s",
                                  "body": "%s",
                                  "idempotencyKey": "%s",
                                  "labels": []
                                }
                                """.formatted(toEmail, subject, body, idempotencyKey)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String createSearchPreset(String token, String keyword) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/search-presets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "v97 pinned preset",
                                  "keyword": "%s",
                                  "folder": "INBOX",
                                  "unread": true
                                }
                                """.formatted(keyword)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String createNote(String token, String title, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/docs/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "content": "%s"
                                }
                                """.formatted(title, content)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void createDocsShare(String token, String noteId, String email, String permission) throws Exception {
        mockMvc.perform(post("/api/v1/docs/notes/" + noteId + "/shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "collaboratorEmail": "%s",
                                  "permission": "%s"
                                }
                                """.formatted(email, permission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value(permission));
    }

    private void createComment(String token, String noteId, String excerpt, String content) throws Exception {
        mockMvc.perform(post("/api/v1/docs/notes/" + noteId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "excerpt": "%s",
                                  "content": "%s"
                                }
                                """.formatted(excerpt, content)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.excerpt").value(excerpt));
    }

    private String productAccessPayload(String... changes) {
        StringBuilder builder = new StringBuilder("{\"products\":[");
        for (int index = 0; index < changes.length; index += 2) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append("{\"productKey\":\"")
                    .append(changes[index])
                    .append("\",\"accessState\":\"")
                    .append(changes[index + 1])
                    .append("\"}");
        }
        return builder.append("]}").toString();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record OrgScenario(
            String suffix,
            String orgId,
            String ownerToken,
            String memberToken,
            String ownerEmail,
            String memberEmail,
            String memberId
    ) {
    }
}
