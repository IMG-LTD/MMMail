import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const docsAndSheetsFiles = [
  new URL('../src/views/app/DocsWorkspaceView.vue', import.meta.url),
  new URL('../src/views/app/DocsEditorView.vue', import.meta.url),
  new URL('../src/views/app/SheetsWorkspaceView.vue', import.meta.url),
  new URL('../src/views/app/SheetsEditorView.vue', import.meta.url)
]

const aggregationFiles = [
  new URL('../src/views/app/NotificationsView.vue', import.meta.url)
]

const collaborationFile = new URL('../src/views/app/CollaborationView.vue', import.meta.url)

test('workspace aggregation and collaboration surfaces consume shared contracts', async () => {
  const docsAndSheetsContents = await Promise.all(docsAndSheetsFiles.map(file => readFile(file, 'utf8')))
  const aggregationContents = await Promise.all(aggregationFiles.map(file => readFile(file, 'utf8')))
  const collaborationContent = await readFile(collaborationFile, 'utf8')

  for (const content of docsAndSheetsContents) {
    assert.match(content, /useCopilotPanel/)
  }

  for (const content of aggregationContents) {
    assert.match(content, /\/api\/v2\/workspace\/aggregation/)
  }

  assert.match(collaborationContent, /listCollaborationProjects/)
  assert.match(collaborationContent, /listCollaborationTasks/)
  assert.match(collaborationContent, /listCollaborationActivity/)
  assert.doesNotMatch(collaborationContent, /\/api\/v2\/workspace\/aggregation/)
})

test('aggregation and collaboration surfaces stay scope-aware', async () => {
  const contents = await Promise.all(aggregationFiles.map(file => readFile(file, 'utf8')))
  const collaborationContent = await readFile(collaborationFile, 'utf8')

  for (const content of contents) {
    assert.match(content, /useScopeGuard/)
    assert.match(content, /scopeHeaders:\s*requestHeaders\.value/)
    assert.match(content, /watch\(\s*requestHeaders/)
  }

  assert.match(collaborationContent, /useScopeGuard/)
  assert.match(collaborationContent, /scopeHeaders = requestHeaders\.value/)
  assert.match(collaborationContent, /JSON\.stringify\(requestHeaders\.value\)/)
})
