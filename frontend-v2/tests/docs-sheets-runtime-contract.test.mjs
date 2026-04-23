import test from 'node:test'
import assert from 'node:assert/strict'
import vm from 'node:vm'
import { createRequire } from 'node:module'
import { readFile } from 'node:fs/promises'

const require = createRequire(import.meta.url)
const ts = require('typescript')

const docsApi = new URL('../src/service/api/docs.ts', import.meta.url)
const sheetsApi = new URL('../src/service/api/sheets.ts', import.meta.url)
const docsWorkspace = new URL('../src/views/app/DocsWorkspaceView.vue', import.meta.url)
const docsEditor = new URL('../src/views/app/DocsEditorView.vue', import.meta.url)
const sheetsWorkspace = new URL('../src/views/app/SheetsWorkspaceView.vue', import.meta.url)
const sheetsEditor = new URL('../src/views/app/SheetsEditorView.vue', import.meta.url)
const routeBoundEditorState = new URL('../src/views/app/route-bound-editor-state.ts', import.meta.url)

async function loadTsModule(fileUrl) {
  const source = await readFile(fileUrl, 'utf8')
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022
    }
  })

  const module = { exports: {} }
  const sandbox = {
    exports: module.exports,
    module,
    require(specifier) {
      throw new Error(`Unexpected import: ${specifier}`)
    }
  }

  vm.runInNewContext(outputText, sandbox, { filename: fileUrl.pathname })
  return module.exports
}

test('docs and sheets surfaces read runtime workspaces instead of placeholder arrays', async () => {
  const [docsApiContent, sheetsApiContent, docsWorkspaceContent, docsEditorContent, sheetsWorkspaceContent, sheetsEditorContent, routeBoundEditorStateContent] = await Promise.all([
    readFile(docsApi, 'utf8'),
    readFile(sheetsApi, 'utf8'),
    readFile(docsWorkspace, 'utf8'),
    readFile(docsEditor, 'utf8'),
    readFile(sheetsWorkspace, 'utf8'),
    readFile(sheetsEditor, 'utf8'),
    readFile(routeBoundEditorState, 'utf8')
  ])

  assert.match(docsApiContent, /listDocsNotes\(token: string, keyword = ''\)/)
  assert.match(docsApiContent, /readDocsNote\(noteId: string, token: string\)/)
  assert.match(docsApiContent, /updateDocsNote\(noteId: string, body: Record<string, unknown>, token: string\)/)
  assert.match(docsApiContent, /\/api\/v1\/docs\/notes/)
  assert.match(docsApiContent, /\/api\/v1\/docs\/notes\/\$\{noteId\}/)

  assert.match(sheetsApiContent, /listSheetsWorkbooks\(token: string\)/)
  assert.match(sheetsApiContent, /readSheetsWorkbook\(workbookId: string, token: string\)/)
  assert.match(sheetsApiContent, /updateSheetsWorkbookCells\(workbookId: string, body: Record<string, unknown>, token: string\)/)
  assert.match(sheetsApiContent, /\/api\/v1\/sheets\/workbooks/)
  assert.match(sheetsApiContent, /\/api\/v1\/sheets\/workbooks\/\$\{workbookId\}\/cells/)

  assert.match(docsWorkspaceContent, /useAuthStore/)
  assert.match(docsWorkspaceContent, /listDocsNotes/)
  assert.match(docsWorkspaceContent, /watch\(\(\) => \[route\.fullPath, authStore\.accessToken\]/)
  assert.match(docsWorkspaceContent, /latestDocsWorkspaceRequest/)
  assert.match(docsEditorContent, /readDocsNote/)
  assert.match(docsEditorContent, /updateDocsNote/)
  assert.match(docsEditorContent, /route-bound-editor-state/)
  assert.match(docsEditorContent, /clearEditorState/)
  assert.match(docsEditorContent, /loadedNoteMatchesRoute/)
  assert.match(docsEditorContent, /editingLocked/)
  assert.match(docsEditorContent, /canSubmitRouteEntitySave/)
  assert.match(docsEditorContent, /latestDocsNoteRequest/)

  assert.match(sheetsWorkspaceContent, /useAuthStore/)
  assert.match(sheetsWorkspaceContent, /listSheetsWorkbooks/)
  assert.match(sheetsWorkspaceContent, /watch\(\(\) => \[route\.fullPath, authStore\.accessToken\]/)
  assert.match(sheetsWorkspaceContent, /latestSheetsWorkspaceRequest/)
  assert.match(sheetsEditorContent, /readSheetsWorkbook/)
  assert.match(sheetsEditorContent, /updateSheetsWorkbookCells/)
  assert.match(sheetsEditorContent, /route-bound-editor-state/)
  assert.match(sheetsEditorContent, /clearEditorState/)
  assert.match(sheetsEditorContent, /loadedWorkbookMatchesRoute/)
  assert.match(sheetsEditorContent, /editingLocked/)
  assert.match(sheetsEditorContent, /canSubmitRouteEntitySave/)
  assert.match(sheetsEditorContent, /latestSheetsWorkbookRequest/)

  assert.match(routeBoundEditorStateContent, /createRouteEntityNavigationReset/)
  assert.match(routeBoundEditorStateContent, /canSubmitRouteEntitySave/)
  assert.doesNotMatch(docsWorkspaceContent, /const docs = \[/)
  assert.doesNotMatch(sheetsWorkspaceContent, /const sheets = \[/)
})

test('route-bound editor state clears stale entities during navigation and blocks stale saves', async () => {
  const {
    canSubmitRouteEntitySave,
    createRouteEntityNavigationReset,
    hasRouteEntityChanged,
    isCurrentRouteEntity,
    isRouteEntityEditingLocked
  } = await loadTsModule(routeBoundEditorState)

  assert.equal(hasRouteEntityChanged('doc-1', 'doc-2'), true)
  assert.equal(hasRouteEntityChanged('doc-1', 'doc-1'), false)

  const loadingReset = createRouteEntityNavigationReset('doc-2', 'token-1')
  assert.equal(loadingReset.entityLoading, true)
  assert.equal(loadingReset.saveLoading, false)

  const idleReset = createRouteEntityNavigationReset('doc-2', '')
  assert.equal(idleReset.entityLoading, false)
  assert.equal(idleReset.saveLoading, false)

  assert.equal(isCurrentRouteEntity('doc-2', 'doc-1'), false)
  assert.equal(isCurrentRouteEntity('doc-2', 'doc-2'), true)

  assert.equal(isRouteEntityEditingLocked('doc-2', 'doc-1', false), true)
  assert.equal(isRouteEntityEditingLocked('doc-2', 'doc-2', true), true)
  assert.equal(isRouteEntityEditingLocked('doc-2', 'doc-2', false), false)

  assert.equal(canSubmitRouteEntitySave('doc-2', 'doc-1', false, false, true, true), false)
  assert.equal(canSubmitRouteEntitySave('doc-2', 'doc-2', true, false, true, true), false)
  assert.equal(canSubmitRouteEntitySave('doc-2', 'doc-2', false, true, true, true), false)
  assert.equal(canSubmitRouteEntitySave('doc-2', 'doc-2', false, false, false, true), false)
  assert.equal(canSubmitRouteEntitySave('doc-2', 'doc-2', false, false, true, false), false)
  assert.equal(canSubmitRouteEntitySave('doc-2', 'doc-2', false, false, true, true), true)
})
