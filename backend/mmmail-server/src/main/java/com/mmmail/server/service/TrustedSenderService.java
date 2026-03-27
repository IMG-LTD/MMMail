package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.TrustedSenderMapper;
import com.mmmail.server.model.entity.TrustedSender;
import com.mmmail.server.model.vo.TrustedSenderVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TrustedSenderService {

    private static final Pattern SIMPLE_EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final TrustedSenderMapper trustedSenderMapper;
    private final AuditService auditService;

    public TrustedSenderService(TrustedSenderMapper trustedSenderMapper, AuditService auditService) {
        this.trustedSenderMapper = trustedSenderMapper;
        this.auditService = auditService;
    }

    public List<TrustedSenderVo> listTrustedSenders(Long userId) {
        return trustedSenderMapper.selectList(new LambdaQueryWrapper<TrustedSender>()
                        .eq(TrustedSender::getOwnerId, userId)
                        .orderByDesc(TrustedSender::getCreatedAt))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public TrustedSenderVo addTrustedSender(Long userId, String email, String ipAddress) {
        TrustedSender trustedSender = upsertTrustedSender(userId, email);
        auditService.record(userId, "TRUSTED_SENDER_ADD", "email=" + trustedSender.getEmail(), ipAddress);
        return toVo(trustedSender);
    }

    @Transactional
    public void removeTrustedSender(Long userId, Long trustedSenderId, String ipAddress) {
        TrustedSender trustedSender = trustedSenderMapper.selectOne(new LambdaQueryWrapper<TrustedSender>()
                .eq(TrustedSender::getId, trustedSenderId)
                .eq(TrustedSender::getOwnerId, userId));
        if (trustedSender == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Trusted sender not found");
        }
        trustedSenderMapper.deleteById(trustedSenderId);
        auditService.record(userId, "TRUSTED_SENDER_REMOVE", "email=" + trustedSender.getEmail(), ipAddress);
    }

    public boolean isTrustedSender(Long userId, String email) {
        String normalizedEmail = normalizeEmail(email);
        return trustedSenderMapper.selectCount(new LambdaQueryWrapper<TrustedSender>()
                .eq(TrustedSender::getOwnerId, userId)
                .eq(TrustedSender::getEmail, normalizedEmail)) > 0;
    }

    @Transactional
    public boolean addTrustedSenderIfAbsent(Long userId, String email) {
        String normalizedEmail = normalizeEmail(email);
        TrustedSender existing = trustedSenderMapper.selectOne(new LambdaQueryWrapper<TrustedSender>()
                .eq(TrustedSender::getOwnerId, userId)
                .eq(TrustedSender::getEmail, normalizedEmail));
        if (existing != null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        TrustedSender trustedSender = new TrustedSender();
        trustedSender.setOwnerId(userId);
        trustedSender.setEmail(normalizedEmail);
        trustedSender.setCreatedAt(now);
        trustedSender.setUpdatedAt(now);
        trustedSender.setDeleted(0);
        trustedSenderMapper.insert(trustedSender);
        return true;
    }

    @Transactional
    public boolean removeTrustedSenderIfPresent(Long userId, String email) {
        String normalizedEmail = normalizeEmail(email);
        TrustedSender existing = trustedSenderMapper.selectOne(new LambdaQueryWrapper<TrustedSender>()
                .eq(TrustedSender::getOwnerId, userId)
                .eq(TrustedSender::getEmail, normalizedEmail));
        if (existing == null) {
            return false;
        }
        trustedSenderMapper.deleteById(existing.getId());
        return true;
    }

    private TrustedSender upsertTrustedSender(Long userId, String email) {
        String normalizedEmail = normalizeEmail(email);
        TrustedSender existing = trustedSenderMapper.selectOne(new LambdaQueryWrapper<TrustedSender>()
                .eq(TrustedSender::getOwnerId, userId)
                .eq(TrustedSender::getEmail, normalizedEmail));
        if (existing != null) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        TrustedSender trustedSender = new TrustedSender();
        trustedSender.setOwnerId(userId);
        trustedSender.setEmail(normalizedEmail);
        trustedSender.setCreatedAt(now);
        trustedSender.setUpdatedAt(now);
        trustedSender.setDeleted(0);
        trustedSenderMapper.insert(trustedSender);
        return trustedSender;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Email is required");
        }
        String normalized = email.trim().toLowerCase();
        if (normalized.length() > 254 || !SIMPLE_EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid email");
        }
        return normalized;
    }

    private TrustedSenderVo toVo(TrustedSender trustedSender) {
        return new TrustedSenderVo(
                String.valueOf(trustedSender.getId()),
                trustedSender.getEmail(),
                trustedSender.getCreatedAt()
        );
    }
}
