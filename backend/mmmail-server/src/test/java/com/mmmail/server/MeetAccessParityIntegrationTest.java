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
class MeetAccessParityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void freePlanShouldExposeWaitlistAndGateWorkspace() throws Exception {
        SessionSeed owner = register("v113-meet-free-owner");

        JsonNode overview = getData(authorized(get("/api/v1/meet/access/overview"), owner.token()));
        assertThat(overview.path("planCode").asText()).isEqualTo("FREE");
        assertThat(overview.path("eligibleForInstantAccess").asBoolean()).isFalse();
        assertThat(overview.path("accessGranted").asBoolean()).isFalse();
        assertThat(overview.path("recommendedAction").asText()).isEqualTo("JOIN_WAITLIST");

        JsonNode waitlist = getData(authorized(post("/api/v1/meet/access/waitlist"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "note": "Notify me when Meet private beta expands"
                        }
                        """));
        assertThat(waitlist.path("waitlistRequested").asBoolean()).isTrue();
        assertThat(waitlist.path("accessState").asText()).isEqualTo("WAITLISTED");
        assertThat(waitlist.path("recommendedAction").asText()).isEqualTo("CONTACT_SALES");

        JsonNode contact = getData(authorized(post("/api/v1/meet/access/contact-sales"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "companyName": "MMMail Labs",
                          "requestedSeats": 120,
                          "note": "Need encrypted internal meetings"
                        }
                        """));
        assertThat(contact.path("salesContactRequested").asBoolean()).isTrue();
        assertThat(contact.path("accessState").asText()).isEqualTo("CONTACT_REQUESTED");
        assertThat(contact.path("recommendedAction").asText()).isEqualTo("CONTACT_REQUESTED");
        assertThat(contact.path("companyName").asText()).isEqualTo("MMMail Labs");
        assertThat(contact.path("requestedSeats").asInt()).isEqualTo(120);

        mockMvc.perform(authorized(post("/api/v1/meet/rooms"), owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "Blocked beta room",
                                  "accessLevel": "PRIVATE",
                                  "maxParticipants": 8
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.getCode()));
    }

    @Test
    void activationShouldRequireEligiblePlan() throws Exception {
        SessionSeed owner = register("v113-meet-ineligible-owner");

        mockMvc.perform(authorized(post("/api/v1/meet/access/activate"), owner.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.FORBIDDEN.getCode()));
    }

    @Test
    void unlimitedPlanShouldActivateMeetAndAllowRoomLifecycle() throws Exception {
        SessionSeed owner = register("v113-meet-unlimited-owner");

        getData(authorized(post("/api/v1/suite/subscription/change"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "planCode": "UNLIMITED"
                        }
                        """));

        JsonNode activated = getData(authorized(post("/api/v1/meet/access/activate"), owner.token()));
        assertThat(activated.path("planCode").asText()).isEqualTo("UNLIMITED");
        assertThat(activated.path("eligibleForInstantAccess").asBoolean()).isTrue();
        assertThat(activated.path("accessGranted").asBoolean()).isTrue();
        assertThat(activated.path("recommendedAction").asText()).isEqualTo("OPEN_WORKSPACE");

        JsonNode room = getData(authorized(post("/api/v1/meet/rooms"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "topic": "Unlimited beta room",
                          "accessLevel": "PRIVATE",
                          "maxParticipants": 6
                        }
                        """));
        String roomId = room.path("roomId").asText();
        assertThat(roomId).isNotBlank();

        JsonNode current = getData(authorized(get("/api/v1/meet/rooms/current"), owner.token()));
        assertThat(current.path("roomId").asText()).isEqualTo(roomId);
        assertThat(current.path("topic").asText()).isEqualTo("Unlimited beta room");
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
                                  "displayName": "Meet Access"
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
