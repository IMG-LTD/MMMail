package com.mmmail.server.commercial;

import com.mmmail.server.model.vo.CommercialLicenseStatusVo;
import org.springframework.stereotype.Service;

@Service
public class CommercialLicenseStatusReader {

    private final LicenseStateRepository repository;
    private final CommercialLicenseStatusMapper mapper;

    public CommercialLicenseStatusReader(
            LicenseStateRepository repository,
            CommercialLicenseStatusMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public CommercialLicenseStatusVo readStatus(long orgId) {
        return repository.findByOrgId(orgId)
                .map(mapper::fromState)
                .orElseGet(() -> mapper.missing(orgId));
    }
}
