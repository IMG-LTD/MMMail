import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const root = new URL('../', import.meta.url);

const deploymentEnvVars = [
  'MMMAIL_SECURITY_MAIL_SEND_RATE_LIMIT_WINDOW_SECONDS',
  'MMMAIL_SECURITY_MAIL_SEND_RATE_LIMIT_MAX_EVENTS',
  'MMMAIL_SECURITY_WEB_PUSH_TEST_RATE_LIMIT_WINDOW_SECONDS',
  'MMMAIL_SECURITY_WEB_PUSH_TEST_RATE_LIMIT_MAX_EVENTS',
  'MMMAIL_SECURITY_COMMAND_RUN_RATE_LIMIT_WINDOW_SECONDS',
  'MMMAIL_SECURITY_COMMAND_RUN_RATE_LIMIT_MAX_EVENTS',
  'MMMAIL_WEBSOCKET_AFFINITY_COOKIE_NAME',
  'MMMAIL_WEBSOCKET_AFFINITY_NODE_ID',
  'MMMAIL_WEBSOCKET_AFFINITY_COOKIE_SECURE',
  'MMMAIL_WEBSOCKET_AFFINITY_COOKIE_MAX_AGE_SECONDS',
  'MMMAIL_WEBSOCKET_SESSION_MAX_IDLE_MS',
  'MMMAIL_WEBSOCKET_CONNECTION_MAX_ACTIVE',
  'MMMAIL_WEBSOCKET_CONNECTION_RETRY_AFTER_MS',
  'MMMAIL_WEBSOCKET_SUBSCRIPTION_MAX_CHANNELS_PER_SESSION',
  'MMMAIL_WEBSOCKET_SUBSCRIPTION_RETRY_AFTER_MS',
  'MMMAIL_WEBSOCKET_RATE_LIMIT_WINDOW_SECONDS',
  'MMMAIL_WEBSOCKET_RATE_LIMIT_MAX_MESSAGES_PER_WINDOW',
  'MMMAIL_WEBSOCKET_RATE_LIMIT_RETRY_AFTER_MS',
  'MMMAIL_MAIL_EXTERNAL_ACCOUNT_SECRET',
  'MMMAIL_WEB_PUSH_VAPID_SUBJECT',
  'MMMAIL_WEB_PUSH_VAPID_PUBLIC_KEY',
  'MMMAIL_WEB_PUSH_VAPID_PRIVATE_KEY',
  'MMMAIL_FEATURE_FLAGS_WATCH_INTERVAL_MS'
];

const devSeedEnvVars = [
  'MMMAIL_DEV_SEED_ENABLED',
  'MMMAIL_DEV_SEED_WALLET',
  'MMMAIL_DEV_SEED_MEET',
  'MMMAIL_DEV_SEED_COMMUNITY',
  'MMMAIL_DEV_SEED_SEARCH_INDEX',
  'MMMAIL_DEV_SEED_DOMAIN',
  'MMMAIL_DEV_SEED_WEBPUSH'
];

async function readText(path) {
  return readFile(new URL(path, root), 'utf8');
}

function assertContainsEvery(source, vars, label) {
  for (const envVar of vars) {
    assert.ok(source.includes(envVar), `${label} missing ${envVar}`);
  }
}

test('v2.1.2 deployment env templates expose all new backend runtime knobs', async () => {
  const [rootEnv, backendEnv] = await Promise.all([readText('.env.example'), readText('config/backend.env.example')]);

  assertContainsEvery(rootEnv, deploymentEnvVars, '.env.example');
  assertContainsEvery(rootEnv, devSeedEnvVars, '.env.example');
  assertContainsEvery(backendEnv, deploymentEnvVars, 'config/backend.env.example');
  assertContainsEvery(backendEnv, devSeedEnvVars, 'config/backend.env.example');
});

test('v2.1.2 compose files pass production runtime knobs through to backend', async () => {
  const [compose, minimalCompose] = await Promise.all([readText('docker-compose.yml'), readText('docker-compose.minimal.yml')]);

  assertContainsEvery(compose, deploymentEnvVars, 'docker-compose.yml');
  assertContainsEvery(minimalCompose, deploymentEnvVars, 'docker-compose.minimal.yml');
});

test('v2.1.2 deployment runbook documents new runtime env groups', async () => {
  const runbook = await readText('docs/deployment-runbook.md');

  assert.match(runbook, /^# MMMail v2\.1\.2 Deployment Runbook/m);
  assertContainsEvery(runbook, deploymentEnvVars, 'docs/deployment-runbook.md');
  assertContainsEvery(runbook, devSeedEnvVars, 'docs/deployment-runbook.md');
});

test('v2.1.2 WebSocket load runbook requires Prometheus credentials to outlive the test', async () => {
  const runbook = await readText('docs/deployment-runbook.md');

  assert.match(runbook, /管理员 token 有效期必须覆盖完整 `WS_DURATION_MS`/);
  assert.match(runbook, /`MMMAIL_JWT_EXPIRE_MINUTES=60`/);
});
