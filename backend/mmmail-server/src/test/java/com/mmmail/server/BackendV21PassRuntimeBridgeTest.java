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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendV21PassRuntimeBridgeTest {

    private static final String PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void v21PassShouldUseRuntimePersonalVaultAndItemState() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String email = "v21-pass-owner-" + suffix + "@mmmail.local";
        String token = register(email, "V21 Pass Owner");

        assertPersonalVault(token, email, 0);
        String itemId = createV21Item(token, "Root Console", "LOGIN", "console.mmmail.local", "root@mmmail.local");

        mockMvc.perform(get("/api/v2/pass/items")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "Root"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(itemId))
                .andExpect(jsonPath("$.data[0].title").value("Root Console"))
                .andExpect(jsonPath("$.data[0].itemType").value("LOGIN"))
                .andExpect(jsonPath("$.data[0].scopeType").value("PERSONAL"));

        assertPersonalVault(token, email, 1);

        mockMvc.perform(patch("/api/v2/pass/items/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Root Console Updated",
                                  "itemType": "NOTE",
                                  "website": "console.mmmail.local",
                                  "username": "root@mmmail.local",
                                  "secretCiphertext": "ciphertext-updated",
                                  "note": "Updated through v2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(itemId))
                .andExpect(jsonPath("$.data.title").value("Root Console Updated"))
                .andExpect(jsonPath("$.data.itemType").value("NOTE"));
    }

    @Test
    void v21PassShouldKeepPremiumPathsBehindAccessGate() throws Exception {
        String token = register("v21-pass-gate-" + System.nanoTime() + "@mmmail.local", "V21 Pass Gate");

        assertPremiumGate(token, "/api/v2/pass/monitor");
        assertPremiumGate(token, "/api/v2/pass/secure-links");
        assertPremiumGate(token, "/api/v2/pass/aliases");
    }

    @Test
    void v21PassShouldRejectInvalidItemIds() throws Exception {
        String token = register("v21-pass-invalid-" + System.nanoTime() + "@mmmail.local", "V21 Pass Invalid");

        mockMvc.perform(patch("/api/v2/pass/items/not-a-number")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Invalid",
                                  "itemType": "LOGIN",
                                  "secretCiphertext": "ciphertext"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    private void assertPersonalVault(String token, String ownerEmail, int itemCount) throws Exception {
        mockMvc.perform(get("/api/v2/pass/vaults")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("personal"))
                .andExpect(jsonPath("$.data[0].name").value("Personal"))
                .andExpect(jsonPath("$.data[0].scopeType").value("PERSONAL"))
                .andExpect(jsonPath("$.data[0].ownerEmail").value(ownerEmail))
                .andExpect(jsonPath("$.data[0].itemCount").value(itemCount));
    }

    private String createV21Item(
            String token,
            String title,
            String itemType,
            String website,
            String username
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v2/pass/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "itemType": "%s",
                                  "website": "%s",
                                  "username": "%s",
                                  "secretCiphertext": "ciphertext",
                                  "note": "Created through v2"
                                }
                                """.formatted(title, itemType, website, username)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.itemType").value(itemType))
                .andExpect(jsonPath("$.data.scopeType").value("PERSONAL"))
                .andReturn();
        return readJson(result).at("/data/id").asText();
    }

    private void assertPremiumGate(String token, String path) throws Exception {
        mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()));
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
