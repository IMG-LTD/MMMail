package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.SearchPresetMapper;
import com.mmmail.server.model.dto.CreateSearchPresetRequest;
import com.mmmail.server.model.dto.UpdateSearchPresetRequest;
import com.mmmail.server.model.entity.SearchPreset;
import com.mmmail.server.model.vo.SearchPresetVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class SearchPresetService {

    private static final Set<String> SUPPORTED_FOLDERS = Set.of(
            "INBOX",
            "SENT",
            "DRAFTS",
            "OUTBOX",
            "SCHEDULED",
            "SNOOZED",
            "ARCHIVE",
            "SPAM",
            "TRASH"
    );

    private final SearchPresetMapper searchPresetMapper;
    private final AuditService auditService;

    public SearchPresetService(SearchPresetMapper searchPresetMapper, AuditService auditService) {
        this.searchPresetMapper = searchPresetMapper;
        this.auditService = auditService;
    }

    public List<SearchPresetVo> list(Long userId) {
        return searchPresetMapper.selectList(new LambdaQueryWrapper<SearchPreset>()
                        .eq(SearchPreset::getOwnerId, userId)
                        .orderByDesc(SearchPreset::getIsPinned)
                        .orderByDesc(SearchPreset::getPinnedAt)
                        .orderByDesc(SearchPreset::getLastUsedAt)
                        .orderByDesc(SearchPreset::getUsageCount)
                        .orderByDesc(SearchPreset::getCreatedAt))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Transactional
    public SearchPresetVo create(Long userId, CreateSearchPresetRequest request, String ipAddress) {
        String normalizedName = normalizeName(request.name());
        ensureNameUnique(userId, normalizedName, null);

        PresetFields fields = normalizeFields(
                request.keyword(),
                request.folder(),
                request.unread(),
                request.starred(),
                request.from(),
                request.to(),
                request.label()
        );

        SearchPreset preset = new SearchPreset();
        LocalDateTime now = LocalDateTime.now();
        preset.setOwnerId(userId);
        preset.setName(normalizedName);
        preset.setKeyword(fields.keyword());
        preset.setFolder(fields.folder());
        preset.setUnread(fields.unread());
        preset.setStarred(fields.starred());
        preset.setFromAt(fields.fromAt());
        preset.setToAt(fields.toAt());
        preset.setLabelName(fields.label());
        preset.setIsPinned(0);
        preset.setPinnedAt(null);
        preset.setUsageCount(0);
        preset.setLastUsedAt(null);
        preset.setCreatedAt(now);
        preset.setUpdatedAt(now);
        preset.setDeleted(0);
        searchPresetMapper.insert(preset);

        auditService.record(userId, "SEARCH_PRESET_CREATE", "name=" + normalizedName, ipAddress);
        return toVo(preset);
    }

    @Transactional
    public SearchPresetVo update(Long userId, Long presetId, UpdateSearchPresetRequest request, String ipAddress) {
        SearchPreset preset = loadPreset(userId, presetId);
        String normalizedName = normalizeName(request.name());
        ensureNameUnique(userId, normalizedName, presetId);

        PresetFields fields = normalizeFields(
                request.keyword(),
                request.folder(),
                request.unread(),
                request.starred(),
                request.from(),
                request.to(),
                request.label()
        );

        LocalDateTime now = LocalDateTime.now();
        preset.setName(normalizedName);
        preset.setKeyword(fields.keyword());
        preset.setFolder(fields.folder());
        preset.setUnread(fields.unread());
        preset.setStarred(fields.starred());
        preset.setFromAt(fields.fromAt());
        preset.setToAt(fields.toAt());
        preset.setLabelName(fields.label());
        preset.setUpdatedAt(now);
        searchPresetMapper.updateById(preset);

        auditService.record(userId, "SEARCH_PRESET_UPDATE", "preset=" + presetId + " name=" + normalizedName, ipAddress);
        return toVo(preset);
    }

    @Transactional
    public SearchPresetVo pin(Long userId, Long presetId, String ipAddress) {
        SearchPreset preset = loadPreset(userId, presetId);
        LocalDateTime now = LocalDateTime.now();

        searchPresetMapper.update(
                null,
                new LambdaUpdateWrapper<SearchPreset>()
                        .eq(SearchPreset::getId, presetId)
                        .eq(SearchPreset::getOwnerId, userId)
                        .set(SearchPreset::getIsPinned, 1)
                        .set(SearchPreset::getPinnedAt, now)
                        .set(SearchPreset::getUpdatedAt, now)
        );

        preset.setIsPinned(1);
        preset.setPinnedAt(now);
        preset.setUpdatedAt(now);

        auditService.record(userId, "SEARCH_PRESET_PIN", "preset=" + presetId, ipAddress);
        return toVo(preset);
    }

    @Transactional
    public SearchPresetVo unpin(Long userId, Long presetId, String ipAddress) {
        SearchPreset preset = loadPreset(userId, presetId);
        LocalDateTime now = LocalDateTime.now();

        searchPresetMapper.update(
                null,
                new LambdaUpdateWrapper<SearchPreset>()
                        .eq(SearchPreset::getId, presetId)
                        .eq(SearchPreset::getOwnerId, userId)
                        .set(SearchPreset::getIsPinned, 0)
                        .set(SearchPreset::getPinnedAt, null)
                        .set(SearchPreset::getUpdatedAt, now)
        );

        preset.setIsPinned(0);
        preset.setPinnedAt(null);
        preset.setUpdatedAt(now);

        auditService.record(userId, "SEARCH_PRESET_UNPIN", "preset=" + presetId, ipAddress);
        return toVo(preset);
    }

    @Transactional
    public SearchPresetVo use(Long userId, Long presetId, String ipAddress) {
        SearchPreset preset = loadPreset(userId, presetId);
        LocalDateTime now = LocalDateTime.now();
        int nextUsageCount = (preset.getUsageCount() == null ? 0 : preset.getUsageCount()) + 1;

        searchPresetMapper.update(
                null,
                new LambdaUpdateWrapper<SearchPreset>()
                        .eq(SearchPreset::getId, presetId)
                        .eq(SearchPreset::getOwnerId, userId)
                        .set(SearchPreset::getUsageCount, nextUsageCount)
                        .set(SearchPreset::getLastUsedAt, now)
                        .set(SearchPreset::getUpdatedAt, now)
        );

        preset.setUsageCount(nextUsageCount);
        preset.setLastUsedAt(now);
        preset.setUpdatedAt(now);

        auditService.record(userId, "SEARCH_PRESET_USE", "preset=" + presetId, ipAddress);
        return toVo(preset);
    }

    @Transactional
    public void delete(Long userId, Long presetId, String ipAddress) {
        SearchPreset preset = loadPreset(userId, presetId);
        searchPresetMapper.deleteById(presetId);
        auditService.record(userId, "SEARCH_PRESET_DELETE", "name=" + preset.getName(), ipAddress);
    }

    private SearchPreset loadPreset(Long userId, Long presetId) {
        SearchPreset preset = searchPresetMapper.selectOne(new LambdaQueryWrapper<SearchPreset>()
                .eq(SearchPreset::getId, presetId)
                .eq(SearchPreset::getOwnerId, userId));
        if (preset == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Saved search does not exist");
        }
        return preset;
    }

    private void ensureNameUnique(Long userId, String name, Long excludePresetId) {
        LambdaQueryWrapper<SearchPreset> query = new LambdaQueryWrapper<SearchPreset>()
                .eq(SearchPreset::getOwnerId, userId)
                .eq(SearchPreset::getName, name);
        if (excludePresetId != null) {
            query.ne(SearchPreset::getId, excludePresetId);
        }
        SearchPreset exists = searchPresetMapper.selectOne(query);
        if (exists != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Saved search name already exists");
        }
    }

    private SearchPresetVo toVo(SearchPreset preset) {
        return new SearchPresetVo(
                String.valueOf(preset.getId()),
                preset.getName(),
                preset.getKeyword(),
                preset.getFolder(),
                toVoBoolean(preset.getUnread()),
                toVoBoolean(preset.getStarred()),
                preset.getFromAt(),
                preset.getToAt(),
                preset.getLabelName(),
                preset.getIsPinned() != null && preset.getIsPinned() == 1,
                preset.getPinnedAt(),
                preset.getUsageCount() == null ? 0 : preset.getUsageCount(),
                preset.getLastUsedAt()
        );
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Saved search name is required");
        }
        return name.trim();
    }

    private PresetFields normalizeFields(
            String keyword,
            String folder,
            Boolean unread,
            Boolean starred,
            String from,
            String to,
            String label
    ) {
        LocalDateTime fromAt = parseSearchTime(from, "from");
        LocalDateTime toAt = parseSearchTime(to, "to");
        if (fromAt != null && toAt != null && fromAt.isAfter(toAt)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "`from` must be earlier than `to`");
        }

        return new PresetFields(
                normalizeNullable(keyword),
                normalizeFolderNullable(folder),
                toDbBoolean(unread),
                toDbBoolean(starred),
                fromAt,
                toAt,
                normalizeNullable(label)
        );
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeFolderNullable(String folder) {
        if (!StringUtils.hasText(folder)) {
            return null;
        }
        String normalized = folder.trim().toUpperCase();
        if (!SUPPORTED_FOLDERS.contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported folder");
        }
        return normalized;
    }

    private LocalDateTime parseSearchTime(String value, String field) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid `%s` datetime format".formatted(field));
        }
    }

    private Integer toDbBoolean(Boolean value) {
        if (value == null) {
            return null;
        }
        return value ? 1 : 0;
    }

    private Boolean toVoBoolean(Integer value) {
        if (value == null) {
            return null;
        }
        return value == 1;
    }

    private record PresetFields(
            String keyword,
            String folder,
            Integer unread,
            Integer starred,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            String label
    ) {
    }
}
