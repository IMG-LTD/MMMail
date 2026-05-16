package com.mmmail.server;

import com.mmmail.common.exception.BizException;
import com.mmmail.server.controller.AuthenticatorController;
import com.mmmail.server.controller.DomainController;
import com.mmmail.server.controller.MailExternalAccountController;
import com.mmmail.server.controller.MeetController;
import com.mmmail.server.controller.SearchController;
import com.mmmail.server.controller.SecurityEventController;
import com.mmmail.server.controller.SheetsFormulaController;
import com.mmmail.server.controller.SystemHealthController;
import com.mmmail.server.controller.VpnController;
import com.mmmail.server.controller.WalletController;
import com.mmmail.server.controller.WalletParityController;
import com.mmmail.server.controller.WebPushController;
import com.mmmail.server.commercial.EditionContextResolver;
import com.mmmail.server.commercial.FeatureGate;
import com.mmmail.server.security.AuthorizationAnnotationInterceptor;
import com.mmmail.server.security.CommercialAuthorizationGate;
import com.mmmail.server.security.JwtPrincipal;
import com.mmmail.server.security.RequireEntitlement;
import com.mmmail.server.security.RequireRole;
import com.mmmail.server.service.AuditService;
import com.mmmail.server.service.OrgProductAccessGuardService;
import com.mmmail.server.service.OrgProductAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackendV212AuthorizationAnnotationsContractTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void premiumModuleControllersShouldDeclareBackendEntitlementGates() {
        assertEntitlement(WalletController.class, "WALLET");
        assertEntitlement(WalletParityController.class, "WALLET");
        assertEntitlement(VpnController.class, "VPN");
        assertEntitlement(MeetController.class, "MEET");
        assertEntitlement(AuthenticatorController.class, "AUTHENTICATOR");
        assertEntitlement(DomainController.class, "HOSTED");
        assertEntitlement(MailExternalAccountController.class, "MAIL");
        assertEntitlement(SheetsFormulaController.class, "SHEETS");
        assertEntitlement(WebPushController.class, "HOSTED");
    }

    @Test
    void adminSecuritySurfacesShouldDeclareRoleGates() {
        assertRole(SecurityEventController.class, "anomalies", "ADMIN");
        assertRole(SecurityEventController.class, "action", "ADMIN");
        assertRole(SearchController.class, "reindex", "ADMIN");
        assertRole(SearchController.class, "reindexJob", "ADMIN");
        assertRole(SystemHealthController.class, "getHealth", "ADMIN");
    }

    @Test
    void entitlementInterceptorShouldEnforceOrgScopedProductAccess() throws Exception {
        OrgProductAccessGuardService guardService = mock(OrgProductAccessGuardService.class);
        OrgProductAccessService accessService = mock(OrgProductAccessService.class);
        AuthorizationAnnotationInterceptor interceptor = interceptor(guardService, accessService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/wallet/accounts");
        HandlerMethod handler = new HandlerMethod(new EntitlementFixtureController(), "handle");
        authenticateAs("USER");

        when(guardService.resolveActiveOrgId(request)).thenReturn(99L);

        interceptor.preHandle(request, new MockHttpServletResponse(), handler);

        verify(accessService).assertCurrentUserProductEnabled(42L, 99L, "WALLET");
    }

    @Test
    void roleInterceptorShouldRejectMissingRole() throws Exception {
        AuthorizationAnnotationInterceptor interceptor = interceptor(
                mock(OrgProductAccessGuardService.class),
                mock(OrgProductAccessService.class)
        );
        HandlerMethod handler = new HandlerMethod(new RoleFixtureController(), "handle");
        authenticateAs("USER");

        assertThatThrownBy(() -> interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), handler))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode()).isEqualTo(30053))
                .hasMessageContaining("Required role is not granted");
    }

    private void assertEntitlement(Class<?> controller, String entitlement) {
        RequireEntitlement gate = controller.getAnnotation(RequireEntitlement.class);

        assertThat(gate).isNotNull();
        assertThat(gate.value()).containsExactly(entitlement);
    }

    private void assertRole(Class<?> controller, String methodName, String role) {
        Method method = Arrays.stream(controller.getDeclaredMethods())
                .filter(item -> item.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        RequireRole gate = method.getAnnotation(RequireRole.class);

        assertThat(gate).isNotNull();
        assertThat(gate.value()).containsExactly(role);
    }

    private AuthorizationAnnotationInterceptor interceptor(
            OrgProductAccessGuardService guardService,
            OrgProductAccessService accessService
    ) {
        return new AuthorizationAnnotationInterceptor(
                guardService,
                accessService,
                new CommercialAuthorizationGate(
                        mock(EditionContextResolver.class),
                        new FeatureGate(),
                        mock(AuditService.class)
                )
        );
    }

    private void authenticateAs(String role) {
        JwtPrincipal principal = new JwtPrincipal(42L, "user@mmmail.local", role, 1, 7L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null)
        );
    }

    @RequireEntitlement("WALLET")
    private static final class EntitlementFixtureController {
        public void handle() {
        }
    }

    private static final class RoleFixtureController {
        @RequireRole("ADMIN")
        public void handle() {
        }
    }
}
