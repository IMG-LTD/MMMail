package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendV21DocsSheetsRuntimeBridgeTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void v21DocsShouldUseRuntimeStateForCommunityPaths() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerToken = register("v21-docs-owner-" + suffix + "@mmmail.local", "V21 Docs Owner");
        String collaboratorEmail = "v21-docs-collab-" + suffix + "@mmmail.local";
        register(collaboratorEmail, "V21 Docs Collaborator");
        String noteId = createV21Doc(ownerToken, "V21 Docs", "Initial body");

        assertV21DocListAndDetail(ownerToken, noteId);
        updateV21Doc(ownerToken, noteId);
        createV21DocComment(ownerToken, noteId);
        shareV21Doc(ownerToken, noteId, collaboratorEmail);
    }

    @Test
    void v21SheetsShouldUseRuntimeStateForCommunityPaths() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String token = register("v21-sheets-owner-" + suffix + "@mmmail.local", "V21 Sheets Owner");
        String workbookId = createV21Workbook(token, "V21 Workbook");

        mockMvc.perform(get("/api/v2/sheets").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(workbookId));

        MvcResult detail = mockMvc.perform(get("/api/v2/sheets/" + workbookId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("V21 Workbook"))
                .andExpect(jsonPath("$.data.activeSheetId").isNotEmpty())
                .andReturn();
        String sheetId = readJson(detail).at("/data/activeSheetId").asText();

        patchV21Workbook(token, workbookId, sheetId);
    }

    @Test
    void v21DocsSheetsShouldExposeExplicitUnsupportedAndPremiumBoundaries() throws Exception {
        String token = register("v21-docs-sheets-gate-" + System.nanoTime() + "@mmmail.local", "V21 Gate Owner");
        String noteId = createV21Doc(token, "V21 Gated Docs", "Gated body");
        assertPremiumGate(token, "/api/v2/docs/" + noteId + "/versions");

        String workbookId = createV21Workbook(token, "V21 Gated Workbook");
        assertInvalidSheetsJsonImport(token, workbookId);
        assertPremiumSheetsCleaningRulesBoundary(token, workbookId);
        assertPremiumSheetsInsightsBoundary(token, workbookId);
    }

    private void assertV21DocListAndDetail(String token, String noteId) throws Exception {
        mockMvc.perform(get("/api/v2/docs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(noteId));

        mockMvc.perform(get("/api/v2/docs/" + noteId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("V21 Docs"))
                .andExpect(jsonPath("$.data.currentVersion").value(1));
    }

    private void updateV21Doc(String token, String noteId) throws Exception {
        mockMvc.perform(patch("/api/v2/docs/" + noteId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "V21 Docs Updated",
                                  "content": "Updated body",
                                  "currentVersion": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("V21 Docs Updated"))
                .andExpect(jsonPath("$.data.currentVersion").value(2));
    }

    private void createV21DocComment(String token, String noteId) throws Exception {
        mockMvc.perform(post("/api/v2/docs/" + noteId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "excerpt": "Initial",
                                  "content": "Please review this v2 doc"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Please review this v2 doc"));

        mockMvc.perform(get("/api/v2/docs/" + noteId + "/comments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    private void shareV21Doc(String token, String noteId, String collaboratorEmail) throws Exception {
        mockMvc.perform(post("/api/v2/docs/" + noteId + "/share")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "collaboratorEmail": "%s",
                                  "permission": "EDIT"
                                }
                                """.formatted(collaboratorEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value("EDIT"));
    }

    private void patchV21Workbook(String token, String workbookId, String sheetId) throws Exception {
        mockMvc.perform(patch("/api/v2/sheets/" + workbookId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentVersion": 1,
                                  "sheetId": "%s",
                                  "edits": [
                                    {
                                      "rowIndex": 0,
                                      "colIndex": 0,
                                      "value": "42"
                                    }
                                  ]
                                }
                                """.formatted(sheetId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentVersion").value(2))
                .andExpect(jsonPath("$.data.grid[0][0]").value("42"));
    }

    private void assertPremiumGate(String token, String path) throws Exception {
        mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
    }

    private void assertInvalidSheetsJsonImport(String token, String workbookId) throws Exception {
        mockMvc.perform(post("/api/v2/sheets/" + workbookId + "/imports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "format": "CSV",
                                  "content": "a,b\\n1,2"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    private void assertPremiumSheetsCleaningRulesBoundary(String token, String workbookId) throws Exception {
        mockMvc.perform(post("/api/v2/sheets/" + workbookId + "/cleaning-rules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
    }

    private void assertPremiumSheetsInsightsBoundary(String token, String workbookId) throws Exception {
        mockMvc.perform(get("/api/v2/sheets/" + workbookId + "/insights")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
    }

    private String createV21Doc(String token, String title, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/docs")
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

    private String createV21Workbook(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/sheets")
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
        return readJson(result).at("/data/id").asText();
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

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
