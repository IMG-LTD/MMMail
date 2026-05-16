import { readdir, readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { gzipSync } from 'node:zlib';

const KB = 1024;
const assetsDir = new URL('../dist/assets/', import.meta.url);
const assetsPath = fileURLToPath(assetsDir);

const routeBudgets = [
  { name: 'workspace-first-screen', limitKb: 500, patterns: [/^home-.*\.js$/, /^index-.*\.css$/] },
  { name: 'wallet', limitKb: 200, patterns: [/^wallet-.*\.js$/] },
  { name: 'vpn', limitKb: 150, patterns: [/^vpn-.*\.js$/] },
  { name: 'meet', limitKb: 350, patterns: [/^meet-.*\.js$/] },
  { name: 'docs', limitKb: 350, patterns: [/^docs-.*\.js$/] },
  { name: 'sheets', limitKb: 250, patterns: [/^sheets-.*\.js$/] }
];

const otherModuleBudgets = [
  /^admin-.*\.js$/,
  /^authenticator-.*\.js$/,
  /^calendar-.*\.js$/,
  /^collaboration-.*\.js$/,
  /^command-center-.*\.js$/,
  /^community-.*\.js$/,
  /^contacts-.*\.js$/,
  /^drive-.*\.js$/,
  /^mail-.*\.js$/,
  /^notes-.*\.js$/,
  /^notifications-.*\.js$/,
  /^pass-.*\.js$/,
  /^search-.*\.js$/,
  /^settings-.*\.js$/,
  /^share-.*\.js$/,
  /^simplelogin-.*\.js$/
];

const fileNames = await readdir(assetsDir);
const bundleGzipSizes = await readGzipSizes(fileNames);
const routeResults = routeBudgets.map(budget => evaluateBudget(budget, bundleGzipSizes));
const moduleResults = otherModuleBudgets.map(pattern => evaluateSingleModule(pattern, bundleGzipSizes));
const failed = [...routeResults, ...moduleResults].filter(result => result.status === 'fail');

for (const result of [...routeResults, ...moduleResults]) {
  console.log(`${result.status.toUpperCase()} ${result.name}: ${formatKb(result.bytes)} / ${result.limitKb} KB`);
}

if (failed.length) {
  throw new Error(`Bundle budget exceeded: ${failed.map(result => result.name).join(', ')}`);
}

async function readGzipSizes(names) {
  const entries = await Promise.all(
    names.map(async fileName => {
      const content = await readFile(join(assetsPath, fileName));
      return [fileName, gzipSync(content).length];
    })
  );

  return new Map(entries);
}

function evaluateBudget(budget, sizes) {
  const matching = [...sizes].filter(([fileName]) => budget.patterns.some(pattern => pattern.test(fileName)));
  if (!matching.length) {
    throw new Error(`No bundle files matched budget ${budget.name}`);
  }
  const bytes = matching.reduce((sum, [, size]) => sum + size, 0);
  return toResult(budget.name, bytes, budget.limitKb);
}

function evaluateSingleModule(pattern, sizes) {
  const matching = [...sizes].filter(([fileName]) => pattern.test(fileName));
  if (!matching.length) {
    throw new Error(`No bundle files matched ${pattern}`);
  }
  const name = pattern.source.replace('^', '').replace('-.*\\.js$', '');
  return toResult(name, matching[0][1], 200);
}

function toResult(name, bytes, limitKb) {
  return {
    bytes,
    limitKb,
    name,
    status: bytes <= limitKb * KB ? 'pass' : 'fail'
  };
}

function formatKb(bytes) {
  return `${(bytes / KB).toFixed(1)} KB`;
}
