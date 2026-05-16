package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.model.entity.CalendarEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CalendarRecurrenceService {

    private static final int DEFAULT_INTERVAL = 1;
    private static final int MAX_EXPANDED_OCCURRENCES = 400;
    private static final int MAX_QUERY_DAYS = 366;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public CalendarRecurrenceRule parseOptional(String rawRule) {
        if (!StringUtils.hasText(rawRule)) {
            return null;
        }

        Map<String, String> parts = parseParts(rawRule);
        String frequency = requirePart(parts, "FREQ");
        int interval = parseInterval(parts.get("INTERVAL"));
        Integer count = parseCount(parts.get("COUNT"));
        LocalDateTime until = parseUntil(parts.get("UNTIL"));
        Set<DayOfWeek> byDays = parseByDays(parts.get("BYDAY"));
        validateSupportedParts(parts);
        validateFrequency(frequency, byDays);

        String normalized = buildNormalizedRule(frequency, interval, count, until, byDays);
        return new CalendarRecurrenceRule(normalized, frequency, interval, until, count, byDays);
    }

    public List<CalendarEventOccurrence> expand(CalendarEvent event, LocalDateTime fromAt, LocalDateTime toAt) {
        assertSupportedWindow(fromAt, toAt);
        CalendarRecurrenceRule rule = parseOptional(event.getRrule());
        if (rule == null) {
            return singleOccurrence(event, fromAt, toAt);
        }

        List<CalendarEventOccurrence> occurrences = expandRule(event, rule, fromAt, toAt);
        occurrences.addAll(extraRdates(event, fromAt, toAt));
        Set<LocalDateTime> exdates = decodeDates(event.getRecurrenceExdatesJson());
        return occurrences.stream()
                .filter(occurrence -> !exdates.contains(occurrence.startAt()))
                .sorted(Comparator.comparing(CalendarEventOccurrence::startAt))
                .toList();
    }

    public String encodeDates(List<LocalDateTime> dates) {
        if (dates == null || dates.isEmpty()) {
            return null;
        }
        return dates.stream()
                .sorted()
                .map(DateTimeFormatter.ISO_LOCAL_DATE_TIME::format)
                .toList()
                .toString();
    }

    public LocalDateTime effectiveUntil(CalendarRecurrenceRule rule) {
        return rule == null ? null : rule.until();
    }

    public LocalDateTime splitCutoff(CalendarEvent event, LocalDateTime requestedStartAt) {
        LocalTime originalTime = event.getStartAt().toLocalTime();
        return requestedStartAt.toLocalDate().atTime(originalTime);
    }

    private Map<String, String> parseParts(String rawRule) {
        Map<String, String> parts = new LinkedHashMap<>();
        for (String token : rawRule.trim().toUpperCase(Locale.ROOT).split(";")) {
            String[] pair = token.split("=", 2);
            if (pair.length != 2 || !StringUtils.hasText(pair[0]) || !StringUtils.hasText(pair[1])) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid RRULE token: " + token);
            }
            parts.put(pair[0].trim(), pair[1].trim());
        }
        return parts;
    }

    private String requirePart(Map<String, String> parts, String key) {
        String value = parts.get(key);
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "RRULE must include " + key);
        }
        return value;
    }

    private int parseInterval(String rawInterval) {
        if (!StringUtils.hasText(rawInterval)) {
            return DEFAULT_INTERVAL;
        }
        try {
            int interval = Integer.parseInt(rawInterval);
            if (interval <= 0) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "RRULE INTERVAL must be positive");
            }
            return interval;
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "RRULE INTERVAL must be numeric");
        }
    }

    private Integer parseCount(String rawCount) {
        if (!StringUtils.hasText(rawCount)) {
            return null;
        }
        try {
            int count = Integer.parseInt(rawCount);
            if (count <= 0) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "RRULE COUNT must be positive");
            }
            return count;
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "RRULE COUNT must be numeric");
        }
    }

    private LocalDateTime parseUntil(String rawUntil) {
        if (!StringUtils.hasText(rawUntil)) {
            return null;
        }
        String normalized = rawUntil.endsWith("Z") ? rawUntil.substring(0, rawUntil.length() - 1) : rawUntil;
        try {
            return normalized.contains("T")
                    ? LocalDateTime.parse(normalized, DATE_TIME_FORMATTER)
                    : LocalDate.parse(normalized, DATE_FORMATTER).atTime(LocalTime.MAX);
        } catch (DateTimeParseException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "RRULE UNTIL is invalid");
        }
    }

    private Set<DayOfWeek> parseByDays(String rawByDays) {
        Set<DayOfWeek> days = new LinkedHashSet<>();
        if (!StringUtils.hasText(rawByDays)) {
            return days;
        }
        for (String token : rawByDays.split(",")) {
            days.add(parseDay(token.trim()));
        }
        return days;
    }

    private DayOfWeek parseDay(String token) {
        return switch (token) {
            case "MO" -> DayOfWeek.MONDAY;
            case "TU" -> DayOfWeek.TUESDAY;
            case "WE" -> DayOfWeek.WEDNESDAY;
            case "TH" -> DayOfWeek.THURSDAY;
            case "FR" -> DayOfWeek.FRIDAY;
            case "SA" -> DayOfWeek.SATURDAY;
            case "SU" -> DayOfWeek.SUNDAY;
            default -> throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported RRULE BYDAY: " + token);
        };
    }

    private void validateSupportedParts(Map<String, String> parts) {
        Set<String> supported = Set.of("FREQ", "INTERVAL", "UNTIL", "COUNT", "BYDAY");
        for (String key : parts.keySet()) {
            if (!supported.contains(key)) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported RRULE part: " + key);
            }
        }
    }

    private void validateFrequency(String frequency, Set<DayOfWeek> byDays) {
        Set<String> supported = Set.of("DAILY", "WEEKLY", "MONTHLY", "YEARLY");
        if (!supported.contains(frequency)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported RRULE FREQ: " + frequency);
        }
        if (!"WEEKLY".equals(frequency) && !byDays.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "RRULE BYDAY is currently supported for WEEKLY only");
        }
    }

    private String buildNormalizedRule(
            String frequency,
            int interval,
            Integer count,
            LocalDateTime until,
            Set<DayOfWeek> byDays
    ) {
        List<String> parts = new ArrayList<>();
        parts.add("FREQ=" + frequency);
        if (!byDays.isEmpty()) {
            parts.add("BYDAY=" + formatByDays(byDays));
        }
        if (interval != DEFAULT_INTERVAL) {
            parts.add("INTERVAL=" + interval);
        }
        if (count != null) {
            parts.add("COUNT=" + count);
        }
        if (until != null) {
            parts.add("UNTIL=" + until.format(DATE_TIME_FORMATTER) + "Z");
        }
        return String.join(";", parts);
    }

    private String formatByDays(Set<DayOfWeek> days) {
        return days.stream()
                .map(this::formatDay)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private String formatDay(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "MO";
            case TUESDAY -> "TU";
            case WEDNESDAY -> "WE";
            case THURSDAY -> "TH";
            case FRIDAY -> "FR";
            case SATURDAY -> "SA";
            case SUNDAY -> "SU";
        };
    }

    private List<CalendarEventOccurrence> singleOccurrence(CalendarEvent event, LocalDateTime fromAt, LocalDateTime toAt) {
        if (overlaps(event.getStartAt(), event.getEndAt(), fromAt, toAt)) {
            return List.of(new CalendarEventOccurrence(event.getStartAt(), event.getEndAt(), 0));
        }
        return List.of();
    }

    private List<CalendarEventOccurrence> expandRule(
            CalendarEvent event,
            CalendarRecurrenceRule rule,
            LocalDateTime fromAt,
            LocalDateTime toAt
    ) {
        return switch (rule.frequency()) {
            case "DAILY" -> expandFixedStep(event, rule, fromAt, toAt, ChronoUnit.DAYS);
            case "MONTHLY" -> expandFixedStep(event, rule, fromAt, toAt, ChronoUnit.MONTHS);
            case "YEARLY" -> expandFixedStep(event, rule, fromAt, toAt, ChronoUnit.YEARS);
            case "WEEKLY" -> expandWeekly(event, rule, fromAt, toAt);
            default -> throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported RRULE FREQ: " + rule.frequency());
        };
    }

    private List<CalendarEventOccurrence> expandFixedStep(
            CalendarEvent event,
            CalendarRecurrenceRule rule,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            ChronoUnit unit
    ) {
        List<CalendarEventOccurrence> result = new ArrayList<>();
        LocalDateTime start = event.getStartAt();
        for (int index = 0; withinCount(rule, index); index++) {
            LocalDateTime occurrenceStart = start.plus((long) index * rule.interval(), unit);
            if (!appendIfVisible(result, event, rule, occurrenceStart, fromAt, toAt, index)) {
                break;
            }
        }
        return result;
    }

    private List<CalendarEventOccurrence> expandWeekly(
            CalendarEvent event,
            CalendarRecurrenceRule rule,
            LocalDateTime fromAt,
            LocalDateTime toAt
    ) {
        List<CalendarEventOccurrence> result = new ArrayList<>();
        Set<DayOfWeek> days = rule.byDays().isEmpty() ? Set.of(event.getStartAt().getDayOfWeek()) : rule.byDays();
        LocalDate current = event.getStartAt().toLocalDate();
        LocalDate end = effectiveEndDate(rule, event, toAt);
        long index = 0;
        while (!current.isAfter(end) && withinCount(rule, index)) {
            if (isWeeklyCandidate(event.getStartAt().toLocalDate(), current, rule.interval(), days)) {
                LocalDateTime occurrenceStart = current.atTime(event.getStartAt().toLocalTime());
                boolean shouldContinue = appendIfVisible(result, event, rule, occurrenceStart, fromAt, toAt, index);
                if (!shouldContinue) {
                    break;
                }
                index++;
            }
            current = current.plusDays(1);
        }
        return result;
    }

    private boolean appendIfVisible(
            List<CalendarEventOccurrence> result,
            CalendarEvent event,
            CalendarRecurrenceRule rule,
            LocalDateTime occurrenceStart,
            LocalDateTime fromAt,
            LocalDateTime toAt,
            long index
    ) {
        if (occurrenceStart.isBefore(event.getStartAt()) || afterEffectiveUntil(event, rule, occurrenceStart)) {
            return !occurrenceStart.isAfter(toAt);
        }
        LocalDateTime occurrenceEnd = occurrenceStart.plus(Duration.between(event.getStartAt(), event.getEndAt()));
        if (overlaps(occurrenceStart, occurrenceEnd, fromAt, toAt)) {
            result.add(new CalendarEventOccurrence(occurrenceStart, occurrenceEnd, index));
        }
        return result.size() < MAX_EXPANDED_OCCURRENCES && occurrenceStart.isBefore(toAt);
    }

    private boolean isWeeklyCandidate(LocalDate startDate, LocalDate current, int interval, Set<DayOfWeek> days) {
        if (!days.contains(current.getDayOfWeek())) {
            return false;
        }
        WeekFields weekFields = WeekFields.ISO;
        long weeks = ChronoUnit.WEEKS.between(startDate.with(weekFields.dayOfWeek(), 1), current.with(weekFields.dayOfWeek(), 1));
        return weeks >= 0 && weeks % interval == 0;
    }

    private LocalDate effectiveEndDate(CalendarRecurrenceRule rule, CalendarEvent event, LocalDateTime toAt) {
        LocalDateTime explicitUntil = minNonNull(rule.until(), event.getRecurrenceUntil());
        LocalDateTime endAt = explicitUntil == null ? toAt : explicitUntil;
        return endAt.toLocalDate();
    }

    private boolean afterEffectiveUntil(CalendarEvent event, CalendarRecurrenceRule rule, LocalDateTime occurrenceStart) {
        LocalDateTime until = minNonNull(rule.until(), event.getRecurrenceUntil());
        return until != null && occurrenceStart.isAfter(until);
    }

    private LocalDateTime minNonNull(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.isBefore(right) ? left : right;
    }

    private boolean withinCount(CalendarRecurrenceRule rule, long index) {
        return rule.count() == null || index < rule.count();
    }

    private boolean overlaps(LocalDateTime startAt, LocalDateTime endAt, LocalDateTime fromAt, LocalDateTime toAt) {
        return startAt.isBefore(toAt) && endAt.isAfter(fromAt);
    }

    private List<CalendarEventOccurrence> extraRdates(CalendarEvent event, LocalDateTime fromAt, LocalDateTime toAt) {
        Duration duration = Duration.between(event.getStartAt(), event.getEndAt());
        return decodeDates(event.getRecurrenceRdatesJson()).stream()
                .filter(startAt -> overlaps(startAt, startAt.plus(duration), fromAt, toAt))
                .map(startAt -> new CalendarEventOccurrence(startAt, startAt.plus(duration), 0))
                .toList();
    }

    private Set<LocalDateTime> decodeDates(String encodedDates) {
        Set<LocalDateTime> dates = new LinkedHashSet<>();
        if (!StringUtils.hasText(encodedDates)) {
            return dates;
        }
        String raw = encodedDates.substring(1, encodedDates.length() - 1);
        for (String token : raw.split(",")) {
            if (StringUtils.hasText(token)) {
                dates.add(LocalDateTime.parse(token.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        }
        return dates;
    }

    private void assertSupportedWindow(LocalDateTime fromAt, LocalDateTime toAt) {
        if (Duration.between(fromAt, toAt).toDays() > MAX_QUERY_DAYS) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Recurring calendar query window must not exceed 366 days");
        }
    }
}
