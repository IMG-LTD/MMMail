package com.mmmail.server.model.vo;

import java.time.LocalDateTime;

public record MailEasySwitchSessionVo(
        String id,
        String provider,
        String sourceEmail,
        boolean importContacts,
        boolean mergeContactDuplicates,
        boolean importCalendar,
        boolean importMail,
        String importedMailFolder,
        String status,
        int contactsCreated,
        int contactsUpdated,
        int contactsSkipped,
        int contactsInvalid,
        int calendarImported,
        int calendarInvalid,
        int mailImported,
        int mailSkipped,
        int mailInvalid,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
