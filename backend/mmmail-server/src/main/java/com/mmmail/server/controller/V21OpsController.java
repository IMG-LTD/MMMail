package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.CreateV21CollaborationCommentRequest;
import com.mmmail.server.model.dto.CreateV21CollaborationProjectRequest;
import com.mmmail.server.model.dto.CreateV21CollaborationTaskRequest;
import com.mmmail.server.model.dto.UpdateV21CollaborationTaskRequest;
import com.mmmail.server.model.dto.V21NotificationPatchRequest;
import com.mmmail.server.model.dto.V21NotificationQuery;
import com.mmmail.server.model.vo.V21CollaborationActivityVo;
import com.mmmail.server.model.vo.V21CollaborationProjectVo;
import com.mmmail.server.model.vo.V21CollaborationTaskVo;
import com.mmmail.server.model.vo.V21CommandCenterCommandVo;
import com.mmmail.server.model.vo.V21NotificationSubscriptionVo;
import com.mmmail.server.model.vo.V21NotificationVo;
import com.mmmail.server.service.V21OpsRuntimeBridgeService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v2")
public class V21OpsController {

    private static final String UNSUPPORTED_SUBSCRIPTION_UPDATE = "v2 notification subscription update is not supported by current runtime bridge";
    private static final String UNSUPPORTED_PREMIUM = "v2 premium ops execution is not supported by current runtime bridge";

    private final V21OpsRuntimeBridgeService opsRuntimeBridgeService;

    public V21OpsController(V21OpsRuntimeBridgeService opsRuntimeBridgeService) {
        this.opsRuntimeBridgeService = opsRuntimeBridgeService;
    }

    @GetMapping("/collaboration/projects")
    public Result<List<V21CollaborationProjectVo>> projects(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request
    ) {
        return Result.success(opsRuntimeBridgeService.listProjects(SecurityUtils.currentUserId(), limit, request));
    }

    @PostMapping("/collaboration/projects")
    public Result<V21CollaborationProjectVo> createProject(
            @Valid @RequestBody CreateV21CollaborationProjectRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(opsRuntimeBridgeService.createProject(SecurityUtils.currentUserId(), request, httpRequest));
    }

    @GetMapping("/collaboration/projects/{id}")
    public Result<V21CollaborationProjectVo> project(@PathVariable String id, HttpServletRequest request) {
        return Result.success(opsRuntimeBridgeService.readProject(SecurityUtils.currentUserId(), id, request));
    }

    @GetMapping("/collaboration/tasks")
    public Result<List<V21CollaborationTaskVo>> tasks(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request
    ) {
        return Result.success(opsRuntimeBridgeService.listTasks(SecurityUtils.currentUserId(), limit, request));
    }

    @PostMapping("/collaboration/tasks")
    public Result<V21CollaborationTaskVo> createTask(
            @Valid @RequestBody CreateV21CollaborationTaskRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(opsRuntimeBridgeService.createTask(SecurityUtils.currentUserId(), request, httpRequest));
    }

    @PatchMapping("/collaboration/tasks/{id}")
    public Result<V21CollaborationTaskVo> updateTask(
            @PathVariable String id,
            @Valid @RequestBody UpdateV21CollaborationTaskRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(opsRuntimeBridgeService.updateTask(SecurityUtils.currentUserId(), id, request, httpRequest));
    }

    @PostMapping("/collaboration/tasks/{id}/comments")
    public Result<V21CollaborationActivityVo> createTaskComment(
            @PathVariable String id,
            @Valid @RequestBody CreateV21CollaborationCommentRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(opsRuntimeBridgeService.createTaskComment(SecurityUtils.currentUserId(), id, request, httpRequest));
    }

    @GetMapping("/collaboration/activity")
    public Result<List<V21CollaborationActivityVo>> activity(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request
    ) {
        return Result.success(opsRuntimeBridgeService.listActivity(SecurityUtils.currentUserId(), limit, request));
    }

    @GetMapping("/notifications")
    public Result<List<V21NotificationVo>> notifications(
            @Valid @ModelAttribute V21NotificationQuery query,
            HttpServletRequest request
    ) {
        V21OpsRuntimeBridgeService.NotificationQuery serviceQuery = new V21OpsRuntimeBridgeService.NotificationQuery(
                query.limit(),
                query.unreadOnly(),
                query.status(),
                query.includeSnoozed()
        );
        return Result.success(opsRuntimeBridgeService.listNotifications(SecurityUtils.currentUserId(), serviceQuery, request));
    }

    @PatchMapping("/notifications/{id}")
    public Result<V21NotificationVo> patchNotification(
            @PathVariable String id,
            @RequestBody V21NotificationPatchRequest patchRequest,
            HttpServletRequest request
    ) {
        V21OpsRuntimeBridgeService.RequestContext context = new V21OpsRuntimeBridgeService.RequestContext(
                SecurityUtils.currentUserId(),
                SecurityUtils.currentSessionId(),
                request
        );
        return Result.success(opsRuntimeBridgeService.patchNotification(
                new V21OpsRuntimeBridgeService.PatchNotificationCommand(context, id, patchRequest)
        ));
    }

    @GetMapping("/notifications/subscriptions")
    public Result<List<V21NotificationSubscriptionVo>> subscriptions(HttpServletRequest request) {
        return Result.success(opsRuntimeBridgeService.listSubscriptions(SecurityUtils.currentUserId(), request));
    }

    @PatchMapping("/notifications/subscriptions/{id}")
    public Result<Void> updateSubscription(@PathVariable String id) {
        return unsupported(UNSUPPORTED_SUBSCRIPTION_UPDATE);
    }

    @GetMapping("/command-center/commands")
    public Result<List<V21CommandCenterCommandVo>> commands(HttpServletRequest request) {
        return Result.success(opsRuntimeBridgeService.listCommands(SecurityUtils.currentUserId(), request));
    }

    @GetMapping("/command-center/commands/{id}")
    public Result<V21CommandCenterCommandVo> command(@PathVariable String id, HttpServletRequest request) {
        return Result.success(opsRuntimeBridgeService.readCommand(SecurityUtils.currentUserId(), id, request));
    }

    @PostMapping("/command-center/runs")
    public Result<Void> createRun() {
        return unsupported(UNSUPPORTED_PREMIUM);
    }

    @GetMapping("/command-center/runs/{id}")
    public Result<Void> readRun(@PathVariable String id) {
        return unsupported(UNSUPPORTED_PREMIUM);
    }

    @PostMapping("/command-center/runs/{id}/cancel")
    public Result<Void> cancelRun(@PathVariable String id) {
        return unsupported(UNSUPPORTED_PREMIUM);
    }

    @PostMapping("/command-center/runs/{id}/retry")
    public Result<Void> retryRun(@PathVariable String id) {
        return unsupported(UNSUPPORTED_PREMIUM);
    }

    @GetMapping("/command-center/workflows")
    public Result<Void> workflows() {
        return unsupported(UNSUPPORTED_PREMIUM);
    }

    @PostMapping("/command-center/workflows")
    public Result<Void> createWorkflow() {
        return unsupported(UNSUPPORTED_PREMIUM);
    }

    @GetMapping("/command-center/audit")
    public Result<Void> audit() {
        return unsupported(UNSUPPORTED_PREMIUM);
    }

    @GetMapping("/notifications/rules")
    public Result<Void> rules() {
        return unsupported(UNSUPPORTED_PREMIUM);
    }

    @PostMapping("/notifications/rules")
    public Result<Void> createRule() {
        return unsupported(UNSUPPORTED_PREMIUM);
    }

    @GetMapping("/notifications/templates")
    public Result<Void> templates() {
        return unsupported(UNSUPPORTED_PREMIUM);
    }

    @PostMapping("/notifications/send")
    public Result<Void> sendNotification() {
        return unsupported(UNSUPPORTED_PREMIUM);
    }

    @GetMapping("/notifications/analytics")
    public Result<Void> analytics() {
        return unsupported(UNSUPPORTED_PREMIUM);
    }

    private <T> Result<T> unsupported(String message) {
        opsRuntimeBridgeService.rejectUnsupported(message);
        return Result.success(null);
    }
}
