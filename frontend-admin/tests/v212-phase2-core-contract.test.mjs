import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 phase 2 exposes core module service APIs against existing v2 backend contracts', async () => {
  const [mail, calendar, drive, apiIndex] = await Promise.all([
    read('src/service/api/mail.ts'),
    read('src/service/api/calendar.ts'),
    read('src/service/api/drive.ts'),
    read('src/service/api/index.ts')
  ]);

  for (const endpoint of [
    '/api/v2/mail/folders',
    '/api/v2/mail/messages',
    '/api/v2/mail/threads/',
    '/api/v2/mail/send',
    '/api/v2/mail/drafts',
    '/api/v2/mail/messages/bulk-action'
  ]) {
    assert.match(mail, new RegExp(endpoint.replaceAll('/', '\\/')));
  }

  for (const endpoint of ['/api/v2/calendar/events', '/api/v2/calendar/availability', '/api/v2/calendar/settings']) {
    assert.match(calendar, new RegExp(endpoint.replaceAll('/', '\\/')));
  }

  for (const endpoint of [
    '/api/v2/drive/files',
    '/api/v2/drive/folders',
    '/api/v2/drive/uploads',
    '/api/v2/drive/storage/summary',
    '/api/v2/drive/files/'
  ]) {
    assert.match(drive, new RegExp(endpoint.replaceAll('/', '\\/')));
  }

  assert.match(apiIndex, /export \* from '\.\/mail'/);
  assert.match(apiIndex, /export \* from '\.\/calendar'/);
  assert.match(apiIndex, /export \* from '\.\/drive'/);
});

test('v2.1.2 phase 2 mail page supports folder navigation, list, reader, compose, and bulk actions', async () => {
  const mailPage = await read('src/views/mail/index.vue');

  for (const marker of [
    'listMailFolders',
    'listMailMessages',
    'readMailMessage',
    'sendMailMessage',
    'saveMailDraft',
    'bulkActionMailMessages'
  ]) {
    assert.match(mailPage, new RegExp(marker));
  }

  assert.match(mailPage, /NDataTable/);
  assert.match(mailPage, /NDrawer/);
  assert.match(mailPage, /NInput/);
  assert.match(mailPage, /selectedRowKeys/);
  assert.match(mailPage, /onMounted/);
  assert.doesNotMatch(mailPage, /NEmpty/);
});

test('v2.1.2 phase 2 calendar page supports event listing, create drawer, availability, and settings loading', async () => {
  const calendarPage = await read('src/views/calendar/index.vue');

  for (const marker of [
    'listCalendarEvents',
    'createCalendarEvent',
    'deleteCalendarEvent',
    'queryCalendarAvailability',
    'readCalendarSettings'
  ]) {
    assert.match(calendarPage, new RegExp(marker));
  }

  assert.match(calendarPage, /NDataTable/);
  assert.match(calendarPage, /NDrawer/);
  assert.match(calendarPage, /NDatePicker/);
  assert.match(calendarPage, /NAlert/);
  assert.match(calendarPage, /onMounted/);
  assert.doesNotMatch(calendarPage, /NEmpty/);
});

test('v2.1.2 phase 2 drive page supports file list, upload, folders, storage, share, and delete', async () => {
  const drivePage = await read('src/views/drive/index.vue');

  for (const marker of [
    'listDriveItems',
    'listDriveFolders',
    'createDriveUpload',
    'readDriveUsage',
    'createDriveShare',
    'deleteDriveFile'
  ]) {
    assert.match(drivePage, new RegExp(marker));
  }

  assert.match(drivePage, /NDataTable/);
  assert.match(drivePage, /NUploadDragger/);
  assert.match(drivePage, /NTree/);
  assert.match(drivePage, /NProgress/);
  assert.match(drivePage, /onMounted/);
  assert.doesNotMatch(drivePage, /NEmpty/);
});

test('v2.1.2 phase 2 adds core module i18n namespaces', async () => {
  const [appTypes, zhCN, zhTW, enUS] = await Promise.all([
    read('src/typings/app.d.ts'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/zh-tw.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  assert.match(appTypes, /mail: \{[\s\S]*compose/);
  assert.match(appTypes, /calendar: \{[\s\S]*availability/);
  assert.match(appTypes, /drive: \{[\s\S]*upload/);

  for (const source of [zhCN, zhTW, enUS]) {
    assert.match(source, /mail:/);
    assert.match(source, /calendar:/);
    assert.match(source, /drive:/);
  }
});
