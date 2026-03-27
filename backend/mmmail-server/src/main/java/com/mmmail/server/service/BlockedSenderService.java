package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.BlockedSenderMapper;
import com.mmmail.server.model.entity.BlockedSender;
import com.mmmail.server.model.vo.BlockedSenderVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class BlockedSenderService {

    private static final Pattern SIMPLE_EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final BlockedSenderMapper blockedSenderMapper;
    private final AuditService auditService;

    public BlockedSenderService(BlockedSenderMapper blockedSenderMapper, AuditService auditService) {
        this.blockedSenderMapper = blockedSenderMapper;
        this.auditService = auditService;
    }

    public List<BlockedSenderVo> listBlockedSenders(Long userId) {
        return blockedSenderMapper.selectList(new LambdaQueryWrapper<BlockedSender>()
                        .eq(BlockedSender::getOwnerId, userId)
                        .orderByDesc(BlockedSender::getCreatedAt))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public BlockedSenderVo addBlockedSender(Long userId, String email, String ipAddress) {
        BlockedSender blockedSender = upsertBlockedSender(userId, email);
        auditService.record(userId, "BLOCKED_SENDER_ADD", "email=" + blockedSender.getEmail(), ipAddress);
        return toVo(blockedSender);
    }

    @Transactional
    public void removeBlockedSender(Long userId, Long blockedSenderId, String ipAddress) {
        BlockedSender blockedSender = blockedSenderMapper.selectOne(new LambdaQueryWrapper<BlockedSender>()
                .eq(BlockedSender::getId, blockedSenderId)
                .eq(BlockedSender::getOwnerId, userId));
        if (blockedSender == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Blocked sender not found");
        }
        blockedSenderMapper.deleteById(blockedSenderId);
        auditService.record(userId, "BLOCKED_SENDER_REMOVE", "email=" + blockedSender.getEmail(), ipAddress);
    }

    public boolean isBlockedSender(Long userId, String email) {
        String normalizedEmail = normalizeEmail(email);
        return blockedSenderMapper.selectCount(new LambdaQueryWrapper<BlockedSender>()
                .eq(BlockedSender::getOwnerId, userId)
                .eq(BlockedSender::getEmail, normalizedEmail)) > 0;
    }

    @Transactional
    public boolean addBlockedSenderIfAbsent(Long userId, String email) {
        String normalizedEmail = normalizeEmail(email);
        BlockedSender existing = blockedSenderMapper.selectOne(new LambdaQueryWrapper<BlockedSender>()
                .eq(BlockedSender::getOwnerId, userId)
                .eq(BlockedSender::getEmail, normalizedEmail));
        if (existing != null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        BlockedSender blockedSender = new BlockedSender();
        blockedSender.setOwnerId(userId);
        blockedSender.setEmail(normalizedEmail);
        blockedSender.setCreatedAt(now);
        blockedSender.setUpdatedAt(now);
        blockedSender.setDeleted(0);
        blockedSenderMapper.insert(blockedSender);
        return true;
    }

    @Transactional
    public boolean removeBlockedSenderIfPresent(Long userId, String email) {
        String normalizedEmail = normalizeEmail(email);
        BlockedSender existing = blockedSenderMapper.selectOne(new LambdaQueryWrapper<BlockedSender>()
                .eq(BlockedSender::getOwnerId, userId)
                .eq(BlockedSender::getEmail, normalizedEmail));
        if (existing == null) {
            return false;
        }
        blockedSenderMapper.deleteById(existing.getId());
        return true;
    }

    private BlockedSender upsertBlockedSender(Long userId, String email) {
        String normalizedEmail = normalizeEmail(email);
        BlockedSender existing = blockedSenderMapper.selectOne(new LambdaQueryWrapper<BlockedSender>()
                .eq(BlockedSender::getOwnerId, userId)
                .eq(BlockedSender::getEmail, normalizedEmail));
        if (existing != null) {
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        BlockedSender blockedSender = new BlockedSender();
        blockedSender.setOwnerId(userId);
        blockedSender.setEmail(normalizedEmail);
        blockedSender.setCreatedAt(now);
        blockedSender.setUpdatedAt(now);
        blockedSender.setDeleted(0);
        blockedSenderMapper.insert(blockedSender);
        return blockedSender;
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

    private BlockedSenderVo toVo(BlockedSender blockedSender) {
        return new BlockedSenderVo(
                String.valueOf(blockedSender.getId()),
                blockedSender.getEmail(),
                blockedSender.getCreatedAt()
        );
    }
}
