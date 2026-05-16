import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const source = await readFile(new URL('../ops/ws-gateway-load-test.mjs', import.meta.url), 'utf8');

test('ws gateway load runner keeps pending sockets alive until open or timeout', () => {
  assert.match(source, /pendingSockets/);
  assert.match(source, /WS_OPEN_TIMEOUT_MS/);
  assert.match(source, /Promise\.race/);
});

test('ws gateway load runner reports prometheus resource samples in summary', () => {
  assert.match(source, /return \{ cpuPercent, memoryBytes \}/);
  assert.match(source, /printSummary\(\{ \.\.\.stats, \.\.\.resourceStats \}\)/);
});

test('ws gateway load runner fails when heartbeat pongs do not match sent pings', () => {
  assert.match(source, /stats\.pongs !== stats\.pings/);
  assert.match(source, /Heartbeat mismatch/);
});
