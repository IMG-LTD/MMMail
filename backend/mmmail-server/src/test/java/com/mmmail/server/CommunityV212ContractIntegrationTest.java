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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityV212ContractIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void communityShouldSupportPostCommentReactionBookmarkAndModerationLifecycle() throws Exception {
        String token = register("v212-community-" + System.nanoTime() + "@mmmail.local", "V212 Community");
        String adminToken = login("admin@mmmail.local");

        mockMvc.perform(get("/api/v1/community/topics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].slug").value("general"));

        String postId = createPost(token);
        mockMvc.perform(get("/api/v1/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .param("q", "CRDT")
                        .param("sort", "latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(postId));

        mockMvc.perform(patch("/api/v1/community/posts/" + postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"CRDT release notes updated\",\"bodyMd\":\"Updated body\",\"tags\":[\"release\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("CRDT release notes updated"));

        String rootCommentId = createComment(token, postId, "Root comment", null);
        createComment(token, postId, "Reply comment", rootCommentId);
        mockMvc.perform(get("/api/v1/community/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(rootCommentId))
                .andExpect(jsonPath("$.data[0].replies[0].bodyMd").value("Reply comment"));

        mockMvc.perform(post("/api/v1/community/posts/" + postId + "/like")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));
        mockMvc.perform(post("/api/v1/community/posts/" + postId + "/bookmark")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookmarked").value(true));
        mockMvc.perform(get("/api/v1/community/me/bookmarks")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(postId));

        String reportId = reportPost(token, postId);
        mockMvc.perform(get("/api/v1/community/admin/reports")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(reportId));
        mockMvc.perform(patch("/api/v1/community/admin/reports/" + reportId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"hide\",\"actionNote\":\"spam confirmed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("actioned"));
        mockMvc.perform(get("/api/v1/community/posts/" + postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("hidden"));
    }

    @Test
    void communityShouldRestrictAuthorOnlyAndAdminOnlyMutations() throws Exception {
        String authorToken = register("v212-community-author-" + System.nanoTime() + "@mmmail.local", "Author");
        String otherToken = register("v212-community-other-" + System.nanoTime() + "@mmmail.local", "Other");
        String postId = createPost(authorToken);

        mockMvc.perform(patch("/api/v1/community/posts/" + postId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"stolen\",\"bodyMd\":\"nope\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/community/posts/" + postId + "/pin")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/community/posts/" + postId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("deleted"));
    }

    @Test
    void communityShouldSupportTopicAdminLifecycleAndViewDeduplication() throws Exception {
        String userToken = register("v212-community-view-" + System.nanoTime() + "@mmmail.local", "Viewer");
        String adminToken = login("admin@mmmail.local");
        String topicId = createTopic(adminToken);

        mockMvc.perform(patch("/api/v1/community/topics/" + topicId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"ops-updated\",\"title\":\"Ops Updated\",\"description\":\"Operational updates\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("ops-updated"));

        String postId = createPost(userToken, topicId);
        mockMvc.perform(post("/api/v1/community/posts/" + postId + "/view")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(1));
        mockMvc.perform(post("/api/v1/community/posts/" + postId + "/view")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(1));

        mockMvc.perform(delete("/api/v1/community/topics/" + topicId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/community/posts/" + postId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/community/topics/" + topicId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));
    }

    @Test
    void communityShouldSanitizeRenderedRichTextHtml() throws Exception {
        String token = register("v212-community-xss-" + System.nanoTime() + "@mmmail.local", "XSS");
        String body = "# Safe\n<img src=x onerror=alert(1)>\n<script>alert(1)</script>";

        MvcResult result = mockMvc.perform(post("/api/v1/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "topicId", "tp_general",
                                "title", "Sanitize rich text",
                                "bodyMd", body
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        String bodyHtml = readJson(result).at("/data/bodyHtml").asText();
        assertThat(bodyHtml).doesNotContain("<script", "onerror", "<img");
        assertThat(bodyHtml).contains("<h1>Safe</h1>");
    }

    private String createPost(String token) throws Exception {
        return createPost(token, "tp_general");
    }

    private String createPost(String token, String topicId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/community/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topicId": "%s",
                                  "title": "CRDT release notes",
                                  "bodyMd": "# Release\\nDiscuss the v2.1.2 CRDT rollout",
                                  "tags": ["release", "feedback"]
                                }
                                """.formatted(topicId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("published"))
                .andExpect(jsonPath("$.data.tags[0]").value("release"))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String createComment(String token, String postId, String body, String parentCommentId) throws Exception {
        String parentJson = parentCommentId == null ? "" : ",\"parentCommentId\":\"" + parentCommentId + "\"";
        MvcResult result = mockMvc.perform(post("/api/v1/community/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bodyMd\":\"" + body + "\"" + parentJson + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("published"))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String reportPost(String token, String postId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/community/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetType": "post",
                                  "targetId": "%s",
                                  "reason": "spam",
                                  "detail": "contains promotional content"
                                }
                                """.formatted(postId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String createTopic(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/community/topics")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug": "ops-%d",
                                  "title": "Ops",
                                  "description": "Operational news"
                                }
                                """.formatted(System.nanoTime())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Ops"))
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

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.path("code").asInt()).isZero();
        return json;
    }
}
