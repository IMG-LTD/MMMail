package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.dto.PatchV21WorkspaceTaskRequest;
import com.mmmail.server.model.dto.UpdateV21CollaborationTaskRequest;
import com.mmmail.server.model.vo.V21CollaborationActivityVo;
import com.mmmail.server.model.vo.V21CollaborationProjectVo;
import com.mmmail.server.model.vo.V21CollaborationTaskVo;
import com.mmmail.server.model.vo.V21WorkspaceActivityItemVo;
import com.mmmail.server.model.vo.V21WorkspaceSummaryProductVo;
import com.mmmail.server.model.vo.V21WorkspaceSummaryVo;
import com.mmmail.server.model.vo.V21WorkspaceTaskVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class V21WorkspaceRuntimeBridgeService {

    private static final String TASK_SOURCE_COLLABORATION = "collaboration-task-";
    private static final String SYSTEM_STATUS_READY = "READY";
    private static final String ENTITLEMENT_STATE_COMMUNITY = "community";
    private static final String STATUS_DONE = "DONE";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String STATUS_OPEN = "OPEN";

    private final V21OpsRuntimeBridgeService opsRuntimeBridgeService;

    public V21WorkspaceRuntimeBridgeService(V21OpsRuntimeBridgeService opsRuntimeBridgeService) {
        this.opsRuntimeBridgeService = opsRuntimeBridgeService;
    }

    public V21WorkspaceSummaryVo summary(Long userId, HttpServletRequest request) {
        List<V21CollaborationProjectVo> projects = opsRuntimeBridgeService.listProjects(userId, null, request);
        List<V21CollaborationTaskVo> tasks = opsRuntimeBridgeService.listTasks(userId, null, request);
        List<V21CollaborationActivityVo> activity = opsRuntimeBridgeService.listActivity(userId, null, request);
        return new V21WorkspaceSummaryVo(
                productCards(projects, tasks, activity),
                activity.size(),
                SYSTEM_STATUS_READY
        );
    }

    public List<V21WorkspaceActivityItemVo> activity(Long userId, HttpServletRequest request) {
        return opsRuntimeBridgeService.listActivity(userId, null, request).stream()
                .map(this::toActivityItem)
                .toList();
    }

    public List<V21WorkspaceTaskVo> tasks(Long userId, HttpServletRequest request) {
        return opsRuntimeBridgeService.listTasks(userId, null, request).stream()
                .filter(task -> isNumericId(task.id()))
                .map(this::toWorkspaceTask)
                .toList();
    }

    public V21WorkspaceTaskVo patchTask(
            Long userId,
            String workspaceTaskId,
            PatchV21WorkspaceTaskRequest request,
            HttpServletRequest httpRequest
    ) {
        assertPatchHasField(request);
        String taskId = parseCollaborationTaskId(workspaceTaskId);
        UpdateV21CollaborationTaskRequest updateRequest = new UpdateV21CollaborationTaskRequest(
                null,
                request.title(),
                statusForCompleted(request.completed()),
                null,
                null
        );
        return toWorkspaceTask(opsRuntimeBridgeService.updateTask(userId, taskId, updateRequest, httpRequest));
    }

    private List<V21WorkspaceSummaryProductVo> productCards(
            List<V21CollaborationProjectVo> projects,
            List<V21CollaborationTaskVo> tasks,
            List<V21CollaborationActivityVo> activity
    ) {
        Map<String, Long> taskCounts = taskCountsByProduct(tasks);
        Map<String, LocalDateTime> latestActivity = latestActivityByProduct(activity);
        return Stream.concat(
                        projects.stream().map(V21CollaborationProjectVo::product),
                        tasks.stream().map(V21CollaborationTaskVo::product)
                )
                .map(this::normalizeProduct)
                .distinct()
                .sorted()
                .map(product -> toProductCard(product, taskCounts.getOrDefault(product, 0L), latestActivity.get(product)))
                .toList();
    }

    private Map<String, Long> taskCountsByProduct(List<V21CollaborationTaskVo> tasks) {
        Map<String, Long> counts = new LinkedHashMap<>();
        tasks.forEach(task -> counts.merge(normalizeProduct(task.product()), 1L, Long::sum));
        return counts;
    }

    private Map<String, LocalDateTime> latestActivityByProduct(List<V21CollaborationActivityVo> activity) {
        Map<String, LocalDateTime> latest = new LinkedHashMap<>();
        activity.forEach(item -> latest.merge(
                normalizeProduct(item.product()),
                item.occurredAt(),
                this::latestTime
        ));
        return latest;
    }

    private V21WorkspaceSummaryProductVo toProductCard(String product, long taskCount, LocalDateTime updatedAt) {
        return new V21WorkspaceSummaryProductVo(
                product.toLowerCase(Locale.ROOT),
                product,
                taskCount + " tasks",
                ENTITLEMENT_STATE_COMMUNITY,
                updatedAt
        );
    }

    private V21WorkspaceActivityItemVo toActivityItem(V21CollaborationActivityVo activity) {
        return new V21WorkspaceActivityItemVo(
                activity.id(),
                normalizeProduct(activity.product()),
                activity.title(),
                activity.occurredAt(),
                null
        );
    }

    private V21WorkspaceTaskVo toWorkspaceTask(V21CollaborationTaskVo task) {
        return new V21WorkspaceTaskVo(
                TASK_SOURCE_COLLABORATION + task.id(),
                task.title(),
                isCompleted(task.status()),
                task.dueAt(),
                normalizeProduct(task.product())
        );
    }

    private String parseCollaborationTaskId(String value) {
        if (!StringUtils.hasText(value) || !value.startsWith(TASK_SOURCE_COLLABORATION)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "workspace task id source is unsupported");
        }
        String taskId = value.substring(TASK_SOURCE_COLLABORATION.length());
        if (!isNumericId(taskId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "workspace task id is invalid");
        }
        return taskId;
    }

    private void assertPatchHasField(PatchV21WorkspaceTaskRequest request) {
        boolean hasField = request != null && (request.completed() != null || request.title() != null);
        if (!hasField) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "workspace task patch is empty");
        }
    }

    private String statusForCompleted(Boolean completed) {
        if (completed == null) {
            return null;
        }
        return completed ? STATUS_DONE : STATUS_OPEN;
    }

    private LocalDateTime latestTime(LocalDateTime current, LocalDateTime candidate) {
        return Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder()).compare(current, candidate) >= 0
                ? current
                : candidate;
    }

    private boolean isCompleted(String status) {
        return STATUS_DONE.equals(status) || STATUS_ARCHIVED.equals(status);
    }

    private boolean isNumericId(String value) {
        return StringUtils.hasText(value) && value.chars().allMatch(Character::isDigit);
    }

    private String normalizeProduct(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "WORKSPACE";
    }
}
