package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.orggovernance.scope.OrgScopeAccessDecision;
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

    public OrgScopeAccessDecision resolveContext(HttpServletRequest request, Long userId) {
        Long activeOrgId = orgProductAccessGuardService.resolveActiveOrgId(request);
        if (activeOrgId == null) {
            return OrgScopeAccessDecision.personal();
        }
        return new OrgScopeAccessDecision(activeOrgId, orgProductAccessService.listEnabledProductKeys(userId, activeOrgId));
    }

    public void assertRemediationActionAllowed(OrgScopeAccessDecision context, String actionCode) {
        if (!context.active()) {
            return;
        }
        String productCode = resolveActionProductCode(actionCode);
        if (context.allows(productCode)) {
            return;
        }
        throw new BizException(
                ErrorCode.ORG_PRODUCT_ACCESS_DENIED,
                "Product " + productCode + " is disabled in active organization scope"
        );
    }

    public void assertRemediationActionsAllowed(OrgScopeAccessDecision context, List<String> actionCodes) {
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
}
