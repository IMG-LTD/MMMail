<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'

const props = defineProps<{
  hasSelection: boolean
  activeCellLabel: string
  rawValue: string
  computedValue: string
  previewHint: string
  dirty: boolean
  saving: boolean
  readonly: boolean
  formulaCellCount: number
  computedErrorCount: number
}>()

const emit = defineEmits<{
  update: [value: string]
}>()

const { t } = useI18n()

const statusCards = computed(() => {
  return [
    { label: t('sheets.formula.metrics.formulaCells'), value: String(props.formulaCellCount) },
    { label: t('sheets.formula.metrics.computedErrors'), value: String(props.computedErrorCount) }
  ]
})
</script>

<template>
  <section class="formula-shell mm-card">
    <header class="formula-header">
      <div>
        <p>{{ t('sheets.formula.eyebrow') }}</p>
        <h2>{{ t('sheets.formula.title') }}</h2>
      </div>
      <span class="cell-pill">{{ hasSelection ? activeCellLabel : t('sheets.formula.noCell') }}</span>
    </header>

    <div class="formula-layout">
      <div class="formula-input-shell">
        <label class="formula-label" for="sheets-formula-input">{{ t('sheets.formula.barLabel') }}</label>
        <el-input
          id="sheets-formula-input"
          :model-value="rawValue"
          :disabled="!hasSelection || saving || readonly"
          :placeholder="t('sheets.formula.placeholder')"
          @update:model-value="emit('update', String($event ?? ''))"
        />
        <p class="formula-help">{{ previewHint }}</p>
      </div>

      <div class="preview-card" :class="{ 'preview-card--dirty': dirty }">
        <span class="preview-label">{{ t('sheets.formula.previewLabel') }}</span>
        <strong>{{ hasSelection ? (computedValue || t('sheets.formula.blank')) : '—' }}</strong>
        <small>{{ dirty ? t('sheets.formula.dirtyHint') : t('sheets.formula.savedHint') }}</small>
      </div>

      <div class="status-grid">
        <article v-for="item in statusCards" :key="item.label" class="status-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.formula-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
  background:
    radial-gradient(circle at top right, rgba(201, 232, 229, 0.6), transparent 28%),
    linear-gradient(180deg, rgba(250, 253, 253, 0.98), rgba(243, 249, 248, 0.96));
}

.formula-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.formula-header p,
.formula-header h2,
.formula-help {
  margin: 0;
}

.formula-header p {
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--mm-muted);
}

.formula-header h2 {
  margin-top: 6px;
  font-size: 22px;
}

.cell-pill {
  padding: 9px 12px;
  border-radius: 999px;
  background: rgba(15, 110, 110, 0.08);
  color: var(--mm-primary-dark);
  font-size: 12px;
  font-weight: 600;
}

.formula-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(220px, 0.9fr) minmax(220px, 0.9fr);
  gap: 14px;
  align-items: stretch;
}

.formula-input-shell,
.preview-card,
.status-card {
  border-radius: 22px;
  border: 1px solid rgba(15, 110, 110, 0.1);
  background: rgba(255, 255, 255, 0.88);
}

.formula-input-shell {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.formula-label,
.preview-label,
.status-card span {
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--mm-muted);
}

.formula-help {
  color: var(--mm-muted);
  line-height: 1.6;
}

.preview-card {
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 8px;
}

.preview-card strong {
  font-size: 28px;
  color: var(--mm-primary-dark);
}

.preview-card small {
  color: var(--mm-muted);
}

.preview-card--dirty {
  border-color: rgba(154, 103, 0, 0.28);
  background: rgba(255, 249, 235, 0.92);
}

.status-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.status-card {
  min-height: 112px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.status-card strong {
  font-size: 28px;
}

@media (max-width: 1180px) {
  .formula-layout {
    grid-template-columns: 1fr;
  }

  .status-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .formula-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .status-grid {
    grid-template-columns: 1fr;
  }
}
</style>
