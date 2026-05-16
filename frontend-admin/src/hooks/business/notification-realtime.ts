import { listNotificationEventsSince } from '@/service/api';
import { getToken } from '@/store/modules/auth/shared';

const HEARTBEAT_INTERVAL_MS = 30_000;
const RECONNECT_DELAY_MS = 1_000;
const NOTIFICATION_WS_PATH = '/ws/notifications';
const HTTPS_PROTOCOL = 'https:';
const WSS_PROTOCOL = 'wss:';
const WS_PROTOCOL = 'ws:';

type RealtimeFrame = Api.Notifications.RealtimeEvent | { type: 'pong' };

interface NotificationRealtimeOptions {
  initialCursor?: number;
  onEvent: (event: Api.Notifications.RealtimeEvent) => void;
  onError?: (error: Error) => void;
}

export function connectNotificationRealtime(options: NotificationRealtimeOptions) {
  let socket: WebSocket | null = null;
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null;
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  let stopped = false;
  let lastCursor = options.initialCursor ?? 0;

  function open() {
    const token = getToken();
    if (!token) {
      options.onError?.(new Error('notification realtime token is required'));
      return;
    }

    socket = new WebSocket(buildNotificationWebSocketUrl(token, lastCursor));
    socket.addEventListener('open', startHeartbeat);
    socket.addEventListener('message', handleMessage);
    socket.addEventListener('error', handleSocketError);
    socket.addEventListener('close', handleSocketClose);
  }

  function startHeartbeat() {
    stopHeartbeat();
    heartbeatTimer = setInterval(() => {
      socket?.send(JSON.stringify({ type: 'ping' }));
    }, HEARTBEAT_INTERVAL_MS);
  }

  function stopHeartbeat() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer);
      heartbeatTimer = null;
    }
  }

  function handleMessage(message: MessageEvent<string>) {
    try {
      const frame = JSON.parse(message.data) as RealtimeFrame;
      if (frame.type === 'pong') {
        return;
      }
      applyRealtimeCursor(frame);
      options.onEvent(frame);
    } catch (error) {
      options.onError?.(error instanceof Error ? error : new Error('notification realtime frame parse failed'));
    }
  }

  function handleSocketError() {
    options.onError?.(new Error('notification realtime websocket error'));
  }

  function handleSocketClose() {
    stopHeartbeat();
    if (!stopped) {
      void replayAndReconnect();
    }
  }

  async function replayAndReconnect() {
    await replaySinceCursor();
    scheduleReconnect();
  }

  async function replaySinceCursor() {
    const { data, error } = await listNotificationEventsSince({ cursor: lastCursor });
    if (error) {
      options.onError?.(new Error('notification realtime replay failed'));
      return;
    }
    data.events.forEach(event => {
      applyRealtimeCursor(event);
      options.onEvent(event);
    });
    lastCursor = Math.max(lastCursor, data.nextCursor);
  }

  function scheduleReconnect() {
    reconnectTimer = setTimeout(open, RECONNECT_DELAY_MS);
  }

  function stop() {
    stopped = true;
    stopHeartbeat();
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    socket?.close();
  }

  function applyRealtimeCursor(event: Api.Notifications.RealtimeEvent) {
    lastCursor = Math.max(lastCursor, event.seq);
  }

  open();

  return stop;
}

export function buildNotificationWebSocketUrl(token: string, cursor: number) {
  const baseUrl = new URL(import.meta.env.VITE_SERVICE_BASE_URL || window.location.origin, window.location.origin);
  baseUrl.protocol = baseUrl.protocol === HTTPS_PROTOCOL ? WSS_PROTOCOL : WS_PROTOCOL;
  baseUrl.pathname = NOTIFICATION_WS_PATH;
  baseUrl.search = '';
  baseUrl.searchParams.set('token', token);
  baseUrl.searchParams.set('since', String(cursor));
  return baseUrl.toString();
}
