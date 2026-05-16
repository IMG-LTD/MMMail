package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.CommandPanelPreferenceMapper;
import com.mmmail.server.model.dto.V21CommandPinRequest;
import com.mmmail.server.model.entity.CommandPanelPreference;
import com.mmmail.server.model.vo.SuiteUnifiedSearchItemVo;
import com.mmmail.server.model.vo.V21CommandActionVo;
import com.mmmail.server.model.vo.V21CommandCatalogItemVo;
import com.mmmail.server.model.vo.V21CommandCenterCommandVo;
import com.mmmail.server.model.vo.V21CommandPreferenceVo;
import com.mmmail.server.model.vo.V21CommandQuickSearchItemVo;
import com.mmmail.server.model.vo.V21CommandRecentVo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class V21CommandPanelService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;
    private static final int FLAG_TRUE = 1;
    private static final int FLAG_FALSE = 0;
    private static final String ACTION_NAVIGATE = "navigate";
    private static final String SOURCE_COMMAND = "command";
    private static final String SOURCE_CONTENT = "content";
    private static final String FALLBACK_GROUP = "Workspace";
    private static final String COMMAND_CENTER_ROUTE = "/command-center";

    private static final List<CommandDefinition> BUILTIN_COMMANDS = List.of(
            new CommandDefinition(
                    "mail.compose",
                    "Compose new mail",
                    new CommandOptions(
                            new CommandPresentation("Mail", "mdi:email-edit-outline", "C"),
                            new CommandTarget("/mail", "compose", List.of("mail:write")),
                            new CommandMatch(List.of("/mail"))
                    )
            ),
            new CommandDefinition(
                    "mail.rules",
                    "Open mail rules",
                    new CommandOptions(
                            new CommandPresentation("Mail", "mdi:filter-outline", null),
                            new CommandTarget("/mail", "rules", List.of("mail:read")),
                            new CommandMatch(List.of("/mail"))
                    )
            ),
            new CommandDefinition(
                    "mail.folder.inbox",
                    "Open inbox",
                    new CommandOptions(
                            new CommandPresentation("Mail", "mdi:inbox-outline", null),
                            new CommandTarget("/mail/inbox", "folder", List.of("mail:read")),
                            new CommandMatch(List.of("/mail"))
                    )
            ),
            new CommandDefinition(
                    "calendar.create",
                    "Create calendar event",
                    new CommandOptions(
                            new CommandPresentation("Calendar", "mdi:calendar-plus-outline", null),
                            new CommandTarget("/calendar", "create", List.of("calendar:write")),
                            new CommandMatch(List.of("/calendar"))
                    )
            ),
            new CommandDefinition(
                    "drive.upload",
                    "Upload drive file",
                    new CommandOptions(
                            new CommandPresentation("Drive", "mdi:cloud-upload-outline", null),
                            new CommandTarget("/drive", "upload", List.of("drive:write")),
                            new CommandMatch(List.of("/drive"))
                    )
            ),
            new CommandDefinition(
                    "command.center",
                    "Open command center",
                    new CommandOptions(
                            new CommandPresentation("Command", "mdi:view-dashboard-outline", null),
                            new CommandTarget(COMMAND_CENTER_ROUTE, "open", List.of("command:center:read")),
                            new CommandMatch(List.of(COMMAND_CENTER_ROUTE))
                    )
            )
    );

    private final V21OpsRuntimeBridgeService opsRuntimeBridgeService;
    private final SuiteInsightService suiteInsightService;
    private final CommandPanelPreferenceMapper preferenceMapper;

    public V21CommandPanelService(
            V21OpsRuntimeBridgeService opsRuntimeBridgeService,
            SuiteInsightService suiteInsightService,
            CommandPanelPreferenceMapper preferenceMapper
    ) {
        this.opsRuntimeBridgeService = opsRuntimeBridgeService;
        this.suiteInsightService = suiteInsightService;
        this.preferenceMapper = preferenceMapper;
    }

    public List<V21CommandCatalogItemVo> listCatalog(Long userId, String context, HttpServletRequest request) {
        Map<String, CommandPanelPreference> preferences = loadPreferences(userId);
        return Stream.concat(
                        BUILTIN_COMMANDS.stream()
                                .filter(command -> command.matches(context))
                                .map(command -> toCatalogItem(command, preferences.get(command.id()))),
                        opsRuntimeBridgeService.listCommands(userId, request).stream()
                                .map(command -> toCatalogItem(command, preferences.get(command.id())))
                )
                .collect(LinkedHashMap::new, this::putCatalogItem, Map::putAll)
                .values()
                .stream()
                .sorted(catalogComparator())
                .toList();
    }

    public List<V21CommandRecentVo> listRecents(Long userId, Integer limit, HttpServletRequest request) {
        Map<String, V21CommandCatalogItemVo> catalog = catalogById(userId, request);
        return loadRecentPreferences(userId, limit).stream()
                .map(preference -> toRecent(preference, catalog.get(preference.getCommandId())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public V21CommandPreferenceVo pinCommand(Long userId, V21CommandPinRequest request, HttpServletRequest httpRequest) {
        String commandId = normalizeCommandId(request.commandId());
        assertCommandExists(userId, commandId, httpRequest);
        CommandPanelPreference preference = findPreference(userId, commandId).orElseGet(() -> newPreference(userId, commandId));
        LocalDateTime now = LocalDateTime.now();
        preference.setPinned(request.pinned() ? FLAG_TRUE : FLAG_FALSE);
        preference.setPinnedAt(request.pinned() ? now : null);
        preference.setLastUsedAt(now);
        preference.setUsageCount(safeUsageCount(preference) + 1);
        preference.setUpdatedAt(now);
        savePreference(preference);
        return toPreferenceVo(preference);
    }

    public List<V21CommandQuickSearchItemVo> quickSearch(CommandPanelUserContext context, QuickSearchOptions options) {
        String keyword = normalizeKeyword(options.query());
        int safeLimit = safeLimit(options.limit());
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        List<V21CommandQuickSearchItemVo> commands = searchCommandItems(context, keyword, safeLimit);
        List<V21CommandQuickSearchItemVo> content = searchContentItems(context, keyword, safeLimit);
        return Stream.concat(commands.stream(), content.stream())
                .limit(safeLimit)
                .toList();
    }

    private V21CommandCatalogItemVo toCatalogItem(CommandDefinition command, CommandPanelPreference preference) {
        return new V21CommandCatalogItemVo(
                command.id(),
                command.title(),
                null,
                command.options().presentation().group(),
                command.options().presentation().icon(),
                command.options().presentation().shortcut(),
                navigateAction(command.options().target().routePath(), Map.of("intent", command.options().target().intent())),
                command.options().target().requires(),
                isPinned(preference),
                preference == null ? null : preference.getLastUsedAt()
        );
    }

    private V21CommandCatalogItemVo toCatalogItem(V21CommandCenterCommandVo command, CommandPanelPreference preference) {
        return new V21CommandCatalogItemVo(
                command.id(),
                command.name(),
                null,
                productGroup(command.product()),
                "mdi:lightning-bolt-outline",
                null,
                navigateAction(COMMAND_CENTER_ROUTE, Map.of("commandId", command.id())),
                List.of("command:center:read"),
                isPinned(preference),
                preference == null ? null : preference.getLastUsedAt()
        );
    }

    private V21CommandActionVo navigateAction(String routePath, Map<String, Object> extraPayload) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("routePath", routePath);
        payload.putAll(extraPayload);
        return new V21CommandActionVo(ACTION_NAVIGATE, payload);
    }

    private List<V21CommandQuickSearchItemVo> searchCommandItems(
            CommandPanelUserContext context,
            String keyword,
            int limit
    ) {
        String normalized = keyword.toLowerCase(Locale.ROOT);
        return listCatalog(context.userId(), null, context.request()).stream()
                .filter(item -> item.title().toLowerCase(Locale.ROOT).contains(normalized))
                .limit(limit)
                .map(this::toQuickSearchItem)
                .toList();
    }

    private List<V21CommandQuickSearchItemVo> searchContentItems(
            CommandPanelUserContext context,
            String keyword,
            int limit
    ) {
        return suiteInsightService.unifiedSearch(context.userId(), keyword, limit, context.request().getRemoteAddr()).items().stream()
                .map(this::toQuickSearchItem)
                .toList();
    }

    private V21CommandQuickSearchItemVo toQuickSearchItem(V21CommandCatalogItemVo item) {
        String routePath = String.valueOf(item.action().payload().get("routePath"));
        return new V21CommandQuickSearchItemVo(SOURCE_COMMAND, item.id(), item.title(), item.group(), routePath, item.group());
    }

    private V21CommandQuickSearchItemVo toQuickSearchItem(SuiteUnifiedSearchItemVo item) {
        return new V21CommandQuickSearchItemVo(
                SOURCE_CONTENT,
                item.itemType() + ":" + item.entityId(),
                item.title(),
                item.summary(),
                item.routePath(),
                item.productCode()
        );
    }

    private Map<String, CommandPanelPreference> loadPreferences(Long userId) {
        return preferenceMapper.selectList(new LambdaQueryWrapper<CommandPanelPreference>()
                        .eq(CommandPanelPreference::getOwnerId, userId))
                .stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.getCommandId(), item), Map::putAll);
    }

    private List<CommandPanelPreference> loadRecentPreferences(Long userId, Integer limit) {
        return preferenceMapper.selectList(new LambdaQueryWrapper<CommandPanelPreference>()
                .eq(CommandPanelPreference::getOwnerId, userId)
                .isNotNull(CommandPanelPreference::getLastUsedAt)
                .orderByDesc(CommandPanelPreference::getLastUsedAt)
                .last("limit " + safeLimit(limit)));
    }

    private Optional<V21CommandRecentVo> toRecent(CommandPanelPreference preference, V21CommandCatalogItemVo item) {
        if (item == null) {
            return Optional.empty();
        }
        String routePath = String.valueOf(item.action().payload().get("routePath"));
        return Optional.of(new V21CommandRecentVo(
                item.id(),
                item.title(),
                item.group(),
                routePath,
                preference.getLastUsedAt(),
                safeUsageCount(preference)
        ));
    }

    private Map<String, V21CommandCatalogItemVo> catalogById(Long userId, HttpServletRequest request) {
        return listCatalog(userId, null, request).stream()
                .collect(LinkedHashMap::new, (map, item) -> map.put(item.id(), item), Map::putAll);
    }

    private Optional<CommandPanelPreference> findPreference(Long userId, String commandId) {
        return Optional.ofNullable(preferenceMapper.selectOne(new LambdaQueryWrapper<CommandPanelPreference>()
                .eq(CommandPanelPreference::getOwnerId, userId)
                .eq(CommandPanelPreference::getCommandId, commandId)
                .last("limit 1")));
    }

    private CommandPanelPreference newPreference(Long userId, String commandId) {
        LocalDateTime now = LocalDateTime.now();
        CommandPanelPreference preference = new CommandPanelPreference();
        preference.setOwnerId(userId);
        preference.setCommandId(commandId);
        preference.setPinned(FLAG_FALSE);
        preference.setUsageCount(0);
        preference.setCreatedAt(now);
        preference.setUpdatedAt(now);
        preference.setDeleted(FLAG_FALSE);
        return preference;
    }

    private void assertCommandExists(Long userId, String commandId, HttpServletRequest request) {
        if (!catalogById(userId, request).containsKey(commandId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "command does not exist");
        }
    }

    private void savePreference(CommandPanelPreference preference) {
        if (preference.getId() == null) {
            preferenceMapper.insert(preference);
            return;
        }
        preferenceMapper.updateById(preference);
    }

    private void putCatalogItem(Map<String, V21CommandCatalogItemVo> items, V21CommandCatalogItemVo item) {
        items.putIfAbsent(item.id(), item);
    }

    private Comparator<V21CommandCatalogItemVo> catalogComparator() {
        return Comparator.comparing(V21CommandCatalogItemVo::pinned).reversed()
                .thenComparing(V21CommandCatalogItemVo::lastUsedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(V21CommandCatalogItemVo::group)
                .thenComparing(V21CommandCatalogItemVo::title);
    }

    private V21CommandPreferenceVo toPreferenceVo(CommandPanelPreference preference) {
        return new V21CommandPreferenceVo(
                preference.getCommandId(),
                isPinned(preference),
                safeUsageCount(preference),
                preference.getLastUsedAt(),
                preference.getPinnedAt()
        );
    }

    private String normalizeCommandId(String commandId) {
        if (!StringUtils.hasText(commandId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "command id is required");
        }
        return commandId.trim();
    }

    private String normalizeKeyword(String query) {
        return StringUtils.hasText(query) ? query.trim() : "";
    }

    private String productGroup(String product) {
        return StringUtils.hasText(product) ? product.trim().toUpperCase(Locale.ROOT) : FALLBACK_GROUP;
    }

    private boolean isPinned(CommandPanelPreference preference) {
        return preference != null && Integer.valueOf(FLAG_TRUE).equals(preference.getPinned());
    }

    private int safeUsageCount(CommandPanelPreference preference) {
        return preference.getUsageCount() == null ? 0 : preference.getUsageCount();
    }

    private int safeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    public record CommandPanelUserContext(
            Long userId,
            HttpServletRequest request
    ) {
    }

    public record QuickSearchOptions(
            String query,
            Integer limit
    ) {
    }

    private record CommandDefinition(
            String id,
            String title,
            CommandOptions options
    ) {
        boolean matches(String context) {
            if (!StringUtils.hasText(context)) {
                return true;
            }
            String normalizedContext = context.trim().toLowerCase(Locale.ROOT);
            return options.match().contextPrefixes().stream()
                    .anyMatch(prefix -> normalizedContext.startsWith(prefix.toLowerCase(Locale.ROOT)));
        }
    }

    private record CommandOptions(
            CommandPresentation presentation,
            CommandTarget target,
            CommandMatch match
    ) {
    }

    private record CommandPresentation(
            String group,
            String icon,
            String shortcut
    ) {
    }

    private record CommandTarget(
            String routePath,
            String intent,
            List<String> requires
    ) {
    }

    private record CommandMatch(List<String> contextPrefixes) {
    }
}
