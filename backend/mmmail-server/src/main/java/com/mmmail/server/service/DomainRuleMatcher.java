package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

final class DomainRuleMatcher {

    private static final Pattern SIMPLE_DOMAIN_PATTERN = Pattern.compile(
            "^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$"
    );
    private static final String WILDCARD_PREFIX = "*.";

    private DomainRuleMatcher() {
    }

    static String normalizeRulePattern(String domainPattern) {
        String normalized = normalizeInput(domainPattern, "Domain is required");
        if (normalized.startsWith(WILDCARD_PREFIX)) {
            String suffix = normalized.substring(WILDCARD_PREFIX.length());
            if (!SIMPLE_DOMAIN_PATTERN.matcher(suffix).matches()) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid domain");
            }
            return WILDCARD_PREFIX + suffix;
        }
        if (!SIMPLE_DOMAIN_PATTERN.matcher(normalized).matches()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid domain");
        }
        return normalized;
    }

    static String normalizeHostDomain(String domain) {
        String normalized = normalizeInput(domain, "Domain is required");
        if (!SIMPLE_DOMAIN_PATTERN.matcher(normalized).matches()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid domain");
        }
        return normalized;
    }

    static String findMatchedRule(List<String> domainRules, String hostDomain) {
        if (!StringUtils.hasText(hostDomain) || domainRules == null || domainRules.isEmpty()) {
            return null;
        }
        String normalizedHostDomain = normalizeHostDomain(hostDomain);

        for (String rule : domainRules) {
            if (!isWildcardRule(rule) && normalizedHostDomain.equals(rule)) {
                return rule;
            }
        }

        return domainRules.stream()
                .filter(DomainRuleMatcher::isWildcardRule)
                .filter(rule -> wildcardMatches(rule, normalizedHostDomain))
                .max(Comparator.comparingInt(String::length))
                .orElse(null);
    }

    private static boolean isWildcardRule(String rule) {
        return StringUtils.hasText(rule) && rule.startsWith(WILDCARD_PREFIX);
    }

    private static boolean wildcardMatches(String wildcardRule, String hostDomain) {
        String suffix = wildcardRule.substring(WILDCARD_PREFIX.length());
        // `*.example.com` 仅匹配子域，不匹配 apex `example.com`。
        return hostDomain.endsWith("." + suffix);
    }

    private static String normalizeInput(String input, String emptyError) {
        if (!StringUtils.hasText(input)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, emptyError);
        }
        String normalized = input.trim().toLowerCase();
        if (normalized.length() > 253) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid domain");
        }
        return normalized;
    }
}
