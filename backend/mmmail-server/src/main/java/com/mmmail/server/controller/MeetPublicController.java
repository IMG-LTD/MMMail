package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.SubmitMeetGuestRequestRequest;
import com.mmmail.server.model.dto.UpdateMeetParticipantMediaRequest;
import com.mmmail.server.model.vo.MeetGuestJoinOverviewVo;
import com.mmmail.server.model.vo.MeetGuestRequestVo;
import com.mmmail.server.model.vo.MeetGuestSessionVo;
import com.mmmail.server.service.MeetGuestRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/meet")
public class MeetPublicController {

    private final MeetGuestRequestService meetGuestRequestService;

    public MeetPublicController(MeetGuestRequestService meetGuestRequestService) {
        this.meetGuestRequestService = meetGuestRequestService;
    }

    @GetMapping("/join/{joinCode}")
    public Result<MeetGuestJoinOverviewVo> joinOverview(@PathVariable String joinCode, HttpServletRequest httpRequest) {
        return Result.success(meetGuestRequestService.getJoinOverview(joinCode, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/join/{joinCode}/requests")
    public Result<MeetGuestRequestVo> submitRequest(
            @PathVariable String joinCode,
            @Valid @RequestBody SubmitMeetGuestRequestRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(meetGuestRequestService.submitGuestRequest(
                joinCode,
                request.displayName(),
                request.audioEnabled(),
                request.videoEnabled(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/requests/{requestToken}")
    public Result<MeetGuestRequestVo> getRequest(@PathVariable String requestToken, HttpServletRequest httpRequest) {
        return Result.success(meetGuestRequestService.getGuestRequest(requestToken, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/sessions/{guestSessionToken}")
    public Result<MeetGuestSessionVo> getSession(@PathVariable String guestSessionToken, HttpServletRequest httpRequest) {
        return Result.success(meetGuestRequestService.getGuestSession(guestSessionToken, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/sessions/{guestSessionToken}/heartbeat")
    public Result<MeetGuestSessionVo> heartbeat(@PathVariable String guestSessionToken, HttpServletRequest httpRequest) {
        return Result.success(meetGuestRequestService.heartbeatGuestSession(guestSessionToken, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/sessions/{guestSessionToken}/media")
    public Result<MeetGuestSessionVo> updateMedia(
            @PathVariable String guestSessionToken,
            @Valid @RequestBody UpdateMeetParticipantMediaRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(meetGuestRequestService.updateGuestSessionMedia(
                guestSessionToken,
                request.audioEnabled(),
                request.videoEnabled(),
                request.screenSharing(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/sessions/{guestSessionToken}/leave")
    public Result<MeetGuestSessionVo> leave(@PathVariable String guestSessionToken, HttpServletRequest httpRequest) {
        return Result.success(meetGuestRequestService.leaveGuestSession(guestSessionToken, httpRequest.getRemoteAddr()));
    }
}
