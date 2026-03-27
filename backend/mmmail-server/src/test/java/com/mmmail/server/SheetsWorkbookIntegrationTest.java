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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SheetsWorkbookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sheetsWorkbookFormulaLifecycleShouldWork() throws Exception {
        String token = register("v78-sheets-formula-owner");
        JsonNode created = createWorkbook(token, "v78-formula-workbook");
        String workbookId = created.path("id").asText();

        JsonNode updated = updateCells(
                token,
                workbookId,
                created.path("currentVersion").asInt(),
                new EditInput(0, 0, "10"),
                new EditInput(0, 1, "20"),
                new EditInput(0, 2, "=SUM(A1:B1)"),
                new EditInput(1, 0, "=AVERAGE(A1:B1)")
        );

        assertThat(updated.path("currentVersion").asInt()).isEqualTo(2);
        assertThat(updated.path("formulaCellCount").asInt()).isEqualTo(2);
        assertThat(updated.path("computedErrorCount").asInt()).isZero();
        assertThat(updated.path("grid").get(0).get(2).asText()).isEqualTo("=SUM(A1:B1)");
        assertThat(updated.path("computedGrid").get(0).get(2).asText()).isEqualTo("30");
        assertThat(updated.path("computedGrid").get(1).get(0).asText()).isEqualTo("15");

        JsonNode reopened = getWorkbook(token, workbookId);
        assertThat(reopened.path("formulaCellCount").asInt()).isEqualTo(2);
        assertThat(reopened.path("computedGrid").get(0).get(2).asText()).isEqualTo("30");
    }

    @Test
    void sheetsWorkbookImportExportAndVersionConflictShouldWork() throws Exception {
        String token = register("v78-sheets-import-owner");
        JsonNode imported = importWorkbook(token, "v78-imported-workbook", "v78-sheet.csv", "10,20,=SUM(A1:B1)\n");
        String workbookId = imported.path("id").asText();

        assertThat(imported.path("rowCount").asInt()).isEqualTo(1);
        assertThat(imported.path("colCount").asInt()).isEqualTo(3);
        assertThat(imported.path("formulaCellCount").asInt()).isEqualTo(1);
        assertThat(imported.path("computedGrid").get(0).get(2).asText()).isEqualTo("30");

        JsonNode csvExport = exportWorkbook(token, workbookId, "CSV");
        assertThat(csvExport.path("format").asText()).isEqualTo("CSV");
        assertThat(csvExport.path("content").asText()).contains("10,20,30");

        JsonNode jsonExport = exportWorkbook(token, workbookId, "JSON");
        JsonNode exportPayload = objectMapper.readTree(jsonExport.path("content").asText());
        assertThat(exportPayload.path("grid").get(0).get(2).asText()).isEqualTo("=SUM(A1:B1)");
        assertThat(exportPayload.path("computedGrid").get(0).get(2).asText()).isEqualTo("30");
        assertThat(exportPayload.path("formulaCellCount").asInt()).isEqualTo(1);

        JsonNode saved = updateCells(token, workbookId, imported.path("currentVersion").asInt(), new EditInput(0, 0, "12"));
        assertThat(saved.path("currentVersion").asInt()).isEqualTo(2);

        assertVersionConflict(token, workbookId);
    }

    private void assertVersionConflict(String token, String workbookId) throws Exception {
        mockMvc.perform(put("/api/v1/sheets/workbooks/" + workbookId + "/cells")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentVersion": 1,
                                  "edits": [
                                    {
                                      "rowIndex": 0,
                                      "colIndex": 1,
                                      "value": "stale-update"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(30032));
    }

    private String register(String prefix) throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String email = prefix + "-" + suffix + "@mmmail.local";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password@123",
                                  "displayName": "Sheets Tester"
                                }
                                """.formatted(email)))
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

    private JsonNode importWorkbook(String token, String title, String fileName, String csvContent) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", fileName, "text/csv", csvContent.getBytes());
        MvcResult result = mockMvc.perform(multipart("/api/v1/sheets/workbooks/import")
                        .file(file)
                        .param("title", title)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private JsonNode getWorkbook(String token, String workbookId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/sheets/workbooks/" + workbookId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private JsonNode updateCells(String token, String workbookId, int currentVersion, EditInput... edits) throws Exception {
        JsonNode payload = buildEditPayload(currentVersion, edits);
        MvcResult result = mockMvc.perform(put("/api/v1/sheets/workbooks/" + workbookId + "/cells")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private JsonNode exportWorkbook(String token, String workbookId, String format) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/sheets/workbooks/" + workbookId + "/export")
                        .header("Authorization", "Bearer " + token)
                        .param("format", format))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private JsonNode buildEditPayload(int currentVersion, EditInput... edits) {
        JsonNode root = objectMapper.createObjectNode().put("currentVersion", currentVersion);
        JsonNode editArray = ((com.fasterxml.jackson.databind.node.ObjectNode) root).putArray("edits");
        for (EditInput edit : edits) {
            ((com.fasterxml.jackson.databind.node.ArrayNode) editArray)
                    .addObject()
                    .put("rowIndex", edit.rowIndex())
                    .put("colIndex", edit.colIndex())
                    .put("value", edit.value());
        }
        return root;
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private record EditInput(int rowIndex, int colIndex, String value) {
    }
}
