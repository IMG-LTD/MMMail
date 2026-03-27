package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.AuthenticatorEntryMapper;
import com.mmmail.server.model.entity.AuthenticatorEntry;
import com.mmmail.server.model.vo.AuthenticatorCodeVo;
import com.mmmail.server.model.vo.AuthenticatorEntryDetailVo;
import com.mmmail.server.model.vo.AuthenticatorEntrySummaryVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthenticatorService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final AuthenticatorEntryMapper authenticatorEntryMapper;
    private final AuditService auditService;
    private final AuthenticatorEntrySupport entrySupport;
    private final TotpCodeService totpCodeService;
    private final AuthenticatorSecurityPreferenceService securityPreferenceService;

    public AuthenticatorService(
            AuthenticatorEntryMapper authenticatorEntryMapper,
            AuditService auditService,
            AuthenticatorEntrySupport entrySupport,
            TotpCodeService totpCodeService,
            AuthenticatorSecurityPreferenceService securityPreferenceService
    ) {
        this.authenticatorEntryMapper = authenticatorEntryMapper;
        this.auditService = auditService;
        this.entrySupport = entrySupport;
        this.totpCodeService = totpCodeService;
        this.securityPreferenceService = securityPreferenceService;
    }

    public List<AuthenticatorEntrySummaryVo> list(Long userId, String keyword, Integer limit) {
        int safeLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        String normalizedKeyword = normalizeKeyword(keyword);
        LambdaQueryWrapper<AuthenticatorEntry> query = new LambdaQueryWrapper<AuthenticatorEntry>()
                .eq(AuthenticatorEntry::getOwnerId, userId)
                .orderByDesc(AuthenticatorEntry::getUpdatedAt)
                .last("limit " + safeLimit);
        if (StringUtils.hasText(normalizedKeyword)) {
            query.and(wrapper -> wrapper
                    .like(AuthenticatorEntry::getIssuer, normalizedKeyword)
                    .or()
                    .like(AuthenticatorEntry::getAccountName, normalizedKeyword));
        }
        return authenticatorEntryMapper.selectList(query).stream()
                .map(entrySupport::toSummaryVo)
                .toList();
    }

    @Transactional
    public AuthenticatorEntryDetailVo create(
            Long userId,
            String issuer,
            String accountName,
            String secretCiphertext,
            String algorithm,
            Integer digits,
            Integer periodSeconds,
            String ipAddress
    ) {
        LocalDateTime now = LocalDateTime.now();
        AuthenticatorEntrySupport.NormalizedAuthenticatorEntry normalized = entrySupport.normalize(
                issuer,
                accountName,
                secretCiphertext,
                algorithm,
                digits,
                periodSeconds
        );
        AuthenticatorEntry entry = entrySupport.create(userId, normalized, now);
        authenticatorEntryMapper.insert(entry);
        auditService.record(userId, "AUTH_ENTRY_CREATE", "entryId=" + entry.getId(), ipAddress);
        securityPreferenceService.markSynced(userId, now);
        return entrySupport.toDetailVo(entry);
    }

    public AuthenticatorEntryDetailVo get(Long userId, Long entryId) {
        return entrySupport.toDetailVo(loadEntry(userId, entryId));
    }

    @Transactional
    public AuthenticatorEntryDetailVo update(
            Long userId,
            Long entryId,
            String issuer,
            String accountName,
            String secretCiphertext,
            String algorithm,
            Integer digits,
            Integer periodSeconds,
            String ipAddress
    ) {
        AuthenticatorEntry entry = loadEntry(userId, entryId);
        AuthenticatorEntrySupport.NormalizedAuthenticatorEntry normalized = entrySupport.normalize(
                issuer,
                accountName,
                secretCiphertext,
                algorithm,
                digits,
                periodSeconds
        );
        entrySupport.apply(entry, normalized, LocalDateTime.now());
        authenticatorEntryMapper.updateById(entry);
        auditService.record(userId, "AUTH_ENTRY_UPDATE", "entryId=" + entryId, ipAddress);
        securityPreferenceService.markSynced(userId, LocalDateTime.now());
        return entrySupport.toDetailVo(entry);
    }

    @Transactional
    public void delete(Long userId, Long entryId, String ipAddress) {
        AuthenticatorEntry entry = loadEntry(userId, entryId);
        authenticatorEntryMapper.deleteById(entry.getId());
        auditService.record(userId, "AUTH_ENTRY_DELETE", "entryId=" + entryId, ipAddress);
        securityPreferenceService.markSynced(userId, LocalDateTime.now());
    }

    public AuthenticatorCodeVo generateCode(Long userId, Long entryId, String ipAddress) {
        AuthenticatorEntry entry = loadEntry(userId, entryId);
        int periodSeconds = entrySupport.normalizePeriodSeconds(entry.getPeriodSeconds(), null);
        int digits = entrySupport.normalizeDigits(entry.getDigits(), null);
        auditService.record(
                userId,
                "AUTH_CODE_GENERATE",
                "entryId=" + entryId + ",digits=" + digits + ",periodSeconds=" + periodSeconds,
                ipAddress
        );
        return totpCodeService.generateCode(
                entry.getSecretCiphertext(),
                entrySupport.normalizeAlgorithm(entry.getAlgorithm(), null),
                digits,
                periodSeconds
        );
    }

    private AuthenticatorEntry loadEntry(Long userId, Long entryId) {
        AuthenticatorEntry entry = authenticatorEntryMapper.selectOne(new LambdaQueryWrapper<AuthenticatorEntry>()
                .eq(AuthenticatorEntry::getId, entryId)
                .eq(AuthenticatorEntry::getOwnerId, userId));
        if (entry == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Authenticator entry is not found");
        }
        return entry;
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

}
