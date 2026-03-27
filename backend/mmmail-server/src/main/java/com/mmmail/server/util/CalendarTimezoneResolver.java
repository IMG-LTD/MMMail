package com.mmmail.server.util;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.util.StringUtils;

import java.time.DateTimeException;
import java.time.ZoneId;

public final class CalendarTimezoneResolver {

    private static final String DEFAULT_TIMEZONE = "UTC";

    private CalendarTimezoneResolver() {
    }

    public static String normalizeOrDefault(String candidate, String fallback, String fieldName) {
        String value = StringUtils.hasText(candidate) ? candidate.trim() : fallbackTimezone(fallback);
        try {
            ZoneId.of(value);
            return value;
        } catch (DateTimeException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, fieldName + " is invalid");
        }
    }

    private static String fallbackTimezone(String fallback) {
        if (!StringUtils.hasText(fallback)) {
            return DEFAULT_TIMEZONE;
        }
        return fallback.trim();
    }
}
