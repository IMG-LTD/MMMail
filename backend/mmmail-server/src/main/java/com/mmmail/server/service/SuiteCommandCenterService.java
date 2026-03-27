package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.SuiteNotificationOperationLogMapper;
import com.mmmail.server.model.entity.SuiteNotificationOperationLog;
import com.mmmail.server.mapper.SuiteNotificationStateMapper;
import com.mmmail.server.model.entity.SuiteNotificationState;
import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.SearchHistoryVo;
import com.mmmail.server.model.vo.SearchPresetVo;
import com.mmmail.server.model.vo.SuiteBatchGovernanceReviewItemVo;
import com.mmmail.server.model.vo.SuiteBatchGovernanceReviewResultVo;
import com.mmmail.server.model.vo.SuiteBatchRemediationExecutionItemVo;
import com.mmmail.server.model.vo.SuiteBatchRemediationExecutionResultVo;
import com.mmmail.server.model.vo.SuiteCommandCenterVo;
import com.mmmail.server.model.vo.SuiteCommandFeedItemVo;
import com.mmmail.server.model.vo.SuiteCommandFeedVo;
import com.mmmail.server.model.vo.SuiteCommandItemVo;
import com.mmmail.server.model.vo.SuiteGovernanceChangeRequestVo;
import com.mmmail.server.model.vo.SuiteNotificationCenterVo;
import com.mmmail.server.model.vo.SuiteNotificationItemVo;
import com.mmmail.server.model.vo.SuiteNotificationMarkReadResultVo;
import com.mmmail.server.model.vo.SuiteNotificationOperationHistoryItemVo;
import com.mmmail.server.model.vo.SuiteNotificationOperationHistoryVo;
import com.mmmail.server.model.vo.SuiteNotificationWorkflowResultVo;
import com.mmmail.server.model.vo.SuiteProductStatusVo;
import com.mmmail.server.model.vo.SuiteReadinessItemVo;
import com.mmmail.server.model.vo.SuiteReadinessReportVo;
import com.mmmail.server.model.vo.SuiteRemediationActionVo;
import com.mmmail.server.model.vo.SuiteRemediationExecutionResultVo;
import com.mmmail.server.model.vo.SuiteSecurityPostureVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SuiteCommandCenterService {

    private static final int MAX_BATCH_REMEDIATION_COUNT = 20;
    private static final int MAX_BATCH_REVIEW_COUNT = 30;
    private static final int MAX_RECENT_KEYWORDS = 6;
    private static final int MAX_PINNED_SEARCHES = 8;
    private static final int MAX_RECOMMENDED_ACTIONS = 8;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 180;
    private static final int DEFAULT_COMMAND_FEED_LIMIT = 20;
    private static final int MAX_COMMAND_FEED_LIMIT = 50;
    private static final int DEFAULT_NOTIFICATION_LIMIT = 24;
    private static final int MAX_NOTIFICATION_LIMIT = 60;
    private static final int MAX_NOTIFICATION_PER_CHANNEL = 10;
    private static final int MAX_NOTIFICATION_MARK_READ_COUNT = 100;
    private static final int DEFAULT_NOTIFICATION_OPERATION_HISTORY_LIMIT = 12;
    private static final int MAX_NOTIFICATION_OPERATION_HISTORY_LIMIT = 50;
    private static final int MAX_ASSIGNEE_DISPLAY_NAME_LENGTH = 64;
    private static final String SUITE_EVENT_PREFIX = "SUITE_";
    private static final String COMMAND_FEED_QUERY_EVENT_TYPE = "SUITE_COMMAND_FEED_QUERY";
    private static final String NOTIFICATION_CENTER_QUERY_EVENT_TYPE = "SUITE_NOTIFICATION_CENTER_QUERY";
    private static final String NOTIFICATION_MARK_READ_EVENT_TYPE = "SUITE_NOTIFICATION_MARK_READ";
    private static final String NOTIFICATION_MARK_ALL_READ_EVENT_TYPE = "SUITE_NOTIFICATION_MARK_ALL_READ";
    private static final String NOTIFICATION_ARCHIVE_EVENT_TYPE = "SUITE_NOTIFICATION_ARCHIVE";
    private static final String NOTIFICATION_IGNORE_EVENT_TYPE = "SUITE_NOTIFICATION_IGNORE";
    private static final String NOTIFICATION_SNOOZE_EVENT_TYPE = "SUITE_NOTIFICATION_SNOOZE";
    private static final String NOTIFICATION_ASSIGN_EVENT_TYPE = "SUITE_NOTIFICATION_ASSIGN";
    private static final String NOTIFICATION_RESTORE_EVENT_TYPE = "SUITE_NOTIFICATION_RESTORE";
    private static final String NOTIFICATION_UNDO_EVENT_TYPE = "SUITE_NOTIFICATION_UNDO";
    private static final String NOTIFICATION_OPERATION_HISTORY_QUERY_EVENT_TYPE = "SUITE_NOTIFICATION_OPERATION_HISTORY_QUERY";
    private static final String NOTIFICATION_STATUS_ALL = "ALL";
    private static final String NOTIFICATION_STATUS_ACTIVE = "ACTIVE";
    private static final String NOTIFICATION_STATUS_ARCHIVED = "ARCHIVED";
    private static final String NOTIFICATION_STATUS_IGNORED = "IGNORED";
    private static final String NOTIFICATION_STATUS_SNOOZED = "SNOOZED";
    private static final Set<String> NOTIFICATION_WORKFLOW_EVENT_TYPES = Set.of(
            NOTIFICATION_ARCHIVE_EVENT_TYPE,
            NOTIFICATION_IGNORE_EVENT_TYPE,
            NOTIFICATION_RESTORE_EVENT_TYPE,
            NOTIFICATION_SNOOZE_EVENT_TYPE,
            NOTIFICATION_ASSIGN_EVENT_TYPE
    );
    private static final Set<String> SCOPE_RESTRICTED_AGGREGATE_COMMAND_EVENT_TYPES = Set.of(
            "SUITE_PLAN_LIST",
            "SUITE_SUBSCRIPTION_QUERY",
            "SUITE_PRODUCT_LIST",
            "SUITE_PLAN_CHANGE",
            "SUITE_READINESS_QUERY",
            "SUITE_SECURITY_POSTURE_QUERY",
            "SUITE_GOVERNANCE_OVERVIEW_QUERY",
            "SUITE_GOVERNANCE_TEMPLATE_LIST",
            "SUITE_GOVERNANCE_CHANGE_REQUEST_LIST",
            "SUITE_COMMAND_CENTER_QUERY",
            "SUITE_COLLABORATION_CENTER_QUERY",
            NOTIFICATION_CENTER_QUERY_EVENT_TYPE,
            NOTIFICATION_OPERATION_HISTORY_QUERY_EVENT_TYPE
    );
    private static final Set<String> PENDING_GOVERNANCE_STATUSES = Set.of(
            "PENDING_REVIEW",
            "PENDING_SECOND_REVIEW",
            "APPROVED_PENDING_EXECUTION"
    );
    private static final Map<String, String> PRODUCT_ROUTE_MAP = buildProductRouteMap();
    private static final Map<String, String> COMMAND_EVENT_TITLE_MAP = buildCommandEventTitleMap();
    private static final Map<String, String> COMMAND_EVENT_ROUTE_MAP = buildCommandEventRouteMap();
    private static final Map<String, Integer> NOTIFICATION_SEVERITY_WEIGHT_MAP = buildNotificationSeverityWeightMap();

    private final SuiteService suiteService;
    private final SuiteInsightService suiteInsightService;
    private final SearchHistoryService searchHistoryService;
    private final SearchPresetService searchPresetService;
    private final AuditService auditService;
    private final SuiteNotificationSyncService suiteNotificationSyncService;
    private final SuiteNotificationStateMapper suiteNotificationStateMapper;
    private final SuiteNotificationOperationLogMapper suiteNotificationOperationLogMapper;

    public SuiteCommandCenterService(
            SuiteService suiteService,
            SuiteInsightService suiteInsightService,
            SearchHistoryService searchHistoryService,
            SearchPresetService searchPresetService,
            AuditService auditService,
            SuiteNotificationSyncService suiteNotificationSyncService,
            SuiteNotificationStateMapper suiteNotificationStateMapper,
            SuiteNotificationOperationLogMapper suiteNotificationOperationLogMapper
    ) {
        this.suiteService = suiteService;
        this.suiteInsightService = suiteInsightService;
        this.searchHistoryService = searchHistoryService;
        this.searchPresetService = searchPresetService;
        this.auditService = auditService;
        this.suiteNotificationSyncService = suiteNotificationSyncService;
        this.suiteNotificationStateMapper = suiteNotificationStateMapper;
        this.suiteNotificationOperationLogMapper = suiteNotificationOperationLogMapper;
    }

    public SuiteCommandCenterVo getCommandCenter(Long userId, String ipAddress) {
        return getCommandCenter(userId, ipAddress, Set.of());
    }

    public SuiteCommandCenterVo getCommandCenter(Long userId, String ipAddress, Set<String> visibleProductCodes) {
        String safeIp = normalizeIp(ipAddress);
        boolean mailVisible = isProductVisible(visibleProductCodes, "MAIL");
        List<SuiteProductStatusVo> products = suiteService.listProducts(userId, safeIp).stream()
                .filter(product -> isProductVisible(visibleProductCodes, product.code()))
                .toList();
        List<SearchHistoryVo> histories = searchHistoryService.list(userId);
        List<SearchPresetVo> presets = searchPresetService.list(userId);
        SuiteSecurityPostureVo posture = suiteInsightService.getSecurityPosture(userId, safeIp, visibleProductCodes);
        List<SuiteGovernanceChangeRequestVo> governanceRequests = suiteInsightService.listGovernanceChangeRequests(userId, safeIp);

        List<SuiteCommandItemVo> quickRoutes = buildQuickRoutes(products);
        List<SuiteCommandItemVo> pinnedSearches = mailVisible ? buildPinnedSearches(presets) : List.of();
        List<String> recentKeywords = mailVisible ? buildRecentKeywords(histories) : List.of();
        List<com.mmmail.server.model.vo.SuiteRemediationActionVo> recommendedActions = posture.recommendedActions()
                .stream()
                .limit(MAX_RECOMMENDED_ACTIONS)
                .toList();
        long pendingGovernanceCount = governanceRequests.stream().filter(this::isPendingGovernance).count();

        auditService.record(
                userId,
                "SUITE_COMMAND_CENTER_QUERY",
                "routes=" + quickRoutes.size() + ",keywords=" + recentKeywords.size() + ",pending=" + pendingGovernanceCount,
                safeIp
        );
        return new SuiteCommandCenterVo(
                LocalDateTime.now(),
                quickRoutes,
                pinnedSearches,
                recentKeywords,
                recommendedActions,
                pendingGovernanceCount,
                posture.alerts().size()
        );
    }

    public SuiteBatchRemediationExecutionResultVo batchExecuteRemediationActions(
            Long userId,
            List<String> actionCodes,
            String ipAddress
    ) {
        String safeIp = normalizeIp(ipAddress);
        List<String> normalizedCodes = normalizeCodes(actionCodes, MAX_BATCH_REMEDIATION_COUNT, true);
        List<SuiteBatchRemediationExecutionItemVo> items = new ArrayList<>();
        int successCount = 0;
        for (String actionCode : normalizedCodes) {
            try {
                SuiteRemediationExecutionResultVo result = suiteInsightService.executeRemediationAction(userId, actionCode, safeIp);
                items.add(new SuiteBatchRemediationExecutionItemVo(actionCode, true, null, result.message(), result));
                successCount++;
            } catch (BizException exception) {
                items.add(new SuiteBatchRemediationExecutionItemVo(
                        actionCode,
                        false,
                        exception.getCode(),
                        safeErrorMessage(exception.getMessage()),
                        null
                ));
            }
        }
        int failedCount = items.size() - successCount;
        auditService.record(userId, "SUITE_REMEDIATION_BATCH_EXECUTE", buildBatchAuditDetail(items.size(), successCount), safeIp);
        return new SuiteBatchRemediationExecutionResultVo(LocalDateTime.now(), items.size(), successCount, failedCount, items);
    }

    public SuiteCommandFeedVo getCommandFeed(Long userId, Integer limit, String ipAddress) {
        return getCommandFeed(userId, limit, ipAddress, Set.of());
    }

    public SuiteCommandFeedVo getCommandFeed(Long userId, Integer limit, String ipAddress, Set<String> visibleProductCodes) {
        String safeIp = normalizeIp(ipAddress);
        int safeLimit = normalizeCommandFeedLimit(limit);
        List<SuiteCommandFeedItemVo> items = auditService.list(userId, false).stream()
                .filter(this::isSuiteCommandEvent)
                .map(this::toCommandFeedItem)
                .filter(item -> shouldIncludeCommandFeedItem(item, visibleProductCodes))
                .filter(item -> isProductVisible(visibleProductCodes, item.productCode()))
                .limit(safeLimit)
                .toList();
        auditService.record(
                userId,
                COMMAND_FEED_QUERY_EVENT_TYPE,
                "count=" + items.size() + ",limit=" + safeLimit,
                safeIp
        );
        return new SuiteCommandFeedVo(LocalDateTime.now(), safeLimit, items.size(), items);
    }

    public SuiteNotificationCenterVo getNotificationCenter(
            Long userId,
            Integer limit,
            Boolean unreadOnly,
            String status,
            Boolean includeSnoozed,
            String ipAddress
    ) {
        return getNotificationCenter(userId, limit, unreadOnly, status, includeSnoozed, ipAddress, Set.of());
    }

    public SuiteNotificationCenterVo getNotificationCenter(
            Long userId,
            Integer limit,
            Boolean unreadOnly,
            String status,
            Boolean includeSnoozed,
            String ipAddress,
            Set<String> visibleProductCodes
    ) {
        String safeIp = normalizeIp(ipAddress);
        int safeLimit = normalizeNotificationLimit(limit);
        String normalizedStatus = normalizeNotificationStatus(status);
        boolean allowSnoozed = Boolean.TRUE.equals(includeSnoozed);
        LocalDateTime now = LocalDateTime.now();
        SuiteSecurityPostureVo posture = suiteInsightService.getSecurityPosture(userId, safeIp, visibleProductCodes);
        SuiteReadinessReportVo readiness = suiteInsightService.getReadinessReport(userId, safeIp, visibleProductCodes);
        List<SuiteGovernanceChangeRequestVo> governanceRequests = suiteInsightService.listGovernanceChangeRequests(userId, safeIp);
        List<AuditEventVo> auditEvents = auditService.list(userId, false);

        List<SuiteNotificationItemVo> notifications = new ArrayList<>();
        appendSecurityAlertNotifications(notifications, posture, now);
        appendRecommendedActionNotifications(notifications, posture.recommendedActions(), now);
        appendReadinessNotifications(notifications, readiness.items(), now);
        appendGovernancePendingNotifications(notifications, governanceRequests);
        appendFailedExecutionNotifications(notifications, auditEvents);

        List<SuiteNotificationItemVo> deduplicated = deduplicateNotifications(notifications);
        List<SuiteNotificationItemVo> scopeFiltered = new ArrayList<>(deduplicated.stream()
                .filter(item -> isProductVisible(visibleProductCodes, item.productCode()))
                .toList());
        scopeFiltered.sort((left, right) -> compareNotificationPriority(right, left));
        Map<String, SuiteNotificationState> stateMap = synchronizeNotificationStates(userId, scopeFiltered, now);
        List<SuiteNotificationItemVo> withState = attachNotificationReadState(scopeFiltered, stateMap);
        List<SuiteNotificationItemVo> filtered = filterNotifications(withState, unreadOnly, normalizedStatus, allowSnoozed, now);
        List<SuiteNotificationItemVo> items = filtered.stream().limit(safeLimit).toList();
        int criticalCount = (int) filtered.stream().filter(item -> "CRITICAL".equals(item.severity())).count();
        int unreadCount = (int) filtered.stream().filter(item -> !item.read()).count();

        auditService.record(
                userId,
                NOTIFICATION_CENTER_QUERY_EVENT_TYPE,
                "count=" + items.size()
                        + ",limit=" + safeLimit
                        + ",critical=" + criticalCount
                        + ",unreadOnly=" + Boolean.TRUE.equals(unreadOnly)
                        + ",status=" + normalizedStatus
                        + ",includeSnoozed=" + allowSnoozed,
                safeIp
        );
        long syncCursor = suiteNotificationSyncService.getCurrentCursor(userId);
        return new SuiteNotificationCenterVo(
                LocalDateTime.now(),
                safeLimit,
                filtered.size(),
                criticalCount,
                unreadCount,
                syncCursor,
                suiteNotificationSyncService.buildSyncVersion(syncCursor),
                items
        );
    }

    public SuiteNotificationOperationHistoryVo getNotificationOperationHistory(Long userId, Integer limit, String ipAddress) {
        String safeIp = normalizeIp(ipAddress);
        int safeLimit = normalizeNotificationOperationHistoryLimit(limit);
        List<SuiteNotificationOperationHistoryItemVo> items = buildNotificationOperationHistory(userId, safeLimit);
        auditService.record(
                userId,
                NOTIFICATION_OPERATION_HISTORY_QUERY_EVENT_TYPE,
                "count=" + items.size() + ",limit=" + safeLimit,
                safeIp
        );
        long syncCursor = suiteNotificationSyncService.getCurrentCursor(userId);
        return new SuiteNotificationOperationHistoryVo(
                LocalDateTime.now(),
                safeLimit,
                items.size(),
                syncCursor,
                suiteNotificationSyncService.buildSyncVersion(syncCursor),
                items
        );
    }

    public SuiteNotificationMarkReadResultVo markNotificationsRead(
            Long userId,
            Long sessionId,
            List<String> notificationIds,
            String ipAddress
    ) {
        String safeIp = normalizeIp(ipAddress);
        List<String> normalizedIds = normalizeCodes(notificationIds, MAX_NOTIFICATION_MARK_READ_COUNT, false);
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<SuiteNotificationState> updateWrapper = new LambdaUpdateWrapper<SuiteNotificationState>()
                .eq(SuiteNotificationState::getOwnerId, userId)
                .in(SuiteNotificationState::getNotificationId, normalizedIds)
                .isNull(SuiteNotificationState::getReadAt)
                .set(SuiteNotificationState::getReadAt, now)
                .set(SuiteNotificationState::getUpdatedAt, now);
        int affectedCount = suiteNotificationStateMapper.update(null, updateWrapper);
        AuditEventVo event = auditService.recordEvent(
                userId,
                NOTIFICATION_MARK_READ_EVENT_TYPE,
                buildMarkReadAuditDetail(normalizedIds.size(), affectedCount, sessionId),
                safeIp
        );
        suiteNotificationSyncService.publish(userId, event);
        return new SuiteNotificationMarkReadResultVo(
                now,
                normalizedIds.size(),
                affectedCount,
                event.id(),
                suiteNotificationSyncService.buildSyncVersion(event.id())
        );
    }

    public SuiteNotificationMarkReadResultVo markAllNotificationsRead(Long userId, Long sessionId, String ipAddress) {
        String safeIp = normalizeIp(ipAddress);
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<SuiteNotificationState> updateWrapper = new LambdaUpdateWrapper<SuiteNotificationState>()
                .eq(SuiteNotificationState::getOwnerId, userId)
                .isNull(SuiteNotificationState::getReadAt)
                .set(SuiteNotificationState::getReadAt, now)
                .set(SuiteNotificationState::getUpdatedAt, now);
        int affectedCount = suiteNotificationStateMapper.update(null, updateWrapper);
        AuditEventVo event = auditService.recordEvent(
                userId,
                NOTIFICATION_MARK_ALL_READ_EVENT_TYPE,
                buildMarkAllReadAuditDetail(affectedCount, sessionId),
                safeIp
        );
        suiteNotificationSyncService.publish(userId, event);
        return new SuiteNotificationMarkReadResultVo(
                now,
                affectedCount,
                affectedCount,
                event.id(),
                suiteNotificationSyncService.buildSyncVersion(event.id())
        );
    }

    public SuiteNotificationWorkflowResultVo archiveNotifications(
            Long userId,
            Long sessionId,
            List<String> notificationIds,
            String ipAddress
    ) {
        return updateNotificationWorkflowStatus(
                userId,
                sessionId,
                notificationIds,
                NOTIFICATION_STATUS_ARCHIVED,
                NOTIFICATION_ARCHIVE_EVENT_TYPE,
                ipAddress
        );
    }

    public SuiteNotificationWorkflowResultVo ignoreNotifications(
            Long userId,
            Long sessionId,
            List<String> notificationIds,
            String ipAddress
    ) {
        return updateNotificationWorkflowStatus(
                userId,
                sessionId,
                notificationIds,
                NOTIFICATION_STATUS_IGNORED,
                NOTIFICATION_IGNORE_EVENT_TYPE,
                ipAddress
        );
    }

    public SuiteNotificationWorkflowResultVo restoreNotifications(
            Long userId,
            Long sessionId,
            List<String> notificationIds,
            String ipAddress
    ) {
        return updateNotificationWorkflowStatus(
                userId,
                sessionId,
                notificationIds,
                NOTIFICATION_STATUS_ACTIVE,
                NOTIFICATION_RESTORE_EVENT_TYPE,
                ipAddress
        );
    }

    public SuiteNotificationWorkflowResultVo snoozeNotifications(
            Long userId,
            Long sessionId,
            List<String> notificationIds,
            LocalDateTime snoozedUntil,
            String ipAddress
    ) {
        if (snoozedUntil == null || !snoozedUntil.isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "snoozedUntil must be a future datetime");
        }
        WorkflowOperationContext operationContext = prepareWorkflowOperation(userId, sessionId, notificationIds, ipAddress);
        LambdaUpdateWrapper<SuiteNotificationState> updateWrapper = new LambdaUpdateWrapper<SuiteNotificationState>()
                .eq(SuiteNotificationState::getOwnerId, userId)
                .in(SuiteNotificationState::getNotificationId, operationContext.notificationIds())
                .set(SuiteNotificationState::getWorkflowStatus, NOTIFICATION_STATUS_SNOOZED)
                .set(SuiteNotificationState::getSnoozedUntil, snoozedUntil)
                .set(SuiteNotificationState::getUpdatedAt, operationContext.executedAt());
        int affectedCount = suiteNotificationStateMapper.update(null, updateWrapper);
        AuditEventVo event = auditService.recordEvent(
                userId,
                NOTIFICATION_SNOOZE_EVENT_TYPE,
                buildWorkflowAuditDetail(operationContext, affectedCount, List.of("until=" + snoozedUntil)),
                operationContext.safeIp()
        );
        suiteNotificationSyncService.publish(userId, event);
        return new SuiteNotificationWorkflowResultVo(
                operationContext.executedAt(),
                "SNOOZE",
                operationContext.notificationIds().size(),
                affectedCount,
                operationContext.operationId(),
                event.id(),
                suiteNotificationSyncService.buildSyncVersion(event.id())
        );
    }

    public SuiteNotificationWorkflowResultVo assignNotifications(
            Long userId,
            Long sessionId,
            List<String> notificationIds,
            Long assigneeUserId,
            String assigneeDisplayName,
            String ipAddress
    ) {
        if (assigneeUserId == null || assigneeUserId <= 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "assigneeUserId must be positive");
        }
        String normalizedDisplayName = normalizeAssigneeDisplayName(assigneeDisplayName);
        WorkflowOperationContext operationContext = prepareWorkflowOperation(userId, sessionId, notificationIds, ipAddress);
        LambdaUpdateWrapper<SuiteNotificationState> updateWrapper = new LambdaUpdateWrapper<SuiteNotificationState>()
                .eq(SuiteNotificationState::getOwnerId, userId)
                .in(SuiteNotificationState::getNotificationId, operationContext.notificationIds())
                .set(SuiteNotificationState::getAssignedToUserId, assigneeUserId)
                .set(SuiteNotificationState::getAssignedToDisplayName, normalizedDisplayName)
                .set(SuiteNotificationState::getUpdatedAt, operationContext.executedAt());
        int affectedCount = suiteNotificationStateMapper.update(null, updateWrapper);
        AuditEventVo event = auditService.recordEvent(
                userId,
                NOTIFICATION_ASSIGN_EVENT_TYPE,
                buildWorkflowAuditDetail(operationContext, affectedCount, List.of("assignee=" + assigneeUserId)),
                operationContext.safeIp()
        );
        suiteNotificationSyncService.publish(userId, event);
        return new SuiteNotificationWorkflowResultVo(
                operationContext.executedAt(),
                "ASSIGN",
                operationContext.notificationIds().size(),
                affectedCount,
                operationContext.operationId(),
                event.id(),
                suiteNotificationSyncService.buildSyncVersion(event.id())
        );
    }

    public SuiteNotificationWorkflowResultVo undoNotificationWorkflow(Long userId, Long sessionId, String operationId, String ipAddress) {
        String normalizedOperationId = normalizeWorkflowOperationId(operationId);
        String safeIp = normalizeIp(ipAddress);
        List<SuiteNotificationOperationLog> logs = listUndoableOperationLogs(userId, normalizedOperationId);
        if (logs.isEmpty()) {
            if (hasOperationLogs(userId, normalizedOperationId)) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "operationId has already been undone");
            }
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "operationId does not exist");
        }
        LocalDateTime now = LocalDateTime.now();
        int affectedCount = applyUndoLogs(userId, logs, now);
        markOperationUndone(userId, normalizedOperationId, now);
        AuditEventVo event = auditService.recordEvent(
                userId,
                NOTIFICATION_UNDO_EVENT_TYPE,
                buildUndoAuditDetail(normalizedOperationId, logs.size(), affectedCount, sessionId),
                safeIp
        );
        suiteNotificationSyncService.publish(userId, event);
        return new SuiteNotificationWorkflowResultVo(
                now,
                "UNDO",
                logs.size(),
                affectedCount,
                normalizedOperationId,
                event.id(),
                suiteNotificationSyncService.buildSyncVersion(event.id())
        );
    }

    private SuiteNotificationWorkflowResultVo updateNotificationWorkflowStatus(
            Long userId,
            Long sessionId,
            List<String> notificationIds,
            String workflowStatus,
            String eventType,
            String ipAddress
    ) {
        WorkflowOperationContext operationContext = prepareWorkflowOperation(userId, sessionId, notificationIds, ipAddress);
        LambdaUpdateWrapper<SuiteNotificationState> updateWrapper = new LambdaUpdateWrapper<SuiteNotificationState>()
                .eq(SuiteNotificationState::getOwnerId, userId)
                .in(SuiteNotificationState::getNotificationId, operationContext.notificationIds())
                .set(SuiteNotificationState::getWorkflowStatus, workflowStatus)
                .set(SuiteNotificationState::getUpdatedAt, operationContext.executedAt());
        if (!NOTIFICATION_STATUS_SNOOZED.equals(workflowStatus)) {
            updateWrapper.set(SuiteNotificationState::getSnoozedUntil, null);
        }
        int affectedCount = suiteNotificationStateMapper.update(null, updateWrapper);
        AuditEventVo event = auditService.recordEvent(
                userId,
                eventType,
                buildWorkflowAuditDetail(operationContext, affectedCount, List.of("status=" + workflowStatus)),
                operationContext.safeIp()
        );
        suiteNotificationSyncService.publish(userId, event);
        String operation = eventType.replace("SUITE_NOTIFICATION_", "");
        return new SuiteNotificationWorkflowResultVo(
                operationContext.executedAt(),
                operation,
                operationContext.notificationIds().size(),
                affectedCount,
                operationContext.operationId(),
                event.id(),
                suiteNotificationSyncService.buildSyncVersion(event.id())
        );
    }

    private WorkflowOperationContext prepareWorkflowOperation(
            Long userId,
            Long sessionId,
            List<String> notificationIds,
            String ipAddress
    ) {
        String safeIp = normalizeIp(ipAddress);
        List<String> normalizedIds = normalizeCodes(notificationIds, MAX_NOTIFICATION_MARK_READ_COUNT, false);
        LocalDateTime now = LocalDateTime.now();
        String operationId = snapshotWorkflowOperation(userId, normalizedIds, now);
        return new WorkflowOperationContext(safeIp, normalizedIds, now, operationId, sessionId);
    }

    private String buildMarkReadAuditDetail(int requestedCount, int affectedCount, Long sessionId) {
        return buildAuditDetail(List.of(
                "requested=" + requestedCount,
                "affected=" + affectedCount,
                buildSessionAuditPart(sessionId)
        ));
    }

    private String buildMarkAllReadAuditDetail(int affectedCount, Long sessionId) {
        return buildAuditDetail(List.of(
                "requested=" + affectedCount,
                "affected=" + affectedCount,
                buildSessionAuditPart(sessionId)
        ));
    }

    private String buildWorkflowAuditDetail(
            WorkflowOperationContext operationContext,
            int affectedCount,
            List<String> extras
    ) {
        List<String> parts = new ArrayList<>();
        parts.add("requested=" + operationContext.notificationIds().size());
        parts.add("affected=" + affectedCount);
        parts.add("operationId=" + operationContext.operationId());
        parts.add(buildSessionAuditPart(operationContext.sessionId()));
        parts.addAll(extras);
        return buildAuditDetail(parts);
    }

    private String buildUndoAuditDetail(String operationId, int requestedCount, int affectedCount, Long sessionId) {
        return buildAuditDetail(List.of(
                "operationId=" + operationId,
                "requested=" + requestedCount,
                "affected=" + affectedCount,
                buildSessionAuditPart(sessionId)
        ));
    }

    private String buildAuditDetail(List<String> parts) {
        return parts.stream().filter(StringUtils::hasText).collect(Collectors.joining(","));
    }

    private String buildSessionAuditPart(Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            return "";
        }
        return "sessionId=" + sessionId;
    }

    private String snapshotWorkflowOperation(Long userId, List<String> notificationIds, LocalDateTime now) {
        String operationId = buildNotificationOperationId();
        Map<String, SuiteNotificationState> stateMap = fetchNotificationStateMap(userId, notificationIds);
        for (String notificationId : notificationIds) {
            SuiteNotificationOperationLog operationLog = buildOperationLog(userId, operationId, notificationId, now, stateMap.get(notificationId));
            suiteNotificationOperationLogMapper.insert(operationLog);
        }
        return operationId;
    }

    private Map<String, SuiteNotificationState> fetchNotificationStateMap(Long userId, List<String> notificationIds) {
        List<SuiteNotificationState> states = suiteNotificationStateMapper.selectList(new LambdaQueryWrapper<SuiteNotificationState>()
                .eq(SuiteNotificationState::getOwnerId, userId)
                .in(SuiteNotificationState::getNotificationId, notificationIds));
        return states.stream().collect(Collectors.toMap(
                SuiteNotificationState::getNotificationId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private SuiteNotificationOperationLog buildOperationLog(
            Long userId,
            String operationId,
            String notificationId,
            LocalDateTime now,
            SuiteNotificationState state
    ) {
        SuiteNotificationOperationLog operationLog = new SuiteNotificationOperationLog();
        operationLog.setOwnerId(userId);
        operationLog.setOperationId(operationId);
        operationLog.setNotificationId(notificationId);
        operationLog.setPreviousWorkflowStatus(resolvePreviousWorkflowStatus(state));
        operationLog.setPreviousSnoozedUntil(state == null ? null : state.getSnoozedUntil());
        operationLog.setPreviousAssignedToUserId(state == null ? null : state.getAssignedToUserId());
        operationLog.setPreviousAssignedToDisplayName(state == null ? null : state.getAssignedToDisplayName());
        operationLog.setUndone(0);
        operationLog.setCreatedAt(now);
        operationLog.setUpdatedAt(now);
        operationLog.setDeleted(0);
        return operationLog;
    }

    private String resolvePreviousWorkflowStatus(SuiteNotificationState state) {
        if (state == null || !StringUtils.hasText(state.getWorkflowStatus())) {
            return NOTIFICATION_STATUS_ACTIVE;
        }
        return state.getWorkflowStatus().trim().toUpperCase(Locale.ROOT);
    }

    private String buildNotificationOperationId() {
        return "NOP-" + UUID.randomUUID().toString().replace("-", "");
    }

    private List<SuiteNotificationOperationLog> listUndoableOperationLogs(Long userId, String operationId) {
        return suiteNotificationOperationLogMapper.selectList(new LambdaQueryWrapper<SuiteNotificationOperationLog>()
                .eq(SuiteNotificationOperationLog::getOwnerId, userId)
                .eq(SuiteNotificationOperationLog::getOperationId, operationId)
                .eq(SuiteNotificationOperationLog::getUndone, 0));
    }

    private boolean hasOperationLogs(Long userId, String operationId) {
        return suiteNotificationOperationLogMapper.selectCount(new LambdaQueryWrapper<SuiteNotificationOperationLog>()
                .eq(SuiteNotificationOperationLog::getOwnerId, userId)
                .eq(SuiteNotificationOperationLog::getOperationId, operationId)) > 0;
    }

    private int applyUndoLogs(Long userId, List<SuiteNotificationOperationLog> logs, LocalDateTime now) {
        int affectedCount = 0;
        for (SuiteNotificationOperationLog log : logs) {
            LambdaUpdateWrapper<SuiteNotificationState> updateWrapper = new LambdaUpdateWrapper<SuiteNotificationState>()
                    .eq(SuiteNotificationState::getOwnerId, userId)
                    .eq(SuiteNotificationState::getNotificationId, log.getNotificationId())
                    .set(SuiteNotificationState::getWorkflowStatus, log.getPreviousWorkflowStatus())
                    .set(SuiteNotificationState::getSnoozedUntil, log.getPreviousSnoozedUntil())
                    .set(SuiteNotificationState::getAssignedToUserId, log.getPreviousAssignedToUserId())
                    .set(SuiteNotificationState::getAssignedToDisplayName, log.getPreviousAssignedToDisplayName())
                    .set(SuiteNotificationState::getUpdatedAt, now);
            affectedCount += suiteNotificationStateMapper.update(null, updateWrapper);
        }
        return affectedCount;
    }

    private void markOperationUndone(Long userId, String operationId, LocalDateTime now) {
        suiteNotificationOperationLogMapper.update(
                null,
                new LambdaUpdateWrapper<SuiteNotificationOperationLog>()
                        .eq(SuiteNotificationOperationLog::getOwnerId, userId)
                        .eq(SuiteNotificationOperationLog::getOperationId, operationId)
                        .set(SuiteNotificationOperationLog::getUndone, 1)
                        .set(SuiteNotificationOperationLog::getUpdatedAt, now)
        );
    }

    private List<SuiteNotificationOperationHistoryItemVo> buildNotificationOperationHistory(Long userId, int limit) {
        List<AuditEventVo> events = auditService.list(userId, false);
        LinkedHashMap<String, SuiteNotificationOperationHistoryItemVo> historyMap = new LinkedHashMap<>();
        for (AuditEventVo event : events) {
            if (!NOTIFICATION_WORKFLOW_EVENT_TYPES.contains(event.eventType())) {
                continue;
            }
            String operationId = parseAuditDetailValue(event.detail(), "operationId");
            if (!StringUtils.hasText(operationId) || historyMap.containsKey(operationId)) {
                continue;
            }
            historyMap.put(operationId, new SuiteNotificationOperationHistoryItemVo(
                    operationId,
                    event.eventType().replace("SUITE_NOTIFICATION_", ""),
                    parseAuditDetailInt(event.detail(), "requested", 0),
                    parseAuditDetailInt(event.detail(), "affected", 0),
                    event.createdAt(),
                    true
            ));
            if (historyMap.size() >= limit) {
                break;
            }
        }
        if (historyMap.isEmpty()) {
            return List.of();
        }
        Map<String, Boolean> undoneStateMap = loadOperationUndoneState(userId, new ArrayList<>(historyMap.keySet()));
        return historyMap.values().stream()
                .map(item -> new SuiteNotificationOperationHistoryItemVo(
                        item.operationId(),
                        item.operation(),
                        item.requestedCount(),
                        item.affectedCount(),
                        item.executedAt(),
                        !undoneStateMap.getOrDefault(item.operationId(), false)
                ))
                .toList();
    }

    private Map<String, Boolean> loadOperationUndoneState(Long userId, List<String> operationIds) {
        if (operationIds.isEmpty()) {
            return Map.of();
        }
        List<SuiteNotificationOperationLog> logs = suiteNotificationOperationLogMapper.selectList(new LambdaQueryWrapper<SuiteNotificationOperationLog>()
                .eq(SuiteNotificationOperationLog::getOwnerId, userId)
                .in(SuiteNotificationOperationLog::getOperationId, operationIds));
        LinkedHashMap<String, Boolean> result = new LinkedHashMap<>();
        for (String operationId : operationIds) {
            result.put(operationId, false);
        }
        for (SuiteNotificationOperationLog log : logs) {
            if (log.getUndone() != null && log.getUndone() == 1) {
                result.put(log.getOperationId(), true);
            }
        }
        return result;
    }

    private int parseAuditDetailInt(String detail, String key, int defaultValue) {
        String rawValue = parseAuditDetailValue(detail, key);
        if (!StringUtils.hasText(rawValue)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private String parseAuditDetailValue(String detail, String key) {
        if (!StringUtils.hasText(detail)) {
            return null;
        }
        for (String fragment : detail.split(",")) {
            String[] pair = fragment.trim().split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            if (key.equals(pair[0].trim())) {
                return pair[1].trim();
            }
        }
        return null;
    }

    public SuiteBatchGovernanceReviewResultVo batchReviewGovernanceRequests(
            Long userId,
            Long sessionId,
            List<String> requestIds,
            String decision,
            String reviewNote,
            String ipAddress
    ) {
        String safeIp = normalizeIp(ipAddress);
        String normalizedDecision = normalizeDecision(decision);
        List<String> normalizedRequestIds = normalizeCodes(requestIds, MAX_BATCH_REVIEW_COUNT, false);
        List<SuiteBatchGovernanceReviewItemVo> items = new ArrayList<>();
        int successCount = 0;
        for (String requestId : normalizedRequestIds) {
            try {
                SuiteGovernanceChangeRequestVo reviewed = suiteInsightService.reviewGovernanceChangeRequest(
                        userId,
                        sessionId,
                        requestId,
                        normalizedDecision,
                        reviewNote,
                        safeIp
                );
                items.add(new SuiteBatchGovernanceReviewItemVo(requestId, true, null, "OK", reviewed));
                successCount++;
            } catch (BizException exception) {
                items.add(new SuiteBatchGovernanceReviewItemVo(
                        requestId,
                        false,
                        exception.getCode(),
                        safeErrorMessage(exception.getMessage()),
                        null
                ));
            }
        }
        int failedCount = items.size() - successCount;
        auditService.record(userId, "SUITE_GOVERNANCE_BATCH_REVIEW", buildBatchAuditDetail(items.size(), successCount), safeIp);
        return new SuiteBatchGovernanceReviewResultVo(
                LocalDateTime.now(),
                normalizedDecision,
                items.size(),
                successCount,
                failedCount,
                items
        );
    }

    private List<SuiteCommandItemVo> buildQuickRoutes(List<SuiteProductStatusVo> products) {
        return products.stream()
                .filter(SuiteProductStatusVo::enabledByPlan)
                .map(product -> new SuiteCommandItemVo(
                        "ROUTE",
                        product.name(),
                        product.description(),
                        PRODUCT_ROUTE_MAP.getOrDefault(product.code(), "/suite"),
                        null,
                        product.code(),
                        null
                ))
                .toList();
    }

    private List<SuiteCommandItemVo> buildPinnedSearches(List<SearchPresetVo> presets) {
        return presets.stream()
                .filter(SearchPresetVo::isPinned)
                .limit(MAX_PINNED_SEARCHES)
                .map(preset -> new SuiteCommandItemVo(
                        "PINNED_SEARCH",
                        preset.name(),
                        buildPresetDescription(preset),
                        buildPresetRoutePath(preset),
                        null,
                        null,
                        null
                ))
                .toList();
    }

    private List<String> buildRecentKeywords(List<SearchHistoryVo> histories) {
        LinkedHashSet<String> keywordSet = new LinkedHashSet<>();
        for (SearchHistoryVo item : histories) {
            if (!StringUtils.hasText(item.keyword())) {
                continue;
            }
            keywordSet.add(item.keyword());
            if (keywordSet.size() >= MAX_RECENT_KEYWORDS) {
                break;
            }
        }
        return new ArrayList<>(keywordSet);
    }

    private boolean isPendingGovernance(SuiteGovernanceChangeRequestVo item) {
        return PENDING_GOVERNANCE_STATUSES.contains(item.status());
    }

    private String buildPresetDescription(SearchPresetVo preset) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(preset.keyword())) {
            parts.add("keyword=" + preset.keyword());
        }
        if (StringUtils.hasText(preset.folder())) {
            parts.add("folder=" + preset.folder());
        }
        if (Boolean.TRUE.equals(preset.unread())) {
            parts.add("unread=true");
        }
        if (Boolean.TRUE.equals(preset.starred())) {
            parts.add("starred=true");
        }
        return String.join(", ", parts);
    }

    private String buildPresetRoutePath(SearchPresetVo preset) {
        if (!StringUtils.hasText(preset.keyword())) {
            return "/search";
        }
        return "/search?keyword=" + preset.keyword().trim();
    }

    private void appendSecurityAlertNotifications(
            List<SuiteNotificationItemVo> notifications,
            SuiteSecurityPostureVo posture,
            LocalDateTime now
    ) {
        int index = 0;
        String severity = normalizeSeverity(posture.overallRiskLevel());
        for (String alert : posture.alerts()) {
            if (!StringUtils.hasText(alert)) {
                continue;
            }
            if (index >= MAX_NOTIFICATION_PER_CHANNEL) {
                break;
            }
            index++;
            addNotification(notifications, new NotificationDraft(
                    "SECURITY",
                    severity,
                    "Security alert #" + index,
                    alert.trim(),
                    "/security",
                    null,
                    null,
                    now.minusSeconds(index)
            ));
        }
    }

    private void appendRecommendedActionNotifications(
            List<SuiteNotificationItemVo> notifications,
            List<SuiteRemediationActionVo> actions,
            LocalDateTime now
    ) {
        int index = 0;
        for (SuiteRemediationActionVo action : actions) {
            if (!StringUtils.hasText(action.actionCode())) {
                continue;
            }
            if (index >= MAX_NOTIFICATION_PER_CHANNEL) {
                break;
            }
            index++;
            addNotification(notifications, new NotificationDraft(
                    "ACTION",
                    mapPriorityToSeverity(action.priority()),
                    "Recommended action: " + action.productCode(),
                    action.action(),
                    "/command-center",
                    action.actionCode(),
                    action.productCode(),
                    now.minusSeconds(20L + index)
            ));
        }
    }

    private void appendReadinessNotifications(
            List<SuiteNotificationItemVo> notifications,
            List<SuiteReadinessItemVo> readinessItems,
            LocalDateTime now
    ) {
        int index = 0;
        for (SuiteReadinessItemVo item : readinessItems) {
            if (item.blockers().isEmpty()) {
                continue;
            }
            if (index >= MAX_NOTIFICATION_PER_CHANNEL) {
                break;
            }
            index++;
            String firstBlocker = item.blockers().get(0);
            String routePath = PRODUCT_ROUTE_MAP.getOrDefault(item.productCode(), "/suite");
            addNotification(notifications, new NotificationDraft(
                    "READINESS",
                    normalizeSeverity(item.riskLevel()),
                    item.productName() + " readiness risk",
                    firstBlocker,
                    routePath,
                    null,
                    item.productCode(),
                    now.minusSeconds(40L + index)
            ));
        }
    }

    private void appendGovernancePendingNotifications(
            List<SuiteNotificationItemVo> notifications,
            List<SuiteGovernanceChangeRequestVo> requests
    ) {
        int index = 0;
        for (SuiteGovernanceChangeRequestVo request : requests) {
            if (!isPendingGovernance(request)) {
                continue;
            }
            if (index >= MAX_NOTIFICATION_PER_CHANNEL) {
                break;
            }
            index++;
            addNotification(notifications, new NotificationDraft(
                    "GOVERNANCE",
                    "HIGH",
                    "Governance review pending",
                    request.requestId() + " · " + request.templateName(),
                    "/suite",
                    null,
                    null,
                    request.requestedAt()
            ));
        }
    }

    private void appendFailedExecutionNotifications(
            List<SuiteNotificationItemVo> notifications,
            List<AuditEventVo> auditEvents
    ) {
        int index = 0;
        for (AuditEventVo event : auditEvents) {
            if (!isFailedEvent(event)) {
                continue;
            }
            if (index >= MAX_NOTIFICATION_PER_CHANNEL) {
                break;
            }
            index++;
            addNotification(notifications, new NotificationDraft(
                    "ACTION",
                    "HIGH",
                    "Execution failed",
                    event.detail(),
                    COMMAND_EVENT_ROUTE_MAP.getOrDefault(event.eventType(), "/command-center"),
                    null,
                    null,
                    event.createdAt()
            ));
        }
    }

    private boolean isFailedEvent(AuditEventVo event) {
        if (!StringUtils.hasText(event.eventType())) {
            return false;
        }
        String eventType = event.eventType().trim();
        if ("SUITE_REMEDIATION_ACTION_FAILED".equals(eventType)) {
            return true;
        }
        if (!eventType.contains("BATCH")) {
            return false;
        }
        if (!StringUtils.hasText(event.detail())) {
            return false;
        }
        return !event.detail().contains("failed=0");
    }

    private List<SuiteNotificationItemVo> deduplicateNotifications(List<SuiteNotificationItemVo> source) {
        LinkedHashMap<String, SuiteNotificationItemVo> uniqueMap = new LinkedHashMap<>();
        for (SuiteNotificationItemVo item : source) {
            String key = item.channel() + "|" + item.title() + "|" + item.message();
            uniqueMap.putIfAbsent(key, item);
        }
        return new ArrayList<>(uniqueMap.values());
    }

    private int compareNotificationPriority(SuiteNotificationItemVo left, SuiteNotificationItemVo right) {
        int severityCompare = Integer.compare(notificationSeverityWeight(left.severity()), notificationSeverityWeight(right.severity()));
        if (severityCompare != 0) {
            return severityCompare;
        }
        return left.createdAt().compareTo(right.createdAt());
    }

    private void addNotification(List<SuiteNotificationItemVo> notifications, NotificationDraft draft) {
        String notificationId = "NTF-" + buildNotificationFingerprint(draft);
        notifications.add(new SuiteNotificationItemVo(
                notificationId,
                draft.channel(),
                draft.severity(),
                draft.title(),
                draft.message(),
                draft.routePath(),
                draft.actionCode(),
                draft.productCode(),
                draft.createdAt(),
                false,
                null,
                NOTIFICATION_STATUS_ACTIVE,
                null,
                null,
                null
        ));
    }

    private Map<String, SuiteNotificationState> synchronizeNotificationStates(
            Long userId,
            List<SuiteNotificationItemVo> notifications,
            LocalDateTime now
    ) {
        if (notifications.isEmpty()) {
            return Map.of();
        }
        List<String> notificationIds = notifications.stream()
                .map(SuiteNotificationItemVo::notificationId)
                .distinct()
                .toList();
        List<SuiteNotificationState> existing = suiteNotificationStateMapper.selectList(new LambdaQueryWrapper<SuiteNotificationState>()
                .eq(SuiteNotificationState::getOwnerId, userId)
                .in(SuiteNotificationState::getNotificationId, notificationIds));
        Map<String, SuiteNotificationState> stateMap = existing.stream()
                .collect(Collectors.toMap(SuiteNotificationState::getNotificationId, item -> item, (left, right) -> left, LinkedHashMap::new));
        refreshOrInsertNotificationStates(userId, notificationIds, now, stateMap);
        return stateMap;
    }

    private void refreshOrInsertNotificationStates(
            Long userId,
            List<String> notificationIds,
            LocalDateTime now,
            Map<String, SuiteNotificationState> stateMap
    ) {
        for (String notificationId : notificationIds) {
            SuiteNotificationState state = stateMap.get(notificationId);
            if (state == null) {
                SuiteNotificationState inserted = createNotificationState(userId, notificationId, now);
                suiteNotificationStateMapper.insert(inserted);
                stateMap.put(notificationId, inserted);
                continue;
            }
            synchronizeExpiredSnooze(state, now);
            normalizeLegacyWorkflowStatus(state);
            state.setLastSeenAt(now);
            state.setUpdatedAt(now);
            suiteNotificationStateMapper.updateById(state);
        }
    }

    private void synchronizeExpiredSnooze(SuiteNotificationState state, LocalDateTime now) {
        if (!NOTIFICATION_STATUS_SNOOZED.equals(state.getWorkflowStatus())) {
            return;
        }
        LocalDateTime snoozedUntil = state.getSnoozedUntil();
        if (snoozedUntil == null || !snoozedUntil.isAfter(now)) {
            state.setWorkflowStatus(NOTIFICATION_STATUS_ACTIVE);
            state.setSnoozedUntil(null);
        }
    }

    private void normalizeLegacyWorkflowStatus(SuiteNotificationState state) {
        if (!StringUtils.hasText(state.getWorkflowStatus())) {
            state.setWorkflowStatus(NOTIFICATION_STATUS_ACTIVE);
        }
    }

    private SuiteNotificationState createNotificationState(Long userId, String notificationId, LocalDateTime now) {
        SuiteNotificationState state = new SuiteNotificationState();
        state.setOwnerId(userId);
        state.setNotificationId(notificationId);
        state.setFirstSeenAt(now);
        state.setLastSeenAt(now);
        state.setReadAt(null);
        state.setWorkflowStatus(NOTIFICATION_STATUS_ACTIVE);
        state.setSnoozedUntil(null);
        state.setAssignedToUserId(null);
        state.setAssignedToDisplayName(null);
        state.setCreatedAt(now);
        state.setUpdatedAt(now);
        state.setDeleted(0);
        return state;
    }

    private List<SuiteNotificationItemVo> attachNotificationReadState(
            List<SuiteNotificationItemVo> notifications,
            Map<String, SuiteNotificationState> stateMap
    ) {
        return notifications.stream()
                .map(item -> {
                    SuiteNotificationState state = stateMap.get(item.notificationId());
                    boolean read = state != null && state.getReadAt() != null;
                    LocalDateTime readAt = state == null ? null : state.getReadAt();
                    String workflowStatus = resolveWorkflowStatus(state);
                    LocalDateTime snoozedUntil = state == null ? null : state.getSnoozedUntil();
                    Long assignedToUserId = state == null ? null : state.getAssignedToUserId();
                    String assignedToDisplayName = state == null ? null : state.getAssignedToDisplayName();
                    return new SuiteNotificationItemVo(
                            item.notificationId(),
                            item.channel(),
                            item.severity(),
                            item.title(),
                            item.message(),
                            item.routePath(),
                            item.actionCode(),
                            item.productCode(),
                            item.createdAt(),
                            read,
                            readAt,
                            workflowStatus,
                            snoozedUntil,
                            assignedToUserId,
                            assignedToDisplayName
                    );
                })
                .toList();
    }

    private String resolveWorkflowStatus(SuiteNotificationState state) {
        if (state == null || !StringUtils.hasText(state.getWorkflowStatus())) {
            return NOTIFICATION_STATUS_ACTIVE;
        }
        return state.getWorkflowStatus().trim().toUpperCase(Locale.ROOT);
    }

    private List<SuiteNotificationItemVo> filterNotifications(
            List<SuiteNotificationItemVo> notifications,
            Boolean unreadOnly,
            String status,
            boolean includeSnoozed,
            LocalDateTime now
    ) {
        return notifications.stream()
                .filter(item -> matchStatusFilter(item, status))
                .filter(item -> includeSnoozed || matchSnoozeFilter(item, status, now))
                .filter(item -> !Boolean.TRUE.equals(unreadOnly) || !item.read())
                .toList();
    }

    private boolean matchStatusFilter(SuiteNotificationItemVo item, String status) {
        if (NOTIFICATION_STATUS_ALL.equals(status)) {
            return true;
        }
        return status.equals(item.workflowStatus());
    }

    private boolean matchSnoozeFilter(SuiteNotificationItemVo item, String status, LocalDateTime now) {
        if (NOTIFICATION_STATUS_SNOOZED.equals(status)) {
            return true;
        }
        if (item.snoozedUntil() == null) {
            return true;
        }
        return !item.snoozedUntil().isAfter(now);
    }

    private String buildNotificationFingerprint(NotificationDraft draft) {
        String raw = String.join("|",
                safeFingerprintPart(draft.channel()),
                safeFingerprintPart(draft.severity()),
                safeFingerprintPart(draft.title()),
                safeFingerprintPart(draft.message()),
                safeFingerprintPart(draft.routePath()),
                safeFingerprintPart(draft.actionCode()),
                safeFingerprintPart(draft.productCode())
        );
        return sha256HexPrefix(raw, 8);
    }

    private String safeFingerprintPart(String value) {
        return value == null ? "" : value.trim();
    }

    private String sha256HexPrefix(String value, int byteLength) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes, 0, byteLength);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String mapPriorityToSeverity(String priority) {
        if ("P0".equalsIgnoreCase(priority)) {
            return "CRITICAL";
        }
        if ("P1".equalsIgnoreCase(priority)) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    private String normalizeSeverity(String riskLevel) {
        if (!StringUtils.hasText(riskLevel)) {
            return "MEDIUM";
        }
        String normalized = riskLevel.trim().toUpperCase(Locale.ROOT);
        if ("CRITICAL".equals(normalized) || "HIGH".equals(normalized) || "MEDIUM".equals(normalized) || "LOW".equals(normalized)) {
            return normalized;
        }
        return "MEDIUM";
    }

    private String normalizeNotificationStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return NOTIFICATION_STATUS_ACTIVE;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (NOTIFICATION_STATUS_ALL.equals(normalized)
                || NOTIFICATION_STATUS_ACTIVE.equals(normalized)
                || NOTIFICATION_STATUS_ARCHIVED.equals(normalized)
                || NOTIFICATION_STATUS_IGNORED.equals(normalized)
                || NOTIFICATION_STATUS_SNOOZED.equals(normalized)) {
            return normalized;
        }
        throw new BizException(
                ErrorCode.INVALID_ARGUMENT,
                "status must be ALL, ACTIVE, ARCHIVED, IGNORED or SNOOZED"
        );
    }

    private String normalizeAssigneeDisplayName(String assigneeDisplayName) {
        if (!StringUtils.hasText(assigneeDisplayName)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "assigneeDisplayName is required");
        }
        String normalized = assigneeDisplayName.trim();
        if (normalized.length() > MAX_ASSIGNEE_DISPLAY_NAME_LENGTH) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "assigneeDisplayName exceeds max length " + MAX_ASSIGNEE_DISPLAY_NAME_LENGTH
            );
        }
        return normalized;
    }

    private String normalizeWorkflowOperationId(String operationId) {
        if (!StringUtils.hasText(operationId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "operationId is required");
        }
        String normalized = operationId.trim();
        if (normalized.length() > 64) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "operationId exceeds max length 64");
        }
        return normalized;
    }

    private int notificationSeverityWeight(String severity) {
        return NOTIFICATION_SEVERITY_WEIGHT_MAP.getOrDefault(severity, 2);
    }

    private int normalizeNotificationLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_NOTIFICATION_LIMIT;
        }
        if (limit < 1 || limit > MAX_NOTIFICATION_LIMIT) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "limit must be between 1 and " + MAX_NOTIFICATION_LIMIT
            );
        }
        return limit;
    }

    private int normalizeNotificationOperationHistoryLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_NOTIFICATION_OPERATION_HISTORY_LIMIT;
        }
        if (limit < 1 || limit > MAX_NOTIFICATION_OPERATION_HISTORY_LIMIT) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "limit must be between 1 and " + MAX_NOTIFICATION_OPERATION_HISTORY_LIMIT
            );
        }
        return limit;
    }

    private boolean isSuiteCommandEvent(AuditEventVo event) {
        if (!StringUtils.hasText(event.eventType())) {
            return false;
        }
        String normalizedType = event.eventType().trim();
        return normalizedType.startsWith(SUITE_EVENT_PREFIX)
                && !COMMAND_FEED_QUERY_EVENT_TYPE.equals(normalizedType);
    }

    private SuiteCommandFeedItemVo toCommandFeedItem(AuditEventVo event) {
        String eventType = event.eventType().trim();
        String title = COMMAND_EVENT_TITLE_MAP.getOrDefault(eventType, normalizeEventTypeAsLabel(eventType));
        return new SuiteCommandFeedItemVo(
                event.id(),
                eventType,
                resolveCommandEventCategory(eventType),
                title,
                event.detail(),
                resolveCommandFeedProductCode(event),
                COMMAND_EVENT_ROUTE_MAP.getOrDefault(eventType, "/command-center"),
                event.ipAddress(),
                event.createdAt()
        );
    }

    private boolean shouldIncludeCommandFeedItem(SuiteCommandFeedItemVo item, Set<String> visibleProductCodes) {
        if (visibleProductCodes == null || visibleProductCodes.isEmpty()) {
            return true;
        }
        if (StringUtils.hasText(item.productCode())) {
            return true;
        }
        return !SCOPE_RESTRICTED_AGGREGATE_COMMAND_EVENT_TYPES.contains(item.eventType());
    }

    private String resolveCommandFeedProductCode(AuditEventVo event) {
        String detailProduct = parseDetailValue(event.detail(), "product");
        if (StringUtils.hasText(detailProduct)) {
            return detailProduct.trim().toUpperCase(Locale.ROOT);
        }
        String actionCode = parseDetailValue(event.detail(), "actionCode");
        if (StringUtils.hasText(actionCode)) {
            return resolveActionProductCode(actionCode);
        }
        return resolveRouteProductCode(COMMAND_EVENT_ROUTE_MAP.getOrDefault(event.eventType(), "/command-center"));
    }

    private String resolveActionProductCode(String actionCode) {
        if (!StringUtils.hasText(actionCode)) {
            return null;
        }
        String normalized = actionCode.trim().toUpperCase(Locale.ROOT);
        for (String productCode : List.of("STANDARD_NOTES", "SIMPLELOGIN", "AUTHENTICATOR", "CALENDAR", "DRIVE", "DOCS", "SHEETS", "WALLET", "MAIL", "PASS", "MEET", "LUMO", "VPN")) {
            if (normalized.startsWith(productCode + "_")) {
                return productCode;
            }
        }
        return null;
    }

    private String resolveRouteProductCode(String routePath) {
        if (!StringUtils.hasText(routePath)) {
            return null;
        }
        if (routePath.startsWith("/authenticator")) {
            return "AUTHENTICATOR";
        }
        if (routePath.startsWith("/calendar")) {
            return "CALENDAR";
        }
        if (routePath.startsWith("/docs")) {
            return "DOCS";
        }
        if (routePath.startsWith("/drive")) {
            return "DRIVE";
        }
        if (routePath.startsWith("/lumo")) {
            return "LUMO";
        }
        if (routePath.startsWith("/meet")) {
            return "MEET";
        }
        if (routePath.startsWith("/pass")) {
            return "PASS";
        }
        if (routePath.startsWith("/search") || routePath.startsWith("/mail/")) {
            return "MAIL";
        }
        if (routePath.startsWith("/sheets")) {
            return "SHEETS";
        }
        if (routePath.startsWith("/simplelogin")) {
            return "SIMPLELOGIN";
        }
        if (routePath.startsWith("/standard-notes")) {
            return "STANDARD_NOTES";
        }
        if (routePath.startsWith("/vpn")) {
            return "VPN";
        }
        if (routePath.startsWith("/wallet")) {
            return "WALLET";
        }
        return null;
    }

    private boolean isProductVisible(Set<String> visibleProductCodes, String productCode) {
        if (visibleProductCodes == null || visibleProductCodes.isEmpty()) {
            return true;
        }
        if (!StringUtils.hasText(productCode)) {
            return true;
        }
        return visibleProductCodes.contains(productCode.trim().toUpperCase(Locale.ROOT));
    }

    private String parseDetailValue(String detail, String key) {
        if (!StringUtils.hasText(detail) || !StringUtils.hasText(key)) {
            return null;
        }
        String prefix = key + "=";
        for (String part : detail.split("[;,]")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(prefix)) {
                return trimmed.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private String resolveCommandEventCategory(String eventType) {
        if (eventType.contains("BATCH")) {
            return "BATCH";
        }
        if (eventType.contains("GOVERNANCE")) {
            return "GOVERNANCE";
        }
        if (eventType.contains("REMEDIATION")) {
            return "REMEDIATION";
        }
        if (eventType.contains("SEARCH")) {
            return "SEARCH";
        }
        if (eventType.contains("SECURITY")) {
            return "SECURITY";
        }
        if (eventType.contains("NOTIFICATION")) {
            return "NOTIFICATION";
        }
        return "SUITE";
    }

    private String normalizeEventTypeAsLabel(String eventType) {
        return eventType.replace('_', ' ').trim();
    }

    private int normalizeCommandFeedLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_COMMAND_FEED_LIMIT;
        }
        if (limit < 1 || limit > MAX_COMMAND_FEED_LIMIT) {
            throw new BizException(
                    ErrorCode.INVALID_ARGUMENT,
                    "limit must be between 1 and " + MAX_COMMAND_FEED_LIMIT
            );
        }
        return limit;
    }

    private List<String> normalizeCodes(List<String> sourceCodes, int maxCount, boolean toUpperCase) {
        if (sourceCodes == null || sourceCodes.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Batch list must not be empty");
        }
        LinkedHashSet<String> normalizedSet = new LinkedHashSet<>();
        for (String code : sourceCodes) {
            if (!StringUtils.hasText(code)) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Batch list contains blank item");
            }
            String normalized = code.trim();
            if (toUpperCase) {
                normalized = normalized.toUpperCase(Locale.ROOT);
            }
            normalizedSet.add(normalized);
            if (normalizedSet.size() > maxCount) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Batch list exceeds max size " + maxCount);
            }
        }
        return new ArrayList<>(normalizedSet);
    }

    private String normalizeDecision(String decision) {
        if (!StringUtils.hasText(decision)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Decision is required");
        }
        String normalized = decision.trim().toUpperCase(Locale.ROOT);
        if (!"APPROVE".equals(normalized) && !"REJECT".equals(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Decision must be APPROVE or REJECT");
        }
        return normalized;
    }

    private String safeErrorMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "unknown";
        }
        String normalized = message.trim();
        if (normalized.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private String buildBatchAuditDetail(int totalCount, int successCount) {
        return "total=" + totalCount + ",success=" + successCount + ",failed=" + (totalCount - successCount);
    }

    private String normalizeIp(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return "unknown";
        }
        return ipAddress.trim();
    }

    private static Map<String, String> buildProductRouteMap() {
        Map<String, String> routeMap = new LinkedHashMap<>();
        routeMap.put("MAIL", "/inbox");
        routeMap.put("DOCS", "/docs");
        routeMap.put("SHEETS", "/sheets");
        routeMap.put("DRIVE", "/drive");
        routeMap.put("PASS", "/pass");
        routeMap.put("SIMPLELOGIN", "/simplelogin");
        routeMap.put("STANDARD_NOTES", "/standard-notes");
        routeMap.put("AUTH", "/authenticator");
        routeMap.put("CALENDAR", "/calendar");
        routeMap.put("VPN", "/vpn");
        routeMap.put("WALLET", "/wallet");
        routeMap.put("MEET", "/meet");
        routeMap.put("LUMO", "/lumo");
        routeMap.put("CONTACTS", "/contacts");
        return routeMap;
    }

    private static Map<String, String> buildCommandEventTitleMap() {
        Map<String, String> titleMap = new LinkedHashMap<>();
        titleMap.put("SUITE_COMMAND_CENTER_QUERY", "Command center viewed");
        titleMap.put("SUITE_UNIFIED_SEARCH_QUERY", "Unified search executed");
        titleMap.put("SUITE_REMEDIATION_ACTION_EXECUTE", "Remediation action executed");
        titleMap.put("SUITE_REMEDIATION_ACTION_FAILED", "Remediation action failed");
        titleMap.put("SUITE_REMEDIATION_BATCH_EXECUTE", "Batch remediation executed");
        titleMap.put("SUITE_GOVERNANCE_CHANGE_REQUEST_CREATE", "Governance request created");
        titleMap.put("SUITE_GOVERNANCE_CHANGE_REQUEST_REVIEW", "Governance request reviewed");
        titleMap.put("SUITE_GOVERNANCE_CHANGE_REQUEST_APPROVE", "Governance request approved");
        titleMap.put("SUITE_GOVERNANCE_CHANGE_REQUEST_ROLLBACK", "Governance request rolled back");
        titleMap.put("SUITE_GOVERNANCE_BATCH_REVIEW", "Batch governance review executed");
        titleMap.put("SUITE_SECURITY_POSTURE_QUERY", "Security posture viewed");
        titleMap.put("SUITE_READINESS_QUERY", "Readiness report viewed");
        titleMap.put("SUITE_NOTIFICATION_CENTER_QUERY", "Notification center viewed");
        titleMap.put("SUITE_NOTIFICATION_MARK_READ", "Notifications marked as read");
        titleMap.put("SUITE_NOTIFICATION_MARK_ALL_READ", "All notifications marked as read");
        titleMap.put("SUITE_NOTIFICATION_ARCHIVE", "Notifications archived");
        titleMap.put("SUITE_NOTIFICATION_IGNORE", "Notifications ignored");
        titleMap.put("SUITE_NOTIFICATION_SNOOZE", "Notifications snoozed");
        titleMap.put("SUITE_NOTIFICATION_ASSIGN", "Notifications assigned");
        titleMap.put("SUITE_NOTIFICATION_RESTORE", "Notifications restored");
        titleMap.put("SUITE_NOTIFICATION_UNDO", "Notification workflow action undone");
        titleMap.put("SUITE_NOTIFICATION_OPERATION_HISTORY_QUERY", "Notification operation history viewed");
        return titleMap;
    }

    private static Map<String, String> buildCommandEventRouteMap() {
        Map<String, String> routeMap = new LinkedHashMap<>();
        routeMap.put("SUITE_UNIFIED_SEARCH_QUERY", "/search");
        routeMap.put("SUITE_REMEDIATION_ACTION_EXECUTE", "/command-center");
        routeMap.put("SUITE_REMEDIATION_ACTION_FAILED", "/command-center");
        routeMap.put("SUITE_REMEDIATION_BATCH_EXECUTE", "/command-center");
        routeMap.put("SUITE_GOVERNANCE_CHANGE_REQUEST_CREATE", "/suite");
        routeMap.put("SUITE_GOVERNANCE_CHANGE_REQUEST_REVIEW", "/suite");
        routeMap.put("SUITE_GOVERNANCE_CHANGE_REQUEST_APPROVE", "/suite");
        routeMap.put("SUITE_GOVERNANCE_CHANGE_REQUEST_ROLLBACK", "/suite");
        routeMap.put("SUITE_GOVERNANCE_BATCH_REVIEW", "/command-center");
        routeMap.put("SUITE_SECURITY_POSTURE_QUERY", "/security");
        routeMap.put("SUITE_READINESS_QUERY", "/suite");
        routeMap.put("SUITE_COMMAND_CENTER_QUERY", "/command-center");
        routeMap.put("SUITE_NOTIFICATION_CENTER_QUERY", "/notifications");
        routeMap.put("SUITE_NOTIFICATION_MARK_READ", "/notifications");
        routeMap.put("SUITE_NOTIFICATION_MARK_ALL_READ", "/notifications");
        routeMap.put("SUITE_NOTIFICATION_ARCHIVE", "/notifications");
        routeMap.put("SUITE_NOTIFICATION_IGNORE", "/notifications");
        routeMap.put("SUITE_NOTIFICATION_SNOOZE", "/notifications");
        routeMap.put("SUITE_NOTIFICATION_ASSIGN", "/notifications");
        routeMap.put("SUITE_NOTIFICATION_RESTORE", "/notifications");
        routeMap.put("SUITE_NOTIFICATION_UNDO", "/notifications");
        routeMap.put("SUITE_NOTIFICATION_OPERATION_HISTORY_QUERY", "/notifications");
        return routeMap;
    }

    private static Map<String, Integer> buildNotificationSeverityWeightMap() {
        Map<String, Integer> severityWeightMap = new LinkedHashMap<>();
        severityWeightMap.put("CRITICAL", 4);
        severityWeightMap.put("HIGH", 3);
        severityWeightMap.put("MEDIUM", 2);
        severityWeightMap.put("LOW", 1);
        severityWeightMap.put("INFO", 0);
        return severityWeightMap;
    }

    private record WorkflowOperationContext(
            String safeIp,
            List<String> notificationIds,
            LocalDateTime executedAt,
            String operationId,
            Long sessionId
    ) {
    }

    private record NotificationDraft(
            String channel,
            String severity,
            String title,
            String message,
            String routePath,
            String actionCode,
            String productCode,
            LocalDateTime createdAt
    ) {
    }
}
