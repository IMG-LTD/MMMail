package com.mmmail.server.model.vo;

import java.util.Map;

public record MailboxStatsVo(
        Map<String, Long> folderCounts,
        long unreadCount,
        long starredCount
) {
}
