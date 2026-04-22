package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.foundation.security.PublicShareTokenCodec;
import com.mmmail.server.model.dto.BatchDriveItemsRequest;
import com.mmmail.server.model.dto.BatchCreateDriveShareRequest;
import com.mmmail.server.mapper.DriveFileVersionMapper;
import com.mmmail.server.mapper.DriveItemMapper;
import com.mmmail.server.mapper.DriveSavedShareMapper;
import com.mmmail.server.mapper.DriveShareAccessLogMapper;
import com.mmmail.server.mapper.DriveShareLinkMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.mapper.UserPreferenceMapper;
import com.mmmail.server.model.dto.CreateDriveFileRequest;
import com.mmmail.server.model.dto.CreateDriveFolderRequest;
import com.mmmail.server.model.dto.CreateEncryptedDriveShareRequest;
import com.mmmail.server.model.dto.CreateDriveShareRequest;
import com.mmmail.server.model.dto.MoveDriveItemRequest;
import com.mmmail.server.model.dto.RenameDriveItemRequest;
import com.mmmail.server.model.dto.SaveDriveSharedWithMeRequest;
import com.mmmail.server.model.dto.UploadDriveFileRequest;
import com.mmmail.server.model.dto.UpdateDriveShareRequest;
import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.DriveBatchActionResultVo;
import com.mmmail.server.model.vo.DriveBatchFailureVo;
import com.mmmail.server.model.vo.DriveBatchShareResultVo;
import com.mmmail.server.model.entity.DriveFileVersion;
import com.mmmail.server.model.entity.DriveItem;
import com.mmmail.server.model.entity.DriveSavedShare;
import com.mmmail.server.model.entity.DriveShareAccessLog;
import com.mmmail.server.model.entity.DriveShareLink;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.entity.UserPreference;
import com.mmmail.server.model.vo.DriveFileDownloadVo;
import com.mmmail.server.model.vo.DriveFilePreviewVo;
import com.mmmail.server.model.vo.DriveFileVersionVo;
import com.mmmail.server.model.vo.DriveItemVo;
import com.mmmail.server.model.vo.DrivePublicShareMetadataVo;
import com.mmmail.server.model.vo.DriveSavedShareVo;
import com.mmmail.server.model.vo.DriveShareAccessLogVo;
import com.mmmail.server.model.vo.DriveShareLinkVo;
import com.mmmail.server.model.vo.DriveTrashItemVo;
import com.mmmail.server.model.vo.DriveUsageVo;
import com.mmmail.server.model.vo.DriveVersionCleanupVo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class DriveService {

    private static final String ITEM_TYPE_FOLDER = "FOLDER";
    private static final String ITEM_TYPE_FILE = "FILE";
    private static final String SHARE_PERMISSION_VIEW = "VIEW";
    private static final String SHARE_PERMISSION_EDIT = "EDIT";
    private static final String SHARE_STATUS_ACTIVE = "ACTIVE";
    private static final String SHARE_STATUS_REVOKED = "REVOKED";
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_BATCH_ITEMS = 100;
    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_VERSION_LIMIT = 50;
    private static final int MAX_VERSION_LIMIT = 200;
    private static final int DEFAULT_VERSION_RETENTION_COUNT = 50;
    private static final int MIN_VERSION_RETENTION_COUNT = 1;
    private static final int MAX_VERSION_RETENTION_COUNT = 200;
    private static final int DEFAULT_VERSION_RETENTION_DAYS = 365;
    private static final int MIN_VERSION_RETENTION_DAYS = 1;
    private static final int MAX_VERSION_RETENTION_DAYS = 3650;
    private static final String DEFAULT_BINARY_MIME = "application/octet-stream";
    private static final String DRIVE_E2EE_PREVIEW_UNAVAILABLE = "Drive E2EE files must be decrypted locally before preview";
    private static final String DRIVE_PUBLIC_SHARE_E2EE_PREVIEW_UNAVAILABLE = "Drive E2EE public shares must be decrypted locally before preview";
    private static final int DEFAULT_RECYCLE_BIN_RETENTION_DAYS = 30;
    private static final int DEFAULT_PREVIEW_TEXT_MAX_BYTES = 262_144;
    private static final int DEFAULT_PUBLIC_RATE_LIMIT_WINDOW_SECONDS = 60;
    private static final int DEFAULT_PUBLIC_RATE_LIMIT_MAX_REQUESTS = 30;
    private static final String DEFAULT_PUBLIC_RATE_LIMIT_REDIS_KEY_PREFIX = "mmmail:drive:share-rate";
    private static final String PUBLIC_ACTION_METADATA = "METADATA";
    private static final String PUBLIC_ACTION_LIST = "LIST";
    private static final String PUBLIC_ACTION_DOWNLOAD = "DOWNLOAD";
    private static final String PUBLIC_ACTION_PREVIEW = "PREVIEW";
    private static final String PUBLIC_ACTION_UPLOAD = "UPLOAD";
    private static final String PUBLIC_ACTION_SAVE = "SAVE";
    private static final String ACCESS_STATUS_ALLOW = "ALLOW";
    private static final String ACCESS_STATUS_DENY_RATE_LIMIT = "DENY_RATE_LIMIT";
    private static final String ACCESS_STATUS_DENY_INVALID_TOKEN = "DENY_INVALID_TOKEN";
    private static final String ACCESS_STATUS_DENY_REVOKED = "DENY_REVOKED";
    private static final String ACCESS_STATUS_DENY_EXPIRED = "DENY_EXPIRED";
    private static final String ACCESS_STATUS_DENY_FILE_MISSING = "DENY_FILE_MISSING";
    private static final String ACCESS_STATUS_DENY_PERMISSION = "DENY_PERMISSION";
    private static final String ACCESS_STATUS_DENY_PASSWORD_REQUIRED = "DENY_PASSWORD_REQUIRED";
    private static final String ACCESS_STATUS_DENY_PASSWORD_INVALID = "DENY_PASSWORD_INVALID";
    private static final String ACCESS_STATUS_DENY_UNSUPPORTED_PREVIEW = "DENY_UNSUPPORTED_PREVIEW";
    private static final String SAVED_SHARE_STATUS_ACTIVE = "ACTIVE";
    private static final String SAVED_SHARE_STATUS_REVOKED = "REVOKED";
    private static final String SAVED_SHARE_STATUS_EXPIRED = "EXPIRED";
    private static final String SAVED_SHARE_STATUS_UNAVAILABLE = "UNAVAILABLE";
    private static final int MAX_SHARE_PASSWORD_LENGTH = 128;

    private final DriveFileVersionMapper driveFileVersionMapper;
    private final DriveItemMapper driveItemMapper;
    private final DriveSavedShareMapper driveSavedShareMapper;
    private final DriveShareLinkMapper driveShareLinkMapper;
    private final DriveShareAccessLogMapper driveShareAccessLogMapper;
    private final UserAccountMapper userAccountMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final SuiteService suiteService;
    private final AuditService auditService;
    private final SuiteCollaborationService suiteCollaborationService;
    private final DriveFileE2eeService driveFileE2eeService;
    private final DriveReadableShareE2eeService driveReadableShareE2eeService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;
    private final PublicShareTokenCodec publicShareTokenCodec = new PublicShareTokenCodec();
    private final Map<String, PublicRateCounter> publicRateCounterMap = new ConcurrentHashMap<>();
    @Value("${mmmail.drive.storage-root:${java.io.tmpdir}/mmmail-drive}")
    private String driveStorageRoot;
    @Value("${mmmail.drive.recycle-bin.retention-days:30}")
    private Integer recycleBinRetentionDays;
    @Value("${mmmail.drive.preview-text-max-bytes:262144}")
    private Integer previewTextMaxBytes;
    @Value("${mmmail.drive.public-share-rate-limit.window-seconds:60}")
    private Integer publicShareRateLimitWindowSeconds;
    @Value("${mmmail.drive.public-share-rate-limit.max-requests:30}")
    private Integer publicShareRateLimitMaxRequests;
    @Value("${mmmail.drive.public-share-rate-limit.redis-key-prefix:mmmail:drive:share-rate}")
    private String publicShareRateLimitRedisKeyPrefix;
    @Value("${mmmail.drive.upload.max-file-size-bytes:52428800}")
    private Long uploadMaxFileSizeBytes;

    public DriveService(
            DriveFileVersionMapper driveFileVersionMapper,
            DriveItemMapper driveItemMapper,
            DriveSavedShareMapper driveSavedShareMapper,
            DriveShareLinkMapper driveShareLinkMapper,
            DriveShareAccessLogMapper driveShareAccessLogMapper,
            UserAccountMapper userAccountMapper,
            UserPreferenceMapper userPreferenceMapper,
            SuiteService suiteService,
            AuditService auditService,
            SuiteCollaborationService suiteCollaborationService,
            DriveFileE2eeService driveFileE2eeService,
            DriveReadableShareE2eeService driveReadableShareE2eeService,
            PasswordEncoder passwordEncoder,
            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider
    ) {
        this.driveFileVersionMapper = driveFileVersionMapper;
        this.driveItemMapper = driveItemMapper;
        this.driveSavedShareMapper = driveSavedShareMapper;
        this.driveShareLinkMapper = driveShareLinkMapper;
        this.driveShareAccessLogMapper = driveShareAccessLogMapper;
        this.userAccountMapper = userAccountMapper;
        this.userPreferenceMapper = userPreferenceMapper;
        this.suiteService = suiteService;
        this.auditService = auditService;
        this.suiteCollaborationService = suiteCollaborationService;
        this.driveFileE2eeService = driveFileE2eeService;
        this.driveReadableShareE2eeService = driveReadableShareE2eeService;
        this.passwordEncoder = passwordEncoder;
        this.stringRedisTemplateProvider = stringRedisTemplateProvider;
    }

    public List<DriveItemVo> listItems(Long userId, Long parentId, String keyword, String itemType, Integer limit) {
        int safeLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        String normalizedType = normalizeItemTypeOrNull(itemType);
        String normalizedKeyword = normalizeKeyword(keyword);

        LambdaQueryWrapper<DriveItem> query = new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getOwnerId, userId)
                .orderByDesc(DriveItem::getItemType)
                .orderByAsc(DriveItem::getName)
                .orderByDesc(DriveItem::getUpdatedAt)
                .last("limit " + safeLimit);

        if (parentId == null) {
            query.isNull(DriveItem::getParentId);
        } else {
            query.eq(DriveItem::getParentId, parentId);
        }
        if (normalizedType != null) {
            query.eq(DriveItem::getItemType, normalizedType);
        }
        if (StringUtils.hasText(normalizedKeyword)) {
            query.like(DriveItem::getName, normalizedKeyword);
        }

        List<DriveItem> items = driveItemMapper.selectList(query);
        Map<Long, Integer> shareCountMap = buildActiveShareCountMap(items);
        return items.stream()
                .map(item -> toItemVo(item, shareCountMap.getOrDefault(item.getId(), 0)))
                .toList();
    }

    public List<DriveTrashItemVo> listTrashItems(Long userId, Integer limit) {
        int safeLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        return driveItemMapper.selectTrashedItems(userId, safeLimit).stream()
                .map(this::toTrashItemVo)
                .toList();
    }

    @Transactional
    public DriveTrashItemVo restoreTrashItem(Long userId, Long itemId, String ipAddress) {
        DriveItem trashedItem = driveItemMapper.selectTrashedById(userId, itemId);
        if (trashedItem == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive trash item is not found");
        }

        Long targetParentId = normalizeRestoreParentId(userId, trashedItem.getParentId());
        ensureNameUnique(userId, targetParentId, trashedItem.getItemType(), trashedItem.getName(), trashedItem.getId());

        int updated = driveItemMapper.restoreTrashedItem(userId, itemId, targetParentId);
        if (updated <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive trash item restore failed");
        }
        trashedItem.setParentId(targetParentId);
        trashedItem.setDeleted(0);
        trashedItem.setTrashedAt(null);
        trashedItem.setPurgeAfterAt(null);
        trashedItem.setUpdatedAt(LocalDateTime.now());
        auditService.record(
                userId,
                "DRIVE_ITEM_RESTORE",
                "itemId=" + itemId + ",parentId=" + nullableId(targetParentId),
                ipAddress
        );
        return toTrashItemVo(trashedItem);
    }

    @Transactional
    public void purgeTrashItem(Long userId, Long itemId, String ipAddress) {
        DriveItem trashedItem = driveItemMapper.selectTrashedById(userId, itemId);
        if (trashedItem == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive trash item is not found");
        }
        List<DriveFileVersion> versions = driveFileVersionMapper.selectList(new LambdaQueryWrapper<DriveFileVersion>()
                .eq(DriveFileVersion::getOwnerId, userId)
                .eq(DriveFileVersion::getItemId, itemId));
        for (DriveFileVersion version : versions) {
            removeStoredFileIfExists(version.getStoragePath());
        }
        if (ITEM_TYPE_FILE.equals(trashedItem.getItemType())) {
            removeStoredFileIfExists(trashedItem.getStoragePath());
        }
        driveFileVersionMapper.purgeByItemId(itemId);
        driveShareLinkMapper.purgeByItemId(itemId);
        int deleted = driveItemMapper.deleteTrashedItemPermanently(userId, itemId);
        if (deleted <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive trash item purge failed");
        }
        auditService.record(
                userId,
                "DRIVE_ITEM_PURGE",
                "itemId=" + itemId + ",type=" + trashedItem.getItemType(),
                ipAddress
        );
    }

    public List<DriveShareAccessLogVo> listShareAccessLogs(Long userId, String action, String accessStatus, Integer limit) {
        int safeLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        String normalizedAction = normalizeNullableLogAction(action);
        String normalizedStatus = normalizeNullableAccessStatus(accessStatus);

        LambdaQueryWrapper<DriveShareAccessLog> query = new LambdaQueryWrapper<DriveShareAccessLog>()
                .eq(DriveShareAccessLog::getOwnerId, userId)
                .orderByDesc(DriveShareAccessLog::getCreatedAt)
                .last("limit " + safeLimit);
        if (normalizedAction != null) {
            query.eq(DriveShareAccessLog::getAction, normalizedAction);
        }
        if (normalizedStatus != null) {
            query.eq(DriveShareAccessLog::getAccessStatus, normalizedStatus);
        }
        return driveShareAccessLogMapper.selectList(query).stream()
                .map(this::toShareAccessLogVo)
                .toList();
    }

    public List<DriveSavedShareVo> listSharedWithMe(Long userId) {
        List<DriveSavedShare> savedShares = driveSavedShareMapper.selectList(new LambdaQueryWrapper<DriveSavedShare>()
                .eq(DriveSavedShare::getRecipientUserId, userId)
                .orderByDesc(DriveSavedShare::getUpdatedAt));
        if (savedShares.isEmpty()) {
            return List.of();
        }
        Map<Long, DriveShareLink> shareMap = loadSavedShareLinkMap(savedShares);
        Map<Long, DriveItem> itemMap = loadSavedShareItemMap(savedShares, shareMap);
        Map<Long, UserAccount> ownerMap = loadSavedShareOwnerMap(savedShares);
        return savedShares.stream()
                .map(savedShare -> toSavedShareVo(savedShare, shareMap, itemMap, ownerMap))
                .toList();
    }

    @Transactional
    public DriveSavedShareVo saveSharedWithMe(
            Long userId,
            SaveDriveSharedWithMeRequest request,
            String ipAddress,
            String userAgent
    ) {
        ShareAndItem pair = loadPublicShareAndItemForAction(
                request.token(),
                request.password(),
                ipAddress,
                userAgent,
                PUBLIC_ACTION_SAVE,
                true
        );
        assertSaveForLaterAllowed(userId, pair, request.token(), ipAddress, userAgent);
        UserAccount owner = loadRequiredUser(pair.share().getOwnerId());
        DriveSavedShare savedShare = buildSavedShare(userId, pair, owner);
        driveSavedShareMapper.insert(savedShare);
        auditService.record(
                userId,
                "DRIVE_SHARED_WITH_ME_SAVE",
                "savedShareId=" + savedShare.getId() + ",shareId=" + savedShare.getShareId(),
                ipAddress
        );
        recordPublicAccess(pair, request.token(), PUBLIC_ACTION_SAVE, ACCESS_STATUS_ALLOW, ipAddress, userAgent);
        return toSavedShareVo(savedShare, pair.share(), pair.item(), owner);
    }

    @Transactional
    public void removeSharedWithMe(Long userId, Long savedShareId, String ipAddress) {
        DriveSavedShare savedShare = loadSavedShare(userId, savedShareId);
        driveSavedShareMapper.deleteById(savedShare.getId());
        auditService.record(
                userId,
                "DRIVE_SHARED_WITH_ME_REMOVE",
                "savedShareId=" + savedShareId + ",shareId=" + savedShare.getShareId(),
                ipAddress
        );
    }

    @Transactional
    public DriveItemVo createFolder(Long userId, CreateDriveFolderRequest request, String ipAddress) {
        String name = normalizeName(request.name());
        ensureParentFolder(userId, request.parentId());
        ensureNameUnique(userId, request.parentId(), ITEM_TYPE_FOLDER, name, null);

        LocalDateTime now = LocalDateTime.now();
        DriveItem item = new DriveItem();
        item.setOwnerId(userId);
        item.setParentId(request.parentId());
        item.setItemType(ITEM_TYPE_FOLDER);
        item.setName(name);
        item.setMimeType(null);
        item.setSizeBytes(0L);
        item.setStoragePath(null);
        item.setChecksum(null);
        applyE2eeMetadata(item, DriveFileE2eeService.DriveFileE2eeMetadata.disabled());
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setDeleted(0);
        driveItemMapper.insert(item);

        AuditEventVo event = auditService.recordEvent(
                userId,
                "DRIVE_ITEM_CREATE",
                "itemId=" + item.getId() + ",type=FOLDER,parentId=" + nullableId(item.getParentId()),
                ipAddress
        );
        suiteCollaborationService.publishToUser(userId, event);
        return toItemVo(item, 0);
    }

    @Transactional
    public DriveItemVo createFile(Long userId, CreateDriveFileRequest request, String ipAddress) {
        String name = normalizeName(request.name());
        ensureParentFolder(userId, request.parentId());
        ensureNameUnique(userId, request.parentId(), ITEM_TYPE_FILE, name, null);

        long sizeBytes = request.sizeBytes() == null ? 0L : request.sizeBytes();
        if (sizeBytes <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "sizeBytes must be greater than 0");
        }
        suiteService.assertDriveStorageQuota(userId, sizeBytes, ipAddress);

        LocalDateTime now = LocalDateTime.now();
        DriveItem item = new DriveItem();
        item.setOwnerId(userId);
        item.setParentId(request.parentId());
        item.setItemType(ITEM_TYPE_FILE);
        item.setName(name);
        item.setMimeType(normalizeNullableText(request.mimeType(), 128));
        item.setSizeBytes(sizeBytes);
        item.setStoragePath(normalizeNullableText(request.storagePath(), 512));
        item.setChecksum(normalizeNullableText(request.checksum(), 128));
        applyE2eeMetadata(item, DriveFileE2eeService.DriveFileE2eeMetadata.disabled());
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setDeleted(0);
        driveItemMapper.insert(item);

        auditService.record(
                userId,
                "DRIVE_ITEM_CREATE",
                "itemId=" + item.getId() + ",type=FILE,sizeBytes=" + sizeBytes + ",parentId=" + nullableId(item.getParentId()),
                ipAddress
        );
        return toItemVo(item, 0);
    }

    @Transactional
    public DriveItemVo uploadFile(Long userId, UploadDriveFileRequest request, String ipAddress) {
        ensureParentFolder(userId, request.getParentId());
        PreparedUpload upload = prepareOwnedUpload(userId, request, null, ipAddress);
        ensureNameUnique(userId, request.getParentId(), ITEM_TYPE_FILE, upload.fileName(), null);
        LocalDateTime now = LocalDateTime.now();
        DriveItem item = new DriveItem();
        item.setOwnerId(userId);
        item.setParentId(request.getParentId());
        item.setItemType(ITEM_TYPE_FILE);
        item.setName(upload.fileName());
        item.setMimeType(upload.mimeType());
        item.setSizeBytes(upload.sizeBytes());
        item.setStoragePath(upload.storagePath());
        item.setChecksum(upload.checksum());
        applyE2eeMetadata(item, upload.e2eeMetadata());
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setDeleted(0);
        driveItemMapper.insert(item);

        auditService.record(
                userId,
                "DRIVE_FILE_UPLOAD",
                "itemId=" + item.getId() + ",sizeBytes=" + upload.sizeBytes() + ",parentId=" + nullableId(request.getParentId()),
                ipAddress
        );
        return toItemVo(item, 0);
    }

    public DriveFileDownloadVo downloadOwnedFile(Long userId, Long itemId, String ipAddress) {
        DriveItem item = loadOwnedFile(userId, itemId);
        byte[] content = readStoredFile(item);
        auditService.record(
                userId,
                "DRIVE_FILE_DOWNLOAD",
                "itemId=" + itemId + ",sizeBytes=" + content.length,
                ipAddress
        );
        return toFileDownloadVo(item, content);
    }

    public DriveFilePreviewVo previewOwnedFile(Long userId, Long itemId, String ipAddress) {
        DriveItem item = loadOwnedFile(userId, itemId);
        PreviewContent previewContent = readStoredPreviewContent(item);
        auditService.record(
                userId,
                "DRIVE_FILE_PREVIEW",
                "itemId=" + itemId + ",sizeBytes=" + previewContent.content().length + ",truncated=" + previewContent.truncated(),
                ipAddress
        );
        return toFilePreviewVo(item, previewContent.content(), previewContent.truncated());
    }

    public List<DriveFileVersionVo> listFileVersions(Long userId, Long itemId, Integer limit) {
        loadOwnedFile(userId, itemId);
        int safeLimit = limit == null ? DEFAULT_VERSION_LIMIT : Math.max(1, Math.min(limit, MAX_VERSION_LIMIT));
        return driveFileVersionMapper.selectList(new LambdaQueryWrapper<DriveFileVersion>()
                        .eq(DriveFileVersion::getOwnerId, userId)
                        .eq(DriveFileVersion::getItemId, itemId)
                        .orderByDesc(DriveFileVersion::getVersionNo)
                        .orderByDesc(DriveFileVersion::getCreatedAt)
                        .last("limit " + safeLimit))
                .stream()
                .map(this::toFileVersionVo)
                .toList();
    }

    @Transactional
    public DriveItemVo uploadFileVersion(Long userId, Long itemId, UploadDriveFileRequest request, String ipAddress) {
        DriveItem currentItem = loadOwnedFile(userId, itemId);
        PreparedUpload upload = prepareOwnedUpload(userId, request, currentItem.getName(), ipAddress);
        snapshotCurrentItemAsVersion(currentItem);
        currentItem.setMimeType(upload.mimeType());
        currentItem.setSizeBytes(upload.sizeBytes());
        currentItem.setStoragePath(upload.storagePath());
        currentItem.setChecksum(upload.checksum());
        applyE2eeMetadata(currentItem, upload.e2eeMetadata());
        currentItem.setUpdatedAt(LocalDateTime.now());
        driveItemMapper.updateById(currentItem);

        AuditEventVo event = auditService.recordEvent(
                userId,
                "DRIVE_FILE_VERSION_UPLOAD",
                "itemId=" + itemId + ",sizeBytes=" + upload.sizeBytes(),
                ipAddress
        );
        suiteCollaborationService.publishToUser(userId, event);
        applyVersionRetentionPolicy(userId, currentItem, "DRIVE_FILE_VERSION_CLEANUP_AUTO", ipAddress);
        return toItemVo(currentItem, activeShareCount(itemId));
    }

    @Transactional
    public DriveItemVo restoreFileVersion(Long userId, Long itemId, Long versionId, String ipAddress) {
        DriveItem currentItem = loadOwnedFile(userId, itemId);
        DriveFileVersion targetVersion = driveFileVersionMapper.selectOne(new LambdaQueryWrapper<DriveFileVersion>()
                .eq(DriveFileVersion::getId, versionId)
                .eq(DriveFileVersion::getOwnerId, userId)
                .eq(DriveFileVersion::getItemId, itemId));
        if (targetVersion == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file version is not found");
        }
        if (!StringUtils.hasText(targetVersion.getStoragePath()) || !isPathWithinStorageRoot(targetVersion.getStoragePath())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file version content is unavailable");
        }

        snapshotCurrentItemAsVersion(currentItem);

        currentItem.setMimeType(targetVersion.getMimeType());
        currentItem.setSizeBytes(targetVersion.getSizeBytes());
        currentItem.setStoragePath(targetVersion.getStoragePath());
        currentItem.setChecksum(targetVersion.getChecksum());
        currentItem.setE2eeEnabled(targetVersion.getE2eeEnabled());
        currentItem.setE2eeAlgorithm(targetVersion.getE2eeAlgorithm());
        currentItem.setE2eeFingerprintsJson(targetVersion.getE2eeFingerprintsJson());
        currentItem.setUpdatedAt(LocalDateTime.now());
        driveItemMapper.updateById(currentItem);

        auditService.record(
                userId,
                "DRIVE_FILE_VERSION_RESTORE",
                "itemId=" + itemId + ",versionId=" + versionId,
                ipAddress
        );
        applyVersionRetentionPolicy(userId, currentItem, "DRIVE_FILE_VERSION_CLEANUP_AUTO", ipAddress);
        return toItemVo(currentItem, activeShareCount(itemId));
    }

    @Transactional
    public DriveVersionCleanupVo cleanupFileVersions(Long userId, Long itemId, String ipAddress) {
        DriveItem currentItem = loadOwnedFile(userId, itemId);
        return applyVersionRetentionPolicy(userId, currentItem, "DRIVE_FILE_VERSION_CLEANUP_MANUAL", ipAddress);
    }

    public DrivePublicShareMetadataVo getPublicShareMetadataByToken(String token, String ipAddress, String userAgent) {
        ShareAndItem pair = loadPublicShareAndItemForAction(token, null, ipAddress, userAgent, PUBLIC_ACTION_METADATA, false);
        auditService.record(
                pair.item().getOwnerId(),
                "DRIVE_PUBLIC_SHARE_METADATA",
                "shareId=" + pair.share().getId() + ",itemId=" + pair.item().getId(),
                ipAddress
        );
        recordPublicAccess(pair, token, PUBLIC_ACTION_METADATA, ACCESS_STATUS_ALLOW, ipAddress, userAgent);
        return toPublicShareMetadataVo(pair.share(), pair.item());
    }

    public List<DriveItemVo> listPublicShareItems(
            String token,
            Long parentId,
            String sharePassword,
            String ipAddress,
            String userAgent
    ) {
        ShareAndItem pair = loadPublicShareAndItemForAction(token, sharePassword, ipAddress, userAgent, PUBLIC_ACTION_LIST, true);
        DriveItem rootFolder = requirePublicSharedFolder(pair.item());
        DriveItem targetFolder = resolvePublicShareFolderTarget(rootFolder, parentId);
        List<DriveItem> items = driveItemMapper.selectList(new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getOwnerId, rootFolder.getOwnerId())
                .eq(DriveItem::getParentId, targetFolder.getId())
                .orderByDesc(DriveItem::getItemType)
                .orderByAsc(DriveItem::getName)
                .orderByDesc(DriveItem::getUpdatedAt)
                .last("limit " + MAX_LIMIT));
        auditService.record(
                rootFolder.getOwnerId(),
                "DRIVE_PUBLIC_SHARE_LIST",
                "shareId=" + pair.share().getId() + ",itemId=" + rootFolder.getId() + ",parentId=" + targetFolder.getId() + ",count=" + items.size(),
                ipAddress
        );
        recordPublicAccess(pair, token, PUBLIC_ACTION_LIST, ACCESS_STATUS_ALLOW, ipAddress, userAgent);
        return items.stream()
                .map(item -> toItemVo(item, 0))
                .toList();
    }

    public DriveFileDownloadVo downloadByPublicToken(String token, String sharePassword, String ipAddress, String userAgent) {
        ShareAndItem pair = loadPublicShareAndItemForAction(token, sharePassword, ipAddress, userAgent, PUBLIC_ACTION_DOWNLOAD, true);
        DriveItem file = requirePublicSharedFile(pair.item());
        boolean readableE2ee = driveReadableShareE2eeService.isEnabled(pair.share().getReadableE2eeEnabled());
        byte[] content = readableE2ee ? readStoredReadableShareFile(pair.share()) : readStoredFile(file);
        auditService.record(
                file.getOwnerId(),
                "DRIVE_PUBLIC_SHARE_DOWNLOAD",
                "shareId=" + pair.share().getId() + ",itemId=" + file.getId() + ",sizeBytes=" + content.length,
                ipAddress
        );
        recordPublicAccess(pair, token, PUBLIC_ACTION_DOWNLOAD, ACCESS_STATUS_ALLOW, ipAddress, userAgent);
        if (readableE2ee) {
            return new DriveFileDownloadVo(
                    encryptedReadableShareFileName(file.getName()),
                    DEFAULT_BINARY_MIME,
                    content
            );
        }
        return toFileDownloadVo(file, content);
    }

    public DriveFilePreviewVo previewByPublicToken(String token, String sharePassword, String ipAddress, String userAgent) {
        ShareAndItem pair = loadPublicShareAndItemForAction(token, sharePassword, ipAddress, userAgent, PUBLIC_ACTION_PREVIEW, true);
        DriveItem file = requirePublicSharedFile(pair.item());
        if (driveReadableShareE2eeService.isEnabled(pair.share().getReadableE2eeEnabled())) {
            recordPublicAccess(pair, token, PUBLIC_ACTION_PREVIEW, ACCESS_STATUS_DENY_UNSUPPORTED_PREVIEW, ipAddress, userAgent);
            throw new BizException(ErrorCode.INVALID_ARGUMENT, DRIVE_PUBLIC_SHARE_E2EE_PREVIEW_UNAVAILABLE);
        }
        PreviewContent previewContent;
        try {
            previewContent = readStoredPreviewContent(file);
        } catch (BizException exception) {
            if (exception.getCode() == ErrorCode.INVALID_ARGUMENT.getCode()
                    && "Drive preview is unavailable for current file type".equals(exception.getMessage())) {
                recordPublicAccess(pair, token, PUBLIC_ACTION_PREVIEW, ACCESS_STATUS_DENY_UNSUPPORTED_PREVIEW, ipAddress, userAgent);
            }
            throw exception;
        }
        auditService.record(
                file.getOwnerId(),
                "DRIVE_PUBLIC_SHARE_PREVIEW",
                "shareId=" + pair.share().getId() + ",itemId=" + file.getId() + ",sizeBytes=" + previewContent.content().length + ",truncated=" + previewContent.truncated(),
                ipAddress
        );
        recordPublicAccess(pair, token, PUBLIC_ACTION_PREVIEW, ACCESS_STATUS_ALLOW, ipAddress, userAgent);
        return toFilePreviewVo(file, previewContent.content(), previewContent.truncated());
    }

    public DriveFileDownloadVo downloadPublicShareItem(
            String token,
            Long itemId,
            String sharePassword,
            String ipAddress,
            String userAgent
    ) {
        ShareAndItem pair = loadPublicShareAndItemForAction(token, sharePassword, ipAddress, userAgent, PUBLIC_ACTION_DOWNLOAD, true);
        DriveItem rootFolder = requirePublicSharedFolder(pair.item());
        DriveItem file = resolvePublicShareFileTarget(rootFolder, itemId);
        byte[] content = readStoredFile(file);
        auditService.record(
                rootFolder.getOwnerId(),
                "DRIVE_PUBLIC_SHARE_DOWNLOAD",
                "shareId=" + pair.share().getId() + ",itemId=" + file.getId() + ",sizeBytes=" + content.length,
                ipAddress
        );
        recordPublicAccess(pair, token, PUBLIC_ACTION_DOWNLOAD, ACCESS_STATUS_ALLOW, ipAddress, userAgent);
        return toFileDownloadVo(file, content);
    }

    public DriveFilePreviewVo previewPublicShareItem(
            String token,
            Long itemId,
            String sharePassword,
            String ipAddress,
            String userAgent
    ) {
        ShareAndItem pair = loadPublicShareAndItemForAction(token, sharePassword, ipAddress, userAgent, PUBLIC_ACTION_PREVIEW, true);
        DriveItem rootFolder = requirePublicSharedFolder(pair.item());
        DriveItem file = resolvePublicShareFileTarget(rootFolder, itemId);
        PreviewContent previewContent;
        try {
            previewContent = readStoredPreviewContent(file);
        } catch (BizException exception) {
            if (exception.getCode() == ErrorCode.INVALID_ARGUMENT.getCode()
                    && "Drive preview is unavailable for current file type".equals(exception.getMessage())) {
                recordPublicAccess(pair, token, PUBLIC_ACTION_PREVIEW, ACCESS_STATUS_DENY_UNSUPPORTED_PREVIEW, ipAddress, userAgent);
            }
            throw exception;
        }
        auditService.record(
                rootFolder.getOwnerId(),
                "DRIVE_PUBLIC_SHARE_PREVIEW",
                "shareId=" + pair.share().getId() + ",itemId=" + file.getId() + ",sizeBytes=" + previewContent.content().length + ",truncated=" + previewContent.truncated(),
                ipAddress
        );
        recordPublicAccess(pair, token, PUBLIC_ACTION_PREVIEW, ACCESS_STATUS_ALLOW, ipAddress, userAgent);
        return toFilePreviewVo(file, previewContent.content(), previewContent.truncated());
    }

    @Transactional
    public DriveItemVo uploadToPublicShareFolder(
            String token,
            Long parentId,
            String sharePassword,
            MultipartFile file,
            String ipAddress,
            String userAgent
    ) {
        ShareAndItem pair = loadPublicShareAndItemForAction(token, sharePassword, ipAddress, userAgent, PUBLIC_ACTION_UPLOAD, true);
        DriveItem rootFolder = requirePublicSharedFolder(pair.item());
        if (!SHARE_PERMISSION_EDIT.equals(pair.share().getPermission())) {
            recordPublicAccess(pair, token, PUBLIC_ACTION_UPLOAD, ACCESS_STATUS_DENY_PERMISSION, ipAddress, userAgent);
            throw new BizException(ErrorCode.UNAUTHORIZED, "Drive share does not allow uploads");
        }
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "file is required");
        }
        DriveItem targetFolder = resolvePublicShareFolderTarget(rootFolder, parentId);
        String fileName = normalizeName(resolveUploadName(file));
        ensureNameUnique(rootFolder.getOwnerId(), targetFolder.getId(), ITEM_TYPE_FILE, fileName, null);
        byte[] content = readUploadBytes(file);
        long sizeBytes = content.length;
        if (sizeBytes <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "file size must be greater than 0");
        }
        assertUploadSizeWithinLimit(sizeBytes);
        suiteService.assertDriveStorageQuota(rootFolder.getOwnerId(), sizeBytes, ipAddress);
        String checksum = sha256(content);
        String mimeType = normalizeNullableText(file.getContentType(), 128);
        String storagePath = normalizeNullableText(storeUploadedFile(rootFolder.getOwnerId(), fileName, content), 512);
        LocalDateTime now = LocalDateTime.now();
        DriveItem item = new DriveItem();
        item.setOwnerId(rootFolder.getOwnerId());
        item.setParentId(targetFolder.getId());
        item.setItemType(ITEM_TYPE_FILE);
        item.setName(fileName);
        item.setMimeType(mimeType == null ? DEFAULT_BINARY_MIME : mimeType);
        item.setSizeBytes(sizeBytes);
        item.setStoragePath(storagePath);
        item.setChecksum(checksum);
        applyE2eeMetadata(item, DriveFileE2eeService.DriveFileE2eeMetadata.disabled());
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setDeleted(0);
        driveItemMapper.insert(item);
        AuditEventVo event = auditService.recordEvent(
                rootFolder.getOwnerId(),
                "DRIVE_PUBLIC_SHARE_UPLOAD",
                "shareId=" + pair.share().getId() + ",itemId=" + item.getId() + ",parentId=" + targetFolder.getId() + ",sizeBytes=" + sizeBytes,
                ipAddress
        );
        suiteCollaborationService.publishToUser(rootFolder.getOwnerId(), event);
        recordPublicAccess(pair, token, PUBLIC_ACTION_UPLOAD, ACCESS_STATUS_ALLOW, ipAddress, userAgent);
        return toItemVo(item, activeShareCount(item.getId()));
    }

    @Transactional
    public DriveItemVo renameItem(Long userId, Long itemId, RenameDriveItemRequest request, String ipAddress) {
        DriveItem item = loadOwnedItem(userId, itemId);
        String newName = normalizeName(request.name());
        ensureNameUnique(userId, item.getParentId(), item.getItemType(), newName, item.getId());

        item.setName(newName);
        item.setUpdatedAt(LocalDateTime.now());
        driveItemMapper.updateById(item);

        AuditEventVo event = auditService.recordEvent(userId, "DRIVE_ITEM_RENAME", "itemId=" + item.getId(), ipAddress);
        suiteCollaborationService.publishToUser(userId, event);
        return toItemVo(item, activeShareCount(item.getId()));
    }

    @Transactional
    public DriveItemVo moveItem(Long userId, Long itemId, MoveDriveItemRequest request, String ipAddress) {
        DriveItem item = loadOwnedItem(userId, itemId);
        Long targetParentId = request.parentId();

        if (sameParent(item.getParentId(), targetParentId)) {
            return toItemVo(item, activeShareCount(item.getId()));
        }

        validateMoveTarget(userId, item, targetParentId);
        ensureNameUnique(userId, targetParentId, item.getItemType(), item.getName(), item.getId());

        LocalDateTime now = LocalDateTime.now();
        driveItemMapper.update(
                null,
                new LambdaUpdateWrapper<DriveItem>()
                        .eq(DriveItem::getId, item.getId())
                        .eq(DriveItem::getOwnerId, userId)
                        .set(DriveItem::getParentId, targetParentId)
                        .set(DriveItem::getUpdatedAt, now)
        );
        item.setParentId(targetParentId);
        item.setUpdatedAt(now);

        auditService.record(
                userId,
                "DRIVE_ITEM_MOVE",
                "itemId=" + item.getId() + ",targetParentId=" + nullableId(targetParentId),
                ipAddress
        );
        return toItemVo(item, activeShareCount(item.getId()));
    }

    @Transactional
    public void deleteItem(Long userId, Long itemId, String ipAddress) {
        DriveItem item = loadOwnedItem(userId, itemId);
        if (ITEM_TYPE_FOLDER.equals(item.getItemType()) && driveItemMapper.countChildren(userId, itemId) > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Cannot delete non-empty folder");
        }

        LocalDateTime now = LocalDateTime.now();
        item.setTrashedAt(now);
        item.setPurgeAfterAt(now.plusDays(effectiveRecycleBinRetentionDays()));
        item.setUpdatedAt(now);
        driveItemMapper.updateById(item);
        driveShareLinkMapper.purgeByItemId(itemId);
        driveItemMapper.deleteById(itemId);
        auditService.record(
                userId,
                "DRIVE_ITEM_DELETE",
                "itemId=" + itemId + ",type=" + item.getItemType(),
                ipAddress
        );
    }

    @Transactional
    public DriveBatchActionResultVo batchDeleteItems(
            Long userId,
            BatchDriveItemsRequest request,
            String ipAddress
    ) {
        List<Long> itemIds = normalizeBatchItemIds(request.itemIds());
        return executeBatchAction(
                userId,
                itemIds,
                ipAddress,
                "DRIVE_ITEM_BATCH_DELETE",
                itemId -> deleteItem(userId, itemId, ipAddress)
        );
    }

    @Transactional
    public DriveBatchActionResultVo batchRestoreTrashItems(
            Long userId,
            BatchDriveItemsRequest request,
            String ipAddress
    ) {
        List<Long> itemIds = normalizeBatchItemIds(request.itemIds());
        return executeBatchAction(
                userId,
                itemIds,
                ipAddress,
                "DRIVE_ITEM_BATCH_RESTORE",
                itemId -> restoreTrashItem(userId, itemId, ipAddress)
        );
    }

    @Transactional
    public DriveBatchActionResultVo batchPurgeTrashItems(
            Long userId,
            BatchDriveItemsRequest request,
            String ipAddress
    ) {
        List<Long> itemIds = normalizeBatchItemIds(request.itemIds());
        return executeBatchAction(
                userId,
                itemIds,
                ipAddress,
                "DRIVE_ITEM_BATCH_PURGE",
                itemId -> purgeTrashItem(userId, itemId, ipAddress)
        );
    }

    public List<DriveShareLinkVo> listShares(Long userId, Long itemId) {
        loadOwnedItem(userId, itemId);
        return driveShareLinkMapper.selectList(new LambdaQueryWrapper<DriveShareLink>()
                        .eq(DriveShareLink::getOwnerId, userId)
                        .eq(DriveShareLink::getItemId, itemId)
                        .orderByDesc(DriveShareLink::getUpdatedAt))
                .stream()
                .map(this::toShareVo)
                .toList();
    }

    @Transactional
    public DriveShareLinkVo createShare(Long userId, Long itemId, CreateDriveShareRequest request, String ipAddress) {
        DriveItem item = loadOwnedItem(userId, itemId);
        if (driveFileE2eeService.isEnabled(item.getE2eeEnabled())) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "Drive E2EE files must use the readable-share E2EE create flow"
            );
        }
        String permission = normalizeSharePermission(request.permission());
        LocalDateTime expiresAt = normalizeShareExpiry(request.expiresAt());
        String passwordHash = encodeSharePassword(request.password());

        LocalDateTime now = LocalDateTime.now();
        DriveShareLink shareLink = new DriveShareLink();
        shareLink.setOwnerId(userId);
        shareLink.setItemId(itemId);
        String rawToken = generateShareToken();
        shareLink.setToken(rawToken);
        shareLink.setTokenHash(publicShareTokenCodec.hash(rawToken));
        shareLink.setPermission(permission);
        shareLink.setExpiresAt(expiresAt);
        shareLink.setPasswordHash(passwordHash);
        shareLink.setStatus(SHARE_STATUS_ACTIVE);
        shareLink.setCreatedAt(now);
        shareLink.setUpdatedAt(now);
        shareLink.setDeleted(0);
        driveShareLinkMapper.insert(shareLink);

        AuditEventVo event = auditService.recordEvent(
                userId,
                "DRIVE_SHARE_CREATE",
                "itemId=" + itemId + ",shareId=" + shareLink.getId() + ",permission=" + permission
                        + ",passwordProtected=" + StringUtils.hasText(passwordHash),
                ipAddress
        );
        suiteCollaborationService.publishToUser(userId, event);
        return toShareVo(shareLink);
    }

    @Transactional
    public DriveShareLinkVo createEncryptedShare(
            Long userId,
            Long itemId,
            CreateEncryptedDriveShareRequest request,
            String ipAddress
    ) {
        DriveItem item = loadOwnedFile(userId, itemId);
        if (!driveFileE2eeService.isEnabled(item.getE2eeEnabled())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive readable-share E2EE requires an E2EE file");
        }
        String permission = normalizeSharePermission(request.getPermission());
        if (!SHARE_PERMISSION_VIEW.equals(permission)) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "Drive readable-share E2EE only supports view-only public links"
            );
        }
        LocalDateTime expiresAt = normalizeShareExpiry(request.getExpiresAt());
        String password = requireSharePassword(request.getPassword());
        DriveReadableShareE2eeService.ReadableShareE2eeMetadata e2eeMetadata =
                driveReadableShareE2eeService.resolveCreate(request.getE2eeAlgorithm());
        MultipartFile file = requireUploadFile(request.getFile());
        byte[] content = readUploadBytes(file);
        validateUploadSize(content.length);
        String storagePath = null;
        try {
            storagePath = normalizeNullableText(storeUploadedFile(userId, encryptedReadableShareFileName(item.getName()), content), 512);
            LocalDateTime now = LocalDateTime.now();
            DriveShareLink shareLink = new DriveShareLink();
            shareLink.setOwnerId(userId);
            shareLink.setItemId(itemId);
            String rawToken = generateShareToken();
            shareLink.setToken(rawToken);
            shareLink.setTokenHash(publicShareTokenCodec.hash(rawToken));
            shareLink.setPermission(permission);
            shareLink.setExpiresAt(expiresAt);
            shareLink.setPasswordHash(passwordEncoder.encode(password));
            shareLink.setReadableE2eeEnabled(e2eeMetadata.flag());
            shareLink.setReadableE2eeAlgorithm(e2eeMetadata.algorithm());
            shareLink.setReadableE2eeStoragePath(storagePath);
            shareLink.setReadableE2eeChecksum(sha256(content));
            shareLink.setStatus(SHARE_STATUS_ACTIVE);
            shareLink.setCreatedAt(now);
            shareLink.setUpdatedAt(now);
            shareLink.setDeleted(0);
            driveShareLinkMapper.insert(shareLink);

            AuditEventVo event = auditService.recordEvent(
                    userId,
                    "DRIVE_SHARE_CREATE",
                    "itemId=" + itemId + ",shareId=" + shareLink.getId() + ",permission=" + permission
                            + ",passwordProtected=true,readableE2ee=true",
                    ipAddress
            );
            suiteCollaborationService.publishToUser(userId, event);
            return toShareVo(shareLink);
        } catch (RuntimeException exception) {
            removeStoredFileIfExists(storagePath);
            throw exception;
        }
    }

    @Transactional
    public DriveBatchShareResultVo batchCreateShares(
            Long userId,
            BatchCreateDriveShareRequest request,
            String ipAddress
    ) {
        List<Long> itemIds = normalizeBatchItemIds(request.itemIds());
        List<DriveShareLinkVo> createdShares = new ArrayList<>();
        List<DriveBatchFailureVo> failedItems = new ArrayList<>();
        CreateDriveShareRequest shareRequest = new CreateDriveShareRequest(
                request.permission(),
                request.expiresAt(),
                request.password()
        );
        int successCount = 0;
        for (Long itemId : itemIds) {
            try {
                createdShares.add(createShare(userId, itemId, shareRequest, ipAddress));
                successCount += 1;
            } catch (BizException ex) {
                failedItems.add(new DriveBatchFailureVo(String.valueOf(itemId), ex.getMessage()));
            } catch (Exception ex) {
                failedItems.add(new DriveBatchFailureVo(String.valueOf(itemId), "Unexpected internal error"));
            }
        }
        auditService.record(
                userId,
                "DRIVE_SHARE_BATCH_CREATE",
                "requested=" + itemIds.size() + ",success=" + successCount + ",failed=" + failedItems.size(),
                ipAddress
        );
        return new DriveBatchShareResultVo(
                itemIds.size(),
                successCount,
                failedItems.size(),
                List.copyOf(createdShares),
                List.copyOf(failedItems)
        );
    }

    @Transactional
    public DriveShareLinkVo updateShare(Long userId, Long shareId, UpdateDriveShareRequest request, String ipAddress) {
        DriveShareLink shareLink = loadOwnedShareLink(userId, shareId);
        if (!SHARE_STATUS_ACTIVE.equals(shareLink.getStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive share link is revoked");
        }
        if (driveReadableShareE2eeService.isEnabled(shareLink.getReadableE2eeEnabled())) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "Drive readable-share E2EE links must be revoked and recreated to change settings"
            );
        }

        String permission = normalizeSharePermission(request.permission());
        LocalDateTime expiresAt = normalizeShareExpiry(request.expiresAt());
        String passwordHash = resolveUpdatedSharePasswordHash(shareLink.getPasswordHash(), request.password(), request.clearPassword());

        LocalDateTime now = LocalDateTime.now();
        driveShareLinkMapper.update(
                null,
                new LambdaUpdateWrapper<DriveShareLink>()
                        .eq(DriveShareLink::getId, shareId)
                        .eq(DriveShareLink::getOwnerId, userId)
                        .set(DriveShareLink::getPermission, permission)
                        .set(DriveShareLink::getExpiresAt, expiresAt)
                        .set(DriveShareLink::getPasswordHash, passwordHash)
                        .set(DriveShareLink::getUpdatedAt, now)
        );
        shareLink.setPermission(permission);
        shareLink.setExpiresAt(expiresAt);
        shareLink.setPasswordHash(passwordHash);
        shareLink.setUpdatedAt(now);

        AuditEventVo event = auditService.recordEvent(
                userId,
                "DRIVE_SHARE_UPDATE",
                "shareId=" + shareId + ",itemId=" + shareLink.getItemId() + ",permission=" + permission
                        + ",passwordProtected=" + StringUtils.hasText(passwordHash)
                        + ",expiresAt=" + (expiresAt == null ? "-" : expiresAt),
                ipAddress
        );
        suiteCollaborationService.publishToUser(userId, event);
        return toShareVo(shareLink);
    }

    @Transactional
    public DriveShareLinkVo revokeShare(Long userId, Long shareId, String ipAddress) {
        DriveShareLink shareLink = driveShareLinkMapper.selectOne(new LambdaQueryWrapper<DriveShareLink>()
                .eq(DriveShareLink::getId, shareId)
                .eq(DriveShareLink::getOwnerId, userId));
        if (shareLink == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive share link is not found");
        }
        if (SHARE_STATUS_REVOKED.equals(shareLink.getStatus())) {
            return toShareVo(shareLink);
        }

        shareLink.setStatus(SHARE_STATUS_REVOKED);
        shareLink.setUpdatedAt(LocalDateTime.now());
        driveShareLinkMapper.updateById(shareLink);
        if (driveReadableShareE2eeService.isEnabled(shareLink.getReadableE2eeEnabled())) {
            removeStoredFileIfExists(shareLink.getReadableE2eeStoragePath());
        }
        auditService.record(
                userId,
                "DRIVE_SHARE_REVOKE",
                "shareId=" + shareId + ",itemId=" + shareLink.getItemId(),
                ipAddress
        );
        return toShareVo(shareLink);
    }

    public DriveUsageVo usage(Long userId, String ipAddress) {
        long fileCount = safeCount(driveItemMapper.selectCount(new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getOwnerId, userId)
                .eq(DriveItem::getItemType, ITEM_TYPE_FILE)));
        long folderCount = safeCount(driveItemMapper.selectCount(new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getOwnerId, userId)
                .eq(DriveItem::getItemType, ITEM_TYPE_FOLDER)));
        long storageBytes = safeCount(driveItemMapper.selectStorageBytesByOwner(userId));
        long storageLimitBytes = suiteService.resolveDriveStorageLimitBytes(userId);
        auditService.record(
                userId,
                "DRIVE_USAGE_QUERY",
                "fileCount=" + fileCount + ",folderCount=" + folderCount + ",storageBytes=" + storageBytes,
                ipAddress
        );
        return new DriveUsageVo(fileCount, folderCount, storageBytes, storageLimitBytes);
    }

    private Map<Long, Integer> buildActiveShareCountMap(List<DriveItem> items) {
        if (items.isEmpty()) {
            return Map.of();
        }
        List<Long> itemIds = items.stream().map(DriveItem::getId).toList();
        List<DriveShareLink> shares = driveShareLinkMapper.selectList(new LambdaQueryWrapper<DriveShareLink>()
                .in(DriveShareLink::getItemId, itemIds)
                .eq(DriveShareLink::getStatus, SHARE_STATUS_ACTIVE));
        Map<Long, Integer> map = new HashMap<>();
        for (DriveShareLink share : shares) {
            map.put(share.getItemId(), map.getOrDefault(share.getItemId(), 0) + 1);
        }
        return map;
    }

    private int activeShareCount(Long itemId) {
        return Math.toIntExact(safeCount(driveShareLinkMapper.selectCount(new LambdaQueryWrapper<DriveShareLink>()
                .eq(DriveShareLink::getItemId, itemId)
                .eq(DriveShareLink::getStatus, SHARE_STATUS_ACTIVE))));
    }

    private DriveItem loadOwnedFile(Long userId, Long itemId) {
        DriveItem item = loadOwnedItem(userId, itemId);
        if (!ITEM_TYPE_FILE.equals(item.getItemType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive item is not a file");
        }
        return item;
    }

    private ShareAndItem loadPublicShareAndItemForAction(
            String rawToken,
            String rawPassword,
            String ipAddress,
            String userAgent,
            String action,
            boolean requirePassword
    ) {
        String token = normalizeShareToken(rawToken);
        if (isPublicShareRateLimited(token, ipAddress, action)) {
            recordPublicAccess(resolveShareForAudit(token), token, action, ACCESS_STATUS_DENY_RATE_LIMIT, ipAddress, userAgent);
            throw new BizException(ErrorCode.RATE_LIMITED, "Public share access is temporarily rate limited");
        }

        if (!StringUtils.hasText(token)) {
            recordPublicAccess(null, rawToken, action, ACCESS_STATUS_DENY_INVALID_TOKEN, ipAddress, userAgent);
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Share link is unavailable");
        }

        String tokenHash = publicShareTokenCodec.hash(token);
        DriveShareLink share = driveShareLinkMapper.selectOne(new LambdaQueryWrapper<DriveShareLink>()
                .eq(DriveShareLink::getTokenHash, tokenHash));
        if (share == null) {
            recordPublicAccess(null, token, action, ACCESS_STATUS_DENY_INVALID_TOKEN, ipAddress, userAgent);
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Share link is unavailable");
        }
        if (!SHARE_STATUS_ACTIVE.equals(share.getStatus())) {
            recordPublicAccess(new ShareAndItem(share, null), token, action, ACCESS_STATUS_DENY_REVOKED, ipAddress, userAgent);
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Share link is unavailable");
        }
        if (share.getExpiresAt() != null && !share.getExpiresAt().isAfter(LocalDateTime.now())) {
            recordPublicAccess(new ShareAndItem(share, null), token, action, ACCESS_STATUS_DENY_EXPIRED, ipAddress, userAgent);
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Share link is unavailable");
        }
        DriveItem item = driveItemMapper.selectOne(new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getId, share.getItemId())
                .eq(DriveItem::getOwnerId, share.getOwnerId()));
        if (item == null) {
            recordPublicAccess(new ShareAndItem(share, null), token, action, ACCESS_STATUS_DENY_FILE_MISSING, ipAddress, userAgent);
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Share link is unavailable");
        }
        ShareAndItem pair = new ShareAndItem(share, item);
        if (requirePassword) {
            assertPublicSharePassword(pair, token, rawPassword, action, ipAddress, userAgent);
        }
        return pair;
    }

    private DriveItem requirePublicSharedFolder(DriveItem item) {
        if (!ITEM_TYPE_FOLDER.equals(item.getItemType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared item is not a folder");
        }
        return item;
    }

    private DriveItem requirePublicSharedFile(DriveItem item) {
        if (!ITEM_TYPE_FILE.equals(item.getItemType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared item is not a file");
        }
        return item;
    }

    private DriveItem resolvePublicShareFolderTarget(DriveItem rootFolder, Long parentId) {
        if (parentId == null || rootFolder.getId().equals(parentId)) {
            return rootFolder;
        }
        DriveItem folder = loadOwnedItemForShare(rootFolder.getOwnerId(), parentId, "Shared folder is unavailable");
        if (!ITEM_TYPE_FOLDER.equals(folder.getItemType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared folder is unavailable");
        }
        assertItemWithinSharedRoot(rootFolder, folder, "Shared folder is unavailable");
        return folder;
    }

    private DriveItem resolvePublicShareFileTarget(DriveItem rootFolder, Long itemId) {
        DriveItem file = loadOwnedItemForShare(rootFolder.getOwnerId(), itemId, "Shared file is unavailable");
        if (!ITEM_TYPE_FILE.equals(file.getItemType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared file is unavailable");
        }
        assertItemWithinSharedRoot(rootFolder, file, "Shared file is unavailable");
        return file;
    }

    private DriveItem loadOwnedItemForShare(Long ownerId, Long itemId, String errorMessage) {
        DriveItem item = driveItemMapper.selectOne(new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getId, itemId)
                .eq(DriveItem::getOwnerId, ownerId));
        if (item == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, errorMessage);
        }
        return item;
    }

    private void assertItemWithinSharedRoot(DriveItem rootFolder, DriveItem item, String errorMessage) {
        if (rootFolder.getId().equals(item.getId())) {
            return;
        }
        Set<Long> visited = new HashSet<>();
        DriveItem cursor = item;
        while (cursor.getParentId() != null && visited.add(cursor.getId())) {
            DriveItem parent = driveItemMapper.selectOne(new LambdaQueryWrapper<DriveItem>()
                    .eq(DriveItem::getId, cursor.getParentId())
                    .eq(DriveItem::getOwnerId, rootFolder.getOwnerId()));
            if (parent == null) {
                break;
            }
            if (rootFolder.getId().equals(parent.getId())) {
                return;
            }
            cursor = parent;
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, errorMessage);
    }

    private ShareAndItem resolveShareForAudit(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String tokenHash = publicShareTokenCodec.hash(token);
        DriveShareLink share = driveShareLinkMapper.selectOne(new LambdaQueryWrapper<DriveShareLink>()
                .eq(DriveShareLink::getTokenHash, tokenHash));
        if (share == null) {
            return null;
        }
        return new ShareAndItem(share, null);
    }

    private PreviewContent readStoredPreviewContent(DriveItem item) {
        assertServerPreviewAllowed(item);
        String mimeType = normalizeMimeType(item.getMimeType());
        String path = item.getStoragePath();
        if (!StringUtils.hasText(path)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file content is unavailable");
        }
        if (!isPathWithinStorageRoot(path)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file content is unavailable");
        }
        if (isTextPreviewMimeType(mimeType)) {
            return readTextPreview(path);
        }
        if (isBinaryPreviewMimeType(mimeType)) {
            return new PreviewContent(readStoredFile(item), false);
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive preview is unavailable for current file type");
    }

    private PreviewContent readTextPreview(String storagePath) {
        int maxBytes = effectivePreviewTextMaxBytes();
        try (InputStream inputStream = Files.newInputStream(Paths.get(storagePath))) {
            byte[] raw = inputStream.readNBytes(maxBytes + 1);
            if (raw.length <= maxBytes) {
                return new PreviewContent(raw, false);
            }
            return new PreviewContent(Arrays.copyOf(raw, maxBytes), true);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file content is unavailable");
        }
    }

    private boolean isTextPreviewMimeType(String mimeType) {
        return mimeType.startsWith("text/")
                || mimeType.contains("json")
                || mimeType.contains("xml");
    }

    private boolean isBinaryPreviewMimeType(String mimeType) {
        return mimeType.startsWith("image/")
                || "application/pdf".equals(mimeType);
    }

    private String normalizeMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return DEFAULT_BINARY_MIME;
        }
        return mimeType.trim().toLowerCase();
    }

    private PreparedUpload prepareOwnedUpload(
            Long userId,
            UploadDriveFileRequest request,
            String fixedFileName,
            String ipAddress
    ) {
        MultipartFile file = requireUploadFile(request.getFile());
        byte[] content = readUploadBytes(file);
        long storedSize = validateUploadSize(content.length);
        suiteService.assertDriveStorageQuota(userId, storedSize, ipAddress);
        DriveFileE2eeService.DriveFileE2eeMetadata e2eeMetadata = driveFileE2eeService.resolveUpload(
                request.getE2eeEnabled(),
                request.getE2eeAlgorithm(),
                request.getE2eeRecipientFingerprintsJson()
        );
        String fileName = resolveUploadFileName(request, file, fixedFileName, e2eeMetadata.enabled());
        String mimeType = resolveUploadMimeType(request, file, e2eeMetadata.enabled());
        String storagePath = normalizeNullableText(storeUploadedFile(userId, fileName, content), 512);
        return new PreparedUpload(fileName, mimeType, storedSize, storagePath, sha256(content), e2eeMetadata);
    }

    private MultipartFile requireUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "file is required");
        }
        return file;
    }

    private long validateUploadSize(long sizeBytes) {
        if (sizeBytes <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "file size must be greater than 0");
        }
        assertUploadSizeWithinLimit(sizeBytes);
        return sizeBytes;
    }

    private String resolveUploadFileName(
            UploadDriveFileRequest request,
            MultipartFile file,
            String fixedFileName,
            boolean e2eeEnabled
    ) {
        if (StringUtils.hasText(fixedFileName)) {
            return normalizeName(fixedFileName);
        }
        if (!e2eeEnabled) {
            return normalizeName(resolveUploadName(file));
        }
        return normalizeName(requireText(request.getFileName(), "Drive file fileName is required"));
    }

    private String resolveUploadMimeType(
            UploadDriveFileRequest request,
            MultipartFile file,
            boolean e2eeEnabled
    ) {
        if (!e2eeEnabled) {
            return normalizeMimeType(file.getContentType());
        }
        return normalizeMimeType(requireText(request.getContentType(), "Drive file contentType is required"));
    }

    private int effectivePreviewTextMaxBytes() {
        if (previewTextMaxBytes == null || previewTextMaxBytes <= 0) {
            return DEFAULT_PREVIEW_TEXT_MAX_BYTES;
        }
        return previewTextMaxBytes;
    }

    private int effectiveRecycleBinRetentionDays() {
        if (recycleBinRetentionDays == null || recycleBinRetentionDays <= 0) {
            return DEFAULT_RECYCLE_BIN_RETENTION_DAYS;
        }
        return recycleBinRetentionDays;
    }

    private int effectivePublicRateLimitWindowSeconds() {
        if (publicShareRateLimitWindowSeconds == null || publicShareRateLimitWindowSeconds <= 0) {
            return DEFAULT_PUBLIC_RATE_LIMIT_WINDOW_SECONDS;
        }
        return publicShareRateLimitWindowSeconds;
    }

    private int effectivePublicRateLimitMaxRequests() {
        if (publicShareRateLimitMaxRequests == null || publicShareRateLimitMaxRequests <= 0) {
            return DEFAULT_PUBLIC_RATE_LIMIT_MAX_REQUESTS;
        }
        return publicShareRateLimitMaxRequests;
    }

    private void assertUploadSizeWithinLimit(long sizeBytes) {
        long maxSizeBytes = effectiveUploadMaxFileSizeBytes();
        if (sizeBytes > maxSizeBytes) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file size exceeded max upload limit");
        }
    }

    private long effectiveUploadMaxFileSizeBytes() {
        if (uploadMaxFileSizeBytes == null || uploadMaxFileSizeBytes <= 0L) {
            return 52_428_800L;
        }
        return uploadMaxFileSizeBytes;
    }

    private String effectivePublicRateLimitRedisKeyPrefix() {
        if (!StringUtils.hasText(publicShareRateLimitRedisKeyPrefix)) {
            return DEFAULT_PUBLIC_RATE_LIMIT_REDIS_KEY_PREFIX;
        }
        return publicShareRateLimitRedisKeyPrefix.trim();
    }

    private boolean isPublicShareRateLimited(String token, String ipAddress, String action) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        String normalizedIp = StringUtils.hasText(ipAddress) ? ipAddress.trim() : "unknown";
        int windowSeconds = effectivePublicRateLimitWindowSeconds();
        int maxRequests = effectivePublicRateLimitMaxRequests();
        Boolean redisLimited = isPublicShareRateLimitedByRedis(token, normalizedIp, action, windowSeconds, maxRequests);
        if (redisLimited != null) {
            return redisLimited;
        }
        long nowSeconds = Instant.now().getEpochSecond();
        String counterKey = token + ":" + normalizedIp + ":" + action;
        AtomicBoolean limited = new AtomicBoolean(false);
        publicRateCounterMap.compute(counterKey, (key, existing) -> {
            PublicRateCounter counter = existing == null ? new PublicRateCounter(nowSeconds) : existing;
            if ((nowSeconds - counter.windowStartSeconds()) >= windowSeconds) {
                counter.reset(nowSeconds);
            }
            int current = counter.count().incrementAndGet();
            counter.setLastSeenSeconds(nowSeconds);
            if (current > maxRequests) {
                limited.set(true);
            }
            return counter;
        });
        cleanupPublicRateCounterIfNeeded(nowSeconds, windowSeconds);
        return limited.get();
    }

    private Boolean isPublicShareRateLimitedByRedis(
            String token,
            String normalizedIp,
            String action,
            int windowSeconds,
            int maxRequests
    ) {
        StringRedisTemplate redisTemplate = stringRedisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return null;
        }
        String key = effectivePublicRateLimitRedisKeyPrefix() + ":" + token + ":" + normalizedIp + ":" + action;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                return null;
            }
            if (count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            return count > maxRequests;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void cleanupPublicRateCounterIfNeeded(long nowSeconds, int windowSeconds) {
        if (publicRateCounterMap.size() < 10_000) {
            return;
        }
        long expireBefore = nowSeconds - (windowSeconds * 2L);
        publicRateCounterMap.entrySet().removeIf(entry -> entry.getValue().lastSeenSeconds() < expireBefore);
    }

    private void recordPublicAccess(
            ShareAndItem pair,
            String token,
            String action,
            String accessStatus,
            String ipAddress,
            String userAgent
    ) {
        DriveShareAccessLog log = new DriveShareAccessLog();
        if (pair != null) {
            if (pair.share() != null) {
                log.setOwnerId(pair.share().getOwnerId());
                log.setShareId(pair.share().getId());
                log.setItemId(pair.share().getItemId());
            } else if (pair.item() != null) {
                log.setOwnerId(pair.item().getOwnerId());
                log.setItemId(pair.item().getId());
            }
        }
        log.setTokenHash(publicShareTokenCodec.hash(token == null ? "" : token));
        log.setAction(action);
        log.setAccessStatus(accessStatus);
        log.setIpAddress(normalizeNullableText(ipAddress, 64));
        log.setUserAgent(normalizeNullableText(userAgent, 512));
        LocalDateTime now = LocalDateTime.now();
        log.setCreatedAt(now);
        log.setUpdatedAt(now);
        log.setDeleted(0);
        driveShareAccessLogMapper.insert(log);
    }

    private String resolveUploadName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName)) {
            return "upload.bin";
        }
        String trimmed = originalName.trim();
        String withoutPath = Paths.get(trimmed).getFileName().toString();
        return withoutPath.replace("\\", "_").replace("/", "_");
    }

    private byte[] readUploadBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to read uploaded file");
        }
    }

    private void assertServerPreviewAllowed(DriveItem item) {
        if (driveFileE2eeService.isEnabled(item.getE2eeEnabled())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, DRIVE_E2EE_PREVIEW_UNAVAILABLE);
        }
    }

    private void applyE2eeMetadata(DriveItem item, DriveFileE2eeService.DriveFileE2eeMetadata metadata) {
        item.setE2eeEnabled(metadata.flag());
        item.setE2eeAlgorithm(metadata.algorithm());
        item.setE2eeFingerprintsJson(metadata.fingerprintsJson());
    }

    private String storeUploadedFile(Long userId, String fileName, byte[] content) {
        String safeName = sanitizeStorageName(fileName);
        String uniqueName = UUID.randomUUID().toString().replace("-", "") + "-" + safeName;
        Path rootPath = Paths.get(driveStorageRoot).toAbsolutePath().normalize();
        Path ownerDir = rootPath.resolve(String.valueOf(userId)).normalize();
        Path target = ownerDir.resolve(uniqueName).normalize();
        if (!target.startsWith(ownerDir)) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Invalid upload target path");
        }
        try {
            Files.createDirectories(ownerDir);
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
        if (!StringUtils.hasText(storagePath)) {
            return;
        }
        if (!isPathWithinStorageRoot(storagePath)) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(storagePath));
        } catch (IOException ignored) {
            // keep DB consistency as primary; stale files can be cleaned by maintenance task.
        }
    }

    private void snapshotCurrentItemAsVersion(DriveItem currentItem) {
        if (currentItem == null) {
            return;
        }
        if (!StringUtils.hasText(currentItem.getStoragePath()) || !isPathWithinStorageRoot(currentItem.getStoragePath())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file content is unavailable");
        }
        DriveFileVersion version = new DriveFileVersion();
        LocalDateTime now = LocalDateTime.now();
        version.setOwnerId(currentItem.getOwnerId());
        version.setItemId(currentItem.getId());
        version.setVersionNo(nextVersionNo(currentItem.getOwnerId(), currentItem.getId()));
        version.setMimeType(currentItem.getMimeType());
        version.setSizeBytes(currentItem.getSizeBytes() == null ? 0L : currentItem.getSizeBytes());
        version.setStoragePath(currentItem.getStoragePath());
        version.setChecksum(currentItem.getChecksum());
        version.setE2eeEnabled(currentItem.getE2eeEnabled());
        version.setE2eeAlgorithm(currentItem.getE2eeAlgorithm());
        version.setE2eeFingerprintsJson(currentItem.getE2eeFingerprintsJson());
        version.setCreatedAt(now);
        version.setUpdatedAt(now);
        version.setDeleted(0);
        driveFileVersionMapper.insert(version);
    }

    private int nextVersionNo(Long userId, Long itemId) {
        DriveFileVersion latestVersion = driveFileVersionMapper.selectOne(new LambdaQueryWrapper<DriveFileVersion>()
                .eq(DriveFileVersion::getOwnerId, userId)
                .eq(DriveFileVersion::getItemId, itemId)
                .orderByDesc(DriveFileVersion::getVersionNo)
                .last("limit 1"));
        if (latestVersion == null || latestVersion.getVersionNo() == null || latestVersion.getVersionNo() < 1) {
            return 1;
        }
        return latestVersion.getVersionNo() + 1;
    }

    private DriveVersionCleanupVo applyVersionRetentionPolicy(
            Long userId,
            DriveItem currentItem,
            String auditEventType,
            String ipAddress
    ) {
        VersionRetentionConfig config = resolveVersionRetentionConfig(userId);
        List<DriveFileVersion> versions = driveFileVersionMapper.selectList(new LambdaQueryWrapper<DriveFileVersion>()
                .eq(DriveFileVersion::getOwnerId, userId)
                .eq(DriveFileVersion::getItemId, currentItem.getId())
                .orderByDesc(DriveFileVersion::getVersionNo)
                .orderByDesc(DriveFileVersion::getCreatedAt));
        if (versions.isEmpty()) {
            DriveVersionCleanupVo emptyResult = new DriveVersionCleanupVo(
                    0,
                    0,
                    config.retentionCount(),
                    config.retentionDays()
            );
            auditService.record(
                    userId,
                    auditEventType,
                    "itemId=" + currentItem.getId() + ",deletedVersions=0,remainingVersions=0,retentionCount="
                            + config.retentionCount() + ",retentionDays=" + config.retentionDays(),
                    ipAddress
            );
            return emptyResult;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(config.retentionDays());
        Set<Long> deleteIds = new HashSet<>();
        List<DriveFileVersion> deleteCandidates = new java.util.ArrayList<>();
        Set<String> retainedPaths = new HashSet<>();

        for (int index = 0; index < versions.size(); index++) {
            DriveFileVersion version = versions.get(index);
            boolean exceedCount = index >= config.retentionCount();
            boolean exceedDays = version.getCreatedAt() != null && version.getCreatedAt().isBefore(cutoff);
            if (exceedCount || exceedDays) {
                deleteIds.add(version.getId());
                deleteCandidates.add(version);
            } else if (StringUtils.hasText(version.getStoragePath())) {
                retainedPaths.add(version.getStoragePath());
            }
        }

        String currentPath = currentItem.getStoragePath();
        for (DriveFileVersion version : deleteCandidates) {
            String path = version.getStoragePath();
            if (!StringUtils.hasText(path)) {
                continue;
            }
            if (StringUtils.hasText(currentPath) && currentPath.equals(path)) {
                continue;
            }
            if (retainedPaths.contains(path)) {
                continue;
            }
            removeStoredFileIfExists(path);
        }

        int deleted = 0;
        if (!deleteIds.isEmpty()) {
            deleted = driveFileVersionMapper.deleteBatchIds(deleteIds);
        }
        int remaining = Math.max(0, versions.size() - deleted);
        DriveVersionCleanupVo result = new DriveVersionCleanupVo(
                deleted,
                remaining,
                config.retentionCount(),
                config.retentionDays()
        );
        auditService.record(
                userId,
                auditEventType,
                "itemId=" + currentItem.getId() + ",deletedVersions=" + deleted + ",remainingVersions=" + remaining
                        + ",retentionCount=" + config.retentionCount() + ",retentionDays=" + config.retentionDays(),
                ipAddress
        );
        return result;
    }

    private VersionRetentionConfig resolveVersionRetentionConfig(Long userId) {
        UserPreference preference = userPreferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getOwnerId, userId));
        int retentionCount = clamp(
                preference == null ? null : preference.getDriveVersionRetentionCount(),
                DEFAULT_VERSION_RETENTION_COUNT,
                MIN_VERSION_RETENTION_COUNT,
                MAX_VERSION_RETENTION_COUNT
        );
        int retentionDays = clamp(
                preference == null ? null : preference.getDriveVersionRetentionDays(),
                DEFAULT_VERSION_RETENTION_DAYS,
                MIN_VERSION_RETENTION_DAYS,
                MAX_VERSION_RETENTION_DAYS
        );
        return new VersionRetentionConfig(retentionCount, retentionDays);
    }

    private int clamp(Integer candidate, int defaultValue, int minValue, int maxValue) {
        int value = candidate == null ? defaultValue : candidate;
        return Math.max(minValue, Math.min(value, maxValue));
    }

    private byte[] readStoredFile(DriveItem item) {
        String path = item.getStoragePath();
        if (!StringUtils.hasText(path)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file content is unavailable");
        }
        if (!isPathWithinStorageRoot(path)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file content is unavailable");
        }
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file content is unavailable");
        }
    }

    private byte[] readStoredReadableShareFile(DriveShareLink share) {
        String path = share.getReadableE2eeStoragePath();
        if (!StringUtils.hasText(path)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive readable-share E2EE content is unavailable");
        }
        if (!isPathWithinStorageRoot(path)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive readable-share E2EE content is unavailable");
        }
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive readable-share E2EE content is unavailable");
        }
    }

    private boolean isPathWithinStorageRoot(String filePath) {
        try {
            Path root = Paths.get(driveStorageRoot).toAbsolutePath().normalize();
            Path target = Paths.get(filePath).toAbsolutePath().normalize();
            return target.startsWith(root);
        } catch (Exception ex) {
            return false;
        }
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "SHA-256 digest is unavailable");
        }
    }

    private DriveItem loadOwnedItem(Long userId, Long itemId) {
        DriveItem item = driveItemMapper.selectOne(new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getId, itemId)
                .eq(DriveItem::getOwnerId, userId));
        if (item == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive item is not found");
        }
        return item;
    }

    private void ensureParentFolder(Long userId, Long parentId) {
        if (parentId == null) {
            return;
        }
        DriveItem parent = loadOwnedItem(userId, parentId);
        if (!ITEM_TYPE_FOLDER.equals(parent.getItemType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Parent item must be a folder");
        }
    }

    private Long normalizeRestoreParentId(Long userId, Long parentId) {
        if (parentId == null) {
            return null;
        }
        try {
            DriveItem parent = loadOwnedItem(userId, parentId);
            if (ITEM_TYPE_FOLDER.equals(parent.getItemType())) {
                return parent.getId();
            }
            return null;
        } catch (BizException ignored) {
            return null;
        }
    }

    private void validateMoveTarget(Long userId, DriveItem item, Long targetParentId) {
        if (targetParentId == null) {
            return;
        }
        if (item.getId().equals(targetParentId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Cannot move item into itself");
        }

        DriveItem cursor = loadOwnedItem(userId, targetParentId);
        if (!ITEM_TYPE_FOLDER.equals(cursor.getItemType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Target parent must be a folder");
        }
        while (cursor.getParentId() != null) {
            if (item.getId().equals(cursor.getParentId())) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Cannot move item into its descendant");
            }
            cursor = loadOwnedItem(userId, cursor.getParentId());
        }
    }

    private void ensureNameUnique(
            Long userId,
            Long parentId,
            String itemType,
            String name,
            Long excludeId
    ) {
        LambdaQueryWrapper<DriveItem> query = new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getOwnerId, userId)
                .eq(DriveItem::getItemType, itemType)
                .eq(DriveItem::getName, name);
        if (parentId == null) {
            query.isNull(DriveItem::getParentId);
        } else {
            query.eq(DriveItem::getParentId, parentId);
        }
        if (excludeId != null) {
            query.ne(DriveItem::getId, excludeId);
        }
        if (driveItemMapper.selectCount(query) > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive item with same name already exists in target folder");
        }
    }

    private String normalizeName(String rawName) {
        if (!StringUtils.hasText(rawName)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Name is required");
        }
        String normalized = rawName.trim();
        if (normalized.length() > 128) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Name is too long");
        }
        return normalized;
    }

    private String normalizeItemTypeOrNull(String itemType) {
        if (!StringUtils.hasText(itemType)) {
            return null;
        }
        String normalized = itemType.trim().toUpperCase();
        if (!ITEM_TYPE_FOLDER.equals(normalized) && !ITEM_TYPE_FILE.equals(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported itemType");
        }
        return normalized;
    }

    private void assertSaveForLaterAllowed(
            Long userId,
            ShareAndItem pair,
            String token,
            String ipAddress,
            String userAgent
    ) {
        if (!SHARE_PERMISSION_VIEW.equals(pair.share().getPermission())) {
            recordPublicAccess(pair, token, PUBLIC_ACTION_SAVE, ACCESS_STATUS_DENY_PERMISSION, ipAddress, userAgent);
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Only view-only shared links can be saved");
        }
        Long existing = driveSavedShareMapper.selectCount(new LambdaQueryWrapper<DriveSavedShare>()
                .eq(DriveSavedShare::getRecipientUserId, userId)
                .eq(DriveSavedShare::getShareId, pair.share().getId()));
        if (safeCount(existing) > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive share is already saved");
        }
    }

    private DriveSavedShare buildSavedShare(Long userId, ShareAndItem pair, UserAccount owner) {
        LocalDateTime now = LocalDateTime.now();
        DriveSavedShare savedShare = new DriveSavedShare();
        savedShare.setRecipientUserId(userId);
        savedShare.setShareId(pair.share().getId());
        savedShare.setOwnerId(pair.share().getOwnerId());
        savedShare.setOwnerEmail(owner.getEmail());
        savedShare.setOwnerDisplayName(owner.getDisplayName());
        savedShare.setItemId(pair.item().getId());
        savedShare.setItemType(pair.item().getItemType());
        savedShare.setItemName(pair.item().getName());
        savedShare.setToken(pair.share().getToken());
        savedShare.setPermission(pair.share().getPermission());
        savedShare.setCreatedAt(now);
        savedShare.setUpdatedAt(now);
        savedShare.setDeleted(0);
        return savedShare;
    }

    private DriveSavedShare loadSavedShare(Long userId, Long savedShareId) {
        DriveSavedShare savedShare = driveSavedShareMapper.selectOne(new LambdaQueryWrapper<DriveSavedShare>()
                .eq(DriveSavedShare::getId, savedShareId)
                .eq(DriveSavedShare::getRecipientUserId, userId));
        if (savedShare == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared with me item is not found");
        }
        return savedShare;
    }

    private UserAccount loadRequiredUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "User not found");
        }
        return user;
    }

    private Map<Long, DriveShareLink> loadSavedShareLinkMap(List<DriveSavedShare> savedShares) {
        Set<Long> shareIds = new LinkedHashSet<>();
        for (DriveSavedShare savedShare : savedShares) {
            shareIds.add(savedShare.getShareId());
        }
        if (shareIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, DriveShareLink> result = new HashMap<>();
        for (DriveShareLink share : driveShareLinkMapper.selectBatchIds(shareIds)) {
            result.put(share.getId(), share);
        }
        return result;
    }

    private Map<Long, DriveItem> loadSavedShareItemMap(List<DriveSavedShare> savedShares, Map<Long, DriveShareLink> shareMap) {
        Set<Long> itemIds = new LinkedHashSet<>();
        for (DriveSavedShare savedShare : savedShares) {
            DriveShareLink share = shareMap.get(savedShare.getShareId());
            itemIds.add(share == null ? savedShare.getItemId() : share.getItemId());
        }
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, DriveItem> result = new HashMap<>();
        for (DriveItem item : driveItemMapper.selectBatchIds(itemIds)) {
            result.put(item.getId(), item);
        }
        return result;
    }

    private Map<Long, UserAccount> loadSavedShareOwnerMap(List<DriveSavedShare> savedShares) {
        Set<Long> ownerIds = new LinkedHashSet<>();
        for (DriveSavedShare savedShare : savedShares) {
            ownerIds.add(savedShare.getOwnerId());
        }
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, UserAccount> result = new HashMap<>();
        for (UserAccount owner : userAccountMapper.selectBatchIds(ownerIds)) {
            result.put(owner.getId(), owner);
        }
        return result;
    }

    private String normalizeSharePermission(String permission) {
        if (!StringUtils.hasText(permission)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "permission is required");
        }
        String normalized = permission.trim().toUpperCase();
        if (!SHARE_PERMISSION_VIEW.equals(normalized) && !SHARE_PERMISSION_EDIT.equals(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported share permission");
        }
        return normalized;
    }

    private LocalDateTime normalizeShareExpiry(LocalDateTime expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "expiresAt must be in the future");
        }
        return expiresAt;
    }

    private DriveShareLink loadOwnedShareLink(Long userId, Long shareId) {
        DriveShareLink shareLink = driveShareLinkMapper.selectOne(new LambdaQueryWrapper<DriveShareLink>()
                .eq(DriveShareLink::getId, shareId)
                .eq(DriveShareLink::getOwnerId, userId));
        if (shareLink == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive share link is not found");
        }
        return shareLink;
    }

    private String encodeSharePassword(String rawPassword) {
        String normalized = normalizeOptionalSharePassword(rawPassword);
        return normalized == null ? null : passwordEncoder.encode(normalized);
    }

    private String requireSharePassword(String rawPassword) {
        String normalized = normalizeOptionalSharePassword(rawPassword);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "Drive readable-share E2EE requires a password-protected public link"
            );
        }
        return normalized;
    }

    private String resolveUpdatedSharePasswordHash(String currentHash, String rawPassword, Boolean clearPassword) {
        if (Boolean.TRUE.equals(clearPassword)) {
            return null;
        }
        String normalized = normalizeOptionalSharePassword(rawPassword);
        return normalized == null ? currentHash : passwordEncoder.encode(normalized);
    }

    private String normalizeOptionalSharePassword(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            return null;
        }
        String normalized = rawPassword.trim();
        if (normalized.length() > MAX_SHARE_PASSWORD_LENGTH) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive share password is too long");
        }
        return normalized;
    }

    private String encryptedReadableShareFileName(String fileName) {
        String normalized = StringUtils.hasText(fileName) ? fileName.trim() : "drive-share";
        if (normalized.endsWith(".pgp")) {
            return normalized;
        }
        return normalized + ".pgp";
    }

    private void assertPublicSharePassword(
            ShareAndItem pair,
            String token,
            String rawPassword,
            String action,
            String ipAddress,
            String userAgent
    ) {
        String passwordHash = pair.share() == null ? null : pair.share().getPasswordHash();
        if (!StringUtils.hasText(passwordHash)) {
            return;
        }
        String normalized = normalizeOptionalSharePassword(rawPassword);
        if (!StringUtils.hasText(normalized)) {
            recordPublicAccess(pair, token, action, ACCESS_STATUS_DENY_PASSWORD_REQUIRED, ipAddress, userAgent);
            throw new BizException(ErrorCode.UNAUTHORIZED, "Drive share password is required");
        }
        if (!passwordEncoder.matches(normalized, passwordHash)) {
            recordPublicAccess(pair, token, action, ACCESS_STATUS_DENY_PASSWORD_INVALID, ipAddress, userAgent);
            throw new BizException(ErrorCode.UNAUTHORIZED, "Drive share password is invalid");
        }
    }

    private String normalizeNullableLogAction(String action) {
        if (!StringUtils.hasText(action)) {
            return null;
        }
        String normalized = action.trim().toUpperCase();
        if (!Set.of(PUBLIC_ACTION_METADATA, PUBLIC_ACTION_LIST, PUBLIC_ACTION_DOWNLOAD, PUBLIC_ACTION_PREVIEW, PUBLIC_ACTION_UPLOAD, PUBLIC_ACTION_SAVE).contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported access log action");
        }
        return normalized;
    }

    private String normalizeNullableAccessStatus(String accessStatus) {
        if (!StringUtils.hasText(accessStatus)) {
            return null;
        }
        String normalized = accessStatus.trim().toUpperCase();
        Set<String> supported = Set.of(
                ACCESS_STATUS_ALLOW,
                ACCESS_STATUS_DENY_RATE_LIMIT,
                ACCESS_STATUS_DENY_INVALID_TOKEN,
                ACCESS_STATUS_DENY_REVOKED,
                ACCESS_STATUS_DENY_EXPIRED,
                ACCESS_STATUS_DENY_FILE_MISSING,
                ACCESS_STATUS_DENY_PERMISSION,
                ACCESS_STATUS_DENY_PASSWORD_REQUIRED,
                ACCESS_STATUS_DENY_PASSWORD_INVALID,
                ACCESS_STATUS_DENY_UNSUPPORTED_PREVIEW
        );
        if (!supported.contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported access log status");
        }
        return normalized;
    }

    private String normalizeShareToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "";
        }
        String normalized = token.trim();
        return normalized.length() >= 12 ? normalized : "";
    }

    private List<Long> normalizeBatchItemIds(List<String> rawIds) {
        if (rawIds == null || rawIds.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "itemIds is required");
        }
        Set<Long> dedupe = new LinkedHashSet<>();
        for (String rawId : rawIds) {
            if (!StringUtils.hasText(rawId)) {
                continue;
            }
            try {
                long id = Long.parseLong(rawId.trim());
                if (id > 0) {
                    dedupe.add(id);
                }
            } catch (NumberFormatException ignored) {
                // Invalid IDs are skipped; request-level empty validation is applied below.
            }
        }
        if (dedupe.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "itemIds is required");
        }
        if (dedupe.size() > MAX_BATCH_ITEMS) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "itemIds exceeded max batch size");
        }
        return List.copyOf(dedupe);
    }

    private DriveBatchActionResultVo executeBatchAction(
            Long userId,
            List<Long> itemIds,
            String ipAddress,
            String auditEventType,
            BatchItemHandler handler
    ) {
        List<DriveBatchFailureVo> failedItems = new ArrayList<>();
        int successCount = 0;
        for (Long itemId : itemIds) {
            try {
                handler.apply(itemId);
                successCount += 1;
            } catch (BizException ex) {
                failedItems.add(new DriveBatchFailureVo(String.valueOf(itemId), ex.getMessage()));
            } catch (Exception ex) {
                failedItems.add(new DriveBatchFailureVo(String.valueOf(itemId), "Unexpected internal error"));
            }
        }

        auditService.record(
                userId,
                auditEventType,
                "requested=" + itemIds.size() + ",success=" + successCount + ",failed=" + failedItems.size(),
                ipAddress
        );
        return new DriveBatchActionResultVo(
                itemIds.size(),
                successCount,
                failedItems.size(),
                failedItems
        );
    }

    private String normalizeKeyword(String rawKeyword) {
        if (!StringUtils.hasText(rawKeyword)) {
            return "";
        }
        return rawKeyword.trim();
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return value.trim();
    }

    private String normalizeNullableText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Text length exceeded");
        }
        return normalized;
    }

    private String generateShareToken() {
        for (int i = 0; i < 8; i++) {
            String candidate = publicShareTokenCodec.generateRawToken();
            String tokenHash = publicShareTokenCodec.hash(candidate);
            long count = safeCount(driveShareLinkMapper.selectCount(new LambdaQueryWrapper<DriveShareLink>()
                    .eq(DriveShareLink::getTokenHash, tokenHash)));
            if (count == 0) {
                return candidate;
            }
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to generate unique share token");
    }

    private DriveSavedShareVo toSavedShareVo(
            DriveSavedShare savedShare,
            Map<Long, DriveShareLink> shareMap,
            Map<Long, DriveItem> itemMap,
            Map<Long, UserAccount> ownerMap
    ) {
        DriveShareLink share = shareMap.get(savedShare.getShareId());
        Long itemId = share == null ? savedShare.getItemId() : share.getItemId();
        DriveItem item = itemMap.get(itemId);
        UserAccount owner = ownerMap.get(savedShare.getOwnerId());
        return toSavedShareVo(savedShare, share, item, owner);
    }

    private DriveSavedShareVo toSavedShareVo(
            DriveSavedShare savedShare,
            DriveShareLink share,
            DriveItem item,
            UserAccount owner
    ) {
        String status = resolveSavedShareStatus(share, item);
        boolean available = SAVED_SHARE_STATUS_ACTIVE.equals(status);
        String itemId = item == null ? String.valueOf(savedShare.getItemId()) : String.valueOf(item.getId());
        String itemType = item == null ? savedShare.getItemType() : item.getItemType();
        String itemName = item == null ? savedShare.getItemName() : item.getName();
        String ownerEmail = owner == null ? savedShare.getOwnerEmail() : owner.getEmail();
        String ownerDisplayName = owner == null ? savedShare.getOwnerDisplayName() : owner.getDisplayName();
        String permission = share == null ? savedShare.getPermission() : share.getPermission();
        String token = share == null ? savedShare.getToken() : share.getToken();
        LocalDateTime expiresAt = share == null ? null : share.getExpiresAt();
        return new DriveSavedShareVo(
                String.valueOf(savedShare.getId()),
                String.valueOf(savedShare.getShareId()),
                token,
                itemId,
                itemType,
                itemName,
                ownerEmail,
                ownerDisplayName,
                permission,
                status,
                expiresAt,
                savedShare.getCreatedAt(),
                available,
                toReadableShareE2eeVo(share)
        );
    }

    private String resolveSavedShareStatus(DriveShareLink share, DriveItem item) {
        if (share == null || item == null) {
            return SAVED_SHARE_STATUS_UNAVAILABLE;
        }
        if (!SHARE_STATUS_ACTIVE.equals(share.getStatus())) {
            return SAVED_SHARE_STATUS_REVOKED;
        }
        if (share.getExpiresAt() != null && !share.getExpiresAt().isAfter(LocalDateTime.now())) {
            return SAVED_SHARE_STATUS_EXPIRED;
        }
        return SAVED_SHARE_STATUS_ACTIVE;
    }

    private DriveItemVo toItemVo(DriveItem item, int shareCount) {
        return new DriveItemVo(
                String.valueOf(item.getId()),
                item.getParentId() == null ? null : String.valueOf(item.getParentId()),
                item.getItemType(),
                item.getName(),
                item.getMimeType(),
                item.getSizeBytes() == null ? 0L : item.getSizeBytes(),
                shareCount,
                driveFileE2eeService.toVo(item.getE2eeEnabled(), item.getE2eeAlgorithm(), item.getE2eeFingerprintsJson()),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private DriveFileVersionVo toFileVersionVo(DriveFileVersion version) {
        return new DriveFileVersionVo(
                String.valueOf(version.getId()),
                String.valueOf(version.getItemId()),
                version.getVersionNo() == null ? 0 : version.getVersionNo(),
                version.getMimeType(),
                version.getSizeBytes() == null ? 0L : version.getSizeBytes(),
                version.getChecksum(),
                driveFileE2eeService.toVo(version.getE2eeEnabled(), version.getE2eeAlgorithm(), version.getE2eeFingerprintsJson()),
                version.getCreatedAt()
        );
    }

    private DriveTrashItemVo toTrashItemVo(DriveItem item) {
        return new DriveTrashItemVo(
                String.valueOf(item.getId()),
                item.getParentId() == null ? null : String.valueOf(item.getParentId()),
                item.getItemType(),
                item.getName(),
                item.getMimeType(),
                item.getSizeBytes() == null ? 0L : item.getSizeBytes(),
                item.getTrashedAt(),
                item.getPurgeAfterAt(),
                item.getUpdatedAt()
        );
    }

    private DriveShareLinkVo toShareVo(DriveShareLink link) {
        return new DriveShareLinkVo(
                String.valueOf(link.getId()),
                String.valueOf(link.getItemId()),
                link.getToken(),
                link.getPermission(),
                link.getExpiresAt(),
                link.getStatus(),
                StringUtils.hasText(link.getPasswordHash()),
                toReadableShareE2eeVo(link),
                link.getCreatedAt(),
                link.getUpdatedAt()
        );
    }

    private DriveFileDownloadVo toFileDownloadVo(DriveItem item, byte[] content) {
        return new DriveFileDownloadVo(
                item.getName(),
                StringUtils.hasText(item.getMimeType()) ? item.getMimeType() : DEFAULT_BINARY_MIME,
                content
        );
    }

    private DriveFilePreviewVo toFilePreviewVo(DriveItem item, byte[] content, boolean truncated) {
        return new DriveFilePreviewVo(
                item.getName(),
                StringUtils.hasText(item.getMimeType()) ? item.getMimeType() : DEFAULT_BINARY_MIME,
                content,
                truncated
        );
    }

    private DrivePublicShareMetadataVo toPublicShareMetadataVo(DriveShareLink share, DriveItem item) {
        return new DrivePublicShareMetadataVo(
                String.valueOf(share.getId()),
                share.getToken(),
                String.valueOf(item.getId()),
                item.getItemType(),
                item.getName(),
                item.getMimeType(),
                item.getSizeBytes() == null ? 0L : item.getSizeBytes(),
                share.getPermission(),
                share.getStatus(),
                share.getExpiresAt(),
                StringUtils.hasText(share.getPasswordHash()),
                toReadableShareE2eeVo(share)
        );
    }

    private com.mmmail.server.model.vo.DriveShareReadableE2eeVo toReadableShareE2eeVo(DriveShareLink share) {
        if (share == null) {
            return null;
        }
        return driveReadableShareE2eeService.toVo(
                share.getReadableE2eeEnabled(),
                share.getReadableE2eeAlgorithm()
        );
    }

    private DriveShareAccessLogVo toShareAccessLogVo(DriveShareAccessLog log) {
        return new DriveShareAccessLogVo(
                String.valueOf(log.getId()),
                log.getShareId() == null ? null : String.valueOf(log.getShareId()),
                log.getItemId() == null ? null : String.valueOf(log.getItemId()),
                log.getAction(),
                log.getAccessStatus(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getCreatedAt()
        );
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }

    private String nullableId(Long value) {
        return value == null ? "ROOT" : String.valueOf(value);
    }

    private boolean sameParent(Long currentParentId, Long targetParentId) {
        if (currentParentId == null && targetParentId == null) {
            return true;
        }
        if (currentParentId == null || targetParentId == null) {
            return false;
        }
        return currentParentId.equals(targetParentId);
    }

    private record PreparedUpload(
            String fileName,
            String mimeType,
            long sizeBytes,
            String storagePath,
            String checksum,
            DriveFileE2eeService.DriveFileE2eeMetadata e2eeMetadata
    ) {
    }

    private record ShareAndItem(DriveShareLink share, DriveItem item) {
    }

    private record PreviewContent(byte[] content, boolean truncated) {
    }

    private record VersionRetentionConfig(int retentionCount, int retentionDays) {
    }

    @FunctionalInterface
    private interface BatchItemHandler {
        void apply(Long itemId);
    }

    private static final class PublicRateCounter {
        private long windowStartSeconds;
        private long lastSeenSeconds;
        private final AtomicInteger count;

        private PublicRateCounter(long nowSeconds) {
            this.windowStartSeconds = nowSeconds;
            this.lastSeenSeconds = nowSeconds;
            this.count = new AtomicInteger(0);
        }

        private void reset(long nowSeconds) {
            this.windowStartSeconds = nowSeconds;
            this.lastSeenSeconds = nowSeconds;
            this.count.set(0);
        }

        private long windowStartSeconds() {
            return windowStartSeconds;
        }

        private long lastSeenSeconds() {
            return lastSeenSeconds;
        }

        private void setLastSeenSeconds(long nowSeconds) {
            this.lastSeenSeconds = nowSeconds;
        }

        private AtomicInteger count() {
            return count;
        }
    }
}
