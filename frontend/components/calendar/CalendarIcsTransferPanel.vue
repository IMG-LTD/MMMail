<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

const props = defineProps<{
  content: string
  errorMessage: string
  importing: boolean
  resultSummary: string
}>()

const emit = defineEmits<{
  (event: 'export'): void
  (event: 'import'): void
  (event: 'retry'): void
  (event: 'update:content', value: string): void
}>()

const { t } = useI18n()

async function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement | null
  const file = input?.files?.[0]
  if (!file) {
    return
  }
  emit('update:content', await file.text())
  input.value = ''
}
</script>

<template>
  <section class="mm-card ics-panel">
    <div class="ics-panel__head">
      <div>
        <h3 class="mm-section-subtitle">{{ t('calendar.ics.title') }}</h3>
        <p class="ics-panel__copy">{{ t('calendar.ics.description') }}</p>
      </div>
      <div class="ics-panel__actions">
        <el-button data-testid="calendar-ics-export" @click="emit('export')">
          {{ t('calendar.actions.exportIcs') }}
        </el-button>
        <el-button
          data-testid="calendar-ics-import"
          type="primary"
          :loading="importing"
          @click="emit('import')"
        >
          {{ t('calendar.actions.importIcs') }}
        </el-button>
      </div>
    </div>

    <input
      data-testid="calendar-ics-file"
      type="file"
      accept=".ics,text/calendar"
      @change="onFileChange"
    >
    <el-input
      :model-value="content"
      data-testid="calendar-ics-textarea"
      type="textarea"
      :rows="6"
      :placeholder="t('calendar.ics.placeholder')"
      @update:model-value="emit('update:content', $event)"
    />

    <p v-if="resultSummary" class="ics-panel__result" data-testid="calendar-ics-result">{{ resultSummary }}</p>
    <div v-if="errorMessage" class="ics-panel__error" data-testid="calendar-ics-error">
      <el-alert :title="errorMessage" type="error" :closable="false" show-icon />
      <el-button data-testid="calendar-ics-retry" type="primary" text @click="emit('retry')">
        {{ t('calendar.actions.retry') }}
      </el-button>
    </div>
  </section>
</template>

<style scoped>
.ics-panel {
  display: grid;
  gap: 12px;
  padding: 18px;
}

.ics-panel__head,
.ics-panel__actions,
.ics-panel__error {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

.ics-panel__copy,
.ics-panel__result {
  margin: 6px 0 0;
  color: #64748b;
}
</style>
