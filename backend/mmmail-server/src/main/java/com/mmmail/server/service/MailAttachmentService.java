package com.mmmail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import com.mmmail.server.mapper.MailAttachmentMapper;
import com.mmmail.server.mapper.MailMessageMapper;
import com.mmmail.server.model.dto.UploadDraftAttachmentRequest;
import com.mmmail.server.model.entity.MailAttachment;
import com.mmmail.server.model.entity.MailMessage;
import com.mmmail.server.model.vo.MailAttachmentDownloadVo;
import com.mmmail.server.model.vo.MailAttachmentE2eeVo;
import com.mmmail.server.model.vo.MailAttachmentUploadVo;
import com.mmmail.server.model.vo.MailAttachmentVo;
import com.mmmail.server.model.vo.MailDeliveryTarget;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class MailAttachmentService {

    private static final long MAX_ATTACHMENT_BYTES = 20L * 1024L * 1024L;
    private static final int E2EE_DISABLED_FLAG = 0;
    private static final int E2EE_ENABLED_FLAG = 1;
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "apk", "app", "bat", "cmd", "com", "cpl", "dll", "exe", "hta",
            "jar", "js", "jse", "lnk", "msi", "msix", "pif", "ps1", "reg",
            "scr", "sh", "vb", "vbe", "vbs", "ws", "wsc", "wsf", "wsh"
    );

    private final MailAttachmentMapper mailAttachmentMapper;
    private final MailMessageMapper mailMessageMapper;
    private final MailE2eeMessageService mailE2eeMessageService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final Path storageRoot;

    public MailAttachmentService(
            MailAttachmentMapper mailAttachmentMapper,
            MailMessageMapper mailMessageMapper,
            MailE2eeMessageService mailE2eeMessageService,
            AuditService auditService,
            ObjectMapper objectMapper,
            @Value("${mmmail.mail.attachment.storage-root:${java.io.tmpdir}/mmmail-mail-attachments}") String storageRoot
    ) {
        this.mailAttachmentMapper = mailAttachmentMapper;
        this.mailMessageMapper = mailMessageMapper;
        this.mailE2eeMessageService = mailE2eeMessageService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.storageRoot = Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    @Transactional
    public MailAttachmentUploadVo uploadDraftAttachment(
            Long userId,
            Long draftId,
            UploadDraftAttachmentRequest request,
            String ipAddress
    ) {
        MailMessage draft = requireDraft(userId, draftId);
        AttachmentE2eeMetadata e2eeMetadata = resolveAttachmentE2eeMetadata(userId, request);
        UploadPayload payload = prepareUpload(request, e2eeMetadata.enabled());
        MailAttachment attachment = buildAttachmentEntity(userId, draft.getId(), payload, e2eeMetadata);
        mailAttachmentMapper.insert(attachment);
        auditService.record(userId, "MAIL_ATTACHMENT_UPLOAD", "draft=" + draftId + ",attachment=" + attachment.getId(), ipAddress);
        return new MailAttachmentUploadVo(String.valueOf(draft.getId()), toVo(attachment));
    }

    @Transactional
    public void deleteDraftAttachment(Long userId, Long draftId, Long attachmentId, String ipAddress) {
        requireDraft(userId, draftId);
        MailAttachment attachment = requireOwnedAttachment(userId, draftId, attachmentId);
        deleteStoredFileIfOrphaned(attachment);
        mailAttachmentMapper.deleteById(attachment.getId());
        auditService.record(userId, "MAIL_ATTACHMENT_DELETE", "draft=" + draftId + ",attachment=" + attachmentId, ipAddress);
    }

    public MailAttachmentDownloadVo downloadAttachment(Long userId, Long mailId, Long attachmentId, String ipAddress) {
        requireOwnedMail(userId, mailId);
        MailAttachment attachment = requireOwnedAttachment(userId, mailId, attachmentId);
        byte[] bytes = readAttachmentContent(attachment);
        auditService.record(userId, "MAIL_ATTACHMENT_DOWNLOAD", "mail=" + mailId + ",attachment=" + attachmentId, ipAddress);
        return new MailAttachmentDownloadVo(attachment.getFileName(), attachment.getContentType(), bytes);
    }

    public List<MailAttachmentVo> listForMail(Long userId, Long mailId) {
        requireOwnedMail(userId, mailId);
        return listRawAttachments(mailId).stream().map(this::toVo).toList();
    }

    public boolean hasAttachments(Long mailId) {
        return mailAttachmentMapper.selectCount(new LambdaQueryWrapper<MailAttachment>()
                .eq(MailAttachment::getMailId, mailId)) > 0;
    }

    @Transactional
    public void replicateToRecipients(Long outboundMailId, List<MailDeliveryTarget> deliveryTargets, List<Long> inboxMailIds, LocalDateTime now) {
        List<MailAttachment> attachments = listRawAttachments(outboundMailId);
        if (attachments.isEmpty()) {
            return;
        }
        for (int index = 0; index < deliveryTargets.size() && index < inboxMailIds.size(); index++) {
            MailDeliveryTarget target = deliveryTargets.get(index);
            if (!target.isInternalMailbox()) {
                continue;
            }
            copyAttachmentRows(attachments, target.ownerId(), inboxMailIds.get(index), now);
        }
    }

    private MailAttachment buildAttachmentEntity(
            Long ownerId,
            Long mailId,
            UploadPayload payload,
            AttachmentE2eeMetadata e2eeMetadata
    ) {
        LocalDateTime now = LocalDateTime.now();
        MailAttachment attachment = new MailAttachment();
        attachment.setOwnerId(ownerId);
        attachment.setMailId(mailId);
        attachment.setFileName(payload.fileName());
        attachment.setContentType(payload.contentType());
        attachment.setFileSize(payload.fileSize());
        attachment.setStoragePath(payload.storagePath());
        attachment.setE2eeEnabled(e2eeMetadata.flag());
        attachment.setE2eeAlgorithm(e2eeMetadata.algorithm());
        attachment.setE2eeFingerprintsJson(e2eeMetadata.fingerprintsJson());
        attachment.setCreatedAt(now);
        attachment.setUpdatedAt(now);
        attachment.setDeleted(0);
        return attachment;
    }

    private AttachmentE2eeMetadata resolveAttachmentE2eeMetadata(Long userId, UploadDraftAttachmentRequest request) {
        if (!Boolean.TRUE.equals(request.getE2eeEnabled())) {
            return AttachmentE2eeMetadata.disabled();
        }
        String algorithm = requireText(request.getE2eeAlgorithm(), "Mail attachment E2EE algorithm is required");
        String fingerprintsJson = mailE2eeMessageService.resolveDraftAttachmentFingerprintsJson(
                userId,
                request.getE2eeRecipientFingerprintsJson()
        );
        return AttachmentE2eeMetadata.enabled(algorithm, fingerprintsJson);
    }

    private UploadPayload prepareUpload(UploadDraftAttachmentRequest request, boolean e2eeEnabled) {
        MultipartFile file = requireMultipartFile(request.getFile());
        AttachmentDisplayMetadata metadata = resolveDisplayMetadata(request, file, e2eeEnabled);
        validateFileName(metadata.fileName());
        validateStoredBlobSize(file);
        String storagePath = storeBlob(file, metadata.fileName());
        return new UploadPayload(metadata.fileName(), metadata.contentType(), metadata.fileSize(), storagePath);
    }

    private MultipartFile requireMultipartFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Attachment file is required");
        }
        return file;
    }

    private AttachmentDisplayMetadata resolveDisplayMetadata(
            UploadDraftAttachmentRequest request,
            MultipartFile file,
            boolean e2eeEnabled
    ) {
        if (!e2eeEnabled) {
            String fileName = normalizeFileName(file.getOriginalFilename());
            String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : DEFAULT_CONTENT_TYPE;
            return new AttachmentDisplayMetadata(fileName, contentType, file.getSize());
        }
        String fileName = normalizeFileName(requireText(request.getFileName(), "Mail attachment fileName is required"));
        String contentType = requireText(request.getContentType(), "Mail attachment contentType is required");
        Long fileSize = request.getFileSize();
        if (fileSize == null || fileSize < 0) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Mail attachment fileSize must be >= 0");
        }
        return new AttachmentDisplayMetadata(fileName, contentType, fileSize);
    }

    private void validateStoredBlobSize(MultipartFile file) {
        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Attachment exceeds 20MB limit");
        }
    }

    private String storeBlob(MultipartFile file, String fileName) {
        Path targetPath = createStoragePath(fileName);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to store attachment");
        }
        return storageRoot.relativize(targetPath).toString();
    }

    private void copyAttachmentRows(List<MailAttachment> source, Long ownerId, Long mailId, LocalDateTime now) {
        for (MailAttachment item : source) {
            MailAttachment copy = new MailAttachment();
            copy.setOwnerId(ownerId);
            copy.setMailId(mailId);
            copy.setFileName(item.getFileName());
            copy.setContentType(item.getContentType());
            copy.setFileSize(item.getFileSize());
            copy.setStoragePath(item.getStoragePath());
            copy.setE2eeEnabled(item.getE2eeEnabled());
            copy.setE2eeAlgorithm(item.getE2eeAlgorithm());
            copy.setE2eeFingerprintsJson(item.getE2eeFingerprintsJson());
            copy.setCreatedAt(now);
            copy.setUpdatedAt(now);
            copy.setDeleted(0);
            mailAttachmentMapper.insert(copy);
        }
    }

    private Path createStoragePath(String fileName) {
        String extension = resolveExtension(fileName);
        String targetName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        return storageRoot.resolve("mail").resolve(targetName).normalize();
    }

    private void validateFileName(String fileName) {
        String extension = resolveExtension(fileName);
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Attachment type is not allowed");
        }
    }

    private String normalizeFileName(String originalName) {
        String candidate = StringUtils.hasText(originalName) ? Paths.get(originalName).getFileName().toString().trim() : "";
        return StringUtils.hasText(candidate) ? candidate : "attachment.bin";
    }

    private String resolveExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private byte[] readAttachmentContent(MailAttachment attachment) {
        Path targetPath = resolveStoragePath(attachment.getStoragePath());
        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Attachment file is missing");
        }
    }

    private void deleteStoredFileIfOrphaned(MailAttachment attachment) {
        long activeCount = mailAttachmentMapper.selectCount(new LambdaQueryWrapper<MailAttachment>()
                .eq(MailAttachment::getStoragePath, attachment.getStoragePath())
                .ne(MailAttachment::getId, attachment.getId()));
        if (activeCount > 0) {
            return;
        }
        Path targetPath = resolveStoragePath(attachment.getStoragePath());
        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Failed to delete attachment file");
        }
    }

    private Path resolveStoragePath(String storagePath) {
        Path targetPath = storageRoot.resolve(storagePath).normalize();
        if (!targetPath.startsWith(storageRoot)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, "Attachment path is invalid");
        }
        return targetPath;
    }

    private MailMessage requireDraft(Long userId, Long draftId) {
        MailMessage draft = mailMessageMapper.selectOne(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getId, draftId)
                .eq(MailMessage::getOwnerId, userId)
                .eq(MailMessage::getFolderType, "DRAFTS")
                .eq(MailMessage::getIsDraft, 1));
        if (draft == null) {
            throw new BizException(ErrorCode.MAIL_NOT_FOUND, "Draft mail not found");
        }
        return draft;
    }

    private MailMessage requireOwnedMail(Long userId, Long mailId) {
        MailMessage mail = mailMessageMapper.selectOne(new LambdaQueryWrapper<MailMessage>()
                .eq(MailMessage::getId, mailId)
                .eq(MailMessage::getOwnerId, userId));
        if (mail == null) {
            throw new BizException(ErrorCode.MAIL_NOT_FOUND);
        }
        return mail;
    }

    private MailAttachment requireOwnedAttachment(Long userId, Long mailId, Long attachmentId) {
        MailAttachment attachment = mailAttachmentMapper.selectOne(new LambdaQueryWrapper<MailAttachment>()
                .eq(MailAttachment::getId, attachmentId)
                .eq(MailAttachment::getOwnerId, userId)
                .eq(MailAttachment::getMailId, mailId));
        if (attachment == null) {
            throw new BizException(ErrorCode.MAIL_NOT_FOUND, "Mail attachment not found");
        }
        return attachment;
    }

    private List<MailAttachment> listRawAttachments(Long mailId) {
        return mailAttachmentMapper.selectList(new LambdaQueryWrapper<MailAttachment>()
                .eq(MailAttachment::getMailId, mailId)
                .orderByAsc(MailAttachment::getCreatedAt));
    }

    private MailAttachmentVo toVo(MailAttachment attachment) {
        return new MailAttachmentVo(
                String.valueOf(attachment.getId()),
                String.valueOf(attachment.getMailId()),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getFileSize() == null ? 0 : attachment.getFileSize(),
                toE2eeVo(attachment)
        );
    }

    private MailAttachmentE2eeVo toE2eeVo(MailAttachment attachment) {
        if (attachment.getE2eeEnabled() == null || attachment.getE2eeEnabled() != E2EE_ENABLED_FLAG) {
            return null;
        }
        return new MailAttachmentE2eeVo(
                true,
                attachment.getE2eeAlgorithm(),
                parseFingerprints(attachment.getE2eeFingerprintsJson())
        );
    }

    private List<String> parseFingerprints(String fingerprintsJson) {
        if (!StringUtils.hasText(fingerprintsJson)) {
            throw new IllegalStateException("Encrypted attachment is missing Mail E2EE fingerprint metadata");
        }
        try {
            List<String> fingerprints = objectMapper.readValue(fingerprintsJson, STRING_LIST_TYPE);
            if (fingerprints == null || fingerprints.isEmpty()) {
                throw new IllegalStateException("Encrypted attachment is missing Mail E2EE fingerprint metadata");
            }
            return fingerprints;
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse Mail E2EE attachment fingerprint metadata", exception);
        }
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, message);
        }
        return value.trim();
    }

    private record UploadPayload(String fileName, String contentType, long fileSize, String storagePath) {
    }

    private record AttachmentDisplayMetadata(String fileName, String contentType, long fileSize) {
    }

    private record AttachmentE2eeMetadata(int flag, String algorithm, String fingerprintsJson) {

        private static AttachmentE2eeMetadata disabled() {
            return new AttachmentE2eeMetadata(E2EE_DISABLED_FLAG, null, null);
        }

        private static AttachmentE2eeMetadata enabled(String algorithm, String fingerprintsJson) {
            return new AttachmentE2eeMetadata(E2EE_ENABLED_FLAG, algorithm, fingerprintsJson);
        }

        private boolean enabled() {
            return flag == E2EE_ENABLED_FLAG;
        }
    }
}
