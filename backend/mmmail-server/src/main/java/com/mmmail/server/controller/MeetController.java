package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateMeetRoomRequest;
import com.mmmail.server.model.dto.JoinMeetRoomRequest;
import com.mmmail.server.model.dto.JoinMeetWaitlistRequest;
import com.mmmail.server.model.dto.PruneMeetInactiveParticipantsRequest;
import com.mmmail.server.model.dto.RequestMeetEnterpriseAccessRequest;
import com.mmmail.server.model.dto.ReportMeetQualityRequest;
import com.mmmail.server.model.dto.SendMeetSignalRequest;
import com.mmmail.server.model.dto.TransferMeetHostRequest;
import com.mmmail.server.model.dto.UpdateMeetParticipantMediaRequest;
import com.mmmail.server.model.dto.UpdateMeetParticipantRoleRequest;
import com.mmmail.server.model.vo.MeetAccessOverviewVo;
import com.mmmail.server.model.vo.MeetParticipantVo;
import com.mmmail.server.model.vo.MeetPruneInactiveResultVo;
import com.mmmail.server.model.vo.MeetQualitySnapshotVo;
import com.mmmail.server.model.vo.MeetRoomVo;
import com.mmmail.server.model.vo.MeetSignalEventVo;
import com.mmmail.server.service.MeetAccessService;
import com.mmmail.server.service.MeetService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meet")
public class MeetController {

    private final MeetAccessService meetAccessService;
    private final MeetService meetService;

    public MeetController(MeetAccessService meetAccessService, MeetService meetService) {
        this.meetAccessService = meetAccessService;
        this.meetService = meetService;
    }

    @GetMapping("/access/overview")
    public Result<MeetAccessOverviewVo> accessOverview(HttpServletRequest httpRequest) {
        Long userId = SecurityUtils.currentUserId();
        return Result.success(meetAccessService.getOverview(userId, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/access/waitlist")
    public Result<MeetAccessOverviewVo> joinWaitlist(
            @Valid @RequestBody(required = false) JoinMeetWaitlistRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.currentUserId();
        String note = request == null ? null : request.note();
        return Result.success(meetAccessService.joinWaitlist(userId, note, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/access/contact-sales")
    public Result<MeetAccessOverviewVo> requestEnterpriseAccess(
            @Valid @RequestBody RequestMeetEnterpriseAccessRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.currentUserId();
        return Result.success(meetAccessService.requestEnterpriseAccess(
                userId,
                request.companyName(),
                request.requestedSeats(),
                request.note(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/access/activate")
    public Result<MeetAccessOverviewVo> activate(HttpServletRequest httpRequest) {
        Long userId = SecurityUtils.currentUserId();
        return Result.success(meetAccessService.activate(userId, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/rooms")
    public Result<List<MeetRoomVo>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "ROOM_LIST");
        return Result.success(meetService.list(userId, status, limit, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/rooms/current")
    public Result<MeetRoomVo> current(HttpServletRequest httpRequest) {
        Long userId = requireMeetAccess(httpRequest, "ROOM_CURRENT");
        return Result.success(meetService.current(userId, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/rooms/history")
    public Result<List<MeetRoomVo>> history(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "ROOM_HISTORY");
        return Result.success(meetService.history(userId, limit, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/rooms")
    public Result<MeetRoomVo> create(
            @Valid @RequestBody CreateMeetRoomRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "ROOM_CREATE");
        return Result.success(meetService.create(
                userId,
                request.topic(),
                request.accessLevel(),
                request.maxParticipants(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/rooms/{roomId}/join-code/rotate")
    public Result<MeetRoomVo> rotateJoinCode(@PathVariable Long roomId, HttpServletRequest httpRequest) {
        Long userId = requireMeetAccess(httpRequest, "ROOM_ROTATE_JOIN_CODE");
        return Result.success(meetService.rotateJoinCode(
                userId,
                roomId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/rooms/{roomId}/end")
    public Result<MeetRoomVo> end(@PathVariable Long roomId, HttpServletRequest httpRequest) {
        Long userId = requireMeetAccess(httpRequest, "ROOM_END");
        return Result.success(meetService.end(
                userId,
                roomId,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/rooms/{roomId}/participants")
    public Result<List<MeetParticipantVo>> participants(@PathVariable Long roomId, HttpServletRequest httpRequest) {
        Long userId = requireMeetAccess(httpRequest, "PARTICIPANT_LIST");
        return Result.success(meetService.participants(
                userId,
                roomId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/rooms/{roomId}/participants/join")
    public Result<MeetParticipantVo> join(
            @PathVariable Long roomId,
            @Valid @RequestBody JoinMeetRoomRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "PARTICIPANT_JOIN");
        return Result.success(meetService.join(
                userId,
                roomId,
                request.displayName(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/rooms/{roomId}/participants/{participantId}/leave")
    public Result<MeetParticipantVo> leave(
            @PathVariable Long roomId,
            @PathVariable Long participantId,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "PARTICIPANT_LEAVE");
        return Result.success(meetService.leave(
                userId,
                roomId,
                participantId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/rooms/{roomId}/participants/{participantId}/heartbeat")
    public Result<MeetParticipantVo> heartbeat(
            @PathVariable Long roomId,
            @PathVariable Long participantId,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "PARTICIPANT_HEARTBEAT");
        return Result.success(meetService.heartbeat(
                userId,
                roomId,
                participantId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/rooms/{roomId}/participants/{participantId}/media")
    public Result<MeetParticipantVo> updateMedia(
            @PathVariable Long roomId,
            @PathVariable Long participantId,
            @Valid @RequestBody UpdateMeetParticipantMediaRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "PARTICIPANT_MEDIA_UPDATE");
        return Result.success(meetService.updateMedia(
                userId,
                roomId,
                participantId,
                request.audioEnabled(),
                request.videoEnabled(),
                request.screenSharing(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/rooms/{roomId}/participants/{participantId}/quality")
    public Result<MeetQualitySnapshotVo> reportQuality(
            @PathVariable Long roomId,
            @PathVariable Long participantId,
            @Valid @RequestBody ReportMeetQualityRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "PARTICIPANT_QUALITY_REPORT");
        return Result.success(meetService.reportQuality(
                userId,
                roomId,
                participantId,
                request.jitterMs(),
                request.packetLossPercent(),
                request.roundTripMs(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/rooms/{roomId}/quality")
    public Result<List<MeetQualitySnapshotVo>> listQualitySnapshots(
            @PathVariable Long roomId,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "QUALITY_LIST");
        return Result.success(meetService.listQualitySnapshots(
                userId,
                roomId,
                limit,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/rooms/{roomId}/participants/{participantId}/role")
    public Result<MeetParticipantVo> updateRole(
            @PathVariable Long roomId,
            @PathVariable Long participantId,
            @Valid @RequestBody UpdateMeetParticipantRoleRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "PARTICIPANT_ROLE_UPDATE");
        return Result.success(meetService.updateRole(
                userId,
                roomId,
                participantId,
                request.role(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/rooms/{roomId}/participants/{participantId}/remove")
    public Result<MeetParticipantVo> remove(
            @PathVariable Long roomId,
            @PathVariable Long participantId,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "PARTICIPANT_REMOVE");
        return Result.success(meetService.remove(
                userId,
                roomId,
                participantId,
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/rooms/{roomId}/participants/prune-inactive")
    public Result<MeetPruneInactiveResultVo> pruneInactiveParticipants(
            @PathVariable Long roomId,
            @Valid @RequestBody PruneMeetInactiveParticipantsRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "PARTICIPANT_PRUNE");
        return Result.success(meetService.pruneInactiveParticipants(
                userId,
                roomId,
                request.inactiveSeconds(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/rooms/{roomId}/host/transfer")
    public Result<MeetParticipantVo> transferHost(
            @PathVariable Long roomId,
            @Valid @RequestBody TransferMeetHostRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "HOST_TRANSFER");
        return Result.success(meetService.transferHost(
                userId,
                roomId,
                request.targetParticipantId(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/rooms/{roomId}/signals/offer")
    public Result<MeetSignalEventVo> sendOffer(
            @PathVariable Long roomId,
            @Valid @RequestBody SendMeetSignalRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "SIGNAL_SEND_OFFER");
        return Result.success(meetService.sendOffer(
                userId,
                roomId,
                request.fromParticipantId(),
                request.toParticipantId(),
                request.payload(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/rooms/{roomId}/signals/answer")
    public Result<MeetSignalEventVo> sendAnswer(
            @PathVariable Long roomId,
            @Valid @RequestBody SendMeetSignalRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "SIGNAL_SEND_ANSWER");
        return Result.success(meetService.sendAnswer(
                userId,
                roomId,
                request.fromParticipantId(),
                request.toParticipantId(),
                request.payload(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/rooms/{roomId}/signals/ice")
    public Result<MeetSignalEventVo> sendIce(
            @PathVariable Long roomId,
            @Valid @RequestBody SendMeetSignalRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "SIGNAL_SEND_ICE");
        return Result.success(meetService.sendIce(
                userId,
                roomId,
                request.fromParticipantId(),
                request.toParticipantId(),
                request.payload(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/rooms/{roomId}/signals")
    public Result<List<MeetSignalEventVo>> listSignals(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long afterEventSeq,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "SIGNAL_LIST");
        return Result.success(meetService.listSignals(
                userId,
                roomId,
                afterEventSeq,
                limit,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/rooms/{roomId}/signals/stream")
    public Result<List<MeetSignalEventVo>> streamSignals(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long afterEventSeq,
            @RequestParam(required = false) Integer timeoutSeconds,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        Long userId = requireMeetAccess(httpRequest, "SIGNAL_STREAM");
        return Result.success(meetService.streamSignals(
                userId,
                roomId,
                afterEventSeq,
                timeoutSeconds,
                limit,
                httpRequest.getRemoteAddr()
        ));
    }

    private Long requireMeetAccess(HttpServletRequest httpRequest, String action) {
        Long userId = SecurityUtils.currentUserId();
        meetAccessService.assertAccessGranted(userId, httpRequest.getRemoteAddr(), action);
        return userId;
    }
}
