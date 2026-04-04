package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.ApproveSuiteGovernanceChangeRequestRequest;
import com.mmmail.server.model.dto.AssignSuiteNotificationsRequest;
import com.mmmail.server.model.dto.BatchExecuteSuiteRemediationActionsRequest;
import com.mmmail.server.model.dto.BatchReviewSuiteGovernanceChangeRequestsRequest;
import com.mmmail.server.model.dto.ChangeSuitePlanRequest;
import com.mmmail.server.model.dto.ExecuteSuiteRemediationActionRequest;
import com.mmmail.server.model.dto.CreateSuiteGovernanceChangeRequestRequest;
import com.mmmail.server.model.dto.DeleteSuiteWebPushSubscriptionRequest;
import com.mmmail.server.model.dto.MarkSuiteNotificationsReadRequest;
import com.mmmail.server.model.dto.RegisterSuiteWebPushSubscriptionRequest;
import com.mmmail.server.model.dto.ReviewSuiteGovernanceChangeRequestRequest;
import com.mmmail.server.model.dto.RollbackSuiteGovernanceChangeRequestRequest;
import com.mmmail.server.model.dto.SnoozeSuiteNotificationsRequest;
import com.mmmail.server.model.dto.UndoSuiteNotificationWorkflowRequest;
import com.mmmail.server.model.vo.SuiteBatchGovernanceReviewResultVo;
import com.mmmail.server.model.vo.SuiteBatchRemediationExecutionResultVo;
import com.mmmail.server.model.vo.SuiteCollaborationCenterVo;
import com.mmmail.server.model.vo.SuiteCollaborationSyncVo;
import com.mmmail.server.model.vo.SuiteCommandCenterVo;
import com.mmmail.server.model.vo.SuiteCommandFeedVo;
import com.mmmail.server.model.vo.SuiteGovernanceChangeRequestVo;
import com.mmmail.server.model.vo.SuiteGovernanceOverviewVo;
import com.mmmail.server.model.vo.SuiteGovernancePolicyTemplateVo;
import com.mmmail.server.model.vo.SuiteNotificationCenterVo;
import com.mmmail.server.model.vo.SuiteNotificationMarkReadResultVo;
import com.mmmail.server.model.vo.SuiteNotificationOperationHistoryVo;
import com.mmmail.server.model.vo.SuiteNotificationSyncVo;
import com.mmmail.server.model.vo.SuiteNotificationWorkflowResultVo;
import com.mmmail.server.model.vo.SuitePlanVo;
import com.mmmail.server.model.vo.SuiteProductStatusVo;
import com.mmmail.server.model.vo.SuiteReadinessReportVo;
import com.mmmail.server.model.vo.SuiteRemediationExecutionResultVo;
import com.mmmail.server.model.vo.SuiteSecurityPostureVo;
import com.mmmail.server.model.vo.SuiteSubscriptionVo;
import com.mmmail.server.model.vo.SuiteUnifiedSearchResultVo;
import com.mmmail.server.model.vo.SuiteWebPushStatusVo;
import com.mmmail.server.model.vo.SuiteWebPushSubscriptionVo;
import com.mmmail.server.service.SuiteCollaborationService;
import com.mmmail.server.service.SuiteCommandCenterService;
import com.mmmail.server.service.SuiteInsightService;
import com.mmmail.server.service.SuiteNotificationSyncService;
import com.mmmail.server.service.SuiteOrgScopeService;
import com.mmmail.server.service.SuiteService;
import com.mmmail.server.service.WebPushService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/suite")
public class SuiteController {

    private final SuiteService suiteService;
    private final SuiteInsightService suiteInsightService;
    private final SuiteCollaborationService suiteCollaborationService;
    private final SuiteCommandCenterService suiteCommandCenterService;
    private final SuiteNotificationSyncService suiteNotificationSyncService;
    private final SuiteOrgScopeService suiteOrgScopeService;
    private final WebPushService webPushService;

    public SuiteController(
            SuiteService suiteService,
            SuiteInsightService suiteInsightService,
            SuiteCollaborationService suiteCollaborationService,
            SuiteCommandCenterService suiteCommandCenterService,
            SuiteNotificationSyncService suiteNotificationSyncService,
            SuiteOrgScopeService suiteOrgScopeService,
            WebPushService webPushService
    ) {
        this.suiteService = suiteService;
        this.suiteInsightService = suiteInsightService;
        this.suiteCollaborationService = suiteCollaborationService;
        this.suiteCommandCenterService = suiteCommandCenterService;
        this.suiteNotificationSyncService = suiteNotificationSyncService;
        this.suiteOrgScopeService = suiteOrgScopeService;
        this.webPushService = webPushService;
    }

    @GetMapping("/plans")
    public Result<List<SuitePlanVo>> listPlans(HttpServletRequest httpRequest) {
        return Result.success(suiteService.listPlans(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @GetMapping("/subscription")
    public Result<SuiteSubscriptionVo> getSubscription(HttpServletRequest httpRequest) {
        return Result.success(suiteService.getSubscription(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @GetMapping("/products")
    public Result<List<SuiteProductStatusVo>> listProducts(HttpServletRequest httpRequest) {
        Long userId = SecurityUtils.currentUserId();
        Set<String> visibleProductCodes = resolveVisibleProductCodes(httpRequest, userId);
        List<SuiteProductStatusVo> products = suiteService.listProducts(userId, httpRequest.getRemoteAddr()).stream()
                .filter(item -> isProductVisible(visibleProductCodes, item.code()))
                .toList();
        return Result.success(products);
    }

    @GetMapping("/readiness")
    public Result<SuiteReadinessReportVo> getReadiness(HttpServletRequest httpRequest) {
        Long userId = SecurityUtils.currentUserId();
        return Result.success(suiteInsightService.getReadinessReport(
                userId,
                httpRequest.getRemoteAddr(),
                resolveVisibleProductCodes(httpRequest, userId)
        ));
    }

    @GetMapping("/security-posture")
    public Result<SuiteSecurityPostureVo> getSecurityPosture(HttpServletRequest httpRequest) {
        Long userId = SecurityUtils.currentUserId();
        return Result.success(suiteInsightService.getSecurityPosture(
                userId,
                httpRequest.getRemoteAddr(),
                resolveVisibleProductCodes(httpRequest, userId)
        ));
    }

    @GetMapping("/command-center")
    public Result<SuiteCommandCenterVo> getCommandCenter(HttpServletRequest httpRequest) {
        Long userId = SecurityUtils.currentUserId();
        return Result.success(suiteCommandCenterService.getCommandCenter(
                userId,
                httpRequest.getRemoteAddr(),
                resolveVisibleProductCodes(httpRequest, userId)
        ));
    }

    @GetMapping("/collaboration-center")
    public Result<SuiteCollaborationCenterVo> getCollaborationCenter(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.currentUserId();
        return Result.success(suiteCollaborationService.getCenter(
                userId,
                limit,
                httpRequest.getRemoteAddr(),
                resolveVisibleProductCodes(httpRequest, userId)
        ));
    }

    @GetMapping("/collaboration-center/sync")
    public Result<SuiteCollaborationSyncVo> getCollaborationSync(
            @RequestParam(required = false) Long afterEventId,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.currentUserId();
        return Result.success(suiteCollaborationService.getSync(
                userId,
                afterEventId,
                limit,
                resolveVisibleProductCodes(httpRequest, userId)
        ));
    }

    @GetMapping(value = "/collaboration-center/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCollaborationSync(
            @RequestParam(required = false) Long afterEventId,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.currentUserId();
        return suiteCollaborationService.openStream(
                userId,
                afterEventId,
                resolveVisibleProductCodes(httpRequest, userId)
        );
    }

    @GetMapping("/command-feed")
    public Result<SuiteCommandFeedVo> getCommandFeed(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.currentUserId();
        return Result.success(suiteCommandCenterService.getCommandFeed(
                userId,
                limit,
                httpRequest.getRemoteAddr(),
                resolveVisibleProductCodes(httpRequest, userId)
        ));
    }

    @GetMapping("/notification-center")
    public Result<SuiteNotificationCenterVo> getNotificationCenter(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean includeSnoozed,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.currentUserId();
        return Result.success(suiteCommandCenterService.getNotificationCenter(
                userId,
                limit,
                unreadOnly,
                status,
                includeSnoozed,
                httpRequest.getRemoteAddr(),
                resolveVisibleProductCodes(httpRequest, userId)
        ));
    }

    @GetMapping("/web-push")
    public Result<SuiteWebPushStatusVo> getWebPushStatus(HttpServletRequest httpRequest) {
        return Result.success(webPushService.getStatus(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/web-push/subscriptions")
    public Result<SuiteWebPushSubscriptionVo> registerWebPushSubscription(
            @Valid @RequestBody RegisterSuiteWebPushSubscriptionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(webPushService.registerSubscription(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/web-push/subscriptions")
    public Result<Boolean> deleteWebPushSubscription(
            @Valid @RequestBody DeleteSuiteWebPushSubscriptionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(webPushService.deleteSubscription(
                SecurityUtils.currentUserId(),
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/notification-center/operations")
    public Result<SuiteNotificationOperationHistoryVo> getNotificationOperationHistory(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteCommandCenterService.getNotificationOperationHistory(
                SecurityUtils.currentUserId(),
                limit,
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/notification-center/sync")
    public Result<SuiteNotificationSyncVo> getNotificationSync(
            @RequestParam(required = false) Long afterEventId,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(suiteNotificationSyncService.getNotificationSync(
                SecurityUtils.currentUserId(),
                afterEventId,
                limit
        ));
    }

    @GetMapping(value = "/notification-center/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotificationSync(@RequestParam(required = false) Long afterEventId) {
        return suiteNotificationSyncService.openStream(SecurityUtils.currentUserId(), afterEventId);
    }

    @PostMapping("/notification-center/mark-read")
    public Result<SuiteNotificationMarkReadResultVo> markNotificationsRead(
            @Valid @RequestBody MarkSuiteNotificationsReadRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteCommandCenterService.markNotificationsRead(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                request.notificationIds(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/notification-center/mark-all-read")
    public Result<SuiteNotificationMarkReadResultVo> markAllNotificationsRead(HttpServletRequest httpRequest) {
        return Result.success(suiteCommandCenterService.markAllNotificationsRead(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/notification-center/archive")
    public Result<SuiteNotificationWorkflowResultVo> archiveNotifications(
            @Valid @RequestBody MarkSuiteNotificationsReadRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteCommandCenterService.archiveNotifications(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                request.notificationIds(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/notification-center/ignore")
    public Result<SuiteNotificationWorkflowResultVo> ignoreNotifications(
            @Valid @RequestBody MarkSuiteNotificationsReadRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteCommandCenterService.ignoreNotifications(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                request.notificationIds(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/notification-center/restore")
    public Result<SuiteNotificationWorkflowResultVo> restoreNotifications(
            @Valid @RequestBody MarkSuiteNotificationsReadRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteCommandCenterService.restoreNotifications(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                request.notificationIds(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/notification-center/snooze")
    public Result<SuiteNotificationWorkflowResultVo> snoozeNotifications(
            @Valid @RequestBody SnoozeSuiteNotificationsRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteCommandCenterService.snoozeNotifications(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                request.notificationIds(),
                request.snoozedUntil(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/notification-center/assign")
    public Result<SuiteNotificationWorkflowResultVo> assignNotifications(
            @Valid @RequestBody AssignSuiteNotificationsRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteCommandCenterService.assignNotifications(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                request.notificationIds(),
                request.assigneeUserId(),
                request.assigneeDisplayName(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/notification-center/undo")
    public Result<SuiteNotificationWorkflowResultVo> undoNotificationWorkflow(
            @Valid @RequestBody UndoSuiteNotificationWorkflowRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteCommandCenterService.undoNotificationWorkflow(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                request.operationId(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/unified-search")
    public Result<SuiteUnifiedSearchResultVo> unifiedSearch(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.currentUserId();
        return Result.success(suiteInsightService.unifiedSearch(
                userId,
                keyword,
                limit,
                httpRequest.getRemoteAddr(),
                resolveVisibleProductCodes(httpRequest, userId)
        ));
    }

    @GetMapping("/governance/overview")
    public Result<SuiteGovernanceOverviewVo> getGovernanceOverview(HttpServletRequest httpRequest) {
        return Result.success(suiteInsightService.getGovernanceOverview(
                SecurityUtils.currentUserId(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/governance/templates")
    public Result<List<SuiteGovernancePolicyTemplateVo>> listGovernanceTemplates(HttpServletRequest httpRequest) {
        return Result.success(suiteInsightService.listGovernanceTemplates(
                SecurityUtils.currentUserId(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/governance/change-requests")
    public Result<List<SuiteGovernanceChangeRequestVo>> listGovernanceChangeRequests(HttpServletRequest httpRequest) {
        return Result.success(suiteInsightService.listGovernanceChangeRequests(
                SecurityUtils.currentUserId(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/governance/change-requests")
    public Result<SuiteGovernanceChangeRequestVo> createGovernanceChangeRequest(
            @Valid @RequestBody CreateSuiteGovernanceChangeRequestRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteInsightService.createGovernanceChangeRequest(
                SecurityUtils.currentUserId(),
                request.templateCode(),
                request.reason(),
                request.orgId(),
                request.secondReviewerUserId(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/governance/change-requests/approve")
    public Result<SuiteGovernanceChangeRequestVo> approveGovernanceChangeRequest(
            @Valid @RequestBody ApproveSuiteGovernanceChangeRequestRequest request,
            HttpServletRequest httpRequest
    ) {
        var principal = SecurityUtils.currentPrincipal();
        return Result.success(suiteInsightService.approveGovernanceChangeRequest(
                principal.userId(),
                principal.sessionId(),
                request.requestId(),
                request.approvalNote(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/governance/change-requests/review")
    public Result<SuiteGovernanceChangeRequestVo> reviewGovernanceChangeRequest(
            @Valid @RequestBody ReviewSuiteGovernanceChangeRequestRequest request,
            HttpServletRequest httpRequest
    ) {
        var principal = SecurityUtils.currentPrincipal();
        return Result.success(suiteInsightService.reviewGovernanceChangeRequest(
                principal.userId(),
                principal.sessionId(),
                request.requestId(),
                request.decision(),
                request.reviewNote(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/governance/change-requests/rollback")
    public Result<SuiteGovernanceChangeRequestVo> rollbackGovernanceChangeRequest(
            @Valid @RequestBody RollbackSuiteGovernanceChangeRequestRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteInsightService.rollbackGovernanceChangeRequest(
                SecurityUtils.currentUserId(),
                request.requestId(),
                request.rollbackReason(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/remediation-actions/execute")
    public Result<SuiteRemediationExecutionResultVo> executeRemediationAction(
            @Valid @RequestBody ExecuteSuiteRemediationActionRequest request,
            HttpServletRequest httpRequest
    ) {
        var principal = SecurityUtils.currentPrincipal();
        suiteOrgScopeService.assertRemediationActionAllowed(
                suiteOrgScopeService.resolveContext(httpRequest, principal.userId()),
                request.actionCode()
        );
        return Result.success(suiteInsightService.executeRemediationAction(
                principal.userId(),
                request.actionCode(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/remediation-actions/batch-execute")
    public Result<SuiteBatchRemediationExecutionResultVo> batchExecuteRemediationActions(
            @Valid @RequestBody BatchExecuteSuiteRemediationActionsRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.currentUserId();
        suiteOrgScopeService.assertRemediationActionsAllowed(
                suiteOrgScopeService.resolveContext(httpRequest, userId),
                request.actionCodes()
        );
        return Result.success(suiteCommandCenterService.batchExecuteRemediationActions(
                userId,
                request.actionCodes(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/governance/change-requests/batch-review")
    public Result<SuiteBatchGovernanceReviewResultVo> batchReviewGovernanceChangeRequests(
            @Valid @RequestBody BatchReviewSuiteGovernanceChangeRequestsRequest request,
            HttpServletRequest httpRequest
    ) {
        var principal = SecurityUtils.currentPrincipal();
        return Result.success(suiteCommandCenterService.batchReviewGovernanceRequests(
                principal.userId(),
                principal.sessionId(),
                request.requestIds(),
                request.decision(),
                request.reviewNote(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/subscription/change")
    public Result<SuiteSubscriptionVo> changePlan(
            @Valid @RequestBody ChangeSuitePlanRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(suiteService.changePlan(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    private Set<String> resolveVisibleProductCodes(HttpServletRequest request, Long userId) {
        return suiteOrgScopeService.resolveContext(request, userId).visibleProductCodes();
    }

    private boolean isProductVisible(Set<String> visibleProductCodes, String productCode) {
        return suiteOrgScopeService.isProductVisible(visibleProductCodes, productCode);
    }
}
