import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const routesFile = new URL('../src/app/router/routes.ts', import.meta.url)
const registryFile = new URL('../src/app/router/redirect-registry.ts', import.meta.url)

test('foundation route contract freezes canonical and same-shape compatibility evidence', async () => {
  const [routesContent, registryContent] = await Promise.all([
    readFile(routesFile, 'utf8'),
    readFile(registryFile, 'utf8')
  ])

  assert.match(routesContent, /path: '\/mail\/:id'/)
  assert.match(routesContent, /path: '\/public\/drive\/shares\/:token'/)
  assert.match(routesContent, /path: '\/folders\/:id'/)
  assert.doesNotMatch(routesContent, /\/folders\/:folderId/)
  assert.match(registryContent, /legacy: '\/folders\/:folderId', canonical: '\/folders\/:id'/)
  assert.match(routesContent, /const redirectRoutes: RouteRecordRaw\[] = redirectRegistry\.map/)
})
