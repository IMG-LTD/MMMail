import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 drive version routes expose timeline and compare entry points', async () => {
  const routes = await read('src/router/routes/custom-routes.ts');

  assert.match(routes, /name: 'drive_file_versions'/);
  assert.match(routes, /path: '\/drive\/files\/:fileId\/versions'/);
  assert.match(routes, /name: 'drive_file_version_compare'/);
  assert.match(routes, /path: '\/drive\/files\/:fileId\/versions\/:verA\/compare\/:verB'/);
  assert.match(routes, /component: 'layout\.base\$view\.drive'/);
});

test('v2.1.2 drive page renders version timeline, restore confirmation, and compare navigation', async () => {
  const page = await read('src/views/drive/index.vue');

  assert.match(page, /useRoute/);
  assert.match(page, /useRouter/);
  assert.match(page, /NTimeline/);
  assert.match(page, /NTimelineItem/);
  assert.match(page, /restoreDriveFileVersion\(fileId, version\.id\)/);
  assert.match(page, /window\.\$dialog\?\.warning/);
  assert.match(page, /openVersionCompare/);
  assert.match(page, /drive_file_version_compare/);
  assert.match(page, /route\.params\.fileId/);
});
