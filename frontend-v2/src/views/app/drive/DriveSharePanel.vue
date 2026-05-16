<script setup lang="ts">
import { NButton } from "naive-ui";
import { computed, ref, watch } from "vue";
import Modal from "@/design-system/components/Modal.vue";
import StatusBadge from "@/design-system/components/StatusBadge.vue";
import type { DriveItem, DriveShareLink } from "@/service/api/drive";

const props = defineProps<{
  item: DriveItem | null;
  shares: DriveShareLink[];
  show: boolean;
}>();

const emit = defineEmits<{
  "update:show": [value: boolean];
}>();

const copied = ref(false);
const confirmingRevoke = ref(false);
const retryCount = ref(0);
const revoked = ref(false);

const shareTitle = computed(() => props.item?.name || "No file selected");
const publicShare = computed(
  () => props.shares.find((share) => share.status !== "REVOKED") || null,
);
const policyStatus = computed(() => {
  if (!props.item) {
    return "Select a file to load its share policy from the Drive API.";
  }
  if (!props.shares.length) {
    return "No active share link is loaded for this file.";
  }
  if (revoked.value) {
    return "Local revoke confirmation is pending backend sync.";
  }
  return "Share policy loaded from the current Drive runtime state.";
});
const retryCopy = computed(() => {
  return retryCount.value ? `Retry requested ${retryCount.value} time(s).` : "Retry share sync";
});
const publicLinkCopy = computed(() => {
  if (!publicShare.value) {
    return "No public link loaded";
  }
  return `Token ${publicShare.value.token.slice(0, 8)}...`;
});

function copyLink() {
  copied.value = Boolean(publicShare.value);
}

function retrySync() {
  retryCount.value += 1;
  copied.value = false;
}

function requestRevoke() {
  confirmingRevoke.value = true;
}

function confirmRevoke() {
  revoked.value = true;
  confirmingRevoke.value = false;
}

watch(
  () => props.show,
  (value) => {
    if (!value) {
      copied.value = false;
      confirmingRevoke.value = false;
      revoked.value = false;
    }
  },
);
</script>

<template>
  <Modal
    :show="show"
    size="lg"
    title="Share settings"
    description="Review file sharing, public link posture, and local action state."
    close-label="Close share settings"
    @update:show="emit('update:show', $event)"
  >
    <section class="drive-share-panel">
      <header class="drive-share-panel__summary">
        <div>
          <span class="section-label">Selected file</span>
          <h3 class="drive-share-panel__title">{{ shareTitle }}</h3>
          <p>{{ policyStatus }}</p>
        </div>
        <StatusBadge class="drive-share-panel__sensitivity" label="Internal" tone="warning" />
      </header>

      <div class="drive-share-panel__members">
        <div class="drive-share-panel__member">
          <strong>Owner</strong>
          <span>alex@mmmail.local</span>
        </div>
        <div class="drive-share-panel__member">
          <strong>Editor</strong>
          <span>design-ops@mmmail.local</span>
        </div>
        <div class="drive-share-panel__member">
          <strong>Viewer</strong>
          <span>review@mmmail.local</span>
        </div>
      </div>

      <section class="drive-share-panel__public-link">
        <div>
          <span class="section-label">Public link</span>
          <strong>{{ publicLinkCopy }}</strong>
          <p>
            {{
              publicShare?.expiresAt ? `Expires ${publicShare.expiresAt}` : "No expiration loaded"
            }}
          </p>
        </div>
        <NButton native-type="button" @click="copyLink">Copy link</NButton>
      </section>

      <p class="drive-share-panel__error" role="alert">
        {{
          copied
            ? "Link copied locally. Backend share state is unchanged."
            : "Share changes are not persisted until the Drive API confirms them."
        }}
      </p>

      <div class="drive-share-panel__actions">
        <NButton class="drive-share-panel__retry" native-type="button" @click="retrySync">
          {{ retryCopy }}
        </NButton>
        <NButton class="drive-share-panel__revoke" native-type="button" @click="requestRevoke">
          Revoke public link
        </NButton>
      </div>

      <section v-if="confirmingRevoke" class="drive-share-panel__confirm">
        <strong>Confirm revoke</strong>
        <p>This only marks the local UI state until a backend revoke endpoint is wired.</p>
        <NButton native-type="button" @click="confirmRevoke">Confirm local revoke state</NButton>
      </section>
    </section>
  </Modal>
</template>

<style scoped>
.drive-share-panel {
  display: grid;
  gap: 16px;
}

.drive-share-panel__summary,
.drive-share-panel__public-link,
.drive-share-panel__actions,
.drive-share-panel__member {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.drive-share-panel__summary h3 {
  margin: 6px 0;
  font-size: 18px;
  letter-spacing: 0;
}

.drive-share-panel__summary p,
.drive-share-panel__public-link p,
.drive-share-panel__confirm p,
.drive-share-panel__error {
  margin: 0;
  color: var(--mm-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.drive-share-panel__members {
  display: grid;
  gap: 8px;
}

.drive-share-panel__member,
.drive-share-panel__public-link,
.drive-share-panel__confirm,
.drive-share-panel__error {
  padding: 12px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface-soft);
}

.drive-share-panel__member span {
  color: var(--mm-text-secondary);
}

.drive-share-panel button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface);
  color: var(--mm-text-primary);
  font-weight: 700;
}

.drive-share-panel__revoke {
  border-color: var(--mm-danger) !important;
  color: var(--mm-danger) !important;
}
</style>
