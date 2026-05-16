import assert from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const root = new URL('../', import.meta.url);

async function read(path) {
  return readFile(new URL(path, root), 'utf8');
}

function exists(path) {
  return existsSync(new URL(path, root));
}

test('v2.2 commercial boundary documents exist and are discoverable', async () => {
  for (const path of [
    'docs/commercial/pricing-boundaries.md',
    'docs/commercial/support-policy.md',
    'docs/commercial/trademark-policy.md',
    'docs/commercial/edition-entitlement-surface.md',
    'docs/billing/private-billing-evidence-template.md'
  ]) {
    assert.equal(exists(path), true, `${path} must exist`);
  }

  const [readme, support, validateLocal] = await Promise.all([
    read('README.md'),
    read('SUPPORT.md'),
    read('scripts/validate-local.sh')
  ]);

  assert.match(readme, /docs\/commercial\/pricing-boundaries\.md/);
  assert.match(support, /docs\/commercial\/support-policy\.md/);
  assert.match(validateLocal, /docs\/commercial\/pricing-boundaries\.md/);
  assert.match(validateLocal, /docs\/commercial\/edition-entitlement-surface\.md/);
  assert.match(validateLocal, /docs\/billing\/private-billing-evidence-template\.md/);
});

test('v2.2 pricing boundary does not claim live payments or weaken Free', async () => {
  const pricing = await read('docs/commercial/pricing-boundaries.md');

  assert.match(pricing, /v2\.1\.2 GA.*Free/s);
  assert.match(pricing, /No public price is committed/);
  assert.match(pricing, /Real payment processing is not live/);
  assert.match(pricing, /license signing private keys stay outside this public repository/);
  assert.doesNotMatch(pricing, /24\/7 SLA|guaranteed uptime|payment is live/i);
});

test('v2.2 edition entitlement surface separates paid gates from upgrade paths and webhooks', async () => {
  const surface = await read('docs/commercial/edition-entitlement-surface.md');

  assert.match(surface, /Business `oidc\.sso`/);
  assert.match(surface, /Business `audit\.export`/);
  assert.match(surface, /Business `dsr\.requests`/);
  assert.match(surface, /Authenticated upgrade path/);
  assert.match(surface, /External billing gateway callback/);
  assert.match(surface, /must not require an already-paid entitlement/);
  assert.match(surface, /No endpoint in the public repository may grant paid success/);
});

test('v2.2 commercial support and trademark policies avoid false SLA or endorsement', async () => {
  const [supportPolicy, trademarkPolicy] = await Promise.all([
    read('docs/commercial/support-policy.md'),
    read('docs/commercial/trademark-policy.md')
  ]);

  assert.match(supportPolicy, /best effort/i);
  assert.match(supportPolicy, /not SLA commitments/i);
  assert.match(supportPolicy, /SECURITY\.md/);
  assert.match(trademarkPolicy, /no endorsement/i);
  assert.match(trademarkPolicy, /do not present a fork.*official MMMail/s);
  assert.doesNotMatch(`${supportPolicy}\n${trademarkPolicy}`, /guaranteed response|24\/7 SLA/i);
});

test('v2.2 private billing evidence template requires real provider and signing evidence', async () => {
  const template = await read('docs/billing/private-billing-evidence-template.md');

  for (const term of [
    'Billing repository commit SHA',
    'Payment provider',
    'Webhook delivery',
    'Customer portal',
    'Invoice / refund',
    'License signing',
    'Idempotency',
    'none` provider events',
    'Mock paid subscription state'
  ]) {
    assert.match(template, new RegExp(term.replaceAll('.', '\\.'), 'i'), `missing ${term}`);
  }

  assert.match(template, /not a substitute for a real private billing repository/i);
  assert.match(template, /private signing key/i);
});
