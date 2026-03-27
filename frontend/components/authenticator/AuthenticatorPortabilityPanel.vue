<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthenticatorApi } from '~/composables/useAuthenticatorApi'
import { useI18n } from '~/composables/useI18n'
import { downloadTextFile } from '~/utils/sheets'
import {
  buildAuthenticatorDownloadMimeType,
  isBackupPassphraseConfirmed,
  normalizeAuthenticatorImportFormat,
  normalizeAuthenticatorPortabilityContent,
  readAuthenticatorPortabilityFile,
  readAuthenticatorQrImageFile
} from '~/utils/authenticator-portability'
import type {
  AuthenticatorBackupPayload,
  AuthenticatorExportPayload,
  AuthenticatorImportFormat
} from '~/types/api'

const emit = defineEmits<{
  imported: [preferredEntryId: string]
  changed: []
}>()

const { t } = useI18n()
const {
  importEntries,
  exportEntries,
  exportEncryptedBackup,
  importEncryptedBackup,
  importQrImage
} = useAuthenticatorApi()

const importDialogVisible = ref(false)
const backupDialogVisible = ref(false)
const importFormat = ref<AuthenticatorImportFormat>('AUTO')
const importContent = ref('')
const backupPassphrase = ref('')
const backupPassphraseConfirm = ref('')
const restorePassphrase = ref('')
const restoreContent = ref('')
const importing = ref(false)
const exporting = ref(false)
const exportingBackup = ref(false)
const importingBackup = ref(false)
const importingQrImage = ref(false)
const importFileInput = ref<HTMLInputElement | null>(null)
const backupFileInput = ref<HTMLInputElement | null>(null)
const qrImageFileInput = ref<HTMLInputElement | null>(null)
const lastExport = ref<AuthenticatorExportPayload | null>(null)
const lastBackup = ref<AuthenticatorBackupPayload | null>(null)

const importFormatOptions = computed(() => [
  { label: t('authenticator.portability.import.format.auto'), value: 'AUTO' },
  { label: t('authenticator.portability.import.format.otpauth'), value: 'OTPAUTH_URI' },
  { label: t('authenticator.portability.import.format.json'), value: 'MMMAIL_JSON' }
])

async function onImportEntries(): Promise<void> {
  const content = normalizeAuthenticatorPortabilityContent(importContent.value)
  if (!content) {
    ElMessage.warning(t('authenticator.portability.messages.importContentRequired'))
    return
  }
  importing.value = true
  try {
    const result = await importEntries({
      format: normalizeAuthenticatorImportFormat(importFormat.value),
      content
    })
    ElMessage.success(t('authenticator.portability.messages.imported', { count: result.importedCount }))
    importDialogVisible.value = false
    importContent.value = ''
    emit('imported', result.entries[0]?.id || '')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'authenticator.portability.messages.importFailed'))
  } finally {
    importing.value = false
  }
}

async function onExportEntries(): Promise<void> {
  exporting.value = true
  try {
    const snapshot = await exportEntries()
    lastExport.value = snapshot
    downloadSnapshot(snapshot.fileName, snapshot.content, 'export')
    ElMessage.success(t('authenticator.portability.messages.exported', { count: snapshot.entryCount }))
    emit('changed')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'authenticator.portability.messages.exportFailed'))
  } finally {
    exporting.value = false
  }
}

async function onCreateBackup(): Promise<void> {
  if (!isBackupPassphraseConfirmed(backupPassphrase.value, backupPassphraseConfirm.value)) {
    ElMessage.warning(t('authenticator.portability.messages.backupPassphraseMismatch'))
    return
  }
  exportingBackup.value = true
  try {
    const snapshot = await exportEncryptedBackup({ passphrase: backupPassphrase.value })
    lastBackup.value = snapshot
    downloadSnapshot(snapshot.fileName, snapshot.content, 'backup')
    ElMessage.success(t('authenticator.portability.messages.backupExported', { count: snapshot.entryCount }))
    emit('changed')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'authenticator.portability.messages.backupExportFailed'))
  } finally {
    exportingBackup.value = false
  }
}

async function onImportBackup(): Promise<void> {
  const content = normalizeAuthenticatorPortabilityContent(restoreContent.value)
  if (!content) {
    ElMessage.warning(t('authenticator.portability.messages.restoreContentRequired'))
    return
  }
  importingBackup.value = true
  try {
    const result = await importEncryptedBackup({
      content,
      passphrase: restorePassphrase.value
    })
    ElMessage.success(t('authenticator.portability.messages.backupImported', { count: result.importedCount }))
    backupDialogVisible.value = false
    restoreContent.value = ''
    emit('imported', result.entries[0]?.id || '')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'authenticator.portability.messages.backupImportFailed'))
  } finally {
    importingBackup.value = false
  }
}

function openImportDialog(): void {
  importDialogVisible.value = true
}

function openBackupDialog(): void {
  backupDialogVisible.value = true
}

function triggerImportFile(): void {
  importFileInput.value?.click()
}

function triggerBackupFile(): void {
  backupFileInput.value?.click()
}

function triggerQrImageFile(): void {
  qrImageFileInput.value?.click()
}

async function onImportFileChange(event: Event): Promise<void> {
  const file = readSelectedFile(event)
  if (!file) {
    return
  }
  importContent.value = await readAuthenticatorPortabilityFile(file)
  ElMessage.success(t('authenticator.portability.messages.fileLoaded'))
}

async function onQrImageFileChange(event: Event): Promise<void> {
  const file = readSelectedFile(event)
  if (!file) {
    return
  }
  importingQrImage.value = true
  try {
    const dataUrl = await readAuthenticatorQrImageFile(file)
    const result = await importQrImage(dataUrl)
    ElMessage.success(t('authenticator.portability.messages.qrImageImported', { count: result.importedCount }))
    emit('imported', result.entries[0]?.id || '')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'authenticator.portability.messages.qrImageImportFailed'))
  } finally {
    importingQrImage.value = false
  }
}

async function onBackupFileChange(event: Event): Promise<void> {
  const file = readSelectedFile(event)
  if (!file) {
    return
  }
  restoreContent.value = await readAuthenticatorPortabilityFile(file)
  ElMessage.success(t('authenticator.portability.messages.fileLoaded'))
}

function readSelectedFile(event: Event): File | null {
  const input = event.target as HTMLInputElement | null
  const file = input?.files?.[0] || null
  if (input) {
    input.value = ''
  }
  return file
}

function reuseLastBackup(): void {
  restoreContent.value = lastBackup.value?.content || ''
}

function downloadLastExport(): void {
  if (!lastExport.value) {
    return
  }
  downloadSnapshot(lastExport.value.fileName, lastExport.value.content, 'export')
}

function downloadLastBackup(): void {
  if (!lastBackup.value) {
    return
  }
  downloadSnapshot(lastBackup.value.fileName, lastBackup.value.content, 'backup')
}

function downloadSnapshot(fileName: string, content: string, kind: 'export' | 'backup'): void {
  downloadTextFile(content, fileName, buildAuthenticatorDownloadMimeType(kind))
}

function resolveErrorMessage(error: unknown, fallbackKey: string): string {
  return error instanceof Error && error.message ? error.message : t(fallbackKey)
}
</script>

<template>
  <article class="mm-card portability-shell">
    <div class="portability-head">
      <div>
        <h2 class="mm-section-title">{{ t('authenticator.portability.title') }}</h2>
        <p class="mm-muted">{{ t('authenticator.portability.description') }}</p>
      </div>
      <div class="portability-actions">
        <input
          ref="qrImageFileInput"
          accept="image/*"
          type="file"
          class="hidden-input"
          @change="onQrImageFileChange"
        />
        <el-button plain @click="openImportDialog">{{ t('authenticator.portability.actions.import') }}</el-button>
        <el-button plain :loading="importingQrImage" @click="triggerQrImageFile">
          {{ t('authenticator.portability.actions.importQrImage') }}
        </el-button>
        <el-button :loading="exporting" @click="onExportEntries">{{ t('authenticator.portability.actions.export') }}</el-button>
        <el-button type="primary" plain @click="openBackupDialog">{{ t('authenticator.portability.actions.backup') }}</el-button>
      </div>
    </div>

    <div class="portability-note">{{ t('authenticator.portability.supportedFormats') }}</div>

    <div class="preview-grid">
      <article class="preview-card">
        <div class="preview-head">
          <div>
            <div class="preview-label">{{ t('authenticator.portability.preview.exportTitle') }}</div>
            <div class="preview-meta">
              {{ lastExport ? t('authenticator.portability.preview.entryCount', { count: lastExport.entryCount }) : t('authenticator.portability.preview.empty') }}
            </div>
          </div>
          <el-button plain :disabled="!lastExport" @click="downloadLastExport">
            {{ t('authenticator.portability.actions.downloadAgain') }}
          </el-button>
        </div>
        <el-input
          :model-value="lastExport?.content || ''"
          type="textarea"
          :rows="6"
          readonly
          :placeholder="t('authenticator.portability.preview.exportPlaceholder')"
        />
      </article>

      <article class="preview-card">
        <div class="preview-head">
          <div>
            <div class="preview-label">{{ t('authenticator.portability.preview.backupTitle') }}</div>
            <div class="preview-meta">
              {{ lastBackup ? `${lastBackup.encryption} · ${t('authenticator.portability.preview.entryCount', { count: lastBackup.entryCount })}` : t('authenticator.portability.preview.empty') }}
            </div>
          </div>
          <el-button plain :disabled="!lastBackup" @click="downloadLastBackup">
            {{ t('authenticator.portability.actions.downloadAgain') }}
          </el-button>
        </div>
        <el-input
          :model-value="lastBackup?.content || ''"
          type="textarea"
          :rows="6"
          readonly
          :placeholder="t('authenticator.portability.preview.backupPlaceholder')"
        />
      </article>
    </div>

    <el-dialog v-model="importDialogVisible" :title="t('authenticator.portability.import.title')" width="680px">
      <div class="dialog-grid">
        <el-select v-model="importFormat">
          <el-option v-for="item in importFormatOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <div class="dialog-inline">
          <input ref="importFileInput" type="file" class="hidden-input" @change="onImportFileChange" />
          <el-button plain @click="triggerImportFile">{{ t('authenticator.portability.actions.loadFile') }}</el-button>
          <span class="mm-muted">{{ t('authenticator.portability.import.hint') }}</span>
        </div>
        <el-input
          v-model="importContent"
          type="textarea"
          :rows="10"
          :placeholder="t('authenticator.portability.import.placeholder')"
        />
      </div>
      <template #footer>
        <el-button @click="importDialogVisible = false">{{ t('authenticator.messages.cancelButton') }}</el-button>
        <el-button type="primary" :loading="importing" @click="onImportEntries">
          {{ t('authenticator.portability.actions.importNow') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="backupDialogVisible" :title="t('authenticator.portability.backup.title')" width="720px">
      <div class="dialog-grid">
        <div class="dialog-section">
          <div class="preview-label">{{ t('authenticator.portability.backup.exportTitle') }}</div>
          <p class="mm-muted">{{ t('authenticator.portability.backup.exportHint') }}</p>
          <el-input v-model="backupPassphrase" show-password :placeholder="t('authenticator.portability.backup.passphrase')" />
          <el-input v-model="backupPassphraseConfirm" show-password :placeholder="t('authenticator.portability.backup.passphraseConfirm')" />
          <el-button type="primary" :loading="exportingBackup" @click="onCreateBackup">
            {{ t('authenticator.portability.actions.exportBackup') }}
          </el-button>
        </div>

        <div class="dialog-section">
          <div class="preview-label">{{ t('authenticator.portability.backup.restoreTitle') }}</div>
          <p class="mm-muted">{{ t('authenticator.portability.backup.restoreHint') }}</p>
          <div class="dialog-inline">
            <input ref="backupFileInput" type="file" class="hidden-input" @change="onBackupFileChange" />
            <el-button plain @click="triggerBackupFile">{{ t('authenticator.portability.actions.loadFile') }}</el-button>
            <el-button plain :disabled="!lastBackup" @click="reuseLastBackup">
              {{ t('authenticator.portability.actions.useLastBackup') }}
            </el-button>
          </div>
          <el-input v-model="restorePassphrase" show-password :placeholder="t('authenticator.portability.backup.restorePassphrase')" />
          <el-input
            v-model="restoreContent"
            type="textarea"
            :rows="8"
            :placeholder="t('authenticator.portability.backup.restorePlaceholder')"
          />
          <el-button type="primary" :loading="importingBackup" @click="onImportBackup">
            {{ t('authenticator.portability.actions.restoreBackup') }}
          </el-button>
        </div>
      </div>
    </el-dialog>
  </article>
</template>

<style scoped>
.portability-shell,
.dialog-grid,
.dialog-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.portability-shell {
  padding: 20px;
}

.portability-head,
.portability-actions,
.dialog-inline,
.preview-head {
  display: flex;
  gap: 12px;
}

.portability-head,
.preview-head {
  justify-content: space-between;
}

.portability-actions,
.dialog-inline {
  flex-wrap: wrap;
}

.portability-note,
.preview-meta,
.mm-muted {
  color: var(--mm-muted);
}

.preview-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.preview-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.preview-label {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--mm-primary-dark);
}

.hidden-input {
  display: none;
}

@media (max-width: 960px) {
  .preview-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .portability-head,
  .preview-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
