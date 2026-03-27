<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { useMailEasySwitchApi } from '~/composables/useMailEasySwitchApi'
import type { MailEasySwitchProvider, MailEasySwitchSession } from '~/types/mail-easy-switch'
import {
  appendMailEasySwitchMessage,
  buildMailEasySwitchPayload,
  createMailEasySwitchDraft
} from '~/utils/mail-easy-switch'

const { t } = useI18n()
const loading = ref(false)
const submitting = ref(false)
const sessions = ref<MailEasySwitchSession[]>([])
const contactsFileName = ref('')
const calendarFileName = ref('')
const mailFileNames = ref<string[]>([])
const manualMailDraft = ref('')

const draft = reactive(createMailEasySwitchDraft())
const { listSessions, createSession, deleteSession } = useMailEasySwitchApi()

const providerOptions: Array<{ labelKey: string; value: MailEasySwitchProvider }> = [
  { labelKey: 'settings.easySwitch.providers.google', value: 'GOOGLE' },
  { labelKey: 'settings.easySwitch.providers.outlook', value: 'OUTLOOK' },
  { labelKey: 'settings.easySwitch.providers.yahoo', value: 'YAHOO' },
  { labelKey: 'settings.easySwitch.providers.other', value: 'OTHER' }
]

async function loadSessions(): Promise<void> {
  loading.value = true
  try {
    sessions.value = await listSessions()
  } finally {
    loading.value = false
  }
}

async function onContactsFileChange(event: Event): Promise<void> {
  const file = getFirstFile(event)
  if (!file) {
    return
  }
  draft.contactsCsv = await file.text()
  contactsFileName.value = file.name
}

async function onCalendarFileChange(event: Event): Promise<void> {
  const file = getFirstFile(event)
  if (!file) {
    return
  }
  draft.calendarIcs = await file.text()
  calendarFileName.value = file.name
}

async function onMailFilesChange(event: Event): Promise<void> {
  const files = getFiles(event)
  if (!files.length) {
    return
  }
  const contents = await Promise.all(files.map((file) => file.text()))
  contents.forEach((content) => {
    draft.mailMessages = appendMailEasySwitchMessage(draft.mailMessages, content)
  })
  mailFileNames.value = [...mailFileNames.value, ...files.map((file) => file.name)]
}

function appendManualMailDraft(): void {
  draft.mailMessages = appendMailEasySwitchMessage(draft.mailMessages, manualMailDraft.value)
  if (manualMailDraft.value.trim()) {
    mailFileNames.value = [...mailFileNames.value, t('settings.easySwitch.queue.pastedMessage')]
  }
  manualMailDraft.value = ''
}

function removeQueuedMail(index: number): void {
  draft.mailMessages = draft.mailMessages.filter((_, itemIndex) => itemIndex !== index)
  mailFileNames.value = mailFileNames.value.filter((_, itemIndex) => itemIndex !== index)
}

async function submitImport(): Promise<void> {
  try {
    const payload = buildMailEasySwitchPayload(draft)
    submitting.value = true
    await createSession(payload)
    ElMessage.success(t('settings.easySwitch.messages.importCreated'))
    resetDraft()
    await loadSessions()
  } catch (error) {
    ElMessage.error(resolveValidationMessage(error))
  } finally {
    submitting.value = false
  }
}

async function removeSession(sessionId: string): Promise<void> {
  try {
    await ElMessageBox.confirm(
      t('settings.easySwitch.messages.deleteConfirm'),
      t('settings.easySwitch.actions.deleteHistory'),
      {
        type: 'warning',
        confirmButtonText: t('common.actions.delete'),
        cancelButtonText: t('common.actions.cancel')
      }
    )
  } catch {
    return
  }

  await deleteSession(sessionId)
  ElMessage.success(t('settings.easySwitch.messages.historyDeleted'))
  await loadSessions()
}

function resolveValidationMessage(error: unknown): string {
  if (error instanceof Error) {
    if (error.message === 'sourceEmail') {
      return t('settings.easySwitch.messages.sourceEmailRequired')
    }
    if (error.message === 'importType') {
      return t('settings.easySwitch.messages.selectImportType')
    }
    if (error.message === 'contactsCsv') {
      return t('settings.easySwitch.messages.contactsRequired')
    }
    if (error.message === 'calendarIcs') {
      return t('settings.easySwitch.messages.calendarRequired')
    }
    if (error.message === 'mailMessages') {
      return t('settings.easySwitch.messages.mailRequired')
    }
    return error.message
  }
  return t('settings.easySwitch.messages.importFailed')
}

function resetDraft(): void {
  Object.assign(draft, createMailEasySwitchDraft())
  contactsFileName.value = ''
  calendarFileName.value = ''
  mailFileNames.value = []
  manualMailDraft.value = ''
}

function getFirstFile(event: Event): File | null {
  const target = event.target as HTMLInputElement | null
  const file = target?.files?.[0] ?? null
  if (target) {
    target.value = ''
  }
  return file
}

function getFiles(event: Event): File[] {
  const target = event.target as HTMLInputElement | null
  const files = target?.files ? Array.from(target.files) : []
  if (target) {
    target.value = ''
  }
  return files
}

onMounted(() => {
  void loadSessions()
})
</script>

<template>
  <section class="mm-card easy-switch-panel">
    <header class="panel-header">
      <div>
        <div class="eyebrow">{{ t('settings.easySwitch.eyebrow') }}</div>
        <h2 class="mm-section-subtitle">{{ t('settings.easySwitch.title') }}</h2>
        <p class="panel-copy">{{ t('settings.easySwitch.description') }}</p>
      </div>
      <el-tag type="warning" effect="plain">{{ t('settings.easySwitch.boundary') }}</el-tag>
    </header>

    <el-alert
      :title="t('settings.easySwitch.noticeTitle')"
      :description="t('settings.easySwitch.noticeDescription')"
      type="info"
      show-icon
      :closable="false"
      class="notice"
    />

    <div class="workspace">
      <section class="composer">
        <div class="form-grid">
          <el-form-item :label="t('settings.easySwitch.fields.provider')">
            <el-select v-model="draft.provider">
              <el-option
                v-for="option in providerOptions"
                :key="option.value"
                :label="t(option.labelKey)"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('settings.easySwitch.fields.sourceEmail')">
            <el-input v-model="draft.sourceEmail" :placeholder="t('settings.easySwitch.placeholders.sourceEmail')" />
          </el-form-item>
        </div>

        <div class="toggle-grid">
          <label class="toggle-card">
            <el-checkbox v-model="draft.importContacts">{{ t('settings.easySwitch.types.contacts') }}</el-checkbox>
            <span class="toggle-copy">{{ t('settings.easySwitch.typeDescriptions.contacts') }}</span>
          </label>
          <label class="toggle-card">
            <el-checkbox v-model="draft.importCalendar">{{ t('settings.easySwitch.types.calendar') }}</el-checkbox>
            <span class="toggle-copy">{{ t('settings.easySwitch.typeDescriptions.calendar') }}</span>
          </label>
          <label class="toggle-card">
            <el-checkbox v-model="draft.importMail">{{ t('settings.easySwitch.types.mail') }}</el-checkbox>
            <span class="toggle-copy">{{ t('settings.easySwitch.typeDescriptions.mail') }}</span>
          </label>
        </div>

        <div class="import-card" :class="{ disabled: !draft.importContacts }">
          <div class="card-head">
            <div>
              <h3>{{ t('settings.easySwitch.cards.contacts.title') }}</h3>
              <p>{{ t('settings.easySwitch.cards.contacts.description') }}</p>
            </div>
            <el-switch v-model="draft.mergeContactDuplicates" :disabled="!draft.importContacts" />
          </div>
          <div class="card-toolbar">
            <input type="file" accept=".csv,text/csv" @change="onContactsFileChange">
            <span class="file-name">{{ contactsFileName || t('settings.easySwitch.fileHints.none') }}</span>
          </div>
          <el-input
            v-model="draft.contactsCsv"
            type="textarea"
            :rows="6"
            :disabled="!draft.importContacts"
            :placeholder="t('settings.easySwitch.placeholders.contactsCsv')"
          />
        </div>

        <div class="import-card" :class="{ disabled: !draft.importCalendar }">
          <div class="card-head">
            <div>
              <h3>{{ t('settings.easySwitch.cards.calendar.title') }}</h3>
              <p>{{ t('settings.easySwitch.cards.calendar.description') }}</p>
            </div>
          </div>
          <div class="card-toolbar">
            <input type="file" accept=".ics,text/calendar" @change="onCalendarFileChange">
            <span class="file-name">{{ calendarFileName || t('settings.easySwitch.fileHints.none') }}</span>
          </div>
          <el-input
            v-model="draft.calendarIcs"
            type="textarea"
            :rows="6"
            :disabled="!draft.importCalendar"
            :placeholder="t('settings.easySwitch.placeholders.calendarIcs')"
          />
        </div>

        <div class="import-card" :class="{ disabled: !draft.importMail }">
          <div class="card-head">
            <div>
              <h3>{{ t('settings.easySwitch.cards.mail.title') }}</h3>
              <p>{{ t('settings.easySwitch.cards.mail.description') }}</p>
            </div>
            <el-form-item :label="t('settings.easySwitch.fields.importedMailFolder')" class="folder-field">
              <el-select v-model="draft.importedMailFolder" :disabled="!draft.importMail">
                <el-option :label="t('nav.inbox')" value="INBOX" />
                <el-option :label="t('nav.archive')" value="ARCHIVE" />
              </el-select>
            </el-form-item>
          </div>
          <div class="card-toolbar">
            <input type="file" accept=".eml,message/rfc822" multiple @change="onMailFilesChange">
            <span class="file-name">
              {{ mailFileNames.length ? t('settings.easySwitch.fileHints.filesQueued', { count: mailFileNames.length }) : t('settings.easySwitch.fileHints.none') }}
            </span>
          </div>
          <el-input
            v-model="manualMailDraft"
            type="textarea"
            :rows="6"
            :disabled="!draft.importMail"
            :placeholder="t('settings.easySwitch.placeholders.mailEml')"
          />
          <div class="manual-actions">
            <el-button :disabled="!draft.importMail" @click="appendManualMailDraft">
              {{ t('settings.easySwitch.actions.queueMail') }}
            </el-button>
          </div>
          <div v-if="draft.mailMessages.length" class="queue-list">
            <div v-for="(item, index) in draft.mailMessages" :key="`${mailFileNames[index]}-${index}`" class="queue-item">
              <div>
                <div class="queue-name">{{ mailFileNames[index] || t('settings.easySwitch.queue.pastedMessage') }}</div>
                <div class="queue-preview">{{ item.slice(0, 80) }}</div>
              </div>
              <el-button text type="danger" @click="removeQueuedMail(index)">{{ t('common.actions.delete') }}</el-button>
            </div>
          </div>
        </div>

        <div class="submit-row">
          <el-button type="primary" :loading="submitting" @click="submitImport">
            {{ t('settings.easySwitch.actions.startImport') }}
          </el-button>
        </div>
      </section>

      <SettingsMailEasySwitchHistory
        :loading="loading"
        :sessions="sessions"
        @delete="removeSession"
      />
    </div>
  </section>
</template>

<style scoped>
.easy-switch-panel {
  padding: 24px;
  margin-top: 20px;
  background:
    radial-gradient(circle at top right, rgba(124, 92, 255, 0.14), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 248, 255, 0.94));
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #6c4cd2;
  margin-bottom: 8px;
}

.panel-copy {
  margin: 8px 0 0;
  max-width: 720px;
  color: var(--mm-muted);
  line-height: 1.6;
}

.notice {
  margin-top: 16px;
}

.workspace {
  margin-top: 20px;
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.9fr);
  gap: 20px;
}

.composer {
  display: grid;
  gap: 16px;
}

.form-grid,
.toggle-grid {
  display: grid;
  gap: 12px;
}

.form-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.toggle-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.toggle-card,
.import-card {
  border: 1px solid rgba(107, 83, 209, 0.14);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 16px 40px rgba(91, 79, 165, 0.08);
}

.toggle-card {
  padding: 14px;
  display: grid;
  gap: 8px;
}

.toggle-copy,
.file-name,
.queue-preview {
  color: var(--mm-muted);
  font-size: 13px;
  line-height: 1.5;
}

.import-card {
  padding: 16px;
  display: grid;
  gap: 12px;
}

.import-card.disabled {
  opacity: 0.72;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.card-head h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

.card-head p {
  margin: 0;
  color: var(--mm-muted);
}

.folder-field {
  min-width: 160px;
}

.card-toolbar,
.manual-actions,
.submit-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.queue-list {
  display: grid;
  gap: 12px;
}

.queue-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(109, 92, 255, 0.06);
}

.queue-name {
  font-weight: 600;
}

@media (max-width: 1100px) {
  .workspace,
  .form-grid,
  .toggle-grid {
    grid-template-columns: 1fr;
  }

  .panel-header,
  .card-head,
  .queue-item {
    flex-direction: column;
  }
}
</style>
