package com.mmmail.server.access;

import com.mmmail.common.exception.ErrorCode;
import com.mmmail.platform.access.AccessDecision;
import com.mmmail.platform.access.AccessDecisionReason;
import com.mmmail.platform.access.AccessEntitlement;
import com.mmmail.platform.access.AccessGate;
import com.mmmail.platform.access.AccessPermission;
import com.mmmail.platform.access.AccessRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class V21ApiAccessGateService implements AccessGate {

    private static final String AUTHENTICATION_REQUIRED = "Authentication required";
    private static final String ENTITLEMENT_REQUIRED = "Required entitlement is not enabled";
    private static final String PERMISSION_DENIED = "Required permission is not granted";
    private static final String UNKNOWN_CONTRACT = "Unknown v2 API contract";

    private final V21ApiEntitlementProvider entitlementProvider;

    public V21ApiAccessGateService(V21ApiEntitlementProvider entitlementProvider) {
        this.entitlementProvider = entitlementProvider;
    }

    @Override
    public AccessDecision evaluate(AccessRequest request) {
        if (request.contract() == null) {
            return denyUnknownContract();
        }
        List<AccessPermission> permissions = requiredPermissions(request);
        AccessEntitlement entitlement = requiredEntitlement(request);
        if (permissions.stream().anyMatch(AccessPermission::publicPermission)) {
            return AccessDecision.allowed(AccessDecisionReason.PUBLIC_CONTRACT, entitlement, permissions);
        }
        if (!request.authenticated()) {
            return denyAuthenticationRequired(entitlement, permissions);
        }
        if (!entitlementProvider.hasEntitlement(request.userId(), entitlement)) {
            return denyEntitlementRequired(entitlement, permissions);
        }
        if (!permissionGranted(request, entitlement)) {
            return denyPermissionDenied(entitlement, permissions);
        }
        return AccessDecision.allowed(AccessDecisionReason.ALLOWED, entitlement, permissions);
    }

    private static List<AccessPermission> requiredPermissions(AccessRequest request) {
        return request.contract().permissions().stream()
                .map(AccessPermission::new)
                .toList();
    }

    private static AccessEntitlement requiredEntitlement(AccessRequest request) {
        return AccessEntitlement.fromContractValue(request.contract().entitlement());
    }

    private static boolean permissionGranted(AccessRequest request, AccessEntitlement entitlement) {
        return request.admin() || AccessEntitlement.COMMUNITY == entitlement;
    }

    private static AccessDecision denyUnknownContract() {
        return AccessDecision.denied(
                AccessDecisionReason.UNKNOWN_CONTRACT,
                HttpStatus.FORBIDDEN.value(),
                ErrorCode.V2_API_CONTRACT_NOT_FOUND.getCode(),
                UNKNOWN_CONTRACT,
                AccessEntitlement.COMMUNITY,
                List.of()
        );
    }

    private static AccessDecision denyAuthenticationRequired(
            AccessEntitlement entitlement,
            List<AccessPermission> permissions
    ) {
        return AccessDecision.denied(
                AccessDecisionReason.AUTHENTICATION_REQUIRED,
                HttpStatus.UNAUTHORIZED.value(),
                ErrorCode.UNAUTHORIZED.getCode(),
                AUTHENTICATION_REQUIRED,
                entitlement,
                permissions
        );
    }

    private static AccessDecision denyEntitlementRequired(
            AccessEntitlement entitlement,
            List<AccessPermission> permissions
    ) {
        return AccessDecision.denied(
                AccessDecisionReason.ENTITLEMENT_REQUIRED,
                HttpStatus.FORBIDDEN.value(),
                ErrorCode.V2_ENTITLEMENT_REQUIRED.getCode(),
                ENTITLEMENT_REQUIRED,
                entitlement,
                permissions
        );
    }

    private static AccessDecision denyPermissionDenied(
            AccessEntitlement entitlement,
            List<AccessPermission> permissions
    ) {
        return AccessDecision.denied(
                AccessDecisionReason.PERMISSION_DENIED,
                HttpStatus.FORBIDDEN.value(),
                ErrorCode.V2_PERMISSION_DENIED.getCode(),
                PERMISSION_DENIED,
                entitlement,
                permissions
        );
    }
}
