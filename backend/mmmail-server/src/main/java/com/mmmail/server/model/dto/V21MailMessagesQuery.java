package com.mmmail.server.model.dto;

import java.util.Locale;

public record V21MailMessagesQuery(
        String folder,
        Integer page,
        Integer size,
        String keyword,
        Boolean unread,
        Boolean needsReply,
        Boolean starred,
        Boolean hasAttachments,
        Boolean importantContact,
        String from,
        String to,
        String label
) {
    private static final String DEFAULT_FOLDER = "inbox";
    private static final String EMPTY_VALUE = "";

    public String normalizedFolder() {
        if (folder == null || folder.isBlank()) {
            return DEFAULT_FOLDER;
        }
        return folder.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizedKeyword() {
        if (keyword == null || keyword.isBlank()) {
            return EMPTY_VALUE;
        }
        return keyword.trim();
    }
}
