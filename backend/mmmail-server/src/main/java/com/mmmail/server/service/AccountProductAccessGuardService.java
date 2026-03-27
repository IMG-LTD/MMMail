package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AccountProductAccessGuardService {

    private static final Set<String> PROTON_ADDRESS_REQUIRED_PRODUCTS = Set.of("MAIL", "CALENDAR");

    private final ProductRouteResolver productRouteResolver;
    private final UserPreferenceService userPreferenceService;

    public AccountProductAccessGuardService(
            ProductRouteResolver productRouteResolver,
            UserPreferenceService userPreferenceService
    ) {
        this.productRouteResolver = productRouteResolver;
        this.userPreferenceService = userPreferenceService;
    }

    public void enforce(HttpServletRequest request, Long userId) {
        String productKey = productRouteResolver.resolveApiProductKey(request.getRequestURI());
        if (productKey == null || !PROTON_ADDRESS_REQUIRED_PRODUCTS.contains(productKey)) {
            return;
        }
        if (!userPreferenceService.isExternalAccount(userId)) {
            return;
        }
        throw new BizException(ErrorCode.ACCOUNT_MAIL_ADDRESS_REQUIRED, productKey + " requires a Proton Mail address");
    }
}
