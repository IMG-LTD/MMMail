package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.DriveItemMapper;
import com.mmmail.server.mapper.DriveShareLinkMapper;
import com.mmmail.server.model.dto.CreateDriveFileRequest;
import com.mmmail.server.model.dto.CreateDriveShareRequest;
import com.mmmail.server.model.dto.MoveDriveItemRequest;
import com.mmmail.server.model.dto.RenameDriveItemRequest;
import com.mmmail.server.model.dto.V21DriveFileUpdateRequest;
import com.mmmail.server.model.dto.V21DriveUploadRequest;
import com.mmmail.server.model.entity.DriveItem;
import com.mmmail.server.model.entity.DriveShareLink;
import com.mmmail.server.model.vo.DriveItemVo;
import com.mmmail.server.model.vo.DriveShareLinkVo;
import com.mmmail.server.model.vo.DriveUsageVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class V21DriveRuntimeBridgeService {

    private static final String ITEM_TYPE_FILE = "FILE";
    private static final String ITEM_TYPE_FOLDER = "FOLDER";
    private static final String SHARE_STATUS_ACTIVE = "ACTIVE";
    private static final String DEFAULT_BINARY_MIME = "application/octet-stream";
    private static final int DELETED_FLAG_ACTIVE = 0;
    private static final int MAX_LIST_LIMIT = 100;
    private static final long EMPTY_SIZE_BYTES = 0L;

    private final DriveService driveService;
    private final DriveItemMapper driveItemMapper;
    private final DriveShareLinkMapper driveShareLinkMapper;
    private final DriveFileE2eeService driveFileE2eeService;

    public V21DriveRuntimeBridgeService(
            DriveService driveService,
            DriveItemMapper driveItemMapper,
            DriveShareLinkMapper driveShareLinkMapper,
            DriveFileE2eeService driveFileE2eeService
    ) {
        this.driveService = driveService;
        this.driveItemMapper = driveItemMapper;
        this.driveShareLinkMapper = driveShareLinkMapper;
        this.driveFileE2eeService = driveFileE2eeService;
    }

    public List<DriveItemVo> listFiles(Long userId, Long parentId, String keyword, Integer limit) {
        return driveService.listItems(userId, parentId, keyword, ITEM_TYPE_FILE, normalizeListLimit(limit));
    }

    public List<DriveItemVo> listFolders(Long userId, Long parentId, String keyword, Integer limit) {
        return driveService.listItems(userId, parentId, keyword, ITEM_TYPE_FOLDER, normalizeListLimit(limit));
    }

    public DriveUsageVo usage(Long userId, String ipAddress) {
        return driveService.usage(userId, ipAddress);
    }

    public DriveItemVo createUpload(Long userId, V21DriveUploadRequest request, String ipAddress) {
        return driveService.createFile(userId, toCreateFileRequest(request), ipAddress);
    }

    public DriveItemVo readUpload(Long userId, Long itemId) {
        DriveItem item = loadOwnedItem(userId, itemId);
        return toItemVo(item, activeShareCount(itemId));
    }

    public DriveItemVo updateFile(Long userId, Long itemId, V21DriveFileUpdateRequest request, String ipAddress) {
        if (request == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file update payload is required");
        }
        boolean rename = request.hasName();
        boolean move = request.hasParentId();
        if (!rename && !move) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file update requires name or parentId");
        }
        DriveItemVo updated = null;
        if (rename) {
            updated = driveService.renameItem(userId, itemId, renameRequest(request), ipAddress);
        }
        if (move) {
            updated = driveService.moveItem(userId, itemId, moveRequest(request), ipAddress);
        }
        return updated;
    }

    public void deleteFile(Long userId, Long itemId, String ipAddress) {
        driveService.deleteItem(userId, itemId, ipAddress);
    }

    public List<DriveShareLinkVo> listShares(Long userId, Long itemId) {
        return driveService.listShares(userId, itemId);
    }

    public DriveShareLinkVo createShare(Long userId, Long itemId, CreateDriveShareRequest request, String ipAddress) {
        return driveService.createShare(userId, itemId, request, ipAddress);
    }

    private static CreateDriveFileRequest toCreateFileRequest(V21DriveUploadRequest request) {
        return new CreateDriveFileRequest(
                request.fileName(),
                request.parentId(),
                DEFAULT_BINARY_MIME,
                request.sizeBytes(),
                null,
                null
        );
    }

    private static Integer normalizeListLimit(Integer limit) {
        if (limit == null) {
            return null;
        }
        if (limit < 1 || limit > MAX_LIST_LIMIT) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive list limit must be between 1 and " + MAX_LIST_LIMIT);
        }
        return limit;
    }

    private DriveItem loadOwnedItem(Long userId, Long itemId) {
        DriveItem item = driveItemMapper.selectOne(new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getId, itemId)
                .eq(DriveItem::getOwnerId, userId)
                .eq(DriveItem::getDeleted, DELETED_FLAG_ACTIVE));
        if (item == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive item is not found");
        }
        return item;
    }

    private int activeShareCount(Long itemId) {
        Long count = driveShareLinkMapper.selectCount(new LambdaQueryWrapper<DriveShareLink>()
                .eq(DriveShareLink::getItemId, itemId)
                .eq(DriveShareLink::getStatus, SHARE_STATUS_ACTIVE));
        return Math.toIntExact(count);
    }

    private DriveItemVo toItemVo(DriveItem item, int shareCount) {
        return new DriveItemVo(
                String.valueOf(item.getId()),
                item.getParentId() == null ? null : String.valueOf(item.getParentId()),
                item.getItemType(),
                item.getName(),
                item.getMimeType(),
                item.getSizeBytes() == null ? EMPTY_SIZE_BYTES : item.getSizeBytes(),
                shareCount,
                driveFileE2eeService.toVo(item.getE2eeEnabled(), item.getE2eeAlgorithm(), item.getE2eeFingerprintsJson()),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private static RenameDriveItemRequest renameRequest(V21DriveFileUpdateRequest request) {
        return new RenameDriveItemRequest(text(request.getName(), "name"));
    }

    private static MoveDriveItemRequest moveRequest(V21DriveFileUpdateRequest request) {
        return new MoveDriveItemRequest(request.getParentId());
    }

    private static String text(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Drive file " + fieldName + " is required");
        }
        return value.trim();
    }
}
