package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.server.mapper.DocsNoteMapper;
import com.mmmail.server.mapper.DocsNoteShareMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.model.entity.DocsNote;
import com.mmmail.server.model.entity.DocsNoteShare;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.DocsNoteSummaryVo;
import com.mmmail.server.model.vo.SuiteCollaborationCenterVo;
import com.mmmail.server.model.vo.SuiteCollaborationEventVo;
import com.mmmail.server.model.vo.SuiteCollaborationSyncVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Locale;

@Service
public class SuiteCollaborationService {

    private static final int DEFAULT_LIMIT = 24;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_DOCS_VISIBLE_NOTES = 50;
    private static final int PER_NOTE_EVENT_LIMIT = 12;
    private static final long STREAM_TIMEOUT_MS = 0L;
    private static final String KIND_BOOTSTRAP = "BOOTSTRAP";
    private static final String KIND_SYNC = "SYNC";
    private static final String KIND_UPDATE = "UPDATE";
    private static final String PRODUCT_DOCS = "DOCS";
    private static final String PRODUCT_DRIVE = "DRIVE";
    private static final String PRODUCT_SHEETS = "SHEETS";
    private static final String PRODUCT_MEET = "MEET";
    private static final Set<String> DOCS_EVENT_TYPES = Set.of(
            "DOCS_NOTE_CREATE",
            "DOCS_NOTE_UPDATE",
            "DOCS_NOTE_DELETE",
            "DOCS_NOTE_SHARE_ADD",
            "DOCS_NOTE_SHARE_PERMISSION_UPDATE",
            "DOCS_NOTE_SHARE_REMOVE",
            "DOCS_NOTE_COMMENT_ADD",
            "DOCS_NOTE_COMMENT_RESOLVE",
            "DOCS_NOTE_SUGGEST_ADD",
            "DOCS_NOTE_SUGGEST_ACCEPT",
            "DOCS_NOTE_SUGGEST_REJECT"
    );
    private static final Set<String> DRIVE_EVENT_TYPES = Set.of(
            "DRIVE_ITEM_CREATE",
            "DRIVE_ITEM_RENAME",
            "DRIVE_FILE_UPLOAD",
            "DRIVE_SHARE_CREATE",
            "DRIVE_SHARE_UPDATE",
            "DRIVE_SHARE_REVOKE",
            "DRIVE_COLLABORATOR_SHARE_ADD",
            "DRIVE_COLLABORATOR_SHARE_PERMISSION_UPDATE",
            "DRIVE_COLLABORATOR_SHARE_REVOKE",
            "DRIVE_COLLABORATOR_SHARE_ACCEPT",
            "DRIVE_COLLABORATOR_SHARE_DECLINE",
            "DRIVE_COLLABORATOR_FOLDER_CREATE",
            "DRIVE_COLLABORATOR_FILE_UPLOAD",
            "DRIVE_FILE_VERSION_UPLOAD",
            "DRIVE_FILE_VERSION_RESTORE",
            "DRIVE_ITEM_DELETE"
    );
    private static final Set<String> SHEETS_EVENT_TYPES = Set.of(
            "SHEETS_WORKBOOK_CREATE",
            "SHEETS_WORKBOOK_IMPORT",
            "SHEETS_WORKBOOK_RENAME",
            "SHEETS_WORKBOOK_SHEET_CREATE",
            "SHEETS_WORKBOOK_SHEET_RENAME",
            "SHEETS_WORKBOOK_SHEET_DELETE",
            "SHEETS_WORKBOOK_ACTIVE_SHEET_SET",
            "SHEETS_WORKBOOK_SHEET_SORT",
            "SHEETS_WORKBOOK_SHEET_FREEZE",
            "SHEETS_WORKBOOK_UPDATE_CELLS",
            "SHEETS_WORKBOOK_SHARE_ADD",
            "SHEETS_WORKBOOK_SHARE_PERMISSION_UPDATE",
            "SHEETS_WORKBOOK_SHARE_REVOKE",
            "SHEETS_WORKBOOK_SHARE_ACCEPT",
            "SHEETS_WORKBOOK_SHARE_DECLINE",
            "SHEETS_WORKBOOK_VERSION_RESTORE",
            "SHEETS_WORKBOOK_EXPORT",
            "SHEETS_WORKBOOK_DELETE"
    );
    private static final Set<String> MEET_EVENT_TYPES = Set.of(
            "MEET_ROOM_CREATE",
            "MEET_ROOM_END",
            "MEET_PARTICIPANT_JOIN",
            "MEET_PARTICIPANT_REMOVE",
            "MEET_HOST_TRANSFER"
    );

    private final AuditService auditService;
    private final DocsAccessService docsAccessService;
    private final DocsNoteMapper docsNoteMapper;
    private final DocsNoteShareMapper docsNoteShareMapper;
    private final UserAccountMapper userAccountMapper;
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<EmitterRegistration>> emitterRegistry = new ConcurrentHashMap<>();

    public SuiteCollaborationService(
            AuditService auditService,
            DocsAccessService docsAccessService,
            DocsNoteMapper docsNoteMapper,
            DocsNoteShareMapper docsNoteShareMapper,
            UserAccountMapper userAccountMapper
    ) {
        this.auditService = auditService;
        this.docsAccessService = docsAccessService;
        this.docsNoteMapper = docsNoteMapper;
        this.docsNoteShareMapper = docsNoteShareMapper;
        this.userAccountMapper = userAccountMapper;
    }

    public SuiteCollaborationCenterVo getCenter(Long userId, Integer limit, String ipAddress) {
        return getCenter(userId, limit, ipAddress, Set.of());
    }

    public SuiteCollaborationCenterVo getCenter(Long userId, Integer limit, String ipAddress, Set<String> visibleProductCodes) {
        int safeLimit = normalizeLimit(limit);
        List<SuiteCollaborationEventVo> items = filterVisibleItems(loadEventItems(userId, null, safeLimit), visibleProductCodes);
        long syncCursor = getCurrentCursor(userId);
        auditService.record(
                userId,
                "SUITE_COLLABORATION_CENTER_QUERY",
                "count=" + items.size() + ",limit=" + safeLimit,
                normalizeIp(ipAddress)
        );
        return new SuiteCollaborationCenterVo(
                LocalDateTime.now(),
                safeLimit,
                items.size(),
                buildProductCounts(items),
                syncCursor,
                buildSyncVersion(syncCursor),
                items
        );
    }

    public SuiteCollaborationSyncVo getSync(Long userId, Long afterEventId, Integer limit) {
        return getSync(userId, afterEventId, limit, Set.of());
    }

    public SuiteCollaborationSyncVo getSync(Long userId, Long afterEventId, Integer limit, Set<String> visibleProductCodes) {
        return buildSyncPayload(KIND_SYNC, userId, afterEventId, normalizeLimit(limit), visibleProductCodes);
    }

    public SseEmitter openStream(Long userId, Long afterEventId) {
        return openStream(userId, afterEventId, Set.of());
    }

    public SseEmitter openStream(Long userId, Long afterEventId, Set<String> visibleProductCodes) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        EmitterRegistration registration = new EmitterRegistration(emitter, Set.copyOf(visibleProductCodes));
        registerEmitter(userId, registration);
        sendPayload(emitter, buildSyncPayload(KIND_BOOTSTRAP, userId, afterEventId, DEFAULT_LIMIT, visibleProductCodes));
        return emitter;
    }

    public long getCurrentCursor(Long userId) {
        long cursor = latestActorEventId(userId, DRIVE_EVENT_TYPES);
        cursor = Math.max(cursor, latestActorEventId(userId, SHEETS_EVENT_TYPES));
        cursor = Math.max(cursor, latestActorEventId(userId, MEET_EVENT_TYPES));
        for (DocsNoteSummaryVo note : docsAccessService.listVisibleNotes(userId, null, MAX_DOCS_VISIBLE_NOTES)) {
            AuditEventVo latest = auditService.latestEventByDetail(DOCS_EVENT_TYPES, noteToken(note.id()));
            if (latest != null && latest.id() != null) {
                cursor = Math.max(cursor, latest.id());
            }
        }
        return cursor;
    }

    public String buildSyncVersion(Long cursor) {
        long safeCursor = cursor == null || cursor < 0 ? 0L : cursor;
        return "COLLAB-" + safeCursor;
    }

    public void publishToUser(Long userId, AuditEventVo event) {
        SuiteCollaborationEventVo item = toEventVo(userId, event);
        if (item == null) {
            return;
        }
        publishToRecipients(Set.of(userId), item);
    }

    public void publishToDocsRecipients(Long noteId, AuditEventVo event) {
        SuiteCollaborationEventVo item = toDocsEventVo(event);
        if (item == null) {
            return;
        }
        publishToRecipients(loadDocsRecipientIds(noteId), item);
    }

    private SuiteCollaborationSyncVo buildSyncPayload(
            String kind,
            Long userId,
            Long afterEventId,
            int limit,
            Set<String> visibleProductCodes
    ) {
        List<SuiteCollaborationEventVo> items = filterVisibleItems(loadEventItems(userId, afterEventId, limit), visibleProductCodes);
        long syncCursor = getCurrentCursor(userId);
        return new SuiteCollaborationSyncVo(
                kind,
                LocalDateTime.now(),
                syncCursor,
                buildSyncVersion(syncCursor),
                !items.isEmpty(),
                items.size(),
                items
        );
    }

    private List<SuiteCollaborationEventVo> loadEventItems(Long userId, Long afterEventId, int limit) {
        boolean incremental = afterEventId != null && afterEventId > 0;
        String currentUserEmail = loadUserEmail(userId);
        Map<Long, SuiteCollaborationEventVo> merged = new LinkedHashMap<>();
        mergeEvents(merged, loadDocsEvents(userId, afterEventId, incremental));
        mergeEvents(merged, mapActorEvents(currentUserEmail, PRODUCT_DRIVE, auditService.listActorEvents(
                userId,
                DRIVE_EVENT_TYPES,
                afterEventId,
                limit,
                incremental
        )));
        mergeEvents(merged, mapActorEvents(currentUserEmail, PRODUCT_SHEETS, auditService.listActorEvents(
                userId,
                SHEETS_EVENT_TYPES,
                afterEventId,
                limit,
                incremental
        )));
        mergeEvents(merged, mapActorEvents(currentUserEmail, PRODUCT_MEET, auditService.listActorEvents(
                userId,
                MEET_EVENT_TYPES,
                afterEventId,
                limit,
                incremental
        )));
        List<SuiteCollaborationEventVo> items = new ArrayList<>(merged.values());
        Comparator<SuiteCollaborationEventVo> order = Comparator.comparingLong(SuiteCollaborationEventVo::eventId);
        items.sort(incremental ? order : order.reversed());
        if (items.size() <= limit) {
            return items;
        }
        return new ArrayList<>(items.subList(0, limit));
    }

    private List<SuiteCollaborationEventVo> loadDocsEvents(Long userId, Long afterEventId, boolean incremental) {
        List<DocsNoteSummaryVo> notes = docsAccessService.listVisibleNotes(userId, null, MAX_DOCS_VISIBLE_NOTES);
        if (notes.isEmpty()) {
            return List.of();
        }
        Map<Long, SuiteCollaborationEventVo> merged = new LinkedHashMap<>();
        for (DocsNoteSummaryVo note : notes) {
            List<AuditEventVo> events = auditService.listEventsByDetail(
                    DOCS_EVENT_TYPES,
                    noteToken(note.id()),
                    afterEventId,
                    PER_NOTE_EVENT_LIMIT,
                    incremental
            );
            for (AuditEventVo event : events) {
                SuiteCollaborationEventVo item = toDocsEventVo(event);
                if (item != null) {
                    merged.putIfAbsent(item.eventId(), item);
                }
            }
        }
        return new ArrayList<>(merged.values());
    }

    private List<SuiteCollaborationEventVo> mapActorEvents(
            String currentUserEmail,
            String productCode,
            List<AuditEventVo> events
    ) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        List<SuiteCollaborationEventVo> items = new ArrayList<>();
        for (AuditEventVo event : events) {
            SuiteCollaborationEventVo item = switch (productCode) {
                case PRODUCT_DRIVE -> toDriveEventVo(event, currentUserEmail);
                case PRODUCT_SHEETS -> toSheetsEventVo(event, currentUserEmail);
                case PRODUCT_MEET -> toMeetEventVo(event, currentUserEmail);
                default -> null;
            };
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private SuiteCollaborationEventVo toEventVo(Long userId, AuditEventVo event) {
        if (event == null || !StringUtils.hasText(event.eventType())) {
            return null;
        }
        if (DOCS_EVENT_TYPES.contains(event.eventType())) {
            return toDocsEventVo(event);
        }
        String currentUserEmail = loadUserEmail(userId);
        if (DRIVE_EVENT_TYPES.contains(event.eventType())) {
            return toDriveEventVo(event, currentUserEmail);
        }
        if (SHEETS_EVENT_TYPES.contains(event.eventType())) {
            return toSheetsEventVo(event, currentUserEmail);
        }
        if (MEET_EVENT_TYPES.contains(event.eventType())) {
            return toMeetEventVo(event, currentUserEmail);
        }
        return null;
    }

    private SuiteCollaborationEventVo toDocsEventVo(AuditEventVo event) {
        String noteId = parseDetailValue(event.detail(), "noteId");
        if (!StringUtils.hasText(noteId)) {
            return null;
        }
        return new SuiteCollaborationEventVo(
                safeEventId(event),
                PRODUCT_DOCS,
                event.eventType(),
                resolveDocsTitle(event.eventType()),
                resolveDocsSummary(event),
                "/docs?noteId=" + noteId,
                parseNullableDetail(event.detail(), "actorEmail"),
                parseNullableDetail(event.detail(), "sessionId"),
                event.createdAt()
        );
    }

    private SuiteCollaborationEventVo toDriveEventVo(AuditEventVo event, String currentUserEmail) {
        String itemId = parseDetailValue(event.detail(), "itemId");
        return new SuiteCollaborationEventVo(
                safeEventId(event),
                PRODUCT_DRIVE,
                event.eventType(),
                resolveDriveTitle(event.eventType()),
                resolveDriveSummary(event),
                StringUtils.hasText(itemId) ? "/drive?itemId=" + itemId : "/drive",
                currentUserEmail,
                parseNullableDetail(event.detail(), "sessionId"),
                event.createdAt()
        );
    }

    private SuiteCollaborationEventVo toSheetsEventVo(AuditEventVo event, String currentUserEmail) {
        String workbookId = parseDetailValue(event.detail(), "workbookId");
        return new SuiteCollaborationEventVo(
                safeEventId(event),
                PRODUCT_SHEETS,
                event.eventType(),
                resolveSheetsTitle(event.eventType()),
                resolveSheetsSummary(event),
                StringUtils.hasText(workbookId) ? "/sheets?workbookId=" + workbookId : "/sheets",
                currentUserEmail,
                parseNullableDetail(event.detail(), "sessionId"),
                event.createdAt()
        );
    }

    private SuiteCollaborationEventVo toMeetEventVo(AuditEventVo event, String currentUserEmail) {
        String roomId = parseDetailValue(event.detail(), "roomId");
        return new SuiteCollaborationEventVo(
                safeEventId(event),
                PRODUCT_MEET,
                event.eventType(),
                resolveMeetTitle(event.eventType()),
                resolveMeetSummary(event),
                StringUtils.hasText(roomId) ? "/meet?roomId=" + roomId : "/meet",
                currentUserEmail,
                parseNullableDetail(event.detail(), "sessionId"),
                event.createdAt()
        );
    }

    private String resolveDocsTitle(String eventType) {
        return switch (eventType) {
            case "DOCS_NOTE_CREATE" -> "Document created";
            case "DOCS_NOTE_UPDATE" -> "Document updated";
            case "DOCS_NOTE_DELETE" -> "Document deleted";
            case "DOCS_NOTE_SHARE_ADD" -> "Collaborator added";
            case "DOCS_NOTE_SHARE_PERMISSION_UPDATE" -> "Collaborator permission updated";
            case "DOCS_NOTE_SHARE_REMOVE" -> "Collaborator removed";
            case "DOCS_NOTE_COMMENT_ADD" -> "Comment added";
            case "DOCS_NOTE_COMMENT_RESOLVE" -> "Comment resolved";
            case "DOCS_NOTE_SUGGEST_ADD" -> "Suggestion created";
            case "DOCS_NOTE_SUGGEST_ACCEPT" -> "Suggestion accepted";
            case "DOCS_NOTE_SUGGEST_REJECT" -> "Suggestion rejected";
            default -> "Docs activity";
        };
    }

    private String resolveDriveTitle(String eventType) {
        return switch (eventType) {
            case "DRIVE_ITEM_CREATE" -> "Drive item created";
            case "DRIVE_ITEM_RENAME" -> "Drive item renamed";
            case "DRIVE_FILE_UPLOAD" -> "Drive file uploaded";
            case "DRIVE_SHARE_CREATE" -> "Drive share created";
            case "DRIVE_SHARE_UPDATE" -> "Drive share updated";
            case "DRIVE_SHARE_REVOKE" -> "Drive share revoked";
            case "DRIVE_COLLABORATOR_SHARE_ADD" -> "Collaborator invited";
            case "DRIVE_COLLABORATOR_SHARE_PERMISSION_UPDATE" -> "Collaborator access updated";
            case "DRIVE_COLLABORATOR_SHARE_REVOKE" -> "Collaborator access revoked";
            case "DRIVE_COLLABORATOR_SHARE_ACCEPT" -> "Collaborator invite accepted";
            case "DRIVE_COLLABORATOR_SHARE_DECLINE" -> "Collaborator invite declined";
            case "DRIVE_COLLABORATOR_FOLDER_CREATE" -> "Shared folder updated";
            case "DRIVE_COLLABORATOR_FILE_UPLOAD" -> "Shared file uploaded";
            case "DRIVE_FILE_VERSION_UPLOAD" -> "New file version uploaded";
            case "DRIVE_FILE_VERSION_RESTORE" -> "File version restored";
            case "DRIVE_ITEM_DELETE" -> "Drive item deleted";
            default -> "Drive activity";
        };
    }

    private String resolveSheetsTitle(String eventType) {
        return switch (eventType) {
            case "SHEETS_WORKBOOK_CREATE" -> "Workbook created";
            case "SHEETS_WORKBOOK_IMPORT" -> "Workbook imported";
            case "SHEETS_WORKBOOK_RENAME" -> "Workbook renamed";
            case "SHEETS_WORKBOOK_SHEET_CREATE" -> "Sheet created";
            case "SHEETS_WORKBOOK_SHEET_RENAME" -> "Sheet renamed";
            case "SHEETS_WORKBOOK_SHEET_DELETE" -> "Sheet deleted";
            case "SHEETS_WORKBOOK_ACTIVE_SHEET_SET" -> "Active sheet changed";
            case "SHEETS_WORKBOOK_SHEET_SORT" -> "Sheet sorted";
            case "SHEETS_WORKBOOK_SHEET_FREEZE" -> "Sheet freeze updated";
            case "SHEETS_WORKBOOK_UPDATE_CELLS" -> "Workbook updated";
            case "SHEETS_WORKBOOK_SHARE_ADD" -> "Workbook shared";
            case "SHEETS_WORKBOOK_SHARE_PERMISSION_UPDATE" -> "Share permission updated";
            case "SHEETS_WORKBOOK_SHARE_REVOKE" -> "Share revoked";
            case "SHEETS_WORKBOOK_SHARE_ACCEPT" -> "Share accepted";
            case "SHEETS_WORKBOOK_SHARE_DECLINE" -> "Share declined";
            case "SHEETS_WORKBOOK_VERSION_RESTORE" -> "Version restored";
            case "SHEETS_WORKBOOK_EXPORT" -> "Workbook exported";
            case "SHEETS_WORKBOOK_DELETE" -> "Workbook deleted";
            default -> "Sheets activity";
        };
    }

    private String resolveMeetTitle(String eventType) {
        return switch (eventType) {
            case "MEET_ROOM_CREATE" -> "Meeting created";
            case "MEET_ROOM_END" -> "Meeting ended";
            case "MEET_PARTICIPANT_JOIN" -> "Participant joined";
            case "MEET_PARTICIPANT_REMOVE" -> "Participant removed";
            case "MEET_HOST_TRANSFER" -> "Host transferred";
            default -> "Meet activity";
        };
    }

    private String resolveDocsSummary(AuditEventVo event) {
        String actorEmail = parseNullableDetail(event.detail(), "actorEmail");
        String version = parseNullableDetail(event.detail(), "version");
        String collaboratorEmail = parseNullableDetail(event.detail(), "collaboratorEmail");
        String permission = parseNullableDetail(event.detail(), "permission");
        return switch (event.eventType()) {
            case "DOCS_NOTE_CREATE" -> actorEmail + " created a new document";
            case "DOCS_NOTE_UPDATE" -> actorEmail + " updated the document to version " + nullableLabel(version, "latest");
            case "DOCS_NOTE_DELETE" -> actorEmail + " deleted the document";
            case "DOCS_NOTE_SHARE_ADD" -> actorEmail + " added collaborator " + nullableLabel(collaboratorEmail, "unknown")
                    + " (" + nullableLabel(permission, "VIEW") + ")";
            case "DOCS_NOTE_SHARE_PERMISSION_UPDATE" -> actorEmail + " changed collaborator permission to "
                    + nullableLabel(permission, "VIEW");
            case "DOCS_NOTE_SHARE_REMOVE" -> actorEmail + " removed collaborator " + nullableLabel(collaboratorEmail, "unknown");
            case "DOCS_NOTE_COMMENT_ADD" -> actorEmail + " added a comment";
            case "DOCS_NOTE_COMMENT_RESOLVE" -> actorEmail + " resolved a comment";
            case "DOCS_NOTE_SUGGEST_ADD" -> actorEmail + " proposed a suggestion";
            case "DOCS_NOTE_SUGGEST_ACCEPT" -> actorEmail + " accepted a suggestion";
            case "DOCS_NOTE_SUGGEST_REJECT" -> actorEmail + " rejected a suggestion";
            default -> actorEmail + " changed the document collaboration state";
        };
    }

    private String resolveDriveSummary(AuditEventVo event) {
        String itemId = parseNullableDetail(event.detail(), "itemId");
        String type = parseNullableDetail(event.detail(), "itemType");
        if (!StringUtils.hasText(type)) {
            type = parseNullableDetail(event.detail(), "type");
        }
        String permission = parseNullableDetail(event.detail(), "permission");
        String versionId = parseNullableDetail(event.detail(), "versionId");
        String collaboratorEmail = parseNullableDetail(event.detail(), "collaboratorEmail");
        String responseStatus = parseNullableDetail(event.detail(), "responseStatus");
        return switch (event.eventType()) {
            case "DRIVE_ITEM_CREATE" -> "Created " + nullableLabel(type, "item") + " " + nullableLabel(itemId, "-");
            case "DRIVE_ITEM_RENAME" -> "Renamed item " + nullableLabel(itemId, "-");
            case "DRIVE_FILE_UPLOAD" -> "Uploaded file " + nullableLabel(itemId, "-");
            case "DRIVE_SHARE_CREATE" -> "Created " + nullableLabel(permission, "VIEW") + " share for item " + nullableLabel(itemId, "-");
            case "DRIVE_SHARE_UPDATE" -> "Updated share settings for item " + nullableLabel(itemId, "-");
            case "DRIVE_SHARE_REVOKE" -> "Revoked share for item " + nullableLabel(itemId, "-");
            case "DRIVE_COLLABORATOR_SHARE_ADD" -> "Invited " + nullableLabel(collaboratorEmail, "collaborator")
                    + " with " + nullableLabel(permission, "VIEW") + " access";
            case "DRIVE_COLLABORATOR_SHARE_PERMISSION_UPDATE" -> "Updated collaborator access to "
                    + nullableLabel(permission, "VIEW") + " for " + nullableLabel(collaboratorEmail, "collaborator");
            case "DRIVE_COLLABORATOR_SHARE_REVOKE" -> "Revoked collaborator access for "
                    + nullableLabel(collaboratorEmail, "collaborator");
            case "DRIVE_COLLABORATOR_SHARE_ACCEPT" -> "Accepted collaborator invite ("
                    + nullableLabel(responseStatus, "ACCEPTED") + ")";
            case "DRIVE_COLLABORATOR_SHARE_DECLINE" -> "Declined collaborator invite ("
                    + nullableLabel(responseStatus, "DECLINED") + ")";
            case "DRIVE_COLLABORATOR_FOLDER_CREATE" -> "Created folder " + nullableLabel(itemId, "-")
                    + " inside shared workspace";
            case "DRIVE_COLLABORATOR_FILE_UPLOAD" -> "Uploaded file " + nullableLabel(itemId, "-")
                    + " inside shared workspace";
            case "DRIVE_FILE_VERSION_UPLOAD" -> "Uploaded a new version for item " + nullableLabel(itemId, "-");
            case "DRIVE_FILE_VERSION_RESTORE" -> "Restored version " + nullableLabel(versionId, "-") + " for item " + nullableLabel(itemId, "-");
            case "DRIVE_ITEM_DELETE" -> "Moved item " + nullableLabel(itemId, "-") + " to trash";
            default -> "Updated drive item " + nullableLabel(itemId, "-");
        };
    }

    private String resolveSheetsSummary(AuditEventVo event) {
        String workbookId = parseNullableDetail(event.detail(), "workbookId");
        String title = parseNullableDetail(event.detail(), "title");
        String version = parseNullableDetail(event.detail(), "version");
        String format = parseNullableDetail(event.detail(), "format");
        String sheetName = parseNullableDetail(event.detail(), "sheetName");
        String permission = parseNullableDetail(event.detail(), "permission");
        String collaboratorEmail = parseNullableDetail(event.detail(), "collaboratorEmail");
        String responseStatus = parseNullableDetail(event.detail(), "responseStatus");
        String restoredFromVersionNo = parseNullableDetail(event.detail(), "restoredFromVersionNo");
        return switch (event.eventType()) {
            case "SHEETS_WORKBOOK_CREATE" -> "Created workbook " + nullableLabel(title, nullableLabel(workbookId, "-"));
            case "SHEETS_WORKBOOK_IMPORT" -> "Imported workbook " + nullableLabel(title, nullableLabel(workbookId, "-"))
                    + " from " + nullableLabel(format, "CSV");
            case "SHEETS_WORKBOOK_RENAME" -> "Renamed workbook to " + nullableLabel(title, nullableLabel(workbookId, "-"));
            case "SHEETS_WORKBOOK_SHEET_CREATE" -> "Added sheet " + nullableLabel(sheetName, "Sheet")
                    + " to workbook " + nullableLabel(title, nullableLabel(workbookId, "-"));
            case "SHEETS_WORKBOOK_SHEET_RENAME" -> "Renamed sheet to " + nullableLabel(sheetName, "Sheet")
                    + " in workbook " + nullableLabel(title, nullableLabel(workbookId, "-"));
            case "SHEETS_WORKBOOK_SHEET_DELETE" -> "Deleted sheet " + nullableLabel(sheetName, "Sheet")
                    + " from workbook " + nullableLabel(title, nullableLabel(workbookId, "-"));
            case "SHEETS_WORKBOOK_ACTIVE_SHEET_SET" -> "Switched active sheet to " + nullableLabel(sheetName, "Sheet")
                    + " in workbook " + nullableLabel(title, nullableLabel(workbookId, "-"));
            case "SHEETS_WORKBOOK_SHEET_SORT" -> "Sorted sheet " + nullableLabel(sheetName, "Sheet")
                    + " in workbook " + nullableLabel(title, nullableLabel(workbookId, "-"));
            case "SHEETS_WORKBOOK_SHEET_FREEZE" -> "Updated freeze pane for " + nullableLabel(sheetName, "Sheet")
                    + " in workbook " + nullableLabel(title, nullableLabel(workbookId, "-"));
            case "SHEETS_WORKBOOK_UPDATE_CELLS" -> "Saved workbook " + nullableLabel(title, nullableLabel(workbookId, "-"))
                    + " at version " + nullableLabel(version, "latest");
            case "SHEETS_WORKBOOK_SHARE_ADD" -> "Shared workbook with " + nullableLabel(collaboratorEmail, "collaborator")
                    + " (" + nullableLabel(permission, "VIEW") + ")";
            case "SHEETS_WORKBOOK_SHARE_PERMISSION_UPDATE" -> "Updated workbook share permission to "
                    + nullableLabel(permission, "VIEW") + " for " + nullableLabel(collaboratorEmail, "collaborator");
            case "SHEETS_WORKBOOK_SHARE_REVOKE" -> "Revoked workbook share for "
                    + nullableLabel(collaboratorEmail, "collaborator");
            case "SHEETS_WORKBOOK_SHARE_ACCEPT" -> "Workbook share accepted (" + nullableLabel(responseStatus, "ACCEPTED") + ")";
            case "SHEETS_WORKBOOK_SHARE_DECLINE" -> "Workbook share declined (" + nullableLabel(responseStatus, "DECLINED") + ")";
            case "SHEETS_WORKBOOK_VERSION_RESTORE" -> "Restored workbook " + nullableLabel(title, nullableLabel(workbookId, "-"))
                    + " from version " + nullableLabel(restoredFromVersionNo, "previous");
            case "SHEETS_WORKBOOK_EXPORT" -> "Exported workbook " + nullableLabel(title, nullableLabel(workbookId, "-"))
                    + " as " + nullableLabel(format, "CSV");
            case "SHEETS_WORKBOOK_DELETE" -> "Deleted workbook " + nullableLabel(title, nullableLabel(workbookId, "-"));
            default -> "Updated workbook " + nullableLabel(title, nullableLabel(workbookId, "-"));
        };
    }

    private String resolveMeetSummary(AuditEventVo event) {
        String roomId = parseNullableDetail(event.detail(), "roomId");
        String participantId = parseNullableDetail(event.detail(), "participantId");
        String targetParticipantId = parseNullableDetail(event.detail(), "targetParticipantId");
        String role = parseNullableDetail(event.detail(), "role");
        return switch (event.eventType()) {
            case "MEET_ROOM_CREATE" -> "Created meeting room " + nullableLabel(roomId, "-");
            case "MEET_ROOM_END" -> "Ended meeting room " + nullableLabel(roomId, "-");
            case "MEET_PARTICIPANT_JOIN" -> "Participant " + nullableLabel(participantId, "-") + " joined as " + nullableLabel(role, "PARTICIPANT");
            case "MEET_PARTICIPANT_REMOVE" -> "Removed participant " + nullableLabel(participantId, "-") + " from room " + nullableLabel(roomId, "-");
            case "MEET_HOST_TRANSFER" -> "Transferred host to participant " + nullableLabel(targetParticipantId, "-");
            default -> "Updated meeting room " + nullableLabel(roomId, "-");
        };
    }

    private Map<String, Integer> buildProductCounts(List<SuiteCollaborationEventVo> items) {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("ALL", items.size());
        result.put(PRODUCT_DOCS, 0);
        result.put(PRODUCT_DRIVE, 0);
        result.put(PRODUCT_SHEETS, 0);
        result.put(PRODUCT_MEET, 0);
        for (SuiteCollaborationEventVo item : items) {
            result.computeIfPresent(item.productCode(), (key, value) -> value + 1);
        }
        return result;
    }

    private Set<Long> loadDocsRecipientIds(Long noteId) {
        Set<Long> recipientIds = new LinkedHashSet<>();
        DocsNote note = docsNoteMapper.selectById(noteId);
        if (note != null && note.getOwnerId() != null) {
            recipientIds.add(note.getOwnerId());
        }
        List<DocsNoteShare> shares = docsNoteShareMapper.selectList(new LambdaQueryWrapper<DocsNoteShare>()
                .eq(DocsNoteShare::getNoteId, noteId));
        for (DocsNoteShare share : shares) {
            if (share.getCollaboratorUserId() != null) {
                recipientIds.add(share.getCollaboratorUserId());
            }
        }
        return recipientIds;
    }

    private long latestActorEventId(Long userId, Set<String> eventTypes) {
        AuditEventVo latest = auditService.latestActorEvent(userId, eventTypes);
        return latest == null || latest.id() == null ? 0L : latest.id();
    }

    private String loadUserEmail(Long userId) {
        UserAccount user = userAccountMapper.selectById(userId);
        return user == null ? "unknown@mmmail.local" : user.getEmail();
    }

    private void publishToRecipients(Set<Long> userIds, SuiteCollaborationEventVo item) {
        if (item == null || userIds == null || userIds.isEmpty()) {
            return;
        }
        SuiteCollaborationSyncVo payload = new SuiteCollaborationSyncVo(
                KIND_UPDATE,
                LocalDateTime.now(),
                item.eventId(),
                buildSyncVersion(item.eventId()),
                true,
                1,
                List.of(item)
        );
        for (Long userId : userIds) {
            for (EmitterRegistration registration : emitterRegistry.getOrDefault(userId, new CopyOnWriteArrayList<>())) {
                if (!isProductVisible(registration.visibleProductCodes(), item.productCode())) {
                    continue;
                }
                if (!sendPayload(registration.emitter(), payload)) {
                    removeEmitter(userId, registration.emitter());
                }
            }
        }
    }

    private void mergeEvents(Map<Long, SuiteCollaborationEventVo> merged, List<SuiteCollaborationEventVo> items) {
        for (SuiteCollaborationEventVo item : items) {
            merged.putIfAbsent(item.eventId(), item);
        }
    }

    private void registerEmitter(Long userId, EmitterRegistration registration) {
        emitterRegistry.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(registration);
        registration.emitter().onCompletion(() -> removeEmitter(userId, registration.emitter()));
        registration.emitter().onTimeout(() -> removeEmitter(userId, registration.emitter()));
        registration.emitter().onError(ignored -> removeEmitter(userId, registration.emitter()));
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<EmitterRegistration> emitters = emitterRegistry.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.removeIf(item -> item.emitter() == emitter);
        if (emitters.isEmpty()) {
            emitterRegistry.remove(userId);
        }
    }

    private boolean sendPayload(SseEmitter emitter, SuiteCollaborationSyncVo payload) {
        try {
            emitter.send(SseEmitter.event().name("suite-collaboration").data(payload));
            return true;
        } catch (IOException exception) {
            emitter.completeWithError(exception);
            return false;
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private List<SuiteCollaborationEventVo> filterVisibleItems(
            List<SuiteCollaborationEventVo> items,
            Set<String> visibleProductCodes
    ) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        if (visibleProductCodes == null || visibleProductCodes.isEmpty()) {
            return items;
        }
        return items.stream()
                .filter(item -> isProductVisible(visibleProductCodes, item.productCode()))
                .toList();
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

    private String normalizeIp(String ipAddress) {
        return StringUtils.hasText(ipAddress) ? ipAddress : "0.0.0.0";
    }

    private String noteToken(String noteId) {
        return "noteId=" + noteId + ";";
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

    private String parseNullableDetail(String detail, String key) {
        String value = parseDetailValue(detail, key);
        return StringUtils.hasText(value) ? value : null;
    }

    private String nullableLabel(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private long safeEventId(AuditEventVo event) {
        return event == null || event.id() == null ? 0L : event.id();
    }

    private record EmitterRegistration(SseEmitter emitter, Set<String> visibleProductCodes) {
    }
}
