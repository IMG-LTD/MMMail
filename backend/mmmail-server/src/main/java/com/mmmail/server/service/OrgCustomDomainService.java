package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.OrgCustomDomainMapper;
import com.mmmail.server.model.dto.CreateOrgCustomDomainRequest;
import com.mmmail.server.model.entity.OrgCustomDomain;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.vo.DomainDnsDiagnosticRecordVo;
import com.mmmail.server.model.vo.DomainDnsDiagnosticsVo;
import com.mmmail.server.model.vo.DomainDnsRecordVo;
import com.mmmail.server.model.vo.DomainDnsRecordsVo;
import com.mmmail.server.model.vo.OrgCustomDomainVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class OrgCustomDomainService {

    private static final String STATUS_PENDING = "PENDING_VERIFICATION";
    private static final String STATUS_VERIFIED = "VERIFIED";
    private static final int DEFAULT_FALSE = 0;
    private static final int DEFAULT_TRUE = 1;
    private static final int TOKEN_LENGTH = 12;
    private static final String DIAGNOSTICS_READY = "READY";
    private static final String DIAGNOSTICS_ACTION_REQUIRED = "ACTION_REQUIRED";

    private final OrgAccessService orgAccessService;
    private final OrgCustomDomainMapper orgCustomDomainMapper;
    private final OrgMailIdentityService orgMailIdentityService;
    private final DomainDnsLookupService domainDnsLookupService;
    private final AuditService auditService;

    public OrgCustomDomainService(
            OrgAccessService orgAccessService,
            OrgCustomDomainMapper orgCustomDomainMapper,
            OrgMailIdentityService orgMailIdentityService,
            DomainDnsLookupService domainDnsLookupService,
            AuditService auditService
    ) {
        this.orgAccessService = orgAccessService;
        this.orgCustomDomainMapper = orgCustomDomainMapper;
        this.orgMailIdentityService = orgMailIdentityService;
        this.domainDnsLookupService = domainDnsLookupService;
        this.auditService = auditService;
    }

    public List<OrgCustomDomainVo> listDomains(Long userId, Long orgId, String ipAddress) {
        orgAccessService.requireActiveMember(userId, orgId);
        auditService.record(userId, "ORG_DOMAIN_LIST", "orgId=" + orgId, ipAddress, orgId);
        return orgCustomDomainMapper.selectList(new LambdaQueryWrapper<OrgCustomDomain>()
                        .eq(OrgCustomDomain::getOrgId, orgId))
                .stream()
                .sorted(Comparator.comparing(OrgCustomDomain::getIsDefault).reversed()
                        .thenComparing(OrgCustomDomain::getUpdatedAt, Comparator.reverseOrder()))
                .map(this::toVo)
                .toList();
    }

    public OrgCustomDomainVo getDomain(Long userId, Long orgId, Long domainId, String ipAddress) {
        orgAccessService.requireActiveMember(userId, orgId);
        OrgCustomDomain domain = loadDomain(orgId, domainId);
        auditService.record(userId, "ORG_DOMAIN_READ", "orgId=" + orgId + ",domainId=" + domainId, ipAddress, orgId);
        return toVo(domain);
    }

    @Transactional
    public OrgCustomDomainVo createDomain(Long userId, Long orgId, CreateOrgCustomDomainRequest request, String ipAddress) {
        OrgMember actor = orgAccessService.requireManageMember(userId, orgId);
        String normalizedDomain = normalizeDomain(request.domain());
        assertDomainUnique(orgId, normalizedDomain);
        LocalDateTime now = LocalDateTime.now();
        OrgCustomDomain domain = new OrgCustomDomain();
        domain.setOrgId(orgId);
        domain.setDomain(normalizedDomain);
        domain.setVerificationToken(generateVerificationToken());
        domain.setStatus(STATUS_PENDING);
        domain.setIsDefault(hasAnyDefault(orgId) ? DEFAULT_FALSE : DEFAULT_TRUE);
        domain.setCreatedBy(userId);
        domain.setCreatedAt(now);
        domain.setUpdatedAt(now);
        domain.setDeleted(DEFAULT_FALSE);
        orgCustomDomainMapper.insert(domain);
        auditService.record(userId, "ORG_DOMAIN_ADD", "orgId=" + orgId + ",domain=" + normalizedDomain + ",role=" + actor.getRole(), ipAddress, orgId);
        return toVo(domain);
    }

    @Transactional
    public OrgCustomDomainVo verifyDomain(Long userId, Long orgId, Long domainId, String ipAddress) {
        orgAccessService.requireManageMember(userId, orgId);
        OrgCustomDomain domain = loadDomain(orgId, domainId);
        if (STATUS_VERIFIED.equals(domain.getStatus())) {
            return toVo(domain);
        }
        domain.setStatus(STATUS_VERIFIED);
        domain.setVerifiedAt(LocalDateTime.now());
        domain.setUpdatedAt(LocalDateTime.now());
        orgCustomDomainMapper.updateById(domain);
        auditService.record(userId, "ORG_DOMAIN_VERIFY", "orgId=" + orgId + ",domain=" + domain.getDomain(), ipAddress, orgId);
        return toVo(domain);
    }

    public DomainDnsRecordsVo getExpectedDnsRecords(Long userId, Long orgId, Long domainId, String ipAddress) {
        orgAccessService.requireActiveMember(userId, orgId);
        OrgCustomDomain domain = loadDomain(orgId, domainId);
        auditService.record(userId, "ORG_DOMAIN_DNS_RECORDS", "orgId=" + orgId + ",domainId=" + domainId, ipAddress, orgId);
        return new DomainDnsRecordsVo(expectedRecords(domain));
    }

    public DomainDnsDiagnosticsVo diagnoseDomain(Long userId, Long orgId, Long domainId, String ipAddress) {
        orgAccessService.requireActiveMember(userId, orgId);
        OrgCustomDomain domain = loadDomain(orgId, domainId);
        List<DomainDnsDiagnosticRecordVo> records = expectedRecords(domain).stream()
                .map(record -> diagnoseRecord(domain.getDomain(), record))
                .toList();
        String status = records.stream().allMatch(DomainDnsDiagnosticRecordVo::matched)
                ? DIAGNOSTICS_READY
                : DIAGNOSTICS_ACTION_REQUIRED;
        auditService.record(userId, "ORG_DOMAIN_DIAGNOSTICS", "orgId=" + orgId + ",domainId=" + domainId + ",status=" + status, ipAddress, orgId);
        return new DomainDnsDiagnosticsVo(String.valueOf(domain.getId()), domain.getDomain(), status, records);
    }

    @Transactional
    public OrgCustomDomainVo verifyDomainWithDns(Long userId, Long orgId, Long domainId, String ipAddress) {
        DomainDnsDiagnosticsVo diagnostics = diagnoseDomain(userId, orgId, domainId, ipAddress);
        if (!DIAGNOSTICS_READY.equals(diagnostics.status())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Custom domain DNS records are incomplete");
        }

        return verifyDomain(userId, orgId, domainId, ipAddress);
    }

    @Transactional
    public OrgCustomDomainVo setDefaultDomain(Long userId, Long orgId, Long domainId, String ipAddress) {
        orgAccessService.requireManageMember(userId, orgId);
        OrgCustomDomain target = loadDomain(orgId, domainId);
        if (!STATUS_VERIFIED.equals(target.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Only verified domains can be the default domain");
        }
        List<OrgCustomDomain> domains = orgCustomDomainMapper.selectList(new LambdaQueryWrapper<OrgCustomDomain>()
                .eq(OrgCustomDomain::getOrgId, orgId));
        for (OrgCustomDomain domain : domains) {
            int nextDefault = domain.getId().equals(target.getId()) ? DEFAULT_TRUE : DEFAULT_FALSE;
            if (domain.getIsDefault() != nextDefault) {
                domain.setIsDefault(nextDefault);
                domain.setUpdatedAt(LocalDateTime.now());
                orgCustomDomainMapper.updateById(domain);
            }
        }
        auditService.record(userId, "ORG_DOMAIN_SET_DEFAULT", "orgId=" + orgId + ",domain=" + target.getDomain(), ipAddress, orgId);
        return toVo(loadDomain(orgId, domainId));
    }

    @Transactional
    public void removeDomain(Long userId, Long orgId, Long domainId, String ipAddress) {
        orgAccessService.requireManageMember(userId, orgId);
        OrgCustomDomain domain = loadDomain(orgId, domainId);
        if (Integer.valueOf(DEFAULT_TRUE).equals(domain.getIsDefault())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Set another default domain before removing the current default domain");
        }
        if (orgMailIdentityService.hasActiveIdentitiesForDomain(orgId, domainId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Remove attached mail identities before deleting this domain");
        }
        orgCustomDomainMapper.deleteById(domain.getId());
        auditService.record(userId, "ORG_DOMAIN_REMOVE", "orgId=" + orgId + ",domain=" + domain.getDomain(), ipAddress, orgId);
    }

    private OrgCustomDomain loadDomain(Long orgId, Long domainId) {
        OrgCustomDomain domain = orgCustomDomainMapper.selectOne(new LambdaQueryWrapper<OrgCustomDomain>()
                .eq(OrgCustomDomain::getOrgId, orgId)
                .eq(OrgCustomDomain::getId, domainId)
                .last("limit 1"));
        if (domain == null) {
            throw new BizException(ErrorCode.ORG_CUSTOM_DOMAIN_NOT_FOUND, "Custom domain not found");
        }
        return domain;
    }

    private void assertDomainUnique(Long orgId, String domain) {
        Long count = orgCustomDomainMapper.selectCount(new LambdaQueryWrapper<OrgCustomDomain>()
                .eq(OrgCustomDomain::getOrgId, orgId)
                .eq(OrgCustomDomain::getDomain, domain));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Custom domain already exists");
        }
    }

    private boolean hasAnyDefault(Long orgId) {
        return orgCustomDomainMapper.selectCount(new LambdaQueryWrapper<OrgCustomDomain>()
                .eq(OrgCustomDomain::getOrgId, orgId)
                .eq(OrgCustomDomain::getIsDefault, DEFAULT_TRUE)) > 0;
    }

    private String normalizeDomain(String rawDomain) {
        String normalized = rawDomain == null ? "" : rawDomain.trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized) || !normalized.contains(".")) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "A valid custom domain is required");
        }
        if (normalized.startsWith(".") || normalized.endsWith(".")) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Custom domain format is invalid");
        }
        return normalized;
    }

    private String generateVerificationToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, TOKEN_LENGTH).toUpperCase(Locale.ROOT);
    }

    private List<DomainDnsRecordVo> expectedRecords(OrgCustomDomain domain) {
        String name = domain.getDomain();

        return List.of(
                new DomainDnsRecordVo("TXT", "_mmmail-verify", "mmmail-verify=" + domain.getVerificationToken()),
                new DomainDnsRecordVo("TXT", "@", "v=spf1 include:_spf.mmmail.com ~all"),
                new DomainDnsRecordVo("TXT", "_dmarc", "v=DMARC1; p=quarantine; rua=mailto:dmarc@" + name),
                new DomainDnsRecordVo("CNAME", "mm._domainkey", "mm.dkim.mmmail.com"),
                new DomainDnsRecordVo("MX", "@", "10 inbound.mmmail.com")
        );
    }

    private DomainDnsDiagnosticRecordVo diagnoseRecord(String domain, DomainDnsRecordVo record) {
        List<String> actual = resolveRecord(domain, record);
        boolean matched = actual.stream().map(this::normalizeDnsValue)
                .anyMatch(value -> value.equals(normalizeDnsValue(record.expected())));

        return new DomainDnsDiagnosticRecordVo(record.type(), record.host(), record.expected(), actual, matched);
    }

    private List<String> resolveRecord(String domain, DomainDnsRecordVo record) {
        String host = resolveHost(domain, record.host());
        return switch (record.type()) {
            case "TXT" -> domainDnsLookupService.resolveTxt(host);
            case "CNAME" -> domainDnsLookupService.resolveCname(host);
            case "MX" -> domainDnsLookupService.resolveMx(host);
            default -> throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported DNS record type");
        };
    }

    private String resolveHost(String domain, String host) {
        return "@".equals(host) ? domain : host + "." + domain;
    }

    private String normalizeDnsValue(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.endsWith(".") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private OrgCustomDomainVo toVo(OrgCustomDomain domain) {
        return new OrgCustomDomainVo(
                String.valueOf(domain.getId()),
                String.valueOf(domain.getOrgId()),
                domain.getDomain(),
                domain.getVerificationToken(),
                domain.getStatus(),
                Integer.valueOf(DEFAULT_TRUE).equals(domain.getIsDefault()),
                domain.getCreatedBy() == null ? null : String.valueOf(domain.getCreatedBy()),
                domain.getVerifiedAt(),
                domain.getUpdatedAt()
        );
    }
}
