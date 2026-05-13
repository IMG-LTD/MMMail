package com.mmmail.server.model.dto;

import com.mmmail.common.exception.BizException;
import com.mmmail.common.exception.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record V21MailBulkActionRequest(
        @NotEmpty List<String> messageIds,
        @NotBlank String action
) {
    private static final String INVALID_MESSAGE_ID = "Mail message id is invalid";

    public BatchMailActionRequest toBatchRequest() {
        List<Long> mailIds = messageIds.stream()
                .map(V21MailBulkActionRequest::parseMessageId)
                .toList();
        return new BatchMailActionRequest(mailIds, action);
    }

    private static Long parseMessageId(String messageId) {
        try {
            return Long.parseLong(messageId);
        } catch (NumberFormatException | NullPointerException exception) {
            throw new BizException(ErrorCode.INVALID_ARGUMENT, INVALID_MESSAGE_ID);
        }
    }
}
