import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 collaboration service binds board read and task move contracts', async () => {
  const [collaborationApi, collaborationTypes] = await Promise.all([
    read('src/service/api/collaboration.ts'),
    read('src/typings/api/expanded.d.ts')
  ]);

  for (const marker of ['readCollaborationBoard', 'moveCollaborationTask']) {
    assert.match(collaborationApi, new RegExp(marker));
  }

  assert.match(collaborationApi, /\/api\/v2\/collaboration\/projects\/\$\{projectId\}\/board/);
  assert.match(collaborationApi, /\/api\/v2\/collaboration\/tasks\/\$\{taskId\}\/move/);
  assert.match(collaborationApi, /method: 'patch'/);

  for (const typeName of ['Board', 'BoardColumn', 'TaskMovePayload', 'TaskMoveResult']) {
    assert.match(collaborationTypes, new RegExp(`interface ${typeName}`));
  }

  assert.match(collaborationTypes, /columnId: string/);
  assert.match(collaborationTypes, /position: string/);
});

test('v2.1.2 collaboration page renders draggable board and persists card moves', async () => {
  const collaborationPage = await read('src/views/collaboration/index.vue');

  for (const marker of ['readCollaborationBoard', 'moveCollaborationTask', 'VueDraggable', 'handleBoardMove']) {
    assert.match(collaborationPage, new RegExp(marker));
  }

  assert.match(collaborationPage, /v-model="column\.tasks"/);
  assert.match(collaborationPage, /@end="handleBoardMove\(column, \$event\)"/);
  assert.match(collaborationPage, /selectedProjectId/);
  assert.match(collaborationPage, /boardColumns/);
});
