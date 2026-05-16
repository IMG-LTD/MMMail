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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SheetsFormulaV212ContractIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final int CIRCULAR_REF_CODE = 42221;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sheetsFormulaApiShouldEvaluateGraphAndRecalculateWorkbook() throws Exception {
        String token = register("v212-sheets-formula-" + System.nanoTime() + "@mmmail.local");
        JsonNode created = createWorkbook(token, "v212 formula workbook");
        String workbookId = created.path("id").asText();

        JsonNode withFormula = updateCells(
                token,
                workbookId,
                created.path("currentVersion").asInt(),
                new EditInput(0, 0, "10"),
                new EditInput(1, 0, "20"),
                new EditInput(0, 1, "=SUM(A1:A2)")
        );

        mockMvc.perform(post("/api/v1/sheets/" + workbookId + "/cells/evaluate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cells": [
                                    { "ref": "B1", "formula": "=SUM(A1:A2)" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].ref").value("B1"))
                .andExpect(jsonPath("$.data.results[0].value").value(30))
                .andExpect(jsonPath("$.data.results[0].type").value("NUMBER"))
                .andExpect(jsonPath("$.data.results[0].dependsOn[0]").value("A1:A2"));

        JsonNode graph = readData(mockMvc.perform(get("/api/v1/sheets/" + workbookId + "/dependency-graph")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(graph.path("topologicalOrder")).extracting(JsonNode::asText).contains("A1", "A2", "B1");
        assertThat(graph.path("nodes").findValuesAsText("ref")).contains("B1");

        updateCells(token, workbookId, withFormula.path("currentVersion").asInt(), new EditInput(0, 0, "15"));
        mockMvc.perform(post("/api/v1/sheets/" + workbookId + "/recalculate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computedGrid[0][1]").value("35"));
    }

    @Test
    void sheetsFormulaApiShouldRejectCircularReferences() throws Exception {
        String token = register("v212-sheets-cycle-" + System.nanoTime() + "@mmmail.local");
        String workbookId = createWorkbook(token, "v212 cycle workbook").path("id").asText();

        mockMvc.perform(post("/api/v1/sheets/" + workbookId + "/cells/evaluate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cells": [
                                    { "ref": "B1", "formula": "=C1" },
                                    { "ref": "C1", "formula": "=B1" }
                                  ]
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(CIRCULAR_REF_CODE));
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "Sheets Formula"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
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

    private JsonNode updateCells(String token, String workbookId, int currentVersion, EditInput... edits) throws Exception {
        MvcResult result = mockMvc.perform(put("/api/v1/sheets/workbooks/" + workbookId + "/cells")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentVersion": %d,
                                  "edits": %s
                                }
                                """.formatted(currentVersion, objectMapper.writeValueAsString(edits))))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    private JsonNode readData(MvcResult result) throws Exception {
        JsonNode json = readJson(result);
        assertThat(json.path("code").asInt()).isZero();
        return json.path("data");
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record EditInput(int rowIndex, int colIndex, String value) {
    }
}
