package com.mmmail.server.controller;

import com.mmmail.common.model.Result;
import com.mmmail.server.model.vo.NotificationRealtimeReplayVo;
import com.mmmail.server.service.NotificationRealtimeFacadeService;
import com.mmmail.server.util.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/notifications")
public class NotificationRealtimeController {

    private final NotificationRealtimeFacadeService notificationRealtimeFacadeService;

    public NotificationRealtimeController(NotificationRealtimeFacadeService notificationRealtimeFacadeService) {
        this.notificationRealtimeFacadeService = notificationRealtimeFacadeService;
    }

    @GetMapping("/since")
    public Result<NotificationRealtimeReplayVo> since(
            @RequestParam(name = "cursor", defaultValue = "0") Long cursor,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(notificationRealtimeFacadeService.replay(SecurityUtils.currentUserId(), cursor, limit));
    }
}
