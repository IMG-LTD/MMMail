package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.CalendarEventMapper;
import com.mmmail.server.mapper.MailEasySwitchSessionMapper;
import com.mmmail.server.mapper.MailMessageMapper;
import com.mmmail.server.mapper.UserPreferenceMapper;
import com.mmmail.server.model.dto.CreateMailEasySwitchSessionRequest;
import com.mmmail.server.model.entity.CalendarEvent;
import com.mmmail.server.model.entity.MailEasySwitchSession;
import com.mmmail.server.model.entity.MailMessage;
import com.mmmail.server.model.entity.UserPreference;
import com.mmmail.server.model.vo.ContactImportResultVo;
import com.mmmail.server.model.vo.MailEasySwitchSessionVo;
import com.mmmail.server.observability.JobRunMonitorService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MailEasySwitchService {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("GOOGLE", "OUTLOOK", "YAHOO", "OTHER");
    private static final Set<String> SUPPORTED_MAIL_FOLDERS = Set.of("INBOX", "ARCHIVE");
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String DEFAULT_MAIL_FOLDER = "ARCHIVE";
    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final String DEFAULT_IMPORTED_LABELS_JSON = "[]";

    private final MailEasySwitchSessionMapper mailEasySwitchSessionMapper;
    private final ContactService contactService;
    private final CalendarEventMapper calendarEventMapper;
    private final MailMessageMapper mailMessageMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final AuditService auditService;
    private final CalendarIcsImportParser calendarIcsImportParser;
    private final MailEmlImportParser mailEmlImportParser;
    private final JobRunMonitorService jobRunMonitorService;

    public MailEasySwitchService(
            MailEasySwitchSessionMapper mailEasySwitchSessionMapper,
            ContactService contactService,
            CalendarEventMapper calendarEventMapper,
            MailMessageMapper mailMessageMapper,
            UserPreferenceMapper userPreferenceMapper,
            AuditService auditService,
            JobRunMonitorService jobRunMonitorService
    ) {
        this.mailEasySwitchSessionMapper = mailEasySwitchSessionMapper;
        this.contactService = contactService;
        this.calendarEventMapper = calendarEventMapper;
        this.mailMessageMapper = mailMessageMapper;
        this.userPreferenceMapper = userPreferenceMapper;
        this.auditService = auditService;
        this.jobRunMonitorService = jobRunMonitorService;
        this.calendarIcsImportParser = new CalendarIcsImportParser();
        this.mailEmlImportParser = new MailEmlImportParser();
    }

    public List<MailEasySwitchSessionVo> list(Long userId) {
        return mailEasySwitchSessionMapper.selectList(new LambdaQueryWrapper<MailEasySwitchSession>()
                        .eq(MailEasySwitchSession::getOwnerId, userId)
                        .orderByDesc(MailEasySwitchSession::getCreatedAt))
                .stream()
                .map(this::toVo)
                .toList();
    }

    public MailEasySwitchSessionVo create(Long userId, CreateMailEasySwitchSessionRequest request, String ipAddress) {
        NormalizedRequest normalized = normalizeRequest(request);
        MailEasySwitchSession session = buildRunningSession(userId, normalized);
        mailEasySwitchSessionMapper.insert(session);
        JobRunMonitorService.JobHandle jobHandle = jobRunMonitorService.start(new JobRunMonitorService.JobDescriptor(
                "MAIL_EASY_SWITCH_IMPORT",
                "USER_ACTION",
                String.valueOf(userId),
                null
        ));

        try {
            applyImport(userId, normalized, session, ipAddress);
            completeSession(session);
            jobRunMonitorService.success(jobHandle, jobDetail(session));
            auditService.record(
                    userId,
                    "MAIL_EASY_SWITCH_IMPORT",
                    "session=" + session.getId() + ",provider=" + session.getProvider(),
                    ipAddress
            );
            return toVo(session);
        } catch (RuntimeException ex) {
            failSession(session, ex.getMessage());
            jobRunMonitorService.fail(jobHandle, jobDetail(session) + ",reason=" + ex.getMessage());
            auditService.record(
                    userId,
                    "MAIL_EASY_SWITCH_IMPORT_FAILED",
                    "session=" + session.getId() + ",reason=" + ex.getMessage(),
                    ipAddress
            );
            throw ex;
        }
    }

    public void delete(Long userId, Long sessionId, String ipAddress) {
        MailEasySwitchSession session = loadSession(userId, sessionId);
        mailEasySwitchSessionMapper.deleteById(sessionId);
        auditService.record(userId, "MAIL_EASY_SWITCH_DELETE", "session=" + sessionId, ipAddress);
    }

    private void applyImport(Long userId, NormalizedRequest request, MailEasySwitchSession session, String ipAddress) {
        if (request.importContacts()) {
            ContactImportResultVo result = contactService.importCsv(
                    userId,
                    request.contactsCsv(),
                    request.mergeContactDuplicates(),
                    ipAddress
            );
            session.setContactsCreated(result.created());
            session.setContactsUpdated(result.updated());
            session.setContactsSkipped(result.skipped());
            session.setContactsInvalid(result.invalid());
        }
        if (request.importCalendar()) {
            CalendarImportStats result = importCalendar(userId, request);
            session.setCalendarImported(result.imported());
            session.setCalendarInvalid(result.invalid());
        }
        if (request.importMail()) {
            MailImportStats result = importMail(userId, request);
            session.setMailImported(result.imported());
            session.setMailSkipped(result.skipped());
            session.setMailInvalid(result.invalid());
        }
    }

    private CalendarImportStats importCalendar(Long userId, NormalizedRequest request) {
        List<CalendarIcsImportParser.CalendarImportDraft> drafts = calendarIcsImportParser.parse(
                request.calendarIcs(),
                resolveTimezone(userId)
        );
        int imported = 0;
        int invalid = 0;
        LocalDateTime now = LocalDateTime.now();

        for (CalendarIcsImportParser.CalendarImportDraft draft : drafts) {
            try {
                CalendarEvent event = new CalendarEvent();
                event.setOwnerId(userId);
                event.setTitle(draft.title());
                event.setDescription(draft.description());
                event.setLocation(draft.location());
                event.setStartAt(draft.startAt());
                event.setEndAt(draft.endAt());
                event.setAllDay(draft.allDay() ? 1 : 0);
                event.setTimezone(draft.timezone());
                event.setReminderMinutes(30);
                event.setCreatedAt(now);
                event.setUpdatedAt(now);
                event.setDeleted(0);
                calendarEventMapper.insert(event);
                imported++;
            } catch (RuntimeException ex) {
                invalid++;
            }
        }
        return new CalendarImportStats(imported, invalid);
    }

    private MailImportStats importMail(Long userId, NormalizedRequest request) {
        int imported = 0;
        int skipped = 0;
        int invalid = 0;
        String fallbackTimezone = resolveTimezone(userId);

        for (String messageContent : request.mailMessages()) {
            try {
                MailEmlImportParser.ParsedEmlMessage parsed = mailEmlImportParser.parse(messageContent, fallbackTimezone);
                String idempotencyKey = buildIdempotencyKey(request.sourceEmail(), messageContent);
                if (existsImportedMail(userId, idempotencyKey)) {
                    skipped++;
                    continue;
                }
                MailMessage mail = new MailMessage();
                LocalDateTime now = LocalDateTime.now();
                mail.setOwnerId(userId);
                mail.setPeerId(null);
                mail.setPeerEmail(parsed.senderEmail());
                mail.setSenderEmail(parsed.senderEmail());
                mail.setDirection("IN");
                mail.setFolderType(request.importedMailFolder());
                mail.setSubject(parsed.subject());
                mail.setBodyCiphertext(parsed.body());
                mail.setIsRead("ARCHIVE".equals(request.importedMailFolder()) ? 1 : 0);
                mail.setIsStarred(0);
                mail.setIsDraft(0);
                mail.setLabelsJson(DEFAULT_IMPORTED_LABELS_JSON);
                mail.setIdempotencyKey(idempotencyKey);
                mail.setSentAt(parsed.sentAt());
                mail.setCreatedAt(now);
                mail.setUpdatedAt(now);
                mail.setDeleted(0);
                mailMessageMapper.insert(mail);
                imported++;
            } catch (BizException ex) {
                invalid++;
            } catch (DuplicateKeyException ex) {
                skipped++;
            }
        }
        return new MailImportStats(imported, skipped, invalid);
    }

    private String buildIdempotencyKey(String sourceEmail, String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((sourceEmail + "\n" + content).getBytes(StandardCharsets.UTF_8));
            return "easy-switch:" + HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to hash imported message");
        }
    }

    private boolean existsImportedMail(Long userId, String idempotencyKey) {
        MailMessage existing = mailMessageMapper.selectOne(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getIdempotencyKey, idempotencyKey));
        return existing != null;
    }

    private String resolveTimezone(Long userId) {
        UserPreference preference = userPreferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getOwnerId, userId));
        if (preference == null || !StringUtils.hasText(preference.getTimezone())) {
            return DEFAULT_TIMEZONE;
        }
        return preference.getTimezone().trim();
    }

    private NormalizedRequest normalizeRequest(CreateMailEasySwitchSessionRequest request) {
        String provider = normalizeProvider(request.provider());
        String sourceEmail = normalizeEmail(request.sourceEmail());
        boolean importContacts = Boolean.TRUE.equals(request.importContacts());
        boolean mergeContactDuplicates = request.mergeContactDuplicates() == null || request.mergeContactDuplicates();
        boolean importCalendar = Boolean.TRUE.equals(request.importCalendar());
        boolean importMail = Boolean.TRUE.equals(request.importMail());
        if (!importContacts && !importCalendar && !importMail) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "At least one import type must be selected");
        }
        String contactsCsv = normalizeRequiredContent(importContacts, request.contactsCsv(), "Contacts CSV content is required");
        String calendarIcs = normalizeRequiredContent(importCalendar, request.calendarIcs(), "Calendar ICS content is required");
        List<String> mailMessages = normalizeMailMessages(importMail, request.mailMessages());
        String importedMailFolder = importMail ? normalizeMailFolder(request.importedMailFolder()) : DEFAULT_MAIL_FOLDER;

        return new NormalizedRequest(
                provider,
                sourceEmail,
                importContacts,
                mergeContactDuplicates,
                contactsCsv,
                importCalendar,
                calendarIcs,
                importMail,
                mailMessages,
                importedMailFolder
        );
    }

    private String normalizeProvider(String provider) {
        if (!StringUtils.hasText(provider)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Provider is required");
        }
        String normalized = provider.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_PROVIDERS.contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported provider");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Source email is required");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!normalized.contains("@")) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Source email is invalid");
        }
        return normalized;
    }

    private String normalizeRequiredContent(boolean enabled, String content, String message) {
        if (!enabled) {
            return null;
        }
        if (!StringUtils.hasText(content)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return content.trim();
    }

    private List<String> normalizeMailMessages(boolean enabled, List<String> mailMessages) {
        if (!enabled) {
            return List.of();
        }
        if (mailMessages == null || mailMessages.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "At least one EML message is required");
        }
        List<String> normalized = mailMessages.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        if (normalized.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "At least one EML message is required");
        }
        return List.copyOf(new LinkedHashSet<>(normalized));
    }

    private String normalizeMailFolder(String folder) {
        if (!StringUtils.hasText(folder)) {
            return DEFAULT_MAIL_FOLDER;
        }
        String normalized = folder.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_MAIL_FOLDERS.contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Imported mail folder must be INBOX or ARCHIVE");
        }
        return normalized;
    }

    private MailEasySwitchSession buildRunningSession(Long userId, NormalizedRequest request) {
        MailEasySwitchSession session = new MailEasySwitchSession();
        LocalDateTime now = LocalDateTime.now();
        session.setOwnerId(userId);
        session.setProvider(request.provider());
        session.setSourceEmail(request.sourceEmail());
        session.setImportContacts(boolToInt(request.importContacts()));
        session.setMergeContactDuplicates(boolToInt(request.mergeContactDuplicates()));
        session.setImportCalendar(boolToInt(request.importCalendar()));
        session.setImportMail(boolToInt(request.importMail()));
        session.setImportedMailFolder(request.importedMailFolder());
        session.setStatus(STATUS_RUNNING);
        session.setContactsCreated(0);
        session.setContactsUpdated(0);
        session.setContactsSkipped(0);
        session.setContactsInvalid(0);
        session.setCalendarImported(0);
        session.setCalendarInvalid(0);
        session.setMailImported(0);
        session.setMailSkipped(0);
        session.setMailInvalid(0);
        session.setErrorMessage(null);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session.setCompletedAt(null);
        session.setDeleted(0);
        return session;
    }

    private void completeSession(MailEasySwitchSession session) {
        LocalDateTime now = LocalDateTime.now();
        session.setStatus(STATUS_COMPLETED);
        session.setUpdatedAt(now);
        session.setCompletedAt(now);
        session.setErrorMessage(null);
        mailEasySwitchSessionMapper.updateById(session);
    }

    private void failSession(MailEasySwitchSession session, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        session.setStatus(STATUS_FAILED);
        session.setUpdatedAt(now);
        session.setCompletedAt(now);
        session.setErrorMessage(StringUtils.hasText(errorMessage) ? errorMessage : "Import failed");
        mailEasySwitchSessionMapper.updateById(session);
    }

    private MailEasySwitchSession loadSession(Long userId, Long sessionId) {
        MailEasySwitchSession session = mailEasySwitchSessionMapper.selectOne(new LambdaQueryWrapper<MailEasySwitchSession>()
                .eq(MailEasySwitchSession::getId, sessionId)
                .eq(MailEasySwitchSession::getOwnerId, userId));
        if (session == null) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Import session does not exist");
        }
        return session;
    }

    private MailEasySwitchSessionVo toVo(MailEasySwitchSession session) {
        return new MailEasySwitchSessionVo(
                String.valueOf(session.getId()),
                session.getProvider(),
                session.getSourceEmail(),
                intToBool(session.getImportContacts()),
                intToBool(session.getMergeContactDuplicates()),
                intToBool(session.getImportCalendar()),
                intToBool(session.getImportMail()),
                session.getImportedMailFolder(),
                session.getStatus(),
                nullSafeInt(session.getContactsCreated()),
                nullSafeInt(session.getContactsUpdated()),
                nullSafeInt(session.getContactsSkipped()),
                nullSafeInt(session.getContactsInvalid()),
                nullSafeInt(session.getCalendarImported()),
                nullSafeInt(session.getCalendarInvalid()),
                nullSafeInt(session.getMailImported()),
                nullSafeInt(session.getMailSkipped()),
                nullSafeInt(session.getMailInvalid()),
                session.getErrorMessage(),
                session.getCreatedAt(),
                session.getCompletedAt()
        );
    }

    private String jobDetail(MailEasySwitchSession session) {
        return "session=" + session.getId() + ",provider=" + session.getProvider() + ",status=" + session.getStatus();
    }

    private int boolToInt(boolean value) {
        return value ? 1 : 0;
    }

    private boolean intToBool(Integer value) {
        return value != null && value == 1;
    }

    private int nullSafeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private record NormalizedRequest(
            String provider,
            String sourceEmail,
            boolean importContacts,
            boolean mergeContactDuplicates,
            String contactsCsv,
            boolean importCalendar,
            String calendarIcs,
            boolean importMail,
            List<String> mailMessages,
            String importedMailFolder
    ) {
    }

    private record CalendarImportStats(int imported, int invalid) {
    }

    private record MailImportStats(int imported, int skipped, int invalid) {
    }
}
