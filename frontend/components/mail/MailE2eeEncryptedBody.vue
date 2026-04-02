<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { MailBodyE2ee } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useMailDetailE2ee } from '~/composables/useMailDetailE2ee'

const props = defineProps<{
  ciphertext: string
  metadata: MailBodyE2ee
}>()

const { t } = useI18n()
const {
  decrypting,
  decryptedBody,
  decryptError,
  passphrase,
  decryptEncryptedBody,
  resetDecryptedBody
} = useMailDetailE2ee()

async function onDecrypt(): Promise<void> {
  try {
    await decryptEncryptedBody(props.ciphertext)
    ElMessage.success(t('mailWorkspace.detail.e2ee.messages.decryptSuccess'))
  } catch {
    ElMessage.error(decryptError.value || t('mailWorkspace.detail.e2ee.messages.decryptFailed'))
  }
}
</script>

<template>
  <section class="mail-e2ee-body" data-testid="mail-detail-e2ee">
    <div class="mail-e2ee-body__meta">
      <span class="mail-e2ee-body__badge">{{ t('mailWorkspace.detail.e2ee.badge') }}</span>
      <p>{{ t('mailWorkspace.detail.e2ee.description') }}</p>
      <p>{{ t('mailWorkspace.detail.e2ee.algorithm', { value: props.metadata.algorithm || 'unknown' }) }}</p>
      <p>{{ t('mailWorkspace.detail.e2ee.fingerprintCount', { count: props.metadata.recipientFingerprints.length }) }}</p>
    </div>

    <el-alert
      v-if="decryptError"
      data-testid="mail-detail-e2ee-error"
      type="error"
      :closable="false"
      :title="decryptError"
    />

    <template v-if="decryptedBody">
      <article class="mail-e2ee-body__content" data-testid="mail-detail-e2ee-plain">{{ decryptedBody }}</article>
      <el-button size="small" @click="resetDecryptedBody">{{ t('mailWorkspace.detail.e2ee.actions.clear') }}</el-button>
    </template>
    <template v-else>
      <p class="mail-e2ee-body__hint">{{ t('mailWorkspace.detail.e2ee.lockedHint') }}</p>
      <el-input
        v-model="passphrase"
        data-testid="mail-detail-e2ee-passphrase"
        type="password"
        show-password
        :placeholder="t('mailWorkspace.detail.e2ee.passphrasePlaceholder')"
      />
      <el-button
        data-testid="mail-detail-e2ee-decrypt"
        type="primary"
        :loading="decrypting"
        @click="onDecrypt"
      >
        {{ t('mailWorkspace.detail.e2ee.actions.decrypt') }}
      </el-button>
    </template>
  </section>
</template>

<style scoped>
.mail-e2ee-body {
  display: grid;
  gap: 12px;
}

.mail-e2ee-body__meta {
  display: grid;
  gap: 6px;
  color: var(--mm-muted);
}

.mail-e2ee-body__badge {
  display: inline-flex;
  width: fit-content;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(19, 126, 67, 0.12);
  color: #137e43;
  font-size: 12px;
  font-weight: 600;
}

.mail-e2ee-body__hint {
  margin: 0;
  color: var(--mm-muted);
}

.mail-e2ee-body__content {
  white-space: pre-wrap;
  line-height: 1.7;
}
</style>
