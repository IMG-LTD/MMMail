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
import com.mmmail.server.model.dto.CreateStandardNoteRequest;
import com.mmmail.server.model.dto.UpdateStandardNoteRequest;
import com.mmmail.server.model.entity.DocsNote;
import com.mmmail.server.model.entity.StandardNoteFolder;
import com.mmmail.server.model.entity.StandardNoteProfile;
import com.mmmail.server.model.vo.StandardNoteChecklistItemVo;
import com.mmmail.server.model.vo.StandardNoteDetailVo;
import com.mmmail.server.model.vo.StandardNoteSummaryVo;
import com.mmmail.server.model.vo.StandardNotesExportVo;
import com.mmmail.server.model.vo.StandardNotesOverviewVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StandardNotesService {

    private static final String WORKSPACE_TYPE_STANDARD_NOTES = "STANDARD_NOTES";
    private static final String NOTE_TYPE_PLAIN_TEXT = "PLAIN_TEXT";
    private static final String NOTE_TYPE_CHECKLIST = "CHECKLIST";
    private static final String SPECIAL_FOLDER_UNFILED = "UNFILED";
    private static final Set<String> NOTE_TYPES = Set.of(NOTE_TYPE_PLAIN_TEXT, "MARKDOWN", NOTE_TYPE_CHECKLIST);
    private static final int DEFAULT_LIST_LIMIT = 40;
    private static final int MAX_LIST_LIMIT = 100;
    private static final int MAX_FETCH_LIMIT = 400;
    private static final int TAG_MAX_COUNT = 12;
    private static final int TAG_MAX_LENGTH = 32;

    private final DocsNoteMapper docsNoteMapper;
    private final StandardNoteProfileMapper standardNoteProfileMapper;
    private final StandardNoteFolderMapper standardNoteFolderMapper;
    private final StandardNotesFolderService standardNotesFolderService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final StandardNotesChecklistCodec checklistCodec;

    public StandardNotesService(
            DocsNoteMapper docsNoteMapper,
            StandardNoteProfileMapper standardNoteProfileMapper,
            StandardNoteFolderMapper standardNoteFolderMapper,
            StandardNotesFolderService standardNotesFolderService,
            AuditService auditService,
            ObjectMapper objectMapper,
            StandardNotesChecklistCodec checklistCodec
    ) {
        this.docsNoteMapper = docsNoteMapper;
        this.standardNoteProfileMapper = standardNoteProfileMapper;
        this.standardNoteFolderMapper = standardNoteFolderMapper;
        this.standardNotesFolderService = standardNotesFolderService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.checklistCodec = checklistCodec;
    }

    public StandardNotesOverviewVo getOverview(Long userId, String ipAddress) {
        OverviewContext context = buildOverviewContext(userId);
        auditService.record(userId, "STANDARD_NOTES_OVERVIEW", "notes=" + context.totalNoteCount() + ",folders=" + context.folderCount(), ipAddress);
        return context.overview();
    }

    public List<StandardNoteSummaryVo> list(
            Long userId,
            String keyword,
            boolean includeArchived,
            String noteType,
            String tag,
            String folderId,
            Integer limit
    ) {
        int safeLimit = normalizeLimit(limit);
        List<StandardNoteProfile> profiles = loadProfilesForList(userId, includeArchived, noteType, safeLimit);
        Map<Long, DocsNote> notesById = loadNotesById(userId, profiles);
        Map<Long, StandardNoteFolder> foldersById = loadFoldersById(userId, profiles);
        FolderFilter folderFilter = normalizeFolderFilter(folderId, userId);
        return buildSummaries(profiles, notesById, foldersById, normalizeKeyword(keyword), normalizeTagFilter(tag), folderFilter, safeLimit);
    }

    @Transactional
    public StandardNoteDetailVo create(Long userId, CreateStandardNoteRequest request, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        DocsNote note = new DocsNote();
        note.setOwnerId(userId);
        note.setWorkspaceType(WORKSPACE_TYPE_STANDARD_NOTES);
        note.setTitle(requireTitle(request.title()));
        note.setContent(normalizeContent(request.content()));
        note.setCurrentVersion(1);
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        note.setDeleted(0);
        docsNoteMapper.insert(note);
        StandardNoteProfile profile = new StandardNoteProfile();
        profile.setNoteId(note.getId());
        profile.setOwnerId(userId);
        profile.setNoteType(normalizeNoteType(request.noteType()));
        profile.setTagsJson(writeTags(request.tags()));
        profile.setFolderId(resolveOwnedFolderId(userId, request.folderId()));
        profile.setPinned(toFlag(Boolean.TRUE.equals(request.pinned())));
        profile.setArchived(0);
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        profile.setDeleted(0);
        standardNoteProfileMapper.insert(profile);
        auditService.record(userId, "STANDARD_NOTE_CREATE", buildAuditDetail(note, profile), ipAddress);
        return toDetail(note, profile, loadFolder(profile.getFolderId()));
    }

    public StandardNoteDetailVo get(Long userId, Long noteId) {
        DocsNote note = loadOwnedNote(userId, noteId);
        StandardNoteProfile profile = loadOwnedProfile(userId, noteId);
        return toDetail(note, profile, loadFolder(profile.getFolderId()));
    }

    @Transactional
    public StandardNoteDetailVo update(Long userId, Long noteId, UpdateStandardNoteRequest request, String ipAddress) {
        DocsNote note = loadOwnedNote(userId, noteId);
        StandardNoteProfile profile = loadOwnedProfile(userId, noteId);
        validateCurrentVersion(note, request.currentVersion());
        note.setTitle(requireTitle(request.title()));
        note.setContent(normalizeContent(request.content()));
        note.setCurrentVersion(note.getCurrentVersion() + 1);
        note.setUpdatedAt(LocalDateTime.now());
        docsNoteMapper.updateById(note);
        profile.setNoteType(normalizeNoteType(request.noteType()));
        profile.setTagsJson(writeTags(request.tags()));
        profile.setFolderId(resolveOwnedFolderId(userId, request.folderId()));
        profile.setPinned(toFlag(Boolean.TRUE.equals(request.pinned())));
        profile.setArchived(toFlag(Boolean.TRUE.equals(request.archived())));
        profile.setUpdatedAt(LocalDateTime.now());
        standardNoteProfileMapper.updateById(profile);
        auditService.record(userId, "STANDARD_NOTE_UPDATE", buildAuditDetail(note, profile), ipAddress);
        return toDetail(note, profile, loadFolder(profile.getFolderId()));
    }

    @Transactional
    public void delete(Long userId, Long noteId, String ipAddress) {
        DocsNote note = loadOwnedNote(userId, noteId);
        StandardNoteProfile profile = loadOwnedProfile(userId, noteId);
        docsNoteMapper.deleteById(note.getId());
        standardNoteProfileMapper.deleteById(profile.getNoteId());
        auditService.record(userId, "STANDARD_NOTE_DELETE", buildAuditDetail(note, profile), ipAddress);
    }

    public StandardNotesExportVo exportWorkspace(Long userId, String ipAddress) {
        OverviewContext context = buildOverviewContext(userId);
        List<StandardNoteProfile> profiles = loadProfilesForOverview(userId);
        Map<Long, DocsNote> notesById = loadNotesById(userId, profiles);
        Map<Long, StandardNoteFolder> foldersById = loadFoldersById(userId, profiles);
        List<StandardNoteDetailVo> notes = profiles.stream()
                .map(profile -> notesById.get(profile.getNoteId()) == null ? null : toDetail(notesById.get(profile.getNoteId()), profile, foldersById.get(profile.getFolderId())))
                .filter(item -> item != null)
                .sorted(Comparator.comparing(StandardNoteDetailVo::updatedAt, Comparator.reverseOrder()))
                .toList();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        auditService.record(userId, "STANDARD_NOTES_EXPORT", "notes=" + notes.size() + ",folders=" + context.folderCount(), ipAddress);
        return new StandardNotesExportVo(
                "standard-notes-workspace-" + timestamp + ".json",
                "DECRYPTED_JSON_FOUNDATION",
                LocalDateTime.now(),
                context.overview(),
                standardNotesFolderService.snapshot(userId),
                notes
        );
    }

    private OverviewContext buildOverviewContext(Long userId) {
        List<StandardNoteProfile> profiles = loadProfilesForOverview(userId);
        Map<Long, DocsNote> notesById = loadNotesById(userId, profiles);
        Set<String> uniqueTags = new LinkedHashSet<>();
        long archivedCount = 0;
        long pinnedCount = 0;
        long checklistNoteCount = 0;
        long checklistTaskCount = 0;
        long completedChecklistTaskCount = 0;
        for (StandardNoteProfile profile : profiles) {
            uniqueTags.addAll(readTags(profile.getTagsJson()));
            archivedCount += isEnabled(profile.getArchived()) ? 1 : 0;
            pinnedCount += isEnabled(profile.getPinned()) ? 1 : 0;
            DocsNote note = notesById.get(profile.getNoteId());
            if (note == null || !NOTE_TYPE_CHECKLIST.equals(profile.getNoteType())) {
                continue;
            }
            checklistNoteCount++;
            StandardNotesChecklistCodec.ChecklistStats stats = checklistCodec.summarize(note.getContent());
            checklistTaskCount += stats.taskCount();
            completedChecklistTaskCount += stats.completedTaskCount();
        }
        long totalNoteCount = profiles.size();
        long folderCount = safeCount(standardNoteFolderMapper.selectCount(new LambdaQueryWrapper<StandardNoteFolder>()
                .eq(StandardNoteFolder::getOwnerId, userId)));
        return new OverviewContext(
                totalNoteCount,
                folderCount,
                new StandardNotesOverviewVo(
                        totalNoteCount,
                        totalNoteCount - archivedCount,
                        pinnedCount,
                        archivedCount,
                        uniqueTags.size(),
                        folderCount,
                        checklistNoteCount,
                        checklistTaskCount,
                        completedChecklistTaskCount,
                        totalNoteCount > 0 || folderCount > 0,
                        LocalDateTime.now()
                )
        );
    }

    private List<StandardNoteProfile> loadProfilesForOverview(Long userId) {
        return standardNoteProfileMapper.selectList(new LambdaQueryWrapper<StandardNoteProfile>()
                .eq(StandardNoteProfile::getOwnerId, userId)
                .orderByDesc(StandardNoteProfile::getUpdatedAt));
    }

    private List<StandardNoteProfile> loadProfilesForList(Long userId, boolean includeArchived, String noteType, int limit) {
        LambdaQueryWrapper<StandardNoteProfile> query = new LambdaQueryWrapper<StandardNoteProfile>()
                .eq(StandardNoteProfile::getOwnerId, userId)
                .orderByDesc(StandardNoteProfile::getPinned)
                .orderByDesc(StandardNoteProfile::getUpdatedAt)
                .last("limit " + Math.min(MAX_FETCH_LIMIT, Math.max(limit * 4, safeFetchFloor(limit))));
        if (!includeArchived) {
            query.eq(StandardNoteProfile::getArchived, 0);
        }
        String normalizedType = normalizeOptionalNoteType(noteType);
        if (normalizedType != null) {
            query.eq(StandardNoteProfile::getNoteType, normalizedType);
        }
        return standardNoteProfileMapper.selectList(query);
    }

    private Map<Long, DocsNote> loadNotesById(Long userId, List<StandardNoteProfile> profiles) {
        List<Long> noteIds = profiles.stream().map(StandardNoteProfile::getNoteId).toList();
        if (noteIds.isEmpty()) {
            return Map.of();
        }
        List<DocsNote> notes = docsNoteMapper.selectList(new LambdaQueryWrapper<DocsNote>()
                .in(DocsNote::getId, noteIds)
                .eq(DocsNote::getOwnerId, userId)
                .eq(DocsNote::getWorkspaceType, WORKSPACE_TYPE_STANDARD_NOTES));
        Map<Long, DocsNote> result = new LinkedHashMap<>();
        for (DocsNote note : notes) {
            result.put(note.getId(), note);
        }
        return result;
    }

    private Map<Long, StandardNoteFolder> loadFoldersById(Long userId, List<StandardNoteProfile> profiles) {
        List<Long> folderIds = profiles.stream().map(StandardNoteProfile::getFolderId).filter(id -> id != null).distinct().toList();
        if (folderIds.isEmpty()) {
            return Map.of();
        }
        List<StandardNoteFolder> folders = standardNoteFolderMapper.selectList(new LambdaQueryWrapper<StandardNoteFolder>()
                .eq(StandardNoteFolder::getOwnerId, userId)
                .in(StandardNoteFolder::getId, folderIds));
        Map<Long, StandardNoteFolder> result = new LinkedHashMap<>();
        for (StandardNoteFolder folder : folders) {
            result.put(folder.getId(), folder);
        }
        return result;
    }

    private List<StandardNoteSummaryVo> buildSummaries(
            List<StandardNoteProfile> profiles,
            Map<Long, DocsNote> notesById,
            Map<Long, StandardNoteFolder> foldersById,
            String keyword,
            String tagFilter,
            FolderFilter folderFilter,
            int limit
    ) {
        List<StandardNoteSummaryVo> items = new ArrayList<>();
        for (StandardNoteProfile profile : profiles) {
            DocsNote note = notesById.get(profile.getNoteId());
            if (note == null || !matchesKeyword(note, keyword) || !matchesFolder(profile, folderFilter)) {
                continue;
            }
            List<String> tags = readTags(profile.getTagsJson());
            if (tagFilter != null && !tags.contains(tagFilter)) {
                continue;
            }
            StandardNotesChecklistCodec.ChecklistStats stats = checklistCodec.summarize(note.getContent());
            Long folderId = profile.getFolderId();
            StandardNoteFolder folder = folderId == null ? null : foldersById.get(folderId);
            items.add(new StandardNoteSummaryVo(
                    String.valueOf(note.getId()),
                    note.getTitle(),
                    shorten(note.getContent(), 120),
                    profile.getNoteType(),
                    tags,
                    isEnabled(profile.getPinned()),
                    isEnabled(profile.getArchived()),
                    note.getCurrentVersion() == null ? 1 : note.getCurrentVersion(),
                    profile.getFolderId() == null ? null : String.valueOf(profile.getFolderId()),
                    folder == null ? null : folder.getName(),
                    stats.taskCount(),
                    stats.completedTaskCount(),
                    note.getUpdatedAt()
            ));
            if (items.size() >= limit) {
                break;
            }
        }
        items.sort(Comparator.comparing(StandardNoteSummaryVo::pinned, Comparator.reverseOrder())
                .thenComparing(StandardNoteSummaryVo::updatedAt, Comparator.reverseOrder()));
        return items;
    }

    private boolean matchesFolder(StandardNoteProfile profile, FolderFilter folderFilter) {
        if (folderFilter == null) {
            return true;
        }
        if (folderFilter.unfiled()) {
            return profile.getFolderId() == null;
        }
        return folderFilter.folderId().equals(profile.getFolderId());
    }

    private DocsNote loadOwnedNote(Long userId, Long noteId) {
        DocsNote note = docsNoteMapper.selectOne(new LambdaQueryWrapper<DocsNote>()
                .eq(DocsNote::getId, noteId)
                .eq(DocsNote::getOwnerId, userId)
                .eq(DocsNote::getWorkspaceType, WORKSPACE_TYPE_STANDARD_NOTES)
                .last("limit 1"));
        if (note == null) {
            throw new BizException(ErrorCode.STANDARD_NOTE_NOT_FOUND);
        }
        return note;
    }

    private StandardNoteProfile loadOwnedProfile(Long userId, Long noteId) {
        StandardNoteProfile profile = standardNoteProfileMapper.selectOne(new LambdaQueryWrapper<StandardNoteProfile>()
                .eq(StandardNoteProfile::getNoteId, noteId)
                .eq(StandardNoteProfile::getOwnerId, userId)
                .last("limit 1"));
        if (profile == null) {
            throw new BizException(ErrorCode.STANDARD_NOTE_NOT_FOUND);
        }
        return profile;
    }

    private Long resolveOwnedFolderId(Long userId, Long folderId) {
        if (folderId == null) {
            return null;
        }
        return standardNotesFolderService.requireOwnedFolder(userId, folderId).getId();
    }

    private StandardNoteFolder loadFolder(Long folderId) {
        if (folderId == null) {
            return null;
        }
        return standardNoteFolderMapper.selectById(folderId);
    }

    private void validateCurrentVersion(DocsNote note, Integer currentVersion) {
        if (currentVersion == null || currentVersion < 1) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "currentVersion is required");
        }
        if (!currentVersion.equals(note.getCurrentVersion())) {
            throw new BizException(ErrorCode.STANDARD_NOTE_VERSION_CONFLICT, "Standard note has been updated by another session");
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIST_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIST_LIMIT));
    }

    private int safeFetchFloor(int limit) {
        return Math.max(60, limit);
    }

    private String requireTitle(String title) {
        if (!StringUtils.hasText(title)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Standard note title is required");
        }
        return title.trim();
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content;
    }

    private String normalizeNoteType(String noteType) {
        String normalized = normalizeOptionalNoteType(noteType);
        return normalized == null ? NOTE_TYPE_PLAIN_TEXT : normalized;
    }

    private String normalizeOptionalNoteType(String noteType) {
        if (!StringUtils.hasText(noteType)) {
            return null;
        }
        String normalized = noteType.trim().toUpperCase();
        if (!NOTE_TYPES.contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported standard note type");
        }
        return normalized;
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim().toLowerCase();
    }

    private boolean matchesKeyword(DocsNote note, String keyword) {
        if (keyword == null) {
            return true;
        }
        return note.getTitle().toLowerCase().contains(keyword)
                || normalizeContent(note.getContent()).toLowerCase().contains(keyword);
    }

    private String normalizeTagFilter(String tag) {
        if (!StringUtils.hasText(tag)) {
            return null;
        }
        return validateTag(tag.trim().toLowerCase());
    }

    private FolderFilter normalizeFolderFilter(String folderId, Long userId) {
        if (!StringUtils.hasText(folderId)) {
            return null;
        }
        String normalized = folderId.trim();
        if (SPECIAL_FOLDER_UNFILED.equalsIgnoreCase(normalized)) {
            return new FolderFilter(null, true);
        }
        try {
            Long parsed = Long.valueOf(normalized);
            resolveOwnedFolderId(userId, parsed);
            return new FolderFilter(parsed, false);
        } catch (NumberFormatException exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "folderId is invalid");
        }
    }

    private String writeTags(List<String> tags) {
        List<String> normalized = normalizeTags(tags);
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to serialize standard note tags");
        }
    }

    private List<String> readTags(String tagsJson) {
        if (!StringUtils.hasText(tagsJson)) {
            return List.of();
        }
        try {
            List<String> raw = objectMapper.readValue(tagsJson, new TypeReference<List<String>>() { });
            return normalizeTags(raw);
        } catch (JsonProcessingException exception) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to parse standard note tags");
        }
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            if (!StringUtils.hasText(tag)) {
                continue;
            }
            normalized.add(validateTag(tag.trim().toLowerCase()));
        }
        if (normalized.size() > TAG_MAX_COUNT) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Too many standard note tags");
        }
        return List.copyOf(normalized);
    }

    private String validateTag(String tag) {
        if (tag.length() > TAG_MAX_LENGTH) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Standard note tag is too long");
        }
        if (!tag.matches("[a-z0-9][a-z0-9_:/-]{0,31}")) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Standard note tag format is invalid");
        }
        return tag;
    }

    private boolean isEnabled(Integer value) {
        return Integer.valueOf(1).equals(value);
    }

    private int toFlag(boolean value) {
        return value ? 1 : 0;
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }

    private String shorten(String content, int limit) {
        String normalized = normalizeContent(content).replace('\n', ' ').trim();
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit - 3) + "...";
    }

    private StandardNoteDetailVo toDetail(DocsNote note, StandardNoteProfile profile, StandardNoteFolder folder) {
        List<StandardNoteChecklistItemVo> checklistItems = NOTE_TYPE_CHECKLIST.equals(profile.getNoteType())
                ? checklistCodec.parse(note.getContent())
                : List.of();
        StandardNotesChecklistCodec.ChecklistStats stats = checklistCodec.summarize(note.getContent());
        return new StandardNoteDetailVo(
                String.valueOf(note.getId()),
                note.getTitle(),
                normalizeContent(note.getContent()),
                profile.getNoteType(),
                readTags(profile.getTagsJson()),
                isEnabled(profile.getPinned()),
                isEnabled(profile.getArchived()),
                note.getCurrentVersion() == null ? 1 : note.getCurrentVersion(),
                profile.getFolderId() == null ? null : String.valueOf(profile.getFolderId()),
                folder == null ? null : folder.getName(),
                checklistItems,
                stats.taskCount(),
                stats.completedTaskCount(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }

    private String buildAuditDetail(DocsNote note, StandardNoteProfile profile) {
        return "noteId=" + note.getId()
                + ",type=" + profile.getNoteType()
                + ",folderId=" + profile.getFolderId()
                + ",pinned=" + isEnabled(profile.getPinned())
                + ",archived=" + isEnabled(profile.getArchived());
    }

    private record FolderFilter(Long folderId, boolean unfiled) {
    }

    private record OverviewContext(long totalNoteCount, long folderCount, StandardNotesOverviewVo overview) {
    }
}
