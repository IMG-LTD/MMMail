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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocsOrgAccessIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final String TWO_FACTOR_LEVEL_ALL = "ALL";
    private static final String AUTH_ISSUER = "MMMail Security";
    private static final String AUTH_SECRET = "JBSWY3DPEHPK3PXP";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void docsEndpointsShouldRejectDisabledProductInActiveOrgScope() throws Exception {
        OrgScenario scenario = createScenario("v111-docs-disabled");
        String noteId = createNote(scenario.memberToken(), scenario.orgId(), "Org docs note", "draft body");

        updateMemberDocsAccess(scenario.ownerToken(), scenario.orgId(), scenario.memberId(), "DISABLED");

        createNoteRequest(scenario.memberToken(), scenario.orgId(), "Blocked note", "blocked body")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(30045));

        createCommentRequest(scenario.memberToken(), scenario.orgId(), noteId, "draft", "blocked comment")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(30045));

        getCollaborationRequest(scenario.memberToken(), scenario.orgId(), noteId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(30045));
    }

    @Test
    void docsWriteEndpointsShouldHonorTwoFactorPolicyInActiveOrgScope() throws Exception {
        OrgScenario scenario = createScenario("v111-docs-2fa");
        String noteId = createNote(scenario.memberToken(), scenario.orgId(), "Protected note", "protected body");

        updatePolicy(scenario.ownerToken(), scenario.orgId(), 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.twoFactorEnforcementLevel").value(TWO_FACTOR_LEVEL_ALL))
                .andExpect(jsonPath("$.data.twoFactorGracePeriodDays").value(0));

        createNoteRequest(scenario.memberToken(), scenario.orgId(), "Blocked by policy", "body")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(30046));

        createCommentRequest(scenario.memberToken(), scenario.orgId(), noteId, "body", "blocked comment")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(30046));

        createAuthenticatorEntry(scenario.memberToken(), scenario.suffix() + "-member");

        createNoteRequest(scenario.memberToken(), scenario.orgId(), "Recovered note", "body")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Recovered note"));

        createCommentRequest(scenario.memberToken(), scenario.orgId(), noteId, "body", "recovered comment")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("recovered comment"));
    }

    private OrgScenario createScenario(String prefix) throws Exception {
        String suffix = prefix + "-" + System.nanoTime();
        String ownerEmail = suffix + "-owner@mmmail.local";
        String memberEmail = suffix + "-member@mmmail.local";
        String ownerToken = register(ownerEmail, "V111 Owner");
        String memberToken = register(memberEmail, "V111 Member");
        String orgId = createOrganization(ownerToken, "V111 Org " + suffix);
        inviteMember(ownerToken, orgId, memberEmail, "MEMBER");
        acceptFirstIncomingInvite(memberToken);
        String memberId = findMemberId(ownerToken, orgId, memberEmail);
        return new OrgScenario(suffix, orgId, ownerToken, memberToken, memberEmail, memberId);
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
        JsonNode members = readJson(mockMvc.perform(get("/api/v1/orgs/" + orgId + "/members")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()).at("/data");
        for (JsonNode item : members) {
            if (email.equals(item.path("email").asText()) || email.equals(item.path("userEmail").asText())) {
                return item.path("id").asText();
            }
        }
        throw new IllegalStateException("member not found: " + email);
    }

    private void updateMemberDocsAccess(String token, String orgId, String memberId, String accessState) throws Exception {
        mockMvc.perform(put("/api/v1/orgs/" + orgId + "/admin-console/product-access/" + memberId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "products": [
                                    {
                                      "productKey": "DOCS",
                                      "accessState": "%s"
                                    }
                                  ]
                                }
                                """.formatted(accessState)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private org.springframework.test.web.servlet.ResultActions updatePolicy(String token, String orgId, int graceDays) throws Exception {
        return mockMvc.perform(put("/api/v1/orgs/" + orgId + "/policy")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "twoFactorEnforcementLevel": "%s",
                          "twoFactorGracePeriodDays": %d
                        }
                        """.formatted(TWO_FACTOR_LEVEL_ALL, graceDays)));
    }

    private void createAuthenticatorEntry(String token, String accountName) throws Exception {
        mockMvc.perform(post("/api/v1/authenticator/entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "issuer": "%s",
                                  "accountName": "%s",
                                  "secretCiphertext": "%s",
                                  "algorithm": "SHA1",
                                  "digits": 6,
                                  "periodSeconds": 30
                                }
                                """.formatted(AUTH_ISSUER, accountName, AUTH_SECRET)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String createNote(String token, String orgId, String title, String content) throws Exception {
        MvcResult result = createNoteRequest(token, orgId, title, content)
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private org.springframework.test.web.servlet.ResultActions createNoteRequest(String token, String orgId, String title, String content) throws Exception {
        return mockMvc.perform(withOrgHeader(post("/api/v1/docs/notes"), token, orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "%s",
                          "content": "%s"
                        }
                        """.formatted(title, content)));
    }

    private org.springframework.test.web.servlet.ResultActions createCommentRequest(
            String token,
            String orgId,
            String noteId,
            String excerpt,
            String content
    ) throws Exception {
        return mockMvc.perform(withOrgHeader(post("/api/v1/docs/notes/" + noteId + "/comments"), token, orgId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "excerpt": "%s",
                          "content": "%s"
                        }
                        """.formatted(excerpt, content)));
    }

    private org.springframework.test.web.servlet.ResultActions getCollaborationRequest(String token, String orgId, String noteId) throws Exception {
        return mockMvc.perform(withOrgHeader(get("/api/v1/docs/notes/" + noteId + "/collaboration"), token, orgId));
    }

    private MockHttpServletRequestBuilder withOrgHeader(MockHttpServletRequestBuilder builder, String token, String orgId) {
        return builder
                .header("Authorization", "Bearer " + token)
                .header("X-MMMAIL-ORG-ID", orgId);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record OrgScenario(
            String suffix,
            String orgId,
            String ownerToken,
            String memberToken,
            String memberEmail,
            String memberId
    ) {
    }
}
