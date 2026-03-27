package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.server.mapper.SheetsWorkbookMapper;
import com.mmmail.server.model.entity.SheetsWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SheetsWorkbookMultiSheetIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SheetsWorkbookMapper sheetsWorkbookMapper;

    @Test
    void sheetsWorkbookMultiSheetLifecycleShouldWork() throws Exception {
        String token = register("v84-sheets-tabs-owner");
        JsonNode created = createWorkbook(token, "v84-tabs-workbook");
        String workbookId = created.path("id").asText();
        String firstSheetId = created.path("activeSheetId").asText();

        assertThat(created.path("sheets")).hasSize(1);
        assertThat(created.path("currentVersion").asInt()).isEqualTo(1);

        JsonNode secondSheetCreated = createSheet(token, workbookId, "{}");
        String secondSheetId = secondSheetCreated.path("activeSheetId").asText();

        assertThat(secondSheetCreated.path("sheets")).hasSize(2);
        assertThat(secondSheetCreated.path("currentVersion").asInt()).isEqualTo(2);
        assertThat(sheetById(secondSheetCreated, secondSheetId).path("name").asText()).isEqualTo("Sheet 2");

        JsonNode renamed = renameSheet(token, workbookId, secondSheetId, "Summary");
        assertThat(renamed.path("currentVersion").asInt()).isEqualTo(3);
        assertThat(sheetById(renamed, secondSheetId).path("name").asText()).isEqualTo("Summary");

        JsonNode secondSheetSaved = updateCells(
                token,
                workbookId,
                renamed.path("currentVersion").asInt(),
                secondSheetId,
                new EditInput(0, 0, "5"),
                new EditInput(0, 1, "7"),
                new EditInput(0, 2, "=SUM(A1:B1)")
        );
        assertThat(secondSheetSaved.path("currentVersion").asInt()).isEqualTo(4);
        assertThat(secondSheetSaved.path("computedGrid").get(0).get(2).asText()).isEqualTo("12");

        JsonNode firstSheetActivated = setActiveSheet(token, workbookId, firstSheetId);
        assertThat(firstSheetActivated.path("currentVersion").asInt()).isEqualTo(4);
        assertThat(firstSheetActivated.path("activeSheetId").asText()).isEqualTo(firstSheetId);
        assertThat(firstSheetActivated.path("grid").get(0).get(0).asText()).isBlank();

        JsonNode firstSheetSaved = updateCells(
                token,
                workbookId,
                firstSheetActivated.path("currentVersion").asInt(),
                firstSheetId,
                new EditInput(0, 0, "1")
        );
        assertThat(firstSheetSaved.path("currentVersion").asInt()).isEqualTo(5);
        assertThat(firstSheetSaved.path("grid").get(0).get(0).asText()).isEqualTo("1");

        JsonNode reopened = getWorkbook(token, workbookId);
        assertThat(reopened.path("sheets")).hasSize(2);
        assertThat(reopened.path("activeSheetId").asText()).isEqualTo(firstSheetId);
        assertThat(reopened.path("grid").get(0).get(0).asText()).isEqualTo("1");
        assertThat(sheetById(reopened, secondSheetId).path("computedGrid").get(0).get(2).asText()).isEqualTo("12");

        JsonNode deleted = deleteSheet(token, workbookId, secondSheetId);
        assertThat(deleted.path("currentVersion").asInt()).isEqualTo(6);
        assertThat(deleted.path("sheets")).hasSize(1);

        mockMvc.perform(delete("/api/v1/sheets/workbooks/" + workbookId + "/sheets/" + firstSheetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10001));
    }

    @Test
    void sheetsWorkbookMultiSheetImportExportShouldWork() throws Exception {
        String token = register("v84-sheets-import-owner");
        JsonNode imported = importWorkbook(token, "v84-imported-workbook", buildMultiSheetXlsx());
        String workbookId = imported.path("id").asText();
        String janSheetId = imported.path("activeSheetId").asText();
        String notesSheetId = imported.path("sheets").get(1).path("id").asText();

        assertThat(imported.path("sheets")).hasSize(2);
        assertThat(imported.path("computedGrid").get(0).get(2).asText()).isEqualTo("30");
        assertThat(sheetById(imported, notesSheetId).path("name").asText()).isEqualTo("Notes");

        JsonNode csvExport = exportWorkbook(token, workbookId, "CSV");
        assertThat(csvExport.path("content").asText()).contains("10,20,30");

        JsonNode jsonExport = exportWorkbook(token, workbookId, "JSON");
        JsonNode exportPayload = objectMapper.readTree(jsonExport.path("content").asText());
        assertThat(exportPayload.path("activeSheetId").asText()).isEqualTo(janSheetId);
        assertThat(exportPayload.path("sheets")).hasSize(2);
        assertThat(exportPayload.path("sheets").get(0).path("grid").get(0).get(2).asText()).isEqualTo("=SUM(A1:B1)");

        JsonNode activated = setActiveSheet(token, workbookId, notesSheetId);
        assertThat(activated.path("activeSheetId").asText()).isEqualTo(notesSheetId);

        JsonNode tsvExport = exportWorkbook(token, workbookId, "TSV");
        assertThat(tsvExport.path("content").asText()).contains("hello");
    }

    @Test
    void legacySingleSheetWorkbookShouldStillReadAsDefaultSheet() throws Exception {
        String token = register("v84-sheets-legacy-owner");
        JsonNode created = createWorkbook(token, "v84-legacy-workbook");
        String workbookId = created.path("id").asText();

        SheetsWorkbook workbook = sheetsWorkbookMapper.selectById(Long.valueOf(workbookId));
        workbook.setSheetsJson(null);
        workbook.setActiveSheetId(null);
        sheetsWorkbookMapper.updateById(workbook);

        JsonNode reopened = getWorkbook(token, workbookId);
        assertThat(reopened.path("sheets")).hasSize(1);
        assertThat(reopened.path("activeSheetId").asText()).isNotBlank();
        assertThat(reopened.path("sheets").get(0).path("name").asText()).isEqualTo("Sheet 1");

        JsonNode updated = updateCells(
                token,
                workbookId,
                reopened.path("currentVersion").asInt(),
                null,
                new EditInput(0, 0, "42")
        );
        assertThat(updated.path("grid").get(0).get(0).asText()).isEqualTo("42");
        assertThat(updated.path("sheets")).hasSize(1);
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

    private JsonNode createSheet(String token, String workbookId, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sheets/workbooks/" + workbookId + "/sheets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private JsonNode renameSheet(String token, String workbookId, String sheetId, String name) throws Exception {
        MvcResult result = mockMvc.perform(put("/api/v1/sheets/workbooks/" + workbookId + "/sheets/" + sheetId + "/rename")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode().put("name", name).toString()))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private JsonNode setActiveSheet(String token, String workbookId, String sheetId) throws Exception {
        MvcResult result = mockMvc.perform(put("/api/v1/sheets/workbooks/" + workbookId + "/active-sheet")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode().put("sheetId", sheetId).toString()))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private JsonNode deleteSheet(String token, String workbookId, String sheetId) throws Exception {
        MvcResult result = mockMvc.perform(delete("/api/v1/sheets/workbooks/" + workbookId + "/sheets/" + sheetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private JsonNode importWorkbook(String token, String title, byte[] xlsxBytes) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "v84-sheets.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                xlsxBytes
        );
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

    private JsonNode updateCells(
            String token,
            String workbookId,
            int currentVersion,
            String sheetId,
            EditInput... edits
    ) throws Exception {
        JsonNode payload = buildEditPayload(currentVersion, sheetId, edits);
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

    private JsonNode buildEditPayload(int currentVersion, String sheetId, EditInput... edits) {
        com.fasterxml.jackson.databind.node.ObjectNode root = objectMapper.createObjectNode();
        root.put("currentVersion", currentVersion);
        if (sheetId != null) {
            root.put("sheetId", sheetId);
        }
        com.fasterxml.jackson.databind.node.ArrayNode editArray = root.putArray("edits");
        for (EditInput edit : edits) {
            editArray.addObject()
                    .put("rowIndex", edit.rowIndex())
                    .put("colIndex", edit.colIndex())
                    .put("value", edit.value());
        }
        return root;
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

    private byte[] buildMultiSheetXlsx() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var jan = workbook.createSheet("Jan");
            jan.createRow(0).createCell(0).setCellValue(10);
            jan.getRow(0).createCell(1).setCellValue(20);
            jan.getRow(0).createCell(2).setCellFormula("SUM(A1:B1)");

            var notes = workbook.createSheet("Notes");
            notes.createRow(0).createCell(0).setCellValue("hello");

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private record EditInput(int rowIndex, int colIndex, String value) {
    }
}
