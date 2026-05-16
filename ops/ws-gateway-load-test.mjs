import { setTimeout as delay } from 'node:timers/promises';

const DEFAULT_CONNECTIONS = 1000;
const DEFAULT_DURATION_MS = 30 * 60 * 1000;
const DEFAULT_HEARTBEAT_MS = 30 * 1000;
const DEFAULT_OPEN_BATCH = 50;
const DEFAULT_OPEN_TIMEOUT_MS = 10 * 1000;
const DEFAULT_CPU_MAX_PERCENT = 30;
const DEFAULT_MEMORY_MAX_BYTES = 1024 * 1024 * 1024;

if (typeof WebSocket === 'undefined') {
  throw new Error('Node.js WebSocket global is required. Run with Node 22 or newer.');
}

const config = readConfig();
const stats = await runLoadTest(config);
const resourceStats = await assertPrometheusThresholds(config);
printSummary({ ...stats, ...resourceStats });

async function runLoadTest(config) {
  const stats = createStats();
  const clients = [];
  const pendingSockets = [];
  let shuttingDown = false;
  let heartbeat;

  try {
    for (let start = 0; start < config.connections; start += config.openBatch) {
      const size = Math.min(config.openBatch, config.connections - start);
      const opened = await Promise.all(
        Array.from({ length: size }, (_, offset) =>
          openClient(start + offset, config, stats, () => shuttingDown, pendingSockets)
        )
      );
      clients.push(...opened);
    }

    heartbeat = setInterval(() => sendHeartbeats(clients, config, stats), config.heartbeatMs);
    await delay(config.durationMs);
  } finally {
    shuttingDown = true;
    if (heartbeat) clearInterval(heartbeat);
    closeClients([...new Set([...clients, ...pendingSockets])]);
    await delay(config.closeGraceMs);
  }

  assertStats(stats, config);
  return stats;
}

function readConfig() {
  return {
    baseUrl: normalizeWsBase(requiredEnv('WS_BASE_URL')),
    channel: process.env.WS_CHANNEL || 'notifications',
    closeGraceMs: readPositiveInt('WS_CLOSE_GRACE_MS', 5000),
    connections: readPositiveInt('WS_CONNECTIONS', DEFAULT_CONNECTIONS),
    cpuMaxPercent: readPositiveNumber('WS_CPU_MAX_PERCENT', DEFAULT_CPU_MAX_PERCENT),
    durationMs: readPositiveInt('WS_DURATION_MS', DEFAULT_DURATION_MS),
    heartbeatMs: readPositiveInt('WS_HEARTBEAT_MS', DEFAULT_HEARTBEAT_MS),
    memoryMaxBytes: readPositiveInt('WS_MEMORY_MAX_BYTES', DEFAULT_MEMORY_MAX_BYTES),
    openBatch: readPositiveInt('WS_OPEN_BATCH', DEFAULT_OPEN_BATCH),
    openTimeoutMs: readPositiveInt('WS_OPEN_TIMEOUT_MS', DEFAULT_OPEN_TIMEOUT_MS),
    prometheusBearerToken: process.env.WS_PROMETHEUS_BEARER_TOKEN || '',
    prometheusUrl: process.env.WS_PROMETHEUS_URL || '',
    since: process.env.WS_SINCE || '',
    token: requiredEnv('WS_TOKEN')
  };
}

function createStats() {
  return {
    closedUnexpected: 0,
    errors: 0,
    opened: 0,
    pings: 0,
    pongs: 0,
    throttles: 0
  };
}

function openClient(index, config, stats, isShuttingDown, pendingSockets) {
  const socket = new WebSocket(buildWsUrl(config, index));
  pendingSockets.push(socket);
  return Promise.race([
    waitForClientOpen(socket, index, stats, isShuttingDown),
    openTimeout(index, config.openTimeoutMs)
  ]);
}

function waitForClientOpen(socket, index, stats, isShuttingDown) {
  return new Promise((resolve, reject) => {
    let opened = false;
    socket.addEventListener('open', () => {
      opened = true;
      stats.opened += 1;
      resolve(socket);
    });
    socket.addEventListener('message', event => recordFrame(event.data, stats));
    socket.addEventListener('error', () => {
      stats.errors += 1;
    });
    socket.addEventListener('close', event => {
      if (!isShuttingDown()) stats.closedUnexpected += 1;
      if (!opened) reject(new Error(`WebSocket ${index} closed before open: ${event.code}`));
    });
  });
}

async function openTimeout(index, timeoutMs) {
  await delay(timeoutMs);
  throw new Error(`WebSocket ${index} did not open within ${timeoutMs}ms`);
}

function closeClients(clients) {
  for (const client of clients) {
    if (client.readyState === WebSocket.CLOSED || client.readyState === WebSocket.CLOSING) continue;
    client.close(1000, 'load-test-complete');
  }
}

function buildWsUrl(config, index) {
  const base = new URL('/ws/notifications', config.baseUrl);
  base.searchParams.set('token', config.token);
  base.searchParams.set('loadClient', String(index));
  if (config.since) base.searchParams.set('since', config.since);
  return base.toString();
}

function sendHeartbeats(clients, config, stats) {
  for (const client of clients) {
    if (client.readyState !== WebSocket.OPEN) continue;
    stats.pings += 1;
    client.send(JSON.stringify({ channel: config.channel, payload: {}, seq: stats.pings, type: 'ping' }));
  }
}

function recordFrame(data, stats) {
  if (typeof data !== 'string') return;
  const frame = JSON.parse(data);
  if (frame.type === 'pong') stats.pongs += 1;
  if (frame.type === 'throttle') stats.throttles += 1;
}

async function assertPrometheusThresholds(config) {
  if (!config.prometheusUrl) return {};
  const response = await fetch(config.prometheusUrl, { headers: prometheusHeaders(config) });
  if (!response.ok) throw new Error(`Prometheus scrape failed: HTTP ${response.status}`);
  const body = await response.text();
  const cpuPercent = requiredMetricSum(body, 'process_cpu_usage') * 100;
  const memoryBytes = requiredMetricSum(body, 'jvm_memory_used_bytes');
  if (cpuPercent > config.cpuMaxPercent) throw new Error(`CPU ${cpuPercent}% exceeds ${config.cpuMaxPercent}%`);
  if (memoryBytes > config.memoryMaxBytes) throw new Error(`Memory ${memoryBytes} exceeds ${config.memoryMaxBytes}`);
  return { cpuPercent, memoryBytes };
}

function prometheusHeaders(config) {
  return config.prometheusBearerToken ? { Authorization: `Bearer ${config.prometheusBearerToken}` } : {};
}

function assertStats(stats, config) {
  if (stats.opened !== config.connections) throw new Error(`Opened ${stats.opened}/${config.connections} WS clients`);
  if (stats.errors > 0) throw new Error(`WebSocket errors observed: ${stats.errors}`);
  if (stats.throttles > 0) throw new Error(`Unexpected throttle frames observed: ${stats.throttles}`);
  if (stats.closedUnexpected > 0) throw new Error(`Unexpected closes observed: ${stats.closedUnexpected}`);
  if (stats.pings > 0 && stats.pongs === 0) throw new Error('Heartbeat ping sent but no pong received');
  if (stats.pongs !== stats.pings) throw new Error(`Heartbeat mismatch: ${stats.pongs}/${stats.pings} pongs`);
}

function printSummary(stats) {
  console.log(JSON.stringify(stats, null, 2));
}

function requiredEnv(name) {
  const value = process.env[name];
  if (!value) throw new Error(`${name} is required`);
  return value;
}

function normalizeWsBase(value) {
  const url = new URL(value);
  if (url.protocol === 'http:') url.protocol = 'ws:';
  if (url.protocol === 'https:') url.protocol = 'wss:';
  if (!['ws:', 'wss:'].includes(url.protocol)) throw new Error('WS_BASE_URL must use ws, wss, http, or https');
  return url.toString();
}

function readPositiveInt(name, defaultValue) {
  const value = process.env[name] || String(defaultValue);
  const parsed = Number.parseInt(value, 10);
  if (!Number.isSafeInteger(parsed) || parsed <= 0) throw new Error(`${name} must be a positive integer`);
  return parsed;
}

function readPositiveNumber(name, defaultValue) {
  const value = process.env[name] || String(defaultValue);
  const parsed = Number.parseFloat(value);
  if (!Number.isFinite(parsed) || parsed <= 0) throw new Error(`${name} must be a positive number`);
  return parsed;
}

function requiredMetricSum(body, metricName) {
  const values = body.split('\n')
    .filter(line => line.startsWith(metricName))
    .map(line => Number.parseFloat(line.slice(line.lastIndexOf(' ') + 1)))
    .filter(Number.isFinite);
  if (!values.length) throw new Error(`Prometheus metric ${metricName} was not found`);
  return values.reduce((sum, value) => sum + value, 0);
}
