package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrgBusinessTeamSpaceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void teamSpaceAclVersionTrashAndActivityFlowShouldWork() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v65-biz-owner-%s@mmmail.local".formatted(suffix);
        String memberEmail = "v65-biz-member-%s@mmmail.local".formatted(suffix);

        String ownerToken = register(ownerEmail, "Password@123", "V65 Biz Owner");
        String memberToken = register(memberEmail, "Password@123", "V65 Biz Member");
        String orgId = createOrganization(ownerToken, "V65 Business Ops");

        inviteMember(ownerToken, orgId, memberEmail, "MEMBER");
        acceptFirstIncomingInvite(memberToken);

        String teamSpaceId = createTeamSpace(ownerToken, orgId, "Operations", "Drive business workspace");

        mockMvc.perform(get("/api/v1/orgs/" + orgId + "/team-spaces")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        String teamSpaceMemberId = addTeamSpaceMember(ownerToken, orgId, teamSpaceId, memberEmail, "VIEWER");

        mockMvc.perform(get("/api/v1/orgs/" + orgId + "/team-spaces")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(teamSpaceId))
                .andExpect(jsonPath("$.data[0].currentAccessRole").value("VIEWER"))
                .andExpect(jsonPath("$.data[0].canWrite").value(false))
                .andExpect(jsonPath("$.data[0].canManage").value(false));

        mockMvc.perform(get("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/members")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        String folderId = createFolder(ownerToken, orgId, teamSpaceId, "Planning", null);
        String fileId = uploadFile(ownerToken, orgId, teamSpaceId, folderId, "team-plan.txt", "roadmap-v65-v1");

        mockMvc.perform(get("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/items")
                        .header("Authorization", "Bearer " + memberToken)
                        .param("parentId", folderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(fileId))
                .andExpect(jsonPath("$.data[0].name").value("team-plan.txt"));

        MockMultipartFile deniedUpload = new MockMultipartFile(
                "file",
                "blocked.txt",
                "text/plain",
                "blocked".getBytes()
        );
        mockMvc.perform(multipart("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/files/upload")
                        .file(deniedUpload)
                        .param("parentId", folderId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        MvcResult downloadV1 = mockMvc.perform(get("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/files/" + fileId + "/download")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("team-plan.txt")))
                .andReturn();
        assertThat(downloadV1.getResponse().getContentAsString()).isEqualTo("roadmap-v65-v1");

        updateTeamSpaceMemberRole(ownerToken, orgId, teamSpaceId, teamSpaceMemberId, "EDITOR");

        mockMvc.perform(get("/api/v1/orgs/" + orgId + "/team-spaces")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].currentAccessRole").value("EDITOR"))
                .andExpect(jsonPath("$.data[0].canWrite").value(true));

        uploadFileVersion(memberToken, orgId, teamSpaceId, fileId, "team-plan-v2.txt", "roadmap-v65-v2");

        MvcResult versionsResult = mockMvc.perform(get("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/files/" + fileId + "/versions")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].versionNo").value(1))
                .andReturn();
        String versionId = readJson(versionsResult).at("/data/0/id").asText();

        mockMvc.perform(post("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/files/" + fileId + "/versions/" + versionId + "/restore")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(fileId));

        MvcResult downloadRestored = mockMvc.perform(get("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/files/" + fileId + "/download")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(downloadRestored.getResponse().getContentAsString()).isEqualTo("roadmap-v65-v1");

        mockMvc.perform(delete("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/items/" + fileId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/trash")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(fileId));

        mockMvc.perform(post("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/trash/" + fileId + "/restore")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(fileId));

        mockMvc.perform(get("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/items")
                        .header("Authorization", "Bearer " + memberToken)
                        .param("parentId", folderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(fileId));

        mockMvc.perform(get("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/activity")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("category", "VERSION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").isNotEmpty())
                .andExpect(jsonPath("$.data[0].teamSpaceId").value(teamSpaceId));

        mockMvc.perform(get("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/activity")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("category", "TRASH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").isNotEmpty())
                .andExpect(jsonPath("$.data[0].teamSpaceId").value(teamSpaceId));
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

    private String createOrganization(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orgs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(name)))
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

    private String createTeamSpace(String token, String orgId, String name, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orgs/" + orgId + "/team-spaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "%s",
                                  "storageLimitMb": 2048
                                }
                                """.formatted(name, description)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(name))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String addTeamSpaceMember(String token, String orgId, String teamSpaceId, String memberEmail, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userEmail": "%s",
                                  "role": "%s"
                                }
                                """.formatted(memberEmail, role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userEmail").value(memberEmail))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void updateTeamSpaceMemberRole(String token, String orgId, String teamSpaceId, String memberId, String role) throws Exception {
        mockMvc.perform(put("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/members/" + memberId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"%s\"}".formatted(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value(role));
    }

    private String createFolder(String token, String orgId, String teamSpaceId, String name, String parentId) throws Exception {
        String parentValue = parentId == null ? "null" : '"' + parentId + '"';
        MvcResult result = mockMvc.perform(post("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/folders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "parentId": %s
                                }
                                """.formatted(name, parentValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemType").value("FOLDER"))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String uploadFile(String token, String orgId, String teamSpaceId, String parentId, String fileName, String content)
            throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                content.getBytes()
        );
        MvcResult result = mockMvc.perform(multipart("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/files/upload")
                        .file(multipartFile)
                        .param("parentId", parentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(fileName))
                .andExpect(jsonPath("$.data.itemType").value("FILE"))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void uploadFileVersion(String token, String orgId, String teamSpaceId, String itemId, String fileName, String content)
            throws Exception {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                fileName,
                "text/plain",
                content.getBytes()
        );
        mockMvc.perform(multipart("/api/v1/orgs/" + orgId + "/team-spaces/" + teamSpaceId + "/files/" + itemId + "/versions")
                        .file(multipartFile)
                        .header("Authorization", "Bearer " + token)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(itemId));
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
