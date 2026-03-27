package com.mmmail.server.service;

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
import java.util.List;

@Service
public class OrgAuditQueryService {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 10_000;
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final OrgAccessService orgAccessService;
    private final AuditService auditService;

    public OrgAuditQueryService(OrgAccessService orgAccessService, AuditService auditService) {
        this.orgAccessService = orgAccessService;
        this.auditService = auditService;
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

    private String buildFileName(Long orgId) {
        return "organization-audit-" + orgId + "-" + LocalDateTime.now().format(FILE_TIME_FORMAT) + ".csv";
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

    public record CsvExportFile(String fileName, byte[] content) {
    }
}
