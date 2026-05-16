import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 mail service exposes folder and label drag actions', async () => {
  const [mailApi, mailTypes] = await Promise.all([read('src/service/api/mail.ts'), read('src/typings/api/mail.d.ts')]);

  for (const marker of ['listMailLabels', 'updateMailLabels', 'moveMailMessagesToFolder']) {
    assert.match(mailApi, new RegExp(marker));
  }

  assert.match(mailApi, /\/api\/v1\/labels/);
  assert.match(mailApi, /\/api\/v1\/mails\/\$\{messageId\}\/labels/);
  assert.match(mailApi, /MOVE_ARCHIVE/);
  assert.match(mailApi, /MOVE_TRASH/);
  assert.match(mailApi, /MOVE_INBOX/);
  assert.match(mailApi, /method: 'put'/);

  assert.match(mailTypes, /interface Label/);
  assert.match(mailTypes, /labels: string\[\]/);
  assert.match(mailTypes, /folderType: string/);
});

test('v2.1.2 mail page supports multi-select drag to folders and labels', async () => {
  const mailPage = await read('src/views/mail/index.vue');

  for (const marker of [
    'VueDraggable',
    'listMailLabels',
    'moveMailMessagesToFolder',
    'updateMailLabels',
    'handleFolderDrop',
    'handleLabelDrop',
    'resolveDraggedMessageIds'
  ]) {
    assert.match(mailPage, new RegExp(marker));
  }

  assert.match(mailPage, /group="mail-drag"/);
  assert.match(mailPage, /v-model:checked-row-keys="selectedRowKeys"/);
  assert.match(mailPage, /selectedRowKeys\.value/);
  assert.match(mailPage, /folderDropBuckets/);
  assert.match(mailPage, /labelDropBuckets/);
});
