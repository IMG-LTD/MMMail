<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { SheetsPermission, SheetsShareResponseStatus, SheetsWorkbookDetail, SheetsWorkbookShare } from '~/types/sheets'
import { formatSheetsTime } from '~/utils/sheets'

const props = defineProps<{
  workbook: SheetsWorkbookDetail | null
  shares: SheetsWorkbookShare[]
  loading: boolean
  submitting: boolean
  mutationId: string
  canManage: boolean
  inviteEmail: string
  invitePermission: Extract<SheetsPermission, 'VIEW' | 'EDIT'>
}>()

const emit = defineEmits<{
  'update:inviteEmail': [value: string]
  'update:invitePermission': [value: Extract<SheetsPermission, 'VIEW' | 'EDIT'>]
  submit: []
  updatePermission: [shareId: string, permission: Extract<SheetsPermission, 'VIEW' | 'EDIT'>]
  remove: [shareId: string]
}>()

const { t } = useI18n()

const permissionOptions = computed(() => [
  { label: t('sheets.share.permission.view'), value: 'VIEW' },
  { label: t('sheets.share.permission.edit'), value: 'EDIT' }
])

function getResponseTagType(status: SheetsShareResponseStatus): 'warning' | 'success' | 'info' {
  if (status === 'NEEDS_ACTION') {
    return 'warning'
  }
  if (status === 'ACCEPTED') {
    return 'success'
  }
  return 'info'
}

function getResponseLabel(status: SheetsShareResponseStatus): string {
  if (status === 'ACCEPTED') {
    return t('sheets.share.status.accepted')
  }
  if (status === 'DECLINED') {
    return t('sheets.share.status.declined')
  }
  return t('sheets.share.status.needsAction')
}

function onPermissionChange(shareId: string, value: string): void {
  emit('updatePermission', shareId, value as Extract<SheetsPermission, 'VIEW' | 'EDIT'>)
}

function onInvitePermissionChange(value: string): void {
  emit('update:invitePermission', value as Extract<SheetsPermission, 'VIEW' | 'EDIT'>)
}
</script>

<template>
  <section class="share-panel mm-card">
    <header class="share-panel__header">
      <div>
        <p>{{ t('sheets.share.eyebrow') }}</p>
        <h3>{{ t('sheets.share.title') }}</h3>
      </div>
      <span class="share-panel__badge">
        {{ canManage ? t('sheets.meta.collaborators', { count: workbook?.collaboratorCount || 0 }) : t('sheets.share.readonlyLabel') }}
      </span>
    </header>

    <p class="share-panel__description">
      {{ canManage ? t('sheets.share.descriptionOwner') : t('sheets.share.descriptionShared') }}
    </p>

    <div v-if="canManage" class="share-create">
      <el-input
        :model-value="inviteEmail"
        :placeholder="t('sheets.share.invitePlaceholder')"
        @update:model-value="emit('update:inviteEmail', String($event ?? ''))"
      >
        <template #prepend>{{ t('sheets.share.inviteEmail') }}</template>
      </el-input>
      <el-select
        :model-value="invitePermission"
        @update:model-value="onInvitePermissionChange(String($event ?? 'VIEW'))"
      >
        <el-option
          v-for="item in permissionOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-button type="primary" :loading="submitting" @click="emit('submit')">
        {{ t('sheets.share.sendInvite') }}
      </el-button>
    </div>

    <div v-else-if="workbook" class="share-readonly">
      <span>{{ t('sheets.share.readonlyOwner', { value: workbook.ownerDisplayName || workbook.ownerEmail }) }}</span>
      <el-tag size="small" effect="plain">{{ workbook.canEdit ? t('sheets.share.permission.edit') : t('sheets.share.permission.view') }}</el-tag>
    </div>

    <el-empty
      v-if="!loading && canManage && shares.length === 0"
      :description="t('sheets.share.empty')"
      :image-size="72"
    />

    <div v-else class="share-list" v-loading="loading">
      <article v-for="share in shares" :key="share.shareId" class="share-item">
        <div class="share-item__meta">
          <div class="share-item__title">
            <div>
              <p>{{ share.collaboratorDisplayName || share.collaboratorEmail }}</p>
              <small>{{ share.collaboratorEmail }}</small>
            </div>
            <div class="share-item__tags">
              <el-tag size="small" effect="plain">{{ share.permission === 'EDIT' ? t('sheets.share.permission.edit') : t('sheets.share.permission.view') }}</el-tag>
              <el-tag size="small" effect="plain" :type="getResponseTagType(share.responseStatus)">
                {{ getResponseLabel(share.responseStatus) }}
              </el-tag>
            </div>
          </div>
          <small>{{ t('sheets.share.updatedAt', { value: formatSheetsTime(share.updatedAt) }) }}</small>
        </div>

        <div class="share-item__actions">
          <el-select
            size="small"
            :model-value="share.permission"
            :disabled="mutationId === share.shareId"
            @update:model-value="onPermissionChange(share.shareId, String($event ?? 'VIEW'))"
          >
            <el-option
              v-for="item in permissionOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          <el-button
            type="danger"
            text
            :loading="mutationId === share.shareId"
            @click="emit('remove', share.shareId)"
          >
            {{ t('sheets.share.revoke') }}
          </el-button>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.share-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
  background:
    radial-gradient(circle at top right, rgba(15, 110, 110, 0.08), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(243, 248, 248, 0.96));
}

.share-panel__header,
.share-create,
.share-readonly,
.share-item,
.share-item__title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.share-panel__header {
  align-items: flex-start;
}

.share-panel__header p,
.share-panel__header h3,
.share-panel__description {
  margin: 0;
}

.share-panel__header p {
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--mm-muted);
}

.share-panel__header h3 {
  margin-top: 6px;
  font-size: 22px;
}

.share-panel__badge,
.share-readonly {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(15, 110, 110, 0.08);
  color: var(--mm-primary-dark);
  font-size: 12px;
}

.share-panel__description {
  color: var(--mm-muted);
  line-height: 1.7;
}

.share-create {
  align-items: center;
}

.share-create :deep(.el-input),
.share-create :deep(.el-select) {
  flex: 1;
}

.share-readonly {
  align-items: center;
  width: fit-content;
}

.share-list {
  display: grid;
  gap: 12px;
  min-height: 120px;
}

.share-item {
  align-items: center;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(15, 110, 110, 0.08);
  background: rgba(255, 255, 255, 0.82);
}

.share-item__meta,
.share-item__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.share-item__meta {
  flex: 1;
}

.share-item__title p,
.share-item__title small,
.share-item__meta > small {
  margin: 0;
}

.share-item__title p {
  font-weight: 700;
}

.share-item__title small,
.share-item__meta > small {
  color: var(--mm-muted);
}

.share-item__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.share-item__actions {
  min-width: 132px;
  align-items: flex-end;
}

@media (max-width: 900px) {
  .share-create,
  .share-item,
  .share-item__title {
    flex-direction: column;
    align-items: flex-start;
  }

  .share-item__actions {
    width: 100%;
    align-items: stretch;
  }
}
</style>
