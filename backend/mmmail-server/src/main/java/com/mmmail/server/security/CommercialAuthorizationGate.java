package com.mmmail.server.security;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.commercial.Edition;
import com.mmmail.server.commercial.EditionContext;
import com.mmmail.server.commercial.EditionContextResolver;
import com.mmmail.server.commercial.FeatureCode;
import com.mmmail.server.commercial.FeatureGate;
import com.mmmail.server.commercial.RequiresEdition;
import com.mmmail.server.commercial.RequiresFeature;
import com.mmmail.server.service.AuditService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CommercialAuthorizationGate {

    private static final String COMMERCIAL_DENIED_EVENT = "COMMERCIAL_ENTITLEMENT_DENIED";

    private final EditionContextResolver editionContextResolver;
    private final FeatureGate featureGate;
    private final AuditService auditService;

    public CommercialAuthorizationGate(
            EditionContextResolver editionContextResolver,
            FeatureGate featureGate,
            AuditService auditService
    ) {
        this.editionContextResolver = editionContextResolver;
        this.featureGate = featureGate;
        this.auditService = auditService;
    }

    public void enforce(HttpServletRequest request, Long orgId, Requirements requirements) {
        if (requirements.absent()) {
            return;
        }
        if (orgId == null) {
            throw missingOrgError(request, requirements);
        }
        EditionContext context = editionContextResolver.resolve(orgId);
        try {
            enforceEditionRequirement(requirements.editionRequirement(), context);
            enforceFeatureRequirement(requirements.featureRequirement(), context);
        } catch (BizException exception) {
            auditDenied(DeniedAccess.upgrade(request, context, requirements));
            throw exception;
        }
    }

    public void enforceFeature(HttpServletRequest request, Long orgId, FeatureCode featureCode) {
        if (orgId == null) {
            throw missingOrgError(request, featureCode.requiredEdition(), featureCode);
        }
        EditionContext context = editionContextResolver.resolve(orgId);
        try {
            featureGate.requireFeature(context, featureCode);
        } catch (BizException exception) {
            auditDenied(DeniedAccess.upgrade(request, context, featureCode));
            throw exception;
        }
    }

    private void enforceEditionRequirement(RequiresEdition requirement, EditionContext context) {
        if (requirement != null) {
            featureGate.requireEdition(context, requirement.value());
        }
    }

    private void enforceFeatureRequirement(RequiresFeature requirement, EditionContext context) {
        if (requirement != null) {
            featureGate.requireFeature(context, requirement.value());
        }
    }

    private BizException missingOrgError(HttpServletRequest request, Requirements requirements) {
        return missingOrgError(request, requirements.requiredEdition(), requirements.featureCode());
    }

    private BizException missingOrgError(HttpServletRequest request, Edition requiredEdition, FeatureCode featureCode) {
        String message = "Edition required: orgId=missing, requiredEdition=%s, currentEdition=UNKNOWN, upgradeAction=select-org"
                .formatted(requiredEdition);
        auditDenied(DeniedAccess.selectOrg(request, requiredEdition, featureCode));
        return new BizException(ErrorCode.V2_ENTITLEMENT_REQUIRED, message);
    }

    private void auditDenied(DeniedAccess access) {
        auditService.record(
                SecurityUtils.currentUserId(),
                COMMERCIAL_DENIED_EVENT,
                deniedDetail(access),
                access.request().getRemoteAddr(),
                access.orgId()
        );
    }

    private String deniedDetail(DeniedAccess access) {
        String detail = "path=%s,requiredEdition=%s,currentEdition=%s,upgradeAction=%s"
                .formatted(
                        access.request().getRequestURI(),
                        access.requiredEdition(),
                        access.currentEdition(),
                        access.upgradeAction()
                );
        if (access.featureCode() == null) {
            return detail;
        }
        return detail + ",feature=" + access.featureCode().code();
    }

    public record Requirements(RequiresEdition editionRequirement, RequiresFeature featureRequirement) {

        private boolean absent() {
            return editionRequirement == null && featureRequirement == null;
        }

        private Edition requiredEdition() {
            if (editionRequirement != null) {
                return editionRequirement.value();
            }
            return featureRequirement.value().requiredEdition();
        }

        private FeatureCode featureCode() {
            if (featureRequirement == null) {
                return null;
            }
            return featureRequirement.value();
        }
    }

    private record DeniedAccess(
            HttpServletRequest request,
            Long orgId,
            String currentEdition,
            Edition requiredEdition,
            FeatureCode featureCode,
            String upgradeAction
    ) {

        private static DeniedAccess upgrade(
                HttpServletRequest request,
                EditionContext context,
                Requirements requirements
        ) {
            return new DeniedAccess(
                    request,
                    context.orgId(),
                    context.edition().name(),
                    requirements.requiredEdition(),
                    requirements.featureCode(),
                    "upgrade"
            );
        }

        private static DeniedAccess upgrade(
                HttpServletRequest request,
                EditionContext context,
                FeatureCode featureCode
        ) {
            return new DeniedAccess(
                    request,
                    context.orgId(),
                    context.edition().name(),
                    featureCode.requiredEdition(),
                    featureCode,
                    "upgrade"
            );
        }

        private static DeniedAccess selectOrg(
                HttpServletRequest request,
                Edition requiredEdition,
                FeatureCode featureCode
        ) {
            return new DeniedAccess(
                    request,
                    null,
                    "UNKNOWN",
                    requiredEdition,
                    featureCode,
                    "select-org"
            );
        }
    }
}
