package com.mmmail.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequestHeaderContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void corsPreflightShouldAllowOrgAndScopeHeaders() throws Exception {
        MvcResult result = mockMvc.perform(options("/api/v1/auth/refresh")
                        .header("Origin", "http://127.0.0.1:5174")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-mmmail-org-id,x-mmmail-scope-id"))
                .andExpect(status().isOk())
                .andReturn();

        String allowHeaders = result.getResponse().getHeader("Access-Control-Allow-Headers");
        assertThat(allowHeaders)
                .isNotBlank()
                .containsIgnoringCase("X-MMMAIL-ORG-ID")
                .containsIgnoringCase("X-MMMAIL-SCOPE-ID");
    }
}
