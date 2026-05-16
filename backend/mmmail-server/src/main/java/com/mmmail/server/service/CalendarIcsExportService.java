package com.mmmail.server.service;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class CalendarIcsExportService {

    private static final String DEFAULT_CALENDAR_ID = "default";
    private static final String SHA_256 = "SHA-256";

    private final CalendarService calendarService;

    public CalendarIcsExportService(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    public NativeIcsExport export(Long userId, String calendarId, String ipAddress) {
        if (!DEFAULT_CALENDAR_ID.equals(calendarId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Only the default calendar can be exported as ICS");
        }
        String content = calendarService.exportCalendar(userId, "ics", ipAddress);
        return new NativeIcsExport(content, buildWeakEtag(content));
    }

    private String buildWeakEtag(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return "W/\"" + HexFormat.of().formatHex(bytes) + "\"";
        } catch (NoSuchAlgorithmException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to create calendar ICS ETag");
        }
    }

    public record NativeIcsExport(String content, String etag) {
    }
}
