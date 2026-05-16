package com.mmmail.server.commercial;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.dto.CommercialLicenseUploadRequest;
import com.mmmail.server.model.vo.CommercialLicenseStatusVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CommercialLicenseUploadService {

    private final LicenseSyncService syncService;
    private final LicensePublicKeyProvider publicKeyProvider;
    private final CommercialLicenseStatusMapper mapper;

    public CommercialLicenseUploadService(
            LicenseSyncService syncService,
            LicensePublicKeyProvider publicKeyProvider,
            CommercialLicenseStatusMapper mapper
    ) {
        this.syncService = syncService;
        this.publicKeyProvider = publicKeyProvider;
        this.mapper = mapper;
    }

    public CommercialLicenseStatusVo upload(long orgId, CommercialLicenseUploadRequest request) {
        String licenseKey = requireLicenseKey(request);
        try {
            LicenseState state = syncService.syncLicense(licenseKey, orgId, publicKeyProvider.requirePublicKey());
            return mapper.fromState(state);
        } catch (LicenseVerificationException ex) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "license verification failed: reason=" + ex.reason()
            );
        }
    }

    private String requireLicenseKey(CommercialLicenseUploadRequest request) {
        if (request == null || !StringUtils.hasText(request.licenseKey())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "licenseKey is required");
        }
        return request.licenseKey().trim();
    }
}
