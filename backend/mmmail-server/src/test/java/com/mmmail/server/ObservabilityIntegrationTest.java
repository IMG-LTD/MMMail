package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObservabilityIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void systemHealthShouldKeepPrometheusAndErrorTrackingFieldsStableForFrontend() throws Exception {
        String adminToken = login("admin@mmmail.local", PASSWORD);

        mockMvc.perform(get("/api/v1/system/health")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.data.applicationName").value("mmmail-server"))
                .andExpect(jsonPath("$.data.metrics.totalRequests").exists())
                .andExpect(jsonPath("$.data.errorTracking.totalEvents").exists())
                .andExpect(jsonPath("$.data.jobs.totalRuns").exists())
                .andExpect(jsonPath("$.data.prometheusPath").value("/actuator/prometheus"));
    }

    @Test
    void systemHealthShouldExposeObservabilityDrilldownAndPrometheusMetricsForAdmin() throws Exception {
        String adminToken = login("admin@mmmail.local", PASSWORD);
        createEasySwitchSession(adminToken);

        mockMvc.perform(post("/api/v1/system/errors/client")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Unhandled client runtime failure",
                                  "category": "WINDOW_ERROR",
                                  "severity": "ERROR",
                                  "detail": "ReferenceError: boom",
                                  "path": "/drive",
                                  "method": "GET",
                                  "requestId": "client-req-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));

        mockMvc.perform(get("/api/v1/system/health")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.data.recentErrors[0].source").value("CLIENT"))
                .andExpect(jsonPath("$.data.recentJobs[0].jobName").value("MAIL_EASY_SWITCH_IMPORT"));

        mockMvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("mmmail_api_requests_total")))
                .andExpect(content().string(Matchers.containsString("mmmail_errors_events_total")))
                .andExpect(content().string(Matchers.containsString("mmmail_jobs_executions_total")));
    }

    @Test
    void nonAdminUsersShouldBeRejectedFromSystemOperationsEndpoints() throws Exception {
        String token = register("observability-user-" + System.nanoTime() + "@mmmail.local", PASSWORD, "Observability User");

        mockMvc.perform(get("/api/v1/system/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private void createEasySwitchSession(String token) throws Exception {
        String contactsCsv = """
                Name,Email,Phone,isFavorite
                Alice Import,alice.import@example.com,VIP,true
                """;
        String calendarIcs = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//MMMail Test//EN
                BEGIN:VEVENT
                DTSTART:20260309T090000
                DTEND:20260309T100000
                SUMMARY:Imported standup
                DESCRIPTION:Daily sync
                LOCATION:Room A
                END:VEVENT
                END:VCALENDAR
                """;
        String mailEml = """
                From: Sender One <sender.one@example.com>
                Subject: Imported EML Subject
                Date: Mon, 9 Mar 2026 09:00:00 +0000

                Imported message body.
                """;
        String payload = objectMapper.writeValueAsString(Map.of(
                "provider", "GOOGLE",
                "sourceEmail", "legacy@example.com",
                "importContacts", true,
                "mergeContactDuplicates", true,
                "contactsCsv", contactsCsv,
                "importCalendar", true,
                "calendarIcs", calendarIcs,
                "importMail", true,
                "mailMessages", List.of(mailEml),
                "importedMailFolder", "INBOX"
        ));

        mockMvc.perform(post("/api/v1/mail-easy-switch/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
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

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
