package com.mmmail.server.service;

import com.mmmail.server.model.vo.NotificationRealtimeReplayVo;
import com.mmmail.server.model.vo.SuiteNotificationSyncVo;
import org.springframework.stereotype.Service;

@Service
public class NotificationRealtimeFacadeService {

    private final SuiteNotificationSyncService suiteNotificationSyncService;
    private final NotificationRealtimeService notificationRealtimeService;

    public NotificationRealtimeFacadeService(
            SuiteNotificationSyncService suiteNotificationSyncService,
            NotificationRealtimeService notificationRealtimeService
    ) {
        this.suiteNotificationSyncService = suiteNotificationSyncService;
        this.notificationRealtimeService = notificationRealtimeService;
    }

    public NotificationRealtimeReplayVo replay(Long userId, Long cursor, Integer limit) {
        SuiteNotificationSyncVo sync = suiteNotificationSyncService.getNotificationSync(userId, cursor, limit);
        return notificationRealtimeService.toReplay(userId, sync);
    }
}
