package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.CollabSnapshotMapper;
import com.mmmail.server.mapper.CollabUpdateMapper;
import com.mmmail.server.model.dto.CollabSnapshotRequest;
import com.mmmail.server.model.entity.CollabSnapshot;
import com.mmmail.server.model.entity.CollabUpdate;
import com.mmmail.server.model.vo.CollabAwarenessVo;
import com.mmmail.server.model.vo.CollabSnapshotVo;
import com.mmmail.server.model.vo.DocsNotePresenceVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class CollabCrdtService {

    private static final String RESOURCE_DOCS = "docs";
    private static final int MAX_UPDATE_BYTES = 262_144;

    private final CollabSnapshotMapper collabSnapshotMapper;
    private final CollabUpdateMapper collabUpdateMapper;
    private final DocsAccessService docsAccessService;
    private final DocsCollaborationService docsCollaborationService;

    public CollabCrdtService(
            CollabSnapshotMapper collabSnapshotMapper,
            CollabUpdateMapper collabUpdateMapper,
            DocsAccessService docsAccessService,
            DocsCollaborationService docsCollaborationService
    ) {
        this.collabSnapshotMapper = collabSnapshotMapper;
        this.collabUpdateMapper = collabUpdateMapper;
        this.docsAccessService = docsAccessService;
        this.docsCollaborationService = docsCollaborationService;
    }

    public CollabSnapshotVo getSnapshot(Long userId, String resourceType, String resourceId) {
        requireReadable(userId, resourceType, resourceId);
        CollabSnapshot snapshot = latestSnapshot(resourceType, resourceId);
        if (snapshot == null) {
            return emptySnapshot(resourceType, resourceId);
        }
        return toSnapshotVo(snapshot);
    }

    @Transactional
    public CollabSnapshotVo writeSnapshot(
            Long userId,
            String resourceType,
            String resourceId,
            CollabSnapshotRequest request
    ) {
        requireEditable(userId, resourceType, resourceId);
        CollabSnapshot latest = latestSnapshot(resourceType, resourceId);
        if (latest != null && request.version() <= latest.getVersion()) {
            return toSnapshotVo(latest);
        }
        CollabSnapshot snapshot = buildSnapshot(resourceType, resourceId, request);
        insertSnapshot(snapshot);
        return toSnapshotVo(snapshot);
    }

    public CollabAwarenessVo getAwareness(Long userId, String resourceType, String resourceId) {
        requireReadable(userId, resourceType, resourceId);
        List<DocsNotePresenceVo> users = RESOURCE_DOCS.equals(resourceType)
                ? docsCollaborationService.listPresence(userId, Long.parseLong(resourceId))
                : List.of();
        return new CollabAwarenessVo(resourceType, resourceId, users);
    }

    @Transactional
    public Long appendUpdate(Long userId, String resourceType, String resourceId, byte[] updatePayload) {
        requireEditable(userId, resourceType, resourceId);
        validateUpdatePayload(updatePayload);
        CollabUpdate update = buildUpdate(resourceType, resourceId, updatePayload);
        collabUpdateMapper.insert(update);
        return update.getSeq();
    }

    public List<byte[]> listUpdatePayloads(Long userId, String resourceType, String resourceId) {
        requireReadable(userId, resourceType, resourceId);
        return collabUpdateMapper.selectList(new LambdaQueryWrapper<CollabUpdate>()
                        .eq(CollabUpdate::getResourceType, resourceType)
                        .eq(CollabUpdate::getResourceId, resourceId)
                        .orderByAsc(CollabUpdate::getSeq))
                .stream()
                .map(CollabUpdate::getUpdatePayload)
                .toList();
    }

    public void requireReadable(Long userId, String resourceType, String resourceId) {
        requireDocsResource(resourceType, resourceId);
        docsAccessService.requireAccessible(userId, Long.parseLong(resourceId));
    }

    public void requireEditable(Long userId, String resourceType, String resourceId) {
        requireDocsResource(resourceType, resourceId);
        docsAccessService.requireEditable(userId, Long.parseLong(resourceId));
    }

    private CollabSnapshot latestSnapshot(String resourceType, String resourceId) {
        return collabSnapshotMapper.selectOne(new LambdaQueryWrapper<CollabSnapshot>()
                .eq(CollabSnapshot::getResourceType, resourceType)
                .eq(CollabSnapshot::getResourceId, resourceId)
                .orderByDesc(CollabSnapshot::getVersion)
                .last("limit 1"));
    }

    private CollabUpdate latestUpdate(String resourceType, String resourceId) {
        return collabUpdateMapper.selectOne(new LambdaQueryWrapper<CollabUpdate>()
                .eq(CollabUpdate::getResourceType, resourceType)
                .eq(CollabUpdate::getResourceId, resourceId)
                .orderByDesc(CollabUpdate::getSeq)
                .last("limit 1"));
    }

    private CollabSnapshot buildSnapshot(String resourceType, String resourceId, CollabSnapshotRequest request) {
        LocalDateTime now = LocalDateTime.now();
        CollabSnapshot snapshot = new CollabSnapshot();
        snapshot.setResourceType(resourceType);
        snapshot.setResourceId(resourceId);
        snapshot.setVersion(request.version());
        snapshot.setSnapshot(decodeBase64(request.snapshotBase64()));
        snapshot.setCreatedAt(now);
        snapshot.setUpdatedAt(now);
        snapshot.setDeleted(0);
        return snapshot;
    }

    private CollabUpdate buildUpdate(String resourceType, String resourceId, byte[] payload) {
        CollabUpdate latest = latestUpdate(resourceType, resourceId);
        CollabUpdate update = new CollabUpdate();
        update.setResourceType(resourceType);
        update.setResourceId(resourceId);
        update.setSeq(latest == null ? 1L : latest.getSeq() + 1L);
        update.setUpdatePayload(payload);
        update.setCreatedAt(LocalDateTime.now());
        update.setDeleted(0);
        return update;
    }

    private void insertSnapshot(CollabSnapshot snapshot) {
        try {
            collabSnapshotMapper.insert(snapshot);
        } catch (DuplicateKeyException exception) {
            CollabSnapshot latest = latestSnapshot(snapshot.getResourceType(), snapshot.getResourceId());
            if (latest == null) {
                throw exception;
            }
            snapshot.setVersion(latest.getVersion());
            snapshot.setSnapshot(latest.getSnapshot());
            snapshot.setUpdatedAt(latest.getUpdatedAt());
        }
    }

    private void requireDocsResource(String resourceType, String resourceId) {
        if (!RESOURCE_DOCS.equals(resourceType) || !StringUtils.hasText(resourceId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Only docs collaboration resources are currently wired");
        }
        try {
            Long.parseLong(resourceId);
        } catch (NumberFormatException exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "docs collaboration resource id must be numeric");
        }
    }

    private byte[] decodeBase64(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "snapshotBase64 is invalid");
        }
    }

    private void validateUpdatePayload(byte[] payload) {
        if (payload == null || payload.length == 0 || payload.length > MAX_UPDATE_BYTES) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "collaboration update payload size is invalid");
        }
    }

    private CollabSnapshotVo emptySnapshot(String resourceType, String resourceId) {
        return new CollabSnapshotVo(resourceType, resourceId, 0, "", null);
    }

    private CollabSnapshotVo toSnapshotVo(CollabSnapshot snapshot) {
        return new CollabSnapshotVo(
                snapshot.getResourceType(),
                snapshot.getResourceId(),
                snapshot.getVersion(),
                Base64.getEncoder().encodeToString(snapshot.getSnapshot()),
                snapshot.getUpdatedAt()
        );
    }
}
