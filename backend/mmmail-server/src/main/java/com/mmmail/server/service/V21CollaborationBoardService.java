package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.V21CollaborationProjectMapper;
import com.mmmail.server.mapper.V21CollaborationTaskMapper;
import com.mmmail.server.model.dto.V21CollaborationTaskMoveRequest;
import com.mmmail.server.model.entity.V21CollaborationProject;
import com.mmmail.server.model.entity.V21CollaborationTask;
import com.mmmail.server.model.vo.V21CollaborationBoardColumnVo;
import com.mmmail.server.model.vo.V21CollaborationBoardVo;
import com.mmmail.server.model.vo.V21CollaborationTaskMoveVo;
import com.mmmail.server.model.vo.V21CollaborationTaskVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class V21CollaborationBoardService {

    private static final Set<String> TASK_COLUMNS = Set.of("OPEN", "IN_PROGRESS", "BLOCKED", "DONE", "ARCHIVED");
    private static final List<ColumnDefinition> COLUMN_DEFINITIONS = List.of(
            new ColumnDefinition("OPEN", "Open"),
            new ColumnDefinition("IN_PROGRESS", "In progress"),
            new ColumnDefinition("BLOCKED", "Blocked"),
            new ColumnDefinition("DONE", "Done"),
            new ColumnDefinition("ARCHIVED", "Archived")
    );

    private final V21CollaborationProjectMapper projectMapper;
    private final V21CollaborationTaskMapper taskMapper;
    private final V21CollaborationEventService eventService;

    public V21CollaborationBoardService(
            V21CollaborationProjectMapper projectMapper,
            V21CollaborationTaskMapper taskMapper,
            V21CollaborationEventService eventService
    ) {
        this.projectMapper = projectMapper;
        this.taskMapper = taskMapper;
        this.eventService = eventService;
    }

    public V21CollaborationBoardVo readBoard(Long userId, String projectId) {
        V21CollaborationProject project = requireProject(userId, parseId(projectId, "project id is invalid"));
        List<V21CollaborationTask> tasks = listProjectTasks(userId, project.getId());
        List<V21CollaborationBoardColumnVo> columns = COLUMN_DEFINITIONS.stream()
                .map(column -> toColumn(column, tasks))
                .toList();
        return new V21CollaborationBoardVo(String.valueOf(project.getId()), columns);
    }

    @Transactional
    public V21CollaborationTaskMoveVo moveTask(V21BoardMoveCommand command) {
        Long userId = command.context().userId();
        V21CollaborationTask task = requireTask(userId, parseId(command.taskId(), "task id is invalid"));
        String columnId = normalizeColumn(command.request());
        LocalDateTime now = LocalDateTime.now();
        List<V21CollaborationTask> orderedTasks = orderedColumnTasks(
                new MovePlacement(userId, task, new MoveTarget(columnId, command.request()))
        );
        applyColumnOrder(orderedTasks, columnId, now);
        eventService.recordTaskUpdated(userId, task, command.context().ipAddress(), now);
        return new V21CollaborationTaskMoveVo(
                String.valueOf(task.getId()),
                String.valueOf(task.getProjectId()),
                task.getBoardColumn(),
                task.getPosition()
        );
    }

    private V21CollaborationBoardColumnVo toColumn(ColumnDefinition column, List<V21CollaborationTask> tasks) {
        List<V21CollaborationTaskVo> columnTasks = tasks.stream()
                .filter(task -> column.id().equals(taskColumn(task)))
                .map(this::toTaskVo)
                .toList();
        return new V21CollaborationBoardColumnVo(column.id(), column.title(), columnTasks);
    }

    private List<V21CollaborationTask> orderedColumnTasks(MovePlacement placement) {
        List<V21CollaborationTask> tasks = new ArrayList<>(listColumnTasks(
                placement.userId(),
                placement.task().getProjectId(),
                placement.target().columnId()
        ));
        tasks.removeIf(columnTask -> columnTask.getId().equals(placement.task().getId()));
        tasks.add(insertionIndex(tasks, placement.target().request()), placement.task());
        return tasks;
    }

    private int insertionIndex(List<V21CollaborationTask> tasks, V21CollaborationTaskMoveRequest request) {
        if (StringUtils.hasText(request.beforeTaskId())) {
            return anchorIndex(tasks, request.beforeTaskId(), "before task id is invalid");
        }
        if (StringUtils.hasText(request.afterTaskId())) {
            return anchorIndex(tasks, request.afterTaskId(), "after task id is invalid") + 1;
        }
        return tasks.size();
    }

    private int anchorIndex(List<V21CollaborationTask> tasks, String taskId, String message) {
        Long parsedId = parseId(taskId, message);
        for (int index = 0; index < tasks.size(); index++) {
            if (tasks.get(index).getId().equals(parsedId)) {
                return index;
            }
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
    }

    private void applyColumnOrder(List<V21CollaborationTask> tasks, String columnId, LocalDateTime now) {
        for (int index = 0; index < tasks.size(); index++) {
            V21CollaborationTask task = tasks.get(index);
            task.setBoardColumn(columnId);
            task.setStatus(columnId);
            task.setPosition(V21CollaborationBoardRanks.atIndex(index));
            task.setUpdatedAt(now);
            taskMapper.updateById(task);
        }
    }

    private List<V21CollaborationTask> listProjectTasks(Long userId, Long projectId) {
        LambdaQueryWrapper<V21CollaborationTask> query = baseTaskQuery(userId, projectId)
                .orderByAsc(V21CollaborationTask::getBoardColumn)
                .orderByAsc(V21CollaborationTask::getPosition)
                .orderByAsc(V21CollaborationTask::getId);
        return taskMapper.selectList(query);
    }

    private List<V21CollaborationTask> listColumnTasks(Long userId, Long projectId, String columnId) {
        LambdaQueryWrapper<V21CollaborationTask> query = baseTaskQuery(userId, projectId)
                .eq(V21CollaborationTask::getBoardColumn, columnId)
                .orderByAsc(V21CollaborationTask::getPosition)
                .orderByAsc(V21CollaborationTask::getId);
        return taskMapper.selectList(query);
    }

    private LambdaQueryWrapper<V21CollaborationTask> baseTaskQuery(Long userId, Long projectId) {
        return new LambdaQueryWrapper<V21CollaborationTask>()
                .eq(V21CollaborationTask::getOwnerId, userId)
                .eq(V21CollaborationTask::getProjectId, projectId);
    }

    private V21CollaborationProject requireProject(Long userId, Long projectId) {
        V21CollaborationProject project = projectMapper.selectOne(new LambdaQueryWrapper<V21CollaborationProject>()
                .eq(V21CollaborationProject::getOwnerId, userId)
                .eq(V21CollaborationProject::getId, projectId));
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

    private String normalizeColumn(V21CollaborationTaskMoveRequest request) {
        String columnId = requireText(request.columnId(), "column id is required").toUpperCase(Locale.ROOT);
        if (!TASK_COLUMNS.contains(columnId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "v2 collaboration board column is invalid");
        }
        return columnId;
    }

    private V21CollaborationTaskVo toTaskVo(V21CollaborationTask task) {
        return new V21CollaborationTaskVo(
                String.valueOf(task.getId()),
                String.valueOf(task.getProjectId()),
                task.getTitle(),
                task.getProduct(),
                task.getStatus(),
                taskColumn(task),
                task.getPosition(),
                task.getAssigneeEmail(),
                task.getDueAt()
        );
    }

    private String taskColumn(V21CollaborationTask task) {
        return StringUtils.hasText(task.getBoardColumn()) ? task.getBoardColumn() : task.getStatus();
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

    public record V21BoardMoveCommand(
            BoardRequestContext context,
            String taskId,
            V21CollaborationTaskMoveRequest request
    ) {
    }

    public record BoardRequestContext(
            Long userId,
            String ipAddress
    ) {
    }

    private record ColumnDefinition(String id, String title) {
    }

    private record MovePlacement(
            Long userId,
            V21CollaborationTask task,
            MoveTarget target
    ) {
    }

    private record MoveTarget(
            String columnId,
            V21CollaborationTaskMoveRequest request
    ) {
    }
}
