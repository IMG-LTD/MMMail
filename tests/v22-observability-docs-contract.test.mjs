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

test('v2.2 SLI/SLO document exists and is discoverable', async () => {
  assert.equal(exists('docs/observability/sli-slo.md'), true);
  assert.equal(exists('docs/observability/opentelemetry.md'), true);

  const [readme, validateLocal] = await Promise.all([
    read('README.md'),
    read('scripts/validate-local.sh')
  ]);

  assert.match(readme, /docs\/observability\/sli-slo\.md/);
  assert.match(readme, /docs\/observability\/opentelemetry\.md/);
  assert.match(validateLocal, /docs\/observability\/sli-slo\.md/);
  assert.match(validateLocal, /docs\/observability\/opentelemetry\.md/);
});

test('v2.2 SLI/SLO document covers required internal signals without public SLA', async () => {
  const doc = await read('docs/observability/sli-slo.md');

  for (const term of [
    'API p99',
    '5xx rate',
    'billing webhook success rate',
    'license verification failure rate',
    'OIDC callback failure rate'
  ]) {
    assert.match(doc, new RegExp(term, 'i'), `missing ${term}`);
  }

  assert.match(doc, /internal target/i);
  assert.match(doc, /not a public SLA/i);
  assert.match(doc, /not a contractual commitment/i);
  assert.doesNotMatch(doc, /24\/7 SLA|guaranteed uptime|guaranteed response/i);
});

test('v2.2 OpenTelemetry document covers runtime spans and explicit OIDC boundary', async () => {
  const doc = await read('docs/observability/opentelemetry.md');

  for (const term of [
    'MMMAIL_OTEL_ENABLED=false',
    'OTEL_EXPORTER_OTLP_ENDPOINT',
    'mmmail.http.request',
    'mmmail.db.operation',
    'mmmail.redis.operation',
    'mmmail.billing.webhook',
    'mmmail.license.verify',
    'mmmail.oidc.callback',
    'BackendV22OpenTelemetryContractTest'
  ]) {
    assert.match(doc, new RegExp(term.replaceAll('.', '\\.'), 'i'), `missing ${term}`);
  }

  assert.match(doc, /OIDC callback/i);
  assert.match(doc, /BUS-01/i);
  assert.match(doc, /OBS-01/i);
  assert.match(doc, /live Keycloak or approved OIDC IdP run/i);
  assert.match(doc, /correlated callback trace evidence/i);
  assert.match(doc, /not a public SLA/i);
  assert.match(doc, /not a contractual commitment/i);
  assert.doesNotMatch(doc, /future OIDC callback path/);
  assert.doesNotMatch(doc, /OIDC callback 专项 span remains pending/);
  assert.doesNotMatch(doc, /24\/7 SLA|guaranteed uptime|guaranteed response/i);
});

test('v2.2 live OIDC evidence template covers trace and gate evidence without proxy completion', async () => {
  const template = await read('docs/commercial/oidc-live-evidence-template.md');

  for (const term of [
    'Backend commit SHA',
    'Provider version',
    'Registered callback URL',
    'Callback success',
    'Token refresh',
    'Logout',
    'Callback error path',
    'mmmail.oidc.callback',
    'CI or release-gate run URL',
    'Local OIDC unit tests',
    'Mock IdP responses'
  ]) {
    assert.match(template, new RegExp(term.replaceAll('.', '\\.'), 'i'), `missing ${term}`);
  }

  assert.match(template, /not a substitute for a real Keycloak or approved OIDC IdP run/i);
  assert.match(template, /Trace evidence must be tied to the same backend commit/i);
});
