import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const routesFile = new URL('../src/app/router/routes.ts', import.meta.url)

test('workspace and blank layouts remain frozen for M1', async () => {
  const content = await readFile(routesFile, 'utf8')

  assert.match(content, /const BaseLayoutMeta = \{ layout: 'base' as const \}/)
  assert.match(content, /const BlankLayoutMeta = \{ layout: 'blank' as const \}/)
  assert.match(content, /contentMode: 'flush' as const/)
  assert.match(content, /section: 'workspace'/)
})
