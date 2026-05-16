import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 calendar services expose RRULE create and scoped update contracts', async () => {
  const [service, types] = await Promise.all([
    read('src/service/api/calendar.ts'),
    read('src/typings/api/calendar.d.ts')
  ]);

  assert.match(service, /\/api\/v2\/calendar\/events/);
  assert.match(service, /scope/);
  for (const key of ['seriesId', 'rrule', 'occurrenceStartAt', 'recurrenceUntil']) {
    assert.match(types, new RegExp(`${key}:`));
  }
  for (const key of ['rrule\\?', 'rdate\\?', 'exdate\\?']) {
    assert.match(types, new RegExp(key));
  }
});

test('v2.1.2 calendar page binds RRULE input into create payload', async () => {
  const page = await read('src/views/calendar/index.vue');

  assert.match(page, /eventModel\.rrule/);
  assert.match(page, /page\.calendar\.rrule/);
  assert.match(page, /rrule: eventModel\.rrule/);
});

test('v2.1.2 calendar RRULE labels are translated', async () => {
  const [appTypes, zhCN, enUS] = await Promise.all([
    read('src/typings/app.d.ts'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  assert.match(appTypes, /calendar: \{[\s\S]*rrule: string/);
  assert.match(zhCN, /rrule: '重复规则'/);
  assert.match(enUS, /rrule: 'Repeat rule'/);
});

test('v2.1.2 calendar exposes ICS subscription and export contracts', async () => {
  const [service, types, page] = await Promise.all([
    read('src/service/api/calendar.ts'),
    read('src/typings/api/calendar.d.ts'),
    read('src/views/calendar/index.vue')
  ]);

  for (const endpoint of [
    '/api/v2/calendar/subscriptions',
    '/api/v2/calendar/subscriptions/${subscriptionId}/sync',
    '/api/v2/calendar/${calendarId}/ics'
  ]) {
    assert.match(service, new RegExp(endpoint.replaceAll('/', '\\/').replaceAll('$', '\\$')));
  }

  for (const marker of [
    'listCalendarSubscriptions',
    'createCalendarSubscription',
    'syncCalendarSubscription',
    'exportCalendarIcs'
  ]) {
    assert.match(service, new RegExp(marker));
  }

  assert.match(types, /interface Subscription/);
  assert.match(types, /interface SubscriptionSync/);

  for (const marker of ['calendarSubscriptions', 'subscriptionModel', 'syncCalendarSubscription']) {
    assert.match(page, new RegExp(marker));
  }
});
