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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CalendarIcsImportIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void importAndExportShouldHandleTimedAndAllDayIcsEvents() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String token = register("calendar-import-" + suffix + "@mmmail.local", "Calendar Import");
        String content = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//MMMail//Calendar Test//EN
                BEGIN:VEVENT
                UID:timed-%s@mmmail.local
                DTSTART:20260410T090000
                DTEND:20260410T100000
                SUMMARY:Imported planning
                DESCRIPTION:Imported from ICS
                LOCATION:Focus room
                END:VEVENT
                BEGIN:VEVENT
                UID:holiday-%s@mmmail.local
                DTSTART;VALUE=DATE:20260411
                DTEND;VALUE=DATE:20260412
                SUMMARY:Imported holiday
                END:VEVENT
                END:VCALENDAR
                """.formatted(suffix, suffix);

        MvcResult importResult = mockMvc.perform(post("/api/v1/calendar/import/ics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": %s,
                                  "timezone": "Asia/Shanghai",
                                  "reminderMinutes": 20
                                }
                                """.formatted(objectMapper.writeValueAsString(content))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.importedCount").value(2))
                .andReturn();

        JsonNode imported = readJson(importResult).at("/data/eventIds");
        assertThat(imported.size()).isEqualTo(2);

        mockMvc.perform(get("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-04-10T00:00:00")
                        .param("to", "2026-04-12T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].title").value("Imported planning"))
                .andExpect(jsonPath("$.data[0].timezone").value("Asia/Shanghai"))
                .andExpect(jsonPath("$.data[1].allDay").value(true));

        mockMvc.perform(get("/api/v1/calendar/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SUMMARY:Imported planning")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DTSTART;VALUE=DATE:20260411")));
    }

    @Test
    void importShouldRejectInvalidIcsInputs() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String token = register("calendar-invalid-ics-" + suffix + "@mmmail.local", "Calendar Invalid ICS");

        mockMvc.perform(post("/api/v1/calendar/import/ics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "BEGIN:VCALENDAR\\nVERSION:2.0\\nEND:VCALENDAR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()))
                .andExpect(jsonPath("$.message").value("ICS content must include at least one VEVENT"));

        mockMvc.perform(post("/api/v1/calendar/import/ics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "BEGIN:VCALENDAR\\nBEGIN:VEVENT\\nDTEND:20260410T100000\\nSUMMARY:Missing start\\nEND:VEVENT\\nEND:VCALENDAR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()))
                .andExpect(jsonPath("$.message").value("ICS event is missing DTSTART"));

        mockMvc.perform(post("/api/v1/calendar/import/ics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "BEGIN:VCALENDAR\\nBEGIN:VEVENT\\nDTSTART:20260410T110000\\nDTEND:20260410T100000\\nSUMMARY:Bad range\\nEND:VEVENT\\nEND:VCALENDAR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()))
                .andExpect(jsonPath("$.message").value("ICS event end time must be later than start time"));

        mockMvc.perform(post("/api/v1/calendar/import/ics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "BEGIN:VCALENDAR\\nBEGIN:VEVENT\\nDTSTART;TZID=Mars/Olympus:20260410T090000\\nDTEND;TZID=Mars/Olympus:20260410T100000\\nSUMMARY:Bad timezone\\nEND:VEVENT\\nEND:VCALENDAR"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()))
                .andExpect(jsonPath("$.message").value("ICS timezone is invalid"));
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
