package com.mmmail.server.access;

import com.mmmail.platform.contract.V21ApiContract;
import com.mmmail.platform.contract.V21ApiContractCatalog;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class V21ApiContractMatcher {

    private final List<RoutePattern> routePatterns;

    public V21ApiContractMatcher() {
        this(V21ApiContractCatalog.defaultCatalog().contracts());
    }

    V21ApiContractMatcher(List<V21ApiContract> contracts) {
        this.routePatterns = contracts.stream()
                .map(RoutePattern::fromContract)
                .toList();
    }

    public Optional<V21ApiContract> match(String method, String path) {
        String normalizedMethod = normalizeMethod(method);
        String normalizedPath = normalizePath(path);
        return routePatterns.stream()
                .filter(pattern -> pattern.matches(normalizedMethod, normalizedPath))
                .map(RoutePattern::contract)
                .findFirst();
    }

    private static String normalizeMethod(String method) {
        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("HTTP method is required");
        }
        return method.trim().toUpperCase();
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Request path is required");
        }
        String normalized = path.trim();
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private record RoutePattern(V21ApiContract contract, List<String> segments) {

        private static RoutePattern fromContract(V21ApiContract contract) {
            return new RoutePattern(contract, split(contract.path()));
        }

        private boolean matches(String method, String path) {
            if (!contract.method().equalsIgnoreCase(method)) {
                return false;
            }
            List<String> pathSegments = split(path);
            if (segments.size() != pathSegments.size()) {
                return false;
            }
            return segmentMatches(pathSegments);
        }

        private boolean segmentMatches(List<String> pathSegments) {
            for (int index = 0; index < segments.size(); index++) {
                if (!segmentMatches(segments.get(index), pathSegments.get(index))) {
                    return false;
                }
            }
            return true;
        }

        private static boolean segmentMatches(String patternSegment, String pathSegment) {
            return patternSegment.startsWith(":") || patternSegment.equals(pathSegment);
        }

        private static List<String> split(String path) {
            String trimmed = path.startsWith("/") ? path.substring(1) : path;
            if (trimmed.isBlank()) {
                return List.of();
            }
            return Arrays.stream(trimmed.split("/"))
                    .filter(segment -> !segment.isBlank())
                    .toList();
        }
    }
}
