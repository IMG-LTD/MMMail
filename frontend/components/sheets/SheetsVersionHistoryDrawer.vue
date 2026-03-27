<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { SheetsWorkbookDetail, SheetsWorkbookVersion } from '~/types/sheets'
import { formatSheetsTime } from '~/utils/sheets'
import { resolveSheetsVersionSourceI18nKey } from '~/utils/sheets-sharing-version'

const props = defineProps<{
  modelValue: boolean
  workbook: SheetsWorkbookDetail | null
  items: SheetsWorkbookVersion[]
  loading: boolean
  mutationId: string
  canRestore: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  restore: [versionId: string]
}>()

const { t } = useI18n()

const drawerVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

function isCurrentVersion(item: SheetsWorkbookVersion): boolean {
  return item.versionNo === props.workbook?.currentVersion
}
</script>

<template>
  <el-drawer v-model="drawerVisible" :title="t('sheets.versions.title')" size="760px">
    <div class="versions-panel">
      <div v-if="workbook" class="versions-hero mm-card">
        <div>
          <p>{{ t('sheets.versions.description') }}</p>
          <h3>{{ workbook.title }}</h3>
        </div>
        <el-tag effect="dark">v{{ workbook.currentVersion }}</el-tag>
      </div>

      <el-empty v-if="!loading && items.length === 0" :description="t('sheets.versions.empty')" :image-size="72" />

      <div v-else class="versions-list" v-loading="loading">
        <article v-for="item in items" :key="item.versionId" class="version-item">
          <div class="version-item__header">
            <div>
              <p>{{ t(resolveSheetsVersionSourceI18nKey(item.sourceEvent)) }}</p>
              <h4>{{ t('sheets.versions.version', { value: item.versionNo }) }}</h4>
            </div>
            <div class="version-item__tags">
              <el-tag size="small" effect="plain">{{ t('sheets.versions.dimensions', { rows: item.rowCount, cols: item.colCount }) }}</el-tag>
              <el-tag v-if="isCurrentVersion(item)" type="success" size="small" effect="plain">
                {{ t('sheets.versions.current') }}
              </el-tag>
            </div>
          </div>

          <div class="version-item__meta">
            <span>{{ t('sheets.versions.createdBy', { value: item.createdByDisplayName || item.createdByEmail }) }}</span>
            <span>{{ t('sheets.versions.createdAt', { value: formatSheetsTime(item.createdAt) }) }}</span>
          </div>

          <div class="version-item__actions">
            <el-button
              type="warning"
              plain
              :disabled="!canRestore || isCurrentVersion(item)"
              :loading="mutationId === item.versionId"
              @click="emit('restore', item.versionId)"
            >
              {{ t('sheets.versions.restore') }}
            </el-button>
          </div>
        </article>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
.versions-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.versions-hero,
.version-item__header,
.version-item__meta,
.version-item__actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.versions-hero {
  align-items: center;
  padding: 18px;
  background:
    radial-gradient(circle at top right, rgba(214, 174, 84, 0.16), transparent 26%),
    linear-gradient(135deg, rgba(10, 43, 64, 0.96), rgba(8, 32, 53, 0.98));
  color: #f7fbff;
}

.versions-hero p,
.versions-hero h3,
.version-item__header p,
.version-item__header h4 {
  margin: 0;
}

.versions-hero p {
  color: rgba(236, 245, 255, 0.82);
}

.versions-list {
  display: grid;
  gap: 12px;
  min-height: 120px;
}

.version-item {
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(15, 110, 110, 0.08);
  background: rgba(255, 255, 255, 0.9);
}

.version-item__header p,
.version-item__meta span {
  color: var(--mm-muted);
  font-size: 13px;
}

.version-item__header h4 {
  margin-top: 6px;
  font-size: 20px;
}

.version-item__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 720px) {
  .versions-hero,
  .version-item__header,
  .version-item__meta,
  .version-item__actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
