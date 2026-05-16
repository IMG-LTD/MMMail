<script setup lang="ts">
import { NButton, NInput, NRadio, NRadioGroup } from "naive-ui";
import { computed, ref, watch } from "vue";
import Modal from "@/design-system/components/Modal.vue";

type ProtectionMode = "warning" | "blocked";

const DEFAULT_RANGE = "C2:D8";

const props = defineProps<{
  selectedCellLabel: string;
  show: boolean;
}>();

const emit = defineEmits<{
  "update:show": [value: boolean];
}>();

const rangeValue = ref(DEFAULT_RANGE);
const mode = ref<ProtectionMode>("blocked");
const saveError = ref("");
const retryCount = ref(0);

const modeCopy = computed(() => {
  if (mode.value === "warning") {
    return "Editors see a warning before changing protected cells.";
  }
  return "Only whitelisted editors can change protected cells.";
});
const retryCopy = computed(() => {
  return retryCount.value ? `Retry queued ${retryCount.value} time(s)` : "Retry save";
});

function saveRule() {
  if (!rangeValue.value.trim()) {
    saveError.value = "Enter a range before saving protection.";
    return;
  }
  saveError.value = "Protected range save is blocked by an overlapping rule.";
}

function retrySave() {
  retryCount.value += 1;
  saveError.value = "Retry requested; overlap must be resolved before saving.";
}

watch(
  () => props.show,
  (value) => {
    if (!value) {
      rangeValue.value = DEFAULT_RANGE;
      mode.value = "blocked";
      saveError.value = "";
      retryCount.value = 0;
    }
  },
);
</script>

<template>
  <Modal
    :show="show"
    size="md"
    title="Protected range"
    description="Stage range protection and expose conflicts before backend persistence."
    close-label="Close protected range editor"
    @update:show="emit('update:show', $event)"
  >
    <section class="sheets-protected-range-modal">
      <header class="sheets-protected-range-modal__summary">
        <span class="section-label">Selected cell</span>
        <h3>{{ selectedCellLabel }}</h3>
        <p>{{ modeCopy }}</p>
      </header>

      <label class="sheets-protected-range-modal__field">
        <span class="section-label">Range</span>
        <NInput v-model:value="rangeValue" class="sheets-protected-range-modal__range-input" />
      </label>

      <NRadioGroup v-model:value="mode" class="sheets-protected-range-modal__mode">
        <NRadio value="warning">Warning only</NRadio>
        <NRadio value="blocked">Block edits</NRadio>
      </NRadioGroup>

      <div class="sheets-protected-range-modal__editors">
        <div>
          <strong>finance-owner@mmmail.local</strong>
          <span>Owner</span>
        </div>
        <div>
          <strong>revops-editor@mmmail.local</strong>
          <span>Allowed editor</span>
        </div>
      </div>

      <p class="sheets-protected-range-modal__conflict" role="alert">
        Range {{ rangeValue }} overlaps an existing finance lock on C2:D6.
      </p>
      <p v-if="saveError" class="sheets-protected-range-modal__error" role="alert">
        {{ saveError }}
      </p>

      <div class="sheets-protected-range-modal__actions">
        <NButton class="sheets-protected-range-modal__save" native-type="button" @click="saveRule">
          Save rule
        </NButton>
        <NButton
          class="sheets-protected-range-modal__retry"
          native-type="button"
          @click="retrySave"
        >
          {{ retryCopy }}
        </NButton>
      </div>
    </section>
  </Modal>
</template>

<style scoped>
.sheets-protected-range-modal {
  display: grid;
  gap: 14px;
}

.sheets-protected-range-modal__summary h3 {
  margin: 6px 0;
  font-size: 18px;
  letter-spacing: 0;
}

.sheets-protected-range-modal__summary p,
.sheets-protected-range-modal__conflict,
.sheets-protected-range-modal__error {
  margin: 0;
  color: var(--mm-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.sheets-protected-range-modal__field {
  display: grid;
  gap: 8px;
}

.sheets-protected-range-modal__range-input {
  min-height: 38px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface-soft);
  color: var(--mm-text-primary);
}

.sheets-protected-range-modal__mode,
.sheets-protected-range-modal__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sheets-protected-range-modal__mode label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
}

.sheets-protected-range-modal__editors {
  display: grid;
  gap: 8px;
}

.sheets-protected-range-modal__editors div,
.sheets-protected-range-modal__conflict,
.sheets-protected-range-modal__error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface-soft);
}

.sheets-protected-range-modal__editors span {
  color: var(--mm-text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.sheets-protected-range-modal__conflict {
  border-color: rgba(217, 119, 6, 0.42);
  background: rgba(217, 119, 6, 0.12);
  color: #7c2d12;
  font-weight: 700;
}

.sheets-protected-range-modal__error {
  border-color: rgba(196, 59, 59, 0.42);
  background: rgba(196, 59, 59, 0.12);
  color: #8f1d1d;
  font-weight: 700;
}

.sheets-protected-range-modal button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface);
  color: var(--mm-text-primary);
  font-weight: 700;
}

.sheets-protected-range-modal__save {
  border-color: var(--mm-accent-border) !important;
  background: var(--mm-accent-soft) !important;
  color: var(--mm-primary) !important;
}
</style>
