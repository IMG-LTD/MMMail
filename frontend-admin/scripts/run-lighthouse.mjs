import { spawn } from 'node:child_process';
import { setTimeout as delay } from 'node:timers/promises';
import lighthouse from 'lighthouse';
import { launch } from 'chrome-launcher';

const MIN_LIGHTHOUSE_SCORE = 80;
const HOST = '127.0.0.1';
const PORT = Number(process.env.MMMAIL_LIGHTHOUSE_PORT || 9725);
const TARGET_URL = process.env.MMMAIL_LIGHTHOUSE_URL || `http://${HOST}:${PORT}/login`;
const CHROME_PATH = process.env.LIGHTHOUSE_CHROME_PATH || '/usr/bin/google-chrome';
const LOCAL_NO_PROXY = '127.0.0.1,localhost';
const CLEANUP_TIMEOUT_MS = 5_000;

let previewProcess;
let chrome;
let passed = false;

try {
  process.env.NO_PROXY = mergeNoProxy(process.env.NO_PROXY);
  process.env.no_proxy = mergeNoProxy(process.env.no_proxy);

  if (process.env.MMMAIL_LIGHTHOUSE_SKIP_BUILD !== '1') {
    await runCommand('pnpm', ['build']);
  }

  previewProcess = spawn('pnpm', ['preview', '--host', HOST, '--port', String(PORT)], {
    stdio: ['ignore', 'pipe', 'pipe']
  });
  await waitForHttp(TARGET_URL);

  chrome = await launch({
    chromePath: CHROME_PATH,
    chromeFlags: ['--headless=new', '--no-sandbox', '--disable-dev-shm-usage', '--no-proxy-server']
  });

  const result = await lighthouse(TARGET_URL, {
    port: chrome.port,
    output: 'json',
    logLevel: 'error',
    preset: 'desktop',
    onlyCategories: ['performance']
  });
  const score = (result?.lhr.categories.performance.score || 0) * 100;
  const displayScore = Number(score.toFixed(1));

  console.log(`[lighthouse] ${TARGET_URL} performance=${displayScore}`);
  if (score <= MIN_LIGHTHOUSE_SCORE) {
    throw new Error(`Lighthouse performance score ${displayScore} must be greater than ${MIN_LIGHTHOUSE_SCORE}`);
  }
  passed = true;
} finally {
  await killChrome(chrome);
  await stopProcess(previewProcess);
}

if (passed) {
  // Lighthouse can leave library handles open after cleanup; this script is a CLI gate.
  process.exit(0);
}

function runCommand(command, args) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { stdio: 'inherit' });
    child.on('error', reject);
    child.on('exit', code => {
      if (code === 0) {
        resolve();
        return;
      }
      reject(new Error(`${command} ${args.join(' ')} failed with exit code ${code}`));
    });
  });
}

async function waitForHttp(url) {
  const deadline = Date.now() + 120_000;
  while (Date.now() < deadline) {
    if (await canFetch(url)) {
      return;
    }
    await delay(500);
  }
  throw new Error(`Timed out waiting for ${url}`);
}

async function canFetch(url) {
  try {
    const response = await fetch(url);
    return response.ok;
  } catch {
    return false;
  }
}

async function killChrome(instance) {
  if (!instance) {
    return;
  }

  await Promise.race([instance.kill(), delay(CLEANUP_TIMEOUT_MS)]);
}

async function stopProcess(child) {
  if (!child || child.exitCode !== null || child.signalCode !== null) {
    return;
  }

  const exited = waitForProcessExit(child);
  if (!child.killed) {
    child.kill('SIGTERM');
  }

  const stopped = await Promise.race([exited.then(() => true), delay(CLEANUP_TIMEOUT_MS).then(() => false)]);
  if (stopped) {
    return;
  }

  child.kill('SIGKILL');
  await Promise.race([exited, delay(CLEANUP_TIMEOUT_MS)]);
}

function waitForProcessExit(child) {
  return new Promise(resolve => {
    child.once('exit', resolve);
  });
}

function mergeNoProxy(value) {
  if (!value) {
    return LOCAL_NO_PROXY;
  }
  const parts = new Set(
    value
      .split(',')
      .map(item => item.trim())
      .filter(Boolean)
  );
  LOCAL_NO_PROXY.split(',').forEach(item => parts.add(item));
  return [...parts].join(',');
}
