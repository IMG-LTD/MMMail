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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletParityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void workspaceShouldSeedDefaultsAndRotatePrivacyAddress() throws Exception {
        SessionSeed owner = register("v111-wallet-owner");
        JsonNode account = createAccount(owner.token(), "Self Custody BTC", "bc1qv111selfcustody00001");
        String accountId = account.path("accountId").asText();

        JsonNode workspace = getData(authorized(get("/api/v1/wallet/accounts/" + accountId + "/parity-workspace"), owner.token()));
        JsonNode profile = workspace.path("profile");
        assertThat(profile.path("bitcoinViaEmailEnabled").asBoolean()).isFalse();
        assertThat(profile.path("balanceMasked").asBoolean()).isFalse();
        assertThat(profile.path("addressPrivacyEnabled").asBoolean()).isTrue();
        assertThat(profile.path("addressPoolSize").asInt()).isEqualTo(3);
        assertThat(profile.path("recoveryPhrasePreview").asText()).contains("•••");
        assertThat(workspace.path("receiveAddresses")).hasSize(1);
        assertThat(workspace.path("receiveAddresses").get(0).path("sourceType").asText()).isEqualTo("PRIMARY");

        JsonNode updatedProfile = getData(authorized(put("/api/v1/wallet/accounts/" + accountId + "/parity-profile"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "bitcoinViaEmailEnabled": true,
                          "aliasEmail": "%s",
                          "balanceMasked": true,
                          "addressPrivacyEnabled": true,
                          "addressPoolSize": 5,
                          "passphraseHint": "metal vault"
                        }
                        """.formatted(owner.email())));
        assertThat(updatedProfile.path("bitcoinViaEmailEnabled").asBoolean()).isTrue();
        assertThat(updatedProfile.path("aliasEmail").asText()).isEqualTo(owner.email());
        assertThat(updatedProfile.path("balanceMasked").asBoolean()).isTrue();
        assertThat(updatedProfile.path("addressPoolSize").asInt()).isEqualTo(5);

        JsonNode rotatedAddress = getData(authorized(post("/api/v1/wallet/accounts/" + accountId + "/receive-addresses/rotate"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "label": "Conference payer"
                        }
                        """));
        assertThat(rotatedAddress.path("label").asText()).isEqualTo("Conference payer");
        assertThat(rotatedAddress.path("sourceType").asText()).isEqualTo("ROTATED");
        assertThat(rotatedAddress.path("address").asText()).startsWith("bc1q");

        JsonNode recovery = getData(authorized(post("/api/v1/wallet/accounts/" + accountId + "/recovery/reveal"), owner.token()));
        assertThat(recovery.path("accountId").asText()).isEqualTo(accountId);
        assertThat(recovery.path("wordCount").asInt()).isEqualTo(12);
        assertThat(recovery.path("recoveryPhrase").asText().split("\\s+")).hasSize(12);

        JsonNode refreshedWorkspace = getData(authorized(get("/api/v1/wallet/accounts/" + accountId + "/parity-workspace"), owner.token()));
        assertThat(refreshedWorkspace.path("receiveAddresses")).hasSize(2);
        assertThat(refreshedWorkspace.path("profile").path("lastRecoveryViewedAt").isNull()).isFalse();
    }

    @Test
    void emailTransferShouldCreateTransferAndAdvanceOnClaim() throws Exception {
        SessionSeed owner = register("v111-wallet-email-owner");
        SessionSeed recipient = register("v111-wallet-email-recipient");
        JsonNode account = createAccount(owner.token(), "Email BTC", "bc1qv111emailtransfer00002");
        String accountId = account.path("accountId").asText();

        getData(authorized(post("/api/v1/wallet/transactions/receive"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "accountId": %s,
                          "amountMinor": 420000,
                          "assetSymbol": "BTC",
                          "sourceAddress": "bc1qsourcewalletv11100001",
                          "memo": "Initial balance"
                        }
                        """.formatted(accountId)));

        getData(authorized(put("/api/v1/wallet/accounts/" + accountId + "/parity-profile"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "bitcoinViaEmailEnabled": true,
                          "aliasEmail": "%s",
                          "balanceMasked": false,
                          "addressPrivacyEnabled": true,
                          "addressPoolSize": 4,
                          "passphraseHint": "team treasury"
                        }
                        """.formatted(owner.email())));

        JsonNode transfer = getData(authorized(post("/api/v1/wallet/accounts/" + accountId + "/email-transfers"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "amountMinor": 120000,
                          "recipientEmail": "%s",
                          "deliveryMessage": "Launch budget",
                          "memo": "Email transfer"
                        }
                        """.formatted(recipient.email())));
        String transferId = transfer.path("transferId").asText();
        assertThat(transfer.path("status").asText()).isEqualTo("PENDING_CLAIM");
        assertThat(transfer.path("inviteRequired").asBoolean()).isFalse();
        assertThat(transfer.path("claimCode").asText()).startsWith("CLAIM-");

        JsonNode claimed = getData(authorized(post("/api/v1/wallet/email-transfers/" + transferId + "/claim"), owner.token()));
        assertThat(claimed.path("status").asText()).isEqualTo("CLAIMED");
        assertThat(claimed.path("claimedAt").isNull()).isFalse();

        JsonNode workspace = getData(authorized(get("/api/v1/wallet/accounts/" + accountId + "/parity-workspace"), owner.token()));
        assertThat(workspace.path("emailTransfers")).hasSize(1);
        assertThat(workspace.path("emailTransfers").get(0).path("status").asText()).isEqualTo("CLAIMED");

        JsonNode transactions = getData(authorized(get("/api/v1/wallet/transactions").param("accountId", accountId), owner.token()));
        assertThat(transactions).hasSize(2);
        assertThat(transactions.get(0).path("counterpartyAddress").asText()).isEqualTo("mailto:" + recipient.email());
        assertThat(transactions.get(0).path("status").asText()).isEqualTo("SIGNED");
    }

    private JsonNode createAccount(String token, String walletName, String address) throws Exception {
        return getData(authorized(post("/api/v1/wallet/accounts"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "walletName": "%s",
                          "assetSymbol": "BTC",
                          "address": "%s"
                        }
                        """.formatted(walletName, address)));
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
                                  "displayName": "Wallet Parity"
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
