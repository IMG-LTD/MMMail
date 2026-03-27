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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletParityAdvancedIntegrationTest {

    private static final String IMPORT_SEED_PHRASE =
            "abandon ability able about above absent absorb abstract absurd abuse access accident";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addressBookShouldExposeReceiveAndChangeListsWithSearchAndGapMetrics() throws Exception {
        SessionSeed owner = register("v112-wallet-address-book-owner");
        JsonNode account = createAccount(owner.token(), "Address Book BTC", "bc1qv112addressbook00001");
        String accountId = account.path("accountId").asText();

        JsonNode workspace = getData(authorized(get("/api/v1/wallet/accounts/" + accountId + "/parity-workspace"), owner.token()));
        assertThat(workspace.path("advancedSettings").path("addressType").asText()).isEqualTo("NATIVE_SEGWIT");
        assertThat(workspace.path("receiveAddresses")).hasSize(1);
        assertThat(workspace.path("changeAddresses")).hasSize(1);
        assertThat(workspace.path("changeAddresses").get(0).path("addressKind").asText()).isEqualTo("CHANGE");
        assertThat(workspace.path("changeAddresses").get(0).path("sourceType").asText()).isEqualTo("INTERNAL");

        rotateAddress(owner.token(), accountId, "Conference payee");
        rotateAddress(owner.token(), accountId, "Treasury reserve");

        JsonNode receiveBook = getData(authorized(get("/api/v1/wallet/accounts/" + accountId + "/address-book")
                .param("kind", "RECEIVE")
                .param("query", "conf"), owner.token()));
        assertThat(receiveBook.path("addressKind").asText()).isEqualTo("RECEIVE");
        assertThat(receiveBook.path("query").asText()).isEqualTo("conf");
        assertThat(receiveBook.path("gapLimit").asInt()).isEqualTo(3);
        assertThat(receiveBook.path("consecutiveUnusedCount").asInt()).isEqualTo(3);
        assertThat(receiveBook.path("addresses")).hasSize(1);
        assertThat(receiveBook.path("addresses").get(0).path("label").asText()).isEqualTo("Conference payee");
        assertThat(receiveBook.path("addresses").get(0).path("addressIndex").asInt()).isEqualTo(1);

        JsonNode changeBook = getData(authorized(get("/api/v1/wallet/accounts/" + accountId + "/address-book")
                .param("kind", "CHANGE"), owner.token()));
        assertThat(changeBook.path("total").asInt()).isEqualTo(1);
        assertThat(changeBook.path("addresses").get(0).path("reservedFor").asText()).isEqualTo("Internal change flow");
    }

    @Test
    void advancedSettingsShouldUpdateSlotAndRejectDuplicateSlot() throws Exception {
        SessionSeed owner = register("v112-wallet-advanced-owner");
        JsonNode accountA = createAccount(owner.token(), "Travel BTC", "bc1qv112travel00001");
        JsonNode accountB = createAccount(owner.token(), "Ops BTC", "bc1qv112ops00002");
        String accountAId = accountA.path("accountId").asText();
        String accountBId = accountB.path("accountId").asText();

        JsonNode updated = getData(authorized(put("/api/v1/wallet/accounts/" + accountAId + "/advanced-settings"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "walletName": "Travel Taproot",
                          "addressType": "TAPROOT",
                          "accountIndex": 2
                        }
                        """));
        assertThat(updated.path("walletName").asText()).isEqualTo("Travel Taproot");
        assertThat(updated.path("addressType").asText()).isEqualTo("TAPROOT");
        assertThat(updated.path("accountIndex").asInt()).isEqualTo(2);
        assertThat(updated.path("address").asText()).startsWith("bc1p");

        JsonNode refreshedWorkspace = getData(authorized(get("/api/v1/wallet/accounts/" + accountAId + "/parity-workspace"), owner.token()));
        assertThat(refreshedWorkspace.path("advancedSettings").path("addressType").asText()).isEqualTo("TAPROOT");
        assertThat(refreshedWorkspace.path("receiveAddresses").get(0).path("address").asText()).startsWith("bc1p");

        mockMvc.perform(authorized(put("/api/v1/wallet/accounts/" + accountBId + "/advanced-settings"), owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "addressType": "TAPROOT",
                                  "accountIndex": 2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    @Test
    void importWalletShouldStoreMetadataOnlyAndBlockRecoveryReveal() throws Exception {
        SessionSeed owner = register("v112-wallet-import-owner");

        JsonNode imported = getData(authorized(post("/api/v1/wallet/accounts/import"), owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "walletName": "Imported Vault",
                          "assetSymbol": "BTC",
                          "seedPhrase": "%s",
                          "passphrase": "hidden",
                          "addressType": "NATIVE_SEGWIT",
                          "accountIndex": 0
                        }
                        """.formatted(IMPORT_SEED_PHRASE)));
        String accountId = imported.path("accountId").asText();
        assertThat(imported.path("imported").asBoolean()).isTrue();
        assertThat(imported.path("walletPassphraseProtected").asBoolean()).isTrue();
        assertThat(imported.path("walletSourceFingerprint").asText()).hasSize(16);
        assertThat(imported.path("address").asText()).startsWith("bc1q");

        JsonNode workspace = getData(authorized(get("/api/v1/wallet/accounts/" + accountId + "/parity-workspace"), owner.token()));
        assertThat(workspace.path("advancedSettings").path("imported").asBoolean()).isTrue();
        assertThat(workspace.path("profile").path("recoveryPhrasePreview").asText()).isEmpty();
        assertThat(workspace.path("receiveAddresses")).hasSize(1);
        assertThat(workspace.path("changeAddresses")).hasSize(1);

        mockMvc.perform(authorized(post("/api/v1/wallet/accounts/" + accountId + "/recovery/reveal"), owner.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_ARGUMENT.getCode()));
    }

    private void rotateAddress(String token, String accountId, String label) throws Exception {
        getData(authorized(post("/api/v1/wallet/accounts/" + accountId + "/receive-addresses/rotate"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "label": "%s"
                        }
                        """.formatted(label)));
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
                                  "displayName": "Wallet Advanced"
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
