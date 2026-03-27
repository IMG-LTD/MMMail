package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.BlockedDomainMapper;
import com.mmmail.server.model.entity.BlockedDomain;
import com.mmmail.server.model.vo.BlockedDomainVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BlockedDomainService {

    private final BlockedDomainMapper blockedDomainMapper;
    private final AuditService auditService;

    public BlockedDomainService(BlockedDomainMapper blockedDomainMapper, AuditService auditService) {
        this.blockedDomainMapper = blockedDomainMapper;
        this.auditService = auditService;
    }

    public List<BlockedDomainVo> listBlockedDomains(Long userId) {
        return blockedDomainMapper.selectList(new LambdaQueryWrapper<BlockedDomain>()
                        .eq(BlockedDomain::getOwnerId, userId)
                        .orderByDesc(BlockedDomain::getCreatedAt))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public BlockedDomainVo addBlockedDomain(Long userId, String domain, String ipAddress) {
        BlockedDomain blockedDomain = upsertBlockedDomain(userId, domain);
        auditService.record(userId, "BLOCKED_DOMAIN_ADD", "domain=" + blockedDomain.getDomain(), ipAddress);
        return toVo(blockedDomain);
    }

    @Transactional
    public void removeBlockedDomain(Long userId, Long blockedDomainId, String ipAddress) {
        BlockedDomain blockedDomain = blockedDomainMapper.selectOne(new LambdaQueryWrapper<BlockedDomain>()
                .eq(BlockedDomain::getId, blockedDomainId)
                .eq(BlockedDomain::getOwnerId, userId));
        if (blockedDomain == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Blocked domain not found");
        }
        blockedDomainMapper.deleteById(blockedDomainId);
        auditService.record(userId, "BLOCKED_DOMAIN_REMOVE", "domain=" + blockedDomain.getDomain(), ipAddress);
    }

    public boolean isBlockedDomain(Long userId, String domain) {
        return findMatchedBlockedDomain(userId, domain) != null;
    }

    public String findMatchedBlockedDomain(Long userId, String domain) {
        String normalizedDomain = DomainRuleMatcher.normalizeHostDomain(domain);
        List<String> rules = blockedDomainMapper.selectList(new LambdaQueryWrapper<BlockedDomain>()
                        .eq(BlockedDomain::getOwnerId, userId))
                .stream()
                .map(BlockedDomain::getDomain)
                .toList();
        return DomainRuleMatcher.findMatchedRule(rules, normalizedDomain);
    }

    @Transactional
    public boolean addBlockedDomainIfAbsent(Long userId, String domain) {
        String normalizedDomain = normalizeDomain(domain);
        BlockedDomain existing = blockedDomainMapper.selectOne(new LambdaQueryWrapper<BlockedDomain>()
                .eq(BlockedDomain::getOwnerId, userId)
                .eq(BlockedDomain::getDomain, normalizedDomain));
        if (existing != null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        BlockedDomain blockedDomain = new BlockedDomain();
        blockedDomain.setOwnerId(userId);
        blockedDomain.setDomain(normalizedDomain);
        blockedDomain.setCreatedAt(now);
        blockedDomain.setUpdatedAt(now);
        blockedDomain.setDeleted(0);
        blockedDomainMapper.insert(blockedDomain);
        return true;
    }

    @Transactional
    public boolean removeBlockedDomainIfPresent(Long userId, String domain) {
        String normalizedDomain = normalizeDomain(domain);
        BlockedDomain existing = blockedDomainMapper.selectOne(new LambdaQueryWrapper<BlockedDomain>()
                .eq(BlockedDomain::getOwnerId, userId)
                .eq(BlockedDomain::getDomain, normalizedDomain));
        if (existing == null) {
            return false;
        }
        blockedDomainMapper.deleteById(existing.getId());
        return true;
    }

    private BlockedDomain upsertBlockedDomain(Long userId, String domain) {
        String normalizedDomain = normalizeDomain(domain);
        BlockedDomain existing = blockedDomainMapper.selectOne(new LambdaQueryWrapper<BlockedDomain>()
                .eq(BlockedDomain::getOwnerId, userId)
                .eq(BlockedDomain::getDomain, normalizedDomain));
        if (existing != null) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        BlockedDomain blockedDomain = new BlockedDomain();
        blockedDomain.setOwnerId(userId);
        blockedDomain.setDomain(normalizedDomain);
        blockedDomain.setCreatedAt(now);
        blockedDomain.setUpdatedAt(now);
        blockedDomain.setDeleted(0);
        blockedDomainMapper.insert(blockedDomain);
        return blockedDomain;
    }

    private String normalizeDomain(String domain) {
        return DomainRuleMatcher.normalizeRulePattern(domain);
    }

    private BlockedDomainVo toVo(BlockedDomain blockedDomain) {
        return new BlockedDomainVo(
                String.valueOf(blockedDomain.getId()),
                blockedDomain.getDomain(),
                blockedDomain.getCreatedAt()
        );
    }
}
