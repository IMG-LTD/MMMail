<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useLocaleText } from "@/locales";
import {
  listPassItems,
  listPassMailboxes,
  readPassMonitor,
  type PassMailbox,
  type PassMonitorItem,
  type PassMonitorOverview,
  type PassWorkspaceItemSummary,
} from "@/service/api/pass";
import { findSurface, passSections } from "@/shared/content/route-surfaces";
import { useAuthStore } from "@/store/modules/auth";
import {
  PASS_ITEMS_FETCH_LIMIT,
  createPassMonitorIssueMap,
  derivePassSectionState,
  isPassItemShared,
} from "./pass-section-state";
import { isPremiumGateError, resolvePremiumNotice } from "@/shared/utils/premium-runtime";
import PassConfirmDialog from "./pass/PassConfirmDialog.vue";
import PassItemDetail from "./pass/PassItemDetail.vue";
import PassItemList from "./pass/PassItemList.vue";
import PassRiskMonitorPanel from "./pass/PassRiskMonitorPanel.vue";
import PassShareSettingsModal from "./pass/PassShareSettingsModal.vue";
import PassVaultRail from "./pass/PassVaultRail.vue";
import type { PassConfirmAction, PassDetailCard, PassSurfaceEntry } from "./pass/pass-types";
import {
  compareItemsForSecureLinks,
  compareItemsForVault,
  compareMailboxes,
  compareMonitorItems,
  createAvatar,
  formatDateTime,
  isSharedItem,
  joinText,
  resolveMailboxStatus,
  resolveMonitorFlags,
  resolvePassItemType,
  resolvePassScope,
} from "./pass/pass-view-helpers";
import "./pass-section-view.css";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { tr } = useLocaleText();
const surfaceKey = computed(() => String(route.meta.surfaceKey ?? "pass"));
const current = computed(() => findSurface(passSections, surfaceKey.value, "pass"));
const passItems = ref<PassWorkspaceItemSummary[]>([]);
const passMailboxes = ref<PassMailbox[]>([]);
const passMonitor = ref<PassMonitorOverview | null>(null);
const passLoading = ref(false);
const loadError = ref("");
const passMonitorError = ref("");
const passMonitorLocked = ref(false);
const selectedEntryKey = ref("");
const secretVisible = ref(false);
const shareSettingsOpen = ref(false);
const passActionError = ref("");
const confirmAction = ref<PassConfirmAction | null>(null);
const selectedRiskId = ref("");
let latestPassRequest = 0;

const sharedItems = computed(() =>
  passItems.value.filter(isSharedItem).slice().sort(compareItemsForVault),
);
const personalItems = computed(() =>
  passItems.value
    .filter((item) => !isPassItemShared(item))
    .slice()
    .sort(compareItemsForVault),
);
const secureLinkItems = computed(() =>
  passItems.value
    .filter((item) => item.secureLinkCount > 0)
    .slice()
    .sort(compareItemsForSecureLinks),
);
const aliasItems = computed(() =>
  passItems.value
    .filter((item) => item.itemType === "ALIAS")
    .slice()
    .sort(compareItemsForVault),
);
const monitorIssueMap = computed(() => createPassMonitorIssueMap(passMonitor.value));
const policyItems = computed(() =>
  Array.from(monitorIssueMap.value.values()).sort(compareMonitorItems),
);
const derivedState = computed(() =>
  derivePassSectionState(
    passItems.value,
    passMailboxes.value,
    passMonitor.value,
    PASS_ITEMS_FETCH_LIMIT,
  ),
);
const visibleEntries = computed<PassSurfaceEntry[]>(resolveVisibleEntries);
const selectedEntry = computed(
  () =>
    visibleEntries.value.find((entry) => entry.key === selectedEntryKey.value) ||
    visibleEntries.value[0] ||
    null,
);
const selectedItem = computed(
  () => selectedEntry.value?.item || findItemForMonitorEntry(selectedEntry.value) || null,
);
const selectedMailbox = computed(() => selectedEntry.value?.mailbox || null);
const selectedMonitorItem = computed(
  () =>
    selectedEntry.value?.monitorItem ||
    monitorIssueMap.value.get(selectedItem.value?.id || "") ||
    null,
);
const activeRisk = computed(
  () =>
    policyItems.value.find((item) => item.id === selectedRiskId.value) ||
    selectedMonitorItem.value ||
    policyItems.value[0] ||
    null,
);
const cappedStateNotice = computed(() =>
  authStore.accessToken && derivedState.value.itemCoverageCapped
    ? "Showing the first 200 items only. Summaries reflect this subset."
    : "",
);
const boardSubtitle = computed(resolveBoardSubtitle);
const emptyCopy = computed(() =>
  authStore.accessToken
    ? "No items are available for this view."
    : "Sign in to load your pass workspace.",
);
const passMonitorNotice = computed(() => {
  if (passMonitorLocked.value) return "Security monitor requires premium access.";
  return passMonitorError.value;
});
const primaryCard = computed<PassDetailCard>(() => createPrimaryCard());
const secondaryCard = computed<PassDetailCard>(() => createSecondaryCard());

async function loadPass() {
  const requestId = ++latestPassRequest;
  const requestToken = authStore.accessToken;
  const requestPath = route.fullPath;
  if (!requestToken) return resetSignedOutPass(requestId, requestToken, requestPath);
  passLoading.value = true;
  loadError.value = "";
  passMonitorError.value = "";
  passMonitorLocked.value = false;
  try {
    const coreRuntimePromise = Promise.all([
      listPassItems(requestToken, { limit: String(PASS_ITEMS_FETCH_LIMIT) }),
      listPassMailboxes(requestToken),
    ]);
    const monitorPromise = loadOptionalPassMonitor(requestToken);
    const [itemsResponse, mailboxesResponse] = await coreRuntimePromise;
    const monitorResult = await monitorPromise;
    if (!isCurrentPassRequest(requestId, requestToken, requestPath)) return;
    passItems.value = Array.isArray(itemsResponse.data) ? itemsResponse.data : [];
    passMailboxes.value = Array.isArray(mailboxesResponse.data) ? mailboxesResponse.data : [];
    passMonitor.value = monitorResult.monitor;
    passMonitorError.value = monitorResult.notice;
    passMonitorLocked.value = monitorResult.locked;
  } catch (error) {
    if (isCurrentPassRequest(requestId, requestToken, requestPath))
      resetPassAfterError(resolveErrorMessage(error));
  } finally {
    if (isCurrentPassRequest(requestId, requestToken, requestPath)) passLoading.value = false;
  }
}

function isCurrentPassRequest(requestId: number, requestToken: string, requestPath: string) {
  return (
    requestId === latestPassRequest &&
    requestToken === authStore.accessToken &&
    requestPath === route.fullPath
  );
}

function resetSignedOutPass(requestId: number, requestToken: string, requestPath: string) {
  if (!isCurrentPassRequest(requestId, requestToken, requestPath)) return;
  passItems.value = [];
  passMailboxes.value = [];
  passMonitor.value = null;
  passMonitorError.value = "";
  passMonitorLocked.value = false;
  selectedEntryKey.value = "";
  loadError.value = "";
  passLoading.value = false;
}

function resetPassAfterError(message: string) {
  passItems.value = [];
  passMailboxes.value = [];
  passMonitor.value = null;
  passMonitorError.value = "";
  passMonitorLocked.value = false;
  selectedEntryKey.value = "";
  loadError.value = message;
}

async function loadOptionalPassMonitor(token: string) {
  try {
    const response = await readPassMonitor(token);
    return { locked: false, monitor: response.data || null, notice: "" };
  } catch (error) {
    return {
      locked: isPremiumGateError(error),
      monitor: null,
      notice: resolvePremiumNotice(error, "Security monitor requires premium access."),
    };
  }
}

function openSection(key: string) {
  const paths: Record<string, string> = {
    pass: "/pass",
    "pass-alias-center": "/pass/alias-center",
    "pass-business-policy": "/pass/business-policy",
    "pass-mailbox": "/pass/mailbox",
    "pass-monitor": "/pass/monitor",
    "pass-secure-links": "/pass/secure-links",
    "pass-shared-library": "/pass/shared-library",
  };
  void router.push(paths[key] ?? "/pass");
}

function resolveVisibleEntries() {
  if (surfaceKey.value === "pass-mailbox")
    return passMailboxes.value.slice().sort(compareMailboxes).map(createMailboxEntry);
  if (surfaceKey.value === "pass-shared-library")
    return sharedItems.value.map((item) => createItemEntry(item, surfaceKey.value));
  if (surfaceKey.value === "pass-secure-links")
    return secureLinkItems.value.map((item) => createItemEntry(item, surfaceKey.value));
  if (surfaceKey.value === "pass-alias-center")
    return aliasItems.value.map((item) => createItemEntry(item, surfaceKey.value));
  if (surfaceKey.value === "pass-business-policy" || surfaceKey.value === "pass-monitor")
    return policyItems.value.map(createMonitorEntry);
  return personalItems.value.map((item) => createItemEntry(item, surfaceKey.value));
}

function createItemEntry(item: PassWorkspaceItemSummary, key: string): PassSurfaceEntry {
  const title = item.title || "Untitled item";
  return {
    avatar: createAvatar(title),
    badge:
      key === "pass-secure-links"
        ? `${item.secureLinkCount} links`
        : item.favorite
          ? "Starred"
          : resolvePassItemType(item.itemType),
    id: item.id,
    item,
    key: `item:${item.id}`,
    meta: joinText([
      resolvePassScope(item.scopeType),
      item.sharedVaultId ? "Shared vault" : null,
      formatDateTime(item.updatedAt),
    ]),
    subtitle: item.username || item.website || resolvePassItemType(item.itemType),
    title,
  };
}

function createMailboxEntry(mailbox: PassMailbox): PassSurfaceEntry {
  return {
    avatar: createAvatar(mailbox.mailboxEmail),
    badge: mailbox.defaultMailbox
      ? "Default"
      : mailbox.primaryMailbox
        ? "Primary"
        : resolveMailboxStatus(mailbox),
    id: mailbox.id,
    key: `mailbox:${mailbox.id}`,
    mailbox,
    meta: joinText([
      mailbox.defaultMailbox ? "Default routing" : null,
      mailbox.primaryMailbox ? "Primary mailbox" : null,
      formatDateTime(mailbox.verifiedAt || mailbox.updatedAt),
    ]),
    subtitle: resolveMailboxStatus(mailbox),
    title: mailbox.mailboxEmail,
  };
}

function createMonitorEntry(item: PassMonitorItem): PassSurfaceEntry {
  const title = item.title || "Untitled item";
  return {
    avatar: createAvatar(title),
    badge: `${resolveMonitorFlags(item) || "Tracked"}`,
    id: item.id,
    key: `policy:${item.id}`,
    meta: joinText([resolveMonitorFlags(item), formatDateTime(item.updatedAt)]),
    monitorItem: item,
    subtitle: item.website || item.username || resolvePassItemType(item.itemType),
    title,
  };
}

function findItemForMonitorEntry(entry: PassSurfaceEntry | null) {
  return entry?.monitorItem
    ? passItems.value.find((item) => item.id === entry.monitorItem?.id) || null
    : null;
}

function createPrimaryCard() {
  if (!authStore.accessToken)
    return createCard(
      "Authenticated runtime",
      "Waiting for sign-in",
      "Sign in to load pass items, mailboxes, and monitor summary.",
    );
  if (surfaceKey.value === "pass-mailbox") return createMailboxCard(selectedMailbox.value);
  if (surfaceKey.value === "pass-business-policy" || surfaceKey.value === "pass-monitor")
    return createPolicyCard(selectedMonitorItem.value);
  return createItemCard(selectedItem.value, selectedMonitorItem.value);
}

function createSecondaryCard() {
  if (passMonitorNotice.value) {
    return createCard(
      "Monitor summary",
      passMonitorLocked.value ? "Premium locked" : "Unavailable",
      passMonitorNotice.value,
    );
  }
  return createCard(
    "Monitor summary",
    `${derivedState.value.trackedItemCount} / ${passMonitor.value?.totalItemCount || 0}`,
    "Items, mailboxes, and monitor state come from authenticated runtime APIs.",
    [
      { label: "Weak passwords", value: `${derivedState.value.weakPasswordCount}` },
      { label: "Reused passwords", value: `${derivedState.value.reusedPasswordCount}` },
      { label: "2FA inactive", value: `${derivedState.value.inactiveTwoFactorCount}` },
      { label: "Secure links", value: `${derivedState.value.totalSecureLinkCount}` },
    ],
  );
}

function createItemCard(
  item: PassWorkspaceItemSummary | null,
  monitorItem: PassMonitorItem | null,
) {
  if (!item)
    return createCard(
      "Selected item",
      "No item selected",
      "Select an item from the list to inspect authenticated detail.",
    );
  return createCard(
    "Selected item",
    item.title || "Untitled item",
    joinText([item.website, item.username, monitorItem ? resolveMonitorFlags(monitorItem) : null]),
    [
      { label: "Type", value: resolvePassItemType(item.itemType) },
      { label: "Scope", value: resolvePassScope(item.scopeType) },
      { label: "Secure links", value: `${item.secureLinkCount}` },
      { label: "Updated", value: formatDateTime(item.updatedAt) },
    ],
  );
}

function createMailboxCard(mailbox: PassMailbox | null) {
  if (!mailbox)
    return createCard(
      "Selected mailbox",
      "No mailbox selected",
      "Select a mailbox to inspect verification state.",
    );
  return createCard("Selected mailbox", mailbox.mailboxEmail, resolveMailboxStatus(mailbox), [
    { label: "Default", value: mailbox.defaultMailbox ? "Yes" : "No" },
    { label: "Primary", value: mailbox.primaryMailbox ? "Yes" : "No" },
    { label: "Updated", value: formatDateTime(mailbox.updatedAt) },
  ]);
}

function createPolicyCard(item: PassMonitorItem | null) {
  if (!item)
    return createCard(
      "Policy focus",
      "No item selected",
      "Select a monitored item to inspect risk state.",
    );
  return createCard("Policy focus", item.title || "Untitled item", resolveMonitorFlags(item), [
    { label: "Weak", value: item.weakPassword ? "Yes" : "No" },
    { label: "Reused", value: item.reusedPassword ? `${item.reusedGroupSize || 2} items` : "No" },
    { label: "2FA", value: item.inactiveTwoFactor ? "Inactive" : "Active" },
  ]);
}

function createCard(
  label: string,
  title: string,
  copy: string,
  facts: PassDetailCard["facts"] = [],
): PassDetailCard {
  return { copy, facts, label, title };
}

function resolveBoardSubtitle() {
  if (!authStore.accessToken)
    return "Sign in to switch this pass surface to authenticated runtime data.";
  if (loadError.value) return loadError.value;
  if (passLoading.value && !passItems.value.length)
    return "Loading items, mailboxes, and monitor summary.";
  if (passMonitorNotice.value)
    return `${visibleEntries.value.length} entries loaded · ${passMonitorNotice.value}`;
  return `${visibleEntries.value.length} entries loaded · ${derivedState.value.trackedItemCount} monitored`;
}

function selectEntry(entryKey: string) {
  selectedEntryKey.value = entryKey;
  secretVisible.value = false;
}

function saveShareSettings() {
  passActionError.value = "Share settings require the next backend persistence endpoint.";
}

function confirmHighRiskAction() {
  passActionError.value = `${confirmAction.value || "Action"} requires backend confirmation before it can be completed.`;
}

function resolveErrorMessage(error: unknown) {
  return error instanceof Error && error.message
    ? error.message
    : "Failed to load pass data. Please try again later.";
}

watch(
  () => [route.fullPath, authStore.accessToken],
  () => {
    void loadPass();
  },
  { immediate: true },
);

watch(
  visibleEntries,
  (entries) => {
    if (!entries.some((entry) => entry.key === selectedEntryKey.value))
      selectedEntryKey.value = entries[0]?.key || "";
    selectedRiskId.value = policyItems.value[0]?.id || selectedRiskId.value;
  },
  { immediate: true },
);
</script>

<template>
  <section class="pass-surface pass-workbench">
    <PassVaultRail :active-key="current.key" :sections="passSections" @open="openSection" />
    <PassItemList
      :empty-copy="emptyCopy"
      :entries="visibleEntries"
      :selected-key="selectedEntryKey"
      :status-copy="`${boardSubtitle} ${cappedStateNotice}`"
      :title="tr(current.label)"
      @select="selectEntry"
    />
    <PassItemDetail
      :primary-card="primaryCard"
      :revealed="secretVisible"
      :secondary-card="secondaryCard"
      :selected-entry="selectedEntry"
      @confirm-revoke="confirmAction = 'revoke'"
      @confirm-rotate="confirmAction = 'rotate'"
      @open-share="shareSettingsOpen = true"
      @toggle-secret="secretVisible = !secretVisible"
    />
    <PassRiskMonitorPanel
      :action-error="passActionError"
      :active-risk="activeRisk"
      :items="policyItems"
      @open-risk="selectedRiskId = $event"
      @retry="passActionError = 'Risk remediation requires the next backend endpoint.'"
    />
    <PassShareSettingsModal
      :action-error="passActionError"
      :open="shareSettingsOpen"
      @close="shareSettingsOpen = false"
      @retry="saveShareSettings"
      @save="saveShareSettings"
    />
    <PassConfirmDialog
      :action="confirmAction"
      :action-error="passActionError"
      @cancel="confirmAction = null"
      @confirm="confirmHighRiskAction"
      @retry="confirmHighRiskAction"
    />
  </section>
</template>
