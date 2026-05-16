<script setup lang="ts">
import { NButton, NInput, NSelect } from "naive-ui";
import { computed, ref, watch } from "vue";
import Modal from "@/design-system/components/Modal.vue";
import type { DocsNoteDetail, DocsPermission } from "@/service/api/docs";

type LinkAccess = "restricted" | "workspace" | "public-read";

const props = defineProps<{
  note: DocsNoteDetail | null;
  show: boolean;
}>();

const emit = defineEmits<{
  "update:show": [value: boolean];
}>();

const inviteEmail = ref("");
const inviteRole = ref<DocsPermission>("VIEW");
const linkAccess = ref<LinkAccess>("restricted");
const validationError = ref("");
const retryCount = ref(0);

const inviteRoleOptions = [
  { label: "Viewer", value: "VIEW" },
  { label: "Editor", value: "EDIT" },
];

const linkAccessOptions = [
  { label: "Restricted", value: "restricted" },
  { label: "Workspace request", value: "workspace" },
  { label: "Public read", value: "public-read" },
];

const noteTitle = computed(() => props.note?.title || "Unavailable document");
const accessSummary = computed(() => {
  if (!props.note) {
    return "Document runtime detail has not loaded yet.";
  }
  return props.note.shared ? "Shared with workspace collaborators." : "Restricted to the owner.";
});
const linkAccessCopy = computed(() => {
  if (linkAccess.value === "workspace") {
    return "Workspace members can request access. Pending backend confirmation.";
  }
  if (linkAccess.value === "public-read") {
    return "Public read is staged locally and requires backend confirmation.";
  }
  return "Only invited collaborators can access this document.";
});
const retryCopy = computed(() => {
  return retryCount.value ? `Retry queued ${retryCount.value} time(s)` : "Retry invite";
});

function sendInvite() {
  if (!inviteEmail.value.trim() || !inviteEmail.value.includes("@")) {
    validationError.value = "Enter a valid email before sending an invite.";
    return;
  }
  validationError.value = "Invite send is pending backend confirmation.";
}

function retryInvite() {
  retryCount.value += 1;
  validationError.value = "Retry requested; invite remains unsent until backend confirmation.";
}

watch(
  () => props.show,
  (value) => {
    if (!value) {
      inviteEmail.value = "";
      inviteRole.value = "VIEW";
      linkAccess.value = "restricted";
      validationError.value = "";
      retryCount.value = 0;
    }
  },
);
</script>

<template>
  <Modal
    :show="show"
    size="md"
    title="Share permissions"
    description="Invite collaborators and stage link access without hiding pending backend state."
    close-label="Close share permissions"
    @update:show="emit('update:show', $event)"
  >
    <section class="docs-share-panel">
      <header class="docs-share-panel__summary">
        <span class="section-label">Document</span>
        <h3>{{ noteTitle }}</h3>
        <p>{{ accessSummary }}</p>
      </header>

      <label class="docs-share-panel__field">
        <span class="section-label">Invite</span>
        <NInput
          v-model:value="inviteEmail"
          class="docs-share-panel__invite-input"
          placeholder="name@company.com"
        />
      </label>

      <div class="docs-share-panel__controls">
        <label class="docs-share-panel__field">
          <span class="section-label">Role</span>
          <NSelect
            v-model:value="inviteRole"
            class="docs-share-panel__role-select"
            :options="inviteRoleOptions"
          />
        </label>
        <label class="docs-share-panel__field">
          <span class="section-label">Link access</span>
          <NSelect
            v-model:value="linkAccess"
            class="docs-share-panel__link-access"
            :options="linkAccessOptions"
          />
        </label>
      </div>

      <p class="docs-share-panel__link-state">{{ linkAccessCopy }}</p>

      <div class="docs-share-panel__collaborators">
        <div>
          <strong>{{ note?.ownerDisplayName || "Document owner" }}</strong>
          <span>Owner</span>
        </div>
        <div>
          <strong>content-review@mmmail.local</strong>
          <span>Viewer</span>
        </div>
        <div>
          <strong>workspace-editor@mmmail.local</strong>
          <span>Editor</span>
        </div>
      </div>

      <p v-if="validationError" class="docs-share-panel__error" role="alert">
        {{ validationError }}
      </p>

      <div class="docs-share-panel__actions">
        <NButton class="docs-share-panel__send" native-type="button" @click="sendInvite">
          Send invite
        </NButton>
        <NButton class="docs-share-panel__retry" native-type="button" @click="retryInvite">
          {{ retryCopy }}
        </NButton>
      </div>
    </section>
  </Modal>
</template>

<style scoped>
.docs-share-panel {
  display: grid;
  gap: 14px;
}

.docs-share-panel__summary h3 {
  margin: 6px 0;
  font-size: 18px;
  letter-spacing: 0;
}

.docs-share-panel__summary p,
.docs-share-panel__link-state,
.docs-share-panel__error {
  margin: 0;
  color: var(--mm-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.docs-share-panel__controls {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.docs-share-panel__field {
  display: grid;
  gap: 8px;
}

.docs-share-panel input,
.docs-share-panel select {
  width: 100%;
  min-height: 38px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface-soft);
  color: var(--mm-text-primary);
}

.docs-share-panel__collaborators {
  display: grid;
  gap: 8px;
}

.docs-share-panel__collaborators div,
.docs-share-panel__error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface-soft);
}

.docs-share-panel__collaborators span {
  color: var(--mm-text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.docs-share-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.docs-share-panel button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface);
  color: var(--mm-text-primary);
  font-weight: 700;
}

.docs-share-panel__send {
  border-color: var(--mm-accent-border) !important;
  background: var(--mm-accent-soft) !important;
  color: var(--mm-primary) !important;
}

@media (max-width: 640px) {
  .docs-share-panel__controls {
    grid-template-columns: 1fr;
  }
}
</style>
