package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.CalendarEventAttendeeMapper;
import com.mmmail.server.mapper.CalendarEventMapper;
import com.mmmail.server.mapper.CalendarEventShareMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.dto.CalendarAttendeeInput;
import com.mmmail.server.model.dto.CreateCalendarEventRequest;
import com.mmmail.server.model.dto.CreateCalendarShareRequest;
import com.mmmail.server.model.dto.RespondCalendarShareRequest;
import com.mmmail.server.model.dto.UpdateCalendarEventRequest;
import com.mmmail.server.model.dto.UpdateCalendarShareRequest;
import com.mmmail.server.model.entity.CalendarEvent;
import com.mmmail.server.model.entity.CalendarEventAttendee;
import com.mmmail.server.model.entity.CalendarEventShare;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.CalendarAgendaItemVo;
import com.mmmail.server.model.vo.CalendarAttendeeVo;
import com.mmmail.server.model.vo.CalendarEventDetailVo;
import com.mmmail.server.model.vo.CalendarEventItemVo;
import com.mmmail.server.model.vo.CalendarEventShareVo;
import com.mmmail.server.model.vo.CalendarIncomingShareVo;
import com.mmmail.server.util.CalendarTimezoneResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CalendarService {

    private static final int DEFAULT_QUERY_DAYS = 30;
    private static final int DEFAULT_AGENDA_DAYS = 7;
    private static final int MAX_AGENDA_DAYS = 60;
    private static final String SHARE_PERMISSION_VIEW = "VIEW";
    private static final String SHARE_PERMISSION_EDIT = "EDIT";
    private static final String SHARE_PERMISSION_OWNER = "OWNER";
    private static final String SHARE_STATUS_NEEDS_ACTION = "NEEDS_ACTION";
    private static final String SHARE_STATUS_ACCEPTED = "ACCEPTED";
    private static final String SHARE_STATUS_DECLINED = "DECLINED";
    private static final DateTimeFormatter ICS_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter ICS_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CalendarEventMapper calendarEventMapper;
    private final CalendarEventAttendeeMapper calendarEventAttendeeMapper;
    private final CalendarEventShareMapper calendarEventShareMapper;
    private final UserAccountMapper userAccountMapper;
    private final SuiteService suiteService;
    private final AuditService auditService;
    private final CalendarInvitationOrchestrationService calendarInvitationOrchestrationService;

    public CalendarService(
            CalendarEventMapper calendarEventMapper,
            CalendarEventAttendeeMapper calendarEventAttendeeMapper,
            CalendarEventShareMapper calendarEventShareMapper,
            UserAccountMapper userAccountMapper,
            SuiteService suiteService,
            AuditService auditService,
            CalendarInvitationOrchestrationService calendarInvitationOrchestrationService
    ) {
        this.calendarEventMapper = calendarEventMapper;
        this.calendarEventAttendeeMapper = calendarEventAttendeeMapper;
        this.calendarEventShareMapper = calendarEventShareMapper;
        this.userAccountMapper = userAccountMapper;
        this.suiteService = suiteService;
        this.auditService = auditService;
        this.calendarInvitationOrchestrationService = calendarInvitationOrchestrationService;
    }

    public List<CalendarEventItemVo> listEvents(Long userId, String from, String to) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fromAt = parseDateTimeOrDefault(from, now.toLocalDate().atStartOfDay());
        LocalDateTime toAt = parseDateTimeOrDefault(to, fromAt.plusDays(DEFAULT_QUERY_DAYS));
        if (!fromAt.isBefore(toAt)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "`from` must be earlier than `to`");
        }

        List<EventAccess> accesses = listAccessibleEvents(userId, fromAt, toAt);
        Map<Long, Integer> attendeeCountMap = buildAttendeeCountMap(accesses.stream().map(access -> access.event().getId()).toList());
        Map<Long, String> ownerEmailMap = buildOwnerEmailMap(accesses.stream().map(access -> access.event().getOwnerId()).toList());

        return accesses.stream()
                .map(access -> toItemVo(
                        access,
                        attendeeCountMap.getOrDefault(access.event().getId(), 0),
                        ownerEmailMap.get(access.event().getOwnerId())
                ))
                .toList();
    }

    public CalendarEventDetailVo getEvent(Long userId, Long eventId) {
        EventAccess access = resolveEventAccess(userId, eventId);
        return toDetailVo(
                access,
                listAttendees(eventId),
                resolveUserEmail(access.event().getOwnerId())
        );
    }

    @Transactional
    public CalendarEventDetailVo createEvent(Long userId, CreateCalendarEventRequest request, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        validateEventTime(request.startAt(), request.endAt());
        suiteService.assertCalendarEventQuota(userId, ipAddress);

        CalendarEvent event = new CalendarEvent();
        event.setOwnerId(userId);
        event.setTitle(request.title().trim());
        event.setDescription(normalizeText(request.description()));
        event.setLocation(normalizeText(request.location()));
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setAllDay(Boolean.TRUE.equals(request.allDay()) ? 1 : 0);
        event.setTimezone(normalizeTimezone(request.timezone()));
        event.setReminderMinutes(request.reminderMinutes());
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        event.setDeleted(0);
        calendarEventMapper.insert(event);

        replaceAttendees(userId, event.getId(), request.attendees(), now);
        calendarInvitationOrchestrationService.syncInternalInvitations(userId, event.getId(), ipAddress, now);
        auditService.record(userId, "CAL_EVENT_CREATE", "eventId=" + event.getId(), ipAddress);
        return getEvent(userId, event.getId());
    }

    @Transactional
    public CalendarEventDetailVo updateEvent(Long userId, Long eventId, UpdateCalendarEventRequest request, String ipAddress) {
        EventAccess access = resolveEventAccess(userId, eventId);
        if (!access.canEdit()) {
            throw new BizException(ErrorCode.FORBIDDEN, "No edit permission for this shared event");
        }

        LocalDateTime now = LocalDateTime.now();
        validateEventTime(request.startAt(), request.endAt());

        CalendarEvent event = access.event();
        event.setTitle(request.title().trim());
        event.setDescription(normalizeText(request.description()));
        event.setLocation(normalizeText(request.location()));
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setAllDay(Boolean.TRUE.equals(request.allDay()) ? 1 : 0);
        event.setTimezone(normalizeTimezone(request.timezone()));
        event.setReminderMinutes(request.reminderMinutes());
        event.setUpdatedAt(now);
        calendarEventMapper.updateById(event);

        replaceAttendees(event.getOwnerId(), eventId, request.attendees(), now);
        calendarInvitationOrchestrationService.syncInternalInvitations(event.getOwnerId(), eventId, ipAddress, now);
        auditService.record(
                userId,
                access.shared() ? "CAL_EVENT_UPDATE_BY_SHARE_EDITOR" : "CAL_EVENT_UPDATE",
                "eventId=" + eventId,
                ipAddress
        );
        return getEvent(userId, eventId);
    }

    @Transactional
    public void deleteEvent(Long userId, Long eventId, String ipAddress) {
        EventAccess access = resolveEventAccess(userId, eventId);
        if (!access.canDelete()) {
            throw new BizException(ErrorCode.FORBIDDEN, "Only event owner can delete this event");
        }

        calendarEventShareMapper.purgeByEventId(eventId);
        calendarEventAttendeeMapper.purgeByEventId(eventId);
        calendarEventMapper.deleteById(eventId);
        auditService.record(userId, "CAL_EVENT_DELETE", "eventId=" + eventId, ipAddress);
    }

    @Transactional
    public CalendarEventShareVo createShare(Long userId, Long eventId, CreateCalendarShareRequest request, String ipAddress) {
        loadOwnedEvent(userId, eventId);
        String targetEmail = normalizeEmail(request.targetEmail(), "Target email");
        String permission = normalizePermission(request.permission());

        UserAccount targetUser = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmail, targetEmail));
        if (targetUser == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "Target user is not found");
        }
        if (targetUser.getId().equals(userId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Cannot share event with yourself");
        }

        LocalDateTime now = LocalDateTime.now();
        CalendarEventShare existingShare = findShareByEventAndTarget(eventId, targetUser.getId());
        if (existingShare != null) {
            existingShare.setTargetEmail(targetEmail);
            existingShare.setPermission(permission);
            existingShare.setSource(CalendarInvitationOrchestrationService.SHARE_SOURCE_MANUAL);
            existingShare.setUpdatedAt(now);
            calendarEventShareMapper.updateById(existingShare);
            auditService.record(userId, "CAL_SHARE_CREATE", "eventId=" + eventId + ",target=" + targetEmail, ipAddress);
            return toShareVo(existingShare);
        }

        suiteService.assertCalendarShareQuota(userId, ipAddress);

        CalendarEventShare share = new CalendarEventShare();
        share.setOwnerId(userId);
        share.setEventId(eventId);
        share.setTargetUserId(targetUser.getId());
        share.setTargetEmail(targetEmail);
        share.setPermission(permission);
        share.setResponseStatus(SHARE_STATUS_NEEDS_ACTION);
        share.setSource(CalendarInvitationOrchestrationService.SHARE_SOURCE_MANUAL);
        share.setCreatedAt(now);
        share.setUpdatedAt(now);
        share.setDeleted(0);
        calendarEventShareMapper.insert(share);

        auditService.record(userId, "CAL_SHARE_CREATE", "eventId=" + eventId + ",target=" + targetEmail, ipAddress);
        return toShareVo(share);
    }

    public List<CalendarEventShareVo> listShares(Long userId, Long eventId) {
        loadOwnedEvent(userId, eventId);
        return calendarEventShareMapper.selectList(new LambdaQueryWrapper<CalendarEventShare>()
                        .eq(CalendarEventShare::getOwnerId, userId)
                        .eq(CalendarEventShare::getEventId, eventId)
                        .orderByDesc(CalendarEventShare::getUpdatedAt)
                        .orderByAsc(CalendarEventShare::getTargetEmail))
                .stream()
                .map(this::toShareVo)
                .toList();
    }

    @Transactional
    public CalendarEventShareVo updateSharePermission(
            Long userId,
            Long eventId,
            Long shareId,
            UpdateCalendarShareRequest request,
            String ipAddress
    ) {
        loadOwnedEvent(userId, eventId);
        CalendarEventShare share = calendarEventShareMapper.selectOne(new LambdaQueryWrapper<CalendarEventShare>()
                .eq(CalendarEventShare::getOwnerId, userId)
                .eq(CalendarEventShare::getEventId, eventId)
                .eq(CalendarEventShare::getId, shareId));
        if (share == null) {
            throw new BizException(ErrorCode.CALENDAR_SHARE_NOT_FOUND);
        }

        String permission = normalizePermission(request.permission());
        share.setPermission(permission);
        share.setSource(CalendarInvitationOrchestrationService.SHARE_SOURCE_MANUAL);
        share.setUpdatedAt(LocalDateTime.now());
        calendarEventShareMapper.updateById(share);
        auditService.record(userId, "CAL_SHARE_PERMISSION_UPDATE", "eventId=" + eventId + ",shareId=" + shareId + ",permission=" + permission, ipAddress);
        return toShareVo(share);
    }

    @Transactional
    public void removeShare(Long userId, Long eventId, Long shareId, String ipAddress) {
        loadOwnedEvent(userId, eventId);
        CalendarEventShare share = loadOwnedShare(userId, eventId, shareId);
        calendarEventShareMapper.purgeByOwnerEventAndId(userId, eventId, shareId);
        if (CalendarInvitationOrchestrationService.SHARE_SOURCE_ATTENDEE.equals(share.getSource())) {
            calendarEventAttendeeMapper.purgeByEventAndEmail(eventId, share.getTargetEmail());
        }
        auditService.record(userId, "CAL_SHARE_REMOVE", "eventId=" + eventId + ",shareId=" + shareId, ipAddress);
    }

    public List<CalendarIncomingShareVo> listIncomingShares(Long userId, String ipAddress) {
        List<CalendarEventShare> shares = calendarEventShareMapper.selectList(new LambdaQueryWrapper<CalendarEventShare>()
                .eq(CalendarEventShare::getTargetUserId, userId)
                .orderByDesc(CalendarEventShare::getUpdatedAt));

        if (shares.isEmpty()) {
            auditService.record(userId, "CAL_SHARE_INCOMING_QUERY", "count=0", ipAddress);
            return List.of();
        }

        Map<Long, CalendarEvent> eventMap = loadEventMap(shares.stream().map(CalendarEventShare::getEventId).toList());
        Map<Long, String> ownerEmailMap = buildOwnerEmailMap(shares.stream().map(CalendarEventShare::getOwnerId).toList());

        List<CalendarIncomingShareVo> result = new ArrayList<>();
        for (CalendarEventShare share : shares) {
            CalendarEvent event = eventMap.get(share.getEventId());
            if (event == null) {
                continue;
            }
            result.add(toIncomingShareVo(share, event, ownerEmailMap.get(share.getOwnerId())));
        }

        auditService.record(userId, "CAL_SHARE_INCOMING_QUERY", "count=" + result.size(), ipAddress);
        return result;
    }

    @Transactional
    public CalendarIncomingShareVo respondShare(Long userId, Long shareId, RespondCalendarShareRequest request, String ipAddress) {
        CalendarEventShare share = calendarEventShareMapper.selectOne(new LambdaQueryWrapper<CalendarEventShare>()
                .eq(CalendarEventShare::getId, shareId)
                .eq(CalendarEventShare::getTargetUserId, userId));
        if (share == null) {
            throw new BizException(ErrorCode.CALENDAR_SHARE_NOT_FOUND);
        }

        String responseStatus = normalizeResponse(request.response());
        LocalDateTime now = LocalDateTime.now();
        share.setResponseStatus(responseStatus);
        share.setUpdatedAt(now);
        calendarEventShareMapper.updateById(share);
        calendarInvitationOrchestrationService.syncAttendeeResponseStatus(
                share.getEventId(),
                share.getTargetEmail(),
                responseStatus,
                now
        );

        CalendarEvent event = calendarEventMapper.selectById(share.getEventId());
        String ownerEmail = resolveUserEmail(share.getOwnerId());
        auditService.record(userId, "CAL_INVITE_RESPONSE", "shareId=" + shareId + ",status=" + responseStatus, ipAddress);

        if (event == null) {
            return new CalendarIncomingShareVo(
                    String.valueOf(share.getId()),
                    String.valueOf(share.getEventId()),
                    "(event deleted)",
                    ownerEmail,
                    share.getPermission(),
                    share.getResponseStatus(),
                    share.getUpdatedAt()
            );
        }

        return toIncomingShareVo(share, event, ownerEmail);
    }

    public List<CalendarAgendaItemVo> listAgenda(Long userId, Integer days, String ipAddress) {
        int safeDays = days == null ? DEFAULT_AGENDA_DAYS : days;
        if (safeDays <= 0 || safeDays > MAX_AGENDA_DAYS) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "`days` must be in range 1-60");
        }

        LocalDateTime fromAt = LocalDate.now().atStartOfDay();
        LocalDateTime toAt = fromAt.plusDays(safeDays);
        List<EventAccess> accesses = listAccessibleEvents(userId, fromAt, toAt);
        Map<Long, Integer> attendeeCountMap = buildAttendeeCountMap(accesses.stream().map(access -> access.event().getId()).toList());
        Map<Long, String> ownerEmailMap = buildOwnerEmailMap(accesses.stream().map(access -> access.event().getOwnerId()).toList());

        auditService.record(userId, "CAL_AGENDA_QUERY", "days=" + safeDays + ",count=" + accesses.size(), ipAddress);
        return accesses.stream()
                .map(access -> new CalendarAgendaItemVo(
                        String.valueOf(access.event().getId()),
                        access.event().getTitle(),
                        access.event().getLocation(),
                        access.event().getStartAt(),
                        access.event().getEndAt(),
                        attendeeCountMap.getOrDefault(access.event().getId(), 0),
                        access.shared(),
                        ownerEmailMap.get(access.event().getOwnerId()),
                        access.permission()
                ))
                .toList();
    }

    public String exportCalendar(Long userId, String format, String ipAddress) {
        String normalizedFormat = StringUtils.hasText(format) ? format.trim().toLowerCase() : "ics";
        if (!"ics".equals(normalizedFormat)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported calendar export format");
        }

        List<CalendarEvent> events = calendarEventMapper.selectList(new LambdaQueryWrapper<CalendarEvent>()
                .eq(CalendarEvent::getOwnerId, userId)
                .orderByAsc(CalendarEvent::getStartAt)
                .orderByAsc(CalendarEvent::getId));

        Map<Long, List<CalendarEventAttendee>> attendeeMap = loadAttendeeMap(events.stream().map(CalendarEvent::getId).toList());
        StringBuilder builder = new StringBuilder();
        builder.append("BEGIN:VCALENDAR\r\n");
        builder.append("VERSION:2.0\r\n");
        builder.append("PRODID:-//MMMail//Calendar//EN\r\n");

        LocalDateTime now = LocalDateTime.now();
        for (CalendarEvent event : events) {
            builder.append("BEGIN:VEVENT\r\n");
            builder.append("UID:").append(event.getId()).append("@mmmail.local\r\n");
            builder.append("DTSTAMP:").append(formatDateTimeIcs(now)).append("\r\n");
            appendStartEnd(builder, event);
            builder.append("SUMMARY:").append(escapeIcsText(event.getTitle())).append("\r\n");
            if (StringUtils.hasText(event.getDescription())) {
                builder.append("DESCRIPTION:").append(escapeIcsText(event.getDescription())).append("\r\n");
            }
            if (StringUtils.hasText(event.getLocation())) {
                builder.append("LOCATION:").append(escapeIcsText(event.getLocation())).append("\r\n");
            }
            for (CalendarEventAttendee attendee : attendeeMap.getOrDefault(event.getId(), List.of())) {
                builder.append("ATTENDEE");
                if (StringUtils.hasText(attendee.getDisplayName())) {
                    builder.append(";CN=").append(escapeIcsText(attendee.getDisplayName()));
                }
                builder.append(":MAILTO:").append(attendee.getEmail()).append("\r\n");
            }
            builder.append("END:VEVENT\r\n");
        }
        builder.append("END:VCALENDAR\r\n");

        auditService.record(userId, "CAL_EXPORT", "format=ics,count=" + events.size(), ipAddress);
        return builder.toString();
    }

    private List<EventAccess> listAccessibleEvents(Long userId, LocalDateTime fromAt, LocalDateTime toAt) {
        List<CalendarEvent> ownEvents = queryEventsByOwnerAndRange(userId, fromAt, toAt);

        List<CalendarEventShare> acceptedShares = calendarEventShareMapper.selectList(new LambdaQueryWrapper<CalendarEventShare>()
                .eq(CalendarEventShare::getTargetUserId, userId)
                .eq(CalendarEventShare::getResponseStatus, SHARE_STATUS_ACCEPTED));

        Map<Long, CalendarEventShare> acceptedShareByEventId = new LinkedHashMap<>();
        for (CalendarEventShare share : acceptedShares) {
            acceptedShareByEventId.putIfAbsent(share.getEventId(), share);
        }

        List<CalendarEvent> sharedEvents = queryEventsByIdsAndRange(
                new ArrayList<>(acceptedShareByEventId.keySet()),
                fromAt,
                toAt
        );

        Map<Long, EventAccess> merged = new LinkedHashMap<>();
        for (CalendarEvent event : ownEvents) {
            merged.put(event.getId(), new EventAccess(event, false, SHARE_PERMISSION_OWNER, true, true));
        }
        for (CalendarEvent event : sharedEvents) {
            CalendarEventShare share = acceptedShareByEventId.get(event.getId());
            if (share == null) {
                continue;
            }
            String permission = normalizePermission(share.getPermission());
            merged.putIfAbsent(
                    event.getId(),
                    new EventAccess(event, true, permission, SHARE_PERMISSION_EDIT.equals(permission), false)
            );
        }

        List<EventAccess> result = new ArrayList<>(merged.values());
        result.sort((left, right) -> {
            int byStart = left.event().getStartAt().compareTo(right.event().getStartAt());
            if (byStart != 0) {
                return byStart;
            }
            return left.event().getId().compareTo(right.event().getId());
        });
        return result;
    }

    private EventAccess resolveEventAccess(Long userId, Long eventId) {
        CalendarEvent event = calendarEventMapper.selectById(eventId);
        if (event == null) {
            throw new BizException(ErrorCode.CALENDAR_EVENT_NOT_FOUND);
        }
        if (event.getOwnerId().equals(userId)) {
            return new EventAccess(event, false, SHARE_PERMISSION_OWNER, true, true);
        }

        CalendarEventShare share = calendarEventShareMapper.selectOne(new LambdaQueryWrapper<CalendarEventShare>()
                .eq(CalendarEventShare::getEventId, eventId)
                .eq(CalendarEventShare::getTargetUserId, userId)
                .eq(CalendarEventShare::getResponseStatus, SHARE_STATUS_ACCEPTED));
        if (share == null) {
            throw new BizException(ErrorCode.CALENDAR_EVENT_NOT_FOUND);
        }

        String permission = normalizePermission(share.getPermission());
        return new EventAccess(event, true, permission, SHARE_PERMISSION_EDIT.equals(permission), false);
    }

    private CalendarEvent loadOwnedEvent(Long ownerId, Long eventId) {
        CalendarEvent event = calendarEventMapper.selectOne(new LambdaQueryWrapper<CalendarEvent>()
                .eq(CalendarEvent::getOwnerId, ownerId)
                .eq(CalendarEvent::getId, eventId));
        if (event == null) {
            throw new BizException(ErrorCode.CALENDAR_EVENT_NOT_FOUND);
        }
        return event;
    }

    private CalendarEventShare loadOwnedShare(Long ownerId, Long eventId, Long shareId) {
        CalendarEventShare share = calendarEventShareMapper.selectOne(new LambdaQueryWrapper<CalendarEventShare>()
                .eq(CalendarEventShare::getOwnerId, ownerId)
                .eq(CalendarEventShare::getEventId, eventId)
                .eq(CalendarEventShare::getId, shareId));
        if (share == null) {
            throw new BizException(ErrorCode.CALENDAR_SHARE_NOT_FOUND);
        }
        return share;
    }

    private CalendarEventShare findShareByEventAndTarget(Long eventId, Long targetUserId) {
        return calendarEventShareMapper.selectOne(new LambdaQueryWrapper<CalendarEventShare>()
                .eq(CalendarEventShare::getEventId, eventId)
                .eq(CalendarEventShare::getTargetUserId, targetUserId));
    }

    private List<CalendarEvent> queryEventsByOwnerAndRange(Long ownerId, LocalDateTime fromAt, LocalDateTime toAt) {
        return calendarEventMapper.selectList(buildOverlapQuery(fromAt, toAt)
                .eq(CalendarEvent::getOwnerId, ownerId));
    }

    private List<CalendarEvent> queryEventsByIdsAndRange(List<Long> eventIds, LocalDateTime fromAt, LocalDateTime toAt) {
        if (eventIds.isEmpty()) {
            return List.of();
        }
        return calendarEventMapper.selectList(buildOverlapQuery(fromAt, toAt)
                .in(CalendarEvent::getId, eventIds));
    }

    private void validateEventTime(LocalDateTime startAt, LocalDateTime endAt) {
        if (!startAt.isBefore(endAt)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "`startAt` must be earlier than `endAt`");
        }
    }

    private LambdaQueryWrapper<CalendarEvent> buildOverlapQuery(LocalDateTime fromAt, LocalDateTime toAt) {
        return new LambdaQueryWrapper<CalendarEvent>()
                .lt(CalendarEvent::getStartAt, toAt)
                .gt(CalendarEvent::getEndAt, fromAt)
                .orderByAsc(CalendarEvent::getStartAt)
                .orderByAsc(CalendarEvent::getId);
    }

    private void replaceAttendees(Long ownerId, Long eventId, List<CalendarAttendeeInput> inputs, LocalDateTime now) {
        Map<String, CalendarEventAttendee> existingByEmail = new LinkedHashMap<>();
        for (CalendarEventAttendee attendee : calendarEventAttendeeMapper.selectList(new LambdaQueryWrapper<CalendarEventAttendee>()
                .eq(CalendarEventAttendee::getOwnerId, ownerId)
                .eq(CalendarEventAttendee::getEventId, eventId))) {
            existingByEmail.put(attendee.getEmail(), attendee);
        }
        calendarEventAttendeeMapper.purgeByOwnerAndEvent(ownerId, eventId);

        Map<String, CalendarAttendeeInput> deduplicated = new LinkedHashMap<>();
        if (inputs != null) {
            for (CalendarAttendeeInput input : inputs) {
                String email = normalizeEmail(input.email(), "Attendee email");
                deduplicated.putIfAbsent(email, input);
            }
        }

        for (Map.Entry<String, CalendarAttendeeInput> entry : deduplicated.entrySet()) {
            CalendarAttendeeInput input = entry.getValue();
            CalendarEventAttendee attendee = new CalendarEventAttendee();
            attendee.setOwnerId(ownerId);
            attendee.setEventId(eventId);
            attendee.setEmail(entry.getKey());
            attendee.setDisplayName(normalizeText(input.displayName()));
            attendee.setResponseStatus(resolveAttendeeResponseStatus(existingByEmail.get(entry.getKey())));
            attendee.setCreatedAt(now);
            attendee.setUpdatedAt(now);
            attendee.setDeleted(0);
            calendarEventAttendeeMapper.insert(attendee);
        }
    }

    private String resolveAttendeeResponseStatus(CalendarEventAttendee existingAttendee) {
        if (existingAttendee == null || !StringUtils.hasText(existingAttendee.getResponseStatus())) {
            return SHARE_STATUS_NEEDS_ACTION;
        }
        return existingAttendee.getResponseStatus();
    }

    private List<CalendarAttendeeVo> listAttendees(Long eventId) {
        return calendarEventAttendeeMapper.selectList(new LambdaQueryWrapper<CalendarEventAttendee>()
                        .eq(CalendarEventAttendee::getEventId, eventId)
                        .orderByAsc(CalendarEventAttendee::getEmail))
                .stream()
                .map(attendee -> new CalendarAttendeeVo(
                        String.valueOf(attendee.getId()),
                        attendee.getEmail(),
                        attendee.getDisplayName(),
                        attendee.getResponseStatus()
                ))
                .toList();
    }

    private Map<Long, Integer> buildAttendeeCountMap(List<Long> eventIds) {
        Map<Long, Integer> countMap = new LinkedHashMap<>();
        if (eventIds.isEmpty()) {
            return countMap;
        }
        for (CalendarEventAttendee attendee : calendarEventAttendeeMapper.selectList(new LambdaQueryWrapper<CalendarEventAttendee>()
                .in(CalendarEventAttendee::getEventId, eventIds))) {
            countMap.merge(attendee.getEventId(), 1, Integer::sum);
        }
        return countMap;
    }

    private Map<Long, List<CalendarEventAttendee>> loadAttendeeMap(List<Long> eventIds) {
        Map<Long, List<CalendarEventAttendee>> attendeeMap = new LinkedHashMap<>();
        if (eventIds.isEmpty()) {
            return attendeeMap;
        }
        for (CalendarEventAttendee attendee : calendarEventAttendeeMapper.selectList(new LambdaQueryWrapper<CalendarEventAttendee>()
                .in(CalendarEventAttendee::getEventId, eventIds)
                .orderByAsc(CalendarEventAttendee::getEmail))) {
            attendeeMap.computeIfAbsent(attendee.getEventId(), key -> new ArrayList<>()).add(attendee);
        }
        return attendeeMap;
    }

    private Map<Long, CalendarEvent> loadEventMap(List<Long> eventIds) {
        Map<Long, CalendarEvent> eventMap = new LinkedHashMap<>();
        if (eventIds.isEmpty()) {
            return eventMap;
        }
        for (CalendarEvent event : calendarEventMapper.selectList(new LambdaQueryWrapper<CalendarEvent>()
                .in(CalendarEvent::getId, new LinkedHashSet<>(eventIds)))) {
            eventMap.put(event.getId(), event);
        }
        return eventMap;
    }

    private Map<Long, String> buildOwnerEmailMap(List<Long> userIds) {
        Map<Long, String> ownerEmailMap = new LinkedHashMap<>();
        Set<Long> distinctUserIds = new LinkedHashSet<>(userIds);
        if (distinctUserIds.isEmpty()) {
            return ownerEmailMap;
        }
        for (UserAccount user : userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>()
                .in(UserAccount::getId, distinctUserIds))) {
            ownerEmailMap.put(user.getId(), user.getEmail());
        }
        return ownerEmailMap;
    }

    private String resolveUserEmail(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        return user == null ? null : user.getEmail();
    }

    private CalendarEventItemVo toItemVo(EventAccess access, int attendeeCount, String ownerEmail) {
        CalendarEvent event = access.event();
        return new CalendarEventItemVo(
                String.valueOf(event.getId()),
                event.getTitle(),
                event.getLocation(),
                event.getStartAt(),
                event.getEndAt(),
                event.getAllDay() != null && event.getAllDay() == 1,
                event.getTimezone(),
                event.getReminderMinutes(),
                attendeeCount,
                event.getUpdatedAt(),
                access.shared(),
                ownerEmail,
                access.permission(),
                access.canEdit(),
                access.canDelete()
        );
    }

    private CalendarEventDetailVo toDetailVo(EventAccess access, List<CalendarAttendeeVo> attendees, String ownerEmail) {
        CalendarEvent event = access.event();
        return new CalendarEventDetailVo(
                String.valueOf(event.getId()),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStartAt(),
                event.getEndAt(),
                event.getAllDay() != null && event.getAllDay() == 1,
                event.getTimezone(),
                event.getReminderMinutes(),
                attendees,
                event.getCreatedAt(),
                event.getUpdatedAt(),
                access.shared(),
                ownerEmail,
                access.permission(),
                access.canEdit(),
                access.canDelete()
        );
    }

    private CalendarEventShareVo toShareVo(CalendarEventShare share) {
        return new CalendarEventShareVo(
                String.valueOf(share.getId()),
                String.valueOf(share.getEventId()),
                String.valueOf(share.getTargetUserId()),
                share.getTargetEmail(),
                share.getPermission(),
                share.getResponseStatus(),
                share.getCreatedAt(),
                share.getUpdatedAt()
        );
    }

    private CalendarIncomingShareVo toIncomingShareVo(CalendarEventShare share, CalendarEvent event, String ownerEmail) {
        return new CalendarIncomingShareVo(
                String.valueOf(share.getId()),
                String.valueOf(share.getEventId()),
                event.getTitle(),
                ownerEmail,
                share.getPermission(),
                share.getResponseStatus(),
                share.getUpdatedAt()
        );
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmail(String email, String fieldName) {
        if (!StringUtils.hasText(email)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, fieldName + " is required");
        }
        return email.trim().toLowerCase();
    }

    private String normalizePermission(String permission) {
        if (!StringUtils.hasText(permission)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Share permission is required");
        }
        String normalized = permission.trim().toUpperCase();
        if (!SHARE_PERMISSION_VIEW.equals(normalized) && !SHARE_PERMISSION_EDIT.equals(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Share permission must be VIEW or EDIT");
        }
        return normalized;
    }

    private String normalizeTimezone(String timezone) {
        return CalendarTimezoneResolver.normalizeOrDefault(timezone, "UTC", "Timezone");
    }

    private String normalizeResponse(String response) {
        if (!StringUtils.hasText(response)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Response is required");
        }
        String normalized = response.trim().toUpperCase();
        return switch (normalized) {
            case "ACCEPT" -> SHARE_STATUS_ACCEPTED;
            case "DECLINE" -> SHARE_STATUS_DECLINED;
            default -> throw new BizException(ErrorCode.INVALID_ARGUMENT, "Response must be ACCEPT or DECLINE");
        };
    }

    private LocalDateTime parseDateTimeOrDefault(String raw, LocalDateTime defaultValue) {
        if (!StringUtils.hasText(raw)) {
            return defaultValue;
        }
        try {
            return LocalDateTime.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid datetime format: " + raw);
        }
    }

    private void appendStartEnd(StringBuilder builder, CalendarEvent event) {
        if (event.getAllDay() != null && event.getAllDay() == 1) {
            builder.append("DTSTART;VALUE=DATE:").append(formatDateIcs(event.getStartAt().toLocalDate())).append("\r\n");
            builder.append("DTEND;VALUE=DATE:").append(formatDateIcs(event.getEndAt().toLocalDate())).append("\r\n");
            return;
        }
        builder.append("DTSTART:").append(formatDateTimeIcs(event.getStartAt())).append("\r\n");
        builder.append("DTEND:").append(formatDateTimeIcs(event.getEndAt())).append("\r\n");
    }

    private String formatDateTimeIcs(LocalDateTime dateTime) {
        return dateTime.withNano(0).format(ICS_DATETIME_FORMATTER);
    }

    private String formatDateIcs(LocalDate date) {
        return date.format(ICS_DATE_FORMATTER);
    }

    private String escapeIcsText(String value) {
        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private record EventAccess(
            CalendarEvent event,
            boolean shared,
            String permission,
            boolean canEdit,
            boolean canDelete
    ) {
    }
}
