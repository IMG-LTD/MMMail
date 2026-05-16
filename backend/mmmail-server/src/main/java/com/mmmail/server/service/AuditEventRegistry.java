package com.mmmail.server.service;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AuditEventRegistry {

    private static final List<AuditEventSpec> REGISTERED_SPECS = List.of(
            new AuditEventSpec("wallet.tx.send", "txId", "high"),
            new AuditEventSpec("wallet.tx.sign", "txId", "critical"),
            new AuditEventSpec("wallet.account.recover", "accountId", "critical"),
            new AuditEventSpec("meet.host.transfer", "roomId", "medium"),
            new AuditEventSpec("domain.add", "domainId", "high"),
            new AuditEventSpec("domain.delete", "domainId", "high"),
            new AuditEventSpec("totp.entry.add", "entryId", "medium"),
            new AuditEventSpec("totp.security.update", "userId", "high"),
            new AuditEventSpec("community.post.delete", "postId", "medium"),
            new AuditEventSpec("community.report.actioned", "reportId", "medium"),
            new AuditEventSpec("auth.login.high_risk", "sessionId", "high"),
            new AuditEventSpec("billing.subscription.action", "orgId", "high"),
            new AuditEventSpec("webpush.subscription.delete", "subscriptionId", "low")
    );

    private static final Map<String, AuditEventSpec> SPECS_BY_TYPE = specsByType();
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("WALLET_TX_SEND", "wallet.tx.send"),
            Map.entry("WALLET_TX_SIGN", "wallet.tx.sign"),
            Map.entry("WALLET_ACCOUNT_IMPORT", "wallet.account.recover"),
            Map.entry("WALLET_RECOVERY_REVEAL", "wallet.account.recover"),
            Map.entry("MEET_HOST_TRANSFER", "meet.host.transfer"),
            Map.entry("ORG_DOMAIN_ADD", "domain.add"),
            Map.entry("ORG_DOMAIN_REMOVE", "domain.delete"),
            Map.entry("AUTH_ENTRY_CREATE", "totp.entry.add"),
            Map.entry("AUTH_SECURITY_UPDATE", "totp.security.update"),
            Map.entry("COMMUNITY_POST_DELETE", "community.post.delete"),
            Map.entry("COMMUNITY_REPORT_ACTION", "community.report.actioned"),
            Map.entry("LOGIN_HIGH_RISK", "auth.login.high_risk"),
            Map.entry("SUITE_BILLING_SUBSCRIPTION_ACTION", "billing.subscription.action"),
            Map.entry("SUITE_WEB_PUSH_SUBSCRIPTION_DELETE", "webpush.subscription.delete")
    );
    private static final Map<String, List<String>> TARGET_KEYS = Map.ofEntries(
            Map.entry("wallet.tx.send", List.of("transactionId", "txId")),
            Map.entry("wallet.tx.sign", List.of("transactionId", "txId")),
            Map.entry("wallet.account.recover", List.of("accountId")),
            Map.entry("meet.host.transfer", List.of("roomId")),
            Map.entry("domain.add", List.of("domainId", "domain")),
            Map.entry("domain.delete", List.of("domainId", "domain")),
            Map.entry("totp.entry.add", List.of("entryId")),
            Map.entry("totp.security.update", List.of("userId")),
            Map.entry("community.post.delete", List.of("postId")),
            Map.entry("community.report.actioned", List.of("reportId")),
            Map.entry("auth.login.high_risk", List.of("sessionId")),
            Map.entry("billing.subscription.action", List.of("orgId")),
            Map.entry("webpush.subscription.delete", List.of("subscriptionId", "endpointHash"))
    );

    private AuditEventRegistry() {
    }

    public static Map<String, AuditEventSpec> registeredSpecsByType() {
        return SPECS_BY_TYPE;
    }

    public static AuditEventSpec resolve(String eventType) {
        return SPECS_BY_TYPE.get(canonicalType(eventType));
    }

    public static String canonicalType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return eventType;
        }
        String trimmed = eventType.trim();
        return ALIASES.getOrDefault(trimmed, trimmed);
    }

    public static String targetIdFor(String eventType, String explicitTargetId, String detail) {
        if (StringUtils.hasText(explicitTargetId)) {
            return explicitTargetId.trim();
        }
        List<String> keys = TARGET_KEYS.get(canonicalType(eventType));
        if (keys == null || keys.isEmpty()) {
            return null;
        }
        return keys.stream()
                .map(key -> detailValue(detail, key))
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private static Map<String, AuditEventSpec> specsByType() {
        Map<String, AuditEventSpec> result = new LinkedHashMap<>();
        REGISTERED_SPECS.forEach(spec -> result.put(spec.eventType(), spec));
        return Map.copyOf(result);
    }

    private static String detailValue(String detail, String key) {
        if (!StringUtils.hasText(detail) || !StringUtils.hasText(key)) {
            return null;
        }
        String token = key + "=";
        int start = detail.indexOf(token);
        if (start < 0) {
            return null;
        }
        int valueStart = start + token.length();
        int valueEnd = detail.indexOf(',', valueStart);
        String value = detail.substring(valueStart, valueEnd < 0 ? detail.length() : valueEnd);
        return value.trim();
    }
}
