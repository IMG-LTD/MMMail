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

test('v2.2 audit export backend and documentation surfaces exist', async () => {
  for (const path of [
    'backend/mmmail-server/src/main/java/com/mmmail/server/controller/OrgAuditExportController.java',
    'backend/mmmail-server/src/test/java/com/mmmail/server/BackendV22AuditExportContractTest.java',
    'docs/compliance/audit-export.md'
  ]) {
    assert.equal(exists(path), true, `${path} must exist`);
  }

  const [readme, validateLocal] = await Promise.all([
    read('README.md'),
    read('scripts/validate-local.sh')
  ]);
  assert.match(readme, /docs\/compliance\/audit-export\.md/);
  assert.match(validateLocal, /docs\/compliance\/audit-export\.md/);
});

test('v2.2 audit export is JSONL and guarded by Business audit export entitlement', async () => {
  const [controller, service, gate, doc] = await Promise.all([
    read('backend/mmmail-server/src/main/java/com/mmmail/server/controller/OrgAuditExportController.java'),
    read('backend/mmmail-server/src/main/java/com/mmmail/server/service/OrgAuditQueryService.java'),
    read('backend/mmmail-server/src/main/java/com/mmmail/server/security/CommercialAuthorizationGate.java'),
    read('docs/compliance/audit-export.md')
  ]);

  assert.match(controller, /\/api\/v2\/orgs/);
  assert.match(controller, /FeatureCode\.AUDIT_EXPORT/);
  assert.match(controller, /application\/x-ndjson/);
  assert.match(service, /schemaVersion/);
  assert.match(service, /mmmail\.audit\.v1/);
  assert.match(service, /listByOrgForExport/);
  assert.match(gate, /enforceFeature/);
  assert.match(doc, /Required feature: `audit\.export`/);
  assert.match(doc, /one audit event per line/i);
});

test('v2.2 audit export gate is wired into local, CI, and spec', async () => {
  const [validateLocal, ciWorkflow, spec] = await Promise.all([
    read('scripts/validate-local.sh'),
    read('.github/workflows/ci.yml'),
    read('docs/v22-open-source-commercial-spec.md')
  ]);

  for (const content of [validateLocal, ciWorkflow, spec]) {
    assert.match(content, /BackendV22AuditExportContractTest/);
  }
  assert.match(spec, /BUS-02[\s\S]+done/);
  assert.match(spec, /audit-export-smoke[\s\S]+BackendV22AuditExportContractTest/);
});
