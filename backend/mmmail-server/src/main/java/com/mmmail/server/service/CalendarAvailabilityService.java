package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.CalendarEventMapper;
import com.mmmail.server.mapper.CalendarEventShareMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.dto.QueryCalendarAvailabilityRequest;
import com.mmmail.server.model.entity.CalendarEvent;
import com.mmmail.server.model.entity.CalendarEventShare;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.CalendarAvailabilitySlotVo;
import com.mmmail.server.model.vo.CalendarAvailabilitySummaryVo;
import com.mmmail.server.model.vo.CalendarAvailabilityVo;
import com.mmmail.server.model.vo.CalendarParticipantAvailabilityVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CalendarAvailabilityService {

    private static final String STATUS_BUSY = "BUSY";
    private static final String STATUS_FREE = "FREE";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String SHARE_STATUS_ACCEPTED = "ACCEPTED";

    private final CalendarEventMapper calendarEventMapper;
    private final CalendarEventShareMapper calendarEventShareMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditService auditService;

    public CalendarAvailabilityService(
            CalendarEventMapper calendarEventMapper,
            CalendarEventShareMapper calendarEventShareMapper,
            UserAccountMapper userAccountMapper,
            AuditService auditService
    ) {
        this.calendarEventMapper = calendarEventMapper;
        this.calendarEventShareMapper = calendarEventShareMapper;
        this.userAccountMapper = userAccountMapper;
        this.auditService = auditService;
    }

    public CalendarAvailabilityVo queryAvailability(Long userId, QueryCalendarAvailabilityRequest request, String ipAddress) {
        validateTimeWindow(request.startAt(), request.endAt());
        List<String> attendeeEmails = normalizeEmails(request.attendeeEmails());
        if (attendeeEmails.isEmpty()) {
            CalendarAvailabilityVo empty = new CalendarAvailabilityVo(
                    request.startAt(),
                    request.endAt(),
                    new CalendarAvailabilitySummaryVo(0, 0, 0, 0, false),
                    List.of()
            );
            auditService.record(userId, "CAL_AVAILABILITY_QUERY", "attendeeCount=0,busyCount=0", ipAddress);
            return empty;
        }

        List<CalendarParticipantAvailabilityVo> attendees = new ArrayList<>();
        int busyCount = 0;
        int freeCount = 0;
        int unknownCount = 0;
        for (String email : attendeeEmails) {
            CalendarParticipantAvailabilityVo item = buildAvailabilityItem(email, request.startAt(), request.endAt(), request.excludeEventId());
            attendees.add(item);
            switch (item.availability()) {
                case STATUS_BUSY -> busyCount++;
                case STATUS_FREE -> freeCount++;
                default -> unknownCount++;
            }
        }

        CalendarAvailabilityVo response = new CalendarAvailabilityVo(
                request.startAt(),
                request.endAt(),
                new CalendarAvailabilitySummaryVo(attendees.size(), busyCount, freeCount, unknownCount, busyCount > 0),
                attendees
        );
        auditService.record(userId, "CAL_AVAILABILITY_QUERY", "attendeeCount=%d,busyCount=%d,unknownCount=%d".formatted(attendees.size(), busyCount, unknownCount), ipAddress);
        return response;
    }

    private CalendarParticipantAvailabilityVo buildAvailabilityItem(
            String email,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Long excludeEventId
    ) {
        UserAccount user = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmail, email));
        if (user == null) {
            return new CalendarParticipantAvailabilityVo(email, STATUS_UNKNOWN, 0, List.of());
        }

        List<CalendarEvent> busyEvents = listVisibleEventsByOverlap(user.getId(), startAt, endAt, excludeEventId);
        if (busyEvents.isEmpty()) {
            return new CalendarParticipantAvailabilityVo(email, STATUS_FREE, 0, List.of());
        }

        List<CalendarAvailabilitySlotVo> busySlots = busyEvents.stream()
                .map(event -> buildOverlapSlot(event, startAt, endAt))
                .toList();
        return new CalendarParticipantAvailabilityVo(email, STATUS_BUSY, busySlots.size(), busySlots);
    }

    private List<CalendarEvent> listVisibleEventsByOverlap(Long userId, LocalDateTime startAt, LocalDateTime endAt, Long excludeEventId) {
        Map<Long, CalendarEvent> merged = new LinkedHashMap<>();
        for (CalendarEvent event : queryOwnedOverlaps(userId, startAt, endAt)) {
            if (excludeEventId != null && excludeEventId.equals(event.getId())) {
                continue;
            }
            merged.put(event.getId(), event);
        }

        List<CalendarEventShare> acceptedShares = calendarEventShareMapper.selectList(new LambdaQueryWrapper<CalendarEventShare>()
                .eq(CalendarEventShare::getTargetUserId, userId)
                .eq(CalendarEventShare::getResponseStatus, SHARE_STATUS_ACCEPTED));
        List<Long> sharedEventIds = acceptedShares.stream()
                .map(CalendarEventShare::getEventId)
                .filter(id -> excludeEventId == null || !excludeEventId.equals(id))
                .toList();
        for (CalendarEvent event : querySharedOverlaps(sharedEventIds, startAt, endAt)) {
            merged.putIfAbsent(event.getId(), event);
        }
        return new ArrayList<>(merged.values());
    }

    private List<CalendarEvent> queryOwnedOverlaps(Long userId, LocalDateTime startAt, LocalDateTime endAt) {
        return calendarEventMapper.selectList(new LambdaQueryWrapper<CalendarEvent>()
                .eq(CalendarEvent::getOwnerId, userId)
                .lt(CalendarEvent::getStartAt, endAt)
                .gt(CalendarEvent::getEndAt, startAt)
                .orderByAsc(CalendarEvent::getStartAt)
                .orderByAsc(CalendarEvent::getId));
    }

    private List<CalendarEvent> querySharedOverlaps(List<Long> eventIds, LocalDateTime startAt, LocalDateTime endAt) {
        if (eventIds.isEmpty()) {
            return List.of();
        }
        return calendarEventMapper.selectList(new LambdaQueryWrapper<CalendarEvent>()
                .in(CalendarEvent::getId, new LinkedHashSet<>(eventIds))
                .lt(CalendarEvent::getStartAt, endAt)
                .gt(CalendarEvent::getEndAt, startAt)
                .orderByAsc(CalendarEvent::getStartAt)
                .orderByAsc(CalendarEvent::getId));
    }

    private CalendarAvailabilitySlotVo buildOverlapSlot(CalendarEvent event, LocalDateTime queryStart, LocalDateTime queryEnd) {
        LocalDateTime startAt = event.getStartAt().isAfter(queryStart) ? event.getStartAt() : queryStart;
        LocalDateTime endAt = event.getEndAt().isBefore(queryEnd) ? event.getEndAt() : queryEnd;
        return new CalendarAvailabilitySlotVo(startAt, endAt, event.getAllDay() != null && event.getAllDay() == 1);
    }

    private void validateTimeWindow(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "`startAt` and `endAt` are required");
        }
        if (!startAt.isBefore(endAt)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "`startAt` must be earlier than `endAt`");
        }
    }

    private List<String> normalizeEmails(List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return List.of();
        }
        Set<String> deduplicated = new LinkedHashSet<>();
        for (String email : emails) {
            if (!StringUtils.hasText(email)) {
                continue;
            }
            deduplicated.add(email.trim().toLowerCase());
        }
        return new ArrayList<>(deduplicated);
    }
}
