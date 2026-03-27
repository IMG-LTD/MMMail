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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CalendarSharingAvailabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void availabilityPermissionUpdateAndAcceptFlowShouldWork() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String busyEmail = "v87-busy-%s@mmmail.local".formatted(suffix);
        String organizerEmail = "v87-organizer-%s@mmmail.local".formatted(suffix);
        String busyToken = register(busyEmail, "Password@123", "Busy User");
        String organizerToken = register(organizerEmail, "Password@123", "Organizer User");

        createEvent(busyToken, "Busy slot", "2026-03-10T09:00:00", "2026-03-10T10:30:00", "[]");

        mockMvc.perform(post("/api/v1/calendar/availability/query")
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"startAt\": \"2026-03-10T10:00:00\",
                                  \"endAt\": \"2026-03-10T11:00:00\",
                                  \"attendeeEmails\": [\"%s\", \"unknown-%s@mmmail.local\"]
                                }
                                """.formatted(busyEmail, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.attendeeCount").value(2))
                .andExpect(jsonPath("$.data.summary.busyCount").value(1))
                .andExpect(jsonPath("$.data.summary.unknownCount").value(1))
                .andExpect(jsonPath("$.data.summary.hasConflicts").value(true))
                .andExpect(jsonPath("$.data.attendees[0].email").value(busyEmail))
                .andExpect(jsonPath("$.data.attendees[0].availability").value("BUSY"))
                .andExpect(jsonPath("$.data.attendees[0].overlapCount").value(1))
                .andExpect(jsonPath("$.data.attendees[0].busySlots[0].startAt").value("2026-03-10T10:00:00"))
                .andExpect(jsonPath("$.data.attendees[0].busySlots[0].endAt").value("2026-03-10T10:30:00"))
                .andExpect(jsonPath("$.data.attendees[1].availability").value("UNKNOWN"));

        String sharedEventId = createEvent(
                organizerToken,
                "Team sync",
                "2026-03-10T10:00:00",
                "2026-03-10T11:00:00",
                """
                [
                  {"email": "%s", "displayName": "Busy User"}
                ]
                """.formatted(busyEmail)
        );
        String shareId = createShare(organizerToken, sharedEventId, busyEmail, "VIEW");

        mockMvc.perform(put("/api/v1/calendar/events/" + sharedEventId + "/shares/" + shareId)
                        .header("Authorization", "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"permission\": \"EDIT\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value("EDIT"));

        mockMvc.perform(get("/api/v1/calendar/shares/incoming")
                        .header("Authorization", "Bearer " + busyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].shareId").value(shareId))
                .andExpect(jsonPath("$.data[0].permission").value("EDIT"))
                .andExpect(jsonPath("$.data[0].responseStatus").value("NEEDS_ACTION"));

        mockMvc.perform(post("/api/v1/calendar/shares/" + shareId + "/response")
                        .header("Authorization", "Bearer " + busyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"response\": \"ACCEPT\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value("EDIT"))
                .andExpect(jsonPath("$.data.responseStatus").value("ACCEPTED"));

        mockMvc.perform(get("/api/v1/calendar/events/" + sharedEventId)
                        .header("Authorization", "Bearer " + busyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sharePermission").value("EDIT"))
                .andExpect(jsonPath("$.data.canEdit").value(true))
                .andExpect(jsonPath("$.data.attendees[0].email").value(busyEmail));
    }

    private String register(String email, String password, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"email\": \"%s\",
                                  \"password\": \"%s\",
                                  \"displayName\": \"%s\"
                                }
                                """.formatted(email, password, displayName)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/accessToken").asText();
    }

    private String createEvent(String token, String title, String startAt, String endAt, String attendeesJson) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"title\": \"%s\",
                                  \"description\": \"Planning event\",
                                  \"location\": \"Board room\",
                                  \"startAt\": \"%s\",
                                  \"endAt\": \"%s\",
                                  \"timezone\": \"UTC\",
                                  \"reminderMinutes\": 15,
                                  \"attendees\": %s
                                }
                                """.formatted(title, startAt, endAt, attendeesJson)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return data.path("id").asText();
    }

    private String createShare(String token, String eventId, String targetEmail, String permission) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/calendar/events/" + eventId + "/shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"targetEmail\": \"%s\",
                                  \"permission\": \"%s\"
                                }
                                """.formatted(targetEmail, permission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.permission").value(permission))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
    }
}
