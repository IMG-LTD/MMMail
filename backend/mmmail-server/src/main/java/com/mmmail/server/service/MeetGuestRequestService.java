package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.MeetGuestRequestMapper;
import com.mmmail.server.mapper.MeetRoomParticipantMapper;
import com.mmmail.server.mapper.MeetRoomSessionMapper;
import com.mmmail.server.model.entity.MeetGuestRequest;
import com.mmmail.server.model.entity.MeetRoomParticipant;
import com.mmmail.server.model.entity.MeetRoomSession;
import com.mmmail.server.model.vo.MeetGuestJoinOverviewVo;
import com.mmmail.server.model.vo.MeetGuestParticipantViewVo;
import com.mmmail.server.model.vo.MeetGuestRequestVo;
import com.mmmail.server.model.vo.MeetGuestSessionVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class MeetGuestRequestService {

    private static final String ROOM_STATUS_ACTIVE = "ACTIVE";
    private static final String ROOM_STATUS_ENDED = "ENDED";
    private static final String ACCESS_LEVEL_PUBLIC = "PUBLIC";
    private static final String ROLE_HOST = "HOST";
    private static final String ROLE_CO_HOST = "CO_HOST";
    private static final String ROLE_PARTICIPANT = "PARTICIPANT";
    private static final String PARTICIPANT_STATUS_ACTIVE = "ACTIVE";
    private static final String PARTICIPANT_STATUS_LEFT = "LEFT";
    private static final String PARTICIPANT_STATUS_REMOVED = "REMOVED";
    private static final String REQUEST_STATUS_PENDING = "PENDING";
    private static final String REQUEST_STATUS_APPROVED = "APPROVED";
    private static final String REQUEST_STATUS_REJECTED = "REJECTED";
    private static final String REQUEST_STATUS_LEFT = "LEFT";
    private static final String SESSION_STATUS_WAITING = "WAITING";
    private static final String SESSION_STATUS_ACTIVE = "ACTIVE";
    private static final String SESSION_STATUS_REJECTED = "REJECTED";
    private static final String SESSION_STATUS_LEFT = "LEFT";
    private static final String SESSION_STATUS_REMOVED = "REMOVED";
    private static final String SESSION_STATUS_ROOM_ENDED = "ROOM_ENDED";
    private static final int FLAG_ON = 1;
    private static final int FLAG_OFF = 0;
    private static final int DEFAULT_LIST_LIMIT = 50;
    private static final String TOKEN_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MeetRoomSessionMapper meetRoomSessionMapper;
    private final MeetRoomParticipantMapper meetRoomParticipantMapper;
    private final MeetGuestRequestMapper meetGuestRequestMapper;
    private final AuditService auditService;

    public MeetGuestRequestService(
            MeetRoomSessionMapper meetRoomSessionMapper,
            MeetRoomParticipantMapper meetRoomParticipantMapper,
            MeetGuestRequestMapper meetGuestRequestMapper,
            AuditService auditService
    ) {
        this.meetRoomSessionMapper = meetRoomSessionMapper;
        this.meetRoomParticipantMapper = meetRoomParticipantMapper;
        this.meetGuestRequestMapper = meetGuestRequestMapper;
        this.auditService = auditService;
    }

    public MeetGuestJoinOverviewVo getJoinOverview(String joinCode, String ipAddress) {
        MeetRoomSession room = loadRoomByJoinCode(joinCode);
        auditService.record(null, "MEET_GUEST_JOIN_OVERVIEW", "roomId=" + room.getId(), ipAddress);
        return toJoinOverview(room);
    }

    @Transactional
    public MeetGuestRequestVo submitGuestRequest(
            String joinCode,
            String displayName,
            Boolean audioEnabled,
            Boolean videoEnabled,
            String ipAddress
    ) {
        MeetRoomSession room = loadRoomByJoinCode(joinCode);
        assertGuestJoinEnabled(room);
        ensureRoomCapacity(room);
        LocalDateTime now = LocalDateTime.now();
        MeetGuestRequest request = new MeetGuestRequest();
        request.setRoomId(room.getId());
        request.setOwnerId(room.getOwnerId());
        request.setJoinCodeSnapshot(room.getJoinCode());
        request.setRequestToken(generateToken("GJ", 10));
        request.setGuestSessionToken(null);
        request.setParticipantId(null);
        request.setDisplayName(requireDisplayName(displayName));
        request.setAudioEnabled(toFlag(audioEnabled));
        request.setVideoEnabled(toFlag(videoEnabled));
        request.setStatus(REQUEST_STATUS_PENDING);
        request.setRequestedAt(now);
        request.setApprovedAt(null);
        request.setRejectedAt(null);
        request.setCreatedAt(now);
        request.setUpdatedAt(now);
        request.setDeleted(0);
        meetGuestRequestMapper.insert(request);
        auditService.record(null, "MEET_GUEST_REQUEST_CREATE", "roomId=" + room.getId() + ",requestId=" + request.getId(), ipAddress);
        return toRequestVo(request, room);
    }

    public MeetGuestRequestVo getGuestRequest(String requestToken, String ipAddress) {
        MeetGuestRequest request = loadRequestByToken(requestToken);
        MeetRoomSession room = loadRoomById(request.getRoomId());
        auditService.record(null, "MEET_GUEST_REQUEST_GET", "requestId=" + request.getId(), ipAddress);
        return toRequestVo(request, room);
    }

    public List<MeetGuestRequestVo> listRoomGuestRequests(Long userId, Long roomId, String ipAddress) {
        MeetRoomSession room = loadRoomById(roomId);
        requireModerator(userId, room);
        List<MeetGuestRequestVo> requests = meetGuestRequestMapper.selectList(new LambdaQueryWrapper<MeetGuestRequest>()
                        .eq(MeetGuestRequest::getRoomId, roomId)
                        .eq(MeetGuestRequest::getOwnerId, room.getOwnerId())
                        .orderByDesc(MeetGuestRequest::getRequestedAt)
                        .last("limit " + DEFAULT_LIST_LIMIT))
                .stream()
                .map(item -> toRequestVo(item, room))
                .toList();
        auditService.record(userId, "MEET_GUEST_REQUEST_LIST", "roomId=" + roomId + ",count=" + requests.size(), ipAddress);
        return requests;
    }

    @Transactional
    public MeetGuestRequestVo approveGuestRequest(Long userId, Long roomId, Long requestId, String ipAddress) {
        MeetRoomSession room = loadRoomById(roomId);
        requireModerator(userId, room);
        assertGuestJoinEnabled(room);
        ensureRoomCapacity(room);
        MeetGuestRequest request = loadRoomRequest(roomId, room.getOwnerId(), requestId);
        if (REQUEST_STATUS_REJECTED.equals(request.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet guest request has already been rejected");
        }
        if (REQUEST_STATUS_APPROVED.equals(request.getStatus()) && request.getParticipantId() != null) {
            return toRequestVo(request, room);
        }
        LocalDateTime now = LocalDateTime.now();
        MeetRoomParticipant participant = createGuestParticipant(room, request, now);
        meetRoomParticipantMapper.insert(participant);
        request.setStatus(REQUEST_STATUS_APPROVED);
        request.setGuestSessionToken(generateToken("GS", 12));
        request.setParticipantId(participant.getId());
        request.setApprovedAt(now);
        request.setUpdatedAt(now);
        meetGuestRequestMapper.updateById(request);
        auditService.record(userId, "MEET_GUEST_REQUEST_APPROVE", "roomId=" + roomId + ",requestId=" + requestId, ipAddress);
        return toRequestVo(request, room);
    }

    @Transactional
    public MeetGuestRequestVo rejectGuestRequest(Long userId, Long roomId, Long requestId, String ipAddress) {
        MeetRoomSession room = loadRoomById(roomId);
        requireModerator(userId, room);
        MeetGuestRequest request = loadRoomRequest(roomId, room.getOwnerId(), requestId);
        if (REQUEST_STATUS_APPROVED.equals(request.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Approved guest request cannot be rejected");
        }
        if (REQUEST_STATUS_REJECTED.equals(request.getStatus())) {
            return toRequestVo(request, room);
        }
        LocalDateTime now = LocalDateTime.now();
        request.setStatus(REQUEST_STATUS_REJECTED);
        request.setRejectedAt(now);
        request.setUpdatedAt(now);
        meetGuestRequestMapper.updateById(request);
        auditService.record(userId, "MEET_GUEST_REQUEST_REJECT", "roomId=" + roomId + ",requestId=" + requestId, ipAddress);
        return toRequestVo(request, room);
    }

    public MeetGuestSessionVo getGuestSession(String guestSessionToken, String ipAddress) {
        MeetGuestRequest request = loadRequestBySessionToken(guestSessionToken);
        MeetRoomSession room = loadRoomById(request.getRoomId());
        MeetRoomParticipant participant = loadParticipant(request.getParticipantId());
        auditService.record(null, "MEET_GUEST_SESSION_GET", "requestId=" + request.getId(), ipAddress);
        return toGuestSession(request, room, participant);
    }

    @Transactional
    public MeetGuestSessionVo heartbeatGuestSession(String guestSessionToken, String ipAddress) {
        MeetGuestRequest request = loadRequestBySessionToken(guestSessionToken);
        MeetRoomSession room = requireActiveRoom(request.getRoomId());
        MeetRoomParticipant participant = requireActiveGuestParticipant(request, room);
        LocalDateTime now = LocalDateTime.now();
        participant.setLastHeartbeatAt(now);
        participant.setUpdatedAt(now);
        meetRoomParticipantMapper.updateById(participant);
        auditService.record(null, "MEET_GUEST_SESSION_HEARTBEAT", "requestId=" + request.getId(), ipAddress);
        return toGuestSession(request, room, participant);
    }

    @Transactional
    public MeetGuestSessionVo updateGuestSessionMedia(
            String guestSessionToken,
            Boolean audioEnabled,
            Boolean videoEnabled,
            Boolean screenSharing,
            String ipAddress
    ) {
        MeetGuestRequest request = loadRequestBySessionToken(guestSessionToken);
        MeetRoomSession room = requireActiveRoom(request.getRoomId());
        MeetRoomParticipant participant = requireActiveGuestParticipant(request, room);
        LocalDateTime now = LocalDateTime.now();
        participant.setAudioEnabled(toFlag(audioEnabled));
        participant.setVideoEnabled(toFlag(videoEnabled));
        participant.setScreenSharing(toFlag(screenSharing));
        participant.setUpdatedAt(now);
        meetRoomParticipantMapper.updateById(participant);
        auditService.record(null, "MEET_GUEST_SESSION_MEDIA_UPDATE", "requestId=" + request.getId(), ipAddress);
        return toGuestSession(request, room, participant);
    }

    @Transactional
    public MeetGuestSessionVo leaveGuestSession(String guestSessionToken, String ipAddress) {
        MeetGuestRequest request = loadRequestBySessionToken(guestSessionToken);
        MeetRoomSession room = loadRoomById(request.getRoomId());
        MeetRoomParticipant participant = loadParticipant(request.getParticipantId());
        LocalDateTime now = LocalDateTime.now();
        if (participant != null && PARTICIPANT_STATUS_ACTIVE.equals(participant.getStatus())) {
            participant.setStatus(PARTICIPANT_STATUS_LEFT);
            participant.setLeftAt(now);
            participant.setUpdatedAt(now);
            meetRoomParticipantMapper.updateById(participant);
        }
        request.setStatus(REQUEST_STATUS_LEFT);
        request.setUpdatedAt(now);
        meetGuestRequestMapper.updateById(request);
        auditService.record(null, "MEET_GUEST_SESSION_LEAVE", "requestId=" + request.getId(), ipAddress);
        return toGuestSession(request, room, participant);
    }

    private MeetRoomSession loadRoomByJoinCode(String joinCode) {
        String safeJoinCode = requireJoinCode(joinCode);
        MeetRoomSession room = meetRoomSessionMapper.selectOne(new LambdaQueryWrapper<MeetRoomSession>()
                .eq(MeetRoomSession::getJoinCode, safeJoinCode)
                .orderByDesc(MeetRoomSession::getStartedAt)
                .last("limit 1"));
        if (room == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet join link is invalid");
        }
        return room;
    }

    private MeetRoomSession loadRoomById(Long roomId) {
        MeetRoomSession room = meetRoomSessionMapper.selectById(roomId);
        if (room == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet room is not found");
        }
        return room;
    }

    private MeetRoomSession requireActiveRoom(Long roomId) {
        MeetRoomSession room = loadRoomById(roomId);
        if (!ROOM_STATUS_ACTIVE.equals(room.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet room is not active");
        }
        return room;
    }

    private MeetGuestRequest loadRequestByToken(String requestToken) {
        String safeToken = requireToken(requestToken, "Meet guest request token is invalid");
        MeetGuestRequest request = meetGuestRequestMapper.selectOne(new LambdaQueryWrapper<MeetGuestRequest>()
                .eq(MeetGuestRequest::getRequestToken, safeToken)
                .last("limit 1"));
        if (request == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet guest request is not found");
        }
        return request;
    }

    private MeetGuestRequest loadRequestBySessionToken(String guestSessionToken) {
        String safeToken = requireToken(guestSessionToken, "Meet guest session token is invalid");
        MeetGuestRequest request = meetGuestRequestMapper.selectOne(new LambdaQueryWrapper<MeetGuestRequest>()
                .eq(MeetGuestRequest::getGuestSessionToken, safeToken)
                .last("limit 1"));
        if (request == null || !REQUEST_STATUS_APPROVED.equals(request.getStatus()) && !REQUEST_STATUS_LEFT.equals(request.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet guest session is not found");
        }
        return request;
    }

    private MeetGuestRequest loadRoomRequest(Long roomId, Long ownerId, Long requestId) {
        MeetGuestRequest request = meetGuestRequestMapper.selectOne(new LambdaQueryWrapper<MeetGuestRequest>()
                .eq(MeetGuestRequest::getId, requestId)
                .eq(MeetGuestRequest::getRoomId, roomId)
                .eq(MeetGuestRequest::getOwnerId, ownerId)
                .last("limit 1"));
        if (request == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet guest request is not found");
        }
        return request;
    }

    private MeetRoomParticipant requireActiveGuestParticipant(MeetGuestRequest request, MeetRoomSession room) {
        MeetRoomParticipant participant = loadParticipant(request.getParticipantId());
        if (participant == null || !PARTICIPANT_STATUS_ACTIVE.equals(participant.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet guest session is not active");
        }
        if (!room.getId().equals(participant.getRoomId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet guest session does not belong to this room");
        }
        return participant;
    }

    private MeetRoomParticipant loadParticipant(Long participantId) {
        if (participantId == null) {
            return null;
        }
        return meetRoomParticipantMapper.selectById(participantId);
    }

    private MeetRoomParticipant createGuestParticipant(MeetRoomSession room, MeetGuestRequest request, LocalDateTime now) {
        MeetRoomParticipant participant = new MeetRoomParticipant();
        participant.setRoomId(room.getId());
        participant.setOwnerId(room.getOwnerId());
        participant.setUserId(-request.getId());
        participant.setDisplayName(request.getDisplayName());
        participant.setRole(ROLE_PARTICIPANT);
        participant.setStatus(PARTICIPANT_STATUS_ACTIVE);
        participant.setAudioEnabled(request.getAudioEnabled());
        participant.setVideoEnabled(request.getVideoEnabled());
        participant.setScreenSharing(FLAG_OFF);
        participant.setJoinedAt(now);
        participant.setLeftAt(null);
        participant.setLastHeartbeatAt(now);
        participant.setCreatedAt(now);
        participant.setUpdatedAt(now);
        participant.setDeleted(0);
        return participant;
    }

    private void requireModerator(Long userId, MeetRoomSession room) {
        if (userId.equals(room.getOwnerId())) {
            return;
        }
        MeetRoomParticipant participant = meetRoomParticipantMapper.selectOne(new LambdaQueryWrapper<MeetRoomParticipant>()
                .eq(MeetRoomParticipant::getRoomId, room.getId())
                .eq(MeetRoomParticipant::getOwnerId, room.getOwnerId())
                .eq(MeetRoomParticipant::getUserId, userId)
                .eq(MeetRoomParticipant::getStatus, PARTICIPANT_STATUS_ACTIVE)
                .last("limit 1"));
        if (participant == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No permission to manage Meet guest requests");
        }
        String role = normalizeRole(participant.getRole());
        if (!ROLE_HOST.equals(role) && !ROLE_CO_HOST.equals(role)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No permission to manage Meet guest requests");
        }
    }

    private void assertGuestJoinEnabled(MeetRoomSession room) {
        if (!ROOM_STATUS_ACTIVE.equals(room.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet room is not active");
        }
        if (!ACCESS_LEVEL_PUBLIC.equals(normalizeAccessLevel(room.getAccessLevel()))) {
            throw new BizException(ErrorCode.FORBIDDEN, "Meet room does not accept guest join links");
        }
    }

    private void ensureRoomCapacity(MeetRoomSession room) {
        long activeParticipants = countActiveParticipants(room);
        if (activeParticipants >= room.getMaxParticipants()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet room is full");
        }
    }

    private long countActiveParticipants(MeetRoomSession room) {
        Long activeParticipants = meetRoomParticipantMapper.selectCount(new LambdaQueryWrapper<MeetRoomParticipant>()
                .eq(MeetRoomParticipant::getRoomId, room.getId())
                .eq(MeetRoomParticipant::getOwnerId, room.getOwnerId())
                .eq(MeetRoomParticipant::getStatus, PARTICIPANT_STATUS_ACTIVE));
        return activeParticipants == null ? 0 : activeParticipants;
    }

    private MeetGuestJoinOverviewVo toJoinOverview(MeetRoomSession room) {
        boolean guestJoinEnabled = ROOM_STATUS_ACTIVE.equals(room.getStatus())
                && ACCESS_LEVEL_PUBLIC.equals(normalizeAccessLevel(room.getAccessLevel()));
        return new MeetGuestJoinOverviewVo(
                String.valueOf(room.getId()),
                room.getRoomCode(),
                room.getTopic(),
                room.getJoinCode(),
                normalizeAccessLevel(room.getAccessLevel()),
                normalizeRoomStatus(room.getStatus()),
                guestJoinEnabled,
                guestJoinEnabled,
                Math.toIntExact(countActiveParticipants(room)),
                room.getMaxParticipants()
        );
    }

    private MeetGuestRequestVo toRequestVo(MeetGuestRequest request, MeetRoomSession room) {
        return new MeetGuestRequestVo(
                String.valueOf(request.getId()),
                String.valueOf(request.getRoomId()),
                room.getRoomCode(),
                normalizeRoomStatus(room.getStatus()),
                request.getDisplayName(),
                request.getAudioEnabled() == FLAG_ON,
                request.getVideoEnabled() == FLAG_ON,
                normalizeRequestStatus(request.getStatus()),
                request.getRequestToken(),
                request.getGuestSessionToken(),
                request.getParticipantId() == null ? null : String.valueOf(request.getParticipantId()),
                request.getRequestedAt(),
                request.getApprovedAt(),
                request.getRejectedAt()
        );
    }

    private MeetGuestSessionVo toGuestSession(MeetGuestRequest request, MeetRoomSession room, MeetRoomParticipant selfParticipant) {
        List<MeetGuestParticipantViewVo> participants = meetRoomParticipantMapper.selectList(new LambdaQueryWrapper<MeetRoomParticipant>()
                        .eq(MeetRoomParticipant::getRoomId, room.getId())
                        .eq(MeetRoomParticipant::getOwnerId, room.getOwnerId())
                        .eq(MeetRoomParticipant::getStatus, PARTICIPANT_STATUS_ACTIVE)
                        .orderByAsc(MeetRoomParticipant::getJoinedAt))
                .stream()
                .map(item -> toGuestParticipant(item, request.getParticipantId()))
                .toList();
        return new MeetGuestSessionVo(
                String.valueOf(room.getId()),
                room.getRoomCode(),
                room.getTopic(),
                resolveSessionStatus(request, room, selfParticipant),
                toGuestParticipant(selfParticipant, request.getParticipantId()),
                participants
        );
    }

    private MeetGuestParticipantViewVo toGuestParticipant(MeetRoomParticipant participant, Long selfParticipantId) {
        if (participant == null) {
            return null;
        }
        return new MeetGuestParticipantViewVo(
                String.valueOf(participant.getId()),
                participant.getDisplayName(),
                normalizeRole(participant.getRole()),
                normalizeParticipantStatus(participant.getStatus()),
                participant.getAudioEnabled() == FLAG_ON,
                participant.getVideoEnabled() == FLAG_ON,
                participant.getScreenSharing() == FLAG_ON,
                participant.getId().equals(selfParticipantId)
        );
    }

    private String resolveSessionStatus(MeetGuestRequest request, MeetRoomSession room, MeetRoomParticipant selfParticipant) {
        if (ROOM_STATUS_ENDED.equals(normalizeRoomStatus(room.getStatus()))) {
            return SESSION_STATUS_ROOM_ENDED;
        }
        String requestStatus = normalizeRequestStatus(request.getStatus());
        if (REQUEST_STATUS_PENDING.equals(requestStatus)) {
            return SESSION_STATUS_WAITING;
        }
        if (REQUEST_STATUS_REJECTED.equals(requestStatus)) {
            return SESSION_STATUS_REJECTED;
        }
        if (REQUEST_STATUS_LEFT.equals(requestStatus)) {
            return SESSION_STATUS_LEFT;
        }
        if (selfParticipant == null) {
            return SESSION_STATUS_WAITING;
        }
        String participantStatus = normalizeParticipantStatus(selfParticipant.getStatus());
        if (PARTICIPANT_STATUS_LEFT.equals(participantStatus)) {
            return SESSION_STATUS_LEFT;
        }
        if (PARTICIPANT_STATUS_REMOVED.equals(participantStatus)) {
            return SESSION_STATUS_REMOVED;
        }
        return SESSION_STATUS_ACTIVE;
    }

    private String requireJoinCode(String joinCode) {
        if (!StringUtils.hasText(joinCode)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet join link is invalid");
        }
        return joinCode.trim().toUpperCase(Locale.ROOT);
    }

    private String requireToken(String token, String message) {
        if (!StringUtils.hasText(token)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return token.trim().toUpperCase(Locale.ROOT);
    }

    private String requireDisplayName(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet guest displayName is required");
        }
        String safeDisplayName = displayName.trim();
        if (safeDisplayName.length() < 2 || safeDisplayName.length() > 64) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet guest displayName length is invalid");
        }
        return safeDisplayName;
    }

    private int toFlag(Boolean value) {
        return Boolean.FALSE.equals(value) ? FLAG_OFF : FLAG_ON;
    }

    private String normalizeAccessLevel(String accessLevel) {
        return StringUtils.hasText(accessLevel) ? accessLevel.trim().toUpperCase(Locale.ROOT) : "PRIVATE";
    }

    private String normalizeRoomStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : ROOM_STATUS_ENDED;
    }

    private String normalizeRequestStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : REQUEST_STATUS_PENDING;
    }

    private String normalizeParticipantStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : PARTICIPANT_STATUS_LEFT;
    }

    private String normalizeRole(String role) {
        return StringUtils.hasText(role) ? role.trim().toUpperCase(Locale.ROOT) : ROLE_PARTICIPANT;
    }

    private String generateToken(String prefix, int size) {
        StringBuilder builder = new StringBuilder(prefix).append('-');
        for (int index = 0; index < size; index++) {
            builder.append(TOKEN_CHARS.charAt(RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        return builder.toString();
    }
}
