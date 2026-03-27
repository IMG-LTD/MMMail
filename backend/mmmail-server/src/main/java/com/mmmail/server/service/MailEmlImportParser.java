package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MailEmlImportParser {

    private static final String DEFAULT_SUBJECT = "Imported message";
    private static final Pattern ANGLE_ADDRESS_PATTERN = Pattern.compile("<([^>]+)>");
    private static final Pattern SIMPLE_ADDRESS_PATTERN = Pattern.compile("([A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,})", Pattern.CASE_INSENSITIVE);
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
    );

    public ParsedEmlMessage parse(String content, String fallbackTimezone) {
        if (!StringUtils.hasText(content)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail EML content is required");
        }
        SplitContent split = splitHeadersAndBody(content);
        Map<String, String> headers = parseHeaders(split.headers());
        String senderEmail = parseSenderEmail(headers.get("from"));
        String subject = StringUtils.hasText(headers.get("subject")) ? headers.get("subject").trim() : DEFAULT_SUBJECT;
        LocalDateTime sentAt = parseSentAt(headers.get("date"), fallbackTimezone);
        return new ParsedEmlMessage(senderEmail, subject, split.body(), sentAt);
    }

    private SplitContent splitHeadersAndBody(String content) {
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        int delimiterIndex = normalized.indexOf("\n\n");
        if (delimiterIndex < 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "EML content is missing headers");
        }
        String headers = normalized.substring(0, delimiterIndex);
        String body = normalized.substring(delimiterIndex + 2).trim();
        return new SplitContent(headers, body);
    }

    private Map<String, String> parseHeaders(String headersBlock) {
        String[] lines = headersBlock.split("\n");
        Map<String, String> headers = new LinkedHashMap<>();
        String currentHeader = null;
        for (String rawLine : lines) {
            if (rawLine.startsWith(" ") || rawLine.startsWith("\t")) {
                if (currentHeader == null) {
                    continue;
                }
                headers.put(currentHeader, headers.get(currentHeader) + " " + rawLine.trim());
                continue;
            }
            int delimiterIndex = rawLine.indexOf(':');
            if (delimiterIndex <= 0) {
                continue;
            }
            currentHeader = rawLine.substring(0, delimiterIndex).trim().toLowerCase(Locale.ROOT);
            headers.put(currentHeader, rawLine.substring(delimiterIndex + 1).trim());
        }
        return headers;
    }

    private String parseSenderEmail(String rawFrom) {
        if (!StringUtils.hasText(rawFrom)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "EML content is missing From header");
        }
        Matcher angleMatcher = ANGLE_ADDRESS_PATTERN.matcher(rawFrom);
        if (angleMatcher.find()) {
            return angleMatcher.group(1).trim().toLowerCase(Locale.ROOT);
        }
        Matcher simpleMatcher = SIMPLE_ADDRESS_PATTERN.matcher(rawFrom);
        if (simpleMatcher.find()) {
            return simpleMatcher.group(1).trim().toLowerCase(Locale.ROOT);
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unable to parse sender email from EML");
    }

    private LocalDateTime parseSentAt(String rawDate, String fallbackTimezone) {
        if (!StringUtils.hasText(rawDate)) {
            return LocalDateTime.now(ZoneId.of(fallbackTimezone));
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                ZonedDateTime zoned = ZonedDateTime.parse(rawDate.trim(), formatter);
                return zoned.withZoneSameInstant(ZoneId.of(fallbackTimezone)).toLocalDateTime();
            } catch (RuntimeException ex) {
                // Try next supported date format.
            }
        }
        try {
            return LocalDateTime.parse(rawDate.trim());
        } catch (DateTimeParseException ex) {
            return LocalDateTime.now(ZoneId.of(fallbackTimezone));
        }
    }

    private record SplitContent(String headers, String body) {
    }

    public record ParsedEmlMessage(
            String senderEmail,
            String subject,
            String body,
            LocalDateTime sentAt
    ) {
    }
}
