import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const routesFile = new URL('../src/app/router/routes.ts', import.meta.url)

test('foundation routes include all canonical compatibility entries', async () => {
  const content = await readFile(routesFile, 'utf8')

  assert.match(content, /path: '\/mail\/:id'/)
  assert.match(content, /path: '\/public\/drive\/shares\/:token'/)
  assert.match(content, /path: '\/folders\/:folderId'/)
  assert.match(content, /path: '\/share\/pass\/:token'/)
})
