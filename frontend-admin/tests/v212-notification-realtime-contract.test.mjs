import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';

async function read(path) {
  return readFile(new URL(`../${path}`, import.meta.url), 'utf8');
}

test('v2.1.2 notifications API exposes realtime replay contract', async () => {
  const [api, types] = await Promise.all([
    read('src/service/api/notifications.ts'),
    read('src/typings/api/notifications.d.ts')
  ]);

  assert.match(api, /listNotificationEventsSince/);
  assert.match(api, /\/api\/v2\/notifications\/since/);
  assert.match(types, /interface RealtimeEvent/);
  assert.match(types, /type RealtimeEventType = 'notification' \| 'badge-update' \| 'subscription-changed'/);
  assert.match(types, /interface RealtimeReplay/);
  assert.match(types, /nextCursor: number/);
});

test('v2.1.2 notification realtime client uses websocket heartbeat and cursor replay', async () => {
  const realtime = await read('src/hooks/business/notification-realtime.ts');

  assert.match(realtime, /new WebSocket/);
  assert.match(realtime, /\/ws\/notifications/);
  assert.match(realtime, /searchParams\.set\('token'/);
  assert.match(realtime, /searchParams\.set\('since'/);
  assert.match(realtime, /HEARTBEAT_INTERVAL_MS/);
  assert.match(realtime, /type: 'ping'/);
  assert.match(realtime, /listNotificationEventsSince/);
});

test('v2.1.2 notifications page subscribes to realtime events and cleans up', async () => {
  const page = await read('src/views/notifications/index.vue');

  assert.match(page, /connectNotificationRealtime/);
  assert.match(page, /onBeforeUnmount/);
  assert.match(page, /applyRealtimeEvent/);
  assert.match(page, /stopNotificationRealtime/);
});
