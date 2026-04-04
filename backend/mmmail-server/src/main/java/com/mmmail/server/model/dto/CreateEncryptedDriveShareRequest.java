package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

public class CreateEncryptedDriveShareRequest {

    private MultipartFile file;
    @Pattern(regexp = "VIEW|EDIT")
    private String permission;
    private LocalDateTime expiresAt;
    @Size(max = 128)
    private String password;
    @Size(max = 64)
    private String e2eeAlgorithm;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getE2eeAlgorithm() {
        return e2eeAlgorithm;
    }

    public void setE2eeAlgorithm(String e2eeAlgorithm) {
        this.e2eeAlgorithm = e2eeAlgorithm;
    }
}
