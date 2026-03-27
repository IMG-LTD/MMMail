package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.DocsNoteMapper;
import com.mmmail.server.mapper.StandardNoteFolderMapper;
import com.mmmail.server.mapper.StandardNoteProfileMapper;
import com.mmmail.server.model.dto.ToggleStandardNoteChecklistItemRequest;
import com.mmmail.server.model.entity.DocsNote;
import com.mmmail.server.model.entity.StandardNoteFolder;
import com.mmmail.server.model.entity.StandardNoteProfile;
import com.mmmail.server.model.vo.StandardNoteChecklistItemVo;
import com.mmmail.server.model.vo.StandardNoteDetailVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StandardNotesChecklistService {

    private final DocsNoteMapper docsNoteMapper;
    private final StandardNoteProfileMapper standardNoteProfileMapper;
    private final StandardNoteFolderMapper standardNoteFolderMapper;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final StandardNotesChecklistCodec checklistCodec;

    public StandardNotesChecklistService(
            DocsNoteMapper docsNoteMapper,
            StandardNoteProfileMapper standardNoteProfileMapper,
            StandardNoteFolderMapper standardNoteFolderMapper,
            AuditService auditService,
            ObjectMapper objectMapper,
            StandardNotesChecklistCodec checklistCodec
    ) {
        this.docsNoteMapper = docsNoteMapper;
        this.standardNoteProfileMapper = standardNoteProfileMapper;
        this.standardNoteFolderMapper = standardNoteFolderMapper;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.checklistCodec = checklistCodec;
    }

    @Transactional
    public StandardNoteDetailVo toggleItem(
            Long userId,
            Long noteId,
            int itemIndex,
            ToggleStandardNoteChecklistItemRequest request,
            String ipAddress
    ) {
        DocsNote note = loadOwnedNote(userId, noteId);
        StandardNoteProfile profile = loadOwnedProfile(userId, noteId);
        validateChecklistNote(profile);
        validateCurrentVersion(note, request.currentVersion());
        note.setContent(checklistCodec.toggle(note.getContent(), itemIndex, Boolean.TRUE.equals(request.completed())));
        note.setCurrentVersion(note.getCurrentVersion() + 1);
        note.setUpdatedAt(LocalDateTime.now());
        docsNoteMapper.updateById(note);
        auditService.record(userId, "STANDARD_NOTE_CHECKLIST_TOGGLE", "noteId=" + noteId + ",itemIndex=" + itemIndex + ",completed=" + request.completed(), ipAddress);
        return toDetail(note, profile, loadFolder(profile.getFolderId()));
    }

    private DocsNote loadOwnedNote(Long userId, Long noteId) {
        DocsNote note = docsNoteMapper.selectOne(new LambdaQueryWrapper<DocsNote>()
                .eq(DocsNote::getId, noteId)
                .eq(DocsNote::getOwnerId, userId)
                .eq(DocsNote::getWorkspaceType, "STANDARD_NOTES")
                .last("limit 1"));
        if (note == null) {
            throw new BizException(ErrorCode.STANDARD_NOTE_NOT_FOUND);
        }
        return note;
    }

    private StandardNoteProfile loadOwnedProfile(Long userId, Long noteId) {
        StandardNoteProfile profile = standardNoteProfileMapper.selectOne(new LambdaQueryWrapper<StandardNoteProfile>()
                .eq(StandardNoteProfile::getOwnerId, userId)
                .eq(StandardNoteProfile::getNoteId, noteId)
                .last("limit 1"));
        if (profile == null) {
            throw new BizException(ErrorCode.STANDARD_NOTE_NOT_FOUND);
        }
        return profile;
    }

    private void validateChecklistNote(StandardNoteProfile profile) {
        if (!"CHECKLIST".equals(profile.getNoteType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Only checklist notes support task toggle");
        }
    }

    private void validateCurrentVersion(DocsNote note, Integer currentVersion) {
        if (currentVersion == null || currentVersion < 1) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "currentVersion is required");
        }
        if (!currentVersion.equals(note.getCurrentVersion())) {
            throw new BizException(ErrorCode.STANDARD_NOTE_VERSION_CONFLICT, "Standard note has been updated by another session");
        }
    }

    private StandardNoteFolder loadFolder(Long folderId) {
        if (folderId == null) {
            return null;
        }
        return standardNoteFolderMapper.selectById(folderId);
    }

    private List<String> readTags(String tagsJson) {
        if (!StringUtils.hasText(tagsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to parse standard note tags");
        }
    }

    private StandardNoteDetailVo toDetail(DocsNote note, StandardNoteProfile profile, StandardNoteFolder folder) {
        List<StandardNoteChecklistItemVo> items = checklistCodec.parse(note.getContent());
        StandardNotesChecklistCodec.ChecklistStats stats = checklistCodec.summarize(note.getContent());
        return new StandardNoteDetailVo(
                String.valueOf(note.getId()),
                note.getTitle(),
                note.getContent() == null ? "" : note.getContent(),
                profile.getNoteType(),
                readTags(profile.getTagsJson()),
                Integer.valueOf(1).equals(profile.getPinned()),
                Integer.valueOf(1).equals(profile.getArchived()),
                note.getCurrentVersion() == null ? 1 : note.getCurrentVersion(),
                profile.getFolderId() == null ? null : String.valueOf(profile.getFolderId()),
                folder == null ? null : folder.getName(),
                items,
                stats.taskCount(),
                stats.completedTaskCount(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
