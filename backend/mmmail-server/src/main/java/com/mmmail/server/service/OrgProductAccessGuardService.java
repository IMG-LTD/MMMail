package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrgProductAccessGuardService {

    public static final String ACTIVE_ORG_HEADER = "X-MMMAIL-ORG-ID";

    private final OrgProductAccessService orgProductAccessService;
    private final OrgPolicyService orgPolicyService;
    private final ProductRouteResolver productRouteResolver;

    public OrgProductAccessGuardService(
            OrgProductAccessService orgProductAccessService,
            OrgPolicyService orgPolicyService,
            ProductRouteResolver productRouteResolver
    ) {
        this.orgProductAccessService = orgProductAccessService;
        this.orgPolicyService = orgPolicyService;
        this.productRouteResolver = productRouteResolver;
    }

    public void enforce(HttpServletRequest request, Long userId) {
        Long activeOrgId = resolveActiveOrgId(request);
        if (activeOrgId == null) {
            return;
        }
        String productKey = productRouteResolver.resolveApiProductKey(request.getRequestURI());
        if (productKey == null) {
            return;
        }
        orgProductAccessService.assertCurrentUserProductEnabled(userId, activeOrgId, productKey);
        orgPolicyService.assertTwoFactorCompliant(userId, activeOrgId, productKey, request.getRemoteAddr());
    }

    public Long resolveActiveOrgId(HttpServletRequest request) {
        return parseActiveOrgId(request.getHeader(ACTIVE_ORG_HEADER));
    }

    private Long parseActiveOrgId(String rawHeader) {
        if (!StringUtils.hasText(rawHeader)) {
            return null;
        }
        try {
            return Long.parseLong(rawHeader.trim());
        } catch (NumberFormatException exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "X-MMMAIL-ORG-ID must be a numeric organization id");
        }
    }
}
