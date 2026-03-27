package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.ContactEntryMapper;
import com.mmmail.server.mapper.MailMessageMapper;
import com.mmmail.server.model.entity.ContactEntry;
import com.mmmail.server.model.entity.MailMessage;
import com.mmmail.server.model.vo.ContactDuplicateGroupVo;
import com.mmmail.server.model.vo.ContactImportResultVo;
import com.mmmail.server.model.vo.ContactItemVo;
import com.mmmail.server.model.vo.ContactSuggestionVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ContactService {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 20;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Set<String> CONTACT_NAME_HEADERS = Set.of("displayname", "display name", "display_name", "name", "full name");
    private static final Set<String> CONTACT_EMAIL_HEADERS = Set.of("email", "email address", "emailaddress", "e-mail", "mail");
    private static final Set<String> CONTACT_NOTE_HEADERS = Set.of("note", "notes", "phone", "phone number", "mobile", "telephone");

    private final ContactEntryMapper contactEntryMapper;
    private final MailMessageMapper mailMessageMapper;
    private final AuditService auditService;
    private final ContactGroupService contactGroupService;

    public ContactService(
            ContactEntryMapper contactEntryMapper,
            MailMessageMapper mailMessageMapper,
            AuditService auditService,
            ContactGroupService contactGroupService
    ) {
        this.contactEntryMapper = contactEntryMapper;
        this.mailMessageMapper = mailMessageMapper;
        this.auditService = auditService;
        this.contactGroupService = contactGroupService;
    }

    public List<ContactItemVo> list(Long userId, String keyword, Boolean favoriteOnly) {
        String normalizedKeyword = normalizeKeyword(keyword);
        LambdaQueryWrapper<ContactEntry> query = new LambdaQueryWrapper<ContactEntry>()
                .eq(ContactEntry::getOwnerId, userId)
                .orderByDesc(ContactEntry::getIsFavorite)
                .orderByDesc(ContactEntry::getUpdatedAt)
                .orderByAsc(ContactEntry::getDisplayName);

        if (Boolean.TRUE.equals(favoriteOnly)) {
            query.eq(ContactEntry::getIsFavorite, 1);
        }
        if (StringUtils.hasText(normalizedKeyword)) {
            query.and(wrapper -> wrapper.like(ContactEntry::getDisplayName, normalizedKeyword)
                    .or()
                    .like(ContactEntry::getEmail, normalizedKeyword));
        }

        return contactEntryMapper.selectList(query).stream()
                .map(this::toItemVo)
                .toList();
    }

    @Transactional
    public ContactItemVo create(Long userId, String displayName, String email, String note, String ipAddress) {
        String normalizedEmail = normalizeEmail(email);
        ensureContactEmailUnique(userId, normalizedEmail, null);

        LocalDateTime now = LocalDateTime.now();
        ContactEntry entry = new ContactEntry();
        entry.setOwnerId(userId);
        entry.setDisplayName(requireDisplayName(displayName));
        entry.setEmail(normalizedEmail);
        entry.setNote(normalizeNote(note));
        entry.setIsFavorite(0);
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        entry.setDeleted(0);
        contactEntryMapper.insert(entry);

        auditService.record(userId, "CONTACT_CREATE", "email=" + normalizedEmail, ipAddress);
        return toItemVo(entry);
    }

    @Transactional
    public ContactItemVo update(Long userId, Long contactId, String displayName, String email, String note, String ipAddress) {
        ContactEntry entry = loadContact(userId, contactId);
        String normalizedEmail = normalizeEmail(email);
        ensureContactEmailUnique(userId, normalizedEmail, contactId);

        entry.setDisplayName(requireDisplayName(displayName));
        entry.setEmail(normalizedEmail);
        entry.setNote(normalizeNote(note));
        entry.setUpdatedAt(LocalDateTime.now());
        contactEntryMapper.updateById(entry);

        auditService.record(userId, "CONTACT_UPDATE", "contactId=" + contactId, ipAddress);
        return toItemVo(entry);
    }

    @Transactional
    public void delete(Long userId, Long contactId, String ipAddress) {
        ContactEntry entry = loadContact(userId, contactId);
        contactEntryMapper.deleteById(entry.getId());
        auditService.record(userId, "CONTACT_DELETE", "contactId=" + contactId, ipAddress);
    }

    @Transactional
    public ContactItemVo favorite(Long userId, Long contactId, boolean favorite, String ipAddress) {
        ContactEntry entry = loadContact(userId, contactId);
        entry.setIsFavorite(favorite ? 1 : 0);
        entry.setUpdatedAt(LocalDateTime.now());
        contactEntryMapper.updateById(entry);
        auditService.record(
                userId,
                favorite ? "CONTACT_FAVORITE" : "CONTACT_UNFAVORITE",
                "contactId=" + contactId,
                ipAddress
        );
        return toItemVo(entry);
    }

    @Transactional
    public ContactItemVo quickAdd(Long userId, String email, String displayName, String ipAddress) {
        String normalizedEmail = normalizeEmail(email);
        String candidateName = StringUtils.hasText(displayName) ? displayName.trim() : defaultDisplayName(normalizedEmail);
        LocalDateTime now = LocalDateTime.now();

        ContactEntry existing = contactEntryMapper.selectOne(new LambdaQueryWrapper<ContactEntry>()
                .eq(ContactEntry::getOwnerId, userId)
                .eq(ContactEntry::getEmail, normalizedEmail));

        if (existing == null) {
            ContactEntry entry = new ContactEntry();
            entry.setOwnerId(userId);
            entry.setDisplayName(candidateName);
            entry.setEmail(normalizedEmail);
            entry.setNote(null);
            entry.setIsFavorite(0);
            entry.setCreatedAt(now);
            entry.setUpdatedAt(now);
            entry.setDeleted(0);
            contactEntryMapper.insert(entry);
            auditService.record(userId, "CONTACT_QUICK_ADD", "created:" + normalizedEmail, ipAddress);
            return toItemVo(entry);
        }

        if (StringUtils.hasText(candidateName)) {
            existing.setDisplayName(candidateName);
        }
        existing.setUpdatedAt(now);
        contactEntryMapper.updateById(existing);
        auditService.record(userId, "CONTACT_QUICK_ADD", "updated:" + normalizedEmail, ipAddress);
        return toItemVo(existing);
    }

    public List<ContactSuggestionVo> suggestions(Long userId, String keyword, Integer limit) {
        int safeLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
        String normalizedKeyword = normalizeKeyword(keyword);

        Map<String, ContactSuggestionAggregate> aggregateMap = new LinkedHashMap<>();

        List<ContactEntry> contacts = contactEntryMapper.selectList(new LambdaQueryWrapper<ContactEntry>()
                .eq(ContactEntry::getOwnerId, userId)
                .orderByDesc(ContactEntry::getIsFavorite)
                .orderByDesc(ContactEntry::getUpdatedAt)
                .last("limit 2000"));

        for (ContactEntry contact : contacts) {
            String email = normalizeEmail(contact.getEmail());
            if (email == null) {
                continue;
            }
            if (!matchesKeyword(normalizedKeyword, email, contact.getDisplayName())) {
                continue;
            }
            ContactSuggestionAggregate aggregate = aggregateMap.computeIfAbsent(
                    email,
                    key -> new ContactSuggestionAggregate(email)
            );
            aggregate.acceptContact(contact);
        }

        List<MailMessage> history = mailMessageMapper.selectList(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getIsDraft, 0)
                .orderByDesc(MailMessage::getSentAt)
                .last("limit 2000"));

        for (MailMessage message : history) {
            String email = normalizeEmail(message.getPeerEmail());
            if (email == null) {
                continue;
            }
            ContactSuggestionAggregate aggregate = aggregateMap.get(email);
            if (aggregate == null && !matchesKeyword(normalizedKeyword, email, null)) {
                continue;
            }
            if (aggregate == null) {
                aggregate = new ContactSuggestionAggregate(email);
                aggregateMap.put(email, aggregate);
            }
            aggregate.acceptHistory(message.getSentAt());
        }

        return aggregateMap.values().stream()
                .sorted(Comparator
                        .comparing(ContactSuggestionAggregate::isFavorite).reversed()
                        .thenComparing(ContactSuggestionAggregate::sourcePriority)
                        .thenComparing(ContactSuggestionAggregate::lastContactAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                        .thenComparing(ContactSuggestionAggregate::messageCount, Comparator.reverseOrder())
                        .thenComparing(ContactSuggestionAggregate::email)
                )
                .limit(safeLimit)
                .map(this::toSuggestionVo)
                .toList();
    }

    @Transactional
    public ContactImportResultVo importCsv(Long userId, String content, Boolean mergeDuplicates, String ipAddress) {
        if (!StringUtils.hasText(content)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "CSV content is required");
        }
        boolean allowMerge = Boolean.TRUE.equals(mergeDuplicates);
        String[] rows = content.split("\\r?\\n");
        LocalDateTime now = LocalDateTime.now();
        ImportCsvCounters counters = ImportCsvCounters.empty();
        for (int i = 0; i < rows.length; i++) {
            counters = importCsvRow(userId, rows[i], i, allowMerge, now, counters);
        }

        auditService.record(
                userId,
                "CONTACT_IMPORT",
                "total=" + counters.totalRows()
                        + ",created=" + counters.created()
                        + ",updated=" + counters.updated()
                        + ",skipped=" + counters.skipped()
                        + ",invalid=" + counters.invalid(),
                ipAddress
        );
        return new ContactImportResultVo(
                counters.totalRows(),
                counters.created(),
                counters.updated(),
                counters.skipped(),
                counters.invalid()
        );
    }

    public String exportContacts(Long userId, String format, String ipAddress) {
        String normalizedFormat = StringUtils.hasText(format) ? format.trim().toLowerCase() : "csv";
        List<ContactEntry> contacts = contactEntryMapper.selectList(new LambdaQueryWrapper<ContactEntry>()
                .eq(ContactEntry::getOwnerId, userId)
                .orderByAsc(ContactEntry::getDisplayName)
                .orderByAsc(ContactEntry::getEmail));
        String content;
        if ("csv".equals(normalizedFormat)) {
            content = exportCsv(contacts);
        } else if ("vcard".equals(normalizedFormat)) {
            content = exportVcard(contacts);
        } else {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported export format");
        }
        auditService.record(userId, "CONTACT_EXPORT", "format=" + normalizedFormat + ",count=" + contacts.size(), ipAddress);
        return content;
    }

    public List<ContactDuplicateGroupVo> listDuplicates(Long userId) {
        List<ContactEntry> contacts = contactEntryMapper.selectList(new LambdaQueryWrapper<ContactEntry>()
                .eq(ContactEntry::getOwnerId, userId)
                .orderByAsc(ContactEntry::getDisplayName)
                .orderByDesc(ContactEntry::getUpdatedAt));

        Map<String, List<ContactEntry>> bySignature = new LinkedHashMap<>();
        for (ContactEntry contact : contacts) {
            String signature = normalizeDuplicateSignature(contact.getDisplayName(), contact.getEmail());
            if (!StringUtils.hasText(signature)) {
                continue;
            }
            bySignature.computeIfAbsent(signature, key -> new java.util.ArrayList<>()).add(contact);
        }

        return bySignature.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> new ContactDuplicateGroupVo(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .sorted(Comparator.comparing(ContactEntry::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                                .map(this::toItemVo)
                                .toList()
                ))
                .sorted(Comparator.comparing(ContactDuplicateGroupVo::count).reversed().thenComparing(ContactDuplicateGroupVo::signature))
                .toList();
    }

    @Transactional
    public ContactItemVo mergeDuplicates(
            Long userId,
            String primaryContactId,
            List<String> duplicateContactIds,
            String ipAddress
    ) {
        Long primaryId = parseContactId(primaryContactId);
        Set<Long> duplicateIds = parseContactIds(duplicateContactIds);
        duplicateIds.remove(primaryId);
        if (duplicateIds.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "No duplicate contacts provided");
        }

        ContactEntry primary = loadContact(userId, primaryId);
        List<ContactEntry> duplicates = contactEntryMapper.selectList(new LambdaQueryWrapper<ContactEntry>()
                .eq(ContactEntry::getOwnerId, userId)
                .in(ContactEntry::getId, duplicateIds));
        if (duplicates.size() != duplicateIds.size()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Some duplicate contacts are invalid");
        }

        LocalDateTime now = LocalDateTime.now();
        for (ContactEntry duplicate : duplicates) {
            primary.setNote(mergeNotes(primary.getNote(), duplicate.getNote()));
            if (duplicate.getIsFavorite() != null && duplicate.getIsFavorite() == 1) {
                primary.setIsFavorite(1);
            }
        }
        primary.setUpdatedAt(now);
        contactEntryMapper.updateById(primary);

        contactGroupService.migrateMemberLinksForMergedContacts(userId, primaryId, duplicates.stream().map(ContactEntry::getId).toList());
        for (ContactEntry duplicate : duplicates) {
            contactEntryMapper.deleteById(duplicate.getId());
        }

        auditService.record(
                userId,
                "CONTACT_MERGE",
                "primary=" + primaryId + ",duplicates=" + duplicateIds,
                ipAddress
        );
        return toItemVo(primary);
    }

    private ContactEntry loadContact(Long userId, Long contactId) {
        ContactEntry entry = contactEntryMapper.selectOne(new LambdaQueryWrapper<ContactEntry>()
                .eq(ContactEntry::getId, contactId)
                .eq(ContactEntry::getOwnerId, userId));
        if (entry == null) {
            throw new BizException(ErrorCode.CONTACT_NOT_FOUND);
        }
        return entry;
    }

    private void ensureContactEmailUnique(Long userId, String normalizedEmail, Long excludeId) {
        ContactEntry existing = contactEntryMapper.selectOne(new LambdaQueryWrapper<ContactEntry>()
                .eq(ContactEntry::getOwnerId, userId)
                .eq(ContactEntry::getEmail, normalizedEmail));
        if (existing != null && (excludeId == null || !existing.getId().equals(excludeId))) {
            throw new BizException(ErrorCode.CONTACT_ALREADY_EXISTS);
        }
    }

    private String requireDisplayName(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Display name is required");
        }
        return displayName.trim();
    }

    private String normalizeNote(String note) {
        if (!StringUtils.hasText(note)) {
            return null;
        }
        return note.trim();
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        return keyword.trim().toLowerCase();
    }

    private boolean matchesKeyword(String normalizedKeyword, String email, String displayName) {
        if (!StringUtils.hasText(normalizedKeyword)) {
            return true;
        }
        if (email.toLowerCase().contains(normalizedKeyword)) {
            return true;
        }
        return StringUtils.hasText(displayName) && displayName.toLowerCase().contains(normalizedKeyword);
    }

    private String normalizeEmail(String rawEmail) {
        if (!StringUtils.hasText(rawEmail)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Email is required");
        }
        String email = rawEmail.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Email format is invalid");
        }
        return email;
    }

    private String defaultDisplayName(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        return email.substring(0, at);
    }

    private List<String> parseCsvColumns(String row) {
        List<String> columns = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < row.length(); i++) {
            char ch = row.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < row.length() && row.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (ch == ',' && !inQuotes) {
                columns.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        columns.add(current.toString());
        return columns;
    }

    private ImportCsvCounters importCsvRow(
            Long userId,
            String row,
            int rowIndex,
            boolean allowMerge,
            LocalDateTime now,
            ImportCsvCounters counters
    ) {
        if (!StringUtils.hasText(row) || isContactCsvHeader(rowIndex, row)) {
            return counters;
        }
        List<String> columns = parseCsvColumns(row);
        if (columns.size() < 2) {
            return counters.incrementTotal().incrementInvalid();
        }
        ContactCsvRow csvRow = toContactCsvRow(columns);
        String normalizedEmail = normalizeImportEmail(csvRow.email());
        if (normalizedEmail == null) {
            return counters.incrementTotal().incrementInvalid();
        }
        ContactEntry existing = contactEntryMapper.selectOne(new LambdaQueryWrapper<ContactEntry>()
                .eq(ContactEntry::getOwnerId, userId)
                .eq(ContactEntry::getEmail, normalizedEmail));
        if (existing == null) {
            createImportedContact(userId, csvRow, normalizedEmail, now);
            return counters.incrementTotal().incrementCreated();
        }
        if (!allowMerge) {
            return counters.incrementTotal().incrementSkipped();
        }
        updateImportedContact(existing, csvRow, now);
        return counters.incrementTotal().incrementUpdated();
    }

    private boolean isContactCsvHeader(int rowIndex, String row) {
        if (rowIndex != 0) {
            return false;
        }
        List<String> columns = parseCsvColumns(row);
        if (columns.size() < 2) {
            return false;
        }
        String firstHeader = normalizeCsvHeader(columns.get(0));
        String secondHeader = normalizeCsvHeader(columns.get(1));
        if (!CONTACT_NAME_HEADERS.contains(firstHeader) || !CONTACT_EMAIL_HEADERS.contains(secondHeader)) {
            return false;
        }
        if (columns.size() < 3) {
            return true;
        }
        String thirdHeader = normalizeCsvHeader(columns.get(2));
        return !StringUtils.hasText(thirdHeader) || CONTACT_NOTE_HEADERS.contains(thirdHeader);
    }

    private String normalizeCsvHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase().replace('_', ' ');
    }

    private ContactCsvRow toContactCsvRow(List<String> columns) {
        String email = columns.get(1).trim();
        String displayName = columns.get(0).trim();
        if (!StringUtils.hasText(displayName)) {
            displayName = defaultDisplayName(email);
        }
        String note = columns.size() > 2 ? columns.get(2).trim() : null;
        String favoriteRaw = columns.size() > 3 ? columns.get(3).trim() : "";
        boolean favorite = "1".equals(favoriteRaw)
                || "true".equalsIgnoreCase(favoriteRaw)
                || "yes".equalsIgnoreCase(favoriteRaw);
        return new ContactCsvRow(displayName, email, note, favorite);
    }

    private String normalizeImportEmail(String email) {
        try {
            return normalizeEmail(email);
        } catch (BizException ex) {
            return null;
        }
    }

    private void createImportedContact(Long userId, ContactCsvRow csvRow, String normalizedEmail, LocalDateTime now) {
        ContactEntry entry = new ContactEntry();
        entry.setOwnerId(userId);
        entry.setDisplayName(csvRow.displayName());
        entry.setEmail(normalizedEmail);
        entry.setNote(StringUtils.hasText(csvRow.note()) ? csvRow.note() : null);
        entry.setIsFavorite(csvRow.favorite() ? 1 : 0);
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        entry.setDeleted(0);
        contactEntryMapper.insert(entry);
    }

    private void updateImportedContact(ContactEntry existing, ContactCsvRow csvRow, LocalDateTime now) {
        existing.setDisplayName(csvRow.displayName());
        existing.setNote(mergeNotes(existing.getNote(), csvRow.note()));
        if (csvRow.favorite()) {
            existing.setIsFavorite(1);
        }
        existing.setUpdatedAt(now);
        contactEntryMapper.updateById(existing);
    }

    private String exportCsv(List<ContactEntry> contacts) {
        StringBuilder builder = new StringBuilder();
        builder.append("displayName,email,note,isFavorite\\n");
        for (ContactEntry contact : contacts) {
            builder.append(csvEscape(contact.getDisplayName())).append(',')
                    .append(csvEscape(contact.getEmail())).append(',')
                    .append(csvEscape(contact.getNote())).append(',')
                    .append(contact.getIsFavorite() != null && contact.getIsFavorite() == 1 ? "true" : "false")
                    .append('\n');
        }
        return builder.toString();
    }

    private String exportVcard(List<ContactEntry> contacts) {
        StringBuilder builder = new StringBuilder();
        for (ContactEntry contact : contacts) {
            builder.append("BEGIN:VCARD\\n")
                    .append("VERSION:3.0\\n")
                    .append("FN:").append(vcardEscape(contact.getDisplayName())).append("\\n")
                    .append("EMAIL:").append(vcardEscape(contact.getEmail())).append("\\n");
            if (StringUtils.hasText(contact.getNote())) {
                builder.append("NOTE:").append(vcardEscape(contact.getNote())).append("\\n");
            }
            builder.append("END:VCARD\\n");
        }
        return builder.toString();
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\"", "\"\"");
        if (normalized.contains(",") || normalized.contains("\"") || normalized.contains("\n")) {
            return "\"" + normalized + "\"";
        }
        return normalized;
    }

    private String vcardEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }

    private String mergeNotes(String first, String second) {
        String normalizedFirst = StringUtils.hasText(first) ? first.trim() : "";
        String normalizedSecond = StringUtils.hasText(second) ? second.trim() : "";
        if (!StringUtils.hasText(normalizedFirst)) {
            return StringUtils.hasText(normalizedSecond) ? normalizedSecond : null;
        }
        if (!StringUtils.hasText(normalizedSecond)) {
            return normalizedFirst;
        }
        if (normalizedFirst.equals(normalizedSecond)) {
            return normalizedFirst;
        }
        return normalizedFirst + "\n" + normalizedSecond;
    }

    private Long parseContactId(String contactId) {
        try {
            return Long.parseLong(contactId);
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid contact id: " + contactId);
        }
    }

    private Set<Long> parseContactIds(List<String> contactIds) {
        Set<Long> ids = new LinkedHashSet<>();
        for (String raw : contactIds) {
            ids.add(parseContactId(raw));
        }
        return ids;
    }

    private String normalizeDuplicateSignature(String displayName, String email) {
        if (StringUtils.hasText(displayName)) {
            return displayName.trim().toLowerCase();
        }
        if (StringUtils.hasText(email)) {
            return email.trim().toLowerCase();
        }
        return null;
    }

    private ContactItemVo toItemVo(ContactEntry entry) {
        return new ContactItemVo(
                String.valueOf(entry.getId()),
                entry.getDisplayName(),
                entry.getEmail(),
                entry.getNote(),
                entry.getIsFavorite() != null && entry.getIsFavorite() == 1,
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    private ContactSuggestionVo toSuggestionVo(ContactSuggestionAggregate aggregate) {
        return new ContactSuggestionVo(
                aggregate.email(),
                aggregate.displayName(),
                aggregate.isFavorite(),
                aggregate.source(),
                aggregate.lastContactAt(),
                aggregate.messageCount()
        );
    }

    private record ContactCsvRow(String displayName, String email, String note, boolean favorite) {
    }

    private record ImportCsvCounters(int totalRows, int created, int updated, int skipped, int invalid) {
        private static ImportCsvCounters empty() {
            return new ImportCsvCounters(0, 0, 0, 0, 0);
        }

        private ImportCsvCounters incrementTotal() {
            return new ImportCsvCounters(totalRows + 1, created, updated, skipped, invalid);
        }

        private ImportCsvCounters incrementCreated() {
            return new ImportCsvCounters(totalRows, created + 1, updated, skipped, invalid);
        }

        private ImportCsvCounters incrementUpdated() {
            return new ImportCsvCounters(totalRows, created, updated + 1, skipped, invalid);
        }

        private ImportCsvCounters incrementSkipped() {
            return new ImportCsvCounters(totalRows, created, updated, skipped + 1, invalid);
        }

        private ImportCsvCounters incrementInvalid() {
            return new ImportCsvCounters(totalRows, created, updated, skipped, invalid + 1);
        }
    }

    private static final class ContactSuggestionAggregate {
        private final String email;
        private String displayName;
        private boolean favorite;
        private boolean fromContact;
        private LocalDateTime lastContactAt;
        private long messageCount;

        private ContactSuggestionAggregate(String email) {
            this.email = email;
        }

        private void acceptContact(ContactEntry contact) {
            fromContact = true;
            favorite = contact.getIsFavorite() != null && contact.getIsFavorite() == 1;
            if (StringUtils.hasText(contact.getDisplayName())) {
                displayName = contact.getDisplayName();
            }
            updateLastContactAt(contact.getUpdatedAt());
        }

        private void acceptHistory(LocalDateTime sentAt) {
            messageCount++;
            updateLastContactAt(sentAt);
        }

        private void updateLastContactAt(LocalDateTime candidate) {
            if (candidate == null) {
                return;
            }
            if (lastContactAt == null || candidate.isAfter(lastContactAt)) {
                lastContactAt = candidate;
            }
        }

        private String source() {
            return fromContact ? "CONTACT" : "HISTORY";
        }

        private int sourcePriority() {
            return fromContact ? 0 : 1;
        }

        private String email() {
            return email;
        }

        private String displayName() {
            return displayName;
        }

        private boolean isFavorite() {
            return favorite;
        }

        private LocalDateTime lastContactAt() {
            return lastContactAt;
        }

        private long messageCount() {
            return messageCount;
        }
    }
}
