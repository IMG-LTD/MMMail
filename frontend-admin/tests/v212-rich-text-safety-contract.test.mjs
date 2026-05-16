import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile, readdir } from 'node:fs/promises';

const root = new URL('../', import.meta.url);
const viewRoot = new URL('src/views/', root);

test('v2.1.2 pages do not render backend rich text with v-html', async () => {
  const vueFiles = await listVueFiles(viewRoot);
  const unsafeFiles = [];

  await Promise.all(
    vueFiles.map(async file => {
      const source = await readFile(file, 'utf8');
      if (source.includes('v-html')) {
        unsafeFiles.push(file.pathname.replace(root.pathname, ''));
      }
    })
  );

  assert.deepEqual(unsafeFiles.sort(), []);
});

async function listVueFiles(directoryUrl) {
  const entries = await readdir(directoryUrl, { withFileTypes: true });
  const nested = await Promise.all(
    entries.map(async entry => {
      const child = new URL(entry.name, ensureTrailingSlash(directoryUrl));
      if (entry.isDirectory()) {
        return listVueFiles(child);
      }
      return entry.name.endsWith('.vue') ? [child] : [];
    })
  );
  return nested.flat();
}

function ensureTrailingSlash(url) {
  const value = url.href.endsWith('/') ? url.href : `${url.href}/`;
  return new URL(value);
}
