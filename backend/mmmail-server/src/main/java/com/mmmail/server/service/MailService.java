package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.MailLabelMapper;
import com.mmmail.server.mapper.MailMessageMapper;
import com.mmmail.server.mapper.UserAccountMapper;
import com.mmmail.server.mapper.UserPreferenceMapper;
import com.mmmail.server.model.dto.BatchMailActionRequest;
import com.mmmail.server.model.dto.PreviewMailFilterRequest;
import com.mmmail.server.model.dto.SaveDraftRequest;
import com.mmmail.server.model.dto.SendMailRequest;
import com.mmmail.server.model.entity.MailLabel;
import com.mmmail.server.model.entity.MailMessage;
import com.mmmail.server.model.entity.UserAccount;
import com.mmmail.server.model.entity.UserPreference;
import com.mmmail.server.model.vo.DraftSaveVo;
import com.mmmail.server.model.vo.MailFilterPreviewVo;
import com.mmmail.server.model.vo.MailBodyE2eeVo;
import com.mmmail.server.model.vo.ConversationDetailVo;
import com.mmmail.server.model.vo.ConversationPageVo;
import com.mmmail.server.model.vo.ConversationSummaryVo;
import com.mmmail.server.model.vo.MailActionResultVo;
import com.mmmail.server.model.vo.MailAttachmentDownloadVo;
import com.mmmail.server.model.vo.MailAttachmentUploadVo;
import com.mmmail.server.model.vo.MailDetailVo;
import com.mmmail.server.model.vo.MailE2eeRecipientStatusVo;
import com.mmmail.server.model.vo.MailPageVo;
import com.mmmail.server.model.vo.MailSenderIdentityVo;
import com.mmmail.server.model.vo.RuleResolutionVo;
import com.mmmail.server.model.vo.MailSummaryVo;
import com.mmmail.server.model.vo.MailDeliveryTarget;
import com.mmmail.server.model.vo.MailboxStatsVo;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MailService {

    private static final TypeReference<List<String>> LABEL_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<MailDeliveryTarget>> DELIVERY_TARGET_TYPE = new TypeReference<>() {
    };
    private static final Set<String> SUPPORTED_FOLDERS = Set.of(
            "INBOX",
            "SENT",
            "DRAFTS",
            "ARCHIVE",
            "SPAM",
            "TRASH",
            "OUTBOX",
            "SCHEDULED",
            "SNOOZED"
    );
    private static final int DEFAULT_UNDO_SEND_SECONDS = 10;
    private static final int MAX_OUTBOX_DISPATCH_BATCH = 100;
    private static final int MAX_CONVERSATION_SCAN = 2000;
    private static final int MAX_CONVERSATION_PAGE_SIZE = 100;
    private static final Pattern SEARCH_OPERATOR_PATTERN = Pattern.compile("(?i)(\\b[a-z]+):(\"[^\"]+\"|\\S+)");
    private static final String NO_SUBJECT_KEY = "(no-subject)";
    private static final Set<String> CONVERSATION_ALLOWED_ACTIONS = Set.of(
            "MARK_READ",
            "MARK_UNREAD",
            "MOVE_ARCHIVE",
            "MOVE_TRASH"
    );

    private final MailMessageMapper mailMessageMapper;
    private final UserAccountMapper userAccountMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final MailLabelMapper mailLabelMapper;
    private final BlockedSenderService blockedSenderService;
    private final TrustedSenderService trustedSenderService;
    private final BlockedDomainService blockedDomainService;
    private final TrustedDomainService trustedDomainService;
    private final MailFilterService mailFilterService;
    private final MailFolderService mailFolderService;
    private final SearchHistoryService searchHistoryService;
    private final MailSenderIdentityService mailSenderIdentityService;
    private final PassAliasContactService passAliasContactService;
    private final MailDeliveryRouteService mailDeliveryRouteService;
    private final MailE2eeRecipientDiscoveryService mailE2eeRecipientDiscoveryService;
    private final MailE2eeMessageService mailE2eeMessageService;
    private final MailAttachmentService mailAttachmentService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public MailService(
            MailMessageMapper mailMessageMapper,
            UserAccountMapper userAccountMapper,
            UserPreferenceMapper userPreferenceMapper,
            MailLabelMapper mailLabelMapper,
            BlockedSenderService blockedSenderService,
            TrustedSenderService trustedSenderService,
            BlockedDomainService blockedDomainService,
            TrustedDomainService trustedDomainService,
            MailFilterService mailFilterService,
            MailFolderService mailFolderService,
            SearchHistoryService searchHistoryService,
            MailSenderIdentityService mailSenderIdentityService,
            PassAliasContactService passAliasContactService,
            MailDeliveryRouteService mailDeliveryRouteService,
            MailE2eeRecipientDiscoveryService mailE2eeRecipientDiscoveryService,
            MailE2eeMessageService mailE2eeMessageService,
            MailAttachmentService mailAttachmentService,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.mailMessageMapper = mailMessageMapper;
        this.userAccountMapper = userAccountMapper;
        this.userPreferenceMapper = userPreferenceMapper;
        this.mailLabelMapper = mailLabelMapper;
        this.blockedSenderService = blockedSenderService;
        this.trustedSenderService = trustedSenderService;
        this.blockedDomainService = blockedDomainService;
        this.trustedDomainService = trustedDomainService;
        this.mailFilterService = mailFilterService;
        this.mailFolderService = mailFolderService;
        this.searchHistoryService = searchHistoryService;
        this.mailSenderIdentityService = mailSenderIdentityService;
        this.passAliasContactService = passAliasContactService;
        this.mailDeliveryRouteService = mailDeliveryRouteService;
        this.mailE2eeRecipientDiscoveryService = mailE2eeRecipientDiscoveryService;
        this.mailE2eeMessageService = mailE2eeMessageService;
        this.mailAttachmentService = mailAttachmentService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public MailPageVo listFolder(Long userId, String folder, long page, long size, String keyword) {
        flushDueMails(userId);
        String normalizedFolder = normalizeFolder(folder);
        Page<MailMessage> pager = Page.of(page, size);
        LambdaQueryWrapper<MailMessage> query = new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getFolderType, normalizedFolder)
                .orderByDesc(MailMessage::getSentAt);
        appendKeywordCondition(query, keyword);

        IPage<MailMessage> pageResult = mailMessageMapper.selectPage(pager, query);
        long unread = countUnread(userId);
        List<MailSummaryVo> items = toSummaries(userId, pageResult.getRecords());
        return new MailPageVo(items, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize(), unread);
    }

    public MailPageVo listCustomFolder(Long userId, Long folderId, long page, long size, String keyword) {
        flushDueMails(userId);
        mailFolderService.requireOwnedFolder(userId, folderId);
        Page<MailMessage> pager = Page.of(page, size);
        LambdaQueryWrapper<MailMessage> query = new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getFolderType, "CUSTOM")
                .eq(MailMessage::getCustomFolderId, folderId)
                .orderByDesc(MailMessage::getSentAt);
        appendKeywordCondition(query, keyword);

        IPage<MailMessage> pageResult = mailMessageMapper.selectPage(pager, query);
        List<MailSummaryVo> items = toSummaries(userId, pageResult.getRecords());
        return new MailPageVo(items, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize(), countUnread(userId));
    }

    public MailPageVo listUnread(Long userId, long page, long size, String keyword) {
        flushDueMails(userId);
        Page<MailMessage> pager = Page.of(page, size);
        LambdaQueryWrapper<MailMessage> query = new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getFolderType, "INBOX")
                .eq(MailMessage::getIsRead, 0)
                .eq(MailMessage::getIsDraft, 0)
                .orderByDesc(MailMessage::getSentAt);
        appendKeywordCondition(query, keyword);

        IPage<MailMessage> pageResult = mailMessageMapper.selectPage(pager, query);
        searchHistoryService.recordKeyword(userId, keyword);
        List<MailSummaryVo> items = toSummaries(userId, pageResult.getRecords());
        return new MailPageVo(items, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize(), countUnread(userId));
    }

    public MailPageVo search(
            Long userId,
            String keyword,
            String folder,
            Boolean unread,
            Boolean starred,
            String from,
            String to,
            String label,
            long page,
            long size
    ) {
        flushDueMails(userId);
        SearchOperatorFilters operatorFilters = parseSearchOperatorFilters(keyword);
        String effectiveFolder = StringUtils.hasText(folder) ? folder : operatorFilters.folder();
        Boolean effectiveUnread = unread != null ? unread : operatorFilters.unread();
        Boolean effectiveStarred = starred != null ? starred : operatorFilters.starred();
        String effectiveLabel = StringUtils.hasText(label) ? label : operatorFilters.label();

        Page<MailMessage> pager = Page.of(page, size);
        LambdaQueryWrapper<MailMessage> query = new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .orderByDesc(MailMessage::getSentAt);

        if (StringUtils.hasText(effectiveFolder)) {
            query.eq(MailMessage::getFolderType, normalizeFolder(effectiveFolder));
        }
        if (effectiveUnread != null) {
            query.eq(MailMessage::getIsRead, effectiveUnread ? 0 : 1);
        }
        if (effectiveStarred != null) {
            query.eq(MailMessage::getIsStarred, effectiveStarred ? 1 : 0);
        }
        LocalDateTime fromAt = parseSearchTime(from, "from");
        LocalDateTime toAt = parseSearchTime(to, "to");
        if (fromAt == null) {
            fromAt = operatorFilters.fromAt();
        }
        if (toAt == null) {
            toAt = operatorFilters.toAt();
        }
        if (fromAt != null && toAt != null && fromAt.isAfter(toAt)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "`from` must be earlier than `to`");
        }
        if (fromAt != null) {
            query.ge(MailMessage::getSentAt, fromAt);
        }
        if (toAt != null) {
            query.le(MailMessage::getSentAt, toAt);
        }
        if (StringUtils.hasText(effectiveLabel)) {
            query.like(MailMessage::getLabelsJson, "\"" + effectiveLabel.trim() + "\"");
        }
        if (StringUtils.hasText(operatorFilters.fromEmail())) {
            query.and(wrapper -> wrapper
                    .like(MailMessage::getPeerEmail, operatorFilters.fromEmail())
                    .or()
                    .like(MailMessage::getSenderEmail, operatorFilters.fromEmail()));
        }
        if (StringUtils.hasText(operatorFilters.subjectKeyword())) {
            query.like(MailMessage::getSubject, operatorFilters.subjectKeyword());
        }
        appendKeywordCondition(query, operatorFilters.freeKeyword());

        IPage<MailMessage> pageResult = mailMessageMapper.selectPage(pager, query);
        searchHistoryService.recordKeyword(userId, keyword);
        List<MailSummaryVo> items = toSummaries(userId, pageResult.getRecords());
        return new MailPageVo(items, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize(), countUnread(userId));
    }

    public MailPageVo listStarred(Long userId, long page, long size, String keyword) {
        flushDueMails(userId);
        Page<MailMessage> pager = Page.of(page, size);
        LambdaQueryWrapper<MailMessage> query = new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getIsStarred, 1)
                .eq(MailMessage::getIsDraft, 0)
                .orderByDesc(MailMessage::getSentAt);
        appendKeywordCondition(query, keyword);

        IPage<MailMessage> pageResult = mailMessageMapper.selectPage(pager, query);
        List<MailSummaryVo> items = toSummaries(userId, pageResult.getRecords());
        return new MailPageVo(items, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize(), countUnread(userId));
    }

    public ConversationPageVo listConversations(Long userId, String keyword, String folder, long page, long size) {
        flushDueMails(userId);
        long safePage = Math.max(1, page);
        long safeSize = Math.max(1, Math.min(size, MAX_CONVERSATION_PAGE_SIZE));

        LambdaQueryWrapper<MailMessage> query = new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getIsDraft, 0)
                .orderByDesc(MailMessage::getSentAt)
                .last("limit " + MAX_CONVERSATION_SCAN);

        if (StringUtils.hasText(folder)) {
            query.eq(MailMessage::getFolderType, normalizeFolder(folder));
        } else {
            query.ne(MailMessage::getFolderType, "TRASH");
        }
        appendKeywordCondition(query, keyword);

        List<MailMessage> mails = mailMessageMapper.selectList(query);
        Map<String, ConversationAggregate> grouped = new LinkedHashMap<>();
        for (MailMessage mail : mails) {
            String normalized = normalizeConversationSubject(mail.getSubject());
            ConversationAggregate aggregate = grouped.computeIfAbsent(
                    normalized,
                    key -> new ConversationAggregate(normalized, mail.getSubject())
            );
            aggregate.accept(mail);
        }

        List<ConversationSummaryVo> ordered = grouped.values().stream()
                .map(this::toConversationSummary)
                .sorted(Comparator.comparing(ConversationSummaryVo::latestAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        int fromIndex = (int) ((safePage - 1) * safeSize);
        int toIndex = (int) Math.min(fromIndex + safeSize, ordered.size());
        List<ConversationSummaryVo> items = fromIndex >= ordered.size()
                ? List.of()
                : ordered.subList(fromIndex, toIndex);

        return new ConversationPageVo(items, ordered.size(), safePage, safeSize);
    }

    public ConversationDetailVo conversationDetail(Long userId, String conversationId) {
        flushDueMails(userId);
        String normalizedConversation = decodeConversationId(conversationId);
        List<MailMessage> candidates = mailMessageMapper.selectList(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getIsDraft, 0)
                .orderByAsc(MailMessage::getSentAt)
                .last("limit " + MAX_CONVERSATION_SCAN));

        List<MailSummaryVo> messages = toSummaries(userId, candidates.stream()
                .filter(mail -> normalizeConversationSubject(mail.getSubject()).equals(normalizedConversation))
                .toList());

        if (messages.isEmpty()) {
            throw new BizException(ErrorCode.MAIL_NOT_FOUND, "Conversation not found");
        }

        String subject = messages.stream()
                .map(MailSummaryVo::subject)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("No subject");

        return new ConversationDetailVo(conversationId, subject, messages);
    }

    @Transactional
    public MailActionResultVo applyConversationAction(Long userId, String conversationId, String action, String ipAddress) {
        flushDueMails(userId);
        String normalizedAction = normalizeAction(action);
        if (!CONVERSATION_ALLOWED_ACTIONS.contains(normalizedAction)) {
            throw new BizException(ErrorCode.MAIL_ACTION_INVALID, "Unsupported conversation action");
        }
        String normalizedConversation = decodeConversationId(conversationId);
        List<MailMessage> candidates = mailMessageMapper.selectList(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getIsDraft, 0)
                .orderByDesc(MailMessage::getSentAt)
                .last("limit " + MAX_CONVERSATION_SCAN));

        List<MailMessage> conversationMails = candidates.stream()
                .filter(mail -> normalizeConversationSubject(mail.getSubject()).equals(normalizedConversation))
                .toList();
        if (conversationMails.isEmpty()) {
            throw new BizException(ErrorCode.MAIL_NOT_FOUND, "Conversation not found");
        }

        int affected = applyActionInternal(conversationMails, normalizedAction);
        auditService.record(
                userId,
                "MAIL_CONVERSATION_ACTION",
                normalizedAction + " conversation=" + conversationId + " affected=" + affected,
                ipAddress
        );
        return new MailActionResultVo(affected, stats(userId));
    }

    @Transactional
    public MailDetailVo detail(Long userId, Long mailId) {
        flushDueMails(userId);
        MailMessage mail = loadMail(userId, mailId);
        if (shouldMarkReadOnOpen(mail)) {
            mail.setIsRead(1);
            mail.setUpdatedAt(LocalDateTime.now());
            mailMessageMapper.updateById(mail);
        }

        MailFolderService.MailFolderReference folderRef = resolveCustomFolderRef(userId, mail.getCustomFolderId());
        MailBodyE2eeVo e2ee = mailE2eeMessageService.toDetailVo(mail);

        return new MailDetailVo(
                String.valueOf(mail.getId()),
                String.valueOf(mail.getOwnerId()),
                mail.getSenderEmail(),
                mail.getPeerEmail(),
                mail.getFolderType(),
                folderRef == null ? null : String.valueOf(folderRef.id()),
                folderRef == null ? null : folderRef.name(),
                mail.getSubject(),
                mail.getBodyCiphertext(),
                mail.getIsRead() != null && mail.getIsRead() == 1,
                mail.getIsStarred() != null && mail.getIsStarred() == 1,
                mail.getIsDraft() != null && mail.getIsDraft() == 1,
                mail.getSentAt(),
                parseLabels(mail.getLabelsJson()),
                mailAttachmentService.listForMail(userId, mail.getId()),
                e2ee
        );
    }

    @Transactional
    public void send(Long userId, SendMailRequest request, String ipAddress) {
        UserAccount sender = userAccountMapper.selectById(userId);
        if (sender == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        String senderEmail = mailSenderIdentityService.resolveAuthorizedSenderEmail(userId, request.fromEmail());
        List<MailDeliveryTarget> deliveryTargets = mailDeliveryRouteService.resolveDeliveryTargets(
                userId,
                senderEmail,
                request.toEmail(),
                ipAddress
        );

        MailMessage exists = mailMessageMapper.selectOne(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getIdempotencyKey, request.idempotencyKey())
                .in(MailMessage::getFolderType, List.of("OUTBOX", "SENT", "SCHEDULED")));
        if (exists != null) {
            throw new BizException(ErrorCode.MAIL_IDEMPOTENCY_CONFLICT);
        }

        List<String> labels = validateLabels(userId, request.labels());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledAt = request.scheduledAt() == null ? now : request.scheduledAt();
        if (request.scheduledAt() != null && scheduledAt.isBefore(now)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Scheduled time must be in the future");
        }
        boolean scheduled = request.scheduledAt() != null && scheduledAt.isAfter(now);
        int undoSeconds = resolveUndoSendSeconds(userId);
        boolean useOutbox = !scheduled && undoSeconds > 0;
        LocalDateTime deliveryAt = scheduled ? scheduledAt : (useOutbox ? now.plusSeconds(undoSeconds) : now);
        MailDeliveryTarget primaryTarget = deliveryTargets.get(0);
        MailE2eeMessageService.OutboundBody outboundBody = mailE2eeMessageService.resolveOutboundBody(userId, senderEmail, request);

        MailMessage sent = resolveOutboundMail(userId, request.draftId(), now);
        sent.setOwnerId(userId);
        sent.setPeerId(primaryTarget.ownerId());
        sent.setPeerEmail(primaryTarget.targetEmail());
        sent.setSenderEmail(senderEmail);
        sent.setDirection("OUT");
        sent.setFolderType(scheduled ? "SCHEDULED" : (useOutbox ? "OUTBOX" : "SENT"));
        sent.setSubject(request.subject());
        sent.setBodyCiphertext(outboundBody.bodyCiphertext());
        sent.setBodyE2eeEnabled(outboundBody.bodyE2eeEnabled());
        sent.setBodyE2eeAlgorithm(outboundBody.bodyE2eeAlgorithm());
        sent.setBodyE2eeFingerprintsJson(outboundBody.bodyE2eeFingerprintsJson());
        sent.setIsRead(1);
        sent.setIsStarred(0);
        sent.setIsDraft(0);
        sent.setCustomFolderId(null);
        sent.setLabelsJson(serializeLabels(labels));
        sent.setDeliveryTargetsJson(serializeDeliveryTargets(deliveryTargets));
        sent.setIdempotencyKey(request.idempotencyKey());
        sent.setSentAt(deliveryAt);
        sent.setUpdatedAt(now);

        if (sent.getId() == null) {
            sent.setCreatedAt(now);
            sent.setDeleted(0);
            mailMessageMapper.insert(sent);
        } else {
            mailMessageMapper.updateById(sent);
        }

        if (!scheduled && !useOutbox) {
            deliverInboundCopies(sent, deliveryTargets, now);
        }

        auditService.record(
                userId,
                scheduled ? "MAIL_SCHEDULED" : (useOutbox ? "MAIL_OUTBOX_QUEUED" : "MAIL_SENT"),
                "mail from " + senderEmail + " to " + request.toEmail() + " at " + deliveryAt + ",routeCount=" + deliveryTargets.size(),
                ipAddress
        );
    }

    public MailAttachmentUploadVo uploadDraftAttachment(Long userId, Long draftId, org.springframework.web.multipart.MultipartFile file, String ipAddress) {
        return mailAttachmentService.uploadDraftAttachment(userId, draftId, file, ipAddress);
    }

    public void deleteDraftAttachment(Long userId, Long draftId, Long attachmentId, String ipAddress) {
        mailAttachmentService.deleteDraftAttachment(userId, draftId, attachmentId, ipAddress);
    }

    public MailAttachmentDownloadVo downloadAttachment(Long userId, Long mailId, Long attachmentId, String ipAddress) {
        return mailAttachmentService.downloadAttachment(userId, mailId, attachmentId, ipAddress);
    }

    @Transactional
    public DraftSaveVo saveDraft(Long userId, SaveDraftRequest request, String ipAddress) {
        String senderEmail = mailSenderIdentityService.resolveAuthorizedSenderEmail(userId, request.fromEmail());
        String draftPeerEmail = resolveDraftRecipient(userId, senderEmail, request.toEmail());
        LocalDateTime now = LocalDateTime.now();
        MailMessage draft;
        if (request.draftId() != null) {
            draft = mailMessageMapper.selectOne(new LambdaQueryWrapper<MailMessage>()
                    .eq(MailMessage::getId, request.draftId())
                    .eq(MailMessage::getOwnerId, userId)
                    .eq(MailMessage::getFolderType, "DRAFTS"));
            if (draft == null) {
                throw new BizException(ErrorCode.MAIL_NOT_FOUND);
            }
        } else {
            draft = new MailMessage();
            draft.setOwnerId(userId);
            draft.setPeerEmail(draftPeerEmail);
            draft.setSenderEmail(senderEmail);
            draft.setDirection("OUT");
            draft.setFolderType("DRAFTS");
            draft.setIsDraft(1);
            draft.setIsRead(1);
            draft.setIsStarred(0);
            draft.setLabelsJson("[]");
            draft.setCreatedAt(now);
            draft.setDeleted(0);
        }

        draft.setSubject(request.subject());
        draft.setBodyCiphertext(request.body());
        draft.setBodyE2eeEnabled(0);
        draft.setBodyE2eeAlgorithm(null);
        draft.setBodyE2eeFingerprintsJson(null);
        draft.setPeerEmail(draftPeerEmail);
        draft.setSenderEmail(senderEmail);
        draft.setSentAt(now);
        draft.setUpdatedAt(now);

        if (draft.getId() == null) {
            mailMessageMapper.insert(draft);
        } else {
            mailMessageMapper.updateById(draft);
        }

        auditService.record(userId, "DRAFT_SAVE", "draft saved", ipAddress);
        return new DraftSaveVo(String.valueOf(draft.getId()));
    }

    @Transactional
    public MailActionResultVo applyAction(Long userId, Long mailId, String action, String ipAddress) {
        flushDueMails(userId);
        MailMessage mail = loadMail(userId, mailId);
        String normalizedAction = normalizeAction(action);
        int affected = applyActionInternal(List.of(mail), normalizedAction);
        auditService.record(userId, actionAuditEvent(normalizedAction, false), normalizedAction + " for mail " + mailId, ipAddress);
        return new MailActionResultVo(affected, stats(userId));
    }

    @Transactional
    public MailActionResultVo applyBatchAction(Long userId, BatchMailActionRequest request, String ipAddress) {
        flushDueMails(userId);
        String normalizedAction = normalizeAction(request.action());
        List<MailMessage> mails = mailMessageMapper.selectList(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .in(MailMessage::getId, request.mailIds()));
        int affected = applyActionInternal(mails, normalizedAction);
        auditService.record(userId, actionAuditEvent(normalizedAction, true), normalizedAction + " count=" + affected, ipAddress);
        return new MailActionResultVo(affected, stats(userId));
    }

    @Transactional
    public void updateLabels(Long userId, Long mailId, List<String> labels, String ipAddress) {
        flushDueMails(userId);
        MailMessage mail = loadMail(userId, mailId);
        List<String> validLabels = validateLabels(userId, labels);
        mail.setLabelsJson(serializeLabels(validLabels));
        mail.setUpdatedAt(LocalDateTime.now());
        mailMessageMapper.updateById(mail);
        auditService.record(userId, "MAIL_LABEL_UPDATE", "mail=" + mailId, ipAddress);
    }

    @Transactional
    public MailActionResultVo restoreAllTrash(Long userId, String ipAddress) {
        flushDueMails(userId);
        LocalDateTime now = LocalDateTime.now();
        int affected = mailMessageMapper.update(
                null,
                new LambdaUpdateWrapper<MailMessage>()
                        .eq(MailMessage::getOwnerId, userId)
                        .eq(MailMessage::getFolderType, "TRASH")
                        .set(MailMessage::getFolderType, "INBOX")
                        .set(MailMessage::getUpdatedAt, now)
        );
        auditService.record(userId, "MAIL_TRASH_RESTORE_ALL", "affected=" + affected, ipAddress);
        return new MailActionResultVo(affected, stats(userId));
    }

    @Transactional
    public MailActionResultVo emptyTrash(Long userId, String ipAddress) {
        flushDueMails(userId);
        LocalDateTime now = LocalDateTime.now();
        int affected = mailMessageMapper.update(
                null,
                new LambdaUpdateWrapper<MailMessage>()
                        .eq(MailMessage::getOwnerId, userId)
                        .eq(MailMessage::getFolderType, "TRASH")
                        .set(MailMessage::getDeleted, 1)
                        .set(MailMessage::getUpdatedAt, now)
        );
        auditService.record(userId, "MAIL_TRASH_EMPTY", "affected=" + affected, ipAddress);
        return new MailActionResultVo(affected, stats(userId));
    }

    @Transactional
    public MailActionResultVo restoreAllSpam(Long userId, String ipAddress) {
        flushDueMails(userId);
        LocalDateTime now = LocalDateTime.now();
        int affected = mailMessageMapper.update(
                null,
                new LambdaUpdateWrapper<MailMessage>()
                        .eq(MailMessage::getOwnerId, userId)
                        .eq(MailMessage::getFolderType, "SPAM")
                        .set(MailMessage::getFolderType, "INBOX")
                        .set(MailMessage::getUpdatedAt, now)
        );
        auditService.record(userId, "MAIL_SPAM_RESTORE_ALL", "affected=" + affected, ipAddress);
        return new MailActionResultVo(affected, stats(userId));
    }

    @Transactional
    public MailActionResultVo emptySpam(Long userId, String ipAddress) {
        flushDueMails(userId);
        LocalDateTime now = LocalDateTime.now();
        int affected = mailMessageMapper.update(
                null,
                new LambdaUpdateWrapper<MailMessage>()
                        .eq(MailMessage::getOwnerId, userId)
                        .eq(MailMessage::getFolderType, "SPAM")
                        .set(MailMessage::getDeleted, 1)
                        .set(MailMessage::getUpdatedAt, now)
        );
        auditService.record(userId, "MAIL_SPAM_EMPTY", "affected=" + affected, ipAddress);
        return new MailActionResultVo(affected, stats(userId));
    }

    @Transactional
    public MailActionResultVo snoozeUntil(Long userId, Long mailId, LocalDateTime untilAt, String ipAddress) {
        if (untilAt == null || !untilAt.isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Snooze time must be in the future");
        }
        flushDueMails(userId);
        MailMessage mail = loadMail(userId, mailId);
        LocalDateTime now = LocalDateTime.now();
        boolean changed = snooze(mail, untilAt, now);
        if (changed) {
            mailMessageMapper.updateById(mail);
        }
        auditService.record(userId, "MAIL_SNOOZE_CUSTOM", "mail=" + mailId + " until=" + untilAt, ipAddress);
        return new MailActionResultVo(changed ? 1 : 0, stats(userId));
    }

    @Transactional
    public void undoSend(Long userId, Long mailId, String ipAddress) {
        MailMessage mail = loadMail(userId, mailId);
        LocalDateTime now = LocalDateTime.now();
        if ("SENT".equals(mail.getFolderType()) && "OUT".equals(mail.getDirection())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Undo window has expired");
        }
        if (!"OUTBOX".equals(mail.getFolderType())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Only outbox mail can be undone");
        }
        if (mail.getSentAt() == null || !mail.getSentAt().isAfter(now)) {
            dispatchDueFolder("OUTBOX", "MAIL_OUTBOX_DISPATCH", now);
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Undo window has expired");
        }
        mail.setFolderType("DRAFTS");
        mail.setIsDraft(1);
        mail.setUpdatedAt(now);
        mailMessageMapper.updateById(mail);
        auditService.record(userId, "MAIL_UNDO_SEND", "mail=" + mailId, ipAddress);
    }

    public MailboxStatsVo stats(Long userId) {
        flushDueMails(userId);
        Map<String, Long> folderCounts = new LinkedHashMap<>();
        for (String folder : SUPPORTED_FOLDERS) {
            long count = mailMessageMapper.selectCount(new LambdaQueryWrapper<MailMessage>()
                    .eq(MailMessage::getOwnerId, userId)
                    .eq(MailMessage::getFolderType, folder));
            folderCounts.put(folder, count);
        }

        long unreadCount = countUnread(userId);
        long starredCount = mailMessageMapper.selectCount(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getIsStarred, 1));

        return new MailboxStatsVo(folderCounts, unreadCount, starredCount);
    }

    public List<MailSenderIdentityVo> listSenderIdentities(Long userId, String ipAddress) {
        return mailSenderIdentityService.listSenderIdentities(userId, ipAddress);
    }

    public MailE2eeRecipientStatusVo previewRecipientE2eeStatus(Long userId, String toEmail, String fromEmail) {
        return mailE2eeRecipientDiscoveryService.preview(userId, toEmail, fromEmail);
    }

    public RuleResolutionVo resolveRuleResolution(Long userId, String senderEmail, String ipAddress) {
        InboundRuleDecision decision = evaluateInboundRule(userId, senderEmail, false);
        auditService.record(
                userId,
                "MAIL_RULE_RESOLUTION_QUERY",
                "sender=" + decision.senderEmail() + " reason=" + decision.reason()
                        + " matchedRule=" + (decision.matchedRule() == null ? "-" : decision.matchedRule()),
                ipAddress
        );
        return new RuleResolutionVo(
                decision.senderEmail(),
                decision.senderDomain(),
                decision.trustedSender(),
                decision.blockedSender(),
                decision.trustedDomain(),
                decision.blockedDomain(),
                decision.folder(),
                decision.reason(),
                decision.matchedRule()
        );
    }

    public MailFilterPreviewVo previewMailFilter(Long userId, PreviewMailFilterRequest request, String ipAddress) {
        InboundDeliveryResolution resolution = resolveInboundDelivery(
                userId,
                request.senderEmail(),
                request.subject(),
                request.body()
        );
        auditService.record(
                userId,
                "MAIL_FILTER_PREVIEW",
                "sender=" + resolution.securityDecision().senderEmail()
                        + " filter=" + (resolution.filterMatch() == null ? "-" : resolution.filterMatch().filterName()),
                ipAddress
        );
        return new MailFilterPreviewVo(
                resolution.securityDecision().senderEmail(),
                request.subject(),
                resolution.securityDecision().folder(),
                resolution.folderType(),
                resolution.customFolderId() == null ? null : String.valueOf(resolution.customFolderId()),
                resolution.customFolderName(),
                resolution.labels(),
                resolution.markRead(),
                isBlockedDecision(resolution.securityDecision()),
                resolution.securityDecision().reason(),
                resolution.securityDecision().matchedRule(),
                resolution.filterMatch() == null ? null : String.valueOf(resolution.filterMatch().filterId()),
                resolution.filterMatch() == null ? null : resolution.filterMatch().filterName()
        );
    }

    private int applyActionInternal(List<MailMessage> mails, String action) {
        String normalizedAction = normalizeAction(action);
        LocalDateTime now = LocalDateTime.now();
        int affected = 0;

        for (MailMessage mail : mails) {
            boolean changed = switch (normalizedAction) {
                case "MARK_READ" -> updateReadStatus(mail, 1, now);
                case "MARK_UNREAD" -> updateReadStatus(mail, 0, now);
                case "STAR" -> updateStarStatus(mail, 1, now);
                case "UNSTAR" -> updateStarStatus(mail, 0, now);
                case "MOVE_ARCHIVE" -> moveFolder(mail, "ARCHIVE", now);
                case "MOVE_SPAM" -> moveFolder(mail, "SPAM", now);
                case "MOVE_TRASH" -> moveFolder(mail, "TRASH", now);
                case "MOVE_INBOX" -> moveFolder(mail, "INBOX", now);
                case "SNOOZE_24H" -> snooze(mail, now.plusHours(24), now);
                case "SNOOZE_7D" -> snooze(mail, now.plusDays(7), now);
                case "UNSNOOZE" -> unsnooze(mail, now);
                case "REPORT_PHISHING" -> reportPhishing(mail, now);
                case "REPORT_NOT_PHISHING" -> reportNotPhishing(mail, now);
                case "BLOCK_SENDER" -> quickBlockSender(mail, now);
                case "TRUST_SENDER" -> quickTrustSender(mail, now);
                case "BLOCK_DOMAIN" -> quickBlockDomain(mail, now);
                case "TRUST_DOMAIN" -> quickTrustDomain(mail, now);
                default -> throw new BizException(ErrorCode.MAIL_ACTION_INVALID);
            };

            if (changed) {
                mailMessageMapper.updateById(mail);
                affected++;
            }
        }
        return affected;
    }

    private boolean updateReadStatus(MailMessage mail, int status, LocalDateTime now) {
        if (mail.getIsRead() != null && mail.getIsRead() == status) {
            return false;
        }
        mail.setIsRead(status);
        mail.setUpdatedAt(now);
        return true;
    }

    private boolean updateStarStatus(MailMessage mail, int status, LocalDateTime now) {
        if (mail.getIsStarred() != null && mail.getIsStarred() == status) {
            return false;
        }
        mail.setIsStarred(status);
        mail.setUpdatedAt(now);
        return true;
    }

    private boolean moveFolder(MailMessage mail, String folder, LocalDateTime now) {
        if (folder.equals(mail.getFolderType()) && mail.getCustomFolderId() == null) {
            return false;
        }
        mail.setFolderType(folder);
        mail.setCustomFolderId(null);
        mail.setUpdatedAt(now);
        return true;
    }

    private boolean snooze(MailMessage mail, LocalDateTime snoozeUntil, LocalDateTime now) {
        if (!"INBOX".equals(mail.getFolderType()) && !"SNOOZED".equals(mail.getFolderType())) {
            throw new BizException(ErrorCode.MAIL_ACTION_INVALID, "Only inbox mails can be snoozed");
        }
        if ("SNOOZED".equals(mail.getFolderType()) && snoozeUntil.equals(mail.getSentAt())) {
            return false;
        }
        mail.setFolderType("SNOOZED");
        mail.setCustomFolderId(null);
        mail.setSentAt(snoozeUntil);
        mail.setUpdatedAt(now);
        return true;
    }

    private boolean unsnooze(MailMessage mail, LocalDateTime now) {
        if (!"SNOOZED".equals(mail.getFolderType())) {
            return false;
        }
        mail.setFolderType("INBOX");
        mail.setCustomFolderId(null);
        mail.setSentAt(now);
        mail.setUpdatedAt(now);
        return true;
    }

    private String resolveDraftRecipient(Long userId, String senderEmail, String toEmail) {
        if (!passAliasContactService.isOwnedEnabledAlias(userId, senderEmail)) {
            return toEmail;
        }
        if (!StringUtils.hasText(toEmail)) {
            return toEmail;
        }
        return passAliasContactService.requireReverseAliasTarget(userId, senderEmail, toEmail).reverseAliasEmail();
    }

    private boolean reportPhishing(MailMessage mail, LocalDateTime now) {
        if (!"IN".equals(mail.getDirection())) {
            throw new BizException(ErrorCode.MAIL_ACTION_INVALID, "Only inbound mails can be reported");
        }
        if (!StringUtils.hasText(mail.getPeerEmail())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail sender is missing");
        }
        boolean blockedAdded = blockedSenderService.addBlockedSenderIfAbsent(mail.getOwnerId(), mail.getPeerEmail());
        boolean trustedRemoved = trustedSenderService.removeTrustedSenderIfPresent(mail.getOwnerId(), mail.getPeerEmail());
        boolean moved = moveFolder(mail, "SPAM", now);
        return moved || blockedAdded || trustedRemoved;
    }

    private boolean reportNotPhishing(MailMessage mail, LocalDateTime now) {
        if (!"IN".equals(mail.getDirection())) {
            throw new BizException(ErrorCode.MAIL_ACTION_INVALID, "Only inbound mails can be marked as not phishing");
        }
        if (!StringUtils.hasText(mail.getPeerEmail())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail sender is missing");
        }
        boolean trustedAdded = trustedSenderService.addTrustedSenderIfAbsent(mail.getOwnerId(), mail.getPeerEmail());
        boolean blockedRemoved = blockedSenderService.removeBlockedSenderIfPresent(mail.getOwnerId(), mail.getPeerEmail());
        boolean moved = moveFolder(mail, "INBOX", now);
        return moved || trustedAdded || blockedRemoved;
    }

    private boolean quickBlockSender(MailMessage mail, LocalDateTime now) {
        String senderEmail = requireInboundSender(mail);
        boolean blockedAdded = blockedSenderService.addBlockedSenderIfAbsent(mail.getOwnerId(), senderEmail);
        boolean trustedRemoved = trustedSenderService.removeTrustedSenderIfPresent(mail.getOwnerId(), senderEmail);
        boolean moved = moveFolder(mail, "SPAM", now);
        return moved || blockedAdded || trustedRemoved;
    }

    private boolean quickTrustSender(MailMessage mail, LocalDateTime now) {
        String senderEmail = requireInboundSender(mail);
        boolean trustedAdded = trustedSenderService.addTrustedSenderIfAbsent(mail.getOwnerId(), senderEmail);
        boolean blockedRemoved = blockedSenderService.removeBlockedSenderIfPresent(mail.getOwnerId(), senderEmail);
        boolean moved = moveFolder(mail, "INBOX", now);
        return moved || trustedAdded || blockedRemoved;
    }

    private boolean quickBlockDomain(MailMessage mail, LocalDateTime now) {
        String senderDomain = extractSenderDomain(requireInboundSender(mail));
        if (!StringUtils.hasText(senderDomain)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail sender domain is missing");
        }
        boolean blockedAdded = blockedDomainService.addBlockedDomainIfAbsent(mail.getOwnerId(), senderDomain);
        boolean trustedRemoved = trustedDomainService.removeTrustedDomainIfPresent(mail.getOwnerId(), senderDomain);
        boolean moved = moveFolder(mail, "SPAM", now);
        return moved || blockedAdded || trustedRemoved;
    }

    private boolean quickTrustDomain(MailMessage mail, LocalDateTime now) {
        String senderDomain = extractSenderDomain(requireInboundSender(mail));
        if (!StringUtils.hasText(senderDomain)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail sender domain is missing");
        }
        boolean trustedAdded = trustedDomainService.addTrustedDomainIfAbsent(mail.getOwnerId(), senderDomain);
        boolean blockedRemoved = blockedDomainService.removeBlockedDomainIfPresent(mail.getOwnerId(), senderDomain);
        boolean moved = moveFolder(mail, "INBOX", now);
        return moved || trustedAdded || blockedRemoved;
    }

    private String requireInboundSender(MailMessage mail) {
        if (!"IN".equals(mail.getDirection())) {
            throw new BizException(ErrorCode.MAIL_ACTION_INVALID, "Only inbound mails support this action");
        }
        if (!StringUtils.hasText(mail.getPeerEmail())) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail sender is missing");
        }
        return mail.getPeerEmail();
    }

    private MailMessage loadMail(Long userId, Long mailId) {
        MailMessage mail = mailMessageMapper.selectOne(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getId, mailId)
                .eq(MailMessage::getOwnerId, userId));
        if (mail == null) {
            throw new BizException(ErrorCode.MAIL_NOT_FOUND);
        }
        return mail;
    }

    private SearchOperatorFilters parseSearchOperatorFilters(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return new SearchOperatorFilters(null, null, null, null, null, null, null, null, null);
        }

        String folder = null;
        Boolean unread = null;
        Boolean starred = null;
        LocalDateTime fromAt = null;
        LocalDateTime toAt = null;
        String label = null;
        String fromEmail = null;
        String subjectKeyword = null;

        Matcher matcher = SEARCH_OPERATOR_PATTERN.matcher(keyword);
        StringBuffer remainder = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase(Locale.ROOT);
            String value = stripOperatorValue(matcher.group(2));
            if (!StringUtils.hasText(value)) {
                throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid `%s` operator value".formatted(key));
            }
            switch (key) {
                case "from" -> fromEmail = value;
                case "subject" -> subjectKeyword = value;
                case "in" -> folder = normalizeFolder(value);
                case "label" -> label = value;
                case "is" -> {
                    String normalized = value.toLowerCase(Locale.ROOT);
                    switch (normalized) {
                        case "unread" -> unread = true;
                        case "read" -> unread = false;
                        case "starred" -> starred = true;
                        case "unstarred" -> starred = false;
                        default -> throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported `is` value");
                    }
                }
                case "after" -> fromAt = parseOperatorDate(value, "after", false);
                case "before" -> toAt = parseOperatorDate(value, "before", true);
                default -> throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported search operator: " + key);
            }
            matcher.appendReplacement(remainder, " ");
        }
        matcher.appendTail(remainder);

        String freeKeyword = remainder.toString().replaceAll("\\s+", " ").trim();
        return new SearchOperatorFilters(
                StringUtils.hasText(freeKeyword) ? freeKeyword : null,
                fromEmail,
                subjectKeyword,
                folder,
                unread,
                starred,
                label,
                fromAt,
                toAt
        );
    }

    private String stripOperatorValue(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private LocalDateTime parseOperatorDate(String value, String operator, boolean endOfDay) {
        try {
            if (value.contains("T")) {
                return LocalDateTime.parse(value);
            }
            LocalDate date = LocalDate.parse(value);
            return endOfDay ? date.atTime(LocalTime.of(23, 59, 59)) : date.atStartOfDay();
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid `%s` date format".formatted(operator));
        }
    }

    private void appendKeywordCondition(LambdaQueryWrapper<MailMessage> query, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        query.and(wrapper -> wrapper
                .like(MailMessage::getSubject, keyword)
                .or()
                .like(MailMessage::getBodyCiphertext, keyword)
                .or()
                .like(MailMessage::getPeerEmail, keyword)
                .or()
                .like(MailMessage::getSenderEmail, keyword));
    }

    private record SearchOperatorFilters(
            String freeKeyword,
            String fromEmail,
            String subjectKeyword,
            String folder,
            Boolean unread,
            Boolean starred,
            String label,
            LocalDateTime fromAt,
            LocalDateTime toAt
    ) {
    }

    private String normalizeFolder(String folder) {
        if (!StringUtils.hasText(folder)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Folder is required");
        }
        String normalized = folder.toUpperCase();
        if (!SUPPORTED_FOLDERS.contains(normalized)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported folder");
        }
        return normalized;
    }

    private String normalizeAction(String action) {
        if (!StringUtils.hasText(action)) {
            throw new BizException(ErrorCode.MAIL_ACTION_INVALID);
        }
        return action.toUpperCase();
    }

    private long countUnread(Long userId) {
        return mailMessageMapper.selectCount(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getFolderType, "INBOX")
                .eq(MailMessage::getIsRead, 0)
                .eq(MailMessage::getIsDraft, 0)
                .le(MailMessage::getSentAt, LocalDateTime.now()));
    }

    private void flushDueMails(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        dispatchDueFolder("OUTBOX", "MAIL_OUTBOX_DISPATCH", now);
        dispatchDueFolder("SCHEDULED", "MAIL_SCHEDULED_DISPATCH", now);
        mailMessageMapper.update(
                null,
                new LambdaUpdateWrapper<MailMessage>()
                        .eq(MailMessage::getOwnerId, userId)
                        .eq(MailMessage::getFolderType, "SNOOZED")
                        .le(MailMessage::getSentAt, now)
                        .set(MailMessage::getFolderType, "INBOX")
                        .set(MailMessage::getCustomFolderId, null)
                        .set(MailMessage::getUpdatedAt, now)
        );
    }

    private void dispatchDueFolder(String sourceFolder, String auditEvent, LocalDateTime now) {
        List<MailMessage> dueMessages = mailMessageMapper.selectList(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getFolderType, sourceFolder)
                .le(MailMessage::getSentAt, now)
                .orderByAsc(MailMessage::getSentAt)
                .last("limit " + MAX_OUTBOX_DISPATCH_BATCH));
        if (dueMessages.isEmpty()) {
            return;
        }
        for (MailMessage message : dueMessages) {
            int transitioned = mailMessageMapper.update(
                    null,
                    new LambdaUpdateWrapper<MailMessage>()
                            .eq(MailMessage::getId, message.getId())
                            .eq(MailMessage::getFolderType, sourceFolder)
                            .le(MailMessage::getSentAt, now)
                            .set(MailMessage::getFolderType, "SENT")
                            .set(MailMessage::getCustomFolderId, null)
                            .set(MailMessage::getUpdatedAt, now)
            );
            if (transitioned == 0) {
                continue;
            }
            List<MailDeliveryTarget> deliveryTargets = resolveStoredDeliveryTargets(message);
            deliverInboundCopies(message, deliveryTargets, now);
            auditService.record(
                    message.getOwnerId(),
                    auditEvent,
                    "mail=" + message.getId() + ",routeCount=" + deliveryTargets.size(),
                    "system"
            );
        }
    }

    private InboundRuleDecision evaluateInboundRule(Long ownerId, String senderEmail, boolean scheduled) {
        String normalizedSender = StringUtils.hasText(senderEmail) ? senderEmail.trim().toLowerCase() : "";
        String senderDomain = extractSenderDomain(normalizedSender);

        boolean trustedSender = StringUtils.hasText(normalizedSender)
                && trustedSenderService.isTrustedSender(ownerId, normalizedSender);
        boolean blockedSender = StringUtils.hasText(normalizedSender)
                && blockedSenderService.isBlockedSender(ownerId, normalizedSender);
        String matchedTrustedDomain = StringUtils.hasText(senderDomain)
                ? trustedDomainService.findMatchedTrustedDomain(ownerId, senderDomain)
                : null;
        String matchedBlockedDomain = StringUtils.hasText(senderDomain)
                ? blockedDomainService.findMatchedBlockedDomain(ownerId, senderDomain)
                : null;
        boolean trustedDomain = matchedTrustedDomain != null;
        boolean blockedDomain = matchedBlockedDomain != null;

        if (trustedSender) {
            return new InboundRuleDecision(
                    normalizedSender, senderDomain, true, blockedSender, trustedDomain, blockedDomain,
                    scheduled ? "SNOOZED" : "INBOX", "TRUSTED_SENDER", normalizedSender
            );
        }
        if (blockedSender) {
            return new InboundRuleDecision(
                    normalizedSender, senderDomain, false, true, trustedDomain, blockedDomain,
                    "SPAM", "BLOCKED_SENDER", normalizedSender
            );
        }
        if (trustedDomain) {
            return new InboundRuleDecision(
                    normalizedSender, senderDomain, false, false, true, blockedDomain,
                    scheduled ? "SNOOZED" : "INBOX", "TRUSTED_DOMAIN", matchedTrustedDomain
            );
        }
        if (blockedDomain) {
            return new InboundRuleDecision(
                    normalizedSender, senderDomain, false, false, false, true,
                    "SPAM", "BLOCKED_DOMAIN", matchedBlockedDomain
            );
        }
        return new InboundRuleDecision(
                normalizedSender, senderDomain, false, false, false, false,
                scheduled ? "SNOOZED" : "INBOX", "DEFAULT", null
        );
    }

    private InboundDeliveryResolution resolveInboundDelivery(
            Long ownerId,
            String senderEmail,
            String subject,
            String body
    ) {
        InboundRuleDecision securityDecision = evaluateInboundRule(ownerId, senderEmail, false);
        if (isBlockedDecision(securityDecision)) {
            return new InboundDeliveryResolution(securityDecision, securityDecision.folder(), null, null, List.of(), false, null);
        }

        MailFilterService.MailFilterMatch filterMatch = mailFilterService.evaluateFirstMatch(ownerId, senderEmail, subject, body);
        if (filterMatch == null) {
            return new InboundDeliveryResolution(securityDecision, securityDecision.folder(), null, null, List.of(), false, null);
        }

        if (filterMatch.targetCustomFolderId() != null) {
            return new InboundDeliveryResolution(
                    securityDecision,
                    "CUSTOM",
                    filterMatch.targetCustomFolderId(),
                    filterMatch.targetCustomFolderName(),
                    filterMatch.labels(),
                    filterMatch.markRead(),
                    filterMatch
            );
        }
        return new InboundDeliveryResolution(
                securityDecision,
                StringUtils.hasText(filterMatch.targetFolder()) ? filterMatch.targetFolder() : securityDecision.folder(),
                null,
                null,
                filterMatch.labels(),
                filterMatch.markRead(),
                filterMatch
        );
    }

    private boolean isBlockedDecision(InboundRuleDecision decision) {
        return "BLOCKED_SENDER".equals(decision.reason()) || "BLOCKED_DOMAIN".equals(decision.reason());
    }

    private String extractSenderDomain(String senderEmail) {
        if (!StringUtils.hasText(senderEmail)) {
            return "";
        }
        String normalizedEmail = senderEmail.trim().toLowerCase();
        int atIndex = normalizedEmail.lastIndexOf('@');
        if (atIndex < 0 || atIndex >= normalizedEmail.length() - 1) {
            return "";
        }
        return normalizedEmail.substring(atIndex + 1);
    }

    private String actionAuditEvent(String normalizedAction, boolean batch) {
        if ("REPORT_PHISHING".equals(normalizedAction)) {
            return "MAIL_REPORT_PHISHING";
        }
        if ("REPORT_NOT_PHISHING".equals(normalizedAction)) {
            return "MAIL_REPORT_NOT_PHISHING";
        }
        if ("BLOCK_SENDER".equals(normalizedAction)) {
            return "MAIL_QUICK_BLOCK_SENDER";
        }
        if ("TRUST_SENDER".equals(normalizedAction)) {
            return "MAIL_QUICK_TRUST_SENDER";
        }
        if ("BLOCK_DOMAIN".equals(normalizedAction)) {
            return "MAIL_QUICK_BLOCK_DOMAIN";
        }
        if ("TRUST_DOMAIN".equals(normalizedAction)) {
            return "MAIL_QUICK_TRUST_DOMAIN";
        }
        return batch ? "MAIL_BATCH_ACTION" : "MAIL_ACTION";
    }

    private int resolveUndoSendSeconds(Long userId) {
        UserPreference preference = userPreferenceMapper.selectOne(new LambdaQueryWrapper<UserPreference>()
                .eq(UserPreference::getOwnerId, userId));
        if (preference == null || preference.getUndoSendSeconds() == null) {
            return DEFAULT_UNDO_SEND_SECONDS;
        }
        return Math.max(0, preference.getUndoSendSeconds());
    }

    private LocalDateTime parseSearchTime(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid datetime for " + fieldName);
        }
    }

    private String buildInboundIdempotencyKey(Long outboxMailId) {
        return buildInboundIdempotencyKey(outboxMailId, 0);
    }

    private String buildInboundIdempotencyKey(Long outboxMailId, int targetIndex) {
        if (targetIndex <= 0) {
            return "inbox-" + outboxMailId;
        }
        return "inbox-" + outboxMailId + '-' + targetIndex;
    }

    private String serializeDeliveryTargets(List<MailDeliveryTarget> deliveryTargets) {
        try {
            return objectMapper.writeValueAsString(deliveryTargets == null ? List.of() : deliveryTargets);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to serialize delivery targets");
        }
    }

    private List<MailDeliveryTarget> resolveStoredDeliveryTargets(MailMessage message) {
        if (StringUtils.hasText(message.getDeliveryTargetsJson())) {
            try {
                List<MailDeliveryTarget> targets = objectMapper.readValue(message.getDeliveryTargetsJson(), DELIVERY_TARGET_TYPE);
                if (targets == null || targets.isEmpty()) {
                    throw new BizException(ErrorCode.INTERNAL_ERROR, "Mail delivery target is empty for mail " + message.getId());
                }
                return targets;
            } catch (BizException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to parse delivery targets for mail " + message.getId());
            }
        }
        if (message.getPeerId() == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Mail delivery target is missing for mail " + message.getId());
        }
        return List.of(new MailDeliveryTarget(message.getPeerId(), message.getPeerEmail(), message.getPeerEmail()));
    }

    private void deliverInboundCopies(MailMessage outbound, List<MailDeliveryTarget> deliveryTargets, LocalDateTime now) {
        for (int index = 0; index < deliveryTargets.size(); index++) {
            MailDeliveryTarget deliveryTarget = deliveryTargets.get(index);
            String inboundKey = buildInboundIdempotencyKey(outbound.getId(), index);
            MailMessage existingInbox = mailMessageMapper.selectOne(new LambdaQueryWrapper<MailMessage>()
                    .eq(MailMessage::getOwnerId, deliveryTarget.ownerId())
                    .eq(MailMessage::getIdempotencyKey, inboundKey));
            if (existingInbox != null) {
                continue;
            }
            InboundDeliveryResolution resolution = resolveInboundDelivery(
                    deliveryTarget.ownerId(),
                    outbound.getSenderEmail(),
                    outbound.getSubject(),
                    outbound.getBodyCiphertext()
            );
            MailMessage inbox = new MailMessage();
            inbox.setOwnerId(deliveryTarget.ownerId());
            inbox.setPeerId(outbound.getOwnerId());
            inbox.setPeerEmail(outbound.getSenderEmail());
            inbox.setSenderEmail(outbound.getSenderEmail());
            inbox.setDirection("IN");
            inbox.setFolderType(resolution.folderType());
            inbox.setCustomFolderId(resolution.customFolderId());
            inbox.setSubject(outbound.getSubject());
            inbox.setBodyCiphertext(outbound.getBodyCiphertext());
            inbox.setBodyE2eeEnabled(outbound.getBodyE2eeEnabled());
            inbox.setBodyE2eeAlgorithm(outbound.getBodyE2eeAlgorithm());
            inbox.setBodyE2eeFingerprintsJson(outbound.getBodyE2eeFingerprintsJson());
            inbox.setIsRead(resolution.markRead() ? 1 : 0);
            inbox.setIsStarred(0);
            inbox.setIsDraft(0);
            inbox.setLabelsJson(serializeLabels(resolution.labels()));
            inbox.setIdempotencyKey(inboundKey);
            inbox.setSentAt(outbound.getSentAt());
            inbox.setCreatedAt(now);
            inbox.setUpdatedAt(now);
            inbox.setDeleted(0);
            try {
                mailMessageMapper.insert(inbox);
                mailAttachmentService.replicateToRecipients(
                        outbound.getId(),
                        List.of(deliveryTarget),
                        List.of(inbox.getId()),
                        now
                );
                recordInboundFilterAudit(deliveryTarget.ownerId(), inbox.getId(), resolution);
            } catch (DuplicateKeyException ignored) {
                // Multiple dispatch attempts may race; keep operation idempotent.
            }
        }
    }

    private MailMessage resolveOutboundMail(Long userId, Long draftId, LocalDateTime now) {
        if (draftId == null) {
            return new MailMessage();
        }
        MailMessage draft = mailMessageMapper.selectOne(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getId, draftId)
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getFolderType, "DRAFTS")
                .eq(MailMessage::getIsDraft, 1));
        if (draft == null) {
            throw new BizException(ErrorCode.MAIL_NOT_FOUND, "Draft mail not found");
        }
        draft.setUpdatedAt(now);
        return draft;
    }

    private void recordInboundFilterAudit(Long userId, Long mailId, InboundDeliveryResolution resolution) {
        if (resolution.filterMatch() == null) {
            return;
        }
        auditService.record(
                userId,
                "MAIL_FILTER_APPLIED",
                "mail=" + mailId
                        + ",filter=" + resolution.filterMatch().filterName()
                        + ",folder=" + resolution.folderType()
                        + ",customFolderId=" + (resolution.customFolderId() == null ? "-" : resolution.customFolderId()),
                "system"
        );
    }

    private ConversationSummaryVo toConversationSummary(ConversationAggregate aggregate) {
        List<String> participants = new ArrayList<>(aggregate.participants());
        if (participants.size() > 3) {
            participants = participants.subList(0, 3);
        }
        return new ConversationSummaryVo(
                encodeConversationId(aggregate.normalizedSubject()),
                aggregate.displaySubject(),
                participants,
                aggregate.messageCount(),
                aggregate.unreadCount(),
                aggregate.latestAt()
        );
    }

    private String normalizeConversationSubject(String subject) {
        if (!StringUtils.hasText(subject)) {
            return NO_SUBJECT_KEY;
        }
        String normalized = subject.trim().toLowerCase();
        while (normalized.startsWith("re:") || normalized.startsWith("fwd:")) {
            normalized = normalized.substring(normalized.indexOf(':') + 1).trim();
        }
        return StringUtils.hasText(normalized) ? normalized : NO_SUBJECT_KEY;
    }

    private String encodeConversationId(String normalizedSubject) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(normalizedSubject.getBytes(StandardCharsets.UTF_8));
    }

    private String decodeConversationId(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Conversation id is required");
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(conversationId), StandardCharsets.UTF_8);
            return StringUtils.hasText(decoded) ? decoded : NO_SUBJECT_KEY;
        } catch (IllegalArgumentException ex) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Invalid conversation id");
        }
    }

    private List<String> validateLabels(Long userId, List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return List.of();
        }

        List<String> normalized = labels.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();

        if (normalized.size() > 20) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Too many labels");
        }

        Set<String> existing = mailLabelMapper.selectList(new LambdaQueryWrapper<MailLabel>()
                        .eq(MailLabel::getOwnerId, userId)
                        .in(MailLabel::getName, normalized))
                .stream()
                .map(MailLabel::getName)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        if (existing.size() != normalized.size()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Labels contain unknown items");
        }

        return normalized;
    }

    private String serializeLabels(List<String> labels) {
        try {
            return objectMapper.writeValueAsString(labels == null ? List.of() : labels);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to serialize labels");
        }
    }

    private List<String> parseLabels(String labelsJson) {
        if (!StringUtils.hasText(labelsJson)) {
            return List.of();
        }
        try {
            List<String> labels = objectMapper.readValue(labelsJson, LABEL_TYPE);
            return labels == null ? List.of() : labels;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<MailSummaryVo> toSummaries(Long userId, List<MailMessage> messages) {
        Map<Long, MailFolderService.MailFolderReference> folderRefs = resolveCustomFolderRefs(userId, messages);
        return messages.stream()
                .map(message -> toSummary(message, lookupCustomFolderRef(folderRefs, message.getCustomFolderId())))
                .toList();
    }

    private Map<Long, MailFolderService.MailFolderReference> resolveCustomFolderRefs(Long userId, List<MailMessage> messages) {
        return mailFolderService.resolveFolderRefs(
                userId,
                messages.stream().map(MailMessage::getCustomFolderId).toList()
        );
    }

    private MailFolderService.MailFolderReference resolveCustomFolderRef(Long userId, Long customFolderId) {
        if (customFolderId == null) {
            return null;
        }
        return mailFolderService.resolveOwnedFolderReference(userId, customFolderId);
    }

    private MailFolderService.MailFolderReference lookupCustomFolderRef(
            Map<Long, MailFolderService.MailFolderReference> folderRefs,
            Long customFolderId
    ) {
        if (customFolderId == null || folderRefs.isEmpty()) {
            return null;
        }
        return folderRefs.get(customFolderId);
    }

    private boolean shouldMarkReadOnOpen(MailMessage mail) {
        if (mail.getIsRead() == null || mail.getIsRead() == 1) {
            return false;
        }
        return "INBOX".equals(mail.getFolderType()) || "CUSTOM".equals(mail.getFolderType());
    }

    private MailSummaryVo toSummary(MailMessage message, MailFolderService.MailFolderReference folderRef) {
        String preview = mailE2eeMessageService.resolvePreview(message);

        return new MailSummaryVo(
                String.valueOf(message.getId()),
                String.valueOf(message.getOwnerId()),
                message.getSenderEmail(),
                message.getPeerEmail(),
                message.getFolderType(),
                folderRef == null ? null : String.valueOf(folderRef.id()),
                folderRef == null ? null : folderRef.name(),
                message.getSubject(),
                preview,
                message.getIsRead() != null && message.getIsRead() == 1,
                message.getIsStarred() != null && message.getIsStarred() == 1,
                message.getIsDraft() != null && message.getIsDraft() == 1,
                message.getSentAt(),
                parseLabels(message.getLabelsJson())
        );
    }


    private static final class ConversationAggregate {
        private final String normalizedSubject;
        private final String displaySubject;
        private final Set<String> participants = new LinkedHashSet<>();
        private long messageCount;
        private long unreadCount;
        private LocalDateTime latestAt;

        private ConversationAggregate(String normalizedSubject, String firstSubject) {
            this.normalizedSubject = normalizedSubject;
            this.displaySubject = StringUtils.hasText(firstSubject) ? firstSubject.trim() : "No subject";
        }

        private void accept(MailMessage message) {
            messageCount++;
            if (message.getIsRead() != null && message.getIsRead() == 0) {
                unreadCount++;
            }
            if (StringUtils.hasText(message.getPeerEmail())) {
                participants.add(message.getPeerEmail().trim().toLowerCase());
            }
            if (message.getSentAt() != null && (latestAt == null || message.getSentAt().isAfter(latestAt))) {
                latestAt = message.getSentAt();
            }
        }

        private String normalizedSubject() {
            return normalizedSubject;
        }

        private String displaySubject() {
            return displaySubject;
        }

        private Set<String> participants() {
            return participants;
        }

        private long messageCount() {
            return messageCount;
        }

        private long unreadCount() {
            return unreadCount;
        }

        private LocalDateTime latestAt() {
            return latestAt;
        }
    }

    private record InboundDeliveryResolution(
            InboundRuleDecision securityDecision,
            String folderType,
            Long customFolderId,
            String customFolderName,
            List<String> labels,
            boolean markRead,
            MailFilterService.MailFilterMatch filterMatch
    ) {
    }

    private record InboundRuleDecision(
            String senderEmail,
            String senderDomain,
            boolean trustedSender,
            boolean blockedSender,
            boolean trustedDomain,
            boolean blockedDomain,
            String folder,
            String reason,
            String matchedRule
    ) {
    }
}
