package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.SheetsWorkbookMapper;
import com.mmmail.server.mapper.SheetsWorkbookVersionMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.entity.SheetsWorkbook;
import com.mmmail.server.model.entity.SheetsWorkbookVersion;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.SheetsWorkbookVersionVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SheetsVersionService {

    public static final String SOURCE_CREATE = "SHEETS_WORKBOOK_CREATE";
    public static final String SOURCE_IMPORT = "SHEETS_WORKBOOK_IMPORT";
    public static final String SOURCE_UPDATE_CELLS = "SHEETS_WORKBOOK_UPDATE_CELLS";
    public static final String SOURCE_RESTORE = "SHEETS_WORKBOOK_VERSION_RESTORE";

    private final SheetsAccessService sheetsAccessService;
    private final SheetsWorkbookMapper sheetsWorkbookMapper;
    private final SheetsWorkbookVersionMapper sheetsWorkbookVersionMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditService auditService;
    private final SuiteCollaborationService suiteCollaborationService;

    public SheetsVersionService(
            SheetsAccessService sheetsAccessService,
            SheetsWorkbookMapper sheetsWorkbookMapper,
            SheetsWorkbookVersionMapper sheetsWorkbookVersionMapper,
            UserAccountMapper userAccountMapper,
            AuditService auditService,
            SuiteCollaborationService suiteCollaborationService
    ) {
        this.sheetsAccessService = sheetsAccessService;
        this.sheetsWorkbookMapper = sheetsWorkbookMapper;
        this.sheetsWorkbookVersionMapper = sheetsWorkbookVersionMapper;
        this.userAccountMapper = userAccountMapper;
        this.auditService = auditService;
        this.suiteCollaborationService = suiteCollaborationService;
    }

    @Transactional
    public void recordSnapshot(SheetsWorkbook workbook, Long actorUserId, String sourceEvent) {
        SheetsWorkbookVersion version = new SheetsWorkbookVersion();
        version.setWorkbookId(workbook.getId());
        version.setVersionNo(workbook.getCurrentVersion());
        version.setTitle(workbook.getTitle());
        version.setRowCount(workbook.getRowCount());
        version.setColCount(workbook.getColCount());
        version.setGridJson(workbook.getGridJson());
        version.setSheetsJson(workbook.getSheetsJson());
        version.setActiveSheetId(workbook.getActiveSheetId());
        version.setCreatedByUserId(actorUserId);
        version.setSourceEvent(sourceEvent);
        version.setCreatedAt(LocalDateTime.now());
        version.setDeleted(0);
        sheetsWorkbookVersionMapper.insert(version);
    }

    public List<SheetsWorkbookVersionVo> listVersions(Long userId, Long workbookId) {
        sheetsAccessService.requireOwned(userId, workbookId);
        List<SheetsWorkbookVersion> versions = sheetsWorkbookVersionMapper.selectList(new LambdaQueryWrapper<SheetsWorkbookVersion>()
                .eq(SheetsWorkbookVersion::getWorkbookId, workbookId)
                .orderByDesc(SheetsWorkbookVersion::getVersionNo)
                .orderByDesc(SheetsWorkbookVersion::getCreatedAt));
        Map<Long, UserAccount> userMap = loadUserMap(versions);
        return versions.stream().map(version -> toVersionVo(version, userMap.get(version.getCreatedByUserId()))).toList();
    }

    @Transactional
    public SheetsWorkbook restoreVersion(Long userId, Long workbookId, Long versionId, String ipAddress) {
        SheetsAccessService.SheetsWorkbookAccessContext context = sheetsAccessService.requireOwned(userId, workbookId);
        SheetsWorkbookVersion version = loadVersion(workbookId, versionId);
        SheetsWorkbook workbook = context.workbook();
        restoreWorkbookState(workbook, version);
        sheetsWorkbookMapper.updateById(workbook);
        recordSnapshot(workbook, userId, SOURCE_RESTORE);
        publishRestoreEvent(userId, workbook, version, ipAddress);
        return workbook;
    }

    @Transactional
    public void purgeByWorkbookId(Long workbookId) {
        sheetsWorkbookVersionMapper.purgeByWorkbookId(workbookId);
    }

    private SheetsWorkbookVersion loadVersion(Long workbookId, Long versionId) {
        SheetsWorkbookVersion version = sheetsWorkbookVersionMapper.selectOne(new LambdaQueryWrapper<SheetsWorkbookVersion>()
                .eq(SheetsWorkbookVersion::getWorkbookId, workbookId)
                .eq(SheetsWorkbookVersion::getId, versionId)
                .last("limit 1"));
        if (version == null) {
            throw new BizException(ErrorCode.SHEETS_WORKBOOK_VERSION_NOT_FOUND);
        }
        return version;
    }

    private void restoreWorkbookState(SheetsWorkbook workbook, SheetsWorkbookVersion version) {
        LocalDateTime now = LocalDateTime.now();
        workbook.setRowCount(version.getRowCount());
        workbook.setColCount(version.getColCount());
        workbook.setGridJson(version.getGridJson());
        workbook.setSheetsJson(version.getSheetsJson());
        workbook.setActiveSheetId(version.getActiveSheetId());
        workbook.setCurrentVersion(workbook.getCurrentVersion() + 1);
        workbook.setUpdatedAt(now);
        workbook.setLastOpenedAt(now);
    }

    private Map<Long, UserAccount> loadUserMap(List<SheetsWorkbookVersion> versions) {
        Set<Long> userIds = versions.stream().map(SheetsWorkbookVersion::getCreatedByUserId).collect(java.util.stream.Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userAccountMapper.selectList(new LambdaQueryWrapper<UserAccount>().in(UserAccount::getId, userIds))
                .stream()
                .collect(java.util.stream.Collectors.toMap(UserAccount::getId, item -> item));
    }

    private SheetsWorkbookVersionVo toVersionVo(SheetsWorkbookVersion version, UserAccount user) {
        return new SheetsWorkbookVersionVo(
                String.valueOf(version.getId()),
                version.getVersionNo(),
                version.getTitle(),
                version.getRowCount(),
                version.getColCount(),
                String.valueOf(version.getCreatedByUserId()),
                user == null ? "" : user.getEmail(),
                user == null ? "" : user.getDisplayName(),
                version.getSourceEvent(),
                version.getCreatedAt()
        );
    }

    private void publishRestoreEvent(
            Long userId,
            SheetsWorkbook workbook,
            SheetsWorkbookVersion restoredVersion,
            String ipAddress
    ) {
        String extraDetail = "restoredFromVersionId=" + restoredVersion.getId()
                + ",restoredFromVersionNo=" + restoredVersion.getVersionNo();
        AuditEventVo event = auditService.recordEvent(
                userId,
                SOURCE_RESTORE,
                buildAuditDetail(workbook, extraDetail),
                ipAddress
        );
        suiteCollaborationService.publishToUser(userId, event);
    }

    private String buildAuditDetail(SheetsWorkbook workbook, String extraDetail) {
        String detail = "workbookId=" + workbook.getId()
                + ",title=" + workbook.getTitle()
                + ",version=" + workbook.getCurrentVersion()
                + ",activeSheetId=" + workbook.getActiveSheetId();
        if (StringUtils.hasText(extraDetail)) {
            return detail + "," + extraDetail.trim();
        }
        return detail;
    }
}
