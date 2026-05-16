package com.mmmail.server.compliance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.platform.jobs.JobRunRecord;
import com.mmmail.platform.jobs.JobRunType;
import com.mmmail.platform.jobs.TypedJobRunHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DsrExecutionService {

    private static final int EXPORT_ROW_LIMIT = 1_000;

    private final JdbcTemplate jdbcTemplate;
    private final DataInventoryCatalog catalog;
    private final ObjectMapper objectMapper;

    public DsrExecutionService(JdbcTemplate jdbcTemplate, DataInventoryCatalog catalog, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.catalog = catalog;
        this.objectMapper = objectMapper;
    }

    public TypedJobRunHandler exportHandler() {
        return typedHandler(JobRunType.DSR_EXPORT, record -> exportSubject(readPayload(record)));
    }

    public TypedJobRunHandler erasureHandler() {
        return typedHandler(JobRunType.DSR_ERASURE, record -> eraseSubject(readPayload(record)));
    }

    public String exportSubject(DsrJobPayload payload) {
        Map<String, Object> result = baseResult("export", payload);
        Map<String, Object> tables = new LinkedHashMap<>();
        for (DataInventoryEntry entry : catalog.entries()) {
            if (entry.exportStrategy() == DsrExportStrategy.EXCLUDE_OPERATIONAL) {
                continue;
            }
            Object subjectValue = subjectValue(entry, payload);
            tables.put(entry.tableName(), exportRows(entry, subjectValue));
        }
        result.put("tables", tables);
        return writeJson(result);
    }

    public String eraseSubject(DsrJobPayload payload) {
        Map<String, Object> result = baseResult("erasure", payload);
        Map<String, Object> tables = new LinkedHashMap<>();
        for (DataInventoryEntry entry : catalog.entries()) {
            tables.put(entry.tableName(), eraseEntry(entry, payload));
        }
        result.put("tables", tables);
        return writeJson(result);
    }

    private List<Map<String, Object>> exportRows(DataInventoryEntry entry, Object subjectValue) {
        if (subjectValue == null || entry.exportStrategy() == DsrExportStrategy.METADATA_ONLY) {
            return List.of();
        }
        return jdbcTemplate.queryForList(selectSql(entry), subjectValue, EXPORT_ROW_LIMIT);
    }

    private Map<String, Object> eraseEntry(DataInventoryEntry entry, DsrJobPayload payload) {
        Object subjectValue = subjectValue(entry, payload);
        if (subjectValue == null || entry.deleteStrategy() == DsrDeleteStrategy.RETAIN) {
            return erasureResult(entry, 0);
        }
        int affected = switch (entry.deleteStrategy()) {
            case DELETE_ROWS -> jdbcTemplate.update(deleteSql(entry), subjectValue);
            case SOFT_DELETE -> jdbcTemplate.update(softDeleteSql(entry), subjectValue);
            case ANONYMIZE -> anonymize(entry, subjectValue, payload.subjectUserId());
            case RETAIN -> 0;
        };
        return erasureResult(entry, affected);
    }

    private int anonymize(DataInventoryEntry entry, Object subjectValue, Long subjectUserId) {
        if (entry.anonymizeColumns().isEmpty()) {
            throw new IllegalStateException("anonymize columns are required for " + entry.tableName());
        }
        List<Object> args = new ArrayList<>();
        for (String ignored : entry.anonymizeColumns()) {
            args.add("deleted-user-" + subjectUserId + "@dsr.local");
        }
        args.add(subjectValue);
        return jdbcTemplate.update(anonymizeSql(entry), args.toArray());
    }

    private Map<String, Object> erasureResult(DataInventoryEntry entry, int affectedRows) {
        return Map.of("strategy", entry.deleteStrategy().name(), "affectedRows", affectedRows);
    }

    private Object subjectValue(DataInventoryEntry entry, DsrJobPayload payload) {
        return switch (entry.subjectRef()) {
            case USER_ID -> payload.subjectUserId();
            case EMAIL -> payload.subjectEmail();
            case ORG_ID -> payload.orgId();
            case NONE -> null;
        };
    }

    private DsrJobPayload readPayload(JobRunRecord record) {
        try {
            return objectMapper.readValue(record.payloadJson(), DsrJobPayload.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid DSR job payload", ex);
        }
    }

    private Map<String, Object> baseResult(String action, DsrJobPayload payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemaVersion", "mmmail.dsr.v1");
        result.put("action", action);
        result.put("orgId", payload.orgId());
        result.put("subjectUserId", payload.subjectUserId());
        result.put("subjectEmail", payload.subjectEmail());
        result.put("requestedAt", payload.requestedAt());
        return result;
    }

    private TypedJobRunHandler typedHandler(JobRunType type, Handler handler) {
        return new TypedJobRunHandler() {
            @Override
            public JobRunType type() {
                return type;
            }

            @Override
            public String handle(JobRunRecord record) {
                return handler.handle(record);
            }
        };
    }

    private String selectSql(DataInventoryEntry entry) {
        return "select * from " + entry.tableName() + " where " + entry.subjectColumn() + " = ? limit ?";
    }

    private String deleteSql(DataInventoryEntry entry) {
        return "delete from " + entry.tableName() + " where " + entry.subjectColumn() + " = ?";
    }

    private String softDeleteSql(DataInventoryEntry entry) {
        return "update " + entry.tableName() + " set deleted = 1, updated_at = current_timestamp where "
                + entry.subjectColumn() + " = ?";
    }

    private String anonymizeSql(DataInventoryEntry entry) {
        return "update " + entry.tableName() + " set " + anonymizeSetClause(entry) + " where "
                + entry.subjectColumn() + " = ?";
    }

    private String anonymizeSetClause(DataInventoryEntry entry) {
        return String.join(" = ?, ", entry.anonymizeColumns()) + " = ?";
    }

    private String writeJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize DSR job result", ex);
        }
    }

    @FunctionalInterface
    private interface Handler {
        String handle(JobRunRecord record);
    }
}
