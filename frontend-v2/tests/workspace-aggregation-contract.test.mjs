import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const files = [
  new URL('../src/views/app/DocsWorkspaceView.vue', import.meta.url),
  new URL('../src/views/app/DocsEditorView.vue', import.meta.url),
  new URL('../src/views/app/SheetsWorkspaceView.vue', import.meta.url),
  new URL('../src/views/app/SheetsEditorView.vue', import.meta.url),
  new URL('../src/views/app/CollaborationView.vue', import.meta.url),
  new URL('../src/views/app/NotificationsView.vue', import.meta.url)
]

test('workspace and aggregation surfaces consume shared contracts', async () => {
  const contents = await Promise.all(files.map(file => readFile(file, 'utf8')))

  assert.match(contents[0], /useCopilotPanel/)
  assert.match(contents[1], /useCopilotPanel/)
  assert.match(contents[2], /useCopilotPanel/)
  assert.match(contents[3], /useCopilotPanel/)
  assert.match(contents[4], /\/api\/v2\/workspace\/aggregation/)
  assert.match(contents[5], /\/api\/v2\/workspace\/aggregation/)
})
