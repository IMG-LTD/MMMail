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
const CHROME_CANDIDATES = ['/usr/bin/google-chrome', '/usr/bin/google-chrome-stable', '/usr/bin/chromium']
const SCREENSHOT_DIR = path.join(REPO_ROOT, '.tmp/v21-browser-visual-qa')
const CHROME_PROFILE_ROOT = path.join(REPO_ROOT, '.tmp/v21-browser-visual-qa-chrome')
const REPORT_PATH = path.join(REPO_ROOT, REPORT_PATH_SUFFIX)

// Chrome DevTools Protocol keeps this runner dependency-free while still using a real browser.
export async function runVisualQa() {
  const chromePath = await resolveChromePath(CHROME_CANDIDATES)
  const serverPort = await findFreePort(HOST)
  const chromePort = await findFreePort(HOST)
  await prepareOutput({ chromeProfileRoot: CHROME_PROFILE_ROOT, reportPath: REPORT_PATH, screenshotDir: SCREENSHOT_DIR })
  const server = await startViteServer({ frontendRoot: FRONTEND_ROOT, host: HOST, port: serverPort })
  const chrome = await startChrome({ chromePath, chromeProfileRoot: CHROME_PROFILE_ROOT, host: HOST, port: chromePort })
  let cdp

  try {
    cdp = await connectCdp(`http://${HOST}:${chromePort}/json/version`)
    const context = { cdp, host: HOST, repoRoot: REPO_ROOT, screenshotDir: SCREENSHOT_DIR, serverPort }
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
