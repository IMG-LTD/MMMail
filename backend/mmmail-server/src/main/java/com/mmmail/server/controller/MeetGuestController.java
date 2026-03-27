package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.vo.MeetGuestRequestVo;
import com.mmmail.server.service.MeetAccessService;
import com.mmmail.server.service.MeetGuestRequestService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meet")
public class MeetGuestController {

    private final MeetAccessService meetAccessService;
    private final MeetGuestRequestService meetGuestRequestService;

    public MeetGuestController(
            MeetAccessService meetAccessService,
            MeetGuestRequestService meetGuestRequestService
    ) {
        this.meetAccessService = meetAccessService;
        this.meetGuestRequestService = meetGuestRequestService;
    }

    @GetMapping("/rooms/{roomId}/guest-requests")
    public Result<List<MeetGuestRequestVo>> listGuestRequests(@PathVariable Long roomId, HttpServletRequest httpRequest) {
        Long userId = requireMeetAccess(httpRequest, "GUEST_REQUEST_LIST");
        return Result.success(meetGuestRequestService.listRoomGuestRequests(userId, roomId, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/rooms/{roomId}/guest-requests/{requestId}/approve")
    public Result<MeetGuestRequestVo> approveGuestRequest(
            @PathVariable Long roomId,
            @PathVariable Long requestId,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "GUEST_REQUEST_APPROVE");
        return Result.success(meetGuestRequestService.approveGuestRequest(userId, roomId, requestId, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/rooms/{roomId}/guest-requests/{requestId}/reject")
    public Result<MeetGuestRequestVo> rejectGuestRequest(
            @PathVariable Long roomId,
            @PathVariable Long requestId,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "GUEST_REQUEST_REJECT");
        return Result.success(meetGuestRequestService.rejectGuestRequest(userId, roomId, requestId, httpRequest.getRemoteAddr()));
    }

    private Long requireMeetAccess(HttpServletRequest httpRequest, String action) {
        Long userId = SecurityUtils.currentUserId();
        meetAccessService.assertAccessGranted(userId, httpRequest.getRemoteAddr(), action);
        return userId;
    }
}
