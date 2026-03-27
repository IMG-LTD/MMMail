package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("mail_easy_switch_session")
public class MailEasySwitchSession {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long ownerId;
    private String provider;
    private String sourceEmail;
    private Integer importContacts;
    private Integer mergeContactDuplicates;
    private Integer importCalendar;
    private Integer importMail;
    private String importedMailFolder;
    private String status;
    private Integer contactsCreated;
    private Integer contactsUpdated;
    private Integer contactsSkipped;
    private Integer contactsInvalid;
    private Integer calendarImported;
    private Integer calendarInvalid;
    private Integer mailImported;
    private Integer mailSkipped;
    private Integer mailInvalid;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getSourceEmail() {
        return sourceEmail;
    }

    public void setSourceEmail(String sourceEmail) {
        this.sourceEmail = sourceEmail;
    }

    public Integer getImportContacts() {
        return importContacts;
    }

    public void setImportContacts(Integer importContacts) {
        this.importContacts = importContacts;
    }

    public Integer getMergeContactDuplicates() {
        return mergeContactDuplicates;
    }

    public void setMergeContactDuplicates(Integer mergeContactDuplicates) {
        this.mergeContactDuplicates = mergeContactDuplicates;
    }

    public Integer getImportCalendar() {
        return importCalendar;
    }

    public void setImportCalendar(Integer importCalendar) {
        this.importCalendar = importCalendar;
    }

    public Integer getImportMail() {
        return importMail;
    }

    public void setImportMail(Integer importMail) {
        this.importMail = importMail;
    }

    public String getImportedMailFolder() {
        return importedMailFolder;
    }

    public void setImportedMailFolder(String importedMailFolder) {
        this.importedMailFolder = importedMailFolder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getContactsCreated() {
        return contactsCreated;
    }

    public void setContactsCreated(Integer contactsCreated) {
        this.contactsCreated = contactsCreated;
    }

    public Integer getContactsUpdated() {
        return contactsUpdated;
    }

    public void setContactsUpdated(Integer contactsUpdated) {
        this.contactsUpdated = contactsUpdated;
    }

    public Integer getContactsSkipped() {
        return contactsSkipped;
    }

    public void setContactsSkipped(Integer contactsSkipped) {
        this.contactsSkipped = contactsSkipped;
    }

    public Integer getContactsInvalid() {
        return contactsInvalid;
    }

    public void setContactsInvalid(Integer contactsInvalid) {
        this.contactsInvalid = contactsInvalid;
    }

    public Integer getCalendarImported() {
        return calendarImported;
    }

    public void setCalendarImported(Integer calendarImported) {
        this.calendarImported = calendarImported;
    }

    public Integer getCalendarInvalid() {
        return calendarInvalid;
    }

    public void setCalendarInvalid(Integer calendarInvalid) {
        this.calendarInvalid = calendarInvalid;
    }

    public Integer getMailImported() {
        return mailImported;
    }

    public void setMailImported(Integer mailImported) {
        this.mailImported = mailImported;
    }

    public Integer getMailSkipped() {
        return mailSkipped;
    }

    public void setMailSkipped(Integer mailSkipped) {
        this.mailSkipped = mailSkipped;
    }

    public Integer getMailInvalid() {
        return mailInvalid;
    }

    public void setMailInvalid(Integer mailInvalid) {
        this.mailInvalid = mailInvalid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
