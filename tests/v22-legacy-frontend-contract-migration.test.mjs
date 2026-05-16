import test from 'node:test';
import assert from 'node:assert/strict';
import { access, readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = fileURLToPath(new URL('..', import.meta.url));

async function read(path) {
  return readFile(join(ROOT, path), 'utf8');
}

async function exists(path) {
  try {
    await access(join(ROOT, path));
    return true;
  } catch {
    return false;
  }
}

test('v2.2 legacy auth and workspace contracts are covered by frontend-admin surfaces', async () => {
  const [request, orgStore, authStore, mailApi, mailView, driveApi, driveView, passApi, passView] = await Promise.all([
    read('frontend-admin/src/service/request/index.ts'),
    read('frontend-admin/src/store/modules/org/index.ts'),
    read('frontend-admin/src/store/modules/auth/index.ts'),
    read('frontend-admin/src/service/api/mail.ts'),
    read('frontend-admin/src/views/mail/index.vue'),
    read('frontend-admin/src/service/api/drive.ts'),
    read('frontend-admin/src/views/drive/index.vue'),
    read('frontend-admin/src/service/api/pass.ts'),
    read('frontend-admin/src/views/pass/index.vue')
  ]);

  assert.match(request, /Authorization/);
  assert.match(request, /X-Org-Id/);
  assert.match(orgStore, /currentOrgId/);
  assert.match(authStore, /localStg\.set\('refreshToken'/);
  assert.match(authStore, /orgStore\.setCurrentOrgId\(payload\.currentOrgId/);
  assert.match(mailApi, /\/api\/v2\/mail\/messages/);
  assert.match(mailApi, /\/api\/v2\/mail\/threads\/\$\{messageId\}/);
  assert.match(mailApi, /\/api\/v2\/mail\/send/);
  assert.match(mailView, /listMailMessages/);
  assert.match(mailView, /readMailMessage/);
  assert.match(driveApi, /\/api\/v2\/drive\/files/);
  assert.match(driveApi, /\/api\/v2\/drive\/storage\/summary/);
  assert.match(driveApi, /\/api\/v2\/drive\/files\/\$\{fileId\}\/share/);
  assert.match(driveView, /Promise\.all\(\[/);
  assert.match(driveView, /listDriveItems/);
  assert.match(driveView, /readDriveUsage/);
  assert.match(passApi, /\/api\/v2\/pass\/items/);
  assert.match(passApi, /\/api\/v2\/pass\/vaults/);
  assert.match(passApi, /\/api\/v2\/pass\/monitor/);
  assert.match(passView, /Promise\.all\(\[/);
  assert.match(passView, /listPassItems/);
  assert.match(passView, /readPassMonitor/);
});

test('v2.2 legacy public share contracts are covered by frontend-admin public routes', async () => {
  const [routeIndex, customRoutes, publicShareRoutes, apiIndex, publicShareApi, publicShareTypes, shareView] = await Promise.all([
    read('frontend-admin/src/router/routes/index.ts'),
    read('frontend-admin/src/router/routes/custom-routes.ts'),
    read('frontend-admin/src/router/routes/public-share-routes.ts'),
    read('frontend-admin/src/service/api/index.ts'),
    read('frontend-admin/src/service/api/public-share.ts'),
    read('frontend-admin/src/typings/api/public-share.d.ts'),
    read('frontend-admin/src/views/share/index.vue')
  ]);

  assert.match(routeIndex, /customRoutes/);
  assert.match(customRoutes, /publicShareRoutes/);
  assert.match(publicShareRoutes, /public_mail_share/);
  assert.match(publicShareRoutes, /\/share\/mail\/:token/);
  assert.match(publicShareRoutes, /public_pass_share/);
  assert.match(publicShareRoutes, /\/share\/pass\/:token/);
  assert.match(publicShareRoutes, /public_drive_share/);
  assert.match(publicShareRoutes, /\/share\/drive\/:token/);
  assert.match(apiIndex, /export \* from '\.\/public-share'/);
  assert.match(publicShareApi, /readPublicMailShare\(token: string\)/);
  assert.match(publicShareApi, /downloadPublicMailAttachment\(token: string, attachmentId: string\)/);
  assert.match(publicShareApi, /readPublicPassShare\(token: string\)/);
  assert.match(publicShareTypes, /interface MailShare/);
  assert.match(publicShareTypes, /interface PassShare/);
  assert.match(shareView, /readPublicMailShare/);
  assert.match(shareView, /downloadPublicMailAttachment/);
  assert.match(shareView, /readPublicPassShare/);
  assert.match(shareView, /readPublicDriveShareMetadata/);
});

test('v2.2 legacy aggregation, settings, and command contracts are covered by frontend-admin', async () => {
  const [workspace, notifications, collaboration, settings, commandApi, commandView] = await Promise.all([
    read('frontend-admin/src/service/api/workspace.ts'),
    read('frontend-admin/src/views/notifications/index.vue'),
    read('frontend-admin/src/views/collaboration/index.vue'),
    read('frontend-admin/src/views/settings/index.vue'),
    read('frontend-admin/src/service/api/command-center.ts'),
    read('frontend-admin/src/views/command-center/index.vue')
  ]);

  assert.match(workspace, /\/api\/v2\/workspace\/summary/);
  assert.match(workspace, /\/api\/v2\/workspace\/activity/);
  assert.match(workspace, /\/api\/v2\/workspace\/tasks/);
  assert.match(notifications, /listNotifications/);
  assert.match(notifications, /listNotificationSubscriptions/);
  assert.doesNotMatch(notifications, /\/api\/v2\/workspace\/aggregation/);
  assert.match(collaboration, /listCollaborationProjects/);
  assert.match(collaboration, /listCollaborationTasks/);
  assert.match(collaboration, /readCollaborationBoard/);
  assert.match(settings, /readSystemHealth|listDeviceSessions/);
  assert.match(settings, /LicensePanel/);
  assert.match(commandApi, /\/api\/v2\/command-center\/catalog/);
  assert.match(commandApi, /\/api\/v2\/command-center\/recents/);
  assert.match(commandApi, /\/api\/v2\/command-center\/quick-search/);
  assert.match(commandView, /quickSearchCommandPanel/);
  assert.match(commandView, /listCommandPanelCatalog/);
});

test('v2.2 legacy frontend-v2 migration signal is retired after contract migration', async () => {
  const [validateLocal, ci, spec, topology, decision] = await Promise.all([
    read('scripts/validate-local.sh'),
    read('.github/workflows/ci.yml'),
    read('docs/v22-open-source-commercial-spec.md'),
    read('docs/frontend/v22-frontend-topology-audit.md'),
    read('docs/frontend/v22-frontend-convergence-decision.md')
  ]);

  assert.equal(await exists('scripts/validate-legacy-frontend-v2.sh'), false);
  assert.doesNotMatch(validateLocal, /validate-legacy-frontend-v2\.sh/);
  assert.doesNotMatch(ci, /legacy-frontend-migration/);
  assert.doesNotMatch(ci, /frontend-v2\/pnpm-lock\.yaml/);
  assert.match(spec, /FE-03 \| done \|/);
  assert.match(topology, /Selected legacy contracts have moved/);
  assert.match(decision, /selected contracts have moved/);
});
