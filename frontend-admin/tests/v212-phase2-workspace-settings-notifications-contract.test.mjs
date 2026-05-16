import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 phase 2 exposes workspace, settings, and notification service APIs', async () => {
  const [workspace, settings, notifications, apiIndex] = await Promise.all([
    read('src/service/api/workspace.ts'),
    read('src/service/api/settings.ts'),
    read('src/service/api/notifications.ts'),
    read('src/service/api/index.ts')
  ]);

  for (const endpoint of ['/api/v2/workspace/summary', '/api/v2/workspace/activity', '/api/v2/workspace/tasks']) {
    assert.match(workspace, new RegExp(endpoint.replaceAll('/', '\\/')));
  }

  for (const endpoint of [
    '/api/v2/settings/profile',
    '/api/v2/settings/security',
    '/api/v2/settings/devices',
    '/api/v2/settings/notifications'
  ]) {
    assert.match(settings, new RegExp(endpoint.replaceAll('/', '\\/')));
  }

  for (const endpoint of ['/api/v2/notifications', '/api/v2/notifications/', '/api/v2/notifications/subscriptions']) {
    assert.match(notifications, new RegExp(endpoint.replaceAll('/', '\\/')));
  }

  assert.match(apiIndex, /export \* from '\.\/workspace'/);
  assert.match(apiIndex, /export \* from '\.\/settings'/);
  assert.match(apiIndex, /export \* from '\.\/notifications'/);
});

test('v2.1.2 phase 2 home page binds workspace summary, activity, and tasks', async () => {
  const home = await read('src/views/home/index.vue');

  for (const marker of ['readWorkspaceSummary', 'listWorkspaceActivity', 'listWorkspaceTasks', 'patchWorkspaceTask']) {
    assert.match(home, new RegExp(marker));
  }

  assert.match(home, /NStatistic/);
  assert.match(home, /NTimeline/);
  assert.match(home, /NCheckbox/);
  assert.match(home, /onMounted/);
  assert.doesNotMatch(home, /Soybean/);
});

test('v2.1.2 phase 2 home chart modules use workspace APIs instead of mock data', async () => {
  const [lineChart, pieChart] = await Promise.all([
    read('src/views/home/modules/line-chart.vue'),
    read('src/views/home/modules/pie-chart.vue')
  ]);

  assert.match(lineChart, /listWorkspaceActivity/);
  assert.match(lineChart, /listWorkspaceTasks/);
  assert.match(pieChart, /readWorkspaceSummary/);

  for (const source of [lineChart, pieChart]) {
    assert.doesNotMatch(source, /mockData/);
    assert.doesNotMatch(source, /setTimeout/);
    assert.doesNotMatch(source, /downloadCount|registerCount|study|entertainment/);
  }
});

test('v2.1.2 phase 2 settings page supports profile, security, devices, and notification preferences', async () => {
  const settings = await read('src/views/settings/index.vue');

  for (const marker of [
    'readUserProfile',
    'updateUserProfile',
    'readSecuritySettings',
    'updateSecuritySettings',
    'listDeviceSessions',
    'deleteDeviceSession',
    'readNotificationSettings',
    'updateNotificationSettings'
  ]) {
    assert.match(settings, new RegExp(marker));
  }

  assert.match(settings, /NForm/);
  assert.match(settings, /NSwitch/);
  assert.match(settings, /NDataTable/);
  assert.doesNotMatch(settings, /NEmpty/);
});

test('v2.1.2 phase 2 notifications page supports list, read/archive patch, and subscriptions', async () => {
  const notifications = await read('src/views/notifications/index.vue');

  for (const marker of ['listNotifications', 'patchNotification', 'listNotificationSubscriptions']) {
    assert.match(notifications, new RegExp(marker));
  }

  assert.match(notifications, /NDataTable/);
  assert.match(notifications, /NBadge/);
  assert.match(notifications, /NTag/);
  assert.match(notifications, /onMounted/);
  assert.doesNotMatch(notifications, /NEmpty/);
});

test('v2.1.2 phase 2 adds workspace, settings, and notifications i18n namespaces', async () => {
  const [appTypes, zhCN, zhTW, enUS] = await Promise.all([
    read('src/typings/app.d.ts'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/zh-tw.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  assert.match(appTypes, /workspace: \{[\s\S]*activity/);
  assert.match(appTypes, /settings: \{[\s\S]*security/);
  assert.match(appTypes, /notifications: \{[\s\S]*subscriptions/);

  for (const source of [zhCN, zhTW, enUS]) {
    assert.match(source, /workspace:/);
    assert.match(source, /settings:/);
    assert.match(source, /notifications:/);
  }
});
