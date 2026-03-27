package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
class SheetsSharingVersionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sheetsSharingPermissionAndVersionRestoreShouldWork() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v89-sheets-owner-%s@mmmail.local".formatted(suffix);
        String collaboratorEmail = "v89-sheets-collab-%s@mmmail.local".formatted(suffix);
        String ownerToken = register(ownerEmail, "Password@123", "V89 Sheets Owner");
        String collaboratorToken = register(collaboratorEmail, "Password@123", "V89 Sheets Collaborator");

        JsonNode created = createWorkbook(ownerToken, "v89-shared-ledger");
        String workbookId = created.path("id").asText();

        JsonNode ownerSeeded = updateCells(
                ownerToken,
                workbookId,
                created.path("currentVersion").asInt(),
                new EditInput(0, 0, "Owner value"),
                new EditInput(0, 1, "100")
        );
        String shareId = createShare(ownerToken, workbookId, collaboratorEmail, "VIEW");

        mockMvc.perform(get("/api/v1/sheets/workbooks/incoming-shares")
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].shareId").value(shareId))
                .andExpect(jsonPath("$.data[0].permission").value("VIEW"))
                .andExpect(jsonPath("$.data[0].responseStatus").value("NEEDS_ACTION"))
                .andExpect(jsonPath("$.data[0].ownerEmail").value(ownerEmail));

        mockMvc.perform(get("/api/v1/sheets/workbooks/" + workbookId)
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(30031));

        respondShare(collaboratorToken, shareId, "ACCEPT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.responseStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.permission").value("VIEW"));

        mockMvc.perform(get("/api/v1/sheets/workbooks")
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(workbookId))
                .andExpect(jsonPath("$.data[0].scope").value("SHARED"))
                .andExpect(jsonPath("$.data[0].permission").value("VIEW"))
                .andExpect(jsonPath("$.data[0].ownerEmail").value(ownerEmail))
                .andExpect(jsonPath("$.data[0].canEdit").value(false));

        mockMvc.perform(get("/api/v1/sheets/workbooks/" + workbookId)
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value("VIEW"))
                .andExpect(jsonPath("$.data.sheetCount").value(1))
                .andExpect(jsonPath("$.data.canEdit").value(false))
                .andExpect(jsonPath("$.data.canManageShares").value(false))
                .andExpect(jsonPath("$.data.canRestoreVersions").value(false));

        mockMvc.perform(put("/api/v1/sheets/workbooks/" + workbookId + "/cells")
                        .header("Authorization", "Bearer " + collaboratorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildEditPayload(ownerSeeded.path("currentVersion").asInt(), new EditInput(0, 0, "Blocked view")).toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(10003));

        updateSharePermission(ownerToken, workbookId, shareId, "EDIT")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value("EDIT"))
                .andExpect(jsonPath("$.data.responseStatus").value("ACCEPTED"));

        mockMvc.perform(get("/api/v1/sheets/workbooks/incoming-shares")
                        .header("Authorization", "Bearer " + collaboratorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].permission").value("EDIT"))
                .andExpect(jsonPath("$.data[0].responseStatus").value("ACCEPTED"));

        JsonNode collaboratorSaved = updateCells(
                collaboratorToken,
                workbookId,
                ownerSeeded.path("currentVersion").asInt(),
                new EditInput(0, 0, "Collaborator value")
        );
        assertThat(collaboratorSaved.path("currentVersion").asInt()).isEqualTo(3);
        assertThat(collaboratorSaved.path("permission").asText()).isEqualTo("EDIT");
        assertThat(collaboratorSaved.path("canEdit").asBoolean()).isTrue();
        assertThat(collaboratorSaved.path("grid").get(0).get(0).asText()).isEqualTo("Collaborator value");

        JsonNode versions = listVersions(ownerToken, workbookId);
        assertThat(versions).hasSize(3);
        assertThat(versions.get(0).path("versionNo").asInt()).isEqualTo(3);
        assertThat(versions.get(0).path("createdByEmail").asText()).isEqualTo(collaboratorEmail);
        assertThat(versions.get(0).path("sourceEvent").asText()).isEqualTo("SHEETS_WORKBOOK_UPDATE_CELLS");

        String versionTwoId = findVersionIdByNo(versions, 2);
        JsonNode restored = restoreVersion(ownerToken, workbookId, versionTwoId);
        assertThat(restored.path("currentVersion").asInt()).isEqualTo(4);
        assertThat(restored.path("grid").get(0).get(0).asText()).isEqualTo("Owner value");
        assertThat(restored.path("permission").asText()).isEqualTo("OWNER");
        assertThat(restored.path("scope").asText()).isEqualTo("OWNED");
        assertThat(restored.path("canManageShares").asBoolean()).isTrue();
        assertThat(restored.path("canRestoreVersions").asBoolean()).isTrue();

        JsonNode restoredVersions = listVersions(ownerToken, workbookId);
        assertThat(restoredVersions).hasSize(4);
        assertThat(restoredVersions.get(0).path("versionNo").asInt()).isEqualTo(4);
        assertThat(restoredVersions.get(0).path("sourceEvent").asText()).isEqualTo("SHEETS_WORKBOOK_VERSION_RESTORE");
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
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/accessToken").asText();
    }

    private JsonNode createWorkbook(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sheets/workbooks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "rowCount": 4,
                                  "colCount": 4
                                }
                                """.formatted(title)))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private String createShare(String token, String workbookId, String targetEmail, String permission) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sheets/workbooks/" + workbookId + "/shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetEmail": "%s",
                                  "permission": "%s"
                                }
                                """.formatted(targetEmail, permission)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/shareId").asText();
    }

    private org.springframework.test.web.servlet.ResultActions respondShare(
            String token,
            String shareId,
            String response
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/sheets/workbooks/incoming-shares/" + shareId + "/respond")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "response": "%s"
                        }
                        """.formatted(response)));
    }

    private org.springframework.test.web.servlet.ResultActions updateSharePermission(
            String token,
            String workbookId,
            String shareId,
            String permission
    ) throws Exception {
        return mockMvc.perform(put("/api/v1/sheets/workbooks/" + workbookId + "/shares/" + shareId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "permission": "%s"
                        }
                        """.formatted(permission)));
    }

    private JsonNode updateCells(String token, String workbookId, int currentVersion, EditInput... edits) throws Exception {
        MvcResult result = mockMvc.perform(put("/api/v1/sheets/workbooks/" + workbookId + "/cells")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildEditPayload(currentVersion, edits).toString()))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private JsonNode listVersions(String token, String workbookId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/sheets/workbooks/" + workbookId + "/versions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private JsonNode restoreVersion(String token, String workbookId, String versionId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sheets/workbooks/" + workbookId + "/versions/" + versionId + "/restore")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private ObjectNode buildEditPayload(int currentVersion, EditInput... edits) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("currentVersion", currentVersion);
        ArrayNode editArray = payload.putArray("edits");
        for (EditInput edit : edits) {
            editArray.addObject()
                    .put("rowIndex", edit.rowIndex())
                    .put("colIndex", edit.colIndex())
                    .put("value", edit.value());
        }
        return payload;
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private String findVersionIdByNo(JsonNode versions, int versionNo) {
        for (JsonNode version : versions) {
            if (version.path("versionNo").asInt() == versionNo) {
                return version.path("versionId").asText();
            }
        }
        throw new IllegalStateException("Version not found: " + versionNo);
    }

    private record EditInput(int rowIndex, int colIndex, String value) {
    }
}
