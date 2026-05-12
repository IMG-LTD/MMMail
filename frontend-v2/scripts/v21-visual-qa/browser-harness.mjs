import { spawn, spawnSync } from 'node:child_process'
import { createServer } from 'node:net'
import { once } from 'node:events'
import { access, mkdir, rm, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { setTimeout as delay } from 'node:timers/promises'
import { CdpClient } from './cdp-client.mjs'
import { resolveScenarioChecks } from './scenarios.mjs'

const SERVER_TIMEOUT_MS = 30000
const PAGE_LOAD_TIMEOUT_MS = 20000
const SETTLE_DELAY_MS = 700
const SELECTOR_WAIT_TIMEOUT_MS = 20000
const SELECTOR_RETRY_DELAY_MS = 250
const HTTP_RETRY_DELAY_MS = 250
const PROCESS_STOP_TIMEOUT_MS = 2000
const SCREENSHOT_MIN_BYTES = 4096
const TEXT_SAMPLE_LIMIT = 160
const MIN_PAGE_TEXT_LENGTH = 1

const ACTION_EXPRESSIONS = {
  clickCommandPalette: clickByLabelExpression('Command palette|命令面板'),
  clickDeleteAccount: clickByLabelExpression('Delete account|删除账户|刪除帳戶'),
  clickQuickCreate: clickSelectorExpression('.quick-create-button'),
  clickThemeDrawer: clickByLabelExpression('Theme settings|主题设置|主題設定'),
  none: '(() => true)()'
}

export async function prepareOutput(paths) {
  await rm(paths.screenshotDir, { force: true, recursive: true })
  await rm(paths.chromeProfileRoot, { force: true, recursive: true })
  await mkdir(paths.screenshotDir, { recursive: true })
  await mkdir(paths.chromeProfileRoot, { recursive: true })
  await mkdir(path.dirname(paths.reportPath), { recursive: true })
}

export async function resolveChromePath(candidates) {
  if (process.env.V21_BROWSER_CHROME) {
    await access(process.env.V21_BROWSER_CHROME)
    return process.env.V21_BROWSER_CHROME
  }
  for (const candidate of candidates) {
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

export async function startViteServer(config) {
  const viteBin = path.join(config.frontendRoot, 'node_modules/vite/bin/vite.js')
  const child = spawn(process.execPath, [viteBin, '--host', config.host, '--port', String(config.port), '--strictPort'], {
    cwd: config.frontendRoot,
    stdio: ['ignore', 'pipe', 'pipe']
  })
  child.stderr.on('data', chunk => process.stderr.write(chunk))
  await waitForHttp(`http://${config.host}:${config.port}/`, SERVER_TIMEOUT_MS)
  return child
}

export async function startChrome(config) {
  const profileDir = path.join(config.chromeProfileRoot, `chrome-profile-${config.port}`)
  await mkdir(profileDir, { recursive: true })
  const child = spawn(config.chromePath, chromeArgs(config.port, profileDir), { stdio: ['ignore', 'pipe', 'pipe'] })
  child.stderr.on('data', chunk => process.stderr.write(chunk))
  await waitForHttp(`http://${config.host}:${config.port}/json/version`, SERVER_TIMEOUT_MS)
  return child
}

export async function connectCdp(versionUrl) {
  if (typeof WebSocket === 'undefined') {
    throw new Error('This Node runtime does not expose WebSocket required for the Chrome DevTools Protocol')
  }
  const response = await fetch(versionUrl)
  const payload = await response.json()
  if (!payload.webSocketDebuggerUrl) {
    throw new Error('Chrome did not expose a browser websocket URL')
  }
  const ws = new WebSocket(payload.webSocketDebuggerUrl)
  await new Promise((resolve, reject) => {
    ws.addEventListener('open', resolve, { once: true })
    ws.addEventListener('error', reject, { once: true })
  })
  return new CdpClient(ws)
}

export async function captureRouteScenario(context, scenario, viewport) {
  const checks = resolveScenarioChecks(scenario, viewport.name)
  return captureScenario(context, scenario, viewport, checks, 'route')
}

export async function captureOverlayScenario(context, scenario, viewport) {
  const checks = resolveScenarioChecks(scenario, viewport.name)
  return captureScenario(context, scenario, viewport, checks, 'overlay')
}

export async function stopProcess(child) {
  if (!child || child.exitCode !== null) {
    return
  }
  child.kill('SIGTERM')
  await Promise.race([once(child, 'exit'), delay(PROCESS_STOP_TIMEOUT_MS).then(() => child.kill('SIGKILL'))])
}

export async function findFreePort(host) {
  const server = createServer()
  server.listen(0, host)
  await once(server, 'listening')
  const address = server.address()
  const port = typeof address === 'object' && address ? address.port : 0
  server.close()
  await once(server, 'close')
  return port
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

async function captureScenario(context, scenario, viewport, checks, kind) {
  const page = await openPage(context.cdp, viewport)
  try {
    await navigate(context.cdp, page.sessionId, buildUrl(context, scenario.path))
    await activateScenario(context.cdp, page.sessionId, scenario)
    await assertPageState(context.cdp, page.sessionId, checks, scenario.id)
    const screenshot = await captureScreenshot(context, page.sessionId, `${scenario.id}-${viewport.name}.png`)
    return createResult(scenario, viewport, checks, screenshot, kind)
  } finally {
    await closePage(context.cdp, page.targetId)
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

async function activateScenario(cdp, sessionId, scenario) {
  if (!scenario.action) {
    return
  }
  const expression = ACTION_EXPRESSIONS[scenario.action]
  if (!expression) {
    throw new Error(`Unknown visual QA action: ${scenario.action}`)
  }
  const activated = await evaluate(cdp, sessionId, expression)
  if (!activated) {
    throw new Error(`Could not activate visual QA scenario ${scenario.id}`)
  }
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
  throwInvalidPageState(state, scenarioId)
}

function throwInvalidPageState(state, scenarioId) {
  if (!state || state.hasViteError || state.textLength < MIN_PAGE_TEXT_LENGTH) {
    throw new Error(`Scenario ${scenarioId} rendered an invalid page state`)
  }
  const missing = state.checks.filter(item => !item.visible).map(item => item.selector)
  if (missing.length > 0) {
    throw new Error(`Scenario ${scenarioId} missing visible selectors: ${formatMissingSelectors(state)} at ${state.url}; text: ${state.textSample}`)
  }
}

async function captureScreenshot(context, sessionId, fileName) {
  const response = await context.cdp.send('Page.captureScreenshot', { captureBeyondViewport: false, format: 'png' }, sessionId)
  const bytes = Buffer.from(response.data, 'base64')
  if (bytes.length < SCREENSHOT_MIN_BYTES) {
    throw new Error(`Screenshot ${fileName} is unexpectedly small`)
  }
  const screenshotPath = path.join(context.screenshotDir, fileName)
  await writeFile(screenshotPath, bytes)
  return path.relative(context.repoRoot, screenshotPath)
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
      checks: checks.map(selector => inspectSelector(selector))
    };
    function inspectSelector(selector) {
      const nodes = Array.from(document.querySelectorAll(selector));
      const details = nodes.map(node => {
        const style = window.getComputedStyle(node);
        const rect = node.getBoundingClientRect();
        return { display: style.display, rect: rect.width + 'x' + rect.height, visible: style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0 };
      });
      return { count: nodes.length, display: details.map(item => item.display).join('/'), rect: details.map(item => item.rect).join('/'), selector, visible: details.some(item => item.visible) };
    }
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

function createDeviceMetrics(viewport) {
  return { deviceScaleFactor: 1, height: viewport.height, mobile: viewport.mobile, width: viewport.width }
}

function createResult(scenario, viewport, checks, screenshot, kind) {
  return { checks, id: scenario.id, kind, route: scenario.path, screenshot, uiGroup: scenario.uiGroup, viewport: `${viewport.name} ${viewport.width}x${viewport.height}` }
}

function fileExists(filePath) {
  return access(filePath).then(() => true).catch(() => false)
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

function buildUrl(context, routePath) {
  return `http://${context.host}:${context.serverPort}${routePath}`
}
