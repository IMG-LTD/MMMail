package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.MailFilterMapper;
import com.mmmail.server.mapper.MailFolderMapper;
import com.mmmail.server.mapper.MailMessageMapper;
import com.mmmail.server.model.dto.CreateMailFolderRequest;
import com.mmmail.server.model.dto.UpdateMailFolderRequest;
import com.mmmail.server.model.entity.MailFilter;
import com.mmmail.server.model.entity.MailFolder;
import com.mmmail.server.model.entity.MailMessage;
import com.mmmail.server.model.vo.MailFolderNodeVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MailFolderService {

    private static final String CUSTOM_FOLDER_TYPE = "CUSTOM";
    private static final String DEFAULT_FOLDER_COLOR = "#5B7CFA";
    private static final long ROOT_PARENT_KEY = 0L;

    private final MailFolderMapper mailFolderMapper;
    private final MailMessageMapper mailMessageMapper;
    private final MailFilterMapper mailFilterMapper;
    private final AuditService auditService;

    public MailFolderService(
            MailFolderMapper mailFolderMapper,
            MailMessageMapper mailMessageMapper,
            MailFilterMapper mailFilterMapper,
            AuditService auditService
    ) {
        this.mailFolderMapper = mailFolderMapper;
        this.mailMessageMapper = mailMessageMapper;
        this.mailFilterMapper = mailFilterMapper;
        this.auditService = auditService;
    }

    public List<MailFolderNodeVo> list(Long userId, String ipAddress) {
        List<MailFolderNodeVo> snapshot = snapshot(userId);
        auditService.record(userId, "MAIL_FOLDER_LIST", "count=" + snapshot.size(), ipAddress);
        return snapshot;
    }

    @Transactional
    public MailFolderNodeVo create(Long userId, CreateMailFolderRequest request, String ipAddress) {
        Long parentId = validateParent(userId, request.parentId());
        String name = normalizeName(request.name());
        assertNameAvailable(userId, parentId, name, null);

        MailFolder folder = new MailFolder();
        LocalDateTime now = LocalDateTime.now();
        folder.setOwnerId(userId);
        folder.setParentId(parentId);
        folder.setName(name);
        folder.setColor(normalizeColor(request.color()));
        folder.setNotificationsEnabled(toFlag(request.notificationsEnabled(), true));
        folder.setCreatedAt(now);
        folder.setUpdatedAt(now);
        folder.setDeleted(0);
        mailFolderMapper.insert(folder);

        auditService.record(userId, "MAIL_FOLDER_CREATE", detail(folder), ipAddress);
        return snapshotNode(userId, folder.getId());
    }

    @Transactional
    public MailFolderNodeVo update(Long userId, Long folderId, UpdateMailFolderRequest request, String ipAddress) {
        MailFolder folder = loadOwnedFolder(userId, folderId);
        String name = normalizeName(request.name());
        assertNameAvailable(userId, folder.getParentId(), name, folder.getId());

        folder.setName(name);
        folder.setColor(normalizeColor(request.color()));
        folder.setNotificationsEnabled(toFlag(request.notificationsEnabled(), folder.getNotificationsEnabled() == null
                || folder.getNotificationsEnabled() == 1));
        folder.setUpdatedAt(LocalDateTime.now());
        mailFolderMapper.updateById(folder);

        auditService.record(userId, "MAIL_FOLDER_UPDATE", detail(folder), ipAddress);
        return snapshotNode(userId, folder.getId());
    }

    @Transactional
    public void delete(Long userId, Long folderId, String ipAddress) {
        MailFolder folder = loadOwnedFolder(userId, folderId);
        assertNoChildFolders(userId, folderId);
        assertNoAttachedFilters(userId, folderId);
        assertNoFolderMails(userId, folderId);
        mailFolderMapper.deleteById(folderId);
        auditService.record(userId, "MAIL_FOLDER_DELETE", detail(folder), ipAddress);
    }

    public MailFolder requireOwnedFolder(Long userId, Long folderId) {
        return loadOwnedFolder(userId, folderId);
    }

    public MailFolderReference resolveOwnedFolderReference(Long userId, Long folderId) {
        if (folderId == null) {
            return null;
        }
        MailFolder folder = loadOwnedFolder(userId, folderId);
        return new MailFolderReference(folder.getId(), folder.getName());
    }

    public Map<Long, MailFolderReference> resolveFolderRefs(Long userId, Collection<Long> folderIds) {
        List<Long> ids = folderIds == null ? List.of() : folderIds.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return mailFolderMapper.selectList(new LambdaQueryWrapper<MailFolder>()
                        .eq(MailFolder::getOwnerId, userId)
                        .in(MailFolder::getId, ids))
                .stream()
                .collect(LinkedHashMap::new,
                        (map, folder) -> map.put(folder.getId(), new MailFolderReference(folder.getId(), folder.getName())),
                        Map::putAll);
    }

    private List<MailFolderNodeVo> snapshot(Long userId) {
        List<MailFolder> folders = loadFolders(userId);
        Map<Long, FolderStats> statsByFolder = buildFolderStats(userId, folders);
        Map<Long, List<MailFolder>> childrenByParent = groupByParent(folders);
        return buildTree(ROOT_PARENT_KEY, childrenByParent, statsByFolder);
    }

    private MailFolderNodeVo snapshotNode(Long userId, Long folderId) {
        return findNode(snapshot(userId), folderId);
    }

    private MailFolderNodeVo findNode(List<MailFolderNodeVo> nodes, Long folderId) {
        for (MailFolderNodeVo node : nodes) {
            if (String.valueOf(folderId).equals(node.id())) {
                return node;
            }
            MailFolderNodeVo child = findNode(node.children(), folderId);
            if (child != null) {
                return child;
            }
        }
        throw new BizException(ErrorCode.MAIL_NOT_FOUND, "Mail folder does not exist");
    }

    private List<MailFolderNodeVo> buildTree(
            Long parentKey,
            Map<Long, List<MailFolder>> childrenByParent,
            Map<Long, FolderStats> statsByFolder
    ) {
        List<MailFolder> children = childrenByParent.getOrDefault(parentKey, List.of());
        List<MailFolderNodeVo> nodes = new ArrayList<>(children.size());
        for (MailFolder folder : children) {
            FolderStats stats = statsByFolder.getOrDefault(folder.getId(), FolderStats.EMPTY);
            List<MailFolderNodeVo> descendants = buildTree(folder.getId(), childrenByParent, statsByFolder);
            nodes.add(new MailFolderNodeVo(
                    String.valueOf(folder.getId()),
                    folder.getParentId() == null ? null : String.valueOf(folder.getParentId()),
                    folder.getName(),
                    folder.getColor(),
                    folder.getNotificationsEnabled() == null || folder.getNotificationsEnabled() == 1,
                    stats.unreadCount(),
                    stats.totalCount(),
                    folder.getUpdatedAt(),
                    descendants
            ));
        }
        return nodes;
    }

    private Map<Long, List<MailFolder>> groupByParent(List<MailFolder> folders) {
        Map<Long, List<MailFolder>> childrenByParent = new LinkedHashMap<>();
        for (MailFolder folder : folders) {
            Long parentKey = folder.getParentId() == null ? ROOT_PARENT_KEY : folder.getParentId();
            childrenByParent.computeIfAbsent(parentKey, ignored -> new ArrayList<>()).add(folder);
        }
        return childrenByParent;
    }

    private Map<Long, FolderStats> buildFolderStats(Long userId, List<MailFolder> folders) {
        List<Long> folderIds = folders.stream().map(MailFolder::getId).toList();
        if (folderIds.isEmpty()) {
            return Map.of();
        }
        List<MailMessage> mails = mailMessageMapper.selectList(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getFolderType, CUSTOM_FOLDER_TYPE)
                .in(MailMessage::getCustomFolderId, folderIds));
        Map<Long, FolderStats> statsByFolder = new LinkedHashMap<>();
        for (MailMessage mail : mails) {
            if (mail.getCustomFolderId() == null) {
                continue;
            }
            FolderStats current = statsByFolder.getOrDefault(mail.getCustomFolderId(), FolderStats.EMPTY);
            statsByFolder.put(mail.getCustomFolderId(), current.add(mail.getIsRead() != null && mail.getIsRead() == 0));
        }
        return statsByFolder;
    }

    private List<MailFolder> loadFolders(Long userId) {
        return mailFolderMapper.selectList(new LambdaQueryWrapper<MailFolder>()
                .eq(MailFolder::getOwnerId, userId)
                .orderByAsc(MailFolder::getParentId)
                .orderByAsc(MailFolder::getName)
                .orderByDesc(MailFolder::getUpdatedAt));
    }

    private MailFolder loadOwnedFolder(Long userId, Long folderId) {
        MailFolder folder = mailFolderMapper.selectOne(new LambdaQueryWrapper<MailFolder>()
                .eq(MailFolder::getOwnerId, userId)
                .eq(MailFolder::getId, folderId)
                .last("limit 1"));
        if (folder == null) {
            throw new BizException(ErrorCode.MAIL_NOT_FOUND, "Mail folder does not exist");
        }
        return folder;
    }

    private Long validateParent(Long userId, Long parentId) {
        if (parentId == null) {
            return null;
        }
        MailFolder parent = loadOwnedFolder(userId, parentId);
        if (parent.getParentId() != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Only one level of subfolders is supported");
        }
        return parent.getId();
    }

    private void assertNameAvailable(Long userId, Long parentId, String name, Long excludeFolderId) {
        LambdaQueryWrapper<MailFolder> query = new LambdaQueryWrapper<MailFolder>()
                .eq(MailFolder::getOwnerId, userId)
                .eq(MailFolder::getName, name);
        if (parentId == null) {
            query.isNull(MailFolder::getParentId);
        } else {
            query.eq(MailFolder::getParentId, parentId);
        }
        if (excludeFolderId != null) {
            query.ne(MailFolder::getId, excludeFolderId);
        }
        MailFolder existing = mailFolderMapper.selectOne(query.last("limit 1"));
        if (existing != null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail folder name already exists");
        }
    }

    private void assertNoChildFolders(Long userId, Long folderId) {
        Long childCount = mailFolderMapper.selectCount(new LambdaQueryWrapper<MailFolder>()
                .eq(MailFolder::getOwnerId, userId)
                .eq(MailFolder::getParentId, folderId));
        if (childCount != null && childCount > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail folder has subfolders and cannot be deleted");
        }
    }

    private void assertNoAttachedFilters(Long userId, Long folderId) {
        Long filterCount = mailFilterMapper.selectCount(new LambdaQueryWrapper<MailFilter>()
                .eq(MailFilter::getOwnerId, userId)
                .eq(MailFilter::getTargetCustomFolderId, folderId));
        if (filterCount != null && filterCount > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail folder is used by mail filters");
        }
    }

    private void assertNoFolderMails(Long userId, Long folderId) {
        Long mailCount = mailMessageMapper.selectCount(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getFolderType, CUSTOM_FOLDER_TYPE)
                .eq(MailMessage::getCustomFolderId, folderId));
        if (mailCount != null && mailCount > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail folder still contains mails");
        }
    }

    private String normalizeName(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail folder name is required");
        }
        return rawValue.trim();
    }

    private String normalizeColor(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return DEFAULT_FOLDER_COLOR;
        }
        return rawValue.trim().toUpperCase();
    }

    private Integer toFlag(Boolean value, boolean defaultValue) {
        boolean resolved = value == null ? defaultValue : value;
        return resolved ? 1 : 0;
    }

    private String detail(MailFolder folder) {
        return "folderId=" + folder.getId()
                + ",parentId=" + (folder.getParentId() == null ? "-" : folder.getParentId())
                + ",name=" + folder.getName();
    }

    public record MailFolderReference(Long id, String name) {
    }

    private record FolderStats(long unreadCount, long totalCount) {
        private static final FolderStats EMPTY = new FolderStats(0, 0);

        private FolderStats add(boolean unread) {
            return new FolderStats(unread ? unreadCount + 1 : unreadCount, totalCount + 1);
        }
    }
}
