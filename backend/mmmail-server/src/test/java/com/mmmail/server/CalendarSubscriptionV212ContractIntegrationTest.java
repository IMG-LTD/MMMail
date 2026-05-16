package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CalendarSubscriptionV212ContractIntegrationTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void calendarSubscriptionsShouldSyncHttpIcsAndExposeNativeIcsExport() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String token = register("calendar-subscription-" + suffix + "@mmmail.local");
        String icsContent = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//MMMail//Subscription Test//EN
                BEGIN:VEVENT
                UID:subscription-%s@mmmail.local
                DTSTART:20260610T090000
                DTEND:20260610T100000
                SUMMARY:Subscribed planning
                DESCRIPTION:Imported by subscription
                LOCATION:Room S
                END:VEVENT
                END:VCALENDAR
                """.formatted(suffix);

        try (IcsTestServer server = IcsTestServer.start(icsContent)) {
            MvcResult createResult = mockMvc.perform(post("/api/v2/calendar/subscriptions")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "url": "%s",
                                      "label": "Team calendar",
                                      "authMode": "none",
                                      "color": "#2f7dd1"
                                    }
                                    """.formatted(server.url())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").isNotEmpty())
                    .andExpect(jsonPath("$.data.label").value("Team calendar"))
                    .andExpect(jsonPath("$.data.syncStatus").value("PENDING"))
                    .andReturn();

            String subscriptionId = readJson(createResult).at("/data/id").asText();
            mockMvc.perform(get("/api/v2/calendar/subscriptions")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(subscriptionId))
                    .andExpect(jsonPath("$.data[0].url").value(server.url()));

            mockMvc.perform(post("/api/v2/calendar/subscriptions/" + subscriptionId + "/sync")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.subscriptionId").value(subscriptionId))
                    .andExpect(jsonPath("$.data.syncStatus").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.importedCount").value(1));
        }

        mockMvc.perform(get("/api/v2/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-06-10T00:00:00")
                        .param("to", "2026-06-11T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Subscribed planning"));

        mockMvc.perform(get("/api/v2/calendar/default/ics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(content().contentTypeCompatibleWith("text/calendar"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BEGIN:VCALENDAR")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SUMMARY:Subscribed planning")));
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "Calendar Subscription"
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

    private record IcsTestServer(HttpServer server, String url) implements AutoCloseable {

        static IcsTestServer start(String content) throws Exception {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/calendar.ics", exchange -> {
                byte[] body = content.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/calendar; charset=utf-8");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream stream = exchange.getResponseBody()) {
                    stream.write(body);
                }
            });
            server.start();
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/calendar.ics";
            return new IcsTestServer(server, url);
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
