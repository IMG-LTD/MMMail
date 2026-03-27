package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.MeetQualitySnapshotMapper;
import com.mmmail.server.mapper.MeetSignalEventMapper;
import com.mmmail.server.mapper.MeetRoomParticipantMapper;
import com.mmmail.server.mapper.MeetRoomSessionMapper;
import com.mmmail.server.model.entity.MeetQualitySnapshot;
import com.mmmail.server.model.entity.MeetRoomParticipant;
import com.mmmail.server.model.entity.MeetRoomSession;
import com.mmmail.server.model.entity.MeetSignalEvent;
import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.MeetParticipantVo;
import com.mmmail.server.model.vo.MeetPruneInactiveResultVo;
import com.mmmail.server.model.vo.MeetQualitySnapshotVo;
import com.mmmail.server.model.vo.MeetRoomVo;
import com.mmmail.server.model.vo.MeetSignalEventVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MeetService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ENDED = "ENDED";
    private static final String ROLE_HOST = "HOST";
    private static final String ROLE_CO_HOST = "CO_HOST";
    private static final String ROLE_PARTICIPANT = "PARTICIPANT";
    private static final String PARTICIPANT_STATUS_ACTIVE = "ACTIVE";
    private static final String PARTICIPANT_STATUS_LEFT = "LEFT";
    private static final String PARTICIPANT_STATUS_REMOVED = "REMOVED";
    private static final String SIGNAL_OFFER = "OFFER";
    private static final String SIGNAL_ANSWER = "ANSWER";
    private static final String SIGNAL_ICE = "ICE";
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_STREAM_TIMEOUT_SECONDS = 10;
    private static final int MIN_STREAM_TIMEOUT_SECONDS = 1;
    private static final int MAX_STREAM_TIMEOUT_SECONDS = 30;
    private static final long STREAM_POLL_INTERVAL_MILLIS = 250L;
    private static final int DEFAULT_MAX_PARTICIPANTS = 20;
    private static final int MIN_MAX_PARTICIPANTS = 2;
    private static final int MAX_MAX_PARTICIPANTS = 200;
    private static final int MAX_SIGNAL_PAYLOAD_LENGTH = 8 * 1024;
    private static final Set<String> SUPPORTED_ACCESS_LEVELS = Set.of("PRIVATE", "PUBLIC");
    private static final Set<String> SUPPORTED_ROLES = Set.of(ROLE_HOST, ROLE_CO_HOST, ROLE_PARTICIPANT);
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Comparator<MeetRoomParticipant> PARTICIPANT_ORDER = Comparator
            .comparingInt((MeetRoomParticipant item) -> switch (item.getRole()) {
                case ROLE_HOST -> 0;
                case ROLE_CO_HOST -> 1;
                default -> 2;
            })
            .thenComparing(MeetRoomParticipant::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder()));

    private final MeetRoomSessionMapper meetRoomSessionMapper;
    private final MeetRoomParticipantMapper meetRoomParticipantMapper;
    private final MeetSignalEventMapper meetSignalEventMapper;
    private final MeetQualitySnapshotMapper meetQualitySnapshotMapper;
    private final AuditService auditService;
    private final SuiteCollaborationService suiteCollaborationService;

    public MeetService(
            MeetRoomSessionMapper meetRoomSessionMapper,
            MeetRoomParticipantMapper meetRoomParticipantMapper,
            MeetSignalEventMapper meetSignalEventMapper,
            MeetQualitySnapshotMapper meetQualitySnapshotMapper,
            AuditService auditService,
            SuiteCollaborationService suiteCollaborationService
    ) {
        this.meetRoomSessionMapper = meetRoomSessionMapper;
        this.meetRoomParticipantMapper = meetRoomParticipantMapper;
        this.meetSignalEventMapper = meetSignalEventMapper;
        this.meetQualitySnapshotMapper = meetQualitySnapshotMapper;
        this.auditService = auditService;
        this.suiteCollaborationService = suiteCollaborationService;
    }

    public List<MeetRoomVo> list(Long userId, String status, Integer limit, String ipAddress) {
        int safeLimit = safeLimit(limit);
        String safeStatus = normalizeStatus(status);
        LambdaQueryWrapper<MeetRoomSession> query = new LambdaQueryWrapper<MeetRoomSession>()
                .eq(MeetRoomSession::getOwnerId, userId)
                .orderByDesc(MeetRoomSession::getStartedAt)
                .last("limit " + safeLimit);
        if (safeStatus != null) {
            query.eq(MeetRoomSession::getStatus, safeStatus);
        }
        List<MeetRoomVo> rooms = meetRoomSessionMapper.selectList(query)
                .stream()
                .map(room -> toRoomVo(room, LocalDateTime.now()))
                .toList();
        auditService.record(userId, "MEET_ROOM_LIST", "count=" + rooms.size() + ",status=" + safeStatus, ipAddress);
        return rooms;
    }

    public MeetRoomVo current(Long userId, String ipAddress) {
        MeetRoomSession current = loadCurrent(userId);
        auditService.record(userId, "MEET_ROOM_CURRENT", "hasCurrent=" + (current != null), ipAddress);
        return toRoomVo(current, LocalDateTime.now());
    }

    public List<MeetRoomVo> history(Long userId, Integer limit, String ipAddress) {
        int safeLimit = safeLimit(limit);
        List<MeetRoomVo> rooms = meetRoomSessionMapper.selectList(new LambdaQueryWrapper<MeetRoomSession>()
                        .eq(MeetRoomSession::getOwnerId, userId)
                        .eq(MeetRoomSession::getStatus, STATUS_ENDED)
                        .orderByDesc(MeetRoomSession::getStartedAt)
                        .last("limit " + safeLimit))
                .stream()
                .map(room -> toRoomVo(room, LocalDateTime.now()))
                .toList();
        auditService.record(userId, "MEET_ROOM_HISTORY", "count=" + rooms.size(), ipAddress);
        return rooms;
    }

    @Transactional
    public MeetRoomVo create(Long userId, String topic, String accessLevel, Integer maxParticipants, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        List<MeetRoomSession> activeRooms = meetRoomSessionMapper.selectList(new LambdaQueryWrapper<MeetRoomSession>()
                .eq(MeetRoomSession::getOwnerId, userId)
                .eq(MeetRoomSession::getStatus, STATUS_ACTIVE));
        for (MeetRoomSession active : activeRooms) {
            active.setStatus(STATUS_ENDED);
            active.setEndedAt(now);
            active.setDurationSeconds(Duration.between(active.getStartedAt(), now).toSeconds());
            active.setUpdatedAt(now);
            meetRoomSessionMapper.updateById(active);
            closeActiveParticipantsForRoom(active.getId(), active.getOwnerId(), now, PARTICIPANT_STATUS_LEFT);
        }

        MeetRoomSession room = new MeetRoomSession();
        room.setOwnerId(userId);
        room.setRoomCode(generateCode("MR", 8));
        room.setTopic(requireTopic(topic));
        room.setAccessLevel(normalizeAccessLevel(accessLevel));
        room.setMaxParticipants(normalizeMaxParticipants(maxParticipants));
        room.setHostUserId(userId);
        room.setJoinCode(generateCode("JC", 8));
        room.setStatus(STATUS_ACTIVE);
        room.setStartedAt(now);
        room.setEndedAt(null);
        room.setDurationSeconds(0L);
        room.setCreatedAt(now);
        room.setUpdatedAt(now);
        room.setDeleted(0);
        meetRoomSessionMapper.insert(room);

        MeetRoomParticipant hostParticipant = createParticipantRecord(
                room,
                userId,
                "Host-" + room.getRoomCode(),
                ROLE_HOST,
                now
        );
        meetRoomParticipantMapper.insert(hostParticipant);

        AuditEventVo event = auditService.recordEvent(
                userId,
                "MEET_ROOM_CREATE",
                "roomId=" + room.getId() + ",roomCode=" + room.getRoomCode() + ",access=" + room.getAccessLevel(),
                ipAddress
        );
        suiteCollaborationService.publishToUser(userId, event);
        return toRoomVo(room, now);
    }

    @Transactional
    public MeetRoomVo rotateJoinCode(Long userId, Long roomId, String ipAddress) {
        MeetRoomSession room = loadRoom(userId, roomId);
        if (!STATUS_ACTIVE.equals(room.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet room is not active");
        }
        LocalDateTime now = LocalDateTime.now();
        String oldCode = room.getJoinCode();
        String nextCode = generateCode("JC", 8);
        if (nextCode.equals(oldCode)) {
            nextCode = generateCode("JC", 8);
        }
        room.setJoinCode(nextCode);
        room.setUpdatedAt(now);
        meetRoomSessionMapper.updateById(room);
        auditService.record(userId, "MEET_ROOM_JOIN_CODE_ROTATE", "roomId=" + roomId, ipAddress);
        return toRoomVo(room, now);
    }

    @Transactional
    public MeetRoomVo end(Long userId, Long roomId, String ipAddress) {
        MeetRoomSession room = loadRoom(userId, roomId);
        LocalDateTime now = LocalDateTime.now();
        if (STATUS_ENDED.equals(room.getStatus())) {
            return toRoomVo(room, now);
        }
        if (!STATUS_ACTIVE.equals(room.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet room status is invalid");
        }
        room.setStatus(STATUS_ENDED);
        room.setEndedAt(now);
        room.setDurationSeconds(Duration.between(room.getStartedAt(), now).toSeconds());
        room.setUpdatedAt(now);
        meetRoomSessionMapper.updateById(room);
        closeActiveParticipantsForRoom(roomId, room.getOwnerId(), now, PARTICIPANT_STATUS_LEFT);
        AuditEventVo event = auditService.recordEvent(userId, "MEET_ROOM_END", "roomId=" + roomId, ipAddress);
        suiteCollaborationService.publishToUser(userId, event);
        return toRoomVo(room, now);
    }

    public List<MeetParticipantVo> participants(Long userId, Long roomId, String ipAddress) {
        MeetRoomSession room = loadRoomById(roomId);
        String actorRole = resolveActorRole(userId, room);
        if (actorRole == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No access to meet room participants");
        }
        List<MeetParticipantVo> participants = meetRoomParticipantMapper.selectList(new LambdaQueryWrapper<MeetRoomParticipant>()
                        .eq(MeetRoomParticipant::getRoomId, roomId)
                        .eq(MeetRoomParticipant::getOwnerId, room.getOwnerId())
                        .eq(MeetRoomParticipant::getStatus, PARTICIPANT_STATUS_ACTIVE))
                .stream()
                .sorted(PARTICIPANT_ORDER)
                .map(item -> toParticipantVo(item, userId, actorRole, room.getOwnerId()))
                .toList();
        auditService.record(userId, "MEET_PARTICIPANT_LIST", "roomId=" + roomId + ",count=" + participants.size(), ipAddress);
        return participants;
    }

    @Transactional
    public MeetParticipantVo join(Long userId, Long roomId, String displayName, String ipAddress) {
        MeetRoomSession room = requireActiveRoomById(roomId);
        LocalDateTime now = LocalDateTime.now();
        String safeName = requireDisplayName(displayName);

        MeetRoomParticipant existing = loadActiveParticipantByUser(roomId, room.getOwnerId(), userId);
        if (existing != null) {
            existing.setDisplayName(safeName);
            existing.setLastHeartbeatAt(now);
            existing.setUpdatedAt(now);
            meetRoomParticipantMapper.updateById(existing);
            AuditEventVo event = auditService.recordEvent(
                    userId,
                    "MEET_PARTICIPANT_JOIN",
                    "roomId=" + roomId + ",participantId=" + existing.getId() + ",rejoin=true",
                    ipAddress
            );
            suiteCollaborationService.publishToUser(userId, event);
            return toParticipantVo(existing, userId, resolveActorRole(userId, room), room.getOwnerId());
        }

        Long activeCount = meetRoomParticipantMapper.selectCount(new LambdaQueryWrapper<MeetRoomParticipant>()
                .eq(MeetRoomParticipant::getRoomId, roomId)
                .eq(MeetRoomParticipant::getOwnerId, room.getOwnerId())
                .eq(MeetRoomParticipant::getStatus, PARTICIPANT_STATUS_ACTIVE));
        if (activeCount != null && activeCount >= room.getMaxParticipants()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet room is full");
        }

        String role = userId.equals(room.getHostUserId()) ? ROLE_HOST : ROLE_PARTICIPANT;
        MeetRoomParticipant participant = createParticipantRecord(room, userId, safeName, role, now);
        meetRoomParticipantMapper.insert(participant);
        AuditEventVo event = auditService.recordEvent(
                userId,
                "MEET_PARTICIPANT_JOIN",
                "roomId=" + roomId + ",participantId=" + participant.getId() + ",role=" + role,
                ipAddress
        );
        suiteCollaborationService.publishToUser(userId, event);
        return toParticipantVo(participant, userId, role, room.getOwnerId());
    }

    @Transactional
    public MeetParticipantVo leave(Long userId, Long roomId, Long participantId, String ipAddress) {
        MeetRoomSession room = loadRoomById(roomId);
        MeetRoomParticipant participant = loadActiveParticipant(roomId, room.getOwnerId(), participantId);
        boolean isOwner = userId.equals(room.getOwnerId());
        if (!isOwner && !userId.equals(participant.getUserId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No permission to leave this participant");
        }
        LocalDateTime now = LocalDateTime.now();

        if (ROLE_HOST.equals(participant.getRole())) {
            MeetRoomParticipant replacement = pickReplacementHost(roomId, room.getOwnerId(), participant.getId());
            if (replacement != null) {
                replacement.setRole(ROLE_HOST);
                replacement.setUpdatedAt(now);
                meetRoomParticipantMapper.updateById(replacement);
                room.setHostUserId(replacement.getUserId());
                room.setUpdatedAt(now);
                meetRoomSessionMapper.updateById(room);
                participant.setRole(ROLE_PARTICIPANT);
            }
        }

        participant.setStatus(PARTICIPANT_STATUS_LEFT);
        participant.setLeftAt(now);
        participant.setUpdatedAt(now);
        meetRoomParticipantMapper.updateById(participant);
        auditService.record(userId, "MEET_PARTICIPANT_LEAVE", "roomId=" + roomId + ",participantId=" + participantId, ipAddress);
        return toParticipantVo(participant, userId, resolveActorRole(userId, room), room.getOwnerId());
    }

    @Transactional
    public MeetParticipantVo heartbeat(Long userId, Long roomId, Long participantId, String ipAddress) {
        MeetRoomSession room = requireActiveRoomById(roomId);
        MeetRoomParticipant participant = loadActiveParticipant(roomId, room.getOwnerId(), participantId);
        if (!userId.equals(participant.getUserId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No permission to heartbeat this participant");
        }
        LocalDateTime now = LocalDateTime.now();
        participant.setLastHeartbeatAt(now);
        participant.setUpdatedAt(now);
        meetRoomParticipantMapper.updateById(participant);
        auditService.record(userId, "MEET_PARTICIPANT_HEARTBEAT", "roomId=" + roomId + ",participantId=" + participantId, ipAddress);
        return toParticipantVo(participant, userId, resolveActorRole(userId, room), room.getOwnerId());
    }

    @Transactional
    public MeetParticipantVo updateMedia(
            Long userId,
            Long roomId,
            Long participantId,
            Boolean audioEnabled,
            Boolean videoEnabled,
            Boolean screenSharing,
            String ipAddress
    ) {
        MeetRoomSession room = requireActiveRoomById(roomId);
        MeetRoomParticipant participant = loadActiveParticipant(roomId, room.getOwnerId(), participantId);
        if (!userId.equals(participant.getUserId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No permission to update this participant media state");
        }
        LocalDateTime now = LocalDateTime.now();
        participant.setAudioEnabled(toFlag(audioEnabled));
        participant.setVideoEnabled(toFlag(videoEnabled));
        participant.setScreenSharing(toFlag(screenSharing));
        participant.setUpdatedAt(now);
        meetRoomParticipantMapper.updateById(participant);
        auditService.record(
                userId,
                "MEET_PARTICIPANT_MEDIA_UPDATE",
                "roomId=" + roomId + ",participantId=" + participantId
                        + ",audio=" + participant.getAudioEnabled()
                        + ",video=" + participant.getVideoEnabled()
                        + ",screen=" + participant.getScreenSharing(),
                ipAddress
        );
        return toParticipantVo(participant, userId, resolveActorRole(userId, room), room.getOwnerId());
    }

    @Transactional
    public MeetSignalEventVo sendOffer(
            Long userId,
            Long roomId,
            Long fromParticipantId,
            Long toParticipantId,
            String payload,
            String ipAddress
    ) {
        return sendSignal(userId, roomId, fromParticipantId, toParticipantId, payload, SIGNAL_OFFER, ipAddress);
    }

    @Transactional
    public MeetSignalEventVo sendAnswer(
            Long userId,
            Long roomId,
            Long fromParticipantId,
            Long toParticipantId,
            String payload,
            String ipAddress
    ) {
        return sendSignal(userId, roomId, fromParticipantId, toParticipantId, payload, SIGNAL_ANSWER, ipAddress);
    }

    @Transactional
    public MeetSignalEventVo sendIce(
            Long userId,
            Long roomId,
            Long fromParticipantId,
            Long toParticipantId,
            String payload,
            String ipAddress
    ) {
        return sendSignal(userId, roomId, fromParticipantId, toParticipantId, payload, SIGNAL_ICE, ipAddress);
    }

    public List<MeetSignalEventVo> listSignals(
            Long userId,
            Long roomId,
            Long afterEventSeq,
            Integer limit,
            String ipAddress
    ) {
        MeetRoomSession room = requireActiveRoomById(roomId);
        requireSignalAccess(userId, room);
        long safeAfterEventSeq = afterEventSeq == null ? 0 : Math.max(0, afterEventSeq);
        int safeLimit = safeLimit(limit);
        List<MeetSignalEventVo> events = querySignalEvents(roomId, room.getOwnerId(), safeAfterEventSeq, safeLimit);
        auditService.record(
                userId,
                "MEET_SIGNAL_LIST",
                "roomId=" + roomId + ",afterEventSeq=" + safeAfterEventSeq + ",count=" + events.size(),
                ipAddress
        );
        return events;
    }

    public List<MeetSignalEventVo> streamSignals(
            Long userId,
            Long roomId,
            Long afterEventSeq,
            Integer timeoutSeconds,
            Integer limit,
            String ipAddress
    ) {
        MeetRoomSession room = requireActiveRoomById(roomId);
        requireSignalAccess(userId, room);
        long safeAfterEventSeq = afterEventSeq == null ? 0 : Math.max(0, afterEventSeq);
        int safeLimit = safeLimit(limit);
        int safeTimeoutSeconds = normalizeStreamTimeoutSeconds(timeoutSeconds);
        long deadline = System.currentTimeMillis() + safeTimeoutSeconds * 1000L;

        List<MeetSignalEventVo> events;
        do {
            events = querySignalEvents(roomId, room.getOwnerId(), safeAfterEventSeq, safeLimit);
            if (!events.isEmpty() || System.currentTimeMillis() >= deadline) {
                break;
            }
            try {
                Thread.sleep(STREAM_POLL_INTERVAL_MILLIS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                break;
            }
        } while (true);

        auditService.record(
                userId,
                "MEET_SIGNAL_STREAM",
                "roomId=" + roomId
                        + ",afterEventSeq=" + safeAfterEventSeq
                        + ",timeoutSeconds=" + safeTimeoutSeconds
                        + ",count=" + events.size(),
                ipAddress
        );
        return events;
    }

    @Transactional
    public MeetQualitySnapshotVo reportQuality(
            Long userId,
            Long roomId,
            Long participantId,
            Integer jitterMs,
            Integer packetLossPercent,
            Integer roundTripMs,
            String ipAddress
    ) {
        MeetRoomSession room = requireActiveRoomById(roomId);
        MeetRoomParticipant participant = loadActiveParticipant(roomId, room.getOwnerId(), participantId);
        if (!userId.equals(participant.getUserId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No permission to report quality for this participant");
        }

        int safeJitterMs = normalizeQualityMetric(jitterMs, 0, 5000, "Meet quality jitterMs is invalid");
        int safePacketLossPercent = normalizeQualityMetric(packetLossPercent, 0, 100, "Meet quality packetLossPercent is invalid");
        int safeRoundTripMs = normalizeQualityMetric(roundTripMs, 0, 10000, "Meet quality roundTripMs is invalid");
        int qualityScore = calculateQualityScore(safeJitterMs, safePacketLossPercent, safeRoundTripMs);
        LocalDateTime now = LocalDateTime.now();

        MeetQualitySnapshot snapshot = new MeetQualitySnapshot();
        snapshot.setRoomId(roomId);
        snapshot.setOwnerId(room.getOwnerId());
        snapshot.setParticipantId(participantId);
        snapshot.setJitterMs(safeJitterMs);
        snapshot.setPacketLossPercent(safePacketLossPercent);
        snapshot.setRoundTripMs(safeRoundTripMs);
        snapshot.setQualityScore(qualityScore);
        snapshot.setCreatedAt(now);
        snapshot.setUpdatedAt(now);
        snapshot.setDeleted(0);
        meetQualitySnapshotMapper.insert(snapshot);

        auditService.record(
                userId,
                "MEET_QUALITY_REPORT",
                "roomId=" + roomId + ",participantId=" + participantId + ",qualityScore=" + qualityScore,
                ipAddress
        );
        return toQualitySnapshotVo(snapshot);
    }

    public List<MeetQualitySnapshotVo> listQualitySnapshots(
            Long userId,
            Long roomId,
            Integer limit,
            String ipAddress
    ) {
        MeetRoomSession room = requireActiveRoomById(roomId);
        requireSignalAccess(userId, room);
        int safeLimit = safeLimit(limit);
        List<MeetQualitySnapshotVo> snapshots = meetQualitySnapshotMapper.selectList(new LambdaQueryWrapper<MeetQualitySnapshot>()
                        .eq(MeetQualitySnapshot::getRoomId, roomId)
                        .eq(MeetQualitySnapshot::getOwnerId, room.getOwnerId())
                        .orderByDesc(MeetQualitySnapshot::getCreatedAt)
                        .last("limit " + safeLimit))
                .stream()
                .map(this::toQualitySnapshotVo)
                .toList();

        auditService.record(
                userId,
                "MEET_QUALITY_LIST",
                "roomId=" + roomId + ",count=" + snapshots.size(),
                ipAddress
        );
        return snapshots;
    }

    @Transactional
    public MeetParticipantVo updateRole(Long userId, Long roomId, Long participantId, String role, String ipAddress) {
        MeetRoomSession room = requireActiveRoomById(roomId);
        String actorRole = requireManagerRole(userId, room);
        String targetRole = normalizeManagedRole(role);
        MeetRoomParticipant participant = loadActiveParticipant(roomId, room.getOwnerId(), participantId);
        if (isGuestUserId(participant.getUserId()) && !ROLE_PARTICIPANT.equals(targetRole)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Guest participant role cannot be elevated");
        }
        if (ROLE_HOST.equals(participant.getRole())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Host role can only be changed by transfer");
        }
        if (ROLE_CO_HOST.equals(actorRole) && ROLE_CO_HOST.equals(participant.getRole())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No permission to update co-host role");
        }
        if (participant.getRole().equals(targetRole)) {
            return toParticipantVo(participant, userId, actorRole, room.getOwnerId());
        }
        participant.setRole(targetRole);
        participant.setUpdatedAt(LocalDateTime.now());
        meetRoomParticipantMapper.updateById(participant);
        auditService.record(userId, "MEET_PARTICIPANT_ROLE_UPDATE", "roomId=" + roomId + ",participantId=" + participantId + ",role=" + targetRole, ipAddress);
        return toParticipantVo(participant, userId, actorRole, room.getOwnerId());
    }

    @Transactional
    public MeetParticipantVo remove(Long userId, Long roomId, Long participantId, String ipAddress) {
        MeetRoomSession room = requireActiveRoomById(roomId);
        String actorRole = requireManagerRole(userId, room);
        MeetRoomParticipant participant = loadActiveParticipant(roomId, room.getOwnerId(), participantId);
        if (userId.equals(participant.getUserId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Self removal is not allowed, use leave");
        }
        if (ROLE_HOST.equals(participant.getRole())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Host cannot be removed directly");
        }
        if (ROLE_CO_HOST.equals(actorRole) && ROLE_CO_HOST.equals(participant.getRole())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Co-host cannot remove another co-host");
        }
        LocalDateTime now = LocalDateTime.now();
        participant.setStatus(PARTICIPANT_STATUS_REMOVED);
        participant.setLeftAt(now);
        participant.setUpdatedAt(now);
        meetRoomParticipantMapper.updateById(participant);
        AuditEventVo event = auditService.recordEvent(
                userId,
                "MEET_PARTICIPANT_REMOVE",
                "roomId=" + roomId + ",participantId=" + participantId,
                ipAddress
        );
        suiteCollaborationService.publishToUser(userId, event);
        return toParticipantVo(participant, userId, actorRole, room.getOwnerId());
    }

    @Transactional
    public MeetPruneInactiveResultVo pruneInactiveParticipants(
            Long userId,
            Long roomId,
            Integer inactiveSeconds,
            String ipAddress
    ) {
        MeetRoomSession room = requireActiveRoomById(roomId);
        requireManagerRole(userId, room);
        int safeInactiveSeconds = normalizeInactiveSeconds(inactiveSeconds);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusSeconds(safeInactiveSeconds);

        List<MeetRoomParticipant> activeParticipants = meetRoomParticipantMapper.selectList(new LambdaQueryWrapper<MeetRoomParticipant>()
                .eq(MeetRoomParticipant::getRoomId, roomId)
                .eq(MeetRoomParticipant::getOwnerId, room.getOwnerId())
                .eq(MeetRoomParticipant::getStatus, PARTICIPANT_STATUS_ACTIVE));

        List<String> removedParticipantIds = new ArrayList<>();
        for (MeetRoomParticipant participant : activeParticipants) {
            if (ROLE_HOST.equals(participant.getRole())) {
                continue;
            }
            if (participant.getLastHeartbeatAt() != null && participant.getLastHeartbeatAt().isAfter(threshold)) {
                continue;
            }
            participant.setStatus(PARTICIPANT_STATUS_REMOVED);
            participant.setLeftAt(now);
            participant.setUpdatedAt(now);
            meetRoomParticipantMapper.updateById(participant);
            removedParticipantIds.add(String.valueOf(participant.getId()));
        }

        auditService.record(
                userId,
                "MEET_PARTICIPANT_PRUNE_INACTIVE",
                "roomId=" + roomId + ",inactiveSeconds=" + safeInactiveSeconds + ",removed=" + removedParticipantIds.size(),
                ipAddress
        );
        return new MeetPruneInactiveResultVo(
                String.valueOf(roomId),
                safeInactiveSeconds,
                removedParticipantIds.size(),
                removedParticipantIds,
                now
        );
    }

    @Transactional
    public MeetParticipantVo transferHost(Long userId, Long roomId, Long targetParticipantId, String ipAddress) {
        MeetRoomSession room = requireActiveRoomById(roomId);
        String actorRole = resolveActorRole(userId, room);
        boolean isOwner = userId.equals(room.getOwnerId());
        if (!(isOwner || ROLE_HOST.equals(actorRole))) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No permission to transfer host");
        }

        MeetRoomParticipant target = loadActiveParticipant(roomId, room.getOwnerId(), targetParticipantId);
        if (isGuestUserId(target.getUserId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Guest participant cannot become host");
        }
        if (ROLE_HOST.equals(target.getRole())) {
            return toParticipantVo(target, userId, actorRole, room.getOwnerId());
        }

        LocalDateTime now = LocalDateTime.now();
        MeetRoomParticipant currentHost = loadActiveParticipantByUser(roomId, room.getOwnerId(), room.getHostUserId());
        if (currentHost != null && !currentHost.getId().equals(target.getId())) {
            currentHost.setRole(ROLE_PARTICIPANT);
            currentHost.setUpdatedAt(now);
            meetRoomParticipantMapper.updateById(currentHost);
        }

        target.setRole(ROLE_HOST);
        target.setUpdatedAt(now);
        meetRoomParticipantMapper.updateById(target);

        room.setHostUserId(target.getUserId());
        room.setUpdatedAt(now);
        meetRoomSessionMapper.updateById(room);

        AuditEventVo event = auditService.recordEvent(
                userId,
                "MEET_HOST_TRANSFER",
                "roomId=" + roomId + ",targetParticipantId=" + targetParticipantId,
                ipAddress
        );
        suiteCollaborationService.publishToUser(userId, event);
        return toParticipantVo(target, userId, resolveActorRole(userId, room), room.getOwnerId());
    }

    private MeetSignalEventVo sendSignal(
            Long userId,
            Long roomId,
            Long fromParticipantId,
            Long toParticipantId,
            String payload,
            String signalType,
            String ipAddress
    ) {
        MeetRoomSession room = requireActiveRoomById(roomId);
        String actorRole = resolveActorRole(userId, room);
        if (actorRole == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No permission to send meet signals");
        }
        MeetRoomParticipant fromParticipant = loadActiveParticipant(roomId, room.getOwnerId(), fromParticipantId);
        if (!userId.equals(fromParticipant.getUserId())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No permission to send signal from this participant");
        }
        if (toParticipantId != null) {
            loadActiveParticipant(roomId, room.getOwnerId(), toParticipantId);
        }
        String safePayload = requireSignalPayload(payload);
        LocalDateTime now = LocalDateTime.now();

        MeetSignalEvent signalEvent = new MeetSignalEvent();
        signalEvent.setRoomId(roomId);
        signalEvent.setOwnerId(room.getOwnerId());
        signalEvent.setEventSeq(nextSignalSeq(roomId, room.getOwnerId()));
        signalEvent.setSignalType(signalType);
        signalEvent.setFromParticipantId(fromParticipantId);
        signalEvent.setToParticipantId(toParticipantId);
        signalEvent.setPayload(safePayload);
        signalEvent.setCreatedAt(now);
        signalEvent.setUpdatedAt(now);
        signalEvent.setDeleted(0);
        meetSignalEventMapper.insert(signalEvent);

        auditService.record(
                userId,
                "MEET_SIGNAL_" + signalType,
                "roomId=" + roomId + ",fromParticipantId=" + fromParticipantId + ",toParticipantId=" + toParticipantId,
                ipAddress
        );
        return toSignalVo(signalEvent);
    }

    private synchronized long nextSignalSeq(Long roomId, Long ownerId) {
        MeetSignalEvent last = meetSignalEventMapper.selectOne(new LambdaQueryWrapper<MeetSignalEvent>()
                .eq(MeetSignalEvent::getRoomId, roomId)
                .eq(MeetSignalEvent::getOwnerId, ownerId)
                .orderByDesc(MeetSignalEvent::getEventSeq)
                .last("limit 1"));
        if (last == null || last.getEventSeq() == null) {
            return 1L;
        }
        return last.getEventSeq() + 1;
    }

    private void requireSignalAccess(Long userId, MeetRoomSession room) {
        String actorRole = resolveActorRole(userId, room);
        if (actorRole == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No permission to access meet signals");
        }
    }

    private List<MeetSignalEventVo> querySignalEvents(Long roomId, Long ownerId, long afterEventSeq, int limit) {
        return meetSignalEventMapper.selectList(new LambdaQueryWrapper<MeetSignalEvent>()
                        .eq(MeetSignalEvent::getRoomId, roomId)
                        .eq(MeetSignalEvent::getOwnerId, ownerId)
                        .gt(MeetSignalEvent::getEventSeq, afterEventSeq)
                        .orderByAsc(MeetSignalEvent::getEventSeq)
                        .last("limit " + limit))
                .stream()
                .map(this::toSignalVo)
                .toList();
    }

    private int normalizeQualityMetric(Integer value, int min, int max, String message) {
        if (value == null || value < min || value > max) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return value;
    }

    private int calculateQualityScore(int jitterMs, int packetLossPercent, int roundTripMs) {
        int score = 100;
        score -= Math.min(40, packetLossPercent * 2);
        score -= Math.min(30, jitterMs / 20);
        score -= Math.min(30, roundTripMs / 40);
        return Math.max(1, score);
    }

    private MeetRoomSession requireActiveRoomById(Long roomId) {
        MeetRoomSession room = loadRoomById(roomId);
        if (!STATUS_ACTIVE.equals(room.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet room is not active");
        }
        return room;
    }

    private MeetRoomSession loadCurrent(Long userId) {
        return meetRoomSessionMapper.selectOne(new LambdaQueryWrapper<MeetRoomSession>()
                .eq(MeetRoomSession::getOwnerId, userId)
                .eq(MeetRoomSession::getStatus, STATUS_ACTIVE)
                .orderByDesc(MeetRoomSession::getStartedAt)
                .last("limit 1"));
    }

    private MeetRoomSession loadRoomById(Long roomId) {
        MeetRoomSession room = meetRoomSessionMapper.selectById(roomId);
        if (room == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet room is not found");
        }
        return room;
    }

    private MeetRoomSession loadRoom(Long userId, Long roomId) {
        MeetRoomSession room = meetRoomSessionMapper.selectOne(new LambdaQueryWrapper<MeetRoomSession>()
                .eq(MeetRoomSession::getId, roomId)
                .eq(MeetRoomSession::getOwnerId, userId));
        if (room == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet room is not found");
        }
        return room;
    }

    private String resolveActorRole(Long userId, MeetRoomSession room) {
        if (userId.equals(room.getOwnerId())) {
            return ROLE_HOST;
        }
        MeetRoomParticipant participant = loadActiveParticipantByUser(room.getId(), room.getOwnerId(), userId);
        return participant == null ? null : participant.getRole();
    }

    private String requireManagerRole(Long userId, MeetRoomSession room) {
        String actorRole = resolveActorRole(userId, room);
        boolean canManage = ROLE_HOST.equals(actorRole) || ROLE_CO_HOST.equals(actorRole);
        if (!canManage) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No permission to manage participants");
        }
        return actorRole;
    }

    private MeetRoomParticipant loadActiveParticipant(Long roomId, Long ownerId, Long participantId) {
        MeetRoomParticipant participant = meetRoomParticipantMapper.selectOne(new LambdaQueryWrapper<MeetRoomParticipant>()
                .eq(MeetRoomParticipant::getId, participantId)
                .eq(MeetRoomParticipant::getRoomId, roomId)
                .eq(MeetRoomParticipant::getOwnerId, ownerId)
                .eq(MeetRoomParticipant::getStatus, PARTICIPANT_STATUS_ACTIVE));
        if (participant == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet participant is not found");
        }
        return participant;
    }

    private MeetRoomParticipant loadActiveParticipantByUser(Long roomId, Long ownerId, Long userId) {
        return meetRoomParticipantMapper.selectOne(new LambdaQueryWrapper<MeetRoomParticipant>()
                .eq(MeetRoomParticipant::getRoomId, roomId)
                .eq(MeetRoomParticipant::getOwnerId, ownerId)
                .eq(MeetRoomParticipant::getUserId, userId)
                .eq(MeetRoomParticipant::getStatus, PARTICIPANT_STATUS_ACTIVE)
                .orderByDesc(MeetRoomParticipant::getJoinedAt)
                .last("limit 1"));
    }

    private MeetRoomParticipant pickReplacementHost(Long roomId, Long ownerId, Long excludedParticipantId) {
        return meetRoomParticipantMapper.selectOne(new LambdaQueryWrapper<MeetRoomParticipant>()
                .eq(MeetRoomParticipant::getRoomId, roomId)
                .eq(MeetRoomParticipant::getOwnerId, ownerId)
                .eq(MeetRoomParticipant::getStatus, PARTICIPANT_STATUS_ACTIVE)
                .gt(MeetRoomParticipant::getUserId, 0L)
                .ne(MeetRoomParticipant::getId, excludedParticipantId)
                .orderByAsc(MeetRoomParticipant::getJoinedAt)
                .last("limit 1"));
    }

    private boolean isGuestUserId(Long userId) {
        return userId != null && userId < 0;
    }

    private void closeActiveParticipantsForRoom(Long roomId, Long ownerId, LocalDateTime now, String status) {
        List<MeetRoomParticipant> activeParticipants = meetRoomParticipantMapper.selectList(new LambdaQueryWrapper<MeetRoomParticipant>()
                .eq(MeetRoomParticipant::getRoomId, roomId)
                .eq(MeetRoomParticipant::getOwnerId, ownerId)
                .eq(MeetRoomParticipant::getStatus, PARTICIPANT_STATUS_ACTIVE));
        for (MeetRoomParticipant participant : activeParticipants) {
            participant.setStatus(status);
            participant.setLeftAt(now);
            participant.setUpdatedAt(now);
            meetRoomParticipantMapper.updateById(participant);
        }
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String safeStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!STATUS_ACTIVE.equals(safeStatus) && !STATUS_ENDED.equals(safeStatus)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet room status is invalid");
        }
        return safeStatus;
    }

    private String requireTopic(String topic) {
        if (!StringUtils.hasText(topic)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet room topic is required");
        }
        String safeTopic = topic.trim();
        if (safeTopic.length() < 3 || safeTopic.length() > 128) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet room topic length is invalid");
        }
        return safeTopic;
    }

    private String requireDisplayName(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Participant displayName is required");
        }
        String safeDisplayName = displayName.trim();
        if (safeDisplayName.length() < 2 || safeDisplayName.length() > 64) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Participant displayName length is invalid");
        }
        return safeDisplayName;
    }

    private String requireSignalPayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet signal payload is required");
        }
        String safePayload = payload.trim();
        if (safePayload.length() > MAX_SIGNAL_PAYLOAD_LENGTH) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet signal payload is too long");
        }
        return safePayload;
    }

    private String normalizeAccessLevel(String accessLevel) {
        if (!StringUtils.hasText(accessLevel)) {
            return "PRIVATE";
        }
        String safeAccessLevel = accessLevel.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_ACCESS_LEVELS.contains(safeAccessLevel)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet access level is invalid");
        }
        return safeAccessLevel;
    }

    private int normalizeMaxParticipants(Integer maxParticipants) {
        if (maxParticipants == null) {
            return DEFAULT_MAX_PARTICIPANTS;
        }
        if (maxParticipants < MIN_MAX_PARTICIPANTS) {
            return MIN_MAX_PARTICIPANTS;
        }
        if (maxParticipants > MAX_MAX_PARTICIPANTS) {
            return MAX_MAX_PARTICIPANTS;
        }
        return maxParticipants;
    }

    private int normalizeInactiveSeconds(Integer inactiveSeconds) {
        if (inactiveSeconds == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet inactiveSeconds is required");
        }
        if (inactiveSeconds < 30 || inactiveSeconds > 86400) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Meet inactiveSeconds is invalid");
        }
        return inactiveSeconds;
    }

    private int safeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        if (limit > MAX_LIMIT) {
            return MAX_LIMIT;
        }
        return limit;
    }

    private int normalizeStreamTimeoutSeconds(Integer timeoutSeconds) {
        if (timeoutSeconds == null) {
            return DEFAULT_STREAM_TIMEOUT_SECONDS;
        }
        if (timeoutSeconds < MIN_STREAM_TIMEOUT_SECONDS) {
            return MIN_STREAM_TIMEOUT_SECONDS;
        }
        if (timeoutSeconds > MAX_STREAM_TIMEOUT_SECONDS) {
            return MAX_STREAM_TIMEOUT_SECONDS;
        }
        return timeoutSeconds;
    }

    private String normalizeManagedRole(String role) {
        if (!StringUtils.hasText(role)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Participant role is required");
        }
        String safeRole = role.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_ROLES.contains(safeRole)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Participant role is invalid");
        }
        if (ROLE_HOST.equals(safeRole)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Host role can only be updated via transfer");
        }
        return safeRole;
    }

    private String generateCode(String prefix, int size) {
        StringBuilder builder = new StringBuilder(prefix.length() + size + 1);
        builder.append(prefix).append('-');
        for (int i = 0; i < size; i++) {
            builder.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return builder.toString();
    }

    private int toFlag(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private boolean isEnabled(Integer value) {
        return value != null && value == 1;
    }

    private MeetRoomParticipant createParticipantRecord(
            MeetRoomSession room,
            Long userId,
            String displayName,
            String role,
            LocalDateTime now
    ) {
        MeetRoomParticipant participant = new MeetRoomParticipant();
        participant.setRoomId(room.getId());
        participant.setOwnerId(room.getOwnerId());
        participant.setUserId(userId);
        participant.setDisplayName(displayName);
        participant.setRole(role);
        participant.setStatus(PARTICIPANT_STATUS_ACTIVE);
        participant.setAudioEnabled(1);
        participant.setVideoEnabled(1);
        participant.setScreenSharing(0);
        participant.setJoinedAt(now);
        participant.setLeftAt(null);
        participant.setLastHeartbeatAt(now);
        participant.setCreatedAt(now);
        participant.setUpdatedAt(now);
        participant.setDeleted(0);
        return participant;
    }

    private MeetParticipantVo toParticipantVo(
            MeetRoomParticipant participant,
            Long actorUserId,
            String actorRole,
            Long roomOwnerId
    ) {
        boolean isOwner = actorUserId.equals(roomOwnerId);
        boolean canManage = isOwner || ROLE_HOST.equals(actorRole) || ROLE_CO_HOST.equals(actorRole);
        boolean canTransfer = isOwner || ROLE_HOST.equals(actorRole);
        return new MeetParticipantVo(
                String.valueOf(participant.getId()),
                String.valueOf(participant.getRoomId()),
                String.valueOf(participant.getUserId()),
                participant.getDisplayName(),
                participant.getRole(),
                participant.getStatus(),
                isEnabled(participant.getAudioEnabled()),
                isEnabled(participant.getVideoEnabled()),
                isEnabled(participant.getScreenSharing()),
                participant.getJoinedAt(),
                participant.getLeftAt(),
                participant.getLastHeartbeatAt(),
                actorUserId.equals(participant.getUserId()),
                canManage,
                canTransfer
        );
    }

    private MeetSignalEventVo toSignalVo(MeetSignalEvent event) {
        return new MeetSignalEventVo(
                String.valueOf(event.getEventSeq()),
                String.valueOf(event.getRoomId()),
                event.getSignalType(),
                String.valueOf(event.getFromParticipantId()),
                event.getToParticipantId() == null ? null : String.valueOf(event.getToParticipantId()),
                event.getPayload(),
                event.getCreatedAt()
        );
    }

    private MeetQualitySnapshotVo toQualitySnapshotVo(MeetQualitySnapshot snapshot) {
        return new MeetQualitySnapshotVo(
                String.valueOf(snapshot.getId()),
                String.valueOf(snapshot.getRoomId()),
                String.valueOf(snapshot.getParticipantId()),
                snapshot.getJitterMs() == null ? 0 : snapshot.getJitterMs(),
                snapshot.getPacketLossPercent() == null ? 0 : snapshot.getPacketLossPercent(),
                snapshot.getRoundTripMs() == null ? 0 : snapshot.getRoundTripMs(),
                snapshot.getQualityScore() == null ? 0 : snapshot.getQualityScore(),
                snapshot.getCreatedAt()
        );
    }

    private MeetRoomVo toRoomVo(MeetRoomSession room, LocalDateTime now) {
        if (room == null) {
            return null;
        }
        long durationSeconds;
        if (STATUS_ACTIVE.equals(room.getStatus())) {
            durationSeconds = Math.max(0, Duration.between(room.getStartedAt(), now).toSeconds());
        } else {
            durationSeconds = room.getDurationSeconds() == null ? 0 : Math.max(0, room.getDurationSeconds());
        }
        String visibleJoinCode = STATUS_ACTIVE.equals(room.getStatus()) ? room.getJoinCode() : null;
        return new MeetRoomVo(
                String.valueOf(room.getId()),
                room.getRoomCode(),
                room.getTopic(),
                room.getAccessLevel(),
                room.getMaxParticipants() == null ? DEFAULT_MAX_PARTICIPANTS : room.getMaxParticipants(),
                visibleJoinCode,
                room.getStatus(),
                room.getStartedAt(),
                room.getEndedAt(),
                durationSeconds
        );
    }
}
