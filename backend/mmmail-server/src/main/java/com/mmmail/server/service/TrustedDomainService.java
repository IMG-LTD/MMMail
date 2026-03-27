package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.TrustedDomainMapper;
import com.mmmail.server.model.entity.TrustedDomain;
import com.mmmail.server.model.vo.TrustedDomainVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrustedDomainService {

    private final TrustedDomainMapper trustedDomainMapper;
    private final AuditService auditService;

    public TrustedDomainService(TrustedDomainMapper trustedDomainMapper, AuditService auditService) {
        this.trustedDomainMapper = trustedDomainMapper;
        this.auditService = auditService;
    }

    public List<TrustedDomainVo> listTrustedDomains(Long userId) {
        return trustedDomainMapper.selectList(new LambdaQueryWrapper<TrustedDomain>()
                        .eq(TrustedDomain::getOwnerId, userId)
                        .orderByDesc(TrustedDomain::getCreatedAt))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public TrustedDomainVo addTrustedDomain(Long userId, String domain, String ipAddress) {
        TrustedDomain trustedDomain = upsertTrustedDomain(userId, domain);
        auditService.record(userId, "TRUSTED_DOMAIN_ADD", "domain=" + trustedDomain.getDomain(), ipAddress);
        return toVo(trustedDomain);
    }

    @Transactional
    public void removeTrustedDomain(Long userId, Long trustedDomainId, String ipAddress) {
        TrustedDomain trustedDomain = trustedDomainMapper.selectOne(new LambdaQueryWrapper<TrustedDomain>()
                .eq(TrustedDomain::getId, trustedDomainId)
                .eq(TrustedDomain::getOwnerId, userId));
        if (trustedDomain == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Trusted domain not found");
        }
        trustedDomainMapper.deleteById(trustedDomainId);
        auditService.record(userId, "TRUSTED_DOMAIN_REMOVE", "domain=" + trustedDomain.getDomain(), ipAddress);
    }

    public boolean isTrustedDomain(Long userId, String domain) {
        return findMatchedTrustedDomain(userId, domain) != null;
    }

    public String findMatchedTrustedDomain(Long userId, String domain) {
        String normalizedDomain = DomainRuleMatcher.normalizeHostDomain(domain);
        List<String> rules = trustedDomainMapper.selectList(new LambdaQueryWrapper<TrustedDomain>()
                        .eq(TrustedDomain::getOwnerId, userId))
                .stream()
                .map(TrustedDomain::getDomain)
                .toList();
        return DomainRuleMatcher.findMatchedRule(rules, normalizedDomain);
    }

    @Transactional
    public boolean addTrustedDomainIfAbsent(Long userId, String domain) {
        String normalizedDomain = normalizeDomain(domain);
        TrustedDomain existing = trustedDomainMapper.selectOne(new LambdaQueryWrapper<TrustedDomain>()
                .eq(TrustedDomain::getOwnerId, userId)
                .eq(TrustedDomain::getDomain, normalizedDomain));
        if (existing != null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        TrustedDomain trustedDomain = new TrustedDomain();
        trustedDomain.setOwnerId(userId);
        trustedDomain.setDomain(normalizedDomain);
        trustedDomain.setCreatedAt(now);
        trustedDomain.setUpdatedAt(now);
        trustedDomain.setDeleted(0);
        trustedDomainMapper.insert(trustedDomain);
        return true;
    }

    @Transactional
    public boolean removeTrustedDomainIfPresent(Long userId, String domain) {
        String normalizedDomain = normalizeDomain(domain);
        TrustedDomain existing = trustedDomainMapper.selectOne(new LambdaQueryWrapper<TrustedDomain>()
                .eq(TrustedDomain::getOwnerId, userId)
                .eq(TrustedDomain::getDomain, normalizedDomain));
        if (existing == null) {
            return false;
        }
        trustedDomainMapper.deleteById(existing.getId());
        return true;
    }

    private TrustedDomain upsertTrustedDomain(Long userId, String domain) {
        String normalizedDomain = normalizeDomain(domain);
        TrustedDomain existing = trustedDomainMapper.selectOne(new LambdaQueryWrapper<TrustedDomain>()
                .eq(TrustedDomain::getOwnerId, userId)
                .eq(TrustedDomain::getDomain, normalizedDomain));
        if (existing != null) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        TrustedDomain trustedDomain = new TrustedDomain();
        trustedDomain.setOwnerId(userId);
        trustedDomain.setDomain(normalizedDomain);
        trustedDomain.setCreatedAt(now);
        trustedDomain.setUpdatedAt(now);
        trustedDomain.setDeleted(0);
        trustedDomainMapper.insert(trustedDomain);
        return trustedDomain;
    }

    private String normalizeDomain(String domain) {
        return DomainRuleMatcher.normalizeRulePattern(domain);
    }

    private TrustedDomainVo toVo(TrustedDomain trustedDomain) {
        return new TrustedDomainVo(
                String.valueOf(trustedDomain.getId()),
                trustedDomain.getDomain(),
                trustedDomain.getCreatedAt()
        );
    }
}
