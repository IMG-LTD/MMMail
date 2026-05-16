package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.SecurityEventMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.mapper.UserSessionMapper;
import com.mmmail.server.model.entity.SecurityEvent;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.entity.UserSession;
import com.mmmail.server.model.vo.SecurityEventVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class SecurityEventService {

    private static final int PAGE_SIZE = 20;

    private final SecurityEventMapper securityEventMapper;
    private final UserSessionMapper userSessionMapper;
    private final UserAccountMapper userAccountMapper;

    public SecurityEventService(
            SecurityEventMapper securityEventMapper,
            UserSessionMapper userSessionMapper,
            UserAccountMapper userAccountMapper
    ) {
        this.securityEventMapper = securityEventMapper;
        this.userSessionMapper = userSessionMapper;
        this.userAccountMapper = userAccountMapper;
    }

    public List<SecurityEventVo> listUserEvents(Long userId, String type, Integer page) {
        LambdaQueryWrapper<SecurityEvent> query = new LambdaQueryWrapper<SecurityEvent>()
                .eq(SecurityEvent::getUserId, userId);
        applyTypeFilter(query, type);
        query.orderByDesc(SecurityEvent::getCreatedAt)
                .last("limit " + PAGE_SIZE + " offset " + offset(page));
        return securityEventMapper.selectList(query).stream().map(this::toVo).toList();
    }

    @Transactional
    public SecurityEventVo acknowledge(Long userId, Long eventId) {
        SecurityEvent event = findUserEvent(userId, eventId);
        LocalDateTime now = LocalDateTime.now();
        securityEventMapper.update(null, new LambdaUpdateWrapper<SecurityEvent>()
                .eq(SecurityEvent::getId, event.getId())
                .set(SecurityEvent::getAcknowledgedAt, now)
                .set(SecurityEvent::getUpdatedAt, now));
        event.setAcknowledgedAt(now);
        return toVo(event);
    }

    public List<SecurityEventVo> listAdminAnomalies() {
        return securityEventMapper.selectList(new LambdaQueryWrapper<SecurityEvent>()
                        .in(SecurityEvent::getSeverity, List.of("MEDIUM", "HIGH"))
                        .orderByDesc(SecurityEvent::getLockedUntil)
                        .orderByDesc(SecurityEvent::getCreatedAt)
                        .last("limit 100"))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public SecurityEventVo applyAdminAction(Long adminUserId, Long eventId, String action) {
        SecurityEvent event = findEvent(eventId);
        String status = normalizeAction(action);
        if ("FORCE_LOGOUT".equals(status)) {
            revokeUserSessions(event);
        }
        if ("BLOCK".equals(status)) {
            blockUser(event);
        }
        LocalDateTime now = LocalDateTime.now();
        updateAction(event, adminUserId, action, status, now);
        return toVo(event);
    }

    private void applyTypeFilter(LambdaQueryWrapper<SecurityEvent> query, String type) {
        if (StringUtils.hasText(type)) {
            query.eq(SecurityEvent::getType, type.trim().toUpperCase());
        }
    }

    private int offset(Integer page) {
        int safePage = page == null ? 0 : Math.max(0, page);
        return safePage * PAGE_SIZE;
    }

    private SecurityEvent findUserEvent(Long userId, Long eventId) {
        SecurityEvent event = securityEventMapper.selectOne(new LambdaQueryWrapper<SecurityEvent>()
                .eq(SecurityEvent::getId, eventId)
                .eq(SecurityEvent::getUserId, userId)
                .last("limit 1"));
        if (event == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Security event is not found");
        }
        return event;
    }

    private SecurityEvent findEvent(Long eventId) {
        SecurityEvent event = securityEventMapper.selectById(eventId);
        if (event == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Security event is not found");
        }
        return event;
    }

    private String normalizeAction(String action) {
        String normalized = StringUtils.hasText(action)
                ? action.trim().replace('-', '_').toUpperCase()
                : "";
        if (List.of("BLOCK", "FORCE_LOGOUT", "MARK_SAFE").contains(normalized)) {
            return normalized;
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported security action");
    }

    private void revokeUserSessions(SecurityEvent event) {
        if (event.getUserId() == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Security event has no user");
        }
        userSessionMapper.update(null, new LambdaUpdateWrapper<UserSession>()
                .eq(UserSession::getOwnerId, event.getUserId())
                .eq(UserSession::getRevoked, 0)
                .set(UserSession::getRevoked, 1)
                .set(UserSession::getUpdatedAt, LocalDateTime.now()));
    }

    private void blockUser(SecurityEvent event) {
        if (event.getUserId() == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Security event has no user");
        }
        userAccountMapper.update(null, new LambdaUpdateWrapper<UserAccount>()
                .eq(UserAccount::getId, event.getUserId())
                .set(UserAccount::getStatus, 0)
                .set(UserAccount::getUpdatedAt, LocalDateTime.now()));
    }

    private void updateAction(
            SecurityEvent event,
            Long adminUserId,
            String action,
            String status,
            LocalDateTime now
    ) {
        securityEventMapper.update(null, new LambdaUpdateWrapper<SecurityEvent>()
                .eq(SecurityEvent::getId, event.getId())
                .set(SecurityEvent::getActionTaken, action)
                .set(SecurityEvent::getActionStatus, status)
                .set(SecurityEvent::getActionBy, adminUserId)
                .set(SecurityEvent::getActionAt, now)
                .set(SecurityEvent::getUpdatedAt, now));
        event.setActionTaken(action);
        event.setActionStatus(status);
        event.setActionBy(adminUserId);
        event.setActionAt(now);
    }

    private SecurityEventVo toVo(SecurityEvent event) {
        return new SecurityEventVo(
                String.valueOf(event.getId()),
                event.getType(),
                event.getSeverity(),
                event.getRisk(),
                splitReasons(event.getReasons()),
                event.getEmail(),
                event.getIpAddress(),
                event.getCity(),
                event.getCountry(),
                event.getSource(),
                event.getDetail(),
                event.getLockedUntil(),
                event.getAcknowledgedAt(),
                event.getActionStatus(),
                event.getActionTaken(),
                event.getActionAt(),
                event.getCreatedAt()
        );
    }

    private List<String> splitReasons(String reasons) {
        if (!StringUtils.hasText(reasons)) {
            return List.of();
        }
        return Arrays.stream(reasons.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
