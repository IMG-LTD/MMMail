package com.mmmail.server.controller;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateCalendarEventRequest;
import com.mmmail.server.model.dto.QueryCalendarAvailabilityRequest;
import com.mmmail.server.model.dto.UpdateCalendarEventRequest;
import com.mmmail.server.model.dto.UpdateCalendarSettingsRequest;
import com.mmmail.server.model.vo.CalendarAvailabilityVo;
import com.mmmail.server.model.vo.CalendarEventDetailVo;
import com.mmmail.server.model.vo.CalendarSettingsVo;
import com.mmmail.server.service.CalendarAvailabilityService;
import com.mmmail.server.service.CalendarService;
import com.mmmail.server.service.CalendarSettingsService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v2/calendar")
public class V21CalendarController {

    private final CalendarService calendarService;
    private final CalendarAvailabilityService calendarAvailabilityService;
    private final CalendarSettingsService calendarSettingsService;

    public V21CalendarController(
            CalendarService calendarService,
            CalendarAvailabilityService calendarAvailabilityService,
            CalendarSettingsService calendarSettingsService
    ) {
        this.calendarService = calendarService;
        this.calendarAvailabilityService = calendarAvailabilityService;
        this.calendarSettingsService = calendarSettingsService;
    }

    @GetMapping("/events")
    public Result<?> events(
            @RequestParam(required = false) String view,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer days,
            HttpServletRequest httpRequest
    ) {
        if (!StringUtils.hasText(view)) {
            return Result.success(calendarService.listEvents(SecurityUtils.currentUserId(), from, to));
        }
        if ("agenda".equalsIgnoreCase(view.trim())) {
            return Result.success(calendarService.listAgenda(SecurityUtils.currentUserId(), days, httpRequest.getRemoteAddr()));
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Calendar view must be agenda");
    }

    @PostMapping("/events")
    public Result<CalendarEventDetailVo> createEvent(
            @Valid @RequestBody CreateCalendarEventRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(calendarService.createEvent(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @PatchMapping("/events/{eventId}")
    public Result<CalendarEventDetailVo> updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateCalendarEventRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(calendarService.updateEvent(SecurityUtils.currentUserId(), eventId, request, httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/events/{eventId}")
    public Result<Void> deleteEvent(@PathVariable Long eventId, HttpServletRequest httpRequest) {
        calendarService.deleteEvent(SecurityUtils.currentUserId(), eventId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @PostMapping("/availability")
    public Result<CalendarAvailabilityVo> availability(
            @Valid @RequestBody QueryCalendarAvailabilityRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(calendarAvailabilityService.queryAvailability(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/settings")
    public Result<CalendarSettingsVo> settings() {
        return Result.success(calendarSettingsService.getSettings(SecurityUtils.currentUserId()));
    }

    @PatchMapping("/settings")
    public Result<CalendarSettingsVo> updateSettings(
            @Valid @RequestBody UpdateCalendarSettingsRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(calendarSettingsService.updateSettings(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }
}
