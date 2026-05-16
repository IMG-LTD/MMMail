package com.mmmail.server.commercial;

public record EditionContext(Long orgId, Edition edition) {

    public EditionContext {
        if (orgId == null) {
            throw new IllegalArgumentException("orgId is required");
        }
        if (edition == null) {
            throw new IllegalArgumentException("edition is required");
        }
    }
}
