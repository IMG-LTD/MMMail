package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.orggovernance.scope.OrgScopeAccessDecision;
import com.mmmail.server.model.dto.V21NotificationPatchRequest;
import com.mmmail.server.model.vo.SuiteCollaborationCenterVo;
import com.mmmail.server.model.vo.SuiteCollaborationEventVo;
import com.mmmail.server.model.vo.SuiteCommandCenterVo;
import com.mmmail.server.model.vo.SuiteCommandItemVo;
import com.mmmail.server.model.vo.SuiteNotificationCenterVo;
import com.mmmail.server.model.vo.SuiteNotificationItemVo;
import com.mmmail.server.model.vo.SuiteRemediationActionVo;
import com.mmmail.server.model.vo.SuiteWebPushStatusVo;
import com.mmmail.server.model.vo.V21CollaborationActivityVo;
import com.mmmail.server.model.vo.V21CollaborationProjectVo;
import com.mmmail.server.model.vo.V21CollaborationTaskVo;
import com.mmmail.server.model.vo.V21CommandCenterCommandVo;
import com.mmmail.server.model.vo.V21NotificationSubscriptionVo;
import com.mmmail.server.model.vo.V21NotificationVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class V21OpsRuntimeBridgeService {

    private static final int NOTIFICATION_RELOAD_LIMIT = 60;
    private static final String NOTIFICATION_STATUS_READ = "READ";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_DONE = "DONE";
    private static final String DEFAULT_COMMAND_SEED = "command";

    private final SuiteCollaborationService collaborationService;
    private final SuiteCommandCenterService commandCenterService;
    private final SuiteOrgScopeService orgScopeService;
    private final WebPushService webPushService;

    public V21OpsRuntimeBridgeService(
            SuiteCollaborationService collaborationService,
            SuiteCommandCenterService commandCenterService,
            SuiteOrgScopeService orgScopeService,
            WebPushService webPushService
    ) {
        this.collaborationService = collaborationService;
        this.commandCenterService = commandCenterService;
        this.orgScopeService = orgScopeService;
        this.webPushService = webPushService;
    }

    public List<V21CollaborationProjectVo> listProjects(Long userId, Integer limit, HttpServletRequest request) {
        return buildProjects(collaborationCenter(userId, limit, request).items());
    }

    public V21CollaborationProjectVo readProject(Long userId, String projectId, HttpServletRequest request) {
        String normalizedId = normalizeRequired(projectId, "project id is required");
        return listProjects(userId, null, request).stream()
                .filter(project -> project.id().equals(normalizedId))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.INVALID_ARGUMENT, "v2 collaboration project does not exist"));
    }

    public List<V21CollaborationTaskVo> listTasks(Long userId, Integer limit, HttpServletRequest request) {
        return collaborationCenter(userId, limit, request).items().stream()
                .map(this::toTask)
                .toList();
    }

    public List<V21CollaborationActivityVo> listActivity(Long userId, Integer limit, HttpServletRequest request) {
        return collaborationCenter(userId, limit, request).items().stream()
                .map(this::toActivity)
                .toList();
    }

    public List<V21NotificationVo> listNotifications(
            Long userId,
            NotificationQuery query,
            HttpServletRequest request
    ) {
        return notificationCenter(userId, query, request).items().stream()
                .map(this::toNotification)
                .toList();
    }

    public V21NotificationVo patchNotification(PatchNotificationCommand command) {
        String notificationId = normalizeRequired(command.notificationId(), "notification id is required");
        assertReadStatus(command.patchRequest());
        commandCenterService.markNotificationsRead(
                command.context().userId(),
                command.context().sessionId(),
                List.of(notificationId),
                command.context().httpRequest().getRemoteAddr()
        );
        return reloadNotification(command.context().userId(), notificationId, command.context().httpRequest());
    }

    public List<V21NotificationSubscriptionVo> listSubscriptions(Long userId, HttpServletRequest request) {
        SuiteWebPushStatusVo status = webPushService.getStatus(userId, request.getRemoteAddr());
        return List.of(new V21NotificationSubscriptionVo(
                "web-push-mail-inbox",
                "MAIL",
                "WEB_PUSH",
                status.enabled()
        ));
    }

    public List<V21CommandCenterCommandVo> listCommands(Long userId, HttpServletRequest request) {
        SuiteCommandCenterVo center = commandCenter(userId, request);
        return Stream.concat(
                        center.quickRoutes().stream().map(this::toRouteCommand),
                        center.recommendedActions().stream().map(this::toActionCommand)
                )
                .collect(LinkedHashMap::new, this::putCommandIfAbsent, Map::putAll)
                .values()
                .stream()
                .toList();
    }

    public V21CommandCenterCommandVo readCommand(Long userId, String commandId, HttpServletRequest request) {
        String normalizedId = normalizeRequired(commandId, "command id is required");
        return listCommands(userId, request).stream()
                .filter(command -> command.id().equals(normalizedId))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.INVALID_ARGUMENT, "v2 command does not exist"));
    }

    public void rejectUnsupported(String message) {
        throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
    }

    private SuiteCollaborationCenterVo collaborationCenter(Long userId, Integer limit, HttpServletRequest request) {
        OrgScopeAccessDecision scope = orgScopeService.resolveContext(request, userId);
        return collaborationService.getCenter(userId, limit, request.getRemoteAddr(), scope.visibleProductCodes());
    }

    private SuiteNotificationCenterVo notificationCenter(
            Long userId,
            NotificationQuery query,
            HttpServletRequest request
    ) {
        OrgScopeAccessDecision scope = orgScopeService.resolveContext(request, userId);
        return commandCenterService.getNotificationCenter(
                userId,
                query.limit(),
                query.unreadOnly(),
                query.status(),
                query.includeSnoozed(),
                request.getRemoteAddr(),
                scope.visibleProductCodes()
        );
    }

    private SuiteCommandCenterVo commandCenter(Long userId, HttpServletRequest request) {
        OrgScopeAccessDecision scope = orgScopeService.resolveContext(request, userId);
        return commandCenterService.getCommandCenter(userId, request.getRemoteAddr(), scope.visibleProductCodes());
    }

    private List<V21CollaborationProjectVo> buildProjects(List<SuiteCollaborationEventVo> events) {
        Map<String, List<SuiteCollaborationEventVo>> byProduct = events.stream()
                .collect(Collectors.groupingBy(
                        event -> normalizeProduct(event.productCode()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        return byProduct.entrySet().stream()
                .map(entry -> toProject(entry.getKey(), entry.getValue()))
                .toList();
    }

    private V21CollaborationProjectVo toProject(String product, List<SuiteCollaborationEventVo> events) {
        LocalDateTime updatedAt = events.stream()
                .map(SuiteCollaborationEventVo::createdAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new V21CollaborationProjectVo(
                projectId(product),
                productName(product),
                product,
                STATUS_ACTIVE,
                events.size(),
                updatedAt
        );
    }

    private V21CollaborationTaskVo toTask(SuiteCollaborationEventVo event) {
        String product = normalizeProduct(event.productCode());
        return new V21CollaborationTaskVo(
                "event-" + event.eventId(),
                projectId(product),
                event.title(),
                product,
                taskStatus(event.eventType()),
                event.actorEmail(),
                null
        );
    }

    private V21CollaborationActivityVo toActivity(SuiteCollaborationEventVo event) {
        return new V21CollaborationActivityVo(
                String.valueOf(event.eventId()),
                event.title(),
                normalizeProduct(event.productCode()),
                event.createdAt()
        );
    }

    private V21NotificationVo toNotification(SuiteNotificationItemVo item) {
        return new V21NotificationVo(
                item.notificationId(),
                item.title(),
                item.message(),
                item.productCode(),
                item.severity(),
                item.read() ? NOTIFICATION_STATUS_READ : "UNREAD",
                item.createdAt(),
                item.readAt()
        );
    }

    private V21NotificationVo reloadNotification(Long userId, String notificationId, HttpServletRequest request) {
        NotificationQuery query = new NotificationQuery(NOTIFICATION_RELOAD_LIMIT, false, "ALL", true);
        return listNotifications(userId, query, request).stream()
                .filter(notification -> notification.id().equals(notificationId))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.INVALID_ARGUMENT, "v2 notification does not exist"));
    }

    private void assertReadStatus(V21NotificationPatchRequest request) {
        if (request != null && NOTIFICATION_STATUS_READ.equalsIgnoreCase(request.status())) {
            return;
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "v2 notifications only support status=READ");
    }

    private V21CommandCenterCommandVo toRouteCommand(SuiteCommandItemVo item) {
        String id = "route-" + slug(firstText(item.actionCode(), item.routePath(), item.label()));
        return new V21CommandCenterCommandVo(id, item.label(), item.description(), item.productCode(), true, 0);
    }

    private V21CommandCenterCommandVo toActionCommand(SuiteRemediationActionVo action) {
        String id = "action-" + slug(firstText(action.actionCode(), action.action(), action.productCode()));
        String description = "Recommended " + action.priority() + " remediation action";
        return new V21CommandCenterCommandVo(id, action.action(), description, action.productCode(), true, 0);
    }

    private void putCommandIfAbsent(Map<String, V21CommandCenterCommandVo> commands, V21CommandCenterCommandVo command) {
        commands.putIfAbsent(command.id(), command);
    }

    private String taskStatus(String eventType) {
        String normalized = eventType == null ? "" : eventType.toUpperCase(Locale.ROOT);
        if (normalized.contains("DELETE") || normalized.contains("REMOVE") || normalized.contains("REVOKE")) {
            return STATUS_DONE;
        }
        return STATUS_OPEN;
    }

    private String productId(String product) {
        return normalizeProduct(product).toLowerCase(Locale.ROOT);
    }

    private String projectId(String product) {
        return productId(product);
    }

    private String productName(String product) {
        return switch (normalizeProduct(product)) {
            case "MAIL" -> "Mail collaboration";
            case "CALENDAR" -> "Calendar collaboration";
            case "DOCS" -> "Docs collaboration";
            case "DRIVE" -> "Drive collaboration";
            case "PASS" -> "Pass collaboration";
            case "SHEETS" -> "Sheets collaboration";
            case "MEET" -> "Meet collaboration";
            default -> "Workspace collaboration";
        };
    }

    private String normalizeProduct(String product) {
        if (!StringUtils.hasText(product)) {
            return "WORKSPACE";
        }
        return product.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return value.trim();
    }

    private String firstText(String first, String second, String third) {
        if (StringUtils.hasText(first)) {
            return first;
        }
        if (StringUtils.hasText(second)) {
            return second;
        }
        return StringUtils.hasText(third) ? third : DEFAULT_COMMAND_SEED;
    }

    private String slug(String value) {
        String slug = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return StringUtils.hasText(slug) ? slug : DEFAULT_COMMAND_SEED;
    }

    public record NotificationQuery(
            Integer limit,
            Boolean unreadOnly,
            String status,
            Boolean includeSnoozed
    ) {
    }

    public record RequestContext(
            Long userId,
            Long sessionId,
            HttpServletRequest httpRequest
    ) {
    }

    public record PatchNotificationCommand(
            RequestContext context,
            String notificationId,
            V21NotificationPatchRequest patchRequest
    ) {
    }
}
