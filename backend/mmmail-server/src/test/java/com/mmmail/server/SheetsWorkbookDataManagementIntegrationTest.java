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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SheetsWorkbookDataManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sheetsWorkbookSortFreezeAndExportShouldWork() throws Exception {
        String token = register("v85-sheets-data-owner");
        JsonNode created = createWorkbook(token, "v85-data-tools", 4, 3);
        String workbookId = created.path("id").asText();
        String sheetId = created.path("activeSheetId").asText();

        JsonNode updated = updateCells(
                token,
                workbookId,
                created.path("currentVersion").asInt(),
                new EditInput(0, 0, "Name"),
                new EditInput(0, 1, "Score"),
                new EditInput(0, 2, "Note"),
                new EditInput(1, 0, "Beta"),
                new EditInput(1, 1, "20"),
                new EditInput(1, 2, "Second"),
                new EditInput(2, 0, "Alpha"),
                new EditInput(2, 1, "10"),
                new EditInput(2, 2, "First"),
                new EditInput(3, 0, "Gamma"),
                new EditInput(3, 1, "30"),
                new EditInput(3, 2, "Third")
        );

        JsonNode sorted = sortSheet(
                token,
                workbookId,
                sheetId,
                updated.path("currentVersion").asInt(),
                1,
                "ASC",
                true
        );
        JsonNode sortedSheet = sheetById(sorted, sheetId);
        assertThat(sorted.path("currentVersion").asInt()).isEqualTo(3);
        assertThat(sortedSheet.path("grid").get(0).get(0).asText()).isEqualTo("Name");
        assertThat(sortedSheet.path("grid").get(1).get(0).asText()).isEqualTo("Alpha");
        assertThat(sortedSheet.path("grid").get(2).get(0).asText()).isEqualTo("Beta");
        assertThat(sortedSheet.path("grid").get(3).get(0).asText()).isEqualTo("Gamma");

        JsonNode frozen = freezeSheet(
                token,
                workbookId,
                sheetId,
                sorted.path("currentVersion").asInt(),
                1,
                1
        );
        JsonNode frozenSheet = sheetById(frozen, sheetId);
        assertThat(frozen.path("currentVersion").asInt()).isEqualTo(4);
        assertThat(frozenSheet.path("frozenRowCount").asInt()).isEqualTo(1);
        assertThat(frozenSheet.path("frozenColCount").asInt()).isEqualTo(1);

        JsonNode reopened = getWorkbook(token, workbookId);
        JsonNode reopenedSheet = sheetById(reopened, sheetId);
        assertThat(reopenedSheet.path("frozenRowCount").asInt()).isEqualTo(1);
        assertThat(reopenedSheet.path("frozenColCount").asInt()).isEqualTo(1);
        assertThat(reopenedSheet.path("grid").get(1).get(0).asText()).isEqualTo("Alpha");

        JsonNode jsonExport = exportWorkbook(token, workbookId, "JSON");
        JsonNode payload = objectMapper.readTree(jsonExport.path("content").asText());
        JsonNode exportedSheet = payload.path("sheets").get(0);
        assertThat(payload.path("currentVersion").asInt()).isEqualTo(4);
        assertThat(exportedSheet.path("frozenRowCount").asInt()).isEqualTo(1);
        assertThat(exportedSheet.path("frozenColCount").asInt()).isEqualTo(1);
        assertThat(exportedSheet.path("grid").get(1).get(0).asText()).isEqualTo("Alpha");
    }

    @Test
    void sheetsWorkbookSortDescWithoutHeaderShouldWork() throws Exception {
        String token = register("v85-sheets-sort-desc-owner");
        JsonNode created = createWorkbook(token, "v85-sort-desc", 3, 2);
        String workbookId = created.path("id").asText();
        String sheetId = created.path("activeSheetId").asText();

        JsonNode updated = updateCells(
                token,
                workbookId,
                created.path("currentVersion").asInt(),
                new EditInput(0, 0, "Alpha"),
                new EditInput(1, 0, "Gamma"),
                new EditInput(2, 0, "Beta")
        );

        JsonNode sorted = sortSheet(
                token,
                workbookId,
                sheetId,
                updated.path("currentVersion").asInt(),
                0,
                "DESC",
                false
        );
        JsonNode sortedSheet = sheetById(sorted, sheetId);
        assertThat(sorted.path("currentVersion").asInt()).isEqualTo(3);
        assertThat(sortedSheet.path("grid").get(0).get(0).asText()).isEqualTo("Gamma");
        assertThat(sortedSheet.path("grid").get(1).get(0).asText()).isEqualTo("Beta");
        assertThat(sortedSheet.path("grid").get(2).get(0).asText()).isEqualTo("Alpha");
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

    private JsonNode createWorkbook(String token, String title, int rowCount, int colCount) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sheets/workbooks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "rowCount": %d,
                                  "colCount": %d
                                }
                                """.formatted(title, rowCount, colCount)))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private JsonNode updateCells(String token, String workbookId, int currentVersion, EditInput... edits) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("currentVersion", currentVersion);
        ArrayNode editArray = payload.putArray("edits");
        for (EditInput edit : edits) {
            editArray.addObject()
                    .put("rowIndex", edit.rowIndex())
                    .put("colIndex", edit.colIndex())
                    .put("value", edit.value());
        }
        MvcResult result = mockMvc.perform(put("/api/v1/sheets/workbooks/" + workbookId + "/cells")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private JsonNode sortSheet(
            String token,
            String workbookId,
            String sheetId,
            int currentVersion,
            int columnIndex,
            String direction,
            boolean includeHeader
    ) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("currentVersion", currentVersion);
        payload.put("columnIndex", columnIndex);
        payload.put("direction", direction);
        payload.put("includeHeader", includeHeader);
        MvcResult result = mockMvc.perform(put("/api/v1/sheets/workbooks/" + workbookId + "/sheets/" + sheetId + "/sort")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private JsonNode freezeSheet(
            String token,
            String workbookId,
            String sheetId,
            int currentVersion,
            int frozenRowCount,
            int frozenColCount
    ) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("currentVersion", currentVersion);
        payload.put("frozenRowCount", frozenRowCount);
        payload.put("frozenColCount", frozenColCount);
        MvcResult result = mockMvc.perform(put("/api/v1/sheets/workbooks/" + workbookId + "/sheets/" + sheetId + "/freeze")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
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

    private JsonNode exportWorkbook(String token, String workbookId, String format) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/sheets/workbooks/" + workbookId + "/export")
                        .header("Authorization", "Bearer " + token)
                        .param("format", format))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private JsonNode sheetById(JsonNode workbookDetail, String sheetId) {
        for (JsonNode sheet : workbookDetail.path("sheets")) {
            if (sheet.path("id").asText().equals(sheetId)) {
                return sheet;
            }
        }
        throw new IllegalStateException("Sheet not found: " + sheetId);
    }

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private record EditInput(int rowIndex, int colIndex, String value) {
    }
}
