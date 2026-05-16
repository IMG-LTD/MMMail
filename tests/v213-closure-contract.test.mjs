import assert from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const root = new URL('../', import.meta.url);

async function read(path) {
  return readFile(new URL(path, root), 'utf8');
}

function countMatches(source, pattern) {
  return [...source.matchAll(pattern)].length;
}

test('v2.1.3 U-1 keeps only documented info-only NAlert usages in closure views', async () => {
  const [admin, calendar, share] = await Promise.all([
    read('frontend-admin/src/views/admin/index.vue'),
    read('frontend-admin/src/views/calendar/index.vue'),
    read('frontend-admin/src/views/share/index.vue')
  ]);
  const combined = `${admin}\n${calendar}\n${share}`;

  assert.match(admin, /import \{ ErrorState \} from '@\/components\/feedback'/);
  assert.match(admin, /<ErrorState[\s\S]+v-if="!orgStore\.currentOrgId"/);
  assert.equal(countMatches(combined, /<NAlert/g), 4);
  assert.equal(countMatches(combined, /info-only, see v213-closure-spec-v1\.1 §2\.1/g), 4);
});

test('v2.1.3 U-2 makes oxfmt a hard release gate in both frontend workspaces', async () => {
  const [releaseGate, frontendPackage] = await Promise.all([
    read('scripts/release-gate.sh'),
    read('frontend-v2/package.json')
  ]);
  const packageJson = JSON.parse(frontendPackage);

  assert.equal(packageJson.scripts.fmt, 'oxfmt');
  assert.equal(packageJson.scripts['fmt:check'], 'oxfmt --check');
  assert.match(packageJson.devDependencies.oxfmt, /\^0\.49\.0/);
  assert.doesNotMatch(releaseGate, /MMMAIL_RELEASE_GATE_FMT_STRICT|WARN: oxfmt drift|warn-only/);
  assert.match(releaseGate, /pnpm --dir frontend-v2 exec oxfmt --check/);
  assert.match(releaseGate, /pnpm --dir frontend-admin exec oxfmt --check/);
});

test('v2.1.3 U-3 turns docker-backed e2e placeholders into runnable specs', async () => {
  const specPaths = [
    'frontend-admin/e2e/v212-business-overview.spec.ts',
    'frontend-admin/e2e/v212-mail-flow.spec.ts',
    'frontend-admin/e2e/v212-wallet-flow.spec.ts',
    'frontend-admin/e2e/v212-meet-flow.spec.ts',
    'frontend-admin/e2e/v212-entitlement-paywall.spec.ts',
    'frontend-admin/e2e/v212-docs-crdt.spec.ts'
  ];
  const [playwrightConfig, dockerRunner, ...specs] = await Promise.all([
    read('frontend-admin/playwright.config.ts'),
    read('scripts/run-tests-docker.sh'),
    ...specPaths.map(read)
  ]);

  assert.match(playwrightConfig, /globalSetup:\s*'\.\/e2e\/global-setup\.ts'/);
  assert.match(playwrightConfig, /globalTeardown:\s*'\.\/e2e\/global-teardown\.ts'/);
  assert.match(dockerRunner, /--setup-only/);
  assert.match(dockerRunner, /--teardown-only/);

  specs.forEach((source, index) => {
    assert.doesNotMatch(source, /test\.skip|describe\.skip|TODO|placeholder/, specPaths[index]);
    assert.match(source, /test\(/, specPaths[index]);
  });
});

test('v2.1.3 docker backend tests use the MySQL driver with the MySQL test URL', async () => {
  const dockerRunner = await read('scripts/run-tests-docker.sh');

  assert.match(dockerRunner, /-Dspring\.datasource\.url="jdbc:mysql:\/\/127\.0\.0\.1:3306\/mmmail/);
  assert.match(dockerRunner, /-Dspring\.datasource\.driver-class-name=com\.mysql\.cj\.jdbc\.Driver/);
  assert.match(dockerRunner, /-Dspring\.flyway\.enabled=true/);
  assert.match(dockerRunner, /-Dspring\.sql\.init\.mode=never/);
  assert.match(dockerRunner, /start_infra\(\) \{[\s\S]+compose down -v --remove-orphans/);
});

test('v2.1.3 backup restore keeps stdin attached when using dockerized mysql client', async () => {
  const dbCommon = await read('scripts/lib/db-common.sh');

  assert.match(dbCommon, /docker run --rm -i --network host[\s\S]+mysql -h "\$DB_HOST"/);
});

test('v2.1.3 U-4 wires CI to docker e2e and the unified release gate', async () => {
  const workflow = await read('.github/workflows/ci.yml');

  assert.match(workflow, /docker-test-baseline:[\s\S]+bash scripts\/run-tests-docker\.sh e2e/);
  assert.match(workflow, /release-gate:[\s\S]+needs: \[frontend, backend, docker-test-baseline\]/);
  assert.match(workflow, /bash scripts\/release-gate\.sh --skip 1,5,8/);
});

test('v2.1.3 U-5 and U-6 close documentation and roadmap ownership', async () => {
  const [progress, changelog, v1Spec, v11Spec] = await Promise.all([
    read('docs/v212-progress-report.md'),
    read('CHANGELOG.md'),
    read('docs/v213-closure-spec.md'),
    read('docs/v213-closure-spec-v1.1.md')
  ]);
  const roadmapPath = new URL('docs/v214-roadmap.md', root);
  assert.ok(existsSync(roadmapPath), 'docs/v214-roadmap.md must exist');
  const roadmap = await read('docs/v214-roadmap.md');

  assert.match(progress, /## 8\. v2\.1\.3 收尾完成（2026-05-16）/);
  assert.match(progress, /T-1[\s\S]+T-7[\s\S]+U-1[\s\S]+U-6/);
  assert.match(changelog, /Closed v2\.1\.2 release-gate via v2\.1\.3 followups \(T-1\.\.T-7 \+ U-1\.\.U-6\)/);
  assert.match(v1Spec, /status: superseded-by: v213-closure-spec-v1\.1\.md/);
  assert.match(v11Spec, /status: implemented/);
  assert.match(roadmap, /18\.4\.3 Sheets\/看板 CRDT 接入/);
  assert.match(roadmap, /富文本 Tiptap/);
  assert.match(roadmap, /全仓 NEmpty\/NSpin 替换/);
});
