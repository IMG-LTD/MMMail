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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CalendarRecurrenceV212ContractIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void calendarEventsShouldExpandWeeklyRruleWithinQueryWindow() throws Exception {
        String token = register("v212-rrule-" + System.nanoTime() + "@mmmail.local");

        MvcResult result = createWeeklyEvent(token, "Weekly sync", "20260630T000000Z");
        JsonNode created = readJson(result).path("data");
        String eventId = created.path("id").asText();

        assertThat(created.path("seriesId").asText()).isEqualTo(eventId);
        assertThat(created.path("rrule").asText()).isEqualTo("FREQ=WEEKLY;BYDAY=MO;UNTIL=20260630T000000Z");

        mockMvc.perform(get("/api/v2/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-05-18T00:00:00")
                        .param("to", "2026-06-09T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].id").value(eventId))
                .andExpect(jsonPath("$.data[0].seriesId").value(eventId))
                .andExpect(jsonPath("$.data[0].startAt").value("2026-05-18T09:00:00"))
                .andExpect(jsonPath("$.data[1].startAt").value("2026-05-25T09:00:00"))
                .andExpect(jsonPath("$.data[2].startAt").value("2026-06-01T09:00:00"))
                .andExpect(jsonPath("$.data[3].startAt").value("2026-06-08T09:00:00"));
    }

    @Test
    void calendarPatchThisAndFollowingShouldSplitRecurringSeries() throws Exception {
        String token = register("v212-split-" + System.nanoTime() + "@mmmail.local");
        String eventId = readJson(createWeeklyEvent(token, "Split sync", "20260630T000000Z")).at("/data/id").asText();

        MvcResult splitResult = mockMvc.perform(patch("/api/v2/calendar/events/" + eventId)
                        .queryParam("scope", "thisAndFollowing")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Split sync late",
                                  "description": "moved",
                                  "location": "Room 2",
                                  "startAt": "2026-06-01T10:00:00",
                                  "endAt": "2026-06-01T10:30:00",
                                  "timezone": "Asia/Shanghai",
                                  "rrule": "FREQ=WEEKLY;BYDAY=MO;UNTIL=20260630T000000Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.truncatedSeriesId").value(eventId))
                .andExpect(jsonPath("$.data.seriesId").isNotEmpty())
                .andReturn();
        String newSeriesId = readJson(splitResult).at("/data/seriesId").asText();
        assertThat(newSeriesId).isNotEqualTo(eventId);

        mockMvc.perform(get("/api/v2/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-05-18T00:00:00")
                        .param("to", "2026-06-09T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].seriesId").value(eventId))
                .andExpect(jsonPath("$.data[0].startAt").value("2026-05-18T09:00:00"))
                .andExpect(jsonPath("$.data[1].seriesId").value(eventId))
                .andExpect(jsonPath("$.data[1].startAt").value("2026-05-25T09:00:00"))
                .andExpect(jsonPath("$.data[2].seriesId").value(newSeriesId))
                .andExpect(jsonPath("$.data[2].startAt").value("2026-06-01T10:00:00"))
                .andExpect(jsonPath("$.data[3].seriesId").value(newSeriesId))
                .andExpect(jsonPath("$.data[3].startAt").value("2026-06-08T10:00:00"));
    }

    private MvcResult createWeeklyEvent(String token, String title, String until) throws Exception {
        return mockMvc.perform(post("/api/v2/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "standup",
                                  "location": "Room 1",
                                  "startAt": "2026-05-18T09:00:00",
                                  "endAt": "2026-05-18T09:30:00",
                                  "timezone": "Asia/Shanghai",
                                  "rrule": "FREQ=WEEKLY;BYDAY=MO;UNTIL=%s"
                                }
                                """.formatted(title, until)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "Calendar V212"
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
