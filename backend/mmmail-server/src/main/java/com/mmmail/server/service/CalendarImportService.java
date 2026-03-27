package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.server.mapper.CalendarEventMapper;
import com.mmmail.server.mapper.UserPreferenceMapper;
import com.mmmail.server.model.dto.ImportCalendarIcsRequest;
import com.mmmail.server.model.entity.CalendarEvent;
import com.mmmail.server.model.entity.UserPreference;
import com.mmmail.server.model.vo.CalendarImportResultVo;
import com.mmmail.server.util.CalendarTimezoneResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CalendarImportService {

    private static final int DEFAULT_REMINDER_MINUTES = 15;

    private final CalendarEventMapper calendarEventMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final CalendarIcsImportParser calendarIcsImportParser;
    private final SuiteService suiteService;
    private final AuditService auditService;

    public CalendarImportService(
            CalendarEventMapper calendarEventMapper,
            UserPreferenceMapper userPreferenceMapper,
            CalendarIcsImportParser calendarIcsImportParser,
            SuiteService suiteService,
            AuditService auditService
    ) {
        this.calendarEventMapper = calendarEventMapper;
        this.userPreferenceMapper = userPreferenceMapper;
        this.calendarIcsImportParser = calendarIcsImportParser;
        this.suiteService = suiteService;
        this.auditService = auditService;
    }

    @Transactional
    public CalendarImportResultVo importIcs(Long userId, ImportCalendarIcsRequest request, String ipAddress) {
        String timezone = resolveTimezone(userId, request.timezone());
        List<CalendarIcsImportParser.CalendarImportDraft> drafts = calendarIcsImportParser.parse(request.content(), timezone);
        LocalDateTime now = LocalDateTime.now();
        int reminderMinutes = request.reminderMinutes() == null ? DEFAULT_REMINDER_MINUTES : request.reminderMinutes();
        List<String> eventIds = new ArrayList<>();

        for (CalendarIcsImportParser.CalendarImportDraft draft : drafts) {
            suiteService.assertCalendarEventQuota(userId, ipAddress);
            CalendarEvent event = buildEvent(userId, draft, reminderMinutes, now);
            calendarEventMapper.insert(event);
            eventIds.add(String.valueOf(event.getId()));
        }

        auditService.record(userId, "CAL_IMPORT", "count=" + eventIds.size(), ipAddress);
        return new CalendarImportResultVo(drafts.size(), eventIds.size(), eventIds);
    }

    private String resolveTimezone(Long userId, String candidate) {
        UserPreference preference = userPreferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getOwnerId, userId));
        String fallback = preference == null ? null : preference.getTimezone();
        return CalendarTimezoneResolver.normalizeOrDefault(candidate, fallback, "Timezone");
    }

    private CalendarEvent buildEvent(
            Long userId,
            CalendarIcsImportParser.CalendarImportDraft draft,
            int reminderMinutes,
            LocalDateTime now
    ) {
        CalendarEvent event = new CalendarEvent();
        event.setOwnerId(userId);
        event.setTitle(draft.title());
        event.setDescription(draft.description());
        event.setLocation(draft.location());
        event.setStartAt(draft.startAt());
        event.setEndAt(draft.endAt());
        event.setAllDay(draft.allDay() ? 1 : 0);
        event.setTimezone(draft.timezone());
        event.setReminderMinutes(reminderMinutes);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        event.setDeleted(0);
        return event;
    }
}
