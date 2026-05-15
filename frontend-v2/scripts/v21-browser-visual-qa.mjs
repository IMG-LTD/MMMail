#!/usr/bin/env node
import { writeFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  captureOverlayScenario,
  captureRouteScenario,
  connectCdp,
  findFreePort,
  prepareOutput,
  resolveChromePath,
  startChrome,
  startViteServer,
  stopProcess
} from './v21-visual-qa/browser-harness.mjs'
import {
  DESKTOP_VIEWPORT,
  OVERLAY_SCENARIOS,
  allRouteScenarios,
  resolveScenarioViewports
} from './v21-visual-qa/scenarios.mjs'
import { REPORT_PATH_SUFFIX, writeVisualQaReport } from './v21-visual-qa/report.mjs'

const FRONTEND_ROOT = path.resolve(fileURLToPath(new URL('..', import.meta.url)))
const REPO_ROOT = path.resolve(FRONTEND_ROOT, '..')
const HOST = '127.0.0.1'
const BACKEND_PORT = process.env.V21_BACKEND_PORT || '8080'
const API_BASE_URL = `http://${HOST}:${BACKEND_PORT}`
const CHROME_CANDIDATES = ['/usr/bin/google-chrome', '/usr/bin/google-chrome-stable', '/usr/bin/chromium']
const SCREENSHOT_DIR = path.join(REPO_ROOT, '.tmp/v21-browser-visual-qa')
const CHROME_PROFILE_ROOT = path.join(REPO_ROOT, '.tmp/v21-browser-visual-qa-chrome')
const REPORT_PATH = path.join(REPO_ROOT, REPORT_PATH_SUFFIX)

async function obtainAuthSession() {
  const email = `visualqa-${Date.now()}@mmmail.dev`
  const body = JSON.stringify({ email, password: 'VisualQa2026!', displayName: 'Visual QA' })
  const res = await fetch(`${API_BASE_URL}/api/v2/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body
  })
  const json = await res.json()
  if (!json.data?.accessToken || !json.data?.user) {
    throw new Error(`Failed to obtain auth session: ${json.message || res.status}`)
  }
  return { accessToken: json.data.accessToken, user: json.data.user }
}

async function prepareRuntimeData(authSession) {
  const [docsNote, sheetsWorkbook] = await Promise.all([
    createRuntimeRecord('/api/v2/docs', authSession.accessToken, {
      content: '# Round6 visual QA\n- Runtime document content\n- Share panel coverage',
      title: 'Round6 visual QA document'
    }),
    createRuntimeRecord('/api/v2/sheets', authSession.accessToken, {
      colCount: 4,
      rowCount: 4,
      title: 'Round6 visual QA workbook'
    })
  ])
  return { docsNoteId: String(docsNote.id), sheetsWorkbookId: String(sheetsWorkbook.id) }
}

async function createRuntimeRecord(apiPath, token, payload) {
  const response = await fetch(`${API_BASE_URL}${apiPath}`, {
    body: JSON.stringify(payload),
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    method: 'POST'
  })
  const body = await readRuntimeResponse(response)
  if (!body.data?.id) {
    throw new Error(`Runtime setup ${apiPath} did not return a data.id`)
  }
  return body.data
}

async function readRuntimeResponse(response) {
  const payload = await response.text()
  if (!response.ok) {
    throw new Error(`Runtime setup failed: HTTP ${response.status} ${response.statusText} ${payload}`)
  }
  return payload ? JSON.parse(payload) : {}
}

// Chrome DevTools Protocol keeps this runner dependency-free while still using a real browser.
export async function runVisualQa() {
  const chromePath = await resolveChromePath(CHROME_CANDIDATES)
  const serverPort = await findFreePort(HOST)
  const chromePort = await findFreePort(HOST)
  await prepareOutput({ chromeProfileRoot: CHROME_PROFILE_ROOT, reportPath: REPORT_PATH, screenshotDir: SCREENSHOT_DIR })
  const authSession = await obtainAuthSession()
  const runtimeData = await prepareRuntimeData(authSession)
  const server = await startViteServer({ apiBaseUrl: API_BASE_URL, frontendRoot: FRONTEND_ROOT, host: HOST, port: serverPort })
  const chrome = await startChrome({ chromePath, chromeProfileRoot: CHROME_PROFILE_ROOT, host: HOST, port: chromePort })
  let cdp

  try {
    cdp = await connectCdp(`http://${HOST}:${chromePort}/json/version`)
    const context = { authSession, cdp, host: HOST, repoRoot: REPO_ROOT, runtimeData, screenshotDir: SCREENSHOT_DIR, serverPort }
    const results = await runScenarios(context)
    await writeVisualQaReport({ generatedAt: new Date().toISOString(), reportPath: REPORT_PATH, results, writeFile })
    console.log(`v2.1 browser visual QA passed: ${results.length} screenshots`)
    console.log(`Report: ${path.relative(REPO_ROOT, REPORT_PATH)}`)
  } finally {
    cdp?.close()
    await stopProcess(chrome)
    await stopProcess(server)
  }
}

async function runScenarios(context) {
  const results = []
  for (const scenario of allRouteScenarios()) {
    for (const viewport of resolveScenarioViewports(scenario)) {
      results.push(await captureRouteScenario(context, scenario, viewport))
    }
  }
  for (const scenario of OVERLAY_SCENARIOS) {
    results.push(await captureOverlayScenario(context, scenario, DESKTOP_VIEWPORT))
  }
  return results
}

runVisualQa().catch(error => {
  console.error(error instanceof Error ? error.message : error)
  process.exitCode = 1
})
