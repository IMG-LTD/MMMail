import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const packageJsonUrl = new URL('../package.json', import.meta.url)
const qaScriptUrl = new URL('../scripts/v21-browser-visual-qa.mjs', import.meta.url)
const browserHarnessUrl = new URL('../scripts/v21-visual-qa/browser-harness.mjs', import.meta.url)
const scenariosUrl = new URL('../scripts/v21-visual-qa/scenarios.mjs', import.meta.url)
const reportUrl = new URL('../scripts/v21-visual-qa/report.mjs', import.meta.url)

const requiredUiGroups = [
  '首页',
  '邮件',
  '日历',
  '云盘',
  '文档',
  'Sheets和labs',
  'Pass',
  'Collaboration',
  'CommandCenter',
  'Notifications',
  'Admin',
  'Setting',
  'PublicAuthShareSystem'
]

const requiredRoutes = [
  '/workspace',
  '/mail/inbox',
  '/mail/compose',
  '/calendar',
  '/drive',
  '/docs',
  '/docs/demo-document',
  '/sheets',
  '/sheets/demo-sheet',
  '/labs',
  '/pass',
  '/pass/secure-links',
  '/pass/monitor',
  '/collaboration',
  '/command-center',
  '/notifications',
  '/admin',
  '/admin/users',
  '/admin/system',
  '/admin/risk',
  '/settings',
  '/login',
  '/register',
  '/boundary',
  '/product-access-blocked',
  '/share/mail/demo-token',
  '/share/drive/demo-token',
  '/share/pass/demo-token',
  '/offline',
  '/maintenance',
  '/404',
  '/500'
]

const requiredEvidenceIds = [
  'command-palette',
  'quick-create',
  'theme-drawer',
  'mail-compose',
  'mail-compose-security',
  'mail-thread-workbench',
  'calendar-event-drawer',
  'drive-share-panel',
  'docs-share-panel',
  'sheets-protected-range',
  'pass-secret-actions',
  'pass-secure-link-settings',
  'pass-risk-detail',
  'settings-delete-confirmation'
]

const requiredOverlayContracts = [
  { action: 'activateMailComposeSecurity', id: 'mail-compose-security', selector: '.mail-trust-panel' },
  { action: 'openCalendarEventDrawer', id: 'calendar-event-drawer', selector: '.calendar-event-drawer' },
  { action: 'clickDriveSharePanel', id: 'drive-share-panel', selector: '.drive-share-panel' },
  { action: 'clickDocsSharePanel', id: 'docs-share-panel', selector: '.docs-share-panel' },
  { action: 'clickSheetsProtectedRange', id: 'sheets-protected-range', selector: '.sheets-protected-range-modal' },
  { action: 'activatePassSecretActions', id: 'pass-secret-actions', selector: '.pass-rotate-confirmation' },
  { action: 'openPassShareSettings', id: 'pass-secure-link-settings', selector: '.pass-share-settings-modal' },
  { action: 'openPassRiskDetail', id: 'pass-risk-detail', selector: '.pass-risk-detail' }
]

test('v2.1 browser visual QA runner exposes expanded coverage registry', async () => {
  const [packageJsonRaw, qaScript, browserHarness, scenarioSource, reportSource] = await Promise.all([
    readFile(packageJsonUrl, 'utf8'),
    readFile(qaScriptUrl, 'utf8'),
    readFile(browserHarnessUrl, 'utf8'),
    readFile(scenariosUrl, 'utf8'),
    readFile(reportUrl, 'utf8')
  ])
  const packageJson = JSON.parse(packageJsonRaw)

  assert.equal(packageJson.scripts['visual:qa'], 'node scripts/v21-browser-visual-qa.mjs')
  assert.match(qaScript, /runVisualQa/)
  assert.match(qaScript, /Chrome DevTools Protocol/)
  assert.match(qaScript, /apiBaseUrl/)
  assert.match(qaScript, /prepareRuntimeData/)
  assert.match(qaScript, /docsNoteId/)
  assert.match(qaScript, /sheetsWorkbookId/)
  assert.match(browserHarness, /injectVisualQaBrowserState/)
  assert.match(browserHarness, /resolveScenarioPath/)
  assert.match(browserHarness, /VITE_API_BASE_URL/)
  assert.match(browserHarness, /mmmail\.auth\.session\.v1/)
  assert.match(browserHarness, /mmmail\.onboarding\.v1/)
  assert.match(browserHarness, /hasSeenGuide:\s*true/)
  assert.match(browserHarness, /vite-error-overlay/)
  assert.match(browserHarness, /Failed to load module/)
  assert.doesNotMatch(browserHarness, /Internal server error\|Failed to load module/)
  assert.match(browserHarness, /openCalendarEventDrawer:\s*clickSelectorExpression\('\.calendar-event-trigger'\)/)
  assert.doesNotMatch(browserHarness, /openCalendarEventDrawer:\s*clickAndSubmitExpression/)
  assert.match(scenarioSource, /1440/)
  assert.match(scenarioSource, /1024/)
  assert.match(scenarioSource, /390/)
  for (const group of requiredUiGroups) {
    assert.match(scenarioSource, new RegExp(group))
  }
  for (const route of requiredRoutes) {
    assert.match(scenarioSource, new RegExp(route.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
  }
  for (const evidenceId of requiredEvidenceIds) {
    assert.match(scenarioSource, new RegExp(evidenceId))
  }
  for (const contract of requiredOverlayContracts) {
    assert.match(scenarioSource, new RegExp(contract.id))
    assert.match(scenarioSource, new RegExp(contract.action))
    assert.match(scenarioSource, new RegExp(escapeRegExp(contract.selector)))
  }
  assert.doesNotMatch(scenarioSource, /overlay\('drive-share-panel', '云盘', '\/drive', 'none', \['\.drive-surface', '\.drive-surface__table'\]\)/)
  assert.doesNotMatch(scenarioSource, /overlay\('docs-share-panel', '文档', '\/docs\/demo-document', 'none', \['\.docs-editor__actions', '\.docs-editor__panel'\]\)/)
  assert.doesNotMatch(scenarioSource, /overlay\('sheets-protected-range', 'Sheets和labs', '\/sheets\/demo-sheet', 'none', \['\.sheets-editor__formula', '\.sheets-editor__side'\]\)/)
  assert.match(reportSource, /UI group/)
  assert.match(reportSource, /Screenshot evidence/)
  assert.match(reportSource, /Covered overlay and panel evidence/)
  assert.match(reportSource, /v21-browser-visual-qa-report\.md/)
})

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
