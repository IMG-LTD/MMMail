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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CalendarReleaseBlockingIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void crudAndRangeQueryShouldHandleCrossDayAllDayAndTimezone() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerToken = register("calendar-owner-" + suffix + "@mmmail.local", "Calendar Owner");

        String crossDayId = createEvent(
                ownerToken,
                """
                {
                  "title": "Night deploy",
                  "location": "Ops",
                  "startAt": "2026-04-01T23:30:00",
                  "endAt": "2026-04-02T01:00:00",
                  "timezone": "Asia/Shanghai",
                  "reminderMinutes": 30,
                  "attendees": []
                }
                """
        );
        String allDayId = createEvent(
                ownerToken,
                """
                {
                  "title": "Company holiday",
                  "location": "Remote",
                  "startAt": "2026-04-02T00:00:00",
                  "endAt": "2026-04-03T00:00:00",
                  "allDay": true,
                  "timezone": "Europe/Zurich",
                  "reminderMinutes": 120,
                  "attendees": [
                    {"email": "teammate-%s@example.com", "displayName": "Teammate"}
                  ]
                }
                """.formatted(suffix)
        );

        mockMvc.perform(get("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("from", "2026-04-02T00:00:00")
                        .param("to", "2026-04-02T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/v1/calendar/events/" + allDayId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allDay").value(true))
                .andExpect(jsonPath("$.data.timezone").value("Europe/Zurich"))
                .andExpect(jsonPath("$.data.attendees[0].email").value("teammate-" + suffix + "@example.com"));

        mockMvc.perform(put("/api/v1/calendar/events/" + allDayId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Company holiday updated",
                                  "location": "Remote",
                                  "description": "Updated holiday",
                                  "startAt": "2026-04-02T00:00:00",
                                  "endAt": "2026-04-03T00:00:00",
                                  "allDay": true,
                                  "timezone": "Europe/Zurich",
                                  "reminderMinutes": 60,
                                  "attendees": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Company holiday updated"))
                .andExpect(jsonPath("$.data.reminderMinutes").value(60));

        mockMvc.perform(delete("/api/v1/calendar/events/" + crossDayId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/v1/calendar/events/" + crossDayId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.CALENDAR_EVENT_NOT_FOUND.getCode()));
    }

    @Test
    void sharePermissionsShouldAllowEditorButRejectViewerAndOutsider() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerToken = register("calendar-share-owner-" + suffix + "@mmmail.local", "Calendar Owner");
        String viewerEmail = "calendar-share-viewer-" + suffix + "@mmmail.local";
        String editorEmail = "calendar-share-editor-" + suffix + "@mmmail.local";
        String outsiderEmail = "calendar-share-outsider-" + suffix + "@mmmail.local";
        String viewerToken = register(viewerEmail, "Calendar Viewer");
        String editorToken = register(editorEmail, "Calendar Editor");
        String outsiderToken = register(outsiderEmail, "Calendar Outsider");

        String eventId = createEvent(
                ownerToken,
                """
                {
                  "title": "Review calendar",
                  "description": "Shared event",
                  "location": "Room A",
                  "startAt": "2026-04-05T09:00:00",
                  "endAt": "2026-04-05T10:00:00",
                  "timezone": "UTC",
                  "reminderMinutes": 15,
                  "attendees": [
                    {"email": "%s", "displayName": "Viewer"}
                  ]
                }
                """.formatted(viewerEmail)
        );
        String viewerShareId = createShare(ownerToken, eventId, viewerEmail, "VIEW");
        String editorShareId = createShare(ownerToken, eventId, editorEmail, "EDIT");
        acceptShare(viewerToken, viewerShareId);
        acceptShare(editorToken, editorShareId);

        mockMvc.perform(get("/api/v1/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canEdit").value(false))
                .andExpect(jsonPath("$.data.canDelete").value(false))
                .andExpect(jsonPath("$.data.sharePermission").value("VIEW"));

        mockMvc.perform(put("/api/v1/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("Viewer blocked", "UTC")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.getCode()));

        mockMvc.perform(delete("/api/v1/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.getCode()));

        mockMvc.perform(put("/api/v1/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + editorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("Editor updated", "UTC")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Editor updated"))
                .andExpect(jsonPath("$.data.sharePermission").value("EDIT"));

        mockMvc.perform(get("/api/v1/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.CALENDAR_EVENT_NOT_FOUND.getCode()));
    }

    @Test
    void invalidTimesAndTimezoneShouldBeRejected() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerToken = register("calendar-invalid-" + suffix + "@mmmail.local", "Calendar Invalid");

        mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Broken range",
                                  "startAt": "2026-04-10T11:00:00",
                                  "endAt": "2026-04-10T10:00:00",
                                  "timezone": "UTC",
                                  "attendees": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));

        mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Broken timezone",
                                  "startAt": "2026-04-10T09:00:00",
                                  "endAt": "2026-04-10T10:00:00",
                                  "timezone": "Mars/Olympus",
                                  "attendees": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()))
                .andExpect(jsonPath("$.message").value("Timezone is invalid"));
    }

    private void acceptShare(String token, String shareId) throws Exception {
        mockMvc.perform(post("/api/v1/calendar/shares/" + shareId + "/response")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "response": "ACCEPT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.responseStatus").value("ACCEPTED"));
    }

    private String createEvent(String token, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String createShare(String token, String eventId, String targetEmail, String permission) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/calendar/events/" + eventId + "/shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetEmail": "%s",
                                  "permission": "%s"
                                }
                                """.formatted(targetEmail, permission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value(permission))
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

    private String updatePayload(String title, String timezone) {
        return """
                {
                  "title": "%s",
                  "description": "Updated shared event",
                  "location": "Room B",
                  "startAt": "2026-04-05T09:00:00",
                  "endAt": "2026-04-05T10:30:00",
                  "timezone": "%s",
                  "reminderMinutes": 45,
                  "attendees": []
                }
                """.formatted(title, timezone);
    }
}
