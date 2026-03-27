package com.mmmail.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VpnWorkspaceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void settingsAndProfilesShouldPersistAndResetDeletedDefaultProfile() throws Exception {
        String token = register("v110-vpn-settings");

        JsonNode defaults = getData(authorized(get("/api/v1/vpn/settings"), token));
        assertThat(defaults.path("netshieldMode").asText()).isEqualTo("OFF");
        assertThat(defaults.path("killSwitchEnabled").asBoolean()).isFalse();
        assertThat(defaults.path("defaultConnectionMode").asText()).isEqualTo("FASTEST");
        assertThat(defaults.path("defaultProfileId").isNull()).isTrue();

        JsonNode createdProfile = getData(authorized(post("/api/v1/vpn/profiles"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Secure Core default",
                          "protocol": "WIREGUARD",
                          "routingMode": "SERVER",
                          "targetServerId": "CH-GVA-SC1",
                          "secureCoreEnabled": true,
                          "netshieldMode": "BLOCK_MALWARE",
                          "killSwitchEnabled": true
                        }
                        """));
        String profileId = createdProfile.path("profileId").asText();
        assertThat(profileId).isNotBlank();

        JsonNode profiles = getData(authorized(get("/api/v1/vpn/profiles"), token));
        assertThat(profiles).hasSize(1);
        assertThat(profiles.get(0).path("targetServerId").asText()).isEqualTo("CH-GVA-SC1");

        JsonNode updatedProfile = getData(authorized(put("/api/v1/vpn/profiles/" + profileId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Swiss privacy route",
                          "protocol": "OPENVPN_TCP",
                          "routingMode": "COUNTRY",
                          "targetCountry": "Switzerland",
                          "secureCoreEnabled": true,
                          "netshieldMode": "BLOCK_MALWARE_ADS_TRACKERS",
                          "killSwitchEnabled": true
                        }
                        """));
        assertThat(updatedProfile.path("name").asText()).isEqualTo("Swiss privacy route");
        assertThat(updatedProfile.path("protocol").asText()).isEqualTo("OPENVPN_TCP");
        assertThat(updatedProfile.path("routingMode").asText()).isEqualTo("COUNTRY");
        assertThat(updatedProfile.path("targetCountry").asText()).isEqualTo("Switzerland");

        JsonNode savedSettings = getData(authorized(put("/api/v1/vpn/settings"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "netshieldMode": "BLOCK_MALWARE_ADS_TRACKERS",
                          "killSwitchEnabled": true,
                          "defaultConnectionMode": "PROFILE",
                          "defaultProfileId": "%s"
                        }
                        """.formatted(profileId)));
        assertThat(savedSettings.path("defaultConnectionMode").asText()).isEqualTo("PROFILE");
        assertThat(savedSettings.path("defaultProfileId").asText()).isEqualTo(profileId);

        mockMvc.perform(authorized(delete("/api/v1/vpn/profiles/" + profileId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        JsonNode reopenedSettings = getData(authorized(get("/api/v1/vpn/settings"), token));
        assertThat(reopenedSettings.path("defaultConnectionMode").asText()).isEqualTo("FASTEST");
        assertThat(reopenedSettings.path("defaultProfileId").isNull()).isTrue();

        JsonNode fallbackQuickConnect = getData(authorized(post("/api/v1/vpn/sessions/quick-connect"), token));
        assertThat(fallbackQuickConnect.path("serverId").asText()).isEqualTo("SE-STO-04");
        assertThat(fallbackQuickConnect.path("connectionSource").asText()).isEqualTo("QUICK_CONNECT");
        assertThat(fallbackQuickConnect.path("profileId").isNull()).isTrue();
    }

    @Test
    void quickConnectHistoryAndManualConnectShouldExposePrivacyMetadata() throws Exception {
        String token = register("v110-vpn-history");

        JsonNode profile = getData(authorized(post("/api/v1/vpn/profiles"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Secure core quick connect",
                          "protocol": "WIREGUARD",
                          "routingMode": "SERVER",
                          "targetServerId": "CH-GVA-SC1",
                          "secureCoreEnabled": true,
                          "netshieldMode": "BLOCK_MALWARE",
                          "killSwitchEnabled": true
                        }
                        """));
        String profileId = profile.path("profileId").asText();

        getData(authorized(put("/api/v1/vpn/settings"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "netshieldMode": "BLOCK_MALWARE_ADS_TRACKERS",
                          "killSwitchEnabled": true,
                          "defaultConnectionMode": "PROFILE",
                          "defaultProfileId": "%s"
                        }
                        """.formatted(profileId)));

        JsonNode quickConnectSession = getData(authorized(post("/api/v1/vpn/sessions/quick-connect"), token));
        assertThat(quickConnectSession.path("serverId").asText()).isEqualTo("CH-GVA-SC1");
        assertThat(quickConnectSession.path("serverTier").asText()).isEqualTo("SECURE_CORE");
        assertThat(quickConnectSession.path("profileId").asText()).isEqualTo(profileId);
        assertThat(quickConnectSession.path("profileName").asText()).isEqualTo("Secure core quick connect");
        assertThat(quickConnectSession.path("netshieldMode").asText()).isEqualTo("BLOCK_MALWARE");
        assertThat(quickConnectSession.path("killSwitchEnabled").asBoolean()).isTrue();
        assertThat(quickConnectSession.path("connectionSource").asText()).isEqualTo("QUICK_CONNECT");

        JsonNode currentSession = getData(authorized(get("/api/v1/vpn/sessions/current"), token));
        assertThat(currentSession.path("sessionId").asText()).isEqualTo(quickConnectSession.path("sessionId").asText());
        assertThat(currentSession.path("profileId").asText()).isEqualTo(profileId);

        JsonNode disconnectedQuickConnect = getData(authorized(post("/api/v1/vpn/sessions/disconnect"), token));
        assertThat(disconnectedQuickConnect.path("status").asText()).isEqualTo("DISCONNECTED");

        JsonNode manualSession = getData(authorized(post("/api/v1/vpn/sessions/connect"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "serverId": "US-NYC-01",
                          "protocol": "WIREGUARD"
                        }
                        """));
        assertThat(manualSession.path("profileId").isNull()).isTrue();
        assertThat(manualSession.path("profileName").isNull()).isTrue();
        assertThat(manualSession.path("netshieldMode").asText()).isEqualTo("BLOCK_MALWARE_ADS_TRACKERS");
        assertThat(manualSession.path("killSwitchEnabled").asBoolean()).isTrue();
        assertThat(manualSession.path("connectionSource").asText()).isEqualTo("MANUAL");

        JsonNode disconnectedManual = getData(authorized(post("/api/v1/vpn/sessions/disconnect"), token));
        assertThat(disconnectedManual.path("status").asText()).isEqualTo("DISCONNECTED");

        JsonNode history = getData(authorized(get("/api/v1/vpn/sessions/history").param("limit", "5"), token));
        assertThat(history).hasSizeGreaterThanOrEqualTo(2);
        assertThat(history.get(0).path("serverId").asText()).isEqualTo("US-NYC-01");
        assertThat(history.get(0).path("profileId").isNull()).isTrue();
        assertThat(history.get(0).path("netshieldMode").asText()).isEqualTo("BLOCK_MALWARE_ADS_TRACKERS");
        assertThat(history.get(0).path("connectionSource").asText()).isEqualTo("MANUAL");
        assertThat(history.get(1).path("serverId").asText()).isEqualTo("CH-GVA-SC1");
        assertThat(history.get(1).path("profileId").asText()).isEqualTo(profileId);
        assertThat(history.get(1).path("profileName").asText()).isEqualTo("Secure core quick connect");
        assertThat(history.get(1).path("netshieldMode").asText()).isEqualTo("BLOCK_MALWARE");
        assertThat(history.get(1).path("connectionSource").asText()).isEqualTo("QUICK_CONNECT");
    }

    @Test
    void stringProfileIdsShouldWorkForSettingsAndProfileQuickConnect() throws Exception {
        String token = register("v110-vpn-string-id");
        String profileId = createProfile(token, "String id route").path("profileId").asText();

        JsonNode savedSettings = putSettings(token, profileId);
        assertThat(savedSettings.path("defaultProfileId").asText()).isEqualTo(profileId);

        JsonNode profileQuickConnect = getData(authorized(post("/api/v1/vpn/sessions/quick-connect"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "profileId": "%s"
                        }
                        """.formatted(profileId)));
        assertThat(profileQuickConnect.path("profileId").asText()).isEqualTo(profileId);
        assertThat(profileQuickConnect.path("connectionSource").asText()).isEqualTo("PROFILE");
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

    private String register(String prefix) throws Exception {
        String email = prefix + "-" + System.nanoTime() + "@mmmail.local";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Password@123",
                                  "displayName": "VPN Workspace"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }

    private JsonNode createProfile(String token, String name) throws Exception {
        return getData(authorized(post("/api/v1/vpn/profiles"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "%s",
                          "protocol": "WIREGUARD",
                          "routingMode": "SERVER",
                          "targetServerId": "CH-GVA-SC1",
                          "secureCoreEnabled": true,
                          "netshieldMode": "BLOCK_MALWARE",
                          "killSwitchEnabled": true
                        }
                        """.formatted(name)));
    }

    private JsonNode putSettings(String token, String profileId) throws Exception {
        return getData(authorized(put("/api/v1/vpn/settings"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "netshieldMode": "BLOCK_MALWARE_ADS_TRACKERS",
                          "killSwitchEnabled": true,
                          "defaultConnectionMode": "PROFILE",
                          "defaultProfileId": "%s"
                        }
                        """.formatted(profileId)));
    }
}
