package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationRealtimeV212ContractIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final Duration FRAME_TIMEOUT = Duration.ofSeconds(3);

    @LocalServerPort
    private int port;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void notificationWsShouldPushPatchEventsAndSinceEndpointShouldReplayThem() throws Exception {
        assertThat(applicationContext.containsBean("notificationWebSocketHandler")).isTrue();
        String token = register("v212-notification-ws-" + System.nanoTime() + "@mmmail.local");
        seedNotification(token);
        String notificationId = listFirstNotificationId(token);

        BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        WebSocketSession session = connect(token, frames);
        session.sendMessage(new TextMessage("{\"type\":\"ping\"}"));
        String pongFrame = assertFrameContains(frames, "\"type\":\"pong\"");
        JsonNode pong = objectMapper.readTree(pongFrame);
        assertThat(pong.path("type").asText()).isEqualTo("pong");
        assertThat(pong.path("channel").asText()).startsWith("user/u_");
        assertThat(pong.path("seq").asLong(-1)).isZero();
        assertThat(pong.path("payload").isObject()).isTrue();

        mockMvc.perform(patch("/api/v2/notifications/" + notificationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READ\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READ"));

        String pushedFrame = assertFrameContains(frames, "\"type\":\"badge-update\"");
        assertThat(pushedFrame).contains("\"seq\":").contains("SUITE_NOTIFICATION_MARK_READ");

        JsonNode replay = getData(token, "/api/v2/notifications/since", "cursor", "0");
        JsonNode replayedEvent = findEvent(replay.path("events"), "badge-update");
        assertThat(replayedEvent.path("seq").asLong()).isGreaterThan(0);
        assertThat(replay.path("nextCursor").asLong()).isGreaterThanOrEqualTo(replayedEvent.path("seq").asLong());
        assertThat(replayedEvent.at("/payload/eventType").asText()).isEqualTo("SUITE_NOTIFICATION_MARK_READ");
        assertThat(replayedEvent.at("/payload/operation").asText()).isEqualTo("MARK_READ");

        session.close();
    }

    private WebSocketSession connect(String token, BlockingQueue<String> frames) throws Exception {
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String uri = "ws://127.0.0.1:" + port + "/ws/notifications?token=" + encodedToken + "&since=0";
        return new StandardWebSocketClient()
                .execute(new QueueingTextHandler(frames), uri)
                .get(FRAME_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    private String assertFrameContains(BlockingQueue<String> frames, String marker) throws InterruptedException {
        long deadline = System.nanoTime() + FRAME_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            String frame = frames.poll(200, TimeUnit.MILLISECONDS);
            if (frame != null && frame.contains(marker)) {
                return frame;
            }
        }
        throw new AssertionError("Expected WebSocket frame containing " + marker);
    }

    private String listFirstNotificationId(String token) throws Exception {
        JsonNode notifications = getData(token, "/api/v2/notifications", "limit", "20");
        assertThat(notifications).isNotEmpty();
        return notifications.path(0).path("id").asText();
    }

    private JsonNode getData(String token, String path, String... params) throws Exception {
        MockHttpServletRequestBuilder request = get(path).header("Authorization", "Bearer " + token);
        for (int index = 0; index < params.length; index += 2) {
            request.param(params[index], params[index + 1]);
        }
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return readJson(result).path("data");
    }

    private JsonNode findEvent(JsonNode events, String type) {
        for (JsonNode event : events) {
            if (type.equals(event.path("type").asText())) {
                return event;
            }
        }
        throw new AssertionError("Expected replay event type not found: " + type);
    }

    private void seedNotification(String token) throws Exception {
        mockMvc.perform(post("/api/v1/suite/governance/change-requests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "SECURITY_BASELINE_HARDENING",
                                  "reason": "v2.1.2 notification realtime seed"
                                }
                                """))
                .andExpect(status().isOk());
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "V212 Notifications"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static final class QueueingTextHandler extends TextWebSocketHandler {
        private final BlockingQueue<String> frames;

        private QueueingTextHandler(BlockingQueue<String> frames) {
            this.frames = frames;
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            frames.add(message.getPayload());
        }
    }
}
