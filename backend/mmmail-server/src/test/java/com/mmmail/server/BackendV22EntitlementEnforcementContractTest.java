package com.mmmail.server;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.commercial.Edition;
import com.mmmail.server.commercial.EditionContext;
import com.mmmail.server.commercial.EditionContextResolver;
import com.mmmail.server.commercial.FeatureCode;
import com.mmmail.server.commercial.FeatureGate;
import com.mmmail.server.commercial.RequiresEdition;
import com.mmmail.server.commercial.RequiresFeature;
import com.mmmail.server.security.AuthorizationAnnotationInterceptor;
import com.mmmail.server.security.CommercialAuthorizationGate;
import com.mmmail.server.security.JwtPrincipal;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackendV22EntitlementEnforcementContractTest {

    private static final long USER_ID = 42L;
    private static final long ORG_ID = 99L;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void freeOrgCallingBusinessFeatureShouldGetExplicitUpgradeErrorAndAudit() throws Exception {
        OrgProductAccessGuardService guardService = mock(OrgProductAccessGuardService.class);
        EditionContextResolver resolver = mock(EditionContextResolver.class);
        AuditService auditService = mock(AuditService.class);
        AuthorizationAnnotationInterceptor interceptor = interceptor(guardService, resolver, auditService);
        MockHttpServletRequest request = request("/api/v2/business/oidc");
        HandlerMethod handler = new HandlerMethod(new BusinessFeatureFixtureController(), "handle");
        authenticateAs("USER");

        when(guardService.resolveActiveOrgId(request)).thenReturn(ORG_ID);
        when(resolver.resolve(ORG_ID)).thenReturn(new EditionContext(ORG_ID, Edition.FREE));

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handler))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode())
                        .isEqualTo(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()))
                .hasMessageContaining("requiredEdition=BUSINESS")
                .hasMessageContaining("currentEdition=FREE")
                .hasMessageContaining("upgradeAction=upgrade");
        verify(auditService).record(eq(USER_ID), eq("COMMERCIAL_ENTITLEMENT_DENIED"), contains("feature=oidc.sso"), eq("203.0.113.10"), eq(ORG_ID));
    }

    @Test
    void businessOrgCallingBusinessFeatureShouldPassWithoutAuditFailure() throws Exception {
        OrgProductAccessGuardService guardService = mock(OrgProductAccessGuardService.class);
        EditionContextResolver resolver = mock(EditionContextResolver.class);
        AuditService auditService = mock(AuditService.class);
        AuthorizationAnnotationInterceptor interceptor = interceptor(guardService, resolver, auditService);
        MockHttpServletRequest request = request("/api/v2/business/oidc");
        HandlerMethod handler = new HandlerMethod(new BusinessFeatureFixtureController(), "handle");
        authenticateAs("ADMIN");

        when(guardService.resolveActiveOrgId(request)).thenReturn(ORG_ID);
        when(resolver.resolve(ORG_ID)).thenReturn(new EditionContext(ORG_ID, Edition.BUSINESS));

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handler)).isTrue();
        verify(auditService, never()).record(eq(USER_ID), eq("COMMERCIAL_ENTITLEMENT_DENIED"), contains("feature=oidc.sso"), eq("203.0.113.10"), eq(ORG_ID));
    }

    @Test
    void commercialGateWithoutOrgContextShouldFailExplicitlyAndAudit() throws Exception {
        OrgProductAccessGuardService guardService = mock(OrgProductAccessGuardService.class);
        EditionContextResolver resolver = mock(EditionContextResolver.class);
        AuditService auditService = mock(AuditService.class);
        AuthorizationAnnotationInterceptor interceptor = interceptor(guardService, resolver, auditService);
        MockHttpServletRequest request = request("/api/v2/billing/license");
        HandlerMethod handler = new HandlerMethod(new ProEditionFixtureController(), "handle");
        authenticateAs("USER");

        when(guardService.resolveActiveOrgId(request)).thenReturn(null);

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handler))
                .isInstanceOf(BizException.class)
                .satisfies(error -> assertThat(((BizException) error).getCode())
                        .isEqualTo(ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode()))
                .hasMessageContaining("requiredEdition=PRO")
                .hasMessageContaining("currentEdition=UNKNOWN")
                .hasMessageContaining("upgradeAction=select-org");
        verify(auditService).record(eq(USER_ID), eq("COMMERCIAL_ENTITLEMENT_DENIED"), contains("requiredEdition=PRO"), eq("203.0.113.10"), eq(null));
    }

    private AuthorizationAnnotationInterceptor interceptor(
            OrgProductAccessGuardService guardService,
            EditionContextResolver resolver,
            AuditService auditService
    ) {
        return new AuthorizationAnnotationInterceptor(
                guardService,
                mock(OrgProductAccessService.class),
                new CommercialAuthorizationGate(resolver, new FeatureGate(), auditService)
        );
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr("203.0.113.10");
        return request;
    }

    private void authenticateAs(String role) {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, "user@mmmail.local", role, 1, 7L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null)
        );
    }

    private static final class BusinessFeatureFixtureController {
        @RequiresFeature(FeatureCode.OIDC_SSO)
        public void handle() {
        }
    }

    private static final class ProEditionFixtureController {
        @RequiresEdition(Edition.PRO)
        public void handle() {
        }
    }
}
