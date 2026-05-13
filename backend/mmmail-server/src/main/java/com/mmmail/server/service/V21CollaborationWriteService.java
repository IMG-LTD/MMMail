package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.V21CollaborationCommentMapper;
import com.mmmail.server.mapper.V21CollaborationProjectMapper;
import com.mmmail.server.mapper.V21CollaborationTaskMapper;
import com.mmmail.server.model.dto.CreateV21CollaborationCommentRequest;
import com.mmmail.server.model.dto.CreateV21CollaborationProjectRequest;
import com.mmmail.server.model.dto.CreateV21CollaborationTaskRequest;
import com.mmmail.server.model.dto.UpdateV21CollaborationTaskRequest;
import com.mmmail.server.model.entity.V21CollaborationComment;
import com.mmmail.server.model.entity.V21CollaborationProject;
import com.mmmail.server.model.entity.V21CollaborationTask;
import com.mmmail.server.model.vo.V21CollaborationActivityVo;
import com.mmmail.server.model.vo.V21CollaborationProjectVo;
import com.mmmail.server.model.vo.V21CollaborationTaskVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class V21CollaborationWriteService {

    private static final int DEFAULT_LIMIT = 24;
    private static final int MAX_LIMIT = 100;
    private static final String DEFAULT_PRODUCT = "WORKSPACE";
    private static final String PROJECT_STATUS_ACTIVE = "ACTIVE";
    private static final Set<String> PROJECT_STATUSES = Set.of("ACTIVE", "ARCHIVED");
    private static final Set<String> TASK_STATUSES = Set.of("OPEN", "IN_PROGRESS", "BLOCKED", "DONE", "ARCHIVED");

    private final V21CollaborationProjectMapper projectMapper;
    private final V21CollaborationTaskMapper taskMapper;
    private final V21CollaborationCommentMapper commentMapper;
    private final V21CollaborationEventService eventService;

    public V21CollaborationWriteService(
            V21CollaborationProjectMapper projectMapper,
            V21CollaborationTaskMapper taskMapper,
            V21CollaborationCommentMapper commentMapper,
            V21CollaborationEventService eventService
    ) {
        this.projectMapper = projectMapper;
        this.taskMapper = taskMapper;
        this.commentMapper = commentMapper;
        this.eventService = eventService;
    }

    @Transactional
    public V21CollaborationProjectVo createProject(
            Long userId,
            CreateV21CollaborationProjectRequest request,
            String ipAddress
    ) {
        LocalDateTime now = LocalDateTime.now();
        String name = requireText(request.name(), "project name is required");
        String product = normalizeProduct(request.product());
        String status = normalizeStatus(request.status(), PROJECT_STATUS_ACTIVE, PROJECT_STATUSES);
        assertProjectNameAvailable(userId, name);
        V21CollaborationProject project = newProject(userId, name, product, status, now);
        projectMapper.insert(project);
        eventService.recordProjectCreated(userId, project, ipAddress, now);
        return toProjectVo(project, 0);
    }

    @Transactional
    public V21CollaborationTaskVo createTask(
            Long userId,
            CreateV21CollaborationTaskRequest request,
            String ipAddress
    ) {
        LocalDateTime now = LocalDateTime.now();
        V21CollaborationProject project = requireProject(userId, parseId(request.projectId(), "project id is invalid"));
        String title = requireText(request.title(), "task title is required");
        String status = normalizeStatus(request.status(), "OPEN", TASK_STATUSES);
        V21CollaborationTask task = newTask(userId, project, title, status, request, now);
        taskMapper.insert(task);
        eventService.recordTaskCreated(userId, task, ipAddress, now);
        return toTaskVo(task);
    }

    @Transactional
    public V21CollaborationTaskVo updateTask(
            Long userId,
            String taskId,
            UpdateV21CollaborationTaskRequest request,
            String ipAddress
    ) {
        assertMutablePatch(request);
        LocalDateTime now = LocalDateTime.now();
        V21CollaborationTask task = requireTask(userId, parseId(taskId, "task id is invalid"));
        applyTaskPatch(userId, task, request, now);
        taskMapper.updateById(task);
        eventService.recordTaskUpdated(userId, task, ipAddress, now);
        return toTaskVo(task);
    }

    @Transactional
    public V21CollaborationActivityVo createComment(
            Long userId,
            String taskId,
            CreateV21CollaborationCommentRequest request,
            String ipAddress
    ) {
        LocalDateTime now = LocalDateTime.now();
        V21CollaborationTask task = requireTask(userId, parseId(taskId, "task id is invalid"));
        String body = requireText(request.body(), "comment body is required");
        V21CollaborationComment comment = newComment(userId, task, body, now);
        commentMapper.insert(comment);
        eventService.recordCommentCreated(userId, task, comment, ipAddress, now);
        return commentActivity(task, comment);
    }

    public List<V21CollaborationProjectVo> listPersistedProjects(Long userId, Integer limit) {
        List<V21CollaborationProject> projects = projectMapper.selectList(new LambdaQueryWrapper<V21CollaborationProject>()
                .eq(V21CollaborationProject::getOwnerId, userId)
                .orderByDesc(V21CollaborationProject::getUpdatedAt)
                .orderByDesc(V21CollaborationProject::getId)
                .last("limit " + safeLimit(limit)));
        return projects.stream().map(project -> toProjectVo(project, taskCount(userId, project.getId()))).toList();
    }

    public V21CollaborationProjectVo readPersistedProject(Long userId, String projectId) {
        V21CollaborationProject project = findProject(userId, parseId(projectId, "project id is invalid"));
        return project == null ? null : toProjectVo(project, taskCount(userId, project.getId()));
    }

    public List<V21CollaborationTaskVo> listPersistedTasks(Long userId, Integer limit) {
        return taskMapper.selectList(new LambdaQueryWrapper<V21CollaborationTask>()
                        .eq(V21CollaborationTask::getOwnerId, userId)
                        .orderByDesc(V21CollaborationTask::getUpdatedAt)
                        .orderByDesc(V21CollaborationTask::getId)
                        .last("limit " + safeLimit(limit)))
                .stream()
                .map(this::toTaskVo)
                .toList();
    }

    public List<V21CollaborationActivityVo> listPersistedActivity(Long userId, Integer limit) {
        List<V21CollaborationActivityVo> activities = new ArrayList<>();
        activities.addAll(projectActivities(userId, limit));
        activities.addAll(taskActivities(userId, limit));
        activities.addAll(commentActivities(userId, limit));
        return activities.stream()
                .sorted(Comparator.comparing(V21CollaborationActivityVo::occurredAt).reversed())
                .limit(safeLimit(limit))
                .toList();
    }

    private V21CollaborationProject newProject(
            Long userId,
            String name,
            String product,
            String status,
            LocalDateTime now
    ) {
        V21CollaborationProject project = new V21CollaborationProject();
        project.setOwnerId(userId);
        project.setName(name);
        project.setProduct(product);
        project.setStatus(status);
        project.setCreatedAt(now);
        project.setUpdatedAt(now);
        project.setDeleted(0);
        return project;
    }

    private V21CollaborationTask newTask(
            Long userId,
            V21CollaborationProject project,
            String title,
            String status,
            CreateV21CollaborationTaskRequest request,
            LocalDateTime now
    ) {
        V21CollaborationTask task = new V21CollaborationTask();
        task.setProjectId(project.getId());
        task.setOwnerId(userId);
        task.setTitle(title);
        task.setProduct(project.getProduct());
        task.setStatus(status);
        task.setAssigneeEmail(normalizeNullable(request.assigneeEmail()));
        task.setDueAt(request.dueAt());
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setDeleted(0);
        return task;
    }

    private V21CollaborationComment newComment(
            Long userId,
            V21CollaborationTask task,
            String body,
            LocalDateTime now
    ) {
        V21CollaborationComment comment = new V21CollaborationComment();
        comment.setTaskId(task.getId());
        comment.setProjectId(task.getProjectId());
        comment.setOwnerId(task.getOwnerId());
        comment.setAuthorUserId(userId);
        comment.setBody(body);
        comment.setCreatedAt(now);
        comment.setDeleted(0);
        return comment;
    }

    private void applyTaskPatch(Long userId, V21CollaborationTask task, UpdateV21CollaborationTaskRequest request, LocalDateTime now) {
        if (request.projectId() != null) {
            V21CollaborationProject project = requireProject(userId, parseId(request.projectId(), "project id is invalid"));
            task.setProjectId(project.getId());
            task.setProduct(project.getProduct());
        }
        if (request.title() != null) {
            task.setTitle(requireText(request.title(), "task title is required"));
        }
        if (request.status() != null) {
            task.setStatus(normalizeStatus(request.status(), task.getStatus(), TASK_STATUSES));
        }
        if (request.assigneeEmail() != null) {
            task.setAssigneeEmail(normalizeNullable(request.assigneeEmail()));
        }
        if (request.dueAt() != null) {
            task.setDueAt(request.dueAt());
        }
        task.setUpdatedAt(now);
    }

    private V21CollaborationProject requireProject(Long userId, Long projectId) {
        V21CollaborationProject project = findProject(userId, projectId);
        if (project == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "v2 collaboration project does not exist");
        }
        return project;
    }

    private V21CollaborationTask requireTask(Long userId, Long taskId) {
        V21CollaborationTask task = taskMapper.selectOne(new LambdaQueryWrapper<V21CollaborationTask>()
                .eq(V21CollaborationTask::getOwnerId, userId)
                .eq(V21CollaborationTask::getId, taskId));
        if (task == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "v2 collaboration task does not exist");
        }
        return task;
    }

    private V21CollaborationProject findProject(Long userId, Long projectId) {
        return projectMapper.selectOne(new LambdaQueryWrapper<V21CollaborationProject>()
                .eq(V21CollaborationProject::getOwnerId, userId)
                .eq(V21CollaborationProject::getId, projectId));
    }

    private void assertProjectNameAvailable(Long userId, String name) {
        Long count = projectMapper.selectCount(new LambdaQueryWrapper<V21CollaborationProject>()
                .eq(V21CollaborationProject::getOwnerId, userId)
                .eq(V21CollaborationProject::getName, name));
        if (count > 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "v2 collaboration project name already exists");
        }
    }

    private void assertMutablePatch(UpdateV21CollaborationTaskRequest request) {
        boolean hasField = request != null && (request.projectId() != null
                || request.title() != null
                || request.status() != null
                || request.assigneeEmail() != null
                || request.dueAt() != null);
        if (!hasField) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "v2 collaboration task patch is empty");
        }
    }

    private int taskCount(Long userId, Long projectId) {
        Long count = taskMapper.selectCount(new LambdaQueryWrapper<V21CollaborationTask>()
                .eq(V21CollaborationTask::getOwnerId, userId)
                .eq(V21CollaborationTask::getProjectId, projectId));
        return Math.toIntExact(count);
    }

    private List<V21CollaborationActivityVo> projectActivities(Long userId, Integer limit) {
        return projectMapper.selectList(new LambdaQueryWrapper<V21CollaborationProject>()
                        .eq(V21CollaborationProject::getOwnerId, userId)
                        .orderByDesc(V21CollaborationProject::getUpdatedAt)
                        .last("limit " + safeLimit(limit)))
                .stream()
                .map(project -> new V21CollaborationActivityVo("project-" + project.getId(), project.getName(), project.getProduct(), project.getUpdatedAt()))
                .toList();
    }

    private List<V21CollaborationActivityVo> taskActivities(Long userId, Integer limit) {
        return taskMapper.selectList(new LambdaQueryWrapper<V21CollaborationTask>()
                        .eq(V21CollaborationTask::getOwnerId, userId)
                        .orderByDesc(V21CollaborationTask::getUpdatedAt)
                        .last("limit " + safeLimit(limit)))
                .stream()
                .map(task -> new V21CollaborationActivityVo("task-" + task.getId(), task.getTitle(), task.getProduct(), task.getUpdatedAt()))
                .toList();
    }

    private List<V21CollaborationActivityVo> commentActivities(Long userId, Integer limit) {
        return commentMapper.selectList(new LambdaQueryWrapper<V21CollaborationComment>()
                        .eq(V21CollaborationComment::getOwnerId, userId)
                        .orderByDesc(V21CollaborationComment::getCreatedAt)
                        .last("limit " + safeLimit(limit)))
                .stream()
                .map(this::commentActivity)
                .toList();
    }

    private V21CollaborationActivityVo commentActivity(V21CollaborationComment comment) {
        V21CollaborationTask task = taskMapper.selectById(comment.getTaskId());
        String product = task == null ? DEFAULT_PRODUCT : task.getProduct();
        return new V21CollaborationActivityVo("comment-" + comment.getId(), "Comment added", product, comment.getCreatedAt());
    }

    private V21CollaborationActivityVo commentActivity(V21CollaborationTask task, V21CollaborationComment comment) {
        return new V21CollaborationActivityVo("comment-" + comment.getId(), "Comment added", task.getProduct(), comment.getCreatedAt());
    }

    private V21CollaborationProjectVo toProjectVo(V21CollaborationProject project, int taskCount) {
        return new V21CollaborationProjectVo(
                String.valueOf(project.getId()),
                project.getName(),
                project.getProduct(),
                project.getStatus(),
                taskCount,
                project.getUpdatedAt()
        );
    }

    private V21CollaborationTaskVo toTaskVo(V21CollaborationTask task) {
        return new V21CollaborationTaskVo(
                String.valueOf(task.getId()),
                String.valueOf(task.getProjectId()),
                task.getTitle(),
                task.getProduct(),
                task.getStatus(),
                task.getAssigneeEmail(),
                task.getDueAt()
        );
    }

    private String normalizeStatus(String value, String defaultValue, Set<String> allowedValues) {
        String status = StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
        if (!allowedValues.contains(status)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "v2 collaboration status is invalid");
        }
        return status;
    }

    private String normalizeProduct(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_PRODUCT;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return value.trim();
    }

    private Long parseId(String value, String message) {
        try {
            return Long.parseLong(requireText(value, message));
        } catch (NumberFormatException exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
    }

    private int safeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

}
