package com.mmmail.server.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("sheets_workbook_version")
public class SheetsWorkbookVersion {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long workbookId;
    private Integer versionNo;
    private String title;
    private Integer rowCount;
    private Integer colCount;
    private String gridJson;
    private String sheetsJson;
    private String activeSheetId;
    private Long createdByUserId;
    private String sourceEvent;
    private LocalDateTime createdAt;
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkbookId() {
        return workbookId;
    }

    public void setWorkbookId(Long workbookId) {
        this.workbookId = workbookId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public Integer getColCount() {
        return colCount;
    }

    public void setColCount(Integer colCount) {
        this.colCount = colCount;
    }

    public String getGridJson() {
        return gridJson;
    }

    public void setGridJson(String gridJson) {
        this.gridJson = gridJson;
    }

    public String getSheetsJson() {
        return sheetsJson;
    }

    public void setSheetsJson(String sheetsJson) {
        this.sheetsJson = sheetsJson;
    }

    public String getActiveSheetId() {
        return activeSheetId;
    }

    public void setActiveSheetId(String activeSheetId) {
        this.activeSheetId = activeSheetId;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getSourceEvent() {
        return sourceEvent;
    }

    public void setSourceEvent(String sourceEvent) {
        this.sourceEvent = sourceEvent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
