package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class UploadDriveFileRequest {

    private MultipartFile file;
    private Long parentId;
    @Size(max = 255)
    private String fileName;
    @Size(max = 255)
    private String contentType;
    @Min(0)
    private Long fileSize;
    private Boolean e2eeEnabled;
    @Size(max = 64)
    private String e2eeAlgorithm;
    @Size(max = 65535)
    private String e2eeRecipientFingerprintsJson;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Boolean getE2eeEnabled() {
        return e2eeEnabled;
    }

    public void setE2eeEnabled(Boolean e2eeEnabled) {
        this.e2eeEnabled = e2eeEnabled;
    }

    public String getE2eeAlgorithm() {
        return e2eeAlgorithm;
    }

    public void setE2eeAlgorithm(String e2eeAlgorithm) {
        this.e2eeAlgorithm = e2eeAlgorithm;
    }

    public String getE2eeRecipientFingerprintsJson() {
        return e2eeRecipientFingerprintsJson;
    }

    public void setE2eeRecipientFingerprintsJson(String e2eeRecipientFingerprintsJson) {
        this.e2eeRecipientFingerprintsJson = e2eeRecipientFingerprintsJson;
    }
}
