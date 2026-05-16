package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.CalendarEventMapper;
import com.mmmail.server.mapper.CalendarSubscriptionEventMapper;
import com.mmmail.server.mapper.CalendarSubscriptionMapper;
import com.mmmail.server.model.dto.CreateCalendarSubscriptionRequest;
import com.mmmail.server.model.entity.CalendarSubscription;
import com.mmmail.server.model.entity.CalendarSubscriptionEvent;
import com.mmmail.server.model.vo.CalendarImportResultVo;
import com.mmmail.server.model.vo.CalendarSubscriptionSyncVo;
import com.mmmail.server.model.vo.CalendarSubscriptionVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CalendarSubscriptionService {

    private static final int DEFAULT_REMINDER_MINUTES = 15;
    private static final int NEXT_SYNC_HOURS = 1;
    private static final String AUTH_NONE = "none";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final CalendarSubscriptionMapper subscriptionMapper;
    private final CalendarSubscriptionEventMapper subscriptionEventMapper;
    private final CalendarEventMapper calendarEventMapper;
    private final CalendarIcsFetchService fetchService;
    private final CalendarImportService importService;
    private final AuditService auditService;

    public CalendarSubscriptionService(
            CalendarSubscriptionMapper subscriptionMapper,
            CalendarSubscriptionEventMapper subscriptionEventMapper,
            CalendarEventMapper calendarEventMapper,
            CalendarIcsFetchService fetchService,
            CalendarImportService importService,
            AuditService auditService
    ) {
        this.subscriptionMapper = subscriptionMapper;
        this.subscriptionEventMapper = subscriptionEventMapper;
        this.calendarEventMapper = calendarEventMapper;
        this.fetchService = fetchService;
        this.importService = importService;
        this.auditService = auditService;
    }

    @Transactional
    public CalendarSubscriptionVo create(Long userId, CreateCalendarSubscriptionRequest request, String ipAddress) {
        String authMode = normalizeAuthMode(request.authMode());
        fetchService.validateHttpUrl(request.url());
        CalendarSubscription subscription = buildSubscription(userId, request, authMode);
        subscriptionMapper.insert(subscription);
        auditService.record(userId, "CAL_SUBSCRIPTION_CREATE", "id=" + subscription.getId(), ipAddress);
        return toVo(subscription);
    }

    public List<CalendarSubscriptionVo> list(Long userId) {
        return subscriptionMapper.selectList(new LambdaQueryWrapper<CalendarSubscription>()
                        .eq(CalendarSubscription::getOwnerId, userId)
                        .orderByDesc(CalendarSubscription::getUpdatedAt)
                        .orderByDesc(CalendarSubscription::getId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public CalendarSubscriptionSyncVo sync(Long userId, Long subscriptionId, String ipAddress) {
        CalendarSubscription subscription = requireSubscription(userId, subscriptionId);
        String content = fetchService.fetch(subscription.getUrl(), subscription.getAuthMode());
        replaceSubscriptionEvents(userId, subscriptionId);
        CalendarImportResultVo importResult = importService.importIcsContent(
                userId,
                content,
                null,
                DEFAULT_REMINDER_MINUTES,
                "CAL_SUBSCRIPTION_SYNC",
                ipAddress
        );
        persistSubscriptionLinks(userId, subscriptionId, importResult.eventIds());
        updateSyncSuccess(subscription);
        auditService.record(userId, "CAL_SUBSCRIPTION_SYNC", "id=" + subscriptionId, ipAddress);
        return toSyncVo(subscriptionId, importResult);
    }

    @Transactional
    public void delete(Long userId, Long subscriptionId, String ipAddress) {
        CalendarSubscription subscription = requireSubscription(userId, subscriptionId);
        replaceSubscriptionEvents(userId, subscriptionId);
        subscriptionMapper.deleteById(subscription.getId());
        auditService.record(userId, "CAL_SUBSCRIPTION_DELETE", "id=" + subscriptionId, ipAddress);
    }

    private CalendarSubscription buildSubscription(
            Long userId,
            CreateCalendarSubscriptionRequest request,
            String authMode
    ) {
        LocalDateTime now = LocalDateTime.now();
        CalendarSubscription subscription = new CalendarSubscription();
        subscription.setOwnerId(userId);
        subscription.setUrl(request.url());
        subscription.setLabel(request.label().trim());
        subscription.setAuthMode(authMode);
        subscription.setColor(request.color());
        subscription.setSyncStatus(STATUS_PENDING);
        subscription.setCreatedAt(now);
        subscription.setUpdatedAt(now);
        subscription.setDeleted(0);
        return subscription;
    }

    private String normalizeAuthMode(String authMode) {
        if (!StringUtils.hasText(authMode)) {
            return AUTH_NONE;
        }
        return authMode.trim().toLowerCase(Locale.ROOT);
    }

    private CalendarSubscription requireSubscription(Long userId, Long subscriptionId) {
        CalendarSubscription subscription = subscriptionMapper.selectOne(new LambdaQueryWrapper<CalendarSubscription>()
                .eq(CalendarSubscription::getOwnerId, userId)
                .eq(CalendarSubscription::getId, subscriptionId));
        if (subscription == null) {
            throw new BizException(ErrorCode.CALENDAR_SUBSCRIPTION_NOT_FOUND);
        }
        return subscription;
    }

    private void replaceSubscriptionEvents(Long userId, Long subscriptionId) {
        List<Long> eventIds = subscriptionEventMapper.selectEventIds(userId, subscriptionId);
        for (Long eventId : eventIds) {
            calendarEventMapper.deleteById(eventId);
        }
        subscriptionEventMapper.deleteBySubscription(userId, subscriptionId);
    }

    private void persistSubscriptionLinks(Long userId, Long subscriptionId, List<String> eventIds) {
        LocalDateTime now = LocalDateTime.now();
        for (String eventId : eventIds) {
            CalendarSubscriptionEvent link = new CalendarSubscriptionEvent();
            link.setOwnerId(userId);
            link.setSubscriptionId(subscriptionId);
            link.setEventId(Long.valueOf(eventId));
            link.setCreatedAt(now);
            subscriptionEventMapper.insert(link);
        }
    }

    private void updateSyncSuccess(CalendarSubscription subscription) {
        LocalDateTime now = LocalDateTime.now();
        subscription.setSyncStatus(STATUS_SUCCESS);
        subscription.setLastSyncAt(now);
        subscription.setLastError(null);
        subscription.setNextSyncAt(now.plusHours(NEXT_SYNC_HOURS));
        subscription.setUpdatedAt(now);
        subscriptionMapper.updateById(subscription);
    }

    private CalendarSubscriptionSyncVo toSyncVo(Long subscriptionId, CalendarImportResultVo result) {
        return new CalendarSubscriptionSyncVo(
                "cal_sync_" + IdWorker.getIdStr(),
                String.valueOf(subscriptionId),
                STATUS_SUCCESS,
                result.totalCount(),
                result.importedCount(),
                result.eventIds(),
                LocalDateTime.now()
        );
    }

    private CalendarSubscriptionVo toVo(CalendarSubscription subscription) {
        return new CalendarSubscriptionVo(
                String.valueOf(subscription.getId()),
                subscription.getUrl(),
                subscription.getLabel(),
                subscription.getAuthMode(),
                subscription.getColor(),
                subscription.getSyncStatus(),
                subscription.getLastSyncAt(),
                subscription.getLastError(),
                subscription.getNextSyncAt(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }
}
