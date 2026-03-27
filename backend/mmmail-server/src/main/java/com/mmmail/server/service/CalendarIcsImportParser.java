package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.util.CalendarTimezoneResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CalendarIcsImportParser {

    private static final String EVENT_BEGIN = "BEGIN:VEVENT";
    private static final String EVENT_END = "END:VEVENT";
    private static final String DEFAULT_TITLE = "Imported event";
    private static final int DEFAULT_EVENT_DURATION_MINUTES = 60;
    private static final DateTimeFormatter LOCAL_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX");

    public List<CalendarImportDraft> parse(String content, String fallbackTimezone) {
        if (!StringUtils.hasText(content)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Calendar ICS content is required");
        }
        List<String> lines = unfoldLines(content);
        List<CalendarImportDraft> drafts = new ArrayList<>();
        List<String> buffer = new ArrayList<>();
        boolean inEvent = false;

        for (String line : lines) {
            if (EVENT_BEGIN.equalsIgnoreCase(line)) {
                inEvent = true;
                buffer.clear();
                continue;
            }
            if (EVENT_END.equalsIgnoreCase(line)) {
                if (!buffer.isEmpty()) {
                    drafts.add(parseEvent(buffer, fallbackTimezone));
                }
                inEvent = false;
                buffer = new ArrayList<>();
                continue;
            }
            if (inEvent) {
                buffer.add(line);
            }
        }

        if (inEvent) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "ICS event is missing END:VEVENT");
        }
        if (drafts.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "ICS content must include at least one VEVENT");
        }
        return drafts;
    }

    private List<String> unfoldLines(String content) {
        String[] rawLines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        List<String> lines = new ArrayList<>();
        for (String raw : rawLines) {
            if (raw.startsWith(" ") || raw.startsWith("\t")) {
                if (lines.isEmpty()) {
                    continue;
                }
                int lastIndex = lines.size() - 1;
                lines.set(lastIndex, lines.get(lastIndex) + raw.substring(1));
                continue;
            }
            if (StringUtils.hasText(raw)) {
                lines.add(raw.trim());
            }
        }
        return lines;
    }

    private CalendarImportDraft parseEvent(List<String> lines, String fallbackTimezone) {
        Map<String, PropertyValue> properties = new LinkedHashMap<>();
        for (String line : lines) {
            int delimiterIndex = line.indexOf(':');
            if (delimiterIndex <= 0) {
                continue;
            }
            String rawKey = line.substring(0, delimiterIndex);
            String value = line.substring(delimiterIndex + 1);
            String name = extractName(rawKey);
            properties.put(name, new PropertyValue(rawKey, value));
        }

        PropertyValue startProperty = properties.get("DTSTART");
        if (startProperty == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "ICS event is missing DTSTART");
        }
        ParsedDateTime start = parseDateTime(startProperty, fallbackTimezone, false);
        PropertyValue endProperty = properties.get("DTEND");
        ParsedDateTime end = endProperty == null
                ? defaultEnd(start)
                : parseDateTime(endProperty, start.timezone(), start.allDay());
        if (!end.value().isAfter(start.value())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "ICS event end time must be later than start time");
        }

        String title = normalizeText(properties.get("SUMMARY"), DEFAULT_TITLE);
        String description = normalizeText(properties.get("DESCRIPTION"), null);
        String location = normalizeText(properties.get("LOCATION"), null);

        return new CalendarImportDraft(
                title,
                description,
                location,
                start.value(),
                end.value(),
                start.allDay(),
                start.timezone()
        );
    }

    private String extractName(String rawKey) {
        int paramsIndex = rawKey.indexOf(';');
        return (paramsIndex >= 0 ? rawKey.substring(0, paramsIndex) : rawKey).trim().toUpperCase(Locale.ROOT);
    }

    private ParsedDateTime parseDateTime(PropertyValue property, String fallbackTimezone, boolean inheritedAllDay) {
        Map<String, String> params = parseParams(property.rawKey());
        String value = property.value().trim();
        boolean allDay = inheritedAllDay || "DATE".equalsIgnoreCase(params.get("VALUE"));
        String timezone = normalizeTimezone(params.get("TZID"), fallbackTimezone);
        try {
            if (allDay) {
                LocalDate date = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
                return new ParsedDateTime(date.atStartOfDay(), timezone, true);
            }
            if (value.endsWith("Z")) {
                ZonedDateTime utc = ZonedDateTime.of(LocalDateTime.parse(value, UTC_FORMAT), ZoneOffset.UTC);
                return new ParsedDateTime(utc.withZoneSameInstant(ZoneId.of(timezone)).toLocalDateTime(), timezone, false);
            }
            return new ParsedDateTime(LocalDateTime.parse(value, LOCAL_FORMAT), timezone, false);
        } catch (DateTimeParseException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "ICS datetime is invalid");
        }
    }

    private ParsedDateTime defaultEnd(ParsedDateTime start) {
        if (start.allDay()) {
            return new ParsedDateTime(start.value().plusDays(1), start.timezone(), true);
        }
        return new ParsedDateTime(start.value().plusMinutes(DEFAULT_EVENT_DURATION_MINUTES), start.timezone(), false);
    }

    private Map<String, String> parseParams(String rawKey) {
        Map<String, String> params = new LinkedHashMap<>();
        String[] parts = rawKey.split(";");
        for (int index = 1; index < parts.length; index++) {
            String part = parts[index];
            int eqIndex = part.indexOf('=');
            if (eqIndex <= 0) {
                continue;
            }
            params.put(
                    part.substring(0, eqIndex).trim().toUpperCase(Locale.ROOT),
                    part.substring(eqIndex + 1).trim()
            );
        }
        return params;
    }

    private String normalizeTimezone(String timezone, String fallbackTimezone) {
        return CalendarTimezoneResolver.normalizeOrDefault(timezone, fallbackTimezone, "ICS timezone");
    }

    private String normalizeText(PropertyValue property, String fallbackValue) {
        if (property == null || !StringUtils.hasText(property.value())) {
            return fallbackValue;
        }
        return property.value()
                .replace("\\n", "\n")
                .replace("\\N", "\n")
                .replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\\\", "\\")
                .trim();
    }

    private record PropertyValue(String rawKey, String value) {
    }

    private record ParsedDateTime(LocalDateTime value, String timezone, boolean allDay) {
    }

    public record CalendarImportDraft(
            String title,
            String description,
            String location,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean allDay,
            String timezone
    ) {
    }
}
