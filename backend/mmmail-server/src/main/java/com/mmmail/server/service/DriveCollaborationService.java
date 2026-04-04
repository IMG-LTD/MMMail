package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.DriveCollaboratorShareMapper;
import com.mmmail.server.mapper.DriveItemMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.dto.CreateDriveCollaboratorShareRequest;
import com.mmmail.server.model.dto.CreateDriveFolderRequest;
import com.mmmail.server.model.dto.RespondDriveCollaboratorShareRequest;
import com.mmmail.server.model.dto.UpdateDriveCollaboratorShareRequest;
import com.mmmail.server.model.entity.DriveCollaboratorShare;
import com.mmmail.server.model.entity.DriveItem;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.DriveCollaboratorShareVo;
import com.mmmail.server.model.vo.DriveCollaboratorSharedItemVo;
import com.mmmail.server.model.vo.DriveFileDownloadVo;
import com.mmmail.server.model.vo.DriveFilePreviewVo;
import com.mmmail.server.model.vo.DriveIncomingCollaboratorShareVo;
import com.mmmail.server.model.vo.DriveItemVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DriveCollaborationService {

    private static final String ITEM_TYPE_FOLDER = "FOLDER";
    private static final String ITEM_TYPE_FILE = "FILE";
    private static final String PERMISSION_VIEW = "VIEW";
    private static final String PERMISSION_EDIT = "EDIT";
    private static final String STATUS_NEEDS_ACTION = "NEEDS_ACTION";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_DECLINED = "DECLINED";
    private static final String STATUS_REVOKED = "REVOKED";
    private static final String DEFAULT_BINARY_MIME = "application/octet-stream";
    private static final String DRIVE_E2EE_PREVIEW_UNAVAILABLE = "Drive E2EE files must be decrypted locally before preview";
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_PREVIEW_TEXT_MAX_BYTES = 262_144;

    private final DriveCollaboratorShareMapper driveCollaboratorShareMapper;
    private final DriveItemMapper driveItemMapper;
    private final UserAccountMapper userAccountMapper;
    private final SuiteService suiteService;
    private final AuditService auditService;
    private final SuiteCollaborationService suiteCollaborationService;
    private final DriveFileE2eeService driveFileE2eeService;

    @Value("${mmmail.drive.storage-root:${java.io.tmpdir}/mmmail-drive}")
    private String driveStorageRoot;

    @Value("${mmmail.drive.preview-text-max-bytes:262144}")
    private Integer previewTextMaxBytes;
    @Value("${mmmail.drive.upload.max-file-size-bytes:52428800}")
    private Long uploadMaxFileSizeBytes;

    public DriveCollaborationService(
            DriveCollaboratorShareMapper driveCollaboratorShareMapper,
            DriveItemMapper driveItemMapper,
            UserAccountMapper userAccountMapper,
            SuiteService suiteService,
            AuditService auditService,
            SuiteCollaborationService suiteCollaborationService,
            DriveFileE2eeService driveFileE2eeService
    ) {
        this.driveCollaboratorShareMapper = driveCollaboratorShareMapper;
        this.driveItemMapper = driveItemMapper;
        this.userAccountMapper = userAccountMapper;
        this.suiteService = suiteService;
        this.auditService = auditService;
        this.suiteCollaborationService = suiteCollaborationService;
        this.driveFileE2eeService = driveFileE2eeService;
    }

    public List<DriveCollaboratorShareVo> listShares(Long userId, Long itemId) {
        DriveItem item = requireOwnedItem(userId, itemId);
        List<DriveCollaboratorShare> shares = driveCollaboratorShareMapper.selectList(new LambdaQueryWrapper<DriveCollaboratorShare>()
                .eq(DriveCollaboratorShare::getOwnerId, userId)
                .eq(DriveCollaboratorShare::getItemId, itemId)
                .orderByDesc(DriveCollaboratorShare::getUpdatedAt)
                .orderByAsc(DriveCollaboratorShare::getCollaboratorUserId));
        return toShareVos(shares, loadUserMapByIds(extractCollaboratorIds(shares)), item);
    }

    @Transactional
    public DriveCollaboratorShareVo createShare(
            Long userId,
            Long itemId,
            CreateDriveCollaboratorShareRequest request,
            String ipAddress
    ) {
        DriveItem item = requireOwnedItem(userId, itemId);
        UserAccount collaborator = loadCollaborator(request.targetEmail());
        ensureNotSelf(userId, collaborator.getId());
        driveCollaboratorShareMapper.purgeByItemAndCollaborator(itemId, collaborator.getId());
        DriveCollaboratorShare share = insertShare(item, collaborator.getId(), normalizePermission(request.permission()));
        publishEvent(new EventPayload(
                userId,
                collaborator.getId(),
                "DRIVE_COLLABORATOR_SHARE_ADD",
                item,
                ipAddress,
                "shareId=" + share.getId() + ",collaboratorEmail=" + collaborator.getEmail()
                        + ",permission=" + share.getPermission() + ",responseStatus=" + share.getResponseStatus()
        ));
        return toShareVo(share, collaborator);
    }

    @Transactional
    public DriveCollaboratorShareVo updateShare(
            Long userId,
            Long itemId,
            Long shareId,
            UpdateDriveCollaboratorShareRequest request,
            String ipAddress
    ) {
        DriveItem item = requireOwnedItem(userId, itemId);
        DriveCollaboratorShare share = loadOwnerShare(userId, itemId, shareId);
        assertShareManageable(share);
        share.setPermission(normalizePermission(request.permission()));
        share.setUpdatedAt(LocalDateTime.now());
        driveCollaboratorShareMapper.updateById(share);
        UserAccount collaborator = loadRequiredUser(share.getCollaboratorUserId());
        publishEvent(new EventPayload(
                userId,
                collaborator.getId(),
                "DRIVE_COLLABORATOR_SHARE_PERMISSION_UPDATE",
                item,
                ipAddress,
                "shareId=" + share.getId() + ",collaboratorEmail=" + collaborator.getEmail()
                        + ",permission=" + share.getPermission() + ",responseStatus=" + share.getResponseStatus()
        ));
        return toShareVo(share, collaborator);
    }

    @Transactional
    public void removeShare(Long userId, Long itemId, Long shareId, String ipAddress) {
        DriveItem item = requireOwnedItem(userId, itemId);
        DriveCollaboratorShare share = loadOwnerShare(userId, itemId, shareId);
        if (STATUS_REVOKED.equals(share.getResponseStatus())) {
            return;
        }
        share.setResponseStatus(STATUS_REVOKED);
        share.setUpdatedAt(LocalDateTime.now());
        driveCollaboratorShareMapper.updateById(share);
        UserAccount collaborator = loadRequiredUser(share.getCollaboratorUserId());
        publishEvent(new EventPayload(
                userId,
                collaborator.getId(),
                "DRIVE_COLLABORATOR_SHARE_REVOKE",
                item,
                ipAddress,
                "shareId=" + share.getId() + ",collaboratorEmail=" + collaborator.getEmail()
                        + ",permission=" + share.getPermission() + ",responseStatus=" + share.getResponseStatus()
        ));
    }

    public List<DriveIncomingCollaboratorShareVo> listIncomingShares(Long userId, String ipAddress) {
        List<DriveCollaboratorShare> shares = driveCollaboratorShareMapper.selectList(new LambdaQueryWrapper<DriveCollaboratorShare>()
                .eq(DriveCollaboratorShare::getCollaboratorUserId, userId)
                .orderByDesc(DriveCollaboratorShare::getUpdatedAt));
        List<DriveIncomingCollaboratorShareVo> result = toIncomingShareVos(shares);
        auditService.record(userId, "DRIVE_COLLABORATOR_INCOMING_QUERY", "count=" + result.size(), ipAddress);
        return result;
    }

    @Transactional
    public DriveIncomingCollaboratorShareVo respondShare(
            Long userId,
            Long shareId,
            RespondDriveCollaboratorShareRequest request,
            String ipAddress
    ) {
        DriveCollaboratorShare share = loadCollaboratorShare(userId, shareId);
        assertShareRespondable(share);
        share.setResponseStatus(normalizeResponse(request.response()));
        share.setUpdatedAt(LocalDateTime.now());
        driveCollaboratorShareMapper.updateById(share);
        DriveItem item = loadSharedItemOrNull(share.getOwnerId(), share.getItemId());
        UserAccount owner = loadRequiredUser(share.getOwnerId());
        UserAccount collaborator = loadRequiredUser(userId);
        publishEvent(new EventPayload(
                userId,
                owner.getId(),
                STATUS_ACCEPTED.equals(share.getResponseStatus())
                        ? "DRIVE_COLLABORATOR_SHARE_ACCEPT"
                        : "DRIVE_COLLABORATOR_SHARE_DECLINE",
                item == null ? placeholderItem(share) : item,
                ipAddress,
                "shareId=" + share.getId() + ",ownerEmail=" + owner.getEmail()
                        + ",collaboratorEmail=" + collaborator.getEmail()
                        + ",permission=" + share.getPermission() + ",responseStatus=" + share.getResponseStatus()
        ));
        return toIncomingShareVo(share, item, owner);
    }

    public List<DriveCollaboratorSharedItemVo> listSharedWithMe(Long userId, String ipAddress) {
        List<DriveCollaboratorShare> shares = driveCollaboratorShareMapper.selectList(new LambdaQueryWrapper<DriveCollaboratorShare>()
                .eq(DriveCollaboratorShare::getCollaboratorUserId, userId)
                .in(DriveCollaboratorShare::getResponseStatus, List.of(STATUS_ACCEPTED, STATUS_REVOKED))
                .orderByDesc(DriveCollaboratorShare::getUpdatedAt));
        List<DriveCollaboratorSharedItemVo> result = toSharedItemVos(shares);
        auditService.record(userId, "DRIVE_COLLABORATOR_SHARED_QUERY", "count=" + result.size(), ipAddress);
        return result;
    }

    public List<DriveItemVo> listSharedItems(
            Long userId,
            Long shareId,
            Long parentId,
            String keyword,
            String itemType,
            Integer limit
    ) {
        WorkspaceContext context = requireAccessibleWorkspace(userId, shareId);
        int safeLimit = normalizeLimit(limit);
        String normalizedType = normalizeItemTypeOrNull(itemType);
        String normalizedKeyword = normalizeKeyword(keyword);
        if (ITEM_TYPE_FILE.equals(context.rootItem().getItemType())) {
            return filterSingleSharedFile(context.rootItem(), parentId, normalizedKeyword, normalizedType);
        }
        DriveItem folder = resolveSharedFolder(context, parentId);
        return listFolderItems(context.ownerId(), folder.getId(), normalizedKeyword, normalizedType, safeLimit);
    }

    @Transactional
    public DriveItemVo createFolder(
            Long userId,
            Long shareId,
            CreateDriveFolderRequest request,
            String ipAddress
    ) {
        WorkspaceContext context = requireEditableWorkspace(userId, shareId);
        DriveItem parentFolder = resolveSharedFolder(context, request.parentId());
        String name = normalizeName(request.name());
        ensureNameUnique(context.ownerId(), parentFolder.getId(), ITEM_TYPE_FOLDER, name, null);
        DriveItem item = insertFolder(context.ownerId(), parentFolder.getId(), name);
        publishEvent(new EventPayload(
                userId,
                context.ownerId(),
                "DRIVE_COLLABORATOR_FOLDER_CREATE",
                item,
                ipAddress,
                "shareId=" + shareId + ",parentId=" + parentFolder.getId() + ",permission=" + context.permission()
        ));
        return toItemVo(item);
    }

    @Transactional
    public DriveItemVo uploadFile(
            Long userId,
            Long shareId,
            Long parentId,
            MultipartFile file,
            String ipAddress
    ) {
        WorkspaceContext context = requireEditableWorkspace(userId, shareId);
        DriveItem parentFolder = resolveSharedFolder(context, parentId);
        UploadPayload payload = prepareUpload(context.ownerId(), parentFolder.getId(), file, ipAddress);
        DriveItem item = insertFile(payload);
        publishEvent(new EventPayload(
                userId,
                context.ownerId(),
                "DRIVE_COLLABORATOR_FILE_UPLOAD",
                item,
                ipAddress,
                "shareId=" + shareId + ",parentId=" + parentFolder.getId() + ",permission=" + context.permission()
                        + ",sizeBytes=" + item.getSizeBytes()
        ));
        return toItemVo(item);
    }

    public DriveFileDownloadVo downloadFile(Long userId, Long shareId, Long itemId, String ipAddress) {
        WorkspaceContext context = requireAccessibleWorkspace(userId, shareId);
        DriveItem file = resolveSharedFile(context, itemId);
        byte[] content = readStoredFile(file);
        auditService.record(userId, "DRIVE_COLLABORATOR_FILE_DOWNLOAD",
                "shareId=" + shareId + ",itemId=" + file.getId() + ",sizeBytes=" + content.length, ipAddress);
        return new DriveFileDownloadVo(file.getName(), normalizeMimeType(file.getMimeType()), content);
    }

    public DriveFilePreviewVo previewFile(Long userId, Long shareId, Long itemId, String ipAddress) {
        WorkspaceContext context = requireAccessibleWorkspace(userId, shareId);
        DriveItem file = resolveSharedFile(context, itemId);
        PreviewPayload preview = readPreview(file);
        auditService.record(userId, "DRIVE_COLLABORATOR_FILE_PREVIEW",
                "shareId=" + shareId + ",itemId=" + file.getId() + ",sizeBytes=" + preview.content().length, ipAddress);
        return new DriveFilePreviewVo(file.getName(), normalizeMimeType(file.getMimeType()), preview.content(), preview.truncated());
    }

    private List<DriveCollaboratorShareVo> toShareVos(
            List<DriveCollaboratorShare> shares,
            Map<Long, UserAccount> userMap,
            DriveItem ignored
    ) {
        List<DriveCollaboratorShareVo> result = new ArrayList<>(shares.size());
        for (DriveCollaboratorShare share : shares) {
            result.add(toShareVo(share, userMap.get(share.getCollaboratorUserId())));
        }
        return result;
    }

    private List<DriveIncomingCollaboratorShareVo> toIncomingShareVos(List<DriveCollaboratorShare> shares) {
        Map<Long, DriveItem> itemMap = loadItemMap(shares);
        Map<Long, UserAccount> ownerMap = loadUserMapByIds(extractOwnerIds(shares));
        List<DriveIncomingCollaboratorShareVo> result = new ArrayList<>(shares.size());
        for (DriveCollaboratorShare share : shares) {
            result.add(toIncomingShareVo(share, itemMap.get(share.getItemId()), ownerMap.get(share.getOwnerId())));
        }
        return result;
    }

    private List<DriveCollaboratorSharedItemVo> toSharedItemVos(List<DriveCollaboratorShare> shares) {
        Map<Long, DriveItem> itemMap = loadItemMap(shares);
        Map<Long, UserAccount> ownerMap = loadUserMapByIds(extractOwnerIds(shares));
        List<DriveCollaboratorSharedItemVo> result = new ArrayList<>(shares.size());
        for (DriveCollaboratorShare share : shares) {
            result.add(toSharedItemVo(share, itemMap.get(share.getItemId()), ownerMap.get(share.getOwnerId())));
        }
        return result;
    }

    private DriveCollaboratorShareVo toShareVo(DriveCollaboratorShare share, UserAccount collaborator) {
        return new DriveCollaboratorShareVo(
                String.valueOf(share.getId()),
                String.valueOf(share.getCollaboratorUserId()),
                collaborator == null ? "" : collaborator.getEmail(),
                collaborator == null ? "" : collaborator.getDisplayName(),
                share.getPermission(),
                share.getResponseStatus(),
                share.getCreatedAt(),
                share.getUpdatedAt()
        );
    }

    private DriveIncomingCollaboratorShareVo toIncomingShareVo(
            DriveCollaboratorShare share,
            DriveItem item,
            UserAccount owner
    ) {
        return new DriveIncomingCollaboratorShareVo(
                String.valueOf(share.getId()),
                String.valueOf(share.getItemId()),
                item == null ? ITEM_TYPE_FILE : item.getItemType(),
                item == null ? "(item unavailable)" : item.getName(),
                owner == null ? "" : owner.getEmail(),
                owner == null ? "" : owner.getDisplayName(),
                share.getPermission(),
                share.getResponseStatus(),
                share.getUpdatedAt()
        );
    }

    private DriveCollaboratorSharedItemVo toSharedItemVo(
            DriveCollaboratorShare share,
            DriveItem item,
            UserAccount owner
    ) {
        boolean available = STATUS_ACCEPTED.equals(share.getResponseStatus()) && item != null;
        return new DriveCollaboratorSharedItemVo(
                String.valueOf(share.getId()),
                String.valueOf(share.getItemId()),
                item == null ? ITEM_TYPE_FILE : item.getItemType(),
                item == null ? "(item unavailable)" : item.getName(),
                owner == null ? "" : owner.getEmail(),
                owner == null ? "" : owner.getDisplayName(),
                share.getPermission(),
                share.getResponseStatus(),
                share.getUpdatedAt(),
                available
        );
    }

    private WorkspaceContext requireAccessibleWorkspace(Long userId, Long shareId) {
        DriveCollaboratorShare share = loadCollaboratorShare(userId, shareId);
        if (!STATUS_ACCEPTED.equals(share.getResponseStatus())) {
            throw new BizException(ErrorCode.DRIVE_COLLABORATOR_SHARE_NOT_FOUND, "Drive collaborator share is unavailable");
        }
        DriveItem rootItem = loadSharedItemOrNull(share.getOwnerId(), share.getItemId());
        if (rootItem == null) {
            throw new BizException(ErrorCode.DRIVE_COLLABORATOR_SHARE_NOT_FOUND, "Shared drive item is unavailable");
        }
        return new WorkspaceContext(share, rootItem);
    }

    private WorkspaceContext requireEditableWorkspace(Long userId, Long shareId) {
        WorkspaceContext context = requireAccessibleWorkspace(userId, shareId);
        if (!PERMISSION_EDIT.equals(context.permission())) {
            throw new BizException(ErrorCode.FORBIDDEN, "Read-only collaborator cannot modify shared drive items");
        }
        if (!ITEM_TYPE_FOLDER.equals(context.rootItem().getItemType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared file does not accept nested writes");
        }
        return context;
    }

    private DriveItem resolveSharedFolder(WorkspaceContext context, Long parentId) {
        DriveItem rootFolder = requireFolder(context.rootItem(), "Shared folder is unavailable");
        if (parentId == null || rootFolder.getId().equals(parentId)) {
            return rootFolder;
        }
        DriveItem folder = loadOwnerItem(context.ownerId(), parentId, "Shared folder is unavailable");
        requireFolder(folder, "Shared folder is unavailable");
        assertWithinRoot(rootFolder, folder, "Shared folder is unavailable");
        return folder;
    }

    private DriveItem resolveSharedFile(WorkspaceContext context, Long itemId) {
        if (ITEM_TYPE_FILE.equals(context.rootItem().getItemType())) {
            if (itemId != null && !context.rootItem().getId().equals(itemId)) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Shared file is unavailable");
            }
            return context.rootItem();
        }
        DriveItem file = loadOwnerItem(context.ownerId(), itemId, "Shared file is unavailable");
        requireFile(file, "Shared file is unavailable");
        assertWithinRoot(context.rootItem(), file, "Shared file is unavailable");
        return file;
    }

    private List<DriveItemVo> filterSingleSharedFile(
            DriveItem file,
            Long parentId,
            String keyword,
            String itemType
    ) {
        if (parentId != null && !file.getId().equals(parentId)) {
            return List.of();
        }
        if (itemType != null && !ITEM_TYPE_FILE.equals(itemType)) {
            return List.of();
        }
        if (StringUtils.hasText(keyword) && !file.getName().toLowerCase().contains(keyword.toLowerCase())) {
            return List.of();
        }
        return List.of(toItemVo(file));
    }

    private List<DriveItemVo> listFolderItems(
            Long ownerId,
            Long parentId,
            String keyword,
            String itemType,
            int limit
    ) {
        LambdaQueryWrapper<DriveItem> query = new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getOwnerId, ownerId)
                .eq(DriveItem::getParentId, parentId)
                .orderByDesc(DriveItem::getItemType)
                .orderByAsc(DriveItem::getName)
                .orderByDesc(DriveItem::getUpdatedAt)
                .last("limit " + limit);
        if (itemType != null) {
            query.eq(DriveItem::getItemType, itemType);
        }
        if (StringUtils.hasText(keyword)) {
            query.like(DriveItem::getName, keyword);
        }
        return driveItemMapper.selectList(query).stream().map(this::toItemVo).toList();
    }

    private DriveCollaboratorShare insertShare(DriveItem item, Long collaboratorId, String permission) {
        DriveCollaboratorShare share = new DriveCollaboratorShare();
        LocalDateTime now = LocalDateTime.now();
        share.setItemId(item.getId());
        share.setOwnerId(item.getOwnerId());
        share.setCollaboratorUserId(collaboratorId);
        share.setPermission(permission);
        share.setResponseStatus(STATUS_NEEDS_ACTION);
        share.setCreatedAt(now);
        share.setUpdatedAt(now);
        share.setDeleted(0);
        driveCollaboratorShareMapper.insert(share);
        return share;
    }

    private DriveItem insertFolder(Long ownerId, Long parentId, String name) {
        DriveItem item = new DriveItem();
        LocalDateTime now = LocalDateTime.now();
        item.setOwnerId(ownerId);
        item.setParentId(parentId);
        item.setItemType(ITEM_TYPE_FOLDER);
        item.setName(name);
        item.setMimeType(null);
        item.setSizeBytes(0L);
        item.setStoragePath(null);
        item.setChecksum(null);
        item.setE2eeEnabled(DriveFileE2eeService.E2EE_DISABLED_FLAG);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setDeleted(0);
        driveItemMapper.insert(item);
        return item;
    }

    private DriveItem insertFile(UploadPayload payload) {
        DriveItem item = new DriveItem();
        LocalDateTime now = LocalDateTime.now();
        item.setOwnerId(payload.ownerId());
        item.setParentId(payload.parentId());
        item.setItemType(ITEM_TYPE_FILE);
        item.setName(payload.fileName());
        item.setMimeType(payload.mimeType());
        item.setSizeBytes(payload.sizeBytes());
        item.setStoragePath(payload.storagePath());
        item.setChecksum(payload.checksum());
        item.setE2eeEnabled(DriveFileE2eeService.E2EE_DISABLED_FLAG);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setDeleted(0);
        driveItemMapper.insert(item);
        return item;
    }

    private UploadPayload prepareUpload(Long ownerId, Long parentId, MultipartFile file, String ipAddress) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "file is required");
        }
        String fileName = normalizeName(resolveUploadName(file));
        ensureNameUnique(ownerId, parentId, ITEM_TYPE_FILE, fileName, null);
        byte[] content = readUploadBytes(file);
        long sizeBytes = content.length;
        if (sizeBytes <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "file size must be greater than 0");
        }
        assertUploadSizeWithinLimit(sizeBytes);
        suiteService.assertDriveStorageQuota(ownerId, sizeBytes, ipAddress);
        String storagePath = storeUploadedFile(ownerId, fileName, content);
        return new UploadPayload(
                ownerId,
                parentId,
                fileName,
                normalizeMimeType(file.getContentType()),
                sizeBytes,
                storagePath,
                sha256(content)
        );
    }

    private void assertUploadSizeWithinLimit(long sizeBytes) {
        if (sizeBytes > effectiveUploadMaxFileSizeBytes()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file size exceeded max upload limit");
        }
    }

    private long effectiveUploadMaxFileSizeBytes() {
        if (uploadMaxFileSizeBytes == null || uploadMaxFileSizeBytes <= 0L) {
            return 52_428_800L;
        }
        return uploadMaxFileSizeBytes;
    }

    private void publishEvent(EventPayload payload) {
        AuditEventVo event = auditService.recordEvent(
                payload.actorId(),
                payload.eventType(),
                buildAuditDetail(payload.item(), payload.extraDetail()),
                payload.ipAddress()
        );
        suiteCollaborationService.publishToUser(payload.actorId(), event);
        if (payload.recipientId() != null && !payload.recipientId().equals(payload.actorId())) {
            suiteCollaborationService.publishToUser(payload.recipientId(), event);
        }
    }

    private String buildAuditDetail(DriveItem item, String extraDetail) {
        String detail = "itemId=" + item.getId()
                + ",itemType=" + item.getItemType()
                + ",ownerId=" + item.getOwnerId()
                + ",parentId=" + nullableId(item.getParentId());
        if (!StringUtils.hasText(extraDetail)) {
            return detail;
        }
        return detail + "," + extraDetail.trim();
    }

    private DriveItem requireOwnedItem(Long userId, Long itemId) {
        DriveItem item = driveItemMapper.selectOne(new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getId, itemId)
                .eq(DriveItem::getOwnerId, userId)
                .last("limit 1"));
        if (item == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive item is not found");
        }
        return item;
    }

    private DriveCollaboratorShare loadOwnerShare(Long userId, Long itemId, Long shareId) {
        DriveCollaboratorShare share = driveCollaboratorShareMapper.selectOne(new LambdaQueryWrapper<DriveCollaboratorShare>()
                .eq(DriveCollaboratorShare::getId, shareId)
                .eq(DriveCollaboratorShare::getOwnerId, userId)
                .eq(DriveCollaboratorShare::getItemId, itemId)
                .last("limit 1"));
        if (share == null) {
            throw new BizException(ErrorCode.DRIVE_COLLABORATOR_SHARE_NOT_FOUND);
        }
        return share;
    }

    private DriveCollaboratorShare loadCollaboratorShare(Long userId, Long shareId) {
        DriveCollaboratorShare share = driveCollaboratorShareMapper.selectOne(new LambdaQueryWrapper<DriveCollaboratorShare>()
                .eq(DriveCollaboratorShare::getId, shareId)
                .eq(DriveCollaboratorShare::getCollaboratorUserId, userId)
                .last("limit 1"));
        if (share == null) {
            throw new BizException(ErrorCode.DRIVE_COLLABORATOR_SHARE_NOT_FOUND);
        }
        return share;
    }

    private DriveItem loadSharedItemOrNull(Long ownerId, Long itemId) {
        return driveItemMapper.selectOne(new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getId, itemId)
                .eq(DriveItem::getOwnerId, ownerId)
                .last("limit 1"));
    }

    private DriveItem loadOwnerItem(Long ownerId, Long itemId, String message) {
        if (itemId == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        DriveItem item = loadSharedItemOrNull(ownerId, itemId);
        if (item == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return item;
    }

    private DriveItem placeholderItem(DriveCollaboratorShare share) {
        DriveItem item = new DriveItem();
        item.setId(share.getItemId());
        item.setOwnerId(share.getOwnerId());
        item.setItemType(ITEM_TYPE_FILE);
        item.setName("(item unavailable)");
        item.setParentId(null);
        return item;
    }

    private UserAccount loadCollaborator(String email) {
        String normalizedEmail = normalizeEmail(email);
        UserAccount collaborator = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getEmail, normalizedEmail)
                .last("limit 1"));
        if (collaborator == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "Collaborator email is not registered");
        }
        return collaborator;
    }

    private UserAccount loadRequiredUser(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private Map<Long, UserAccount> loadUserMapByIds(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, UserAccount> result = new HashMap<>();
        for (UserAccount user : userAccountMapper.selectBatchIds(userIds)) {
            result.put(user.getId(), user);
        }
        return result;
    }

    private Map<Long, DriveItem> loadItemMap(List<DriveCollaboratorShare> shares) {
        Set<Long> itemIds = new LinkedHashSet<>();
        for (DriveCollaboratorShare share : shares) {
            itemIds.add(share.getItemId());
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

    private Set<Long> extractCollaboratorIds(List<DriveCollaboratorShare> shares) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (DriveCollaboratorShare share : shares) {
            userIds.add(share.getCollaboratorUserId());
        }
        return userIds;
    }

    private Set<Long> extractOwnerIds(List<DriveCollaboratorShare> shares) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (DriveCollaboratorShare share : shares) {
            userIds.add(share.getOwnerId());
        }
        return userIds;
    }

    private void ensureNameUnique(Long ownerId, Long parentId, String itemType, String name, Long excludeId) {
        LambdaQueryWrapper<DriveItem> query = new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getOwnerId, ownerId)
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

    private void assertWithinRoot(DriveItem rootFolder, DriveItem item, String message) {
        if (rootFolder.getId().equals(item.getId())) {
            return;
        }
        Set<Long> visited = new HashSet<>();
        DriveItem cursor = item;
        while (cursor.getParentId() != null && visited.add(cursor.getId())) {
            DriveItem parent = loadSharedItemOrNull(rootFolder.getOwnerId(), cursor.getParentId());
            if (parent == null) {
                break;
            }
            if (rootFolder.getId().equals(parent.getId())) {
                return;
            }
            cursor = parent;
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
    }

    private void assertShareManageable(DriveCollaboratorShare share) {
        if (STATUS_REVOKED.equals(share.getResponseStatus())) {
            throw new BizException(ErrorCode.DRIVE_COLLABORATOR_SHARE_NOT_FOUND, "Drive collaborator share is revoked");
        }
    }

    private void assertShareRespondable(DriveCollaboratorShare share) {
        if (STATUS_REVOKED.equals(share.getResponseStatus())) {
            throw new BizException(ErrorCode.DRIVE_COLLABORATOR_SHARE_NOT_FOUND, "Drive collaborator share is unavailable");
        }
        if (!STATUS_NEEDS_ACTION.equals(share.getResponseStatus())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive collaborator share response is already recorded");
        }
    }

    private void ensureNotSelf(Long ownerId, Long collaboratorId) {
        if (ownerId.equals(collaboratorId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Cannot share drive item with yourself");
        }
    }

    private DriveItem requireFolder(DriveItem item, String message) {
        if (!ITEM_TYPE_FOLDER.equals(item.getItemType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return item;
    }

    private void requireFile(DriveItem item, String message) {
        if (!ITEM_TYPE_FILE.equals(item.getItemType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private String normalizePermission(String permission) {
        if (!StringUtils.hasText(permission)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "permission is required");
        }
        String normalized = permission.trim().toUpperCase();
        if (!PERMISSION_VIEW.equals(normalized) && !PERMISSION_EDIT.equals(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported collaborator permission");
        }
        return normalized;
    }

    private String normalizeResponse(String response) {
        if (!StringUtils.hasText(response)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "response is required");
        }
        return switch (response.trim().toUpperCase()) {
            case "ACCEPT" -> STATUS_ACCEPTED;
            case "DECLINE" -> STATUS_DECLINED;
            default -> throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported collaborator share response");
        };
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

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : "";
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Email is required");
        }
        return email.trim().toLowerCase();
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Name is required");
        }
        String normalized = name.trim();
        if (normalized.length() > 128) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Name is too long");
        }
        return normalized;
    }

    private String normalizeMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            return DEFAULT_BINARY_MIME;
        }
        return mimeType.trim().toLowerCase();
    }

    private String resolveUploadName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName)) {
            return "upload.bin";
        }
        String fileName = Paths.get(originalName.trim()).getFileName().toString();
        return fileName.replace("\\", "_").replace("/", "_");
    }

    private byte[] readUploadBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to read uploaded file");
        }
    }

    private String storeUploadedFile(Long ownerId, String fileName, byte[] content) {
        Path root = Paths.get(driveStorageRoot).toAbsolutePath().normalize();
        Path ownerDir = root.resolve(String.valueOf(ownerId)).normalize();
        Path target = ownerDir.resolve(UUID.randomUUID().toString().replace("-", "") + "-" + sanitizeStorageName(fileName)).normalize();
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
        String sanitized = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!StringUtils.hasText(sanitized)) {
            return "file.bin";
        }
        return sanitized.length() > 120 ? sanitized.substring(0, 120) : sanitized;
    }

    private byte[] readStoredFile(DriveItem item) {
        String path = item.getStoragePath();
        if (!StringUtils.hasText(path) || !isPathWithinRoot(path)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file content is unavailable");
        }
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file content is unavailable");
        }
    }

    private PreviewPayload readPreview(DriveItem item) {
        if (driveFileE2eeService.isEnabled(item.getE2eeEnabled())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, DRIVE_E2EE_PREVIEW_UNAVAILABLE);
        }
        String mimeType = normalizeMimeType(item.getMimeType());
        if (mimeType.startsWith("text/") || mimeType.contains("json") || mimeType.contains("xml")) {
            return readTextPreview(item.getStoragePath());
        }
        if (mimeType.startsWith("image/") || "application/pdf".equals(mimeType)) {
            return new PreviewPayload(readStoredFile(item), false);
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive preview is unavailable for current file type");
    }

    private PreviewPayload readTextPreview(String storagePath) {
        if (!StringUtils.hasText(storagePath) || !isPathWithinRoot(storagePath)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file content is unavailable");
        }
        int maxBytes = previewTextMaxBytes == null || previewTextMaxBytes <= 0
                ? DEFAULT_PREVIEW_TEXT_MAX_BYTES
                : previewTextMaxBytes;
        try (InputStream inputStream = Files.newInputStream(Paths.get(storagePath))) {
            byte[] raw = inputStream.readNBytes(maxBytes + 1);
            if (raw.length <= maxBytes) {
                return new PreviewPayload(raw, false);
            }
            return new PreviewPayload(Arrays.copyOf(raw, maxBytes), true);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file content is unavailable");
        }
    }

    private boolean isPathWithinRoot(String filePath) {
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

    private DriveItemVo toItemVo(DriveItem item) {
        return new DriveItemVo(
                String.valueOf(item.getId()),
                item.getParentId() == null ? null : String.valueOf(item.getParentId()),
                item.getItemType(),
                item.getName(),
                item.getMimeType(),
                item.getSizeBytes() == null ? 0L : item.getSizeBytes(),
                0,
                driveFileE2eeService.toVo(item.getE2eeEnabled(), item.getE2eeAlgorithm(), item.getE2eeFingerprintsJson()),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private String nullableId(Long id) {
        return id == null ? "-" : String.valueOf(id);
    }

    private record WorkspaceContext(DriveCollaboratorShare share, DriveItem rootItem) {
        private Long ownerId() {
            return share.getOwnerId();
        }

        private String permission() {
            return share.getPermission();
        }
    }

    private record EventPayload(
            Long actorId,
            Long recipientId,
            String eventType,
            DriveItem item,
            String ipAddress,
            String extraDetail
    ) {
    }

    private record UploadPayload(
            Long ownerId,
            Long parentId,
            String fileName,
            String mimeType,
            long sizeBytes,
            String storagePath,
            String checksum
    ) {
    }

    private record PreviewPayload(byte[] content, boolean truncated) {
    }
}
