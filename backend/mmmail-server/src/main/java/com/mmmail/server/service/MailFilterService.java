package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.MailFilterMapper;
import com.mmmail.server.mapper.MailLabelMapper;
import com.mmmail.server.model.dto.CreateMailFilterRequest;
import com.mmmail.server.model.dto.UpdateMailFilterRequest;
import com.mmmail.server.model.entity.MailFilter;
import com.mmmail.server.model.entity.MailLabel;
import com.mmmail.server.model.vo.MailFilterVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class MailFilterService {

    private static final TypeReference<List<String>> LABEL_TYPE = new TypeReference<>() {
    };
    private static final Set<String> SUPPORTED_TARGET_FOLDERS = Set.of("INBOX", "ARCHIVE", "SPAM", "TRASH");

    private final MailFilterMapper mailFilterMapper;
    private final MailLabelMapper mailLabelMapper;
    private final MailFolderService mailFolderService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public MailFilterService(
            MailFilterMapper mailFilterMapper,
            MailLabelMapper mailLabelMapper,
            MailFolderService mailFolderService,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.mailFilterMapper = mailFilterMapper;
        this.mailLabelMapper = mailLabelMapper;
        this.mailFolderService = mailFolderService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public List<MailFilterVo> list(Long userId) {
        List<MailFilter> filters = loadFilters(userId);
        Map<Long, MailFolderService.MailFolderReference> folderRefs = resolveFolderRefs(userId, filters);
        return filters.stream()
                .map(filter -> toVo(filter, lookupCustomFolderRef(folderRefs, filter.getTargetCustomFolderId())))
                .toList();
    }

    @Transactional
    public MailFilterVo create(Long userId, CreateMailFilterRequest request, String ipAddress) {
        NormalizedMailFilter normalized = normalizeRequest(
                userId,
                request.name(),
                request.senderContains(),
                request.subjectContains(),
                request.keywordContains(),
                request.targetFolder(),
                request.targetCustomFolderId(),
                request.labels(),
                request.markRead(),
                request.enabled()
        );
        ensureNameUnique(userId, normalized.name(), null);

        MailFilter filter = new MailFilter();
        LocalDateTime now = LocalDateTime.now();
        filter.setOwnerId(userId);
        filter.setCreatedAt(now);
        filter.setDeleted(0);
        applyNormalizedFields(filter, normalized, now);
        mailFilterMapper.insert(filter);

        auditService.record(userId, "MAIL_FILTER_CREATE", "filter=" + normalized.name(), ipAddress);
        return toVo(filter, normalized.targetCustomFolder());
    }

    @Transactional
    public MailFilterVo update(Long userId, Long filterId, UpdateMailFilterRequest request, String ipAddress) {
        MailFilter filter = loadFilter(userId, filterId);
        NormalizedMailFilter normalized = normalizeRequest(
                userId,
                request.name(),
                request.senderContains(),
                request.subjectContains(),
                request.keywordContains(),
                request.targetFolder(),
                request.targetCustomFolderId(),
                request.labels(),
                request.markRead(),
                request.enabled()
        );
        ensureNameUnique(userId, normalized.name(), filterId);

        applyNormalizedFields(filter, normalized, LocalDateTime.now());
        mailFilterMapper.updateById(filter);

        auditService.record(userId, "MAIL_FILTER_UPDATE", "filter=" + filterId, ipAddress);
        return toVo(filter, normalized.targetCustomFolder());
    }

    @Transactional
    public void delete(Long userId, Long filterId, String ipAddress) {
        MailFilter filter = loadFilter(userId, filterId);
        mailFilterMapper.deleteById(filterId);
        auditService.record(userId, "MAIL_FILTER_DELETE", "filter=" + filter.getName(), ipAddress);
    }

    public MailFilterMatch evaluateFirstMatch(Long userId, String senderEmail, String subject, String body) {
        String normalizedSender = normalizeMatchText(senderEmail);
        String normalizedSubject = normalizeMatchText(subject);
        String normalizedBody = normalizeMatchText(body);
        List<MailFilter> filters = mailFilterMapper.selectList(new LambdaQueryWrapper<MailFilter>()
                .eq(MailFilter::getOwnerId, userId)
                .eq(MailFilter::getEnabled, 1)
                .orderByAsc(MailFilter::getCreatedAt)
                .orderByAsc(MailFilter::getId));
        Map<Long, MailFolderService.MailFolderReference> folderRefs = resolveFolderRefs(userId, filters);
        for (MailFilter filter : filters) {
            if (!matches(filter, normalizedSender, normalizedSubject, normalizedBody)) {
                continue;
            }
            MailFolderService.MailFolderReference customFolder =
                    lookupCustomFolderRef(folderRefs, filter.getTargetCustomFolderId());
            return new MailFilterMatch(
                    filter.getId(),
                    filter.getName(),
                    filter.getTargetFolder(),
                    customFolder == null ? null : customFolder.id(),
                    customFolder == null ? null : customFolder.name(),
                    parseLabels(filter.getLabelsJson()),
                    filter.getMarkRead() != null && filter.getMarkRead() == 1
            );
        }
        return null;
    }

    public void assertLabelNotUsed(Long userId, String labelName) {
        if (!StringUtils.hasText(labelName)) {
            return;
        }
        Long count = mailFilterMapper.selectCount(new LambdaQueryWrapper<MailFilter>()
                .eq(MailFilter::getOwnerId, userId)
                .like(MailFilter::getLabelsJson, "\"" + labelName.trim() + "\""));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Label is used by a mail filter");
        }
    }

    private List<MailFilter> loadFilters(Long userId) {
        return mailFilterMapper.selectList(new LambdaQueryWrapper<MailFilter>()
                .eq(MailFilter::getOwnerId, userId)
                .orderByDesc(MailFilter::getUpdatedAt)
                .orderByDesc(MailFilter::getCreatedAt));
    }

    private Map<Long, MailFolderService.MailFolderReference> resolveFolderRefs(Long userId, List<MailFilter> filters) {
        return mailFolderService.resolveFolderRefs(
                userId,
                filters.stream().map(MailFilter::getTargetCustomFolderId).toList()
        );
    }

    private MailFolderService.MailFolderReference lookupCustomFolderRef(
            Map<Long, MailFolderService.MailFolderReference> folderRefs,
            Long folderId
    ) {
        if (folderId == null || folderRefs.isEmpty()) {
            return null;
        }
        return folderRefs.get(folderId);
    }

    private void applyNormalizedFields(MailFilter filter, NormalizedMailFilter normalized, LocalDateTime now) {
        filter.setName(normalized.name());
        filter.setSenderContains(normalized.senderContains());
        filter.setSubjectContains(normalized.subjectContains());
        filter.setKeywordContains(normalized.keywordContains());
        filter.setTargetFolder(normalized.targetFolder());
        filter.setTargetCustomFolderId(normalized.targetCustomFolder() == null ? null : normalized.targetCustomFolder().id());
        filter.setLabelsJson(serializeLabels(normalized.labels()));
        filter.setMarkRead(normalized.markRead() ? 1 : 0);
        filter.setEnabled(normalized.enabled() ? 1 : 0);
        filter.setUpdatedAt(now);
    }

    private MailFilter loadFilter(Long userId, Long filterId) {
        MailFilter filter = mailFilterMapper.selectOne(new LambdaQueryWrapper<MailFilter>()
                .eq(MailFilter::getId, filterId)
                .eq(MailFilter::getOwnerId, userId)
                .last("limit 1"));
        if (filter == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail filter does not exist");
        }
        return filter;
    }

    private void ensureNameUnique(Long userId, String name, Long excludeFilterId) {
        LambdaQueryWrapper<MailFilter> query = new LambdaQueryWrapper<MailFilter>()
                .eq(MailFilter::getOwnerId, userId)
                .eq(MailFilter::getName, name);
        if (excludeFilterId != null) {
            query.ne(MailFilter::getId, excludeFilterId);
        }
        MailFilter existing = mailFilterMapper.selectOne(query.last("limit 1"));
        if (existing != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail filter name already exists");
        }
    }

    private NormalizedMailFilter normalizeRequest(
            Long userId,
            String name,
            String senderContains,
            String subjectContains,
            String keywordContains,
            String targetFolder,
            Long targetCustomFolderId,
            List<String> labels,
            Boolean markRead,
            Boolean enabled
    ) {
        String normalizedName = normalizeRequired(name, "Mail filter name is required");
        String normalizedSender = normalizeOptional(senderContains);
        String normalizedSubject = normalizeOptional(subjectContains);
        String normalizedKeyword = normalizeOptional(keywordContains);
        String normalizedFolder = normalizeTargetFolder(targetFolder);
        MailFolderService.MailFolderReference customFolder = resolveTargetCustomFolder(userId, targetCustomFolderId);
        List<String> normalizedLabels = normalizeLabels(userId, labels);
        boolean effectiveMarkRead = Boolean.TRUE.equals(markRead);
        boolean effectiveEnabled = enabled == null || enabled;

        assertSingleTarget(normalizedFolder, customFolder);
        if (!hasAnyCondition(normalizedSender, normalizedSubject, normalizedKeyword)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "At least one filter condition is required");
        }
        if (!hasAnyAction(normalizedFolder, customFolder, normalizedLabels, effectiveMarkRead)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "At least one filter action is required");
        }

        return new NormalizedMailFilter(
                normalizedName,
                lowercase(normalizedSender),
                lowercase(normalizedSubject),
                lowercase(normalizedKeyword),
                normalizedFolder,
                customFolder,
                normalizedLabels,
                effectiveMarkRead,
                effectiveEnabled
        );
    }

    private void assertSingleTarget(String targetFolder, MailFolderService.MailFolderReference customFolder) {
        if (StringUtils.hasText(targetFolder) && customFolder != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Only one filter target folder is allowed");
        }
    }

    private MailFolderService.MailFolderReference resolveTargetCustomFolder(Long userId, Long targetCustomFolderId) {
        if (targetCustomFolderId == null) {
            return null;
        }
        return mailFolderService.resolveOwnedFolderReference(userId, targetCustomFolderId);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeTargetFolder(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_TARGET_FOLDERS.contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported mail filter target folder");
        }
        return normalized;
    }

    private List<String> normalizeLabels(Long userId, List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return List.of();
        }
        List<String> normalized = labels.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (normalized.size() > 20) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Too many labels");
        }
        Set<String> existing = mailLabelMapper.selectList(new LambdaQueryWrapper<MailLabel>()
                        .eq(MailLabel::getOwnerId, userId)
                        .in(MailLabel::getName, normalized)
                        .orderByAsc(MailLabel::getName))
                .stream()
                .map(MailLabel::getName)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        if (existing.size() != normalized.size()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail filter labels contain unknown items");
        }
        return normalized;
    }

    private boolean hasAnyCondition(String senderContains, String subjectContains, String keywordContains) {
        return StringUtils.hasText(senderContains)
                || StringUtils.hasText(subjectContains)
                || StringUtils.hasText(keywordContains);
    }

    private boolean hasAnyAction(
            String targetFolder,
            MailFolderService.MailFolderReference customFolder,
            List<String> labels,
            boolean markRead
    ) {
        return StringUtils.hasText(targetFolder) || customFolder != null || !labels.isEmpty() || markRead;
    }

    private boolean matches(
            MailFilter filter,
            String normalizedSender,
            String normalizedSubject,
            String normalizedBody
    ) {
        return matchesContains(normalizedSender, filter.getSenderContains())
                && matchesContains(normalizedSubject, filter.getSubjectContains())
                && matchesKeyword(normalizedSubject, normalizedBody, filter.getKeywordContains());
    }

    private boolean matchesContains(String haystack, String needle) {
        if (!StringUtils.hasText(needle)) {
            return true;
        }
        return haystack.contains(needle);
    }

    private boolean matchesKeyword(String subject, String body, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return subject.contains(keyword) || body.contains(keyword);
    }

    private String normalizeMatchText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String lowercase(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private String serializeLabels(List<String> labels) {
        try {
            return objectMapper.writeValueAsString(labels == null ? List.of() : labels);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to serialize mail filter labels");
        }
    }

    private List<String> parseLabels(String labelsJson) {
        if (!StringUtils.hasText(labelsJson)) {
            return List.of();
        }
        try {
            List<String> labels = objectMapper.readValue(labelsJson, LABEL_TYPE);
            return labels == null ? List.of() : labels;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private MailFilterVo toVo(MailFilter filter, MailFolderService.MailFolderReference customFolder) {
        return new MailFilterVo(
                String.valueOf(filter.getId()),
                filter.getName(),
                filter.getEnabled() != null && filter.getEnabled() == 1,
                filter.getSenderContains(),
                filter.getSubjectContains(),
                filter.getKeywordContains(),
                filter.getTargetFolder(),
                customFolder == null ? null : String.valueOf(customFolder.id()),
                customFolder == null ? null : customFolder.name(),
                parseLabels(filter.getLabelsJson()),
                filter.getMarkRead() != null && filter.getMarkRead() == 1,
                filter.getCreatedAt(),
                filter.getUpdatedAt()
        );
    }

    public record MailFilterMatch(
            Long filterId,
            String filterName,
            String targetFolder,
            Long targetCustomFolderId,
            String targetCustomFolderName,
            List<String> labels,
            boolean markRead
    ) {
    }

    private record NormalizedMailFilter(
            String name,
            String senderContains,
            String subjectContains,
            String keywordContains,
            String targetFolder,
            MailFolderService.MailFolderReference targetCustomFolder,
            List<String> labels,
            boolean markRead,
            boolean enabled
    ) {
    }
}
