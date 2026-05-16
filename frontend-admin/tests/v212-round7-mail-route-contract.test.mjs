import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('round7 mail exposes explicit folder routes instead of falling through to 404', async () => {
  const router = await read('src/router/routes/index.ts');

  assert.match(router, /mail_folder/);
  assert.match(router, /\/mail\/:folder/);
  assert.match(router, /layout\.base\$view\.mail/);
  assert.match(router, /route\.mail/);
});

test('round7 mail page initializes the active folder from route params', async () => {
  const mailPage = await read('src/views/mail/index.vue');

  assert.match(mailPage, /useRoute/);
  assert.match(mailPage, /route\.params\.folder/);
  assert.match(mailPage, /resolveRouteFolder/);
  assert.match(mailPage, /activeFolder\.value = routeFolder/);
});
