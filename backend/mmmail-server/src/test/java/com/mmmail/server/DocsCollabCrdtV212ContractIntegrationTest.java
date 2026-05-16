package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocsCollabCrdtV212ContractIntegrationTest {

    private static final String PASSWORD = "Password@123";
    private static final Duration FRAME_TIMEOUT = Duration.ofSeconds(3);

    @LocalServerPort
    private int port;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void docsCollabSnapshotShouldPersistOpaqueCrdtState() throws Exception {
        String token = register("v212-crdt-snapshot-" + System.nanoTime() + "@mmmail.local");
        String noteId = createNote(token);
        String snapshotBase64 = Base64.getEncoder().encodeToString(new byte[] {1, 2, 3, 4});

        mockMvc.perform(post("/api/v1/collab/docs/" + noteId + "/snapshot")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 1,
                                  "snapshotBase64": "%s"
                                }
                                """.formatted(snapshotBase64)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resourceType").value("docs"))
                .andExpect(jsonPath("$.data.resourceId").value(noteId))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.snapshotBase64").value(snapshotBase64));

        mockMvc.perform(get("/api/v1/collab/docs/" + noteId + "/snapshot")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.snapshotBase64").value(snapshotBase64));
    }

    @Test
    void docsCollabSnapshotShouldTreatStaleVersionAsConflictAndReturnLatest() throws Exception {
        String token = register("v212-crdt-snapshot-conflict-" + System.nanoTime() + "@mmmail.local");
        String noteId = createNote(token);
        String firstSnapshot = Base64.getEncoder().encodeToString(new byte[] {1, 2, 3, 4});
        String staleSnapshot = Base64.getEncoder().encodeToString(new byte[] {5, 6, 7, 8});

        mockMvc.perform(post("/api/v1/collab/docs/" + noteId + "/snapshot")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 1,
                                  "snapshotBase64": "%s"
                                }
                                """.formatted(firstSnapshot)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.snapshotBase64").value(firstSnapshot));

        mockMvc.perform(post("/api/v1/collab/docs/" + noteId + "/snapshot")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 1,
                                  "snapshotBase64": "%s"
                                }
                                """.formatted(staleSnapshot)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.snapshotBase64").value(firstSnapshot));
    }

    @Test
    void docsCollabWebSocketShouldBroadcastBinaryUpdatesAndStoreWal() throws Exception {
        String token = register("v212-crdt-ws-" + System.nanoTime() + "@mmmail.local");
        String noteId = createNote(token);
        BlockingQueue<byte[]> received = new LinkedBlockingQueue<>();
        WebSocketSession receiver = connect(token, noteId, received);
        WebSocketSession sender = connect(token, noteId, new LinkedBlockingQueue<>());
        byte[] update = new byte[] {9, 8, 7, 6};

        sender.sendMessage(new BinaryMessage(update));

        assertThat(received.poll(FRAME_TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isEqualTo(update);
        Integer updateCount = jdbcTemplate.queryForObject(
                "select count(*) from collab_update where resource_type = ? and resource_id = ?",
                Integer.class,
                "docs",
                noteId
        );
        assertThat(updateCount).isEqualTo(1);
        sender.close();
        receiver.close();
    }

    @Test
    void docsCollabWebSocketShouldReplayStoredWalToLateJoiner() throws Exception {
        String token = register("v212-crdt-ws-replay-" + System.nanoTime() + "@mmmail.local");
        String noteId = createNote(token);
        WebSocketSession sender = connect(token, noteId, new LinkedBlockingQueue<>());
        byte[] update = new byte[] {4, 3, 2, 1};
        sender.sendMessage(new BinaryMessage(update));
        sender.close();

        BlockingQueue<byte[]> replayed = new LinkedBlockingQueue<>();
        WebSocketSession receiver = connect(token, noteId, replayed);

        assertThat(replayed.poll(FRAME_TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isEqualTo(update);
        receiver.close();
    }

    @Test
    void docsCollabTextPingShouldReturnGatewayEnvelope() throws Exception {
        String token = register("v212-crdt-ping-" + System.nanoTime() + "@mmmail.local");
        String noteId = createNote(token);
        BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        WebSocketSession session = connectText(token, noteId, frames);

        session.sendMessage(new TextMessage("""
                {"type":"ping","channel":"collab/docs/%s","seq":1,"payload":{}}
                """.formatted(noteId)));

        String pongFrame = frames.poll(FRAME_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        assertThat(pongFrame).isNotNull();
        JsonNode pong = objectMapper.readTree(pongFrame);
        assertThat(pong.path("type").asText()).isEqualTo("pong");
        assertThat(pong.path("channel").asText()).isEqualTo("docs/" + noteId);
        assertThat(pong.path("seq").asLong(-1)).isZero();
        assertThat(pong.path("payload").isObject()).isTrue();
        session.close();
    }

    private WebSocketSession connect(String token, String noteId, BlockingQueue<byte[]> frames) throws Exception {
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String uri = "ws://127.0.0.1:%d/ws/collab/docs/%s?token=%s".formatted(port, noteId, encodedToken);
        return new StandardWebSocketClient()
                .execute(new QueueingBinaryHandler(frames), uri)
                .get(FRAME_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    private WebSocketSession connectText(String token, String noteId, BlockingQueue<String> frames) throws Exception {
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String uri = "ws://127.0.0.1:%d/ws/collab/docs/%s?token=%s".formatted(port, noteId, encodedToken);
        return new StandardWebSocketClient()
                .execute(new QueueingTextHandler(frames), uri)
                .get(FRAME_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    private String createNote(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/docs/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "v2.1.2 CRDT doc",
                                  "content": "initial"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "displayName": "V212 CRDT"
                                }
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).at("/data/accessToken").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static final class QueueingBinaryHandler extends BinaryWebSocketHandler {
        private final BlockingQueue<byte[]> frames;

        private QueueingBinaryHandler(BlockingQueue<byte[]> frames) {
            this.frames = frames;
        }

        @Override
        protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
            ByteBuffer buffer = message.getPayload();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            frames.add(bytes);
        }
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
