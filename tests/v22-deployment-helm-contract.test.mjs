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

test('v2.2 Helm chart exposes the required deployment files', () => {
  for (const path of [
    'helm/mmmail/Chart.yaml',
    'helm/mmmail/values.yaml',
    'helm/mmmail/templates/_helpers.tpl',
    'helm/mmmail/templates/backend-configmap.yaml',
    'helm/mmmail/templates/backend-deployment.yaml',
    'helm/mmmail/templates/backend-secret.yaml',
    'helm/mmmail/templates/backend-service.yaml',
    'helm/mmmail/templates/frontend-deployment.yaml',
    'helm/mmmail/templates/frontend-service.yaml',
    'helm/mmmail/templates/ingress.yaml',
    'docs/ops/helm.md',
    'scripts/validate-helm-chart.sh'
  ]) {
    assert.equal(exists(path), true, `${path} must exist`);
  }
});

test('v2.2 Helm values cover commercial and enterprise self-hosting knobs', async () => {
  const values = await read('helm/mmmail/values.yaml');

  for (const term of [
    'frontend-admin',
    'license',
    'billing',
    'oidc',
    'auditExport',
    'otel',
    'externalDatabase',
    'externalRedis',
    'existingSecret',
    'secretKeys'
  ]) {
    assert.match(values, new RegExp(term), `values.yaml must include ${term}`);
  }

  assert.doesNotMatch(values, /frontend-v2/);
  assert.doesNotMatch(values, /replace-with-/);
  assert.doesNotMatch(values, /MMMAIL_BILLING_WEBHOOK_SECRET:\s*['"][^'"]+/);
  assert.doesNotMatch(values, /MMMAIL_LICENSE_PUBLIC_KEY:\s*['"][^'"]+/);
});

test('v2.2 Helm templates publish backend and frontend-admin only', async () => {
  const templatePaths = [
    'helm/mmmail/templates/backend-configmap.yaml',
    'helm/mmmail/templates/backend-deployment.yaml',
    'helm/mmmail/templates/backend-secret.yaml',
    'helm/mmmail/templates/backend-service.yaml',
    'helm/mmmail/templates/frontend-deployment.yaml',
    'helm/mmmail/templates/frontend-service.yaml',
    'helm/mmmail/templates/ingress.yaml'
  ];
  const templates = (await Promise.all(templatePaths.map(read))).join('\n');

  assert.match(templates, /MMMAIL_BILLING_PROVIDER/);
  assert.match(templates, /MMMAIL_LICENSE_PUBLIC_KEY/);
  assert.match(templates, /MMMAIL_OIDC_ENABLED/);
  assert.match(templates, /MMMAIL_AUDIT_EXPORT_ENABLED/);
  assert.match(templates, /OTEL_EXPORTER_OTLP_ENDPOINT/);
  assert.match(templates, /secretKeyRef/);
  assert.match(templates, /frontend-admin/);
  assert.doesNotMatch(templates, /frontend-v2/);
});

test('v2.2 Helm validation is wired into local, release, CI, and spec gates', async () => {
  const [script, validateLocal, releaseGate, ciWorkflow, spec] = await Promise.all([
    read('scripts/validate-helm-chart.sh'),
    read('scripts/validate-local.sh'),
    read('scripts/release-gate.sh'),
    read('.github/workflows/ci.yml'),
    read('docs/v22-open-source-commercial-spec.md')
  ]);

  assert.match(script, /helm lint/);
  assert.match(script, /helm template/);
  assert.match(script, /frontend-v2/);
  assert.match(validateLocal, /validate-helm-chart\.sh/);
  assert.match(releaseGate, /step_helm_lint/);
  assert.match(releaseGate, /helm-lint/);
  assert.match(ciWorkflow, /Setup Helm/);
  assert.match(spec, /DEP-01/);
  assert.match(spec, /Helm chart/);
});
