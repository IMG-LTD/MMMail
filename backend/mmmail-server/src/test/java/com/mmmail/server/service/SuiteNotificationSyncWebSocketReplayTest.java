package com.mmmail.server.service;

import com.mmmail.server.model.vo.AuditEventVo;
import com.mmmail.server.model.vo.SuiteNotificationSyncVo;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuiteNotificationSyncWebSocketReplayTest {

    private static final Long USER_ID = 101L;
    private static final Long LAST_SEQ = 500L;
    private static final int SPEC_WS_REPLAY_LIMIT = 1000;
    private static final long SPEC_WS_REPLAY_WINDOW_SECONDS = 300L;

    @Test
    void websocketReplayShouldUseFiveMinuteWindowAndThousandEventLimit() {
        AuditEventVo event = auditEvent(501L);
        AuditService auditService = mock(AuditService.class);
        NotificationRealtimeService realtimeService = mock(NotificationRealtimeService.class);
        when(auditService.listRecentActorEvents(
                eq(USER_ID),
                anySet(),
                eq(LAST_SEQ),
                eq(SPEC_WS_REPLAY_LIMIT),
                any(LocalDateTime.class),
                eq(true)
        )).thenReturn(List.of(event));
        when(auditService.latestActorEvent(eq(USER_ID), anySet())).thenReturn(event);
        SuiteNotificationSyncService service = new SuiteNotificationSyncService(auditService, realtimeService);

        SuiteNotificationSyncVo replay = service.getNotificationWebSocketReplay(USER_ID, LAST_SEQ);

        assertThat(replay.items()).hasSize(1);
        assertThat(replay.items().getFirst().eventId()).isEqualTo(501L);
        var cutoff = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        verify(auditService).listRecentActorEvents(
                eq(USER_ID),
                anySet(),
                eq(LAST_SEQ),
                eq(SPEC_WS_REPLAY_LIMIT),
                cutoff.capture(),
                eq(true)
        );
        long ageSeconds = Duration.between(cutoff.getValue(), LocalDateTime.now()).toSeconds();
        assertThat(ageSeconds).isBetween(SPEC_WS_REPLAY_WINDOW_SECONDS - 2, SPEC_WS_REPLAY_WINDOW_SECONDS + 2);
    }

    private AuditEventVo auditEvent(Long eventId) {
        return new AuditEventVo(
                eventId,
                "SUITE_NOTIFICATION_MARK_READ",
                "notification",
                "NTF-1",
                "low",
                "127.0.0.1",
                "{\"operationId\":\"op_1\",\"requested\":1,\"affected\":1,\"sessionId\":\"s_1\"}",
                LocalDateTime.now()
        );
    }
}
