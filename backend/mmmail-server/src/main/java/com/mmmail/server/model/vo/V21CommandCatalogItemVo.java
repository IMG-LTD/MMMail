package com.mmmail.server.model.vo;

import java.time.LocalDateTime;
import java.util.List;

public record V21CommandCatalogItemVo(
        String id,
        String title,
        String i18nKey,
        String group,
        String icon,
        String shortcut,
        V21CommandActionVo action,
        List<String> requires,
        boolean pinned,
        LocalDateTime lastUsedAt
) {
    public V21CommandCatalogItemVo {
        requires = List.copyOf(requires);
    }
}
