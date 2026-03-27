package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateCalendarEventRequest;
import com.mmmail.server.model.dto.CreateCalendarShareRequest;
import com.mmmail.server.model.dto.ImportCalendarIcsRequest;
import com.mmmail.server.model.dto.QueryCalendarAvailabilityRequest;
import com.mmmail.server.model.dto.UpdateCalendarShareRequest;
import com.mmmail.server.model.dto.RespondCalendarShareRequest;
import com.mmmail.server.model.dto.UpdateCalendarEventRequest;
import com.mmmail.server.model.vo.CalendarAgendaItemVo;
import com.mmmail.server.model.vo.CalendarEventDetailVo;
import com.mmmail.server.model.vo.CalendarEventItemVo;
import com.mmmail.server.model.vo.CalendarImportResultVo;
import com.mmmail.server.model.vo.CalendarEventShareVo;
import com.mmmail.server.model.vo.CalendarAvailabilityVo;
import com.mmmail.server.model.vo.CalendarIncomingShareVo;
import com.mmmail.server.service.CalendarAvailabilityService;
import com.mmmail.server.service.CalendarImportService;
import com.mmmail.server.service.CalendarService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    private final CalendarService calendarService;
    private final CalendarAvailabilityService calendarAvailabilityService;
    private final CalendarImportService calendarImportService;

    public CalendarController(
            CalendarService calendarService,
            CalendarAvailabilityService calendarAvailabilityService,
            CalendarImportService calendarImportService
    ) {
        this.calendarService = calendarService;
        this.calendarAvailabilityService = calendarAvailabilityService;
        this.calendarImportService = calendarImportService;
    }

    @GetMapping("/events")
    public Result<List<CalendarEventItemVo>> events(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return Result.success(calendarService.listEvents(SecurityUtils.currentUserId(), from, to));
    }

    @GetMapping("/events/{eventId}")
    public Result<CalendarEventDetailVo> detail(@PathVariable Long eventId) {
        return Result.success(calendarService.getEvent(SecurityUtils.currentUserId(), eventId));
    }

    @PostMapping("/events")
    public Result<CalendarEventDetailVo> create(
            @Valid @RequestBody CreateCalendarEventRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(calendarService.createEvent(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @PutMapping("/events/{eventId}")
    public Result<CalendarEventDetailVo> update(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateCalendarEventRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(calendarService.updateEvent(SecurityUtils.currentUserId(), eventId, request, httpRequest.getRemoteAddr()));
    }

    @DeleteMapping("/events/{eventId}")
    public Result<Void> delete(
            @PathVariable Long eventId,
            HttpServletRequest httpRequest
    ) {
        calendarService.deleteEvent(SecurityUtils.currentUserId(), eventId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @PostMapping("/events/{eventId}/shares")
    public Result<CalendarEventShareVo> createShare(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateCalendarShareRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(calendarService.createShare(
                SecurityUtils.currentUserId(),
                eventId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/events/{eventId}/shares")
    public Result<List<CalendarEventShareVo>> listShares(@PathVariable Long eventId) {
        return Result.success(calendarService.listShares(SecurityUtils.currentUserId(), eventId));
    }

    @DeleteMapping("/events/{eventId}/shares/{shareId}")
    public Result<Void> removeShare(
            @PathVariable Long eventId,
            @PathVariable Long shareId,
            HttpServletRequest httpRequest
    ) {
        calendarService.removeShare(SecurityUtils.currentUserId(), eventId, shareId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @PutMapping("/events/{eventId}/shares/{shareId}")
    public Result<CalendarEventShareVo> updateShare(
            @PathVariable Long eventId,
            @PathVariable Long shareId,
            @Valid @RequestBody UpdateCalendarShareRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(calendarService.updateSharePermission(
                SecurityUtils.currentUserId(),
                eventId,
                shareId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/shares/incoming")
    public Result<List<CalendarIncomingShareVo>> incoming(HttpServletRequest httpRequest) {
        return Result.success(calendarService.listIncomingShares(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/shares/{shareId}/response")
    public Result<CalendarIncomingShareVo> respondShare(
            @PathVariable Long shareId,
            @Valid @RequestBody RespondCalendarShareRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(calendarService.respondShare(
                SecurityUtils.currentUserId(),
                shareId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/agenda")
    public Result<List<CalendarAgendaItemVo>> agenda(
            @RequestParam(required = false) Integer days,
            HttpServletRequest httpRequest
    ) {
        return Result.success(calendarService.listAgenda(SecurityUtils.currentUserId(), days, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/availability/query")
    public Result<CalendarAvailabilityVo> queryAvailability(
            @Valid @RequestBody QueryCalendarAvailabilityRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(calendarAvailabilityService.queryAvailability(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/export")
    public Result<String> export(
            @RequestParam(defaultValue = "ics") String format,
            HttpServletRequest httpRequest
    ) {
        return Result.success(calendarService.exportCalendar(SecurityUtils.currentUserId(), format, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/import/ics")
    public Result<CalendarImportResultVo> importIcs(
            @Valid @RequestBody ImportCalendarIcsRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(calendarImportService.importIcs(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }
}
