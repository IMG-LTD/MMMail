package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.dto.BatchMailActionRequest;
import com.mmmail.server.model.dto.ConversationActionRequest;
import com.mmmail.server.model.dto.MailActionRequest;
import com.mmmail.server.model.dto.SaveDraftRequest;
import com.mmmail.server.model.dto.SendMailRequest;
import com.mmmail.server.model.dto.SnoozeUntilRequest;
import com.mmmail.server.model.dto.UploadDraftAttachmentRequest;
import com.mmmail.server.model.dto.UpdateMailLabelsRequest;
import com.mmmail.server.model.vo.DraftSaveVo;
import com.mmmail.server.model.vo.ConversationDetailVo;
import com.mmmail.server.model.vo.ConversationPageVo;
import com.mmmail.server.model.vo.MailActionResultVo;
import com.mmmail.server.model.vo.MailAttachmentDownloadVo;
import com.mmmail.server.model.vo.MailAttachmentUploadVo;
import com.mmmail.server.model.vo.MailDetailVo;
import com.mmmail.server.model.vo.MailE2eeRecipientStatusVo;
import com.mmmail.server.model.vo.MailPageVo;
import com.mmmail.server.model.vo.MailSenderIdentityVo;
import com.mmmail.server.model.vo.MailboxStatsVo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.mmmail.server.service.MailService;
import com.mmmail.server.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/mails")
public class MailController {

    private final MailService mailService;

    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    @GetMapping("/inbox")
    public Result<MailPageVo> inbox(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(defaultValue = "") String keyword
    ) {
        return Result.success(mailService.listFolder(SecurityUtils.currentUserId(), "INBOX", page, size, keyword));
    }

    @GetMapping("/unread")
    public Result<MailPageVo> unread(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(defaultValue = "") String keyword
    ) {
        return Result.success(mailService.listUnread(SecurityUtils.currentUserId(), page, size, keyword));
    }

    @GetMapping("/sent")
    public Result<MailPageVo> sent(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(defaultValue = "") String keyword
    ) {
        return Result.success(mailService.listFolder(SecurityUtils.currentUserId(), "SENT", page, size, keyword));
    }

    @GetMapping("/drafts")
    public Result<MailPageVo> drafts(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(defaultValue = "") String keyword
    ) {
        return Result.success(mailService.listFolder(SecurityUtils.currentUserId(), "DRAFTS", page, size, keyword));
    }

    @GetMapping("/archive")
    public Result<MailPageVo> archive(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(defaultValue = "") String keyword
    ) {
        return Result.success(mailService.listFolder(SecurityUtils.currentUserId(), "ARCHIVE", page, size, keyword));
    }

    @GetMapping("/spam")
    public Result<MailPageVo> spam(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(defaultValue = "") String keyword
    ) {
        return Result.success(mailService.listFolder(SecurityUtils.currentUserId(), "SPAM", page, size, keyword));
    }

    @GetMapping("/trash")
    public Result<MailPageVo> trash(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(defaultValue = "") String keyword
    ) {
        return Result.success(mailService.listFolder(SecurityUtils.currentUserId(), "TRASH", page, size, keyword));
    }

    @GetMapping("/outbox")
    public Result<MailPageVo> outbox(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(defaultValue = "") String keyword
    ) {
        return Result.success(mailService.listFolder(SecurityUtils.currentUserId(), "OUTBOX", page, size, keyword));
    }

    @GetMapping("/scheduled")
    public Result<MailPageVo> scheduled(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(defaultValue = "") String keyword
    ) {
        return Result.success(mailService.listFolder(SecurityUtils.currentUserId(), "SCHEDULED", page, size, keyword));
    }

    @GetMapping("/snoozed")
    public Result<MailPageVo> snoozed(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(defaultValue = "") String keyword
    ) {
        return Result.success(mailService.listFolder(SecurityUtils.currentUserId(), "SNOOZED", page, size, keyword));
    }

    @GetMapping("/starred")
    public Result<MailPageVo> starred(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(defaultValue = "") String keyword
    ) {
        return Result.success(mailService.listStarred(SecurityUtils.currentUserId(), page, size, keyword));
    }

    @GetMapping("/search")
    public Result<MailPageVo> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String folder,
            @RequestParam(required = false) Boolean unread,
            @RequestParam(required = false) Boolean starred,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String label,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return Result.success(mailService.search(
                SecurityUtils.currentUserId(),
                keyword,
                folder,
                unread,
                starred,
                from,
                to,
                label,
                page,
                size
        ));
    }

    @GetMapping("/conversations")
    public Result<ConversationPageVo> conversations(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) String folder,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return Result.success(mailService.listConversations(SecurityUtils.currentUserId(), keyword, folder, page, size));
    }

    @GetMapping("/conversations/{conversationId}")
    public Result<ConversationDetailVo> conversationDetail(@PathVariable String conversationId) {
        return Result.success(mailService.conversationDetail(SecurityUtils.currentUserId(), conversationId));
    }

    @PostMapping("/conversations/{conversationId}/actions")
    public Result<MailActionResultVo> conversationAction(
            @PathVariable String conversationId,
            @Valid @RequestBody ConversationActionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(mailService.applyConversationAction(
                SecurityUtils.currentUserId(),
                conversationId,
                request.action(),
                httpRequest.getRemoteAddr()
        ));
    }

    @GetMapping("/stats")
    public Result<MailboxStatsVo> stats() {
        return Result.success(mailService.stats(SecurityUtils.currentUserId()));
    }

    @GetMapping("/identities")
    public Result<List<MailSenderIdentityVo>> identities(HttpServletRequest httpRequest) {
        return Result.success(mailService.listSenderIdentities(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @GetMapping("/e2ee-recipient-status")
    public Result<MailE2eeRecipientStatusVo> e2eeRecipientStatus(
            @RequestParam @Email @NotBlank String toEmail,
            @RequestParam(required = false) @Email String fromEmail
    ) {
        return Result.success(mailService.previewRecipientE2eeStatus(SecurityUtils.currentUserId(), toEmail, fromEmail));
    }

    @GetMapping("/{mailId}")
    public Result<MailDetailVo> detail(@PathVariable Long mailId) {
        return Result.success(mailService.detail(SecurityUtils.currentUserId(), mailId));
    }

    @PostMapping("/send")
    public Result<Void> send(@Valid @RequestBody SendMailRequest request, HttpServletRequest httpRequest) {
        mailService.send(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @PostMapping("/drafts")
    public Result<DraftSaveVo> saveDraft(@Valid @RequestBody SaveDraftRequest request, HttpServletRequest httpRequest) {
        return Result.success(mailService.saveDraft(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/drafts/{draftId}/attachments")
    public Result<MailAttachmentUploadVo> uploadDraftAttachment(
            @PathVariable Long draftId,
            @Valid @ModelAttribute UploadDraftAttachmentRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(mailService.uploadDraftAttachment(
                SecurityUtils.currentUserId(),
                draftId,
                request,
                httpRequest.getRemoteAddr()
        ));
    }

    @DeleteMapping("/drafts/{draftId}/attachments/{attachmentId}")
    public Result<Void> deleteDraftAttachment(
            @PathVariable Long draftId,
            @PathVariable Long attachmentId,
            HttpServletRequest httpRequest
    ) {
        mailService.deleteDraftAttachment(SecurityUtils.currentUserId(), draftId, attachmentId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @GetMapping("/{mailId}/attachments/{attachmentId}/download")
    public ResponseEntity<ByteArrayResource> downloadAttachment(
            @PathVariable Long mailId,
            @PathVariable Long attachmentId,
            HttpServletRequest httpRequest
    ) {
        MailAttachmentDownloadVo file = mailService.downloadAttachment(
                SecurityUtils.currentUserId(),
                mailId,
                attachmentId,
                httpRequest.getRemoteAddr()
        );
        ByteArrayResource resource = new ByteArrayResource(file.content());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.content().length)
                .body(resource);
    }

    @PostMapping("/{mailId}/undo-send")
    public Result<Void> undoSend(@PathVariable Long mailId, HttpServletRequest httpRequest) {
        mailService.undoSend(SecurityUtils.currentUserId(), mailId, httpRequest.getRemoteAddr());
        return Result.success(null);
    }

    @PostMapping("/{mailId}/snooze")
    public Result<MailActionResultVo> snoozeUntil(
            @PathVariable Long mailId,
            @Valid @RequestBody SnoozeUntilRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(mailService.snoozeUntil(
                SecurityUtils.currentUserId(),
                mailId,
                request.untilAt(),
                httpRequest.getRemoteAddr()
        ));
    }

    @PostMapping("/trash/restore-all")
    public Result<MailActionResultVo> restoreAllTrash(HttpServletRequest httpRequest) {
        return Result.success(mailService.restoreAllTrash(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/trash/empty")
    public Result<MailActionResultVo> emptyTrash(HttpServletRequest httpRequest) {
        return Result.success(mailService.emptyTrash(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/spam/restore-all")
    public Result<MailActionResultVo> restoreAllSpam(HttpServletRequest httpRequest) {
        return Result.success(mailService.restoreAllSpam(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/spam/empty")
    public Result<MailActionResultVo> emptySpam(HttpServletRequest httpRequest) {
        return Result.success(mailService.emptySpam(SecurityUtils.currentUserId(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/{mailId}/actions")
    public Result<MailActionResultVo> action(
            @PathVariable Long mailId,
            @Valid @RequestBody MailActionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(mailService.applyAction(SecurityUtils.currentUserId(), mailId, request.action(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/actions/batch")
    public Result<MailActionResultVo> batchAction(
            @Valid @RequestBody BatchMailActionRequest request,
            HttpServletRequest httpRequest
    ) {
        return Result.success(mailService.applyBatchAction(SecurityUtils.currentUserId(), request, httpRequest.getRemoteAddr()));
    }

    @PutMapping("/{mailId}/labels")
    public Result<Void> labels(
            @PathVariable Long mailId,
            @Valid @RequestBody UpdateMailLabelsRequest request,
            HttpServletRequest httpRequest
    ) {
        mailService.updateLabels(SecurityUtils.currentUserId(), mailId, request.labels(), httpRequest.getRemoteAddr());
        return Result.success(null);
    }
}
