package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.dto.UpdateCalendarSettingsRequest;
import com.mmmail.server.model.vo.CalendarSettingsVo;
import com.mmmail.server.model.vo.UserPreferenceVo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CalendarSettingsService {

    private static final String SUPPORTED_WEEK_START = "monday";
    private static final List<String> SUPPORTED_WORKING_HOURS = List.of("09:00", "18:00");

    private final UserPreferenceService userPreferenceService;

    public CalendarSettingsService(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    public CalendarSettingsVo getSettings(Long userId) {
        return toSettings(userPreferenceService.getProfile(userId));
    }

    public CalendarSettingsVo updateSettings(Long userId, UpdateCalendarSettingsRequest request, String ipAddress) {
        assertSupportedCalendarFields(request);
        UserPreferenceVo preference = userPreferenceService.updateCalendarTimezone(
                userId,
                request.defaultTimezone(),
                ipAddress
        );
        return toSettings(preference);
    }

    private void assertSupportedCalendarFields(UpdateCalendarSettingsRequest request) {
        if (!SUPPORTED_WEEK_START.equals(request.weekStartsOn())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Calendar week start is not configurable in Community runtime");
        }
        if (!SUPPORTED_WORKING_HOURS.equals(request.workingHours())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Calendar working hours are not configurable in Community runtime");
        }
    }

    private CalendarSettingsVo toSettings(UserPreferenceVo preference) {
        return new CalendarSettingsVo(preference.timezone(), SUPPORTED_WEEK_START, SUPPORTED_WORKING_HOURS);
    }
}
