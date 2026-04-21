package com.mmmail.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicShareCapabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void capabilityEndpointShouldRemainPublic() throws Exception {
        mockMvc.perform(get("/api/v2/public-share/capabilities"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void v1PublicControllersStillExposeTheFrozenPaths() throws Exception {
        mockMvc.perform(get("/api/v1/public/pass/secure-links/demo-token"))
                .andExpect(status().is4xxClientError());
    }
}
