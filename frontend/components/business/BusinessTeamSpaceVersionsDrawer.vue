<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { OrgTeamSpaceFileVersion, OrgTeamSpaceItem } from '~/types/business'
import { formatBusinessBytes, formatBusinessTime } from '~/utils/business'

const props = defineProps<{
  modelValue: boolean
  targetItem: OrgTeamSpaceItem | null
  versions: OrgTeamSpaceFileVersion[]
  loadingVersions: boolean
  versionMutationId: string
  versionUploadLoading: boolean
  canWrite: boolean
  canManage: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'upload-version': [file: File]
  'restore-version': [versionId: string]
}>()
const { t } = useI18n()

const uploadInputRef = ref<HTMLInputElement | null>(null)

function triggerUpload(): void {
  uploadInputRef.value?.click()
}

function onFileChange(event: Event): void {
  const input = event.target as HTMLInputElement | null
  const file = input?.files?.[0] || null
  if (!file) {
    return
  }
  emit('upload-version', file)
  if (input) {
    input.value = ''
  }
}
</script>

<template>
  <el-drawer :model-value="modelValue" size="520px" @close="emit('update:modelValue', false)">
    <template #header>
      <div>
        <h2 class="drawer-title">{{ t('business.versions.title') }}</h2>
        <p class="drawer-subtitle">{{ targetItem?.name || t('business.versions.subtitleEmpty') }}</p>
      </div>
    </template>

    <div class="drawer-actions">
      <input ref="uploadInputRef" class="hidden-input" type="file" @change="onFileChange" />
      <el-button v-if="canWrite && targetItem" type="primary" :loading="versionUploadLoading" @click="triggerUpload">{{ t('business.versions.upload') }}</el-button>
    </div>

    <el-empty v-if="!targetItem" :description="t('business.versions.selectFile')" />
    <el-empty v-else-if="versions.length === 0 && !loadingVersions" :description="t('business.versions.empty')" />
    <div v-else class="version-list">
      <article v-for="version in versions" :key="version.id" class="version-card">
        <div>
          <div class="version-title">{{ t('business.versions.item', { number: version.versionNo }) }}</div>
          <div class="version-meta">{{ version.ownerEmail || t('business.versions.unknownOwner') }} · {{ formatBusinessTime(version.createdAt) }}</div>
        </div>
        <div class="version-side">
          <div class="version-meta">{{ formatBusinessBytes(version.sizeBytes) }}</div>
          <el-button
            v-if="canManage"
            size="small"
            link
            type="primary"
            :loading="versionMutationId === version.id"
            @click="emit('restore-version', version.id)"
          >
            {{ t('business.versions.restore') }}
          </el-button>
        </div>
      </article>
    </div>
  </el-drawer>
</template>

<style scoped>
.drawer-title {
  margin: 0;
  font-size: 22px;
}

.drawer-subtitle,
.version-meta {
  color: var(--mm-muted);
}

.drawer-actions,
.version-card,
.version-side {
  display: flex;
  gap: 10px;
}

.drawer-actions {
  margin-bottom: 14px;
}

.version-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.version-card {
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid var(--mm-border);
  background: rgba(255, 255, 255, 0.9);
}

.version-title {
  font-weight: 700;
  color: var(--mm-primary-dark);
}

.version-side {
  align-items: center;
}

.hidden-input {
  display: none;
}

@media (max-width: 768px) {
  .version-card {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
