package com.mmmail.server.model.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class V21DriveFileUpdateRequest {

    private String name;
    private Long parentId;
    private boolean namePresent;
    private boolean parentIdPresent;

    @Size(max = 128)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.namePresent = true;
    }

    @Positive
    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
        this.parentIdPresent = true;
    }

    public boolean hasName() {
        return namePresent;
    }

    public boolean hasParentId() {
        return parentIdPresent;
    }

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object ignoredValue) {
        throw new BizException(ErrorCode.INVALID_ARGUMENT, "Unsupported drive file update field: " + fieldName);
    }
}
