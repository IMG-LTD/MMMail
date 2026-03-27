package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.DriveItemMapper;
import com.mmmail.server.mapper.OrgMemberMapper;
import com.mmmail.server.mapper.OrgPolicyMapper;
import com.mmmail.server.mapper.OrgTeamSpaceMapper;
import com.mmmail.server.mapper.OrgWorkspaceMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.dto.CreateOrgTeamSpaceFolderRequest;
import com.mmmail.server.model.dto.CreateOrgTeamSpaceRequest;
import com.mmmail.server.model.entity.DriveItem;
import com.mmmail.server.model.entity.OrgMember;
import com.mmmail.server.model.entity.OrgPolicy;
import com.mmmail.server.model.entity.OrgTeamSpace;
import com.mmmail.server.model.entity.OrgWorkspace;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.DriveFileDownloadVo;
import com.mmmail.server.model.vo.OrgBusinessOverviewVo;
import com.mmmail.server.model.vo.OrgTeamSpaceItemVo;
import com.mmmail.server.model.vo.OrgTeamSpaceVo;
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
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class OrgBusinessService {

    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INVITED = "INVITED";
    private static final String ITEM_TYPE_FOLDER = "FOLDER";
    private static final String ITEM_TYPE_FILE = "FILE";
    private static final String DEFAULT_BINARY_MIME = "application/octet-stream";
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_TEAM_SPACE_STORAGE_LIMIT_MB = 10240;
    private static final String POLICY_ALLOWED_EMAIL_DOMAINS = "allowedEmailDomains";
    private static final String POLICY_GOVERNANCE_REVIEW_SLA_HOURS = "governanceReviewSlaHours";
    private static final String POLICY_REQUIRE_DUAL_REVIEW_GOVERNANCE = "requireDualReviewGovernance";

    private final OrgWorkspaceMapper orgWorkspaceMapper;
    private final OrgMemberMapper orgMemberMapper;
    private final OrgPolicyMapper orgPolicyMapper;
    private final OrgTeamSpaceMapper orgTeamSpaceMapper;
    private final DriveItemMapper driveItemMapper;
    private final UserAccountMapper userAccountMapper;
    private final AuditService auditService;
    private final SuiteCollaborationService suiteCollaborationService;
    private final OrgTeamSpaceGovernanceService orgTeamSpaceGovernanceService;

    @Value("${mmmail.drive.storage-root:${java.io.tmpdir}/mmmail-drive}")
    private String driveStorageRoot;

    public OrgBusinessService(
            OrgWorkspaceMapper orgWorkspaceMapper,
            OrgMemberMapper orgMemberMapper,
            OrgPolicyMapper orgPolicyMapper,
            OrgTeamSpaceMapper orgTeamSpaceMapper,
            DriveItemMapper driveItemMapper,
            UserAccountMapper userAccountMapper,
            AuditService auditService,
            SuiteCollaborationService suiteCollaborationService,
            OrgTeamSpaceGovernanceService orgTeamSpaceGovernanceService
    ) {
        this.orgWorkspaceMapper = orgWorkspaceMapper;
        this.orgMemberMapper = orgMemberMapper;
        this.orgPolicyMapper = orgPolicyMapper;
        this.orgTeamSpaceMapper = orgTeamSpaceMapper;
        this.driveItemMapper = driveItemMapper;
        this.userAccountMapper = userAccountMapper;
        this.auditService = auditService;
        this.suiteCollaborationService = suiteCollaborationService;
        this.orgTeamSpaceGovernanceService = orgTeamSpaceGovernanceService;
    }

    public OrgBusinessOverviewVo getBusinessOverview(Long userId, Long orgId, String ipAddress) {
        OrgMember membership = requireActiveMembership(userId, orgId);
        OrgWorkspace org = loadOrg(orgId);
        List<OrgMember> members = loadMembers(orgId);
        List<OrgTeamSpace> teamSpaces = loadTeamSpaces(orgId);
        PolicySnapshot policy = loadPolicy(orgId);
        int memberCount = countByStatus(members, STATUS_ACTIVE);
        int adminCount = countByRole(members, Set.of(ROLE_OWNER, ROLE_ADMIN));
        int pendingInviteCount = countByStatus(members, STATUS_INVITED);
        long storageBytes = teamSpaces.stream().mapToLong(space -> safeLong(driveItemMapper.selectStorageBytesByTeamSpace(space.getId()))).sum();
        long storageLimitBytes = teamSpaces.stream().mapToLong(space -> safeStorageLimitBytes(space.getStorageLimitMb())).sum();
        auditService.record(userId, "ORG_BUSINESS_OVERVIEW_QUERY", "orgId=" + orgId + ",teamSpaces=" + teamSpaces.size(), ipAddress, orgId);
        return new OrgBusinessOverviewVo(
                String.valueOf(orgId),
                org.getName(),
                membership.getRole(),
                memberCount,
                adminCount,
                pendingInviteCount,
                teamSpaces.size(),
                storageBytes,
                storageLimitBytes,
                policy.allowedEmailDomains(),
                policy.governanceReviewSlaHours(),
                policy.requireDualReviewGovernance(),
                LocalDateTime.now()
        );
    }

    public List<OrgTeamSpaceVo> listTeamSpaces(Long userId, Long orgId, String ipAddress) {
        requireActiveMembership(userId, orgId);
        List<OrgTeamSpaceVo> result = loadTeamSpaces(orgId).stream()
                .map(space -> new Object[]{space, orgTeamSpaceGovernanceService.describeAccess(userId, orgId, space.getId())})
                .filter(pair -> ((OrgTeamSpaceGovernanceService.TeamSpaceAccessScope) pair[1]).canRead())
                .map(pair -> toTeamSpaceVo((OrgTeamSpace) pair[0], (OrgTeamSpaceGovernanceService.TeamSpaceAccessScope) pair[1]))
                .toList();
        auditService.record(userId, "ORG_TEAM_SPACE_LIST", "orgId=" + orgId + ",count=" + result.size(), ipAddress, orgId);
        return result;
    }

    @Transactional
    public OrgTeamSpaceVo createTeamSpace(Long userId, Long orgId, CreateOrgTeamSpaceRequest request, String ipAddress) {
        requireManageMembership(userId, orgId);
        String name = normalizeName(request.name());
        String description = normalizeNullableText(request.description(), 256);
        String slug = generateUniqueSlug(orgId, name);
        int storageLimitMb = normalizeStorageLimit(request.storageLimitMb());
        LocalDateTime now = LocalDateTime.now();

        DriveItem root = new DriveItem();
        root.setOwnerId(userId);
        root.setParentId(null);
        root.setTeamSpaceId(null);
        root.setItemType(ITEM_TYPE_FOLDER);
        root.setName("__TEAM_SPACE_ROOT__" + orgId + "__" + slug);
        root.setMimeType(null);
        root.setSizeBytes(0L);
        root.setStoragePath(null);
        root.setChecksum(null);
        root.setCreatedAt(now);
        root.setUpdatedAt(now);
        root.setDeleted(0);
        driveItemMapper.insert(root);

        OrgTeamSpace teamSpace = new OrgTeamSpace();
        teamSpace.setOrgId(orgId);
        teamSpace.setName(name);
        teamSpace.setSlug(slug);
        teamSpace.setDescription(description);
        teamSpace.setRootItemId(root.getId());
        teamSpace.setStorageLimitMb(storageLimitMb);
        teamSpace.setCreatedBy(userId);
        teamSpace.setCreatedAt(now);
        teamSpace.setUpdatedAt(now);
        teamSpace.setDeleted(0);
        orgTeamSpaceMapper.insert(teamSpace);

        root.setTeamSpaceId(teamSpace.getId());
        driveItemMapper.updateById(root);
        orgTeamSpaceGovernanceService.ensureCreatorManager(orgId, teamSpace.getId(), userId);

        auditService.record(userId, "ORG_TEAM_SPACE_CREATE", "orgId=" + orgId + ",teamSpaceId=" + teamSpace.getId() + ",storageLimitMb=" + storageLimitMb, ipAddress, orgId);
        AuditEventVo event = auditService.recordEvent(userId, "DRIVE_ITEM_CREATE", "itemId=" + root.getId() + ",type=FOLDER,teamSpaceId=" + teamSpace.getId(), ipAddress, orgId);
        suiteCollaborationService.publishToUser(userId, event);
        OrgTeamSpaceGovernanceService.TeamSpaceAccessScope access = orgTeamSpaceGovernanceService.describeAccess(userId, orgId, teamSpace.getId());
        return toTeamSpaceVo(teamSpace, access);
    }

    public List<OrgTeamSpaceItemVo> listTeamSpaceItems(
            Long userId,
            Long orgId,
            Long teamSpaceId,
            Long parentId,
            String keyword,
            String itemType,
            Integer limit,
            String ipAddress
    ) {
        orgTeamSpaceGovernanceService.requireReadAccess(userId, orgId, teamSpaceId);
        OrgTeamSpace teamSpace = loadTeamSpace(orgId, teamSpaceId);
        Long effectiveParentId = resolveBrowseParent(teamSpace, parentId);
        int safeLimit = normalizeLimit(limit);
        String safeKeyword = normalizeKeyword(keyword);
        String safeItemType = normalizeItemType(itemType);
        List<DriveItem> items = queryTeamSpaceItems(teamSpaceId, effectiveParentId, safeKeyword, safeItemType, safeLimit);
        auditService.record(userId, "ORG_TEAM_SPACE_ITEM_LIST", "orgId=" + orgId + ",teamSpaceId=" + teamSpaceId + ",count=" + items.size(), ipAddress, orgId);
        return items.stream().map(this::toTeamSpaceItemVo).toList();
    }

    @Transactional
    public OrgTeamSpaceItemVo createFolder(
            Long userId,
            Long orgId,
            Long teamSpaceId,
            CreateOrgTeamSpaceFolderRequest request,
            String ipAddress
    ) {
        orgTeamSpaceGovernanceService.requireWriteAccess(userId, orgId, teamSpaceId);
        OrgTeamSpace teamSpace = loadTeamSpace(orgId, teamSpaceId);
        Long parentId = resolveTargetParent(teamSpace, request.parentId());
        String name = normalizeName(request.name());
        ensureTeamSpaceNameUnique(teamSpaceId, parentId, ITEM_TYPE_FOLDER, name, null);
        LocalDateTime now = LocalDateTime.now();
        DriveItem item = new DriveItem();
        item.setOwnerId(userId);
        item.setParentId(parentId);
        item.setTeamSpaceId(teamSpaceId);
        item.setItemType(ITEM_TYPE_FOLDER);
        item.setName(name);
        item.setMimeType(null);
        item.setSizeBytes(0L);
        item.setStoragePath(null);
        item.setChecksum(null);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setDeleted(0);
        driveItemMapper.insert(item);
        auditService.record(userId, "ORG_TEAM_SPACE_FOLDER_CREATE", "orgId=" + orgId + ",teamSpaceId=" + teamSpaceId + ",itemId=" + item.getId(), ipAddress, orgId);
        AuditEventVo event = auditService.recordEvent(userId, "DRIVE_ITEM_CREATE", "itemId=" + item.getId() + ",type=FOLDER,teamSpaceId=" + teamSpaceId, ipAddress, orgId);
        suiteCollaborationService.publishToUser(userId, event);
        return toTeamSpaceItemVo(item);
    }

    @Transactional
    public OrgTeamSpaceItemVo uploadFile(
            Long userId,
            Long orgId,
            Long teamSpaceId,
            Long parentId,
            MultipartFile file,
            String ipAddress
    ) {
        orgTeamSpaceGovernanceService.requireWriteAccess(userId, orgId, teamSpaceId);
        OrgTeamSpace teamSpace = loadTeamSpace(orgId, teamSpaceId);
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "file is required");
        }
        Long targetParentId = resolveTargetParent(teamSpace, parentId);
        String fileName = normalizeName(resolveUploadName(file));
        ensureTeamSpaceNameUnique(teamSpaceId, targetParentId, ITEM_TYPE_FILE, fileName, null);
        byte[] content = readUploadBytes(file);
        long sizeBytes = content.length;
        assertTeamSpaceQuota(teamSpace, sizeBytes);
        LocalDateTime now = LocalDateTime.now();
        DriveItem item = new DriveItem();
        item.setOwnerId(userId);
        item.setParentId(targetParentId);
        item.setTeamSpaceId(teamSpaceId);
        item.setItemType(ITEM_TYPE_FILE);
        item.setName(fileName);
        item.setMimeType(normalizeNullableText(file.getContentType(), 128));
        item.setSizeBytes(sizeBytes);
        item.setStoragePath(storeUploadedFile(teamSpaceId, fileName, content));
        item.setChecksum(sha256(content));
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setDeleted(0);
        driveItemMapper.insert(item);
        auditService.record(userId, "ORG_TEAM_SPACE_FILE_UPLOAD", "orgId=" + orgId + ",teamSpaceId=" + teamSpaceId + ",itemId=" + item.getId() + ",sizeBytes=" + sizeBytes, ipAddress, orgId);
        AuditEventVo event = auditService.recordEvent(userId, "DRIVE_FILE_UPLOAD", "itemId=" + item.getId() + ",teamSpaceId=" + teamSpaceId + ",sizeBytes=" + sizeBytes, ipAddress, orgId);
        suiteCollaborationService.publishToUser(userId, event);
        return toTeamSpaceItemVo(item);
    }

    public DriveFileDownloadVo downloadFile(Long userId, Long orgId, Long teamSpaceId, Long itemId, String ipAddress) {
        orgTeamSpaceGovernanceService.requireReadAccess(userId, orgId, teamSpaceId);
        loadTeamSpace(orgId, teamSpaceId);
        DriveItem item = loadTeamSpaceItem(teamSpaceId, itemId);
        if (!ITEM_TYPE_FILE.equals(item.getItemType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Only file item can be downloaded");
        }
        byte[] content = readStoredFile(item);
        auditService.record(userId, "ORG_TEAM_SPACE_FILE_DOWNLOAD", "orgId=" + orgId + ",teamSpaceId=" + teamSpaceId + ",itemId=" + itemId, ipAddress, orgId);
        return new DriveFileDownloadVo(item.getName(), defaultMime(item.getMimeType()), content);
    }

    private List<DriveItem> queryTeamSpaceItems(Long teamSpaceId, Long parentId, String keyword, String itemType, int limit) {
        LambdaQueryWrapper<DriveItem> query = new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getTeamSpaceId, teamSpaceId)
                .eq(DriveItem::getParentId, parentId)
                .orderByDesc(DriveItem::getItemType)
                .orderByAsc(DriveItem::getName)
                .orderByDesc(DriveItem::getUpdatedAt)
                .last("limit " + limit);
        if (StringUtils.hasText(keyword)) {
            query.like(DriveItem::getName, keyword);
        }
        if (StringUtils.hasText(itemType)) {
            query.eq(DriveItem::getItemType, itemType);
        }
        return driveItemMapper.selectList(query);
    }

    private OrgMember requireManageMembership(Long userId, Long orgId) {
        OrgMember member = requireActiveMembership(userId, orgId);
        if (ROLE_OWNER.equals(member.getRole()) || ROLE_ADMIN.equals(member.getRole())) {
            return member;
        }
        throw new BizException(ErrorCode.ORG_FORBIDDEN, "Only OWNER or ADMIN can manage team spaces");
    }

    private OrgMember requireActiveMembership(Long userId, Long orgId) {
        OrgWorkspace org = loadOrg(orgId);
        OrgMember member = orgMemberMapper.selectOne(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getOrgId, orgId)
                .eq(OrgMember::getUserId, userId)
                .eq(OrgMember::getStatus, STATUS_ACTIVE)
                .last("limit 1"));
        if (member == null) {
            throw new BizException(ErrorCode.ORG_FORBIDDEN, "No active organization access for orgId=" + org.getId());
        }
        return member;
    }

    private OrgWorkspace loadOrg(Long orgId) {
        OrgWorkspace org = orgWorkspaceMapper.selectById(orgId);
        if (org == null) {
            throw new BizException(ErrorCode.ORG_NOT_FOUND);
        }
        return org;
    }

    private List<OrgMember> loadMembers(Long orgId) {
        return orgMemberMapper.selectList(new LambdaQueryWrapper<OrgMember>()
                .eq(OrgMember::getOrgId, orgId)
                .orderByDesc(OrgMember::getUpdatedAt));
    }

    private List<OrgTeamSpace> loadTeamSpaces(Long orgId) {
        return orgTeamSpaceMapper.selectList(new LambdaQueryWrapper<OrgTeamSpace>()
                .eq(OrgTeamSpace::getOrgId, orgId)
                .orderByDesc(OrgTeamSpace::getUpdatedAt));
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

    private DriveItem loadTeamSpaceItem(Long teamSpaceId, Long itemId) {
        DriveItem item = driveItemMapper.selectOne(new LambdaQueryWrapper<DriveItem>()
                .eq(DriveItem::getId, itemId)
                .eq(DriveItem::getTeamSpaceId, teamSpaceId)
                .last("limit 1"));
        if (item == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Team space item is not found");
        }
        return item;
    }

    private Long resolveBrowseParent(OrgTeamSpace teamSpace, Long parentId) {
        if (parentId == null) {
            return teamSpace.getRootItemId();
        }
        DriveItem parent = loadTeamSpaceItem(teamSpace.getId(), parentId);
        if (!ITEM_TYPE_FOLDER.equals(parent.getItemType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Parent item must be a folder");
        }
        return parent.getId();
    }

    private Long resolveTargetParent(OrgTeamSpace teamSpace, Long parentId) {
        Long targetParent = resolveBrowseParent(teamSpace, parentId);
        DriveItem parent = loadTeamSpaceItem(teamSpace.getId(), targetParent);
        if (!ITEM_TYPE_FOLDER.equals(parent.getItemType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Parent item must be a folder");
        }
        return targetParent;
    }

    private void ensureTeamSpaceNameUnique(Long teamSpaceId, Long parentId, String itemType, String name, Long excludeId) {
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

    private OrgTeamSpaceVo toTeamSpaceVo(OrgTeamSpace space, OrgTeamSpaceGovernanceService.TeamSpaceAccessScope access) {
        long storageBytes = safeLong(driveItemMapper.selectStorageBytesByTeamSpace(space.getId()));
        long itemCount = safeLong(driveItemMapper.countItemsByTeamSpace(space.getId())) - 1L;
        UserAccount creator = userAccountMapper.selectById(space.getCreatedBy());
        return new OrgTeamSpaceVo(
                String.valueOf(space.getId()),
                String.valueOf(space.getOrgId()),
                space.getName(),
                space.getSlug(),
                space.getDescription(),
                String.valueOf(space.getRootItemId()),
                storageBytes,
                safeStorageLimitBytes(space.getStorageLimitMb()),
                Math.max(itemCount, 0L),
                creator == null ? null : creator.getEmail(),
                access.role(),
                access.canWrite(),
                access.canManage(),
                space.getUpdatedAt()
        );
    }

    private OrgTeamSpaceItemVo toTeamSpaceItemVo(DriveItem item) {
        UserAccount owner = userAccountMapper.selectById(item.getOwnerId());
        return new OrgTeamSpaceItemVo(
                String.valueOf(item.getId()),
                String.valueOf(item.getTeamSpaceId()),
                item.getParentId() == null ? null : String.valueOf(item.getParentId()),
                item.getItemType(),
                item.getName(),
                item.getMimeType(),
                item.getSizeBytes() == null ? 0L : item.getSizeBytes(),
                owner == null ? null : owner.getEmail(),
                item.getUpdatedAt()
        );
    }

    private PolicySnapshot loadPolicy(Long orgId) {
        List<OrgPolicy> rows = orgPolicyMapper.selectList(new LambdaQueryWrapper<OrgPolicy>()
                .eq(OrgPolicy::getOrgId, orgId));
        List<String> allowedDomains = List.of();
        int reviewSla = 24;
        boolean dualReview = false;
        for (OrgPolicy row : rows) {
            if (POLICY_ALLOWED_EMAIL_DOMAINS.equals(row.getPolicyKey())) {
                allowedDomains = parseDomains(row.getPolicyValue());
            }
            if (POLICY_GOVERNANCE_REVIEW_SLA_HOURS.equals(row.getPolicyKey())) {
                reviewSla = parsePositiveInt(row.getPolicyValue(), 24);
            }
            if (POLICY_REQUIRE_DUAL_REVIEW_GOVERNANCE.equals(row.getPolicyKey())) {
                dualReview = Boolean.parseBoolean(row.getPolicyValue());
            }
        }
        return new PolicySnapshot(allowedDomains, reviewSla, dualReview);
    }

    private List<String> parseDomains(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        Set<String> items = new LinkedHashSet<>();
        for (String item : raw.split(",")) {
            if (StringUtils.hasText(item)) {
                items.add(item.trim().toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(items);
    }

    private int countByStatus(List<OrgMember> members, String status) {
        return (int) members.stream().filter(item -> status.equals(item.getStatus())).count();
    }

    private int countByRole(List<OrgMember> members, Set<String> roles) {
        return (int) members.stream().filter(item -> STATUS_ACTIVE.equals(item.getStatus()) && roles.contains(item.getRole())).count();
    }

    private int normalizeLimit(Integer limit) {
        int value = limit == null ? DEFAULT_LIMIT : limit;
        return Math.max(1, Math.min(value, MAX_LIMIT));
    }

    private int normalizeStorageLimit(Integer storageLimitMb) {
        if (storageLimitMb == null) {
            return DEFAULT_TEAM_SPACE_STORAGE_LIMIT_MB;
        }
        if (storageLimitMb < 1 || storageLimitMb > 102400) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "storageLimitMb must be between 1 and 102400");
        }
        return storageLimitMb;
    }

    private void assertTeamSpaceQuota(OrgTeamSpace teamSpace, long deltaBytes) {
        long currentBytes = safeLong(driveItemMapper.selectStorageBytesByTeamSpace(teamSpace.getId()));
        long limitBytes = safeStorageLimitBytes(teamSpace.getStorageLimitMb());
        if (currentBytes + deltaBytes > limitBytes) {
            throw new BizException(ErrorCode.QUOTA_EXCEEDED, "Team space storage quota exceeded");
        }
    }

    private String generateUniqueSlug(Long orgId, String name) {
        String base = slugify(name);
        String candidate = base;
        int suffix = 2;
        while (orgTeamSpaceMapper.selectCount(new LambdaQueryWrapper<OrgTeamSpace>()
                .eq(OrgTeamSpace::getOrgId, orgId)
                .eq(OrgTeamSpace::getSlug, candidate)) > 0) {
            candidate = base + "-" + suffix;
            suffix += 1;
        }
        return candidate;
    }

    private String slugify(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return StringUtils.hasText(normalized) ? normalized : "team-space";
    }

    private int parsePositiveInt(String raw, int fallback) {
        if (!StringUtils.hasText(raw)) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String normalizeName(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Name is required");
        }
        String value = raw.trim();
        if (value.length() > 128) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Name is too long");
        }
        return value;
    }

    private String normalizeKeyword(String raw) {
        return StringUtils.hasText(raw) ? raw.trim() : "";
    }

    private String normalizeItemType(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if (!ITEM_TYPE_FILE.equals(value) && !ITEM_TYPE_FOLDER.equals(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported itemType");
        }
        return value;
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

    private String resolveUploadName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName)) {
            return "upload.bin";
        }
        String trimmed = originalName.trim();
        return Paths.get(trimmed).getFileName().toString().replace("\\", "_").replace("/", "_");
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

    private String defaultMime(String mimeType) {
        return StringUtils.hasText(mimeType) ? mimeType : DEFAULT_BINARY_MIME;
    }

    private long safeStorageLimitBytes(Integer storageLimitMb) {
        return (long) (storageLimitMb == null ? DEFAULT_TEAM_SPACE_STORAGE_LIMIT_MB : storageLimitMb) * 1024L * 1024L;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private record PolicySnapshot(
            List<String> allowedEmailDomains,
            int governanceReviewSlaHours,
            boolean requireDualReviewGovernance
    ) {
    }
}
