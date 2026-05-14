package com.mmmail.server.controller;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.BatchMailActionRequest;
import com.mmmail.server.model.dto.SaveDraftRequest;
import com.mmmail.server.model.dto.SendMailRequest;
import com.mmmail.server.model.dto.V21MailBulkActionRequest;
import com.mmmail.server.model.dto.V21MailMessagesQuery;
import com.mmmail.server.model.vo.MailDetailVo;
import com.mmmail.server.model.vo.MailPageVo;
import com.mmmail.server.model.vo.MailSummaryVo;
import com.mmmail.server.model.vo.MailboxStatsVo;
import com.mmmail.server.model.vo.V21MailFolderVo;
import com.mmmail.server.service.MailService;
import com.mmmail.server.service.PublicBaseUrlResolver;
import com.mmmail.server.util.ClientIpResolver;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Validated
@RestController
@RequestMapping("/api/v2/mail")
public class V21MailController {

    private static final long DEFAULT_PAGE = 1L;
    private static final long DEFAULT_SIZE = 20L;
    private static final long MAX_SIZE = 100L;
    private static final String UNSUPPORTED_FOLDER = "Unsupported v2 mail folder";
    private static final String INVALID_MAIL_ID = "Mail message id is invalid";
    private static final String RECIPIENT_TRUST = "recipient-trust";
    private static final Set<String> SYSTEM_FOLDERS = Set.of(
            "sent", "drafts", "archive", "spam", "trash", "outbox", "scheduled", "snoozed"
    );
    private static final List<FolderSpec> FOLDERS = List.of(
            new FolderSpec("inbox", "Inbox"),
            new FolderSpec("unread", "Unread"),
            new FolderSpec("starred", "Starred"),
            new FolderSpec("sent", "Sent"),
            new FolderSpec("drafts", "Drafts"),
            new FolderSpec("archive", "Archive"),
            new FolderSpec("spam", "Spam"),
            new FolderSpec("trash", "Trash"),
            new FolderSpec("outbox", "Outbox"),
            new FolderSpec("scheduled", "Scheduled"),
            new FolderSpec("snoozed", "Snoozed")
    );

    private final MailService mailService;
    private final PublicBaseUrlResolver publicBaseUrlResolver;
    private final ClientIpResolver clientIpResolver;

    public V21MailController(
            MailService mailService,
            PublicBaseUrlResolver publicBaseUrlResolver,
            ClientIpResolver clientIpResolver
    ) {
        this.mailService = mailService;
        this.publicBaseUrlResolver = publicBaseUrlResolver;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/folders")
    public Result<List<V21MailFolderVo>> folders() {
        MailboxStatsVo stats = mailService.stats(SecurityUtils.currentUserId());
        return Result.success(FOLDERS.stream()
                .map(folder -> toFolderVo(folder, stats.unreadCount()))
                .toList());
    }

    @GetMapping("/messages")
    public Result<MailPageVo> messages(@Valid @ModelAttribute V21MailMessagesQuery query) {
        return Result.success(resolveMessages(SecurityUtils.currentUserId(), query));
    }

    @GetMapping("/threads/{id}")
    public Result<MailDetailVo> thread(@PathVariable String id) {
        return Result.success(mailService.detail(SecurityUtils.currentUserId(), parseMailId(id)));
    }

    @PostMapping("/drafts")
    public Result<MailDetailVo> saveDraft(
            @Valid @RequestBody SaveDraftRequest request,
            HttpServletRequest httpRequest
    ) {
        Long draftId = parseMailId(mailService.saveDraft(SecurityUtils.currentUserId(), request, ip(httpRequest)).draftId());
        return Result.success(mailService.detail(SecurityUtils.currentUserId(), draftId));
    }

    @PatchMapping("/drafts/{id}")
    public Result<MailDetailVo> updateDraft(
            @PathVariable String id,
            @Valid @RequestBody SaveDraftRequest request,
            HttpServletRequest httpRequest
    ) {
        Long draftId = parseMailId(id);
        SaveDraftRequest pathRequest = draftRequestForPath(draftId, request);
        mailService.saveDraft(SecurityUtils.currentUserId(), pathRequest, ip(httpRequest));
        return Result.success(mailService.detail(SecurityUtils.currentUserId(), draftId));
    }

    @PostMapping("/send")
    public Result<Void> send(@Valid @RequestBody SendMailRequest request, HttpServletRequest httpRequest) {
        mailService.send(SecurityUtils.currentUserId(), request, ip(httpRequest), publicBaseUrlResolver.resolve(httpRequest));
        return Result.success(null);
    }

    @PostMapping("/messages/bulk-action")
    public Result<List<MailSummaryVo>> bulkAction(
            @Valid @RequestBody V21MailBulkActionRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.currentUserId();
        BatchMailActionRequest batchRequest = request.toBatchRequest();
        mailService.listSummariesByIds(userId, batchRequest.mailIds());
        mailService.applyBatchAction(userId, batchRequest, ip(httpRequest));
        return Result.success(mailService.listSummariesByIds(userId, batchRequest.mailIds()));
    }

    @GetMapping("/contacts")
    public Result<?> contacts(
            @Valid @ModelAttribute ContactsQuery query,
            HttpServletRequest httpRequest
    ) {
        Long userId = SecurityUtils.currentUserId();
        if (!StringUtils.hasText(query.capability())) {
            return Result.success(mailService.listSenderIdentities(userId, ip(httpRequest)));
        }
        if (RECIPIENT_TRUST.equals(query.capability())) {
            return Result.success(mailService.previewRecipientE2eeStatus(
                    userId,
                    requireToEmail(query.toEmail()),
                    query.fromEmail()
            ));
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported v2 mail contacts capability");
    }

    private MailPageVo resolveMessages(Long userId, V21MailMessagesQuery query) {
        String folder = query.normalizedFolder();
        MessageQueryArgs args = new MessageQueryArgs(page(query), size(query), query.normalizedKeyword());
        if ("inbox".equals(folder)) {
            return mailService.listInbox(userId, args.page(), args.size(), args.keyword(), triageFilters(query));
        }
        if ("unread".equals(folder)) {
            return mailService.listUnread(userId, args.page(), args.size(), args.keyword());
        }
        if ("starred".equals(folder)) {
            return mailService.listStarred(userId, args.page(), args.size(), args.keyword());
        }
        if ("search".equals(folder)) {
            return search(userId, query, args);
        }
        if (SYSTEM_FOLDERS.contains(folder)) {
            return mailService.listFolder(userId, folder.toUpperCase(Locale.ROOT), args.page(), args.size(), args.keyword());
        }
        throw new BizException(ErrorCode.INVALID_ARGUMENT, UNSUPPORTED_FOLDER);
    }

    private MailPageVo search(Long userId, V21MailMessagesQuery query, MessageQueryArgs args) {
        return mailService.search(
                userId,
                args.keyword(),
                null,
                query.unread(),
                query.starred(),
                query.from(),
                query.to(),
                query.label(),
                args.page(),
                args.size()
        );
    }

    private MailService.InboxTriageFilters triageFilters(V21MailMessagesQuery query) {
        return new MailService.InboxTriageFilters(
                query.unread(),
                query.needsReply(),
                query.starred(),
                query.hasAttachments(),
                query.importantContact()
        );
    }

    private SaveDraftRequest draftRequestForPath(Long draftId, SaveDraftRequest request) {
        if (request.draftId() != null && !request.draftId().equals(draftId)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Draft id does not match path");
        }
        return new SaveDraftRequest(draftId, request.toEmail(), request.fromEmail(), request.subject(), request.body(), request.e2ee());
    }

    private V21MailFolderVo toFolderVo(FolderSpec folder, long unreadCount) {
        long folderUnread = ("inbox".equals(folder.key()) || "unread".equals(folder.key())) ? unreadCount : 0L;
        return new V21MailFolderVo(folder.key(), folder.label(), folderUnread);
    }

    private String requireToEmail(String toEmail) {
        if (!StringUtils.hasText(toEmail)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "toEmail is required");
        }
        return toEmail;
    }

    private long page(V21MailMessagesQuery query) {
        long page = query.page() == null ? DEFAULT_PAGE : query.page();
        if (page < 1) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail page must be greater than 0");
        }
        return page;
    }

    private long size(V21MailMessagesQuery query) {
        long size = query.size() == null ? DEFAULT_SIZE : query.size();
        if (size < 1 || size > MAX_SIZE) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail page size must be between 1 and " + MAX_SIZE);
        }
        return size;
    }

    private Long parseMailId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException | NullPointerException exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, INVALID_MAIL_ID);
        }
    }

    private String ip(HttpServletRequest httpRequest) {
        return clientIpResolver.resolve(httpRequest);
    }

    private record FolderSpec(String key, String label) {
    }

    private record MessageQueryArgs(long page, long size, String keyword) {
    }

    private record ContactsQuery(
            String capability,
            @Email String fromEmail,
            @Email String toEmail
    ) {
    }
}
