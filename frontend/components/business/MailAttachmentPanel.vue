<script setup lang="ts">
import { computed } from 'vue'
import type { MailAttachment } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { formatMailAttachmentSize } from '~/utils/mail-attachments'

export interface FailedMailAttachmentUpload {
  id: string
  fileName: string
  message: string
}

const props = withDefaults(defineProps<{
  attachments?: MailAttachment[]
  failedUploads?: FailedMailAttachmentUpload[]
  uploadLoading?: boolean
  readOnly?: boolean
  activeAttachmentIds?: string[]
}>(), {
  attachments: () => [],
  failedUploads: () => [],
  uploadLoading: false,
  readOnly: false,
  activeAttachmentIds: () => []
})

const emit = defineEmits<{
  upload: [{ files: File[] }]
  retry: [{ failureId: string }]
  remove: [{ attachmentId: string }]
  download: [{ attachmentId: string }]
}>()

const activeAttachmentSet = computed(() => new Set(props.activeAttachmentIds))
const { t } = useI18n()

function onFileChange(event: Event): void {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (files.length > 0) {
    emit('upload', { files })
  }
  input.value = ''
}
</script>

<template>
  <section class="attachment-panel">
    <div class="attachment-panel__header">
      <div>
        <h3 class="attachment-panel__title">{{ t('mailCompose.attachments.title') }}</h3>
        <p class="attachment-panel__hint">{{ t('mailCompose.attachments.hint') }}</p>
      </div>
      <label v-if="!props.readOnly" class="attachment-panel__upload">
        <span>{{ props.uploadLoading ? t('mailCompose.attachments.uploading') : t('mailCompose.attachments.addFiles') }}</span>
        <input
          class="attachment-panel__input"
          type="file"
          multiple
          data-testid="mail-attachment-input"
          :disabled="props.uploadLoading"
          @change="onFileChange"
        >
      </label>
    </div>

    <ul v-if="props.attachments.length > 0" class="attachment-panel__list" data-testid="mail-attachment-list">
      <li v-for="item in props.attachments" :key="item.id" class="attachment-panel__item">
        <div class="attachment-panel__meta">
          <strong>{{ item.fileName }}</strong>
          <span>{{ formatMailAttachmentSize(item.fileSize) }}</span>
        </div>
        <div class="attachment-panel__actions">
          <el-button size="small" text @click="emit('download', { attachmentId: item.id })">{{ t('mailCompose.attachments.download') }}</el-button>
          <el-button
            v-if="!props.readOnly"
            size="small"
            text
            type="danger"
            :loading="activeAttachmentSet.has(item.id)"
            @click="emit('remove', { attachmentId: item.id })"
          >
            {{ t('mailCompose.attachments.remove') }}
          </el-button>
        </div>
      </li>
    </ul>
    <el-empty
      v-else
      :description="props.readOnly ? t('mailCompose.attachments.emptyReadOnly') : t('mailCompose.attachments.empty')"
      :image-size="64"
    />

    <div v-if="props.failedUploads.length > 0" class="attachment-panel__failures" data-testid="mail-attachment-failures">
      <el-alert
        v-for="failure in props.failedUploads"
        :key="failure.id"
        type="error"
        :closable="false"
        :title="`${failure.fileName}: ${failure.message}`"
      >
        <template #default>
          <el-button size="small" @click="emit('retry', { failureId: failure.id })">{{ t('mailCompose.attachments.retry') }}</el-button>
        </template>
      </el-alert>
    </div>
  </section>
</template>

<style scoped>
.attachment-panel {
  display: grid;
  gap: 12px;
}

.attachment-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.attachment-panel__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.attachment-panel__hint {
  margin: 4px 0 0;
  color: var(--mm-muted);
  font-size: 12px;
}

.attachment-panel__upload {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 112px;
  padding: 10px 14px;
  border: 1px dashed var(--el-border-color);
  border-radius: 12px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}

.attachment-panel__input {
  display: none;
}

.attachment-panel__list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}

.attachment-panel__item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
}

.attachment-panel__meta {
  display: grid;
  gap: 2px;
  word-break: break-word;
}

.attachment-panel__meta span {
  color: var(--mm-muted);
  font-size: 12px;
}

.attachment-panel__actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.attachment-panel__failures {
  display: grid;
  gap: 8px;
}

@media (max-width: 720px) {
  .attachment-panel__header,
  .attachment-panel__item {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
