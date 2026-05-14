package com.mmmail.labs;

public record LabsModuleDescriptor(
        String key,
        String label,
        String description,
        boolean enabled,
        String maturity,
        boolean premium,
        boolean hosted,
        String updatedAt
) {
}
