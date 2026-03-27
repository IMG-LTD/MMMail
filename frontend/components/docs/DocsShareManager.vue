<script setup lang="ts">
import { reactive } from 'vue'
import type { DocsNoteShare } from '~/types/api'
import { useI18n } from '~/composables/useI18n'

const props = defineProps<{
  shares: DocsNoteShare[]
  canManage: boolean
  sharing: boolean
  revokingShareId: string
  updatingShareId: string
}>()

const emit = defineEmits<{
  (event: 'create', payload: { collaboratorEmail: string; permission: 'VIEW' | 'EDIT' }): void
  (event: 'update-permission', payload: { shareId: string; permission: 'VIEW' | 'EDIT' }): void
  (event: 'revoke', share: DocsNoteShare): void
}>()

const { t } = useI18n()

const createForm = reactive({
  collaboratorEmail: '',
  permission: 'EDIT' as 'VIEW' | 'EDIT'
})

function submitCreate(): void {
  emit('create', {
    collaboratorEmail: createForm.collaboratorEmail.trim(),
    permission: createForm.permission
  })
}

function updatePermission(shareId: string, value: string | number | boolean): void {
  if (value === 'VIEW' || value === 'EDIT') {
    emit('update-permission', { shareId, permission: value })
  }
}

function formatTime(value: string): string {
  return value.replace('T', ' ').slice(0, 19)
}
</script>

<template>
  <section class="share-manager">
    <div class="rail-title">{{ t('docs.share.title') }}</div>
    <p class="rail-copy">{{ t('docs.share.description') }}</p>
    <div v-if="props.canManage" class="share-form">
      <el-input v-model="createForm.collaboratorEmail" :placeholder="t('docs.share.emailPlaceholder')" />
      <el-select v-model="createForm.permission">
        <el-option :label="t('docs.share.edit')" value="EDIT" />
        <el-option :label="t('docs.share.view')" value="VIEW" />
      </el-select>
      <el-button type="primary" :loading="props.sharing" @click="submitCreate">{{ t('docs.share.add') }}</el-button>
    </div>

    <div v-if="props.shares.length" class="share-list">
      <article v-for="share in props.shares" :key="share.shareId" class="share-item">
        <div class="share-item__meta">
          <div class="share-name">{{ share.collaboratorDisplayName || share.collaboratorEmail }}</div>
          <div class="share-meta">{{ share.collaboratorEmail }}</div>
          <div class="share-meta">{{ t('docs.share.columns.addedAt') }} · {{ formatTime(share.createdAt) }}</div>
        </div>
        <div class="share-item__actions">
          <el-select
            v-if="props.canManage"
            :model-value="share.permission"
            size="small"
            class="share-select"
            :disabled="props.updatingShareId === share.shareId"
            @change="updatePermission(share.shareId, $event)"
          >
            <el-option :label="t('docs.share.edit')" value="EDIT" />
            <el-option :label="t('docs.share.view')" value="VIEW" />
          </el-select>
          <el-tag v-else size="small">{{ share.permission === 'EDIT' ? t('docs.share.edit') : t('docs.share.view') }}</el-tag>
          <el-button
            v-if="props.canManage"
            type="danger"
            text
            :loading="props.revokingShareId === share.shareId"
            @click="emit('revoke', share)"
          >
            {{ t('docs.share.revoke') }}
          </el-button>
        </div>
      </article>
    </div>
    <el-empty v-else :description="t('docs.share.empty')" :image-size="70" />
  </section>
</template>

<style scoped>
.share-manager {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rail-title {
  font-size: 0.92rem;
  font-weight: 800;
}

.rail-copy {
  color: rgba(15, 23, 42, 0.68);
  font-size: 0.82rem;
  line-height: 1.5;
}

.share-form,
.share-item,
.share-item__actions {
  display: flex;
  gap: 10px;
}

.share-form {
  flex-direction: column;
}

.share-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.share-item {
  align-items: center;
  justify-content: space-between;
  border: 1px solid rgba(13, 17, 23, 0.08);
  border-radius: 18px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.7);
}

.share-item__meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.share-item__actions {
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.share-name {
  font-size: 0.95rem;
  font-weight: 700;
}

.share-meta {
  color: rgba(15, 23, 42, 0.68);
  font-size: 0.82rem;
}

.share-select {
  width: 124px;
}

@media (max-width: 640px) {
  .share-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .share-item__actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
