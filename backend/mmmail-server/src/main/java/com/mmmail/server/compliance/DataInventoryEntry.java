package com.mmmail.server.compliance;

import java.util.List;

public record DataInventoryEntry(
        String tableName,
        String subjectColumn,
        DsrSubjectRef subjectRef,
        DsrExportStrategy exportStrategy,
        DsrDeleteStrategy deleteStrategy,
        List<String> anonymizeColumns
) {

    public DataInventoryEntry {
        requireText(tableName, "tableName");
        if (subjectRef == null || exportStrategy == null || deleteStrategy == null) {
            throw new IllegalArgumentException("inventory strategies are required");
        }
        if (subjectRef != DsrSubjectRef.NONE) {
            requireText(subjectColumn, "subjectColumn");
        }
        anonymizeColumns = List.copyOf(anonymizeColumns == null ? List.of() : anonymizeColumns);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
