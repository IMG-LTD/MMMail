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

test('v2.2 image publishing workflow exists for tags and manual dry runs', async () => {
  assert.equal(exists('.github/workflows/images.yml'), true);
  const workflow = await read('.github/workflows/images.yml');

  assert.match(workflow, /name: MMMail Images/);
  assert.match(workflow, /push:[\s\S]+tags:[\s\S]+v\*/);
  assert.match(workflow, /workflow_dispatch:/);
  assert.match(workflow, /packages: write/);
  assert.match(workflow, /docker\/setup-qemu-action@v4/);
  assert.match(workflow, /docker\/setup-buildx-action@v4/);
  assert.match(workflow, /docker\/login-action@v4/);
  assert.match(workflow, /docker\/metadata-action@v6/);
  assert.match(workflow, /docker\/build-push-action@v7/);
  assert.match(workflow, /linux\/amd64,linux\/arm64/);
  assert.match(workflow, /startsWith\(github\.ref, 'refs\/tags\/'\)/);
});

test('v2.2 image matrix publishes backend and frontend-admin only', async () => {
  const workflow = await read('.github/workflows/images.yml');

  assert.match(workflow, /backend\/Dockerfile/);
  assert.match(workflow, /frontend-admin\/Dockerfile/);
  assert.match(workflow, /mmmail-backend/);
  assert.match(workflow, /mmmail-frontend-admin/);
  assert.doesNotMatch(workflow, /frontend-v2/);
  assert.doesNotMatch(workflow, /mmmail-frontend-v2/);
});

test('v2.2 image digests are part of release notes and release gates', async () => {
  const [template, evidenceTemplate, releaseGate, validateLocal, spec] = await Promise.all([
    read('docs/release/release-notes-template.md'),
    read('docs/release/image-digest-evidence-template.md'),
    read('scripts/release-gate.sh'),
    read('scripts/validate-local.sh'),
    read('docs/v22-open-source-commercial-spec.md')
  ]);

  assert.match(template, /Image Digests/);
  assert.match(template, /mmmail-backend/);
  assert.match(template, /mmmail-frontend-admin/);
  assert.match(evidenceTemplate, /not a substitute for a real tag-triggered image publishing workflow run/);
  assert.match(evidenceTemplate, /GitHub workflow run URL/);
  assert.match(evidenceTemplate, /Backend immutable digest/);
  assert.match(evidenceTemplate, /Frontend immutable digest/);
  assert.match(evidenceTemplate, /workflow_dispatch.*without tag-published image digests/s);
  assert.match(releaseGate, /step_image_workflow_contract/);
  assert.match(releaseGate, /image-workflow-contract/);
  assert.match(validateLocal, /v22-image-publishing-contract\.test\.mjs/);
  assert.match(spec, /DEP-02/);
  assert.match(spec, /image-workflow-contract/);
});
