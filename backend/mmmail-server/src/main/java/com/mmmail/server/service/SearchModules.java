package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class SearchModules {
    static final String ALL = "all";
    static final List<String> SUPPORTED = List.of("mail", "doc", "sheet", "drive", "contact", "note", "community");

    private SearchModules() {
    }

    static LinkedHashSet<String> parse(String raw) {
        if (!StringUtils.hasText(raw) || ALL.equalsIgnoreCase(raw.trim())) {
            return new LinkedHashSet<>(SUPPORTED);
        }
        LinkedHashSet<String> modules = new LinkedHashSet<>();
        for (String token : raw.split(",")) {
            String module = token.trim().toLowerCase();
            if (module.isEmpty()) {
                continue;
            }
            ensureSupported(module);
            modules.add(module);
        }
        return modules.isEmpty() ? new LinkedHashSet<>(SUPPORTED) : modules;
    }

    static String normalizeOne(String raw) {
        String module = StringUtils.hasText(raw) ? raw.trim().toLowerCase() : ALL;
        if (ALL.equals(module)) {
            return ALL;
        }
        ensureSupported(module);
        return module;
    }

    static void ensureSupported(String module) {
        if (!SUPPORTED.contains(module)) {
            throw new BizException(ErrorCode.SEARCH_MODULE_UNSUPPORTED);
        }
    }

    static String aclToken(Long userId) {
        return "|" + userId + "|";
    }

    static Set<String> supportedSet() {
        return Set.copyOf(SUPPORTED);
    }
}
