package com.mmmail.server.commercial;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class FeatureGate {

    public void requireEdition(EditionContext context, Edition requiredEdition) {
        if (!context.edition().allows(requiredEdition)) {
            throw denied(context, requiredEdition);
        }
    }

    public void requireFeature(EditionContext context, FeatureCode featureCode) {
        requireEdition(context, featureCode.requiredEdition());
    }

    private BizException denied(EditionContext context, Edition requiredEdition) {
        String message = "Edition required: orgId=%s, requiredEdition=%s, currentEdition=%s, upgradeAction=upgrade"
                .formatted(context.orgId(), requiredEdition, context.edition());
        return new BizException(ErrorCode.V2_ENTITLEMENT_REQUIRED, message);
    }
}
