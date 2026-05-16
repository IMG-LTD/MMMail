import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 docs CRDT client exposes snapshot and awareness APIs', async () => {
  const [packageJson, service, types] = await Promise.all([
    read('package.json'),
    read('src/service/api/docs.ts'),
    read('src/typings/api/expanded.d.ts')
  ]);

  for (const dependency of [
    '@tiptap/vue-3',
    '@tiptap/starter-kit',
    '@tiptap/extension-collaboration',
    'yjs',
    'y-prosemirror',
    'y-protocols'
  ]) {
    assert.match(packageJson, new RegExp(`"${dependency}"`));
  }
  assert.match(service, /getCollabSnapshot/);
  assert.match(service, /writeCollabSnapshot/);
  assert.match(service, /getCollabAwareness/);
  assert.match(service, /\/api\/v1\/collab\/\$\{resourceType\}\/\$\{resourceId\}\/snapshot/);
  assert.match(service, /\/api\/v1\/collab\/\$\{resourceType\}\/\$\{resourceId\}\/awareness/);
  assert.match(types, /interface CollabSnapshot/);
  assert.match(types, /interface CollabAwareness/);
});

test('v2.1.2 docs editor binds Yjs updates to the collaboration websocket', async () => {
  const [hook, docsPage] = await Promise.all([
    read('src/hooks/business/docs-collab-crdt.ts'),
    read('src/views/docs/index.vue')
  ]);

  assert.match(hook, /import \* as Y from 'yjs'/);
  assert.match(hook, /y-protocols\/awareness/);
  assert.match(hook, /y-prosemirror/);
  assert.match(hook, /binaryType = 'arraybuffer'/);
  assert.match(hook, /Y\.applyUpdate/);
  assert.match(hook, /Y\.encodeStateAsUpdate/);
  assert.match(hook, /writeCollabSnapshot/);
  assert.match(hook, /\/ws\/collab\/\$\{resourceType\}\/\$\{resourceId\}/);
  assert.match(docsPage, /EditorContent/);
  assert.match(docsPage, /CollaborationExtension\.configure/);
  assert.match(docsPage, /createDocsCollabSession/);
  assert.doesNotMatch(docsPage, /type="textarea" :autosize="\{ minRows: 14 \}"/);
});
