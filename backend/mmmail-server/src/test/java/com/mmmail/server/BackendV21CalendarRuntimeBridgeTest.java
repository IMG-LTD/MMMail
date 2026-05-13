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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendV21CalendarRuntimeBridgeTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void v21EventsShouldUseRuntimeCalendarCrudAndAvailability() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "v21-calendar-owner-" + suffix + "@mmmail.local";
        String token = register(ownerEmail, "V21 Calendar Owner");

        String eventId = createV21Event(token, """
                {
                  "title": "V2 runtime review",
                  "location": "Room V2",
                  "startAt": "2026-05-22T09:00:00",
                  "endAt": "2026-05-22T10:00:00",
                  "timezone": "Asia/Shanghai",
                  "reminderMinutes": 15,
                  "attendees": []
                }
                """);

        mockMvc.perform(get("/api/v2/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-05-22T00:00:00")
                        .param("to", "2026-05-23T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(eventId))
                .andExpect(jsonPath("$.data[0].title").value("V2 runtime review"));

        mockMvc.perform(get("/api/v2/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .param("view", "agenda")
                        .param("days", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("V2 runtime review"));

        mockMvc.perform(post("/api/v2/calendar/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2026-05-22T09:30:00",
                                  "endAt": "2026-05-22T09:45:00",
                                  "attendeeEmails": ["%s"]
                                }
                                """.formatted(ownerEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.busyCount").value(1));

        mockMvc.perform(patch("/api/v2/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "V2 runtime review updated",
                                  "location": "Room V2",
                                  "startAt": "2026-05-22T09:00:00",
                                  "endAt": "2026-05-22T10:30:00",
                                  "timezone": "Asia/Shanghai",
                                  "reminderMinutes": 30,
                                  "attendees": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("V2 runtime review updated"))
                .andExpect(jsonPath("$.data.reminderMinutes").value(30));

        mockMvc.perform(delete("/api/v2/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void v21CalendarSettingsShouldPersistTimezoneThroughUserPreference() throws Exception {
        String token = register(
                "v21-calendar-settings-" + System.nanoTime() + "@mmmail.local",
                "V21 Calendar Settings"
        );

        mockMvc.perform(get("/api/v2/calendar/settings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultTimezone").value("UTC"))
                .andExpect(jsonPath("$.data.weekStartsOn").value("monday"))
                .andExpect(jsonPath("$.data.workingHours[0]").value("09:00"));

        mockMvc.perform(patch("/api/v2/calendar/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "defaultTimezone": "Asia/Shanghai",
                                  "weekStartsOn": "monday",
                                  "workingHours": ["09:00", "18:00"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultTimezone").value("Asia/Shanghai"));

        mockMvc.perform(get("/api/v2/calendar/settings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultTimezone").value("Asia/Shanghai"));
    }

    @Test
    void premiumCalendarResourcesShouldRemainEntitlementGated() throws Exception {
        String token = register(
                "v21-calendar-premium-" + System.nanoTime() + "@mmmail.local",
                "V21 Calendar Premium"
        );

        mockMvc.perform(get("/api/v2/calendar/resources")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));

        mockMvc.perform(post("/api/v2/calendar/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "1",
                                  "resourceId": "room-a",
                                  "startAt": "2026-05-22T09:00:00",
                                  "endAt": "2026-05-22T10:00:00"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
    }

    private String createV21Event(String token, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
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
