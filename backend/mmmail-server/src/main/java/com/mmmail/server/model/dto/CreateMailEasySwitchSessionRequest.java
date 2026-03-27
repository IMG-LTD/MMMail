package com.mmmail.server.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateMailEasySwitchSessionRequest(
        @NotBlank String provider,
        @NotBlank @Email String sourceEmail,
        Boolean importContacts,
        Boolean mergeContactDuplicates,
        String contactsCsv,
        Boolean importCalendar,
        String calendarIcs,
        Boolean importMail,
        List<@NotBlank String> mailMessages,
        @Size(max = 16) String importedMailFolder
) {
}
