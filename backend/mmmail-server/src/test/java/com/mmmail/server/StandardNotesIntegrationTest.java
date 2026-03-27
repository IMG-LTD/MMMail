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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StandardNotesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void standardNotesWorkspaceShouldExposeFoldersTasksExportAndSuiteIntegration() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String email = "v77-standard-notes-%s@mmmail.local".formatted(suffix);
        String token = register(email, "Password@123", "V77 Standard Notes User");

        JsonNode initialOverview = getOverview(token);
        assertThat(initialOverview.path("totalNoteCount").asInt()).isZero();
        assertThat(initialOverview.path("folderCount").asInt()).isZero();
        assertThat(initialOverview.path("exportReady").asBoolean()).isFalse();

        JsonNode folder = createFolder(token, "Focus " + suffix, "#C7A57A", "Task execution lane");
        String folderId = folder.path("id").asText();
        assertThat(folder.path("name").asText()).contains("Focus");

        JsonNode foldersBeforeNote = listFolders(token);
        assertThat(foldersBeforeNote).hasSize(1);
        assertThat(foldersBeforeNote.get(0).path("noteCount").asInt()).isZero();

        JsonNode created = createNote(token, "Checklist " + suffix, "- [ ] Review\n- [x] Ship", "CHECKLIST", folderId);
        String noteId = created.path("id").asText();
        assertThat(created.path("folderId").asText()).isEqualTo(folderId);
        assertThat(created.path("checklistItems").size()).isEqualTo(2);
        assertThat(created.path("checklistTaskCount").asInt()).isEqualTo(2);
        assertThat(created.path("completedChecklistTaskCount").asInt()).isEqualTo(1);

        JsonNode updated = updateNote(token, noteId, created.path("currentVersion").asInt(), "Checklist " + suffix, "- [ ] Review\n- [x] Ship", "CHECKLIST", folderId, true, false);
        assertThat(updated.path("pinned").asBoolean()).isTrue();

        JsonNode toggled = toggleChecklistItem(token, noteId, 0, updated.path("currentVersion").asInt(), true);
        assertThat(toggled.path("completedChecklistTaskCount").asInt()).isEqualTo(2);
        assertThat(toggled.path("tags").get(0).asText()).isEqualTo("vault");

        JsonNode reopened = getNote(token, noteId);
        assertThat(reopened.path("content").asText()).contains("[x] Review");
        assertThat(reopened.path("folderName").asText()).contains("Focus");

        JsonNode listedFolders = listFolders(token);
        assertThat(listedFolders).hasSize(1);
        assertThat(listedFolders.get(0).path("noteCount").asInt()).isEqualTo(1);
        assertThat(listedFolders.get(0).path("checklistTaskCount").asInt()).isEqualTo(2);
        assertThat(listedFolders.get(0).path("completedChecklistTaskCount").asInt()).isEqualTo(2);

        JsonNode filteredNotes = listNotes(token, folderId);
        assertThat(filteredNotes).hasSize(1);
        assertThat(filteredNotes.get(0).path("id").asText()).isEqualTo(noteId);
        assertThat(filteredNotes.get(0).path("folderId").asText()).isEqualTo(folderId);

        JsonNode overview = getOverview(token);
        assertThat(overview.path("totalNoteCount").asInt()).isEqualTo(1);
        assertThat(overview.path("pinnedNoteCount").asInt()).isEqualTo(1);
        assertThat(overview.path("folderCount").asInt()).isEqualTo(1);
        assertThat(overview.path("checklistNoteCount").asInt()).isEqualTo(1);
        assertThat(overview.path("checklistTaskCount").asInt()).isEqualTo(2);
        assertThat(overview.path("completedChecklistTaskCount").asInt()).isEqualTo(2);
        assertThat(overview.path("exportReady").asBoolean()).isTrue();

        JsonNode export = getExport(token);
        assertThat(export.path("fileName").asText()).contains("standard-notes-workspace-");
        assertThat(export.path("folders")).hasSize(1);
        assertThat(export.path("notes")).hasSize(1);

        JsonNode looseNote = createNote(token, "Loose " + suffix, "Capture inbox", "PLAIN_TEXT", null);
        JsonNode allNotes = listNotes(token, null);
        assertThat(allNotes).hasSize(2);
        assertThat(containsNoteId(allNotes, noteId)).isTrue();
        assertThat(containsNoteId(allNotes, looseNote.path("id").asText())).isTrue();

        JsonNode products = getSuiteProducts(token);
        assertThat(containsProduct(products, "STANDARD_NOTES")).isTrue();

        JsonNode quickRoutes = getCommandCenterQuickRoutes(token);
        assertThat(containsQuickRoute(quickRoutes, "STANDARD_NOTES", "/standard-notes")).isTrue();

        JsonNode folderSearchItems = getUnifiedSearchItems(token, "Focus " + suffix);
        JsonNode folderMatch = findByProductAndType(folderSearchItems, "STANDARD_NOTES", "FOLDER");
        assertThat(folderMatch).isNotNull();
        assertThat(folderMatch.path("routePath").asText()).isEqualTo("/standard-notes?folderId=" + folderId);

        JsonNode noteSearchItems = getUnifiedSearchItems(token, "Checklist " + suffix);
        JsonNode noteMatch = findByProductAndType(noteSearchItems, "STANDARD_NOTES", "NOTE");
        assertThat(noteMatch).isNotNull();
        assertThat(noteMatch.path("routePath").asText()).isEqualTo("/standard-notes?noteId=" + noteId);

        JsonNode readiness = getReadinessItems(token);
        JsonNode standardNotesItem = findByCode(readiness, "STANDARD_NOTES");
        assertThat(standardNotesItem).isNotNull();
        assertThat(findSignalValue(standardNotesItem.path("signals"), "folder_count")).isEqualTo(1);
        assertThat(findSignalValue(standardNotesItem.path("signals"), "checklist_task_count")).isEqualTo(2);

        mockMvc.perform(get("/api/v1/docs/notes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        removeFolder(token, folderId);
        JsonNode foldersAfterDelete = listFolders(token);
        assertThat(foldersAfterDelete).isEmpty();
        JsonNode noteAfterFolderDelete = getNote(token, noteId);
        assertThat(noteAfterFolderDelete.path("folderId").isNull()).isTrue();
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

    private JsonNode createFolder(String token, String name, String color, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/standard-notes/folders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": %s,
                                  "color": %s,
                                  "description": %s
                                }
                                """.formatted(json(name), json(color), json(description))))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode listFolders(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/standard-notes/folders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private void removeFolder(String token, String folderId) throws Exception {
        mockMvc.perform(delete("/api/v1/standard-notes/folders/" + folderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private JsonNode createNote(String token, String title, String content, String noteType, String folderId) throws Exception {
        String folderField = folderId == null ? "" : "\n                                  \"folderId\": %s,".formatted(folderId);
        MvcResult result = mockMvc.perform(post("/api/v1/standard-notes/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": %s,
                                  "content": %s,
                                  "noteType": %s,
                                  "tags": ["vault", "private"],%s
                                  "pinned": false
                                }
                                """.formatted(json(title), json(content), json(noteType), folderField)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode updateNote(
            String token,
            String noteId,
            int currentVersion,
            String title,
            String content,
            String noteType,
            String folderId,
            boolean pinned,
            boolean archived
    ) throws Exception {
        MvcResult result = mockMvc.perform(put("/api/v1/standard-notes/notes/" + noteId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": %s,
                                  "content": %s,
                                  "currentVersion": %d,
                                  "noteType": %s,
                                  "tags": ["vault", "knowledge"],
                                  "folderId": %s,
                                  "pinned": %s,
                                  "archived": %s
                                }
                                """.formatted(json(title), json(content), currentVersion, json(noteType), folderId, pinned, archived)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode toggleChecklistItem(String token, String noteId, int itemIndex, int currentVersion, boolean completed) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/standard-notes/notes/" + noteId + "/checklist-items/" + itemIndex + "/toggle")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentVersion": %d,
                                  "completed": %s
                                }
                                """.formatted(currentVersion, completed)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode listNotes(String token, String folderId) throws Exception {
        var request = get("/api/v1/standard-notes/notes")
                .header("Authorization", "Bearer " + token);
        if (folderId != null) {
            request.param("folderId", folderId);
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode getNote(String token, String noteId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/standard-notes/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode getOverview(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/standard-notes/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode getExport(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/standard-notes/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode getSuiteProducts(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/suite/products")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private JsonNode getCommandCenterQuickRoutes(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/suite/command-center")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("quickRoutes");
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

    private boolean containsProduct(JsonNode rows, String productCode) {
        for (JsonNode row : rows) {
            if (productCode.equals(row.path("code").asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsNoteId(JsonNode rows, String noteId) {
        for (JsonNode row : rows) {
            if (noteId.equals(row.path("id").asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean containsQuickRoute(JsonNode rows, String productCode, String routePath) {
        for (JsonNode row : rows) {
            if (productCode.equals(row.path("productCode").asText())
                    && routePath.equals(row.path("routePath").asText())) {
                return true;
            }
        }
        return false;
    }

    private JsonNode findByProductAndType(JsonNode rows, String productCode, String itemType) {
        for (JsonNode row : rows) {
            if (productCode.equals(row.path("productCode").asText())
                    && itemType.equals(row.path("itemType").asText())) {
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

    private int findSignalValue(JsonNode rows, String key) {
        for (JsonNode row : rows) {
            if (key.equals(row.path("key").asText())) {
                return row.path("value").asInt();
            }
        }
        return -1;
    }

    private String json(String value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").isMissingNode()
                ? objectMapper.readTree(result.getResponse().getContentAsString())
                : objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
