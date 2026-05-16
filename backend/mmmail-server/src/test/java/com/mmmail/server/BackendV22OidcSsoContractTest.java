package com.mmmail.server;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.commercial.Edition;
import com.mmmail.server.commercial.EditionContext;
import com.mmmail.server.commercial.FeatureGate;
import com.mmmail.server.commercial.oidc.OidcAuthorizationStartRequest;
import com.mmmail.server.commercial.oidc.OidcAuthorizationStartVo;
import com.mmmail.server.commercial.oidc.OidcCallbackResult;
import com.mmmail.server.commercial.oidc.OidcClientConfig;
import com.mmmail.server.commercial.oidc.OidcConfigRepository;
import com.mmmail.server.commercial.oidc.OidcIdTokenValidator;
import com.mmmail.server.commercial.oidc.OidcSessionIssuer;
import com.mmmail.server.commercial.oidc.OidcSessionRequest;
import com.mmmail.server.commercial.oidc.OidcSsoDependencies;
import com.mmmail.server.commercial.oidc.OidcSsoService;
import com.mmmail.server.commercial.oidc.OidcStateRecord;
import com.mmmail.server.commercial.oidc.OidcStateRepository;
import com.mmmail.server.commercial.oidc.OidcStateService;
import com.mmmail.server.commercial.oidc.OidcTokenExchangeClient;
import com.mmmail.server.commercial.oidc.OidcTokenResponse;
import com.mmmail.server.commercial.oidc.OidcUserIdentity;
import com.mmmail.server.model.vo.AuthResponse;
import com.mmmail.server.model.vo.UserProfileVo;
import com.mmmail.server.observability.RuntimeTraceService;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackendV22OidcSsoContractTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-17T02:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldDeclareOidcRuntimeDependencyConfigMigrationAndInventory() throws Exception {
        String pom = read("backend/mmmail-server/pom.xml");
        String application = read("backend/mmmail-server/src/main/resources/application.yml");
        String envExample = read(".env.example");
        String backendEnvExample = read("config/backend.env.example");
        String migration = read("backend/mmmail-server/src/main/resources/db/migration/V39__oidc_sso_init.sql");
        String inventory = read("docs/compliance/data-inventory.yaml");

        assertThat(pom).contains("spring-boot-starter-oauth2-client");
        assertThat(application).contains(
                "oidc:",
                "enabled: ${MMMAIL_OIDC_ENABLED:false}",
                "callback-path: ${MMMAIL_OIDC_CALLBACK_PATH:/api/v2/auth/oidc/callback}"
        );
        assertThat(envExample).contains(
                "MMMAIL_OIDC_ENABLED=false",
                "MMMAIL_OIDC_CALLBACK_PATH=/api/v2/auth/oidc/callback"
        );
        assertThat(backendEnvExample).contains(
                "MMMAIL_OIDC_ENABLED=false",
                "MMMAIL_OIDC_CLIENT_SECRET="
        );
        assertThat(migration).contains(
                "create table if not exists org_oidc_config",
                "create table if not exists oidc_auth_state",
                "uk_org_oidc_config_org",
                "uk_oidc_auth_state_state"
        );
        assertThat(inventory).contains(
                "org_oidc_config:",
                "oidc_auth_state:"
        );
    }

    @Test
    void stateServiceShouldCreatePkceAuthorizationUrlAndRejectUnsafeRedirects() {
        RecordingStateRepository repository = new RecordingStateRepository();
        OidcStateService service = new OidcStateService(repository, FIXED_CLOCK, tokens("state-token", "nonce-token", "verifier-token"));
        OidcClientConfig config = keycloakConfig();

        OidcAuthorizationStartVo start = service.start(config, new OidcAuthorizationStartRequest(
                99L,
                "https://app.example.com/admin/oidc/complete"
        ));
        Map<String, String> query = query(start.authorizationUrl());

        assertThat(start.state()).isEqualTo("state-token");
        assertThat(query).containsEntry("response_type", "code");
        assertThat(query).containsEntry("client_id", "mmmail-business");
        assertThat(query).containsEntry("redirect_uri", "https://app.example.com/api/v2/auth/oidc/callback");
        assertThat(query).containsEntry("scope", "openid email profile");
        assertThat(query).containsEntry("state", "state-token");
        assertThat(query).containsEntry("nonce", "nonce-token");
        assertThat(query).containsEntry("code_challenge_method", "S256");
        assertThat(query.get("code_challenge")).isNotBlank().isNotEqualTo("verifier-token");
        assertThat(repository.saved().codeVerifier()).isEqualTo("verifier-token");

        assertThatThrownBy(() -> service.start(config, new OidcAuthorizationStartRequest(
                99L,
                "https://evil.example.com/callback"
        )))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode())
                        .isEqualTo(ErrorCode.INVALID_ARGUMENT.getCode()))
                .hasMessageContaining("OIDC post-login redirect is not allowed");
    }

    @Test
    void stateServiceShouldConsumeStateOnceAndRejectExpiredOrMismatchedCallback() {
        RecordingStateRepository repository = new RecordingStateRepository();
        OidcStateService service = new OidcStateService(repository, FIXED_CLOCK, tokens("state-token", "nonce-token", "verifier-token"));
        OidcAuthorizationStartVo start = service.start(keycloakConfig(), new OidcAuthorizationStartRequest(
                99L,
                "https://app.example.com/admin/oidc/complete"
        ));

        OidcStateRecord consumed = service.consume(start.state(), "https://app.example.com/api/v2/auth/oidc/callback");

        assertThat(consumed.state()).isEqualTo("state-token");
        assertThat(repository.consumedState()).isEqualTo("state-token");
        assertThatThrownBy(() -> service.consume(start.state(), "https://app.example.com/api/v2/auth/oidc/callback"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("OIDC state is invalid");
        assertThatThrownBy(() -> service.consume("missing", "https://app.example.com/api/v2/auth/oidc/callback"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("OIDC state is invalid");
    }

    @Test
    void stateServiceShouldDeclareAutowiredRuntimeConstructor() {
        long autowiredRepositoryConstructors = Arrays.stream(OidcStateService.class.getConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Autowired.class))
                .filter(this::isRepositoryOnlyConstructor)
                .count();

        assertThat(autowiredRepositoryConstructors).isEqualTo(1);
    }

    @Test
    void callbackShouldExchangeCodeValidateIdTokenAndIssueMmmailSession() {
        RecordingStateRepository states = new RecordingStateRepository();
        OidcStateService stateService = new OidcStateService(states, FIXED_CLOCK, tokens("state-token", "nonce-token", "verifier-token"));
        RecordingTokenExchangeClient tokenExchange = new RecordingTokenExchangeClient();
        RecordingIdTokenValidator idTokenValidator = new RecordingIdTokenValidator();
        RecordingSessionIssuer sessionIssuer = new RecordingSessionIssuer();
        OidcSsoService service = oidcService(states, stateService, tokenExchange, idTokenValidator, sessionIssuer);

        stateService.start(keycloakConfig(), new OidcAuthorizationStartRequest(
                99L,
                "https://app.example.com/admin/oidc/complete"
        ));
        OidcCallbackResult result = service.callback(
                "state-token",
                "auth-code",
                "https://app.example.com/api/v2/auth/oidc/callback",
                "203.0.113.10"
        );

        assertThat(result.status()).isEqualTo("authenticated");
        assertThat(result.auth().accessToken()).isEqualTo("access-token");
        assertThat(tokenExchange.exchangeRequest()).isEqualTo("auth-code|verifier-token");
        assertThat(idTokenValidator.validationRequest()).isEqualTo("id-token|nonce-token");
        assertThat(sessionIssuer.request()).isEqualTo(new OidcSessionRequest(
                "idp-subject",
                "owner@example.com",
                "203.0.113.10"
        ));
    }

    @Test
    void shouldWireOidcControllerSecurityDocsAndGates() throws Exception {
        String controller = readServerJava("controller/OidcSsoController.java");
        String service = readServerJava("commercial/oidc/OidcSsoService.java");
        String securityConfig = readServerJava("config/SecurityConfig.java");
        String threatModel = read("docs/security/threat-model.md");
        String oidcDoc = read("docs/commercial/oidc-sso.md");
        String validateLocal = read("scripts/validate-local.sh");
        String ci = read(".github/workflows/ci.yml");
        String spec = read("docs/v22-open-source-commercial-spec.md");

        assertThat(controller).contains(
                "/api/v2/orgs/{orgId}/oidc/config",
                "/api/v2/auth/oidc/login",
                "/api/v2/auth/oidc/callback",
                "enforceFeature(request, orgId, FeatureCode.OIDC_SSO)"
        );
        assertThat(service).contains(
                "mmmail.oidc.callback",
                "state",
                "nonce",
                "codeVerifier",
                "tokenExchangeClient",
                "idTokenValidator",
                "sessionIssuer",
                "FeatureCode.OIDC_SSO"
        );
        assertThat(securityConfig).contains(
                "/api/v2/auth/oidc/login",
                "/api/v2/auth/oidc/callback"
        );
        assertThat(threatModel).contains("OIDC", "state", "nonce", "PKCE", "redirect_uri");
        assertThat(oidcDoc).contains("Keycloak", "callback URL", "飞书", "钉钉", "企业微信");
        assertThat(validateLocal).contains("BackendV22OidcSsoContractTest");
        assertThat(ci).contains("BackendV22OidcSsoContractTest");
        assertThat(spec).contains("| BUS-01 | partial done |", "BackendV22OidcSsoContractTest");
    }

    private OidcClientConfig keycloakConfig() {
        return new OidcClientConfig(
                99L,
                true,
                "https://idp.example.com/realms/mmmail",
                "mmmail-business",
                "MMMAIL_OIDC_CLIENT_SECRET",
                "https://app.example.com/api/v2/auth/oidc/callback",
                Set.of("openid", "email", "profile"),
                Set.of("https://app.example.com/admin/oidc/complete")
        );
    }

    private OidcSsoService oidcService(
            RecordingStateRepository states,
            OidcStateService stateService,
            RecordingTokenExchangeClient tokenExchange,
            RecordingIdTokenValidator idTokenValidator,
            RecordingSessionIssuer sessionIssuer
    ) {
        OidcSsoDependencies dependencies = new OidcSsoDependencies(
                new RecordingConfigRepository(states),
                stateService,
                orgId -> new EditionContext(orgId, Edition.BUSINESS),
                new FeatureGate(),
                new RuntimeTraceService(ObservationRegistry.NOOP),
                tokenExchange,
                idTokenValidator,
                sessionIssuer
        );
        return new OidcSsoService(dependencies);
    }

    private Supplier<String> tokens(String... values) {
        return new Supplier<>() {
            private int index;

            @Override
            public String get() {
                return values[index++];
            }
        };
    }

    private Map<String, String> query(String authorizationUrl) {
        String query = authorizationUrl.substring(authorizationUrl.indexOf('?') + 1);
        Map<String, String> values = new HashMap<>();
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            values.put(decode(pair[0]), pair.length == 1 ? "" : decode(pair[1]));
        }
        return values;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private boolean isRepositoryOnlyConstructor(Constructor<?> constructor) {
        return constructor.getParameterCount() == 1
                && constructor.getParameterTypes()[0].equals(OidcStateRepository.class);
    }

    private String readServerJava(String path) throws Exception {
        return read("backend/mmmail-server/src/main/java/com/mmmail/server/" + path);
    }

    private String read(String path) throws Exception {
        return Files.readString(repoRoot().resolve(path));
    }

    private Path repoRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null && !Files.exists(current.resolve(".github/workflows/ci.yml"))) {
            current = current.getParent();
        }
        assertThat(current).isNotNull();
        return current;
    }

    private static final class RecordingStateRepository implements OidcStateRepository {
        private OidcStateRecord saved;
        private String consumedState;

        @Override
        public void save(OidcStateRecord record) {
            this.saved = record;
        }

        @Override
        public Optional<OidcStateRecord> findActive(String state, Instant now) {
            if (saved == null || !saved.state().equals(state) || saved.expiresAt().isBefore(now) || saved.consumedAt() != null) {
                return Optional.empty();
            }
            return Optional.of(saved);
        }

        @Override
        public void markConsumed(String state, Instant consumedAt) {
            this.consumedState = state;
            this.saved = saved.markConsumed(consumedAt);
        }

        private OidcStateRecord saved() {
            return saved;
        }

        private String consumedState() {
            return consumedState;
        }
    }

    private static final class RecordingConfigRepository implements OidcConfigRepository {
        private final RecordingStateRepository states;

        private RecordingConfigRepository(RecordingStateRepository states) {
            this.states = states;
        }

        @Override
        public Optional<OidcClientConfig> findByOrgId(Long orgId) {
            if (states.saved() == null || !states.saved().orgId().equals(orgId)) {
                return Optional.empty();
            }
            return Optional.of(new OidcClientConfig(
                    orgId,
                    true,
                    "https://idp.example.com/realms/mmmail",
                    "mmmail-business",
                    "MMMAIL_OIDC_CLIENT_SECRET",
                    states.saved().callbackUri(),
                    Set.of("openid", "email", "profile"),
                    Set.of(states.saved().postLoginRedirectUri())
            ));
        }

        @Override
        public void save(OidcClientConfig config) {
        }
    }

    private static final class RecordingTokenExchangeClient implements OidcTokenExchangeClient {
        private String exchangeRequest;

        @Override
        public OidcTokenResponse exchange(OidcClientConfig config, OidcStateRecord state, String code) {
            this.exchangeRequest = code + "|" + state.codeVerifier();
            return new OidcTokenResponse("id-token", "idp-access-token", "idp-refresh-token", 300L, "Bearer");
        }

        private String exchangeRequest() {
            return exchangeRequest;
        }
    }

    private static final class RecordingIdTokenValidator implements OidcIdTokenValidator {
        private String validationRequest;

        @Override
        public OidcUserIdentity validate(OidcClientConfig config, OidcTokenResponse tokens, String nonce) {
            this.validationRequest = tokens.idToken() + "|" + nonce;
            return new OidcUserIdentity("idp-subject", "owner@example.com");
        }

        private String validationRequest() {
            return validationRequest;
        }
    }

    private static final class RecordingSessionIssuer implements OidcSessionIssuer {
        private OidcSessionRequest request;

        @Override
        public AuthResponse issue(OidcSessionRequest request) {
            this.request = request;
            return new AuthResponse("access-token", "refresh-token", new UserProfileVo(
                    7L,
                    request.email(),
                    "Owner",
                    "USER",
                    "alias"
            ));
        }

        private OidcSessionRequest request() {
            return request;
        }
    }
}
