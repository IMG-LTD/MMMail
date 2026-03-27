package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.DriveFileVersionMapper;
import com.mmmail.server.mapper.DriveItemMapper;
import com.mmmail.server.mapper.DriveShareLinkMapper;
import com.mmmail.server.mapper.OrgTeamSpaceMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.entity.DriveFileVersion;
import com.mmmail.server.model.entity.DriveItem;
import com.mmmail.server.model.entity.OrgTeamSpace;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.OrgTeamSpaceFileVersionVo;
import com.mmmail.server.model.vo.OrgTeamSpaceItemVo;
import com.mmmail.server.model.vo.OrgTeamSpaceTrashItemVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrgTeamSpaceFileService {

    private static final String ITEM_TYPE_FOLDER = "FOLDER";
    private static final String ITEM_TYPE_FILE = "FILE";
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_RECYCLE_BIN_RETENTION_DAYS = 30;
    private static final String TEAM_SPACE_TOKEN = "teamSpaceId=";

    private final OrgTeamSpaceGovernanceService orgTeamSpaceGovernanceService;
    private final OrgTeamSpaceMapper orgTeamSpaceMapper;
    private final DriveItemMapper driveItemMapper;
    private final DriveFileVersionMapper driveFileVersionMapper;
    private final DriveShareLinkMapper driveShareLinkMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditService auditService;

    @Value("${mmmail.drive.storage-root:${java.io.tmpdir}/mmmail-drive}")
    private String driveStorageRoot;
    @Value("${mmmail.drive.recycle-bin.retention-days:30}")
    private Integer recycleBinRetentionDays;

    public OrgTeamSpaceFileService(
            OrgTeamSpaceGovernanceService orgTeamSpaceGovernanceService,
            OrgTeamSpaceMapper orgTeamSpaceMapper,
            DriveItemMapper driveItemMapper,
            DriveFileVersionMapper driveFileVersionMapper,
            DriveShareLinkMapper driveShareLinkMapper,
            UserAccountMapper userAccountMapper,
            AuditService auditService
    ) {
        this.orgTeamSpaceGovernanceService = orgTeamSpaceGovernanceService;
        this.orgTeamSpaceMapper = orgTeamSpaceMapper;
        this.driveItemMapper = driveItemMapper;
        this.driveFileVersionMapper = driveFileVersionMapper;
        this.driveShareLinkMapper = driveShareLinkMapper;
        this.userAccountMapper = userAccountMapper;
        this.auditService = auditService;
    }

    public List<OrgTeamSpaceFileVersionVo> listFileVersions(Long userId, Long orgId, Long teamSpaceId, Long itemId, Integer limit) {
        orgTeamSpaceGovernanceService.requireReadAccess(userId, orgId, teamSpaceId);
        loadActiveFile(teamSpaceId, itemId);
        List<DriveFileVersion> versions = driveFileVersionMapper.selectList(new LambdaQueryWrapper<DriveFileVersion>()
                .eq(DriveFileVersion::getItemId, itemId)
                .orderByDesc(DriveFileVersion::getVersionNo)
                .orderByDesc(DriveFileVersion::getCreatedAt)
                .last("limit " + normalizeLimit(limit)));
        Map<Long, String> emailMap = loadUserEmailMap(versions.stream().map(DriveFileVersion::getOwnerId).collect(Collectors.toSet()));
        return versions.stream().map(version -> toVersionVo(version, emailMap)).toList();
    }

    @Transactional
    public OrgTeamSpaceItemVo uploadFileVersion(
            Long userId,
            Long orgId,
            Long teamSpaceId,
            Long itemId,
            MultipartFile file,
            String ipAddress
    ) {
        orgTeamSpaceGovernanceService.requireWriteAccess(userId, orgId, teamSpaceId);
        DriveItem currentItem = loadActiveFile(teamSpaceId, itemId);
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "file is required");
        }
        byte[] content = readUploadBytes(file);
        if (content.length <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "file is required");
        }
        OrgTeamSpace teamSpace = loadTeamSpace(orgId, teamSpaceId);
        assertTeamSpaceQuota(teamSpace, content.length - safeLong(currentItem.getSizeBytes()));
        snapshotCurrentItemAsVersion(currentItem);
        currentItem.setOwnerId(userId);
        currentItem.setMimeType(normalizeNullableText(file.getContentType(), 128));
        currentItem.setSizeBytes((long) content.length);
        currentItem.setStoragePath(storeUploadedFile(teamSpaceId, currentItem.getName(), content));
        currentItem.setChecksum(sha256(content));
        currentItem.setUpdatedAt(LocalDateTime.now());
        driveItemMapper.updateById(currentItem);
        auditService.record(userId, "ORG_TEAM_SPACE_FILE_VERSION_UPLOAD", detail(teamSpaceId, "itemId=" + itemId + ",sizeBytes=" + content.length), ipAddress, orgId);
        return toItemVo(currentItem);
    }

    @Transactional
    public OrgTeamSpaceItemVo restoreFileVersion(
            Long userId,
            Long orgId,
            Long teamSpaceId,
            Long itemId,
            Long versionId,
            String ipAddress
    ) {
        orgTeamSpaceGovernanceService.requireManagerAccess(userId, orgId, teamSpaceId);
        DriveItem currentItem = loadActiveFile(teamSpaceId, itemId);
        DriveFileVersion targetVersion = driveFileVersionMapper.selectOne(new LambdaQueryWrapper<DriveFileVersion>()
                .eq(DriveFileVersion::getId, versionId)
                .eq(DriveFileVersion::getItemId, itemId)
                .last("limit 1"));
        if (targetVersion == null || !StringUtils.hasText(targetVersion.getStoragePath()) || !isPathWithinStorageRoot(targetVersion.getStoragePath())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Team space file version is not found");
        }
        snapshotCurrentItemAsVersion(currentItem);
        currentItem.setOwnerId(userId);
        currentItem.setMimeType(targetVersion.getMimeType());
        currentItem.setSizeBytes(targetVersion.getSizeBytes());
        currentItem.setStoragePath(targetVersion.getStoragePath());
        currentItem.setChecksum(targetVersion.getChecksum());
        currentItem.setUpdatedAt(LocalDateTime.now());
        driveItemMapper.updateById(currentItem);
        auditService.record(userId, "ORG_TEAM_SPACE_FILE_VERSION_RESTORE", detail(teamSpaceId, "itemId=" + itemId + ",versionId=" + versionId), ipAddress, orgId);
        return toItemVo(currentItem);
    }

    @Transactional
    public void deleteItem(Long userId, Long orgId, Long teamSpaceId, Long itemId, String ipAddress) {
        orgTeamSpaceGovernanceService.requireWriteAccess(userId, orgId, teamSpaceId);
        OrgTeamSpace teamSpace = loadTeamSpace(orgId, teamSpaceId);
        DriveItem item = loadActiveItem(teamSpaceId, itemId);
        if (teamSpace.getRootItemId() != null && teamSpace.getRootItemId().equals(itemId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Team space root cannot be deleted");
        }
        if (ITEM_TYPE_FOLDER.equals(item.getItemType()) && driveItemMapper.countChildrenByTeamSpace(teamSpaceId, itemId) > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Cannot delete non-empty folder");
        }
        LocalDateTime now = LocalDateTime.now();
        item.setTrashedAt(now);
        item.setPurgeAfterAt(now.plusDays(effectiveRecycleBinRetentionDays()));
        item.setUpdatedAt(now);
        driveItemMapper.updateById(item);
        driveShareLinkMapper.purgeByItemId(itemId);
        driveItemMapper.deleteById(itemId);
        auditService.record(userId, "ORG_TEAM_SPACE_ITEM_DELETE", detail(teamSpaceId, "itemId=" + itemId + ",type=" + item.getItemType()), ipAddress, orgId);
    }

    public List<OrgTeamSpaceTrashItemVo> listTrashItems(Long userId, Long orgId, Long teamSpaceId, Integer limit) {
        orgTeamSpaceGovernanceService.requireManagerAccess(userId, orgId, teamSpaceId);
        List<DriveItem> items = driveItemMapper.selectTrashedItemsByTeamSpace(teamSpaceId, normalizeLimit(limit));
        Map<Long, String> emailMap = loadUserEmailMap(items.stream().map(DriveItem::getOwnerId).collect(Collectors.toSet()));
        return items.stream().map(item -> toTrashItemVo(item, emailMap)).toList();
    }

    @Transactional
    public OrgTeamSpaceTrashItemVo restoreTrashItem(Long userId, Long orgId, Long teamSpaceId, Long itemId, String ipAddress) {
        orgTeamSpaceGovernanceService.requireManagerAccess(userId, orgId, teamSpaceId);
        DriveItem trashedItem = driveItemMapper.selectTrashedItemByTeamSpace(teamSpaceId, itemId);
        if (trashedItem == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Team space trash item is not found");
        }
        OrgTeamSpace teamSpace = loadTeamSpace(orgId, teamSpaceId);
        Long targetParentId = normalizeRestoreParentId(teamSpace, trashedItem.getParentId());
        ensureNameUnique(teamSpaceId, targetParentId, trashedItem.getItemType(), trashedItem.getName(), trashedItem.getId());
        int updated = driveItemMapper.restoreTrashedItemByTeamSpace(teamSpaceId, itemId, targetParentId);
        if (updated <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Team space trash item restore failed");
        }
        trashedItem.setParentId(targetParentId);
        trashedItem.setDeleted(0);
        trashedItem.setTrashedAt(null);
        trashedItem.setPurgeAfterAt(null);
        trashedItem.setUpdatedAt(LocalDateTime.now());
        auditService.record(userId, "ORG_TEAM_SPACE_ITEM_RESTORE", detail(teamSpaceId, "itemId=" + itemId + ",parentId=" + nullableId(targetParentId)), ipAddress, orgId);
        return toTrashItemVo(trashedItem, loadUserEmailMap(Set.of(trashedItem.getOwnerId())));
    }

    @Transactional
    public void purgeTrashItem(Long userId, Long orgId, Long teamSpaceId, Long itemId, String ipAddress) {
        orgTeamSpaceGovernanceService.requireManagerAccess(userId, orgId, teamSpaceId);
        DriveItem trashedItem = driveItemMapper.selectTrashedItemByTeamSpace(teamSpaceId, itemId);
        if (trashedItem == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Team space trash item is not found");
        }
        List<DriveFileVersion> versions = driveFileVersionMapper.selectList(new LambdaQueryWrapper<DriveFileVersion>()
                .eq(DriveFileVersion::getItemId, itemId));
        versions.forEach(version -> removeStoredFileIfExists(version.getStoragePath()));
        if (ITEM_TYPE_FILE.equals(trashedItem.getItemType())) {
            removeStoredFileIfExists(trashedItem.getStoragePath());
        }
        driveFileVersionMapper.purgeByItemId(itemId);
        driveShareLinkMapper.purgeByItemId(itemId);
        int deleted = driveItemMapper.deleteTrashedItemPermanentlyByTeamSpace(teamSpaceId, itemId);
        if (deleted <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Team space trash item purge failed");
        }
        auditService.record(userId, "ORG_TEAM_SPACE_ITEM_PURGE", detail(teamSpaceId, "itemId=" + itemId + ",type=" + trashedItem.getItemType()), ipAddress, orgId);
    }

    private OrgTeamSpace loadTeamSpace(Long orgId, Long teamSpaceId) {
        OrgTeamSpace teamSpace = orgTeamSpaceMapper.selectOne(new LambdaQueryWrapper<OrgTeamSpace>()
                .eq(OrgTeamSpace::getId, teamSpaceId)
                .eq(OrgTeamSpace::getOrgId, orgId)
                .last("limit 1"));
        if (teamSpace == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Team space is not found");
        }
        return teamSpace;
    }

    private DriveItem loadActiveItem(Long teamSpaceId, Long itemId) {
        DriveItem item = driveItemMapper.selectOne(new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getId, itemId)
                .eq(DriveItem::getTeamSpaceId, teamSpaceId)
                .last("limit 1"));
        if (item == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Team space item is not found");
        }
        return item;
    }

    private DriveItem loadActiveFile(Long teamSpaceId, Long itemId) {
        DriveItem item = loadActiveItem(teamSpaceId, itemId);
        if (!ITEM_TYPE_FILE.equals(item.getItemType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Only file item supports this operation");
        }
        return item;
    }

    private Long normalizeRestoreParentId(OrgTeamSpace teamSpace, Long parentId) {
        if (parentId == null) {
            return teamSpace.getRootItemId();
        }
        DriveItem parent = driveItemMapper.selectOne(new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getId, parentId)
                .eq(DriveItem::getTeamSpaceId, teamSpace.getId())
                .eq(DriveItem::getItemType, ITEM_TYPE_FOLDER)
                .eq(DriveItem::getDeleted, 0)
                .last("limit 1"));
        return parent == null ? teamSpace.getRootItemId() : parentId;
    }

    private void ensureNameUnique(Long teamSpaceId, Long parentId, String itemType, String name, Long excludeId) {
        LambdaQueryWrapper<DriveItem> query = new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getTeamSpaceId, teamSpaceId)
                .eq(DriveItem::getParentId, parentId)
                .eq(DriveItem::getItemType, itemType)
                .eq(DriveItem::getName, name);
        if (excludeId != null) {
            query.ne(DriveItem::getId, excludeId);
        }
        if (driveItemMapper.selectCount(query) > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Team space item with same name already exists in target folder");
        }
    }

    private OrgTeamSpaceFileVersionVo toVersionVo(DriveFileVersion version, Map<Long, String> emailMap) {
        return new OrgTeamSpaceFileVersionVo(
                String.valueOf(version.getId()),
                String.valueOf(version.getItemId()),
                version.getVersionNo() == null ? 0 : version.getVersionNo(),
                version.getMimeType(),
                safeLong(version.getSizeBytes()),
                version.getChecksum(),
                emailMap.get(version.getOwnerId()),
                version.getCreatedAt()
        );
    }

    private OrgTeamSpaceTrashItemVo toTrashItemVo(DriveItem item, Map<Long, String> emailMap) {
        return new OrgTeamSpaceTrashItemVo(
                String.valueOf(item.getId()),
                nullableId(item.getParentId()),
                item.getItemType(),
                item.getName(),
                item.getMimeType(),
                safeLong(item.getSizeBytes()),
                emailMap.get(item.getOwnerId()),
                item.getTrashedAt(),
                item.getPurgeAfterAt(),
                item.getUpdatedAt()
        );
    }

    private OrgTeamSpaceItemVo toItemVo(DriveItem item) {
        String ownerEmail = loadUserEmailMap(Set.of(item.getOwnerId())).get(item.getOwnerId());
        return new OrgTeamSpaceItemVo(
                String.valueOf(item.getId()),
                String.valueOf(item.getTeamSpaceId()),
                nullableId(item.getParentId()),
                item.getItemType(),
                item.getName(),
                item.getMimeType(),
                safeLong(item.getSizeBytes()),
                ownerEmail,
                item.getUpdatedAt()
        );
    }

    private Map<Long, String> loadUserEmailMap(Set<Long> userIds) {
        Set<Long> ids = userIds.stream().filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>().in(UserAccount::getId, ids))
                .stream()
                .collect(Collectors.toMap(UserAccount::getId, UserAccount::getEmail, (left, right) -> left, HashMap::new));
    }

    private void snapshotCurrentItemAsVersion(DriveItem currentItem) {
        if (!StringUtils.hasText(currentItem.getStoragePath()) || !isPathWithinStorageRoot(currentItem.getStoragePath())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file content is unavailable");
        }
        LocalDateTime now = LocalDateTime.now();
        DriveFileVersion version = new DriveFileVersion();
        version.setOwnerId(currentItem.getOwnerId());
        version.setItemId(currentItem.getId());
        version.setVersionNo(nextVersionNo(currentItem.getId()));
        version.setMimeType(currentItem.getMimeType());
        version.setSizeBytes(safeLong(currentItem.getSizeBytes()));
        version.setStoragePath(currentItem.getStoragePath());
        version.setChecksum(currentItem.getChecksum());
        version.setCreatedAt(now);
        version.setUpdatedAt(now);
        version.setDeleted(0);
        driveFileVersionMapper.insert(version);
    }

    private int nextVersionNo(Long itemId) {
        DriveFileVersion latest = driveFileVersionMapper.selectOne(new LambdaQueryWrapper<DriveFileVersion>()
                .eq(DriveFileVersion::getItemId, itemId)
                .orderByDesc(DriveFileVersion::getVersionNo)
                .last("limit 1"));
        if (latest == null || latest.getVersionNo() == null || latest.getVersionNo() < 1) {
            return 1;
        }
        return latest.getVersionNo() + 1;
    }

    private void assertTeamSpaceQuota(OrgTeamSpace teamSpace, long deltaBytes) {
        long currentBytes = safeLong(driveItemMapper.selectStorageBytesByTeamSpace(teamSpace.getId()));
        long storageLimitMb = teamSpace.getStorageLimitMb() == null ? 0L : teamSpace.getStorageLimitMb().longValue();
        long limitBytes = Math.max(1L, storageLimitMb) * 1024L * 1024L;
        if (currentBytes + Math.max(deltaBytes, 0L) > limitBytes) {
            throw new BizException(ErrorCode.QUOTA_EXCEEDED, "Team space storage quota exceeded");
        }
    }

    private int normalizeLimit(Integer limit) {
        int value = limit == null ? DEFAULT_LIMIT : limit;
        return Math.max(1, Math.min(value, MAX_LIMIT));
    }

    private int effectiveRecycleBinRetentionDays() {
        Integer value = recycleBinRetentionDays;
        return value == null || value < 1 ? DEFAULT_RECYCLE_BIN_RETENTION_DAYS : value;
    }

    private String detail(Long teamSpaceId, String suffix) {
        return TEAM_SPACE_TOKEN + teamSpaceId + "," + suffix;
    }

    private String normalizeNullableText(String raw, int maxLength) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = raw.trim();
        if (value.length() > maxLength) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Text length exceeded");
        }
        return value;
    }

    private byte[] readUploadBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to read uploaded file");
        }
    }

    private String storeUploadedFile(Long teamSpaceId, String fileName, byte[] content) {
        String safeName = sanitizeStorageName(fileName);
        String uniqueName = UUID.randomUUID().toString().replace("-", "") + "-" + safeName;
        Path rootPath = Paths.get(driveStorageRoot).toAbsolutePath().normalize();
        Path spaceDir = rootPath.resolve("team-space").resolve(String.valueOf(teamSpaceId)).normalize();
        Path target = spaceDir.resolve(uniqueName).normalize();
        if (!target.startsWith(spaceDir)) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Invalid upload target path");
        }
        try {
            Files.createDirectories(spaceDir);
            Files.write(target, content);
            return target.toString();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to store uploaded file");
        }
    }

    private String sanitizeStorageName(String fileName) {
        String normalized = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!StringUtils.hasText(normalized)) {
            return "file.bin";
        }
        return normalized.length() > 120 ? normalized.substring(0, 120) : normalized;
    }

    private void removeStoredFileIfExists(String storagePath) {
        if (!StringUtils.hasText(storagePath) || !isPathWithinStorageRoot(storagePath)) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(storagePath));
        } catch (IOException ignored) {
        }
    }

    private boolean isPathWithinStorageRoot(String filePath) {
        Path rootPath = Paths.get(driveStorageRoot).toAbsolutePath().normalize();
        Path targetPath = Paths.get(filePath).toAbsolutePath().normalize();
        return targetPath.startsWith(rootPath);
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(messageDigest.digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "SHA-256 algorithm unavailable");
        }
    }

    private String nullableId(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
