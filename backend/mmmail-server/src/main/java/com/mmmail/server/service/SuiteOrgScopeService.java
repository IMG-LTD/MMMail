package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
public class SuiteOrgScopeService {

    private static final List<String> ACTION_PRODUCT_PREFIXES = List.of(
            "STANDARD_NOTES",
            "SIMPLELOGIN",
            "AUTHENTICATOR",
            "CALENDAR",
            "DRIVE",
            "DOCS",
            "SHEETS",
            "WALLET",
            "MAIL",
            "PASS",
            "MEET",
            "LUMO",
            "VPN"
    );

    private final OrgProductAccessGuardService orgProductAccessGuardService;
    private final OrgProductAccessService orgProductAccessService;

    public SuiteOrgScopeService(
            OrgProductAccessGuardService orgProductAccessGuardService,
            OrgProductAccessService orgProductAccessService
    ) {
        this.orgProductAccessGuardService = orgProductAccessGuardService;
        this.orgProductAccessService = orgProductAccessService;
    }

    public SuiteOrgScopeContext resolveContext(HttpServletRequest request, Long userId) {
        Long activeOrgId = orgProductAccessGuardService.resolveActiveOrgId(request);
        if (activeOrgId == null) {
            return SuiteOrgScopeContext.personal();
        }
        return new SuiteOrgScopeContext(activeOrgId, orgProductAccessService.listEnabledProductKeys(userId, activeOrgId));
    }

    public void assertRemediationActionAllowed(SuiteOrgScopeContext context, String actionCode) {
        if (!context.active()) {
            return;
        }
        String productCode = resolveActionProductCode(actionCode);
        if (isProductVisible(context.visibleProductCodes(), productCode)) {
            return;
        }
        throw new BizException(
                ErrorCode.ORG_PRODUCT_ACCESS_DENIED,
                "Product " + productCode + " is disabled in active organization scope"
        );
    }

    public void assertRemediationActionsAllowed(SuiteOrgScopeContext context, List<String> actionCodes) {
        if (actionCodes == null || actionCodes.isEmpty()) {
            return;
        }
        for (String actionCode : actionCodes) {
            assertRemediationActionAllowed(context, actionCode);
        }
    }

    public String resolveActionProductCode(String actionCode) {
        if (!StringUtils.hasText(actionCode)) {
            return null;
        }
        String normalized = actionCode.trim().toUpperCase();
        for (String productCode : ACTION_PRODUCT_PREFIXES) {
            if (normalized.startsWith(productCode + "_")) {
                return productCode;
            }
        }
        return null;
    }

    public boolean isProductVisible(Set<String> visibleProductCodes, String productCode) {
        if (visibleProductCodes == null || visibleProductCodes.isEmpty()) {
            return true;
        }
        if (!StringUtils.hasText(productCode)) {
            return true;
        }
        return visibleProductCodes.contains(productCode.trim().toUpperCase());
    }

    public record SuiteOrgScopeContext(Long activeOrgId, Set<String> visibleProductCodes) {

        public static SuiteOrgScopeContext personal() {
            return new SuiteOrgScopeContext(null, Set.of());
        }

        public boolean active() {
            return activeOrgId != null;
        }
    }
}
