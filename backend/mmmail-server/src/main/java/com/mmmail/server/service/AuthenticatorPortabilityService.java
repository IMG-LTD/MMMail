package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.AuthenticatorEntryMapper;
import com.mmmail.server.model.dto.CreateAuthenticatorBackupRequest;
import com.mmmail.server.model.dto.ImportAuthenticatorBackupRequest;
import com.mmmail.server.model.dto.ImportAuthenticatorEntriesRequest;
import com.mmmail.server.model.entity.AuthenticatorEntry;
import com.mmmail.server.model.vo.AuthenticatorBackupVo;
import com.mmmail.server.model.vo.AuthenticatorExportVo;
import com.mmmail.server.model.vo.AuthenticatorImportResultVo;
import com.mmmail.server.model.vo.AuthenticatorEntrySummaryVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthenticatorPortabilityService {

    private static final String EXPORT_FORMAT = "MMMAIL_JSON";
    private static final String EXPORT_MARKER = "MMMAIL_AUTHENTICATOR_EXPORT";
    private static final String BACKUP_MARKER = "MMMAIL_AUTHENTICATOR_BACKUP";
    private static final String FORMAT_AUTO = "AUTO";
    private static final String FORMAT_OTPAUTH = "OTPAUTH_URI";
    private static final String FORMAT_JSON = "MMMAIL_JSON";
    private static final int EXPORT_VERSION = 1;
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final AuthenticatorEntryMapper authenticatorEntryMapper;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final AuthenticatorEntrySupport entrySupport;
    private final AuthenticatorBackupCodec backupCodec;
    private final AuthenticatorSecurityPreferenceService securityPreferenceService;

    public AuthenticatorPortabilityService(
            AuthenticatorEntryMapper authenticatorEntryMapper,
            AuditService auditService,
            ObjectMapper objectMapper,
            AuthenticatorEntrySupport entrySupport,
            AuthenticatorBackupCodec backupCodec,
            AuthenticatorSecurityPreferenceService securityPreferenceService
    ) {
        this.authenticatorEntryMapper = authenticatorEntryMapper;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.entrySupport = entrySupport;
        this.backupCodec = backupCodec;
        this.securityPreferenceService = securityPreferenceService;
    }

    @Transactional
    public AuthenticatorImportResultVo importEntries(
            Long userId,
            ImportAuthenticatorEntriesRequest request,
            String ipAddress
    ) {
        List<PortableAuthenticatorEntry> parsed = parseImportContent(request.format(), request.content());
        List<AuthenticatorEntrySummaryVo> importedEntries = upsertEntries(userId, parsed);
        auditService.record(
                userId,
                "AUTH_IMPORT",
                "count=" + importedEntries.size() + ",format=" + normalizeFormat(request.format(), request.content()),
                ipAddress
        );
        securityPreferenceService.markSynced(userId, LocalDateTime.now());
        return new AuthenticatorImportResultVo(importedEntries.size(), parsed.size(), importedEntries);
    }

    public AuthenticatorExportVo exportEntries(Long userId, String ipAddress) {
        List<AuthenticatorEntry> entries = loadEntries(userId);
        LocalDateTime exportedAt = LocalDateTime.now();
        String content = serializeExport(entries, exportedAt);
        auditService.record(userId, "AUTH_EXPORT", "count=" + entries.size() + ",format=" + EXPORT_FORMAT, ipAddress);
        return new AuthenticatorExportVo(
                EXPORT_FORMAT,
                buildFileName("authenticator-export", exportedAt, "json"),
                content,
                entries.size(),
                exportedAt
        );
    }

    public AuthenticatorBackupVo exportEncryptedBackup(
            Long userId,
            CreateAuthenticatorBackupRequest request,
            String ipAddress
    ) {
        List<AuthenticatorEntry> entries = loadEntries(userId);
        LocalDateTime exportedAt = LocalDateTime.now();
        String plainExport = serializeExport(entries, exportedAt);
        AuthenticatorBackupCodec.EncryptedPayload encrypted = backupCodec.encrypt(plainExport, request.passphrase());
        String content = serializeBackup(entries.size(), exportedAt, encrypted);
        auditService.record(userId, "AUTH_BACKUP_EXPORT", "count=" + entries.size(), ipAddress);
        securityPreferenceService.markBackedUp(userId, exportedAt);
        return new AuthenticatorBackupVo(
                buildFileName("authenticator-backup", exportedAt, "bak"),
                content,
                entries.size(),
                encrypted.encryption(),
                exportedAt
        );
    }

    @Transactional
    public AuthenticatorImportResultVo importEncryptedBackup(
            Long userId,
            ImportAuthenticatorBackupRequest request,
            String ipAddress
    ) {
        BackupDocument backup = parseBackupDocument(request.content());
        String plainExport = backupCodec.decrypt(backup.envelope(), request.passphrase());
        List<PortableAuthenticatorEntry> parsed = parseJsonEntries(plainExport);
        List<AuthenticatorEntrySummaryVo> importedEntries = upsertEntries(userId, parsed);
        auditService.record(userId, "AUTH_BACKUP_IMPORT", "count=" + importedEntries.size(), ipAddress);
        securityPreferenceService.markSynced(userId, LocalDateTime.now());
        return new AuthenticatorImportResultVo(importedEntries.size(), parsed.size(), importedEntries);
    }

    private List<AuthenticatorEntry> loadEntries(Long userId) {
        return authenticatorEntryMapper.selectList(new LambdaQueryWrapper<AuthenticatorEntry>()
                .eq(AuthenticatorEntry::getOwnerId, userId)
                .orderByAsc(AuthenticatorEntry::getIssuer)
                .orderByAsc(AuthenticatorEntry::getAccountName));
    }

    private List<AuthenticatorEntrySummaryVo> upsertEntries(Long userId, List<PortableAuthenticatorEntry> parsed) {
        LocalDateTime now = LocalDateTime.now();
        List<AuthenticatorEntrySummaryVo> importedEntries = new ArrayList<>();
        for (PortableAuthenticatorEntry item : dedupe(parsed)) {
            AuthenticatorEntrySummaryVo imported = upsertEntry(userId, item, now);
            importedEntries.add(imported);
        }
        return importedEntries;
    }

    private AuthenticatorEntrySummaryVo upsertEntry(Long userId, PortableAuthenticatorEntry item, LocalDateTime now) {
        AuthenticatorEntrySupport.NormalizedAuthenticatorEntry normalized = entrySupport.normalize(
                item.issuer(),
                item.accountName(),
                item.secretCiphertext(),
                item.algorithm(),
                item.digits(),
                item.periodSeconds()
        );
        AuthenticatorEntry existing = authenticatorEntryMapper.selectOne(new LambdaQueryWrapper<AuthenticatorEntry>()
                .eq(AuthenticatorEntry::getOwnerId, userId)
                .eq(AuthenticatorEntry::getIssuer, normalized.issuer())
                .eq(AuthenticatorEntry::getAccountName, normalized.accountName())
                .last("limit 1"));
        if (existing == null) {
            AuthenticatorEntry created = entrySupport.create(userId, normalized, now);
            authenticatorEntryMapper.insert(created);
            return entrySupport.toSummaryVo(created);
        }
        entrySupport.apply(existing, normalized, now);
        authenticatorEntryMapper.updateById(existing);
        return entrySupport.toSummaryVo(existing);
    }

    private List<PortableAuthenticatorEntry> parseImportContent(String format, String content) {
        String normalizedFormat = normalizeFormat(format, content);
        return switch (normalizedFormat) {
            case FORMAT_JSON -> parseJsonEntries(content);
            case FORMAT_OTPAUTH -> parseOtpauthEntries(content);
            default -> throw new BizException(ErrorCode.AUTHENTICATOR_IMPORT_INVALID, "Unsupported authenticator import format");
        };
    }

    private String normalizeFormat(String format, String content) {
        String candidate = StringUtils.hasText(format) ? format.trim().toUpperCase(Locale.ROOT) : FORMAT_AUTO;
        if (!FORMAT_AUTO.equals(candidate)) {
            return candidate;
        }
        String trimmed = content.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[") ? FORMAT_JSON : FORMAT_OTPAUTH;
    }

    private List<PortableAuthenticatorEntry> parseJsonEntries(String content) {
        JsonNode root = readTree(content, ErrorCode.AUTHENTICATOR_IMPORT_INVALID, "Authenticator import JSON is invalid");
        JsonNode entriesNode = resolveEntriesNode(root);
        if (!entriesNode.isArray()) {
            throw new BizException(ErrorCode.AUTHENTICATOR_IMPORT_INVALID, "Authenticator import entries are invalid");
        }
        List<PortableAuthenticatorEntry> entries = new ArrayList<>();
        for (JsonNode item : entriesNode) {
            entries.add(new PortableAuthenticatorEntry(
                    readText(item, "issuer"),
                    readText(item, "accountName"),
                    readText(item, "secretCiphertext", "secret"),
                    readOptionalText(item, "algorithm"),
                    readOptionalInt(item, "digits"),
                    readOptionalInt(item, "periodSeconds")
            ));
        }
        validateImportedEntries(entries);
        return entries;
    }

    private JsonNode resolveEntriesNode(JsonNode root) {
        if (root.isArray()) {
            return root;
        }
        if (EXPORT_MARKER.equals(root.path("format").asText())) {
            return root.path("entries");
        }
        if (root.has("entries")) {
            return root.path("entries");
        }
        throw new BizException(ErrorCode.AUTHENTICATOR_IMPORT_INVALID, "Authenticator import entries are invalid");
    }

    private List<PortableAuthenticatorEntry> parseOtpauthEntries(String content) {
        List<PortableAuthenticatorEntry> entries = new ArrayList<>();
        for (String line : splitNonBlankLines(content)) {
            entries.add(parseOtpauthUri(line));
        }
        validateImportedEntries(entries);
        return entries;
    }

    private PortableAuthenticatorEntry parseOtpauthUri(String line) {
        try {
            URI uri = URI.create(line.trim());
            if (!"otpauth".equalsIgnoreCase(uri.getScheme()) || !"totp".equalsIgnoreCase(uri.getHost())) {
                throw invalidImport("Authenticator import only supports otpauth://totp URIs");
            }
            Map<String, String> query = parseQuery(uri.getRawQuery());
            String label = decode(stripLeadingSlash(uri.getRawPath()));
            String issuer = resolveIssuer(label, query.get("issuer"));
            String accountName = resolveAccountName(label);
            return new PortableAuthenticatorEntry(
                    issuer,
                    accountName,
                    query.get("secret"),
                    query.get("algorithm"),
                    parseOptionalInteger(query.get("digits")),
                    parseOptionalInteger(query.get("period"))
            );
        } catch (IllegalArgumentException ex) {
            throw invalidImport("Authenticator import URI is invalid");
        }
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> values = new LinkedHashMap<>();
        if (!StringUtils.hasText(rawQuery)) {
            return values;
        }
        for (String pair : rawQuery.split("&")) {
            String[] tokens = pair.split("=", 2);
            String key = decode(tokens[0]);
            String value = tokens.length > 1 ? decode(tokens[1]) : "";
            values.put(key, value);
        }
        return values;
    }

    private String resolveIssuer(String label, String issuerParam) {
        if (StringUtils.hasText(issuerParam)) {
            return issuerParam;
        }
        int separator = label.indexOf(':');
        return separator > 0 ? label.substring(0, separator) : label;
    }

    private String resolveAccountName(String label) {
        int separator = label.indexOf(':');
        return separator > -1 ? label.substring(separator + 1) : label;
    }

    private List<String> splitNonBlankLines(String content) {
        List<String> lines = new ArrayList<>();
        for (String rawLine : content.replace("\r\n", "\n").split("\n")) {
            if (StringUtils.hasText(rawLine)) {
                lines.add(rawLine.trim());
            }
        }
        return lines;
    }

    private void validateImportedEntries(List<PortableAuthenticatorEntry> entries) {
        if (entries.isEmpty()) {
            throw new BizException(ErrorCode.AUTHENTICATOR_IMPORT_INVALID, "Authenticator import entries are required");
        }
    }

    private String serializeExport(List<AuthenticatorEntry> entries, LocalDateTime exportedAt) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("format", EXPORT_MARKER);
        root.put("version", EXPORT_VERSION);
        root.put("exportedAt", exportedAt.toString());
        ArrayNode items = root.putArray("entries");
        for (AuthenticatorEntry entry : entries) {
            ObjectNode item = items.addObject();
            item.put("issuer", entry.getIssuer());
            item.put("accountName", entry.getAccountName());
            item.put("secretCiphertext", entry.getSecretCiphertext());
            item.put("algorithm", entrySupport.normalizeAlgorithm(entry.getAlgorithm(), null));
            item.put("digits", entrySupport.normalizeDigits(entry.getDigits(), null));
            item.put("periodSeconds", entrySupport.normalizePeriodSeconds(entry.getPeriodSeconds(), null));
        }
        return writeJson(root, ErrorCode.AUTHENTICATOR_EXPORT_INVALID, "Failed to export authenticator entries");
    }

    private String serializeBackup(
            int entryCount,
            LocalDateTime exportedAt,
            AuthenticatorBackupCodec.EncryptedPayload encrypted
    ) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("format", BACKUP_MARKER);
        root.put("version", EXPORT_VERSION);
        root.put("exportedAt", exportedAt.toString());
        root.put("entryCount", entryCount);
        root.put("algorithm", encrypted.encryption());
        root.put("keyDerivation", encrypted.keyDerivation());
        root.put("iterations", encrypted.iterations());
        root.put("salt", encrypted.salt());
        root.put("iv", encrypted.iv());
        root.put("ciphertext", encrypted.ciphertext());
        return writeJson(root, ErrorCode.AUTHENTICATOR_BACKUP_INVALID, "Failed to export authenticator backup");
    }

    private BackupDocument parseBackupDocument(String content) {
        JsonNode root = readTree(content, ErrorCode.AUTHENTICATOR_BACKUP_INVALID, "Authenticator backup is invalid");
        if (!BACKUP_MARKER.equals(root.path("format").asText())) {
            throw new BizException(ErrorCode.AUTHENTICATOR_BACKUP_INVALID, "Authenticator backup format is invalid");
        }
        return new BackupDocument(
                new AuthenticatorBackupCodec.BackupEnvelope(
                        readText(root, "salt"),
                        readText(root, "iv"),
                        readText(root, "ciphertext")
                )
        );
    }

    private JsonNode readTree(String content, ErrorCode errorCode, String message) {
        try {
            return objectMapper.readTree(content);
        } catch (JsonProcessingException ex) {
            throw new BizException(errorCode, message);
        }
    }

    private String writeJson(ObjectNode node, ErrorCode errorCode, String message) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new BizException(errorCode, message);
        }
    }

    private String buildFileName(String prefix, LocalDateTime exportedAt, String extension) {
        return prefix + "-" + exportedAt.format(FILE_TIME_FORMAT) + "." + extension;
    }

    private List<PortableAuthenticatorEntry> dedupe(List<PortableAuthenticatorEntry> parsed) {
        Map<String, PortableAuthenticatorEntry> deduped = new LinkedHashMap<>();
        for (PortableAuthenticatorEntry item : parsed) {
            deduped.put(item.issuer() + "\u0000" + item.accountName(), item);
        }
        return new ArrayList<>(deduped.values());
    }

    private String readText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText();
        if (!StringUtils.hasText(value)) {
            throw invalidImport("Authenticator import field `" + fieldName + "` is required");
        }
        return value;
    }

    private String readText(JsonNode node, String primary, String fallback) {
        String value = node.path(primary).asText();
        if (StringUtils.hasText(value)) {
            return value;
        }
        return readText(node, fallback);
    }

    private String readOptionalText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText();
        return StringUtils.hasText(value) ? value : null;
    }

    private Integer readOptionalInt(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.path(fieldName).asInt() : null;
    }

    private Integer parseOptionalInteger(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException ex) {
            throw invalidImport("Authenticator import numeric field is invalid");
        }
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String stripLeadingSlash(String value) {
        return value != null && value.startsWith("/") ? value.substring(1) : value;
    }

    private BizException invalidImport(String message) {
        return new BizException(ErrorCode.AUTHENTICATOR_IMPORT_INVALID, message);
    }

    private record PortableAuthenticatorEntry(
            String issuer,
            String accountName,
            String secretCiphertext,
            String algorithm,
            Integer digits,
            Integer periodSeconds
    ) {
    }

    private record BackupDocument(AuthenticatorBackupCodec.BackupEnvelope envelope) {
    }
}
