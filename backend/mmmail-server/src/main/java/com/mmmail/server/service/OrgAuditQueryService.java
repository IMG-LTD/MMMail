package com.mmmail.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.vo.OrgAuditEventVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OrgAuditQueryService {

    private static final String JSONL_SCHEMA_VERSION = "mmmail.audit.v1";
    private static final String JSONL_SOURCE = "mmmail";
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 10_000;
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final OrgAccessService orgAccessService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public OrgAuditQueryService(
            OrgAccessService orgAccessService,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.orgAccessService = orgAccessService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public List<OrgAuditEventVo> listEvents(
            Long userId,
            Long orgId,
            Integer limit,
            String eventType,
            String actorEmail,
            String keyword,
            String fromDate,
            String toDate,
            String sortDirection,
            String ipAddress
    ) {
        orgAccessService.requireManageMember(userId, orgId);
        AuditFilters filters = buildFilters(limit, eventType, actorEmail, keyword, fromDate, toDate, sortDirection);
        List<OrgAuditEventVo> events = auditService.listByOrg(
                orgId,
                filters.limit(),
                filters.eventType(),
                filters.actorEmail(),
                filters.keyword(),
                filters.fromAt(),
                filters.toAt(),
                filters.ascending()
        );
        auditService.record(userId, "ORG_AUDIT_LIST", buildListAuditDetail(orgId, events.size(), filters), ipAddress, orgId);
        return events;
    }

    public CsvExportFile exportEvents(
            Long userId,
            Long orgId,
            Integer limit,
            String eventType,
            String actorEmail,
            String keyword,
            String fromDate,
            String toDate,
            String sortDirection,
            String ipAddress
    ) {
        orgAccessService.requireManageMember(userId, orgId);
        AuditFilters filters = buildFilters(limit, eventType, actorEmail, keyword, fromDate, toDate, sortDirection);
        List<OrgAuditEventVo> events = auditService.listByOrg(
                orgId,
                filters.limit(),
                filters.eventType(),
                filters.actorEmail(),
                filters.keyword(),
                filters.fromAt(),
                filters.toAt(),
                filters.ascending()
        );
        auditService.record(userId, "ORG_AUDIT_EXPORT", buildExportAuditDetail(orgId, events.size(), filters), ipAddress, orgId);
        return new CsvExportFile(buildFileName(orgId), buildCsvBytes(events));
    }

    public JsonlExportFile exportJsonlEvents(
            Long userId,
            Long orgId,
            Integer limit,
            String eventTypes,
            String cursor,
            String fromDate,
            String toDate,
            String sortDirection,
            String ipAddress
    ) {
        orgAccessService.requireManageMember(userId, orgId);
        AuditExportFilters filters = buildExportFilters(limit, eventTypes, cursor, fromDate, toDate, sortDirection);
        List<OrgAuditEventVo> events = auditService.listByOrgForExport(
                orgId,
                filters.limit(),
                filters.eventTypes(),
                filters.cursor(),
                filters.fromAt(),
                filters.toAt(),
                filters.ascending()
        );
        auditService.record(userId, "ORG_AUDIT_JSONL_EXPORT", buildJsonlAuditDetail(orgId, events.size(), filters), ipAddress, orgId);
        return new JsonlExportFile(buildJsonlFileName(orgId), buildJsonlBytes(events));
    }

    public int maxLimit() {
        return MAX_LIMIT;
    }

    private AuditFilters buildFilters(
            Integer limit,
            String eventType,
            String actorEmail,
            String keyword,
            String fromDate,
            String toDate,
            String sortDirection
    ) {
        LocalDateTime fromAt = parseFromDate(fromDate);
        LocalDateTime toAt = parseToDate(toDate);
        validateDateRange(fromAt, toAt);
        return new AuditFilters(
                normalizeLimit(limit),
                normalizeText(eventType),
                normalizeText(actorEmail),
                normalizeText(keyword),
                fromAt,
                toAt,
                isAscending(sortDirection)
        );
    }

    private AuditExportFilters buildExportFilters(
            Integer limit,
            String eventTypes,
            String cursor,
            String fromDate,
            String toDate,
            String sortDirection
    ) {
        LocalDateTime fromAt = parseFromDate(fromDate);
        LocalDateTime toAt = parseToDate(toDate);
        validateDateRange(fromAt, toAt);
        return new AuditExportFilters(
                normalizeLimit(limit),
                parseEventTypes(eventTypes),
                parseCursor(cursor),
                fromAt,
                toAt,
                isAscending(sortDirection)
        );
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private boolean isAscending(String sortDirection) {
        return "ASC".equalsIgnoreCase(normalizeText(sortDirection));
    }

    private LocalDateTime parseFromDate(String fromDate) {
        if (!StringUtils.hasText(fromDate)) {
            return null;
        }
        return LocalDate.parse(fromDate.trim()).atStartOfDay();
    }

    private LocalDateTime parseToDate(String toDate) {
        if (!StringUtils.hasText(toDate)) {
            return null;
        }
        return LocalDate.parse(toDate.trim()).atTime(LocalTime.MAX);
    }

    private Set<String> parseEventTypes(String eventTypes) {
        if (!StringUtils.hasText(eventTypes)) {
            return Set.of();
        }
        Set<String> parsed = new LinkedHashSet<>();
        Arrays.stream(eventTypes.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(parsed::add);
        return parsed;
    }

    private Long parseCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return null;
        }
        try {
            return Long.parseLong(cursor.trim());
        } catch (NumberFormatException exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "cursor must be a numeric audit event id");
        }
    }

    private void validateDateRange(LocalDateTime fromAt, LocalDateTime toAt) {
        if (fromAt != null && toAt != null && fromAt.isAfter(toAt)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "fromDate must be on or before toDate");
        }
    }

    private String buildListAuditDetail(Long orgId, int count, AuditFilters filters) {
        return "orgId=" + orgId
                + ",count=" + count
                + ",direction=" + (filters.ascending() ? "ASC" : "DESC")
                + ",fromDate=" + nullToDash(filters.fromAt())
                + ",toDate=" + nullToDash(filters.toAt());
    }

    private String buildExportAuditDetail(Long orgId, int count, AuditFilters filters) {
        return "orgId=" + orgId
                + ",count=" + count
                + ",direction=" + (filters.ascending() ? "ASC" : "DESC")
                + ",eventType=" + nullToDash(filters.eventType())
                + ",fromDate=" + nullToDash(filters.fromAt())
                + ",toDate=" + nullToDash(filters.toAt());
    }

    private String buildJsonlAuditDetail(Long orgId, int count, AuditExportFilters filters) {
        return "orgId=" + orgId
                + ",count=" + count
                + ",direction=" + (filters.ascending() ? "ASC" : "DESC")
                + ",eventTypes=" + nullToDash(filters.eventTypes())
                + ",cursor=" + nullToDash(filters.cursor());
    }

    private String buildFileName(Long orgId) {
        return "organization-audit-" + orgId + "-" + LocalDateTime.now().format(FILE_TIME_FORMAT) + ".csv";
    }

    private String buildJsonlFileName(Long orgId) {
        return "organization-audit-" + orgId + "-" + LocalDateTime.now().format(FILE_TIME_FORMAT) + ".jsonl";
    }

    private byte[] buildCsvBytes(List<OrgAuditEventVo> events) {
        StringBuilder builder = new StringBuilder("id,orgId,actorEmail,eventType,ipAddress,detail,createdAt\n");
        for (OrgAuditEventVo event : events) {
            builder.append(csvCell(event.id())).append(',')
                    .append(csvCell(event.orgId())).append(',')
                    .append(csvCell(event.actorEmail())).append(',')
                    .append(csvCell(event.eventType())).append(',')
                    .append(csvCell(event.ipAddress())).append(',')
                    .append(csvCell(event.detail())).append(',')
                    .append(csvCell(event.createdAt()))
                    .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] buildJsonlBytes(List<OrgAuditEventVo> events) {
        StringBuilder builder = new StringBuilder();
        for (OrgAuditEventVo event : events) {
            builder.append(toJsonLine(event)).append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String toJsonLine(OrgAuditEventVo event) {
        try {
            return objectMapper.writeValueAsString(jsonLineMap(event));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize audit JSONL event", exception);
        }
    }

    private Map<String, Object> jsonLineMap(OrgAuditEventVo event) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("schemaVersion", JSONL_SCHEMA_VERSION);
        row.put("source", JSONL_SOURCE);
        row.put("id", event.id());
        row.put("cursor", event.id());
        row.put("orgId", event.orgId());
        row.put("actorId", event.actorId());
        row.put("actorEmail", event.actorEmail());
        row.put("eventType", event.eventType());
        row.put("targetType", event.targetType());
        row.put("targetId", event.targetId());
        row.put("severity", event.severity());
        row.put("ipAddress", event.ipAddress());
        row.put("detail", event.detail());
        row.put("createdAt", event.createdAt() == null ? null : event.createdAt().toString());
        return row;
    }

    private String csvCell(Object value) {
        String normalized = value == null ? "" : String.valueOf(value);
        String escaped = normalized.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String nullToDash(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private record AuditFilters(
            int limit,
            String eventType,
            String actorEmail,
            String keyword,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            boolean ascending
    ) {
    }

    private record AuditExportFilters(
            int limit,
            Set<String> eventTypes,
            Long cursor,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            boolean ascending
    ) {
    }

    public record CsvExportFile(String fileName, byte[] content) {
    }

    public record JsonlExportFile(String fileName, byte[] content) {
        public String contentAsString() {
            return new String(content, StandardCharsets.UTF_8);
        }
    }
}
