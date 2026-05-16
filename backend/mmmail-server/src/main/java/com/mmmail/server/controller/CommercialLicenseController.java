package com.mmmail.server.controller;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.common.model.Result;
import com.mmmail.server.commercial.CommercialLicenseStatusReader;
import com.mmmail.server.commercial.CommercialLicenseUploadService;
import com.mmmail.server.commercial.Edition;
import com.mmmail.server.model.dto.CommercialLicenseUploadRequest;
import com.mmmail.server.model.vo.CommercialLicenseStatusVo;
import com.mmmail.server.service.OrgProductAccessGuardService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/billing/license")
public class CommercialLicenseController {

    private final OrgProductAccessGuardService orgProductAccessGuardService;
    private final CommercialLicenseStatusReader statusReader;
    private final CommercialLicenseUploadService uploadService;

    public CommercialLicenseController(
            OrgProductAccessGuardService orgProductAccessGuardService,
            CommercialLicenseStatusReader statusReader,
            CommercialLicenseUploadService uploadService
    ) {
        this.orgProductAccessGuardService = orgProductAccessGuardService;
        this.statusReader = statusReader;
        this.uploadService = uploadService;
    }

    @GetMapping("/status")
    public Result<CommercialLicenseStatusVo> readStatus(HttpServletRequest request) {
        return Result.success(statusReader.readStatus(activeOrgId(request)));
    }

    @PostMapping
    public Result<CommercialLicenseStatusVo> uploadLicense(
            @Valid @RequestBody CommercialLicenseUploadRequest requestBody,
            HttpServletRequest request
    ) {
        return Result.success(uploadService.upload(activeOrgId(request), requestBody));
    }

    private long activeOrgId(HttpServletRequest request) {
        Long orgId = orgProductAccessGuardService.resolveActiveOrgId(request);
        if (orgId == null) {
            throw missingOrgError();
        }
        return orgId;
    }

    private BizException missingOrgError() {
        String message = "Edition required: orgId=missing, requiredEdition=%s, currentEdition=UNKNOWN, upgradeAction=select-org"
                .formatted(Edition.PRO);
        return new BizException(ErrorCode.V2_ENTITLEMENT_REQUIRED, message);
    }
}
