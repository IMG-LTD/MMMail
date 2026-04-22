package com.mmmail.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicShareCapabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicShareCapabilitiesShouldExposeFrozenPayloadShape() throws Exception {
        mockMvc.perform(get("/api/v2/public-share/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.states.length()").value(7))
                .andExpect(jsonPath("$.data.states[0]").value("token-valid"))
                .andExpect(jsonPath("$.data.states[1]").value("password-required"))
                .andExpect(jsonPath("$.data.states[2]").value("unlocked"))
                .andExpect(jsonPath("$.data.states[3]").value("expired"))
                .andExpect(jsonPath("$.data.states[4]").value("revoked"))
                .andExpect(jsonPath("$.data.states[5]").value("locked"))
                .andExpect(jsonPath("$.data.states[6]").value("download-blocked"))
                .andExpect(jsonPath("$.data.auditedActions.length()").value(4))
                .andExpect(jsonPath("$.data.auditedActions[0]").value("preview"))
                .andExpect(jsonPath("$.data.auditedActions[1]").value("download"))
                .andExpect(jsonPath("$.data.auditedActions[2]").value("copy"))
                .andExpect(jsonPath("$.data.auditedActions[3]").value("reshare"))
                .andExpect(jsonPath("$.data.passwordHeader").value("X-Drive-Share-Password"))
                .andExpect(jsonPath("$.data.supportsAudit").value(true))
                .andExpect(jsonPath("$.data.supportsPasswordUnlock").value(true));
    }

    @Test
    void v1PublicControllersStillExposeTheFrozenPaths() throws Exception {
        mockMvc.perform(get("/api/v1/public/pass/secure-links/demo-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pass secure link is not found"));
    }
}
