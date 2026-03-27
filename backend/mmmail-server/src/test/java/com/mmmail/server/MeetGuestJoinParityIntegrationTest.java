package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MeetGuestJoinParityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void publicRoomShouldSupportGuestRequestApprovalAndSessionFlow() throws Exception {
        SessionSeed owner = register("v114-meet-guest-owner");
        activateMeet(owner.token());
        JsonNode room = createRoom(owner.token(), "Guest lobby room", "PUBLIC");
        String roomId = room.path("roomId").asText();
        String joinCode = room.path("joinCode").asText();

        JsonNode overview = getData(get("/api/v1/public/meet/join/{joinCode}", joinCode));
        assertThat(overview.path("guestJoinEnabled").asBoolean()).isTrue();
        assertThat(overview.path("roomId").asText()).isEqualTo(roomId);

        JsonNode guestRequest = getData(post("/api/v1/public/meet/join/{joinCode}/requests", joinCode)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "displayName": "Guest Runner",
                          "audioEnabled": true,
                          "videoEnabled": false
                        }
                        """));
        assertThat(guestRequest.path("status").asText()).isEqualTo("PENDING");
        assertThat(guestRequest.path("requestToken").asText()).isNotBlank();

        JsonNode queue = getData(authorized(get("/api/v1/meet/rooms/{roomId}/guest-requests", roomId), owner.token()));
        assertThat(queue.isArray()).isTrue();
        assertThat(queue.get(0).path("displayName").asText()).isEqualTo("Guest Runner");

        String requestId = guestRequest.path("requestId").asText();
        JsonNode approved = getData(authorized(
                post("/api/v1/meet/rooms/{roomId}/guest-requests/{requestId}/approve", roomId, requestId),
                owner.token()
        ));
        assertThat(approved.path("status").asText()).isEqualTo("APPROVED");
        assertThat(approved.path("guestSessionToken").asText()).isNotBlank();

        JsonNode polledRequest = getData(get("/api/v1/public/meet/requests/{requestToken}", guestRequest.path("requestToken").asText()));
        assertThat(polledRequest.path("status").asText()).isEqualTo("APPROVED");
        String guestSessionToken = polledRequest.path("guestSessionToken").asText();

        JsonNode session = getData(get("/api/v1/public/meet/sessions/{guestSessionToken}", guestSessionToken));
        assertThat(session.path("sessionStatus").asText()).isEqualTo("ACTIVE");
        assertThat(session.path("participants").isArray()).isTrue();
        assertThat(session.path("participants").size()).isEqualTo(2);

        JsonNode mediaUpdated = getData(post("/api/v1/public/meet/sessions/{guestSessionToken}/media", guestSessionToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "audioEnabled": false,
                          "videoEnabled": true,
                          "screenSharing": false
                        }
                        """));
        assertThat(mediaUpdated.path("selfParticipant").path("audioEnabled").asBoolean()).isFalse();
        assertThat(mediaUpdated.path("selfParticipant").path("videoEnabled").asBoolean()).isTrue();

        JsonNode left = getData(post("/api/v1/public/meet/sessions/{guestSessionToken}/leave", guestSessionToken));
        assertThat(left.path("sessionStatus").asText()).isEqualTo("LEFT");
    }

    @Test
    void privateRoomShouldRejectGuestJoinAndHostCanRejectPendingRequest() throws Exception {
        SessionSeed owner = register("v114-meet-guest-private");
        activateMeet(owner.token());
        JsonNode privateRoom = createRoom(owner.token(), "Private room", "PRIVATE");
        String privateJoinCode = privateRoom.path("joinCode").asText();

        mockMvc.perform(post("/api/v1/public/meet/join/{joinCode}/requests", privateJoinCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Blocked Guest",
                                  "audioEnabled": true,
                                  "videoEnabled": true
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.getCode()));

        JsonNode publicRoom = createRoom(owner.token(), "Reject room", "PUBLIC");
        String publicRoomId = publicRoom.path("roomId").asText();
        String publicJoinCode = publicRoom.path("joinCode").asText();

        JsonNode request = getData(post("/api/v1/public/meet/join/{joinCode}/requests", publicJoinCode)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "displayName": "Rejected Guest",
                          "audioEnabled": true,
                          "videoEnabled": true
                        }
                        """));
        JsonNode rejected = getData(authorized(
                post("/api/v1/meet/rooms/{roomId}/guest-requests/{requestId}/reject", publicRoomId, request.path("requestId").asText()),
                owner.token()
        ));
        assertThat(rejected.path("status").asText()).isEqualTo("REJECTED");

        JsonNode polled = getData(get("/api/v1/public/meet/requests/{requestToken}", request.path("requestToken").asText()));
        assertThat(polled.path("status").asText()).isEqualTo("REJECTED");
    }

    private JsonNode createRoom(String token, String topic, String accessLevel) throws Exception {
        return getData(authorized(post("/api/v1/meet/rooms"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "topic": "%s",
                          "accessLevel": "%s",
                          "maxParticipants": 6
                        }
                        """.formatted(topic, accessLevel)));
    }

    private void activateMeet(String token) throws Exception {
        getData(authorized(post("/api/v1/suite/subscription/change"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "planCode": "UNLIMITED"
                        }
                        """));
        getData(authorized(post("/api/v1/meet/access/activate"), token));
    }

    private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private JsonNode getData(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private SessionSeed register(String prefix) throws Exception {
        String email = prefix + "-" + System.nanoTime() + "@mmmail.local";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password@123",
                                  "displayName": "Meet Guest"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return new SessionSeed(data.path("accessToken").asText(), email);
    }

    private record SessionSeed(String token, String email) {
    }
}
