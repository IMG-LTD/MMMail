import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const root = new URL('../', import.meta.url);

async function read(path) {
  return readFile(new URL(path, root), 'utf8');
}

function assertLockfileOmits(lockfile, packageName, version) {
  const escapedName = packageName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  assert.doesNotMatch(
    lockfile,
    new RegExp(`^ {2}${escapedName}@${version}:`, 'm')
  );
}

test('v2.2 governance requires Dependabot alert remediation before merge', async () => {
  const [agents, contributing, pullRequestTemplate] = await Promise.all([
    read('AGENTS.md'),
    read('CONTRIBUTING.md'),
    read('.github/pull_request_template.md')
  ]);
  const governance = `${agents}\n${contributing}\n${pullRequestTemplate}`;

  assert.match(governance, /Dependabot 告警/);
  assert.match(governance, /gh api repos\/IMG-LTD\/MMMail\/dependabot\/alerts\?state=open/);
  assert.match(governance, /read:packages/);
  assert.match(governance, /供应链安全/);
});

test('v2.2 frontend lockfile keeps known vulnerable npm versions out', async () => {
  const [packageJson, lockfile] = await Promise.all([
    read('frontend-admin/package.json'),
    read('frontend-admin/pnpm-lock.yaml')
  ]);
  const parsedPackage = JSON.parse(packageJson);

  assert.equal(parsedPackage.pnpm.overrides['braces@<3.0.3'], '3.0.3');
  assert.equal(parsedPackage.pnpm.overrides['glob@<10.5.0'], '10.5.0');
  assert.equal(parsedPackage.pnpm.overrides['postcss@<8.5.10'], '8.5.14');
  assert.equal(parsedPackage.pnpm.overrides['postcss-prefix-selector@<2.1.1'], '2.1.1');
  assertLockfileOmits(lockfile, 'braces', '2.3.2');
  assertLockfileOmits(lockfile, 'glob', '10.4.5');
  assertLockfileOmits(lockfile, 'postcss', '5.2.18');
  assertLockfileOmits(lockfile, 'postcss-prefix-selector', '1.16.1');
});

test('v2.2 backend dependency baseline pins patched Bouncy Castle', async () => {
  const pom = await read('backend/mmmail-server/pom.xml');

  assert.match(pom, /<artifactId>bcprov-jdk18on<\/artifactId>\s*<version>1\.84<\/version>/);
});
