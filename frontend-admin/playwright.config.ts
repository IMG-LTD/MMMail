import { defineConfig, devices } from '@playwright/test';

const HOST = '127.0.0.1';
const PORT = Number(process.env.MMMAIL_E2E_PORT || 19527);
const BASE_URL = `http://${HOST}:${PORT}`;
const BACKEND_PORT = Number(process.env.MMMAIL_E2E_BACKEND_PORT || 18080);
const API_BASE_URL = process.env.MMMAIL_E2E_API_BASE_URL || `http://127.0.0.1:${BACKEND_PORT}`;
const CHROME_EXECUTABLE = process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE || '/usr/bin/google-chrome';
const LOCAL_NO_PROXY = '127.0.0.1,localhost';

process.env.NO_PROXY = mergeNoProxy(process.env.NO_PROXY);
process.env.no_proxy = mergeNoProxy(process.env.no_proxy);

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  fullyParallel: false,
  reporter: [['list']],
  globalSetup: './e2e/global-setup.ts',
  globalTeardown: './e2e/global-teardown.ts',
  expect: {
    timeout: 10_000
  },
  use: {
    baseURL: BASE_URL,
    actionTimeout: 10_000,
    navigationTimeout: 20_000,
    trace: 'retain-on-failure',
    launchOptions: {
      executablePath: CHROME_EXECUTABLE,
      args: ['--no-sandbox', '--disable-dev-shm-usage', '--no-proxy-server']
    }
  },
  webServer: {
    command: `VITE_DEVTOOLS_ENABLED=N VITE_SERVICE_BASE_URL=${API_BASE_URL} pnpm dev --mode test --host ${HOST} --port ${PORT}`,
    url: `${BASE_URL}/login`,
    reuseExistingServer: false,
    timeout: 120_000,
    stdout: 'pipe',
    stderr: 'pipe'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ]
});

function mergeNoProxy(value?: string) {
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
