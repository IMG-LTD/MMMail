import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const files = {
  commandCenterApi: new URL("../src/service/api/command-center.ts", import.meta.url),
  commandCenterView: new URL("../src/views/app/CommandCenterView.vue", import.meta.url),
  notificationsApi: new URL("../src/service/api/notifications.ts", import.meta.url),
  notificationsView: new URL("../src/views/app/NotificationsView.vue", import.meta.url),
  passApi: new URL("../src/service/api/pass.ts", import.meta.url),
  passMonitorView: new URL("../src/views/app/PassMonitorView.vue", import.meta.url),
};

async function readRequiredFile(fileUrl, label) {
  try {
    return await readFile(fileUrl, "utf8");
  } catch (error) {
    assert.fail(`expected ${label} to exist: ${error.message}`);
  }
}

function assertContainsAll(source, patterns) {
  for (const pattern of patterns) {
    assert.match(source, pattern);
  }
}

function functionBody(source, functionName) {
  const marker = new RegExp(`(?:async\\s+)?function\\s+${functionName}\\s*\\(`);
  const match = marker.exec(source);
  const start = match?.index ?? -1;
  assert.notEqual(start, -1, `expected ${functionName} to exist`);
  const searchOffset = start + match[0].length;
  const nextFunction = /\n(?:async\s+)?function\s+\w+/.exec(source.slice(searchOffset));
  return source.slice(start, nextFunction ? searchOffset + nextFunction.index : source.length);
}

test("v2.1 pass API and monitor boundaries use section 14 endpoints", async () => {
  const [passApi, passMonitorView] = await Promise.all([
    readRequiredFile(files.passApi, "Pass API client"),
    readRequiredFile(files.passMonitorView, "Pass monitor view"),
  ]);

  assertContainsAll(passApi, [
    /\/api\/v2\/pass\/vaults/,
    /\/api\/v2\/pass\/items/,
    /`\/api\/v2\/pass\/items\/\$\{itemId\}`/,
    /\/api\/v2\/pass\/share/,
    /\/api\/v2\/pass\/secure-links/,
    /`\/api\/v2\/pass\/secure-links\/\$\{linkId\}`/,
    /\/api\/v2\/pass\/aliases/,
    /`\/api\/v2\/pass\/aliases\/\$\{aliasId\}`/,
    /\/api\/v2\/pass\/monitor/,
    /listPassVaults/,
    /listPassItems/,
    /createPassItem/,
    /patchPassItem/,
    /sharePassItem/,
    /listPassSecureLinks/,
    /createPassSecureLink/,
    /deletePassSecureLink/,
    /listPassAliases/,
    /patchPassAlias/,
    /readPassMonitor/,
  ]);
  assert.doesNotMatch(passApi, /\/api\/v1\/pass/);

  assertContainsAll(passMonitorView, [
    /useAuthStore/,
    /readPassMonitor/,
    /latestPassMonitorRequest/,
    /watch\(/,
    /weakPasswords/,
    /reusedPasswords/,
    /inactiveTwoFactorItems/,
  ]);
  assert.doesNotMatch(passMonitorView, /const issueColumns = \[/);
});

test("v2.1 notifications API and runtime boundaries use section 14 endpoints", async () => {
  const [notificationsApi, notificationsView] = await Promise.all([
    readRequiredFile(files.notificationsApi, "Notifications API client"),
    readRequiredFile(files.notificationsView, "Notifications view"),
  ]);

  assertContainsAll(notificationsApi, [
    /\/api\/v2\/notifications/,
    /`\/api\/v2\/notifications\/\$\{notificationId\}`/,
    /\/api\/v2\/notifications\/rules/,
    /\/api\/v2\/notifications\/subscriptions/,
    /`\/api\/v2\/notifications\/subscriptions\/\$\{subscriptionId\}`/,
    /\/api\/v2\/notifications\/templates/,
    /\/api\/v2\/notifications\/send/,
    /\/api\/v2\/notifications\/analytics/,
    /listNotifications/,
    /patchNotification/,
    /listNotificationRules/,
    /createNotificationRule/,
    /listNotificationSubscriptions/,
    /patchNotificationSubscription/,
    /listNotificationTemplates/,
    /sendNotification/,
    /readNotificationAnalytics/,
  ]);

  assertContainsAll(notificationsView, [
    /useAuthStore/,
    /useScopeGuard/,
    /listNotifications/,
    /patchNotification/,
    /listNotificationRules/,
    /listNotificationSubscriptions/,
    /listNotificationTemplates/,
    /readNotificationAnalytics/,
    /latestNotificationsRequest/,
    /watch\(/,
  ]);
  assert.doesNotMatch(notificationsView, /\/api\/v2\/workspace\/aggregation/);
  assert.doesNotMatch(notificationsView, /const notifications = \[/);
});

test("v2.1 command center API and runtime boundaries use section 14 endpoints", async () => {
  const [commandCenterApi, commandCenterView] = await Promise.all([
    readRequiredFile(files.commandCenterApi, "Command Center API client"),
    readRequiredFile(files.commandCenterView, "Command Center view"),
  ]);

  assertContainsAll(commandCenterApi, [
    /\/api\/v2\/command-center\/commands/,
    /`\/api\/v2\/command-center\/commands\/\$\{commandId\}`/,
    /\/api\/v2\/command-center\/runs/,
    /`\/api\/v2\/command-center\/runs\/\$\{runId\}`/,
    /`\/api\/v2\/command-center\/runs\/\$\{runId\}\/cancel`/,
    /`\/api\/v2\/command-center\/runs\/\$\{runId\}\/retry`/,
    /\/api\/v2\/command-center\/workflows/,
    /\/api\/v2\/command-center\/audit/,
    /listCommandCenterCommands/,
    /readCommandCenterCommand/,
    /createCommandCenterRun/,
    /readCommandCenterRun/,
    /cancelCommandCenterRun/,
    /retryCommandCenterRun/,
    /listCommandCenterWorkflows/,
    /createCommandCenterWorkflow/,
    /listCommandCenterAudit/,
  ]);

  assertContainsAll(commandCenterView, [
    /useAuthStore/,
    /useAutomationRunbook/,
    /listCommandCenterCommands/,
    /createCommandCenterRun/,
    /listCommandCenterWorkflows/,
    /listCommandCenterAudit/,
    /latestCommandCenterRequest/,
    /watch\(/,
  ]);
  assert.doesNotMatch(commandCenterView, /const routes = \[/);
  assert.doesNotMatch(commandCenterView, /const history = \[/);
  assert.doesNotMatch(commandCenterView, /const feed = \[/);
});

test("v2.1 community surfaces keep core data visible when premium endpoints are gated", async () => {
  const [passView, notificationsView, commandCenterView, httpClient, premiumRuntime] =
    await Promise.all([
      readRequiredFile(
        new URL("../src/views/app/PassSectionView.vue", import.meta.url),
        "Pass section view",
      ),
      readRequiredFile(files.notificationsView, "Notifications view"),
      readRequiredFile(files.commandCenterView, "Command Center view"),
      readRequiredFile(new URL("../src/service/request/http.ts", import.meta.url), "HTTP client"),
      readRequiredFile(
        new URL("../src/shared/utils/premium-runtime.ts", import.meta.url),
        "Premium runtime utility",
      ),
    ]);

  assert.match(httpClient, /class HttpRequestError extends Error/);
  assert.match(httpClient, /isHttpRequestError/);
  assertContainsAll(premiumRuntime, [
    /isPremiumGateError/,
    /resolveOptionalRuntimeNotice/,
    /runtimeError/,
  ]);

  assertContainsAll(passView, [
    /passMonitorLocked/,
    /isPremiumGateError/,
    /loadOptionalPassMonitor/,
    /Security monitor requires premium access/,
  ]);
  const passLoadBody = functionBody(passView, "loadPass");
  assert.doesNotMatch(passLoadBody, /Promise\.all\(\[[\s\S]*?readPassMonitor/);

  assertContainsAll(commandCenterView, [
    /premiumRuntimeNotice/,
    /loadOptionalCommandCenterRuntime/,
    /Command Center automation requires premium access/,
    /resolveOptionalRuntimeNotice/,
  ]);
  const commandCenterLoadBody = functionBody(commandCenterView, "loadCommandCenter");
  assert.doesNotMatch(commandCenterLoadBody, /Promise\.all\(\[[\s\S]*?listCommandCenterWorkflows/);

  assertContainsAll(notificationsView, [
    /premiumRuntimeNotice/,
    /loadOptionalNotificationRuntime/,
    /Notification automation and analytics require premium access/,
    /resolveOptionalRuntimeNotice/,
  ]);
  const notificationsLoadBody = functionBody(notificationsView, "loadNotifications");
  assert.doesNotMatch(notificationsLoadBody, /Promise\.all\(\[[\s\S]*?listNotificationRules/);
});
