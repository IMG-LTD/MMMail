package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.DocsNoteMapper;
import com.mmmail.server.mapper.StandardNoteFolderMapper;
import com.mmmail.server.mapper.StandardNoteProfileMapper;
import com.mmmail.server.model.dto.CreateStandardNoteFolderRequest;
import com.mmmail.server.model.dto.UpdateStandardNoteFolderRequest;
import com.mmmail.server.model.entity.DocsNote;
import com.mmmail.server.model.entity.StandardNoteFolder;
import com.mmmail.server.model.entity.StandardNoteProfile;
import com.mmmail.server.model.vo.StandardNoteFolderVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class StandardNotesFolderService {

    private static final String DEFAULT_FOLDER_COLOR = "#C7A57A";
    private static final Pattern COLOR_PATTERN = Pattern.compile("#[0-9A-Fa-f]{6}");

    private final StandardNoteFolderMapper standardNoteFolderMapper;
    private final StandardNoteProfileMapper standardNoteProfileMapper;
    private final DocsNoteMapper docsNoteMapper;
    private final AuditService auditService;
    private final StandardNotesChecklistCodec checklistCodec;

    public StandardNotesFolderService(
            StandardNoteFolderMapper standardNoteFolderMapper,
            StandardNoteProfileMapper standardNoteProfileMapper,
            DocsNoteMapper docsNoteMapper,
            AuditService auditService,
            StandardNotesChecklistCodec checklistCodec
    ) {
        this.standardNoteFolderMapper = standardNoteFolderMapper;
        this.standardNoteProfileMapper = standardNoteProfileMapper;
        this.docsNoteMapper = docsNoteMapper;
        this.auditService = auditService;
        this.checklistCodec = checklistCodec;
    }

    public List<StandardNoteFolderVo> list(Long userId, String ipAddress) {
        List<StandardNoteFolderVo> items = snapshot(userId);
        auditService.record(userId, "STANDARD_NOTE_FOLDER_LIST", "count=" + items.size(), ipAddress);
        return items;
    }

    List<StandardNoteFolderVo> snapshot(Long userId) {
        List<StandardNoteFolder> folders = loadFolders(userId);
        Map<Long, FolderStats> statsMap = buildStats(userId, folders);
        return folders.stream()
                .map(folder -> toVo(folder, statsMap.get(folder.getId())))
                .toList();
    }

    @Transactional
    public StandardNoteFolderVo create(Long userId, CreateStandardNoteFolderRequest request, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        String name = normalizeName(request.name());
        assertFolderNameAvailable(userId, name, null);
        StandardNoteFolder folder = new StandardNoteFolder();
        folder.setOwnerId(userId);
        folder.setName(name);
        folder.setColor(normalizeColor(request.color()));
        folder.setDescription(normalizeDescription(request.description()));
        folder.setCreatedAt(now);
        folder.setUpdatedAt(now);
        folder.setDeleted(0);
        standardNoteFolderMapper.insert(folder);
        auditService.record(userId, "STANDARD_NOTE_FOLDER_CREATE", "folderId=" + folder.getId() + ",name=" + folder.getName(), ipAddress);
        return toVo(folder, FolderStats.EMPTY);
    }

    @Transactional
    public StandardNoteFolderVo update(Long userId, Long folderId, UpdateStandardNoteFolderRequest request, String ipAddress) {
        StandardNoteFolder folder = loadOwnedFolder(userId, folderId);
        String name = normalizeName(request.name());
        assertFolderNameAvailable(userId, name, folderId);
        folder.setName(name);
        folder.setColor(normalizeColor(request.color()));
        folder.setDescription(normalizeDescription(request.description()));
        folder.setUpdatedAt(LocalDateTime.now());
        standardNoteFolderMapper.updateById(folder);
        auditService.record(userId, "STANDARD_NOTE_FOLDER_UPDATE", "folderId=" + folder.getId() + ",name=" + folder.getName(), ipAddress);
        FolderStats stats = buildStats(userId, List.of(folder)).getOrDefault(folder.getId(), FolderStats.EMPTY);
        return toVo(folder, stats);
    }

    @Transactional
    public void delete(Long userId, Long folderId, String ipAddress) {
        StandardNoteFolder folder = loadOwnedFolder(userId, folderId);
        standardNoteProfileMapper.update(null, new LambdaUpdateWrapper<StandardNoteProfile>()
                .eq(StandardNoteProfile::getOwnerId, userId)
                .eq(StandardNoteProfile::getFolderId, folderId)
                .set(StandardNoteProfile::getFolderId, null)
                .set(StandardNoteProfile::getUpdatedAt, LocalDateTime.now()));
        standardNoteFolderMapper.deleteById(folderId);
        auditService.record(userId, "STANDARD_NOTE_FOLDER_DELETE", "folderId=" + folderId + ",name=" + folder.getName(), ipAddress);
    }

    StandardNoteFolder requireOwnedFolder(Long userId, Long folderId) {
        return loadOwnedFolder(userId, folderId);
    }

    private List<StandardNoteFolder> loadFolders(Long userId) {
        return standardNoteFolderMapper.selectList(new LambdaQueryWrapper<StandardNoteFolder>()
                .eq(StandardNoteFolder::getOwnerId, userId)
                .orderByAsc(StandardNoteFolder::getName)
                .orderByDesc(StandardNoteFolder::getUpdatedAt));
    }

    private Map<Long, FolderStats> buildStats(Long userId, List<StandardNoteFolder> folders) {
        if (folders.isEmpty()) {
            return Map.of();
        }
        List<Long> folderIds = folders.stream().map(StandardNoteFolder::getId).toList();
        List<StandardNoteProfile> profiles = standardNoteProfileMapper.selectList(new LambdaQueryWrapper<StandardNoteProfile>()
                .eq(StandardNoteProfile::getOwnerId, userId)
                .in(StandardNoteProfile::getFolderId, folderIds));
        Map<Long, DocsNote> notesById = loadNotesById(userId, profiles);
        Map<Long, FolderStats> statsMap = new LinkedHashMap<>();
        for (StandardNoteProfile profile : profiles) {
            DocsNote note = notesById.get(profile.getNoteId());
            if (note == null || profile.getFolderId() == null) {
                continue;
            }
            StandardNotesChecklistCodec.ChecklistStats checklistStats = checklistCodec.summarize(note.getContent());
            FolderStats current = statsMap.getOrDefault(profile.getFolderId(), FolderStats.EMPTY);
            statsMap.put(profile.getFolderId(), current.add(checklistStats.taskCount(), checklistStats.completedTaskCount()));
        }
        return statsMap;
    }

    private Map<Long, DocsNote> loadNotesById(Long userId, List<StandardNoteProfile> profiles) {
        List<Long> noteIds = profiles.stream().map(StandardNoteProfile::getNoteId).toList();
        if (noteIds.isEmpty()) {
            return Map.of();
        }
        List<DocsNote> notes = docsNoteMapper.selectList(new LambdaQueryWrapper<DocsNote>()
                .eq(DocsNote::getOwnerId, userId)
                .eq(DocsNote::getWorkspaceType, "STANDARD_NOTES")
                .in(DocsNote::getId, noteIds));
        Map<Long, DocsNote> notesById = new LinkedHashMap<>();
        for (DocsNote note : notes) {
            notesById.put(note.getId(), note);
        }
        return notesById;
    }

    private StandardNoteFolder loadOwnedFolder(Long userId, Long folderId) {
        StandardNoteFolder folder = standardNoteFolderMapper.selectOne(new LambdaQueryWrapper<StandardNoteFolder>()
                .eq(StandardNoteFolder::getId, folderId)
                .eq(StandardNoteFolder::getOwnerId, userId)
                .last("limit 1"));
        if (folder == null) {
            throw new BizException(ErrorCode.STANDARD_NOTE_FOLDER_NOT_FOUND);
        }
        return folder;
    }

    private void assertFolderNameAvailable(Long userId, String name, Long excludeFolderId) {
        StandardNoteFolder existing = standardNoteFolderMapper.selectOne(new LambdaQueryWrapper<StandardNoteFolder>()
                .eq(StandardNoteFolder::getOwnerId, userId)
                .eq(StandardNoteFolder::getName, name)
                .last("limit 1"));
        if (existing == null) {
            return;
        }
        if (excludeFolderId != null && excludeFolderId.equals(existing.getId())) {
            return;
        }
        throw new BizException(ErrorCode.STANDARD_NOTE_FOLDER_CONFLICT, "Folder name already exists");
    }

    private String normalizeName(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Folder name is required");
        }
        return rawValue.trim();
    }

    private String normalizeColor(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return DEFAULT_FOLDER_COLOR;
        }
        String normalized = rawValue.trim();
        if (!COLOR_PATTERN.matcher(normalized).matches()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Folder color must be a hex code");
        }
        return normalized.toUpperCase();
    }

    private String normalizeDescription(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        return rawValue.trim();
    }

    private StandardNoteFolderVo toVo(StandardNoteFolder folder, FolderStats stats) {
        FolderStats safeStats = stats == null ? FolderStats.EMPTY : stats;
        return new StandardNoteFolderVo(
                String.valueOf(folder.getId()),
                folder.getName(),
                folder.getColor(),
                folder.getDescription(),
                safeStats.noteCount(),
                safeStats.checklistTaskCount(),
                safeStats.completedChecklistTaskCount(),
                folder.getUpdatedAt()
        );
    }

    private record FolderStats(int noteCount, int checklistTaskCount, int completedChecklistTaskCount) {
        private static final FolderStats EMPTY = new FolderStats(0, 0, 0);

        private FolderStats add(int taskCount, int completedTaskCount) {
            return new FolderStats(noteCount + 1, checklistTaskCount + taskCount, completedChecklistTaskCount + completedTaskCount);
        }
    }
}
