package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendV21CollaborationWriteRuntimeTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void v21CollaborationShouldPersistProjectTaskUpdateCommentAuditAndOutbox() throws Exception {
        String token = register("v21-collab-" + System.nanoTime() + "@mmmail.local");
        String projectId = createProject(token, "Launch Readiness", "WORKSPACE");
        String taskId = createTask(token, projectId, "Prepare acceptance checklist");

        updateTask(token, taskId);
        commentTask(token, taskId);
        assertProjectReadBack(token, projectId);
        assertTaskReadBack(token, taskId);
        assertActivityReadBack(token);
        assertOutboxEvents();
        assertAuditEvents();
    }

    @Test
    void v21CollaborationShouldRejectInvalidWriteInput() throws Exception {
        String token = register("v21-collab-invalid-" + System.nanoTime() + "@mmmail.local");

        mockMvc.perform(post("/api/v2/collaboration/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v2/collaboration/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "999999",
                                  "title": "Missing project"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    private String createProject(String token, String name, String product) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/collaboration/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "product": "%s"
                                }
                                """.formatted(name, product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(name))
                .andExpect(jsonPath("$.data.product").value(product))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String createTask(String token, String projectId, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/collaboration/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "%s",
                                  "title": "%s"
                                }
                                """.formatted(projectId, title)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(projectId))
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void updateTask(String token, String taskId) throws Exception {
        mockMvc.perform(patch("/api/v2/collaboration/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DONE",
                                  "assigneeEmail": "owner@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(taskId))
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.assigneeEmail").value("owner@example.com"));
    }

    private void commentTask(String token, String taskId) throws Exception {
        mockMvc.perform(post("/api/v2/collaboration/tasks/" + taskId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Acceptance checklist is ready"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.product").value("WORKSPACE"));
    }

    private void assertProjectReadBack(String token, String projectId) throws Exception {
        mockMvc.perform(get("/api/v2/collaboration/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(projectId))
                .andExpect(jsonPath("$.data[0].taskCount").value(1));
    }

    private void assertTaskReadBack(String token, String taskId) throws Exception {
        mockMvc.perform(get("/api/v2/collaboration/tasks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(taskId))
                .andExpect(jsonPath("$.data[0].status").value("DONE"));
    }

    private void assertActivityReadBack(String token) throws Exception {
        mockMvc.perform(get("/api/v2/collaboration/activity")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].product").value("WORKSPACE"));
    }

    private void assertOutboxEvents() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from platform_outbox_event
                where event_type in (
                  'collaboration.project.created.v1',
                  'collaboration.task.created.v1',
                  'collaboration.task.updated.v1',
                  'collaboration.comment.created.v1'
                )
                """, Integer.class);
        assertThat(count).isEqualTo(4);
    }

    private void assertAuditEvents() {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from audit_event
                where event_type in (
                  'V21_COLLABORATION_PROJECT_CREATE',
                  'V21_COLLABORATION_TASK_CREATE',
                  'V21_COLLABORATION_TASK_UPDATE',
                  'V21_COLLABORATION_COMMENT_CREATE'
                )
                """, Integer.class);
        assertThat(count).isEqualTo(4);
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "V21 Collaboration"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
