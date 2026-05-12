#!/usr/bin/env node
import { spawn, spawnSync } from 'node:child_process'
import { createServer } from 'node:net'
import { once } from 'node:events'
import { access, mkdir, rm, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { setTimeout as delay } from 'node:timers/promises'

const FRONTEND_ROOT = path.resolve(fileURLToPath(new URL('..', import.meta.url)))
const REPO_ROOT = path.resolve(FRONTEND_ROOT, '..')
const HOST = '127.0.0.1'
const SERVER_TIMEOUT_MS = 30000
const PAGE_LOAD_TIMEOUT_MS = 20000
const SETTLE_DELAY_MS = 700
const SELECTOR_WAIT_TIMEOUT_MS = 8000
const SELECTOR_RETRY_DELAY_MS = 250
const HTTP_RETRY_DELAY_MS = 250
const PROCESS_STOP_TIMEOUT_MS = 2000
const SCREENSHOT_MIN_BYTES = 4096
const TEXT_SAMPLE_LIMIT = 160
const MIN_PAGE_TEXT_LENGTH = 1
const CHROME_CANDIDATES = ['/usr/bin/google-chrome', '/usr/bin/google-chrome-stable', '/usr/bin/chromium']
const SCREENSHOT_DIR = path.join(REPO_ROOT, '.tmp/v21-browser-visual-qa')
const CHROME_PROFILE_ROOT = path.join(REPO_ROOT, '.tmp/v21-browser-visual-qa-chrome')
const REPORT_PATH = path.join(REPO_ROOT, 'docs/superpowers/progress/v21-browser-visual-qa-report.md')

const VIEWPORTS = [
  { height: 900, mobile: false, name: 'desktop', width: 1440 },
  { height: 768, mobile: false, name: 'tablet', width: 1024 },
  { height: 844, mobile: false, name: 'mobile', width: 390 }
]

const ROUTE_SCENARIOS = [
  {
    checks: ['.base-layout', '.top-bar', '.base-layout__content'],
    id: 'workspace-shell',
    path: '/workspace',
    viewportChecks: {
      desktop: ['.side-nav', '.context-panel'],
      tablet: ['.side-nav', '.context-panel'],
      mobile: ['.mobile-tab-bar']
    }
  },
  { checks: ['.command-grid', '.chart-card', '.data-table', '.terminal-log'], id: 'command-center', path: '/command-center' },
  { checks: ['.notifications-layout', '.chart-card', '.data-table'], id: 'notifications', path: '/notifications' },
  { checks: ['.sheets-workspace', '.data-table', '.sheets-preview'], id: 'sheets', path: '/sheets' }
]

const OVERLAY_SCENARIOS = [
  {
    action: clickByLabelExpression('Command palette|命令面板'),
    checks: ['.command-palette[role="dialog"]'],
    id: 'command-palette',
    path: '/workspace'
  },
  {
    action: clickSelectorExpression('.quick-create-button'),
    checks: ['.mm-modal[role="dialog"][aria-modal="true"]'],
    id: 'quick-create',
    path: '/workspace'
  },
  {
    action: clickByLabelExpression('Theme settings|主题设置|主題設定'),
    checks: ['.mm-drawer[role="dialog"][aria-modal="true"]'],
    id: 'theme-drawer',
    path: '/workspace'
  }
]

class CdpClient {
  constructor(ws) {
    this.nextId = 1
    this.pending = new Map()
    this.waiters = []
    this.ws = ws
    ws.addEventListener('message', event => this.handleMessage(event.data))
  }

  close() {
    this.ws.close()
  }

  handleMessage(data) {
    const text = typeof data === 'string' ? data : data.toString()
    const message = JSON.parse(text)
    if (message.id && this.pending.has(message.id)) {
      this.resolvePending(message)
      return
    }
    this.resolveWaiters(message)
  }

  resolvePending(message) {
    const pending = this.pending.get(message.id)
    this.pending.delete(message.id)
    if (message.error) {
      pending.reject(new Error(`${message.error.message}: ${JSON.stringify(message.error.data || '')}`))
      return
    }
    pending.resolve(message.result || {})
  }

  resolveWaiters(message) {
    this.waiters = this.waiters.filter(waiter => {
      if (waiter.method !== message.method || waiter.sessionId !== message.sessionId) {
        return true
      }
      clearTimeout(waiter.timer)
      waiter.resolve(message.params || {})
      return false
    })
  }

  send(method, params = {}, sessionId) {
    const id = this.nextId++
    const payload = sessionId ? { id, method, params, sessionId } : { id, method, params }
    this.ws.send(JSON.stringify(payload))
    return new Promise((resolve, reject) => {
      this.pending.set(id, { reject, resolve })
    })
  }

  waitForEvent(method, sessionId, timeoutMs) {
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error(`Timed out waiting for ${method}`)), timeoutMs)
      this.waiters.push({ method, reject, resolve, sessionId, timer })
    })
  }
}

async function main() {
  const chromePath = await resolveChromePath()
  const serverPort = await findFreePort()
  const chromePort = await findFreePort()
  await prepareOutput()
  const server = await startViteServer(serverPort)
  const chrome = await startChrome(chromePath, chromePort)
  let cdp

  try {
    const browserWs = await readBrowserWsUrl(chromePort)
    cdp = await connectCdp(browserWs)
    const results = await runScenarios(cdp, serverPort)
    await writeReport(results)
    console.log(`v2.1 browser visual QA passed: ${results.length} screenshots`)
    console.log(`Report: ${path.relative(REPO_ROOT, REPORT_PATH)}`)
  } finally {
    cdp?.close()
    await stopProcess(chrome)
    await stopProcess(server)
  }
}

async function runScenarios(cdp, serverPort) {
  const results = []
  for (const scenario of ROUTE_SCENARIOS) {
    for (const viewport of VIEWPORTS) {
      results.push(await captureRouteScenario(cdp, serverPort, scenario, viewport))
    }
  }
  for (const scenario of OVERLAY_SCENARIOS) {
    results.push(await captureOverlayScenario(cdp, serverPort, scenario))
  }
  return results
}

async function captureRouteScenario(cdp, serverPort, scenario, viewport) {
  const page = await openPage(cdp, viewport)
  try {
    const url = buildUrl(serverPort, scenario.path)
    await navigate(cdp, page.sessionId, url)
    const checks = resolveScenarioChecks(scenario, viewport.name)
    await assertPageState(cdp, page.sessionId, checks, scenario.id)
    const screenshot = await captureScreenshot(cdp, page.sessionId, `${scenario.id}-${viewport.name}.png`)
    return createResult(scenario, viewport, checks, screenshot)
  } finally {
    await closePage(cdp, page.targetId)
  }
}

async function captureOverlayScenario(cdp, serverPort, scenario) {
  const viewport = VIEWPORTS[0]
  const page = await openPage(cdp, viewport)
  try {
    const url = buildUrl(serverPort, scenario.path)
    await navigate(cdp, page.sessionId, url)
    const clicked = await evaluate(cdp, page.sessionId, scenario.action)
    if (!clicked) {
      throw new Error(`Could not activate overlay scenario ${scenario.id}`)
    }
    await delay(SETTLE_DELAY_MS)
    await assertPageState(cdp, page.sessionId, scenario.checks, scenario.id)
    const screenshot = await captureScreenshot(cdp, page.sessionId, `${scenario.id}-${viewport.name}.png`)
    return createResult(scenario, viewport, scenario.checks, screenshot)
  } finally {
    await closePage(cdp, page.targetId)
  }
}

async function openPage(cdp, viewport) {
  const { targetId } = await cdp.send('Target.createTarget', { url: 'about:blank' })
  const { sessionId } = await cdp.send('Target.attachToTarget', { flatten: true, targetId })
  await cdp.send('Page.enable', {}, sessionId)
  await cdp.send('Runtime.enable', {}, sessionId)
  await cdp.send('Emulation.setDeviceMetricsOverride', createDeviceMetrics(viewport), sessionId)
  return { sessionId, targetId }
}

async function navigate(cdp, sessionId, url) {
  const loaded = cdp.waitForEvent('Page.loadEventFired', sessionId, PAGE_LOAD_TIMEOUT_MS)
  await cdp.send('Page.navigate', { url }, sessionId)
  await loaded
  await delay(SETTLE_DELAY_MS)
}

async function assertPageState(cdp, sessionId, checks, scenarioId) {
  const deadline = Date.now() + SELECTOR_WAIT_TIMEOUT_MS
  let state = null
  while (Date.now() < deadline) {
    state = await evaluate(cdp, sessionId, buildDomCheckExpression(checks))
    if (isPageStateValid(state)) {
      return
    }
    await delay(SELECTOR_RETRY_DELAY_MS)
  }
  if (!state || state.hasViteError || state.textLength < MIN_PAGE_TEXT_LENGTH) {
    throw new Error(`Scenario ${scenarioId} rendered an invalid page state`)
  }
  const missing = state.checks.filter(item => !item.visible).map(item => item.selector)
  if (missing.length > 0) {
    throw new Error(`Scenario ${scenarioId} missing visible selectors: ${formatMissingSelectors(state)} at ${state.url}; text: ${state.textSample}`)
  }
}

function formatMissingSelectors(state) {
  return state.checks
    .filter(item => !item.visible)
    .map(item => `${item.selector} count=${item.count} display=${item.display} rect=${item.rect}`)
    .join(', ')
}

function isPageStateValid(state) {
  return Boolean(state) && !state.hasViteError && state.textLength >= MIN_PAGE_TEXT_LENGTH && state.checks.every(item => item.visible)
}

async function captureScreenshot(cdp, sessionId, fileName) {
  const response = await cdp.send('Page.captureScreenshot', { captureBeyondViewport: false, format: 'png' }, sessionId)
  const bytes = Buffer.from(response.data, 'base64')
  if (bytes.length < SCREENSHOT_MIN_BYTES) {
    throw new Error(`Screenshot ${fileName} is unexpectedly small`)
  }
  const screenshotPath = path.join(SCREENSHOT_DIR, fileName)
  await writeFile(screenshotPath, bytes)
  return path.relative(REPO_ROOT, screenshotPath)
}

async function evaluate(cdp, sessionId, expression) {
  const response = await cdp.send('Runtime.evaluate', { awaitPromise: true, expression, returnByValue: true }, sessionId)
  if (response.exceptionDetails) {
    throw new Error(`Browser evaluation failed: ${response.exceptionDetails.text}`)
  }
  return response.result.value
}

async function closePage(cdp, targetId) {
  await cdp.send('Target.closeTarget', { targetId })
}

function createDeviceMetrics(viewport) {
  return {
    deviceScaleFactor: 1,
    height: viewport.height,
    mobile: viewport.mobile,
    width: viewport.width
  }
}

function resolveScenarioChecks(scenario, viewportName) {
  return [...scenario.checks, ...(scenario.viewportChecks?.[viewportName] || [])]
}

function buildDomCheckExpression(checks) {
  return `(() => {
    const checks = ${JSON.stringify(checks)};
    const text = document.body?.innerText || '';
    const hasViteError = Boolean(document.querySelector('vite-error-overlay')) || /Internal server error|Failed to load module/i.test(text);
    return {
      hasViteError,
      textSample: text.trim().slice(0, ${TEXT_SAMPLE_LIMIT}),
      textLength: text.trim().length,
      url: window.location.href,
      checks: checks.map(selector => {
        const nodes = Array.from(document.querySelectorAll(selector));
        const details = nodes.map(node => {
          const style = window.getComputedStyle(node);
          const rect = node.getBoundingClientRect();
          return { display: style.display, rect: rect.width + 'x' + rect.height, visible: style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0 };
        });
        return {
          count: nodes.length,
          display: details.map(item => item.display).join('/'),
          rect: details.map(item => item.rect).join('/'),
          selector,
          visible: details.some(item => item.visible)
        };
      })
    };
  })()`
}

function clickSelectorExpression(selector) {
  return `(() => {
    const target = document.querySelector(${JSON.stringify(selector)});
    if (!target) return false;
    target.click();
    return true;
  })()`
}

function clickByLabelExpression(pattern) {
  return `(() => {
    const matcher = new RegExp(${JSON.stringify(pattern)}, 'i');
    const buttons = Array.from(document.querySelectorAll('button'));
    const target = buttons.find(button => matcher.test(button.getAttribute('aria-label') || button.textContent || ''));
    if (!target) return false;
    target.click();
    return true;
  })()`
}

function createResult(scenario, viewport, checks, screenshot) {
  return {
    checks,
    id: scenario.id,
    route: scenario.path,
    screenshot,
    viewport: `${viewport.name} ${viewport.width}x${viewport.height}`
  }
}

async function writeReport(results) {
  const rows = results.map(item => {
    return `| ${item.id} | ${item.viewport} | \`${item.route}\` | \`${item.checks.join('`, `')}\` | \`${item.screenshot}\` |`
  })
  const report = [
    '# v2.1 Browser Visual QA Report',
    '',
    'Generated by `pnpm --dir frontend-v2 visual:qa`.',
    '',
    '| Scenario | Viewport | Route | Required visible selectors | Screenshot evidence |',
    '| --- | --- | --- | --- | --- |',
    ...rows,
    '',
    'Screenshots are evidence artifacts under `.tmp/` and are intentionally not committed.'
  ].join('\n')
  await writeFile(REPORT_PATH, `${report}\n`)
}

async function prepareOutput() {
  await rm(SCREENSHOT_DIR, { force: true, recursive: true })
  await rm(CHROME_PROFILE_ROOT, { force: true, recursive: true })
  await mkdir(SCREENSHOT_DIR, { recursive: true })
  await mkdir(CHROME_PROFILE_ROOT, { recursive: true })
  await mkdir(path.dirname(REPORT_PATH), { recursive: true })
}

async function resolveChromePath() {
  if (process.env.V21_BROWSER_CHROME) {
    await access(process.env.V21_BROWSER_CHROME)
    return process.env.V21_BROWSER_CHROME
  }
  for (const candidate of CHROME_CANDIDATES) {
    if (await fileExists(candidate)) {
      return candidate
    }
  }
  const resolved = spawnSync('which', ['google-chrome'], { encoding: 'utf8' })
  if (resolved.status === 0 && resolved.stdout.trim()) {
    return resolved.stdout.trim()
  }
  throw new Error('google-chrome was not found; set V21_BROWSER_CHROME to an executable Chrome path')
}

async function fileExists(filePath) {
  try {
    await access(filePath)
    return true
  } catch {
    return false
  }
}

async function startViteServer(port) {
  const viteBin = path.join(FRONTEND_ROOT, 'node_modules/vite/bin/vite.js')
  const child = spawn(process.execPath, [viteBin, '--host', HOST, '--port', String(port), '--strictPort'], {
    cwd: FRONTEND_ROOT,
    stdio: ['ignore', 'pipe', 'pipe']
  })
  child.stderr.on('data', chunk => process.stderr.write(chunk))
  await waitForHttp(`http://${HOST}:${port}/`, SERVER_TIMEOUT_MS)
  return child
}

async function startChrome(chromePath, port) {
  const profileDir = path.join(CHROME_PROFILE_ROOT, `chrome-profile-${port}`)
  await mkdir(profileDir, { recursive: true })
  const child = spawn(chromePath, chromeArgs(port, profileDir), { stdio: ['ignore', 'pipe', 'pipe'] })
  child.stderr.on('data', chunk => process.stderr.write(chunk))
  await waitForHttp(`http://${HOST}:${port}/json/version`, SERVER_TIMEOUT_MS)
  return child
}

function chromeArgs(port, profileDir) {
  return [
    '--headless=new',
    '--disable-gpu',
    '--disable-dev-shm-usage',
    '--no-first-run',
    '--no-default-browser-check',
    '--no-sandbox',
    `--remote-debugging-port=${port}`,
    `--user-data-dir=${profileDir}`,
    'about:blank'
  ]
}

async function readBrowserWsUrl(port) {
  const response = await fetch(`http://${HOST}:${port}/json/version`)
  const payload = await response.json()
  if (!payload.webSocketDebuggerUrl) {
    throw new Error('Chrome did not expose a browser websocket URL')
  }
  return payload.webSocketDebuggerUrl
}

async function connectCdp(webSocketUrl) {
  if (typeof WebSocket === 'undefined') {
    throw new Error('This Node runtime does not expose WebSocket required for the Chrome DevTools Protocol')
  }
  const ws = new WebSocket(webSocketUrl)
  await new Promise((resolve, reject) => {
    ws.addEventListener('open', resolve, { once: true })
    ws.addEventListener('error', reject, { once: true })
  })
  return new CdpClient(ws)
}

async function waitForHttp(url, timeoutMs) {
  const startedAt = Date.now()
  while (Date.now() - startedAt < timeoutMs) {
    try {
      const response = await fetch(url)
      if (response.ok) {
        return
      }
    } catch {
    }
    await delay(HTTP_RETRY_DELAY_MS)
  }
  throw new Error(`Timed out waiting for ${url}`)
}

async function findFreePort() {
  const server = createServer()
  server.listen(0, HOST)
  await once(server, 'listening')
  const address = server.address()
  const port = typeof address === 'object' && address ? address.port : 0
  server.close()
  await once(server, 'close')
  return port
}

async function stopProcess(child) {
  if (!child || child.exitCode !== null) {
    return
  }
  child.kill('SIGTERM')
  await Promise.race([once(child, 'exit'), delay(PROCESS_STOP_TIMEOUT_MS).then(() => child.kill('SIGKILL'))])
}

function buildUrl(port, routePath) {
  return `http://${HOST}:${port}${routePath}`
}

main().catch(error => {
  console.error(error instanceof Error ? error.message : error)
  process.exitCode = 1
})
