<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { useLabelApi } from '~/composables/useLabelApi'
import { useMailFilterApi } from '~/composables/useMailFilterApi'
import { useMailFolderApi } from '~/composables/useMailFolderApi'
import type { LabelItem } from '~/types/api'
import type {
  MailFilter,
  MailFilterDraft,
  MailFilterEffectiveFolder,
  MailFilterPreview,
  MailFilterTargetFolder
} from '~/types/mail-filters'
import type { MailFolderNode } from '~/types/mail-folders'
import {
  buildMailFilterPayload,
  createMailFilterDraft,
  describeMailFilterActions,
  describeMailFilterConditions,
  MAIL_FILTER_TARGET_FOLDERS,
  resolveFolderKey
} from '~/utils/mail-filters'
import { flattenMailFolderTree } from '~/utils/mail-folders'

const { t } = useI18n()
const loading = ref(false)
const saving = ref(false)
const previewing = ref(false)
const dialogVisible = ref(false)
const editingFilterId = ref<string | null>(null)
const filters = ref<MailFilter[]>([])
const labels = ref<LabelItem[]>([])
const mailFolders = ref<MailFolderNode[]>([])
const previewResult = ref<MailFilterPreview | null>(null)

const draft = reactive<MailFilterDraft>(createMailFilterDraft())
const previewForm = reactive({
  senderEmail: '',
  subject: '',
  body: ''
})

const { listLabels } = useLabelApi()
const { listMailFolders } = useMailFolderApi()
const {
  listMailFilters,
  createMailFilter,
  updateMailFilter,
  deleteMailFilter,
  previewMailFilter
} = useMailFilterApi()

const activeCount = computed(() => filters.value.filter((item) => item.enabled).length)
const archiveCount = computed(() => filters.value.filter((item) => item.targetFolder === 'ARCHIVE').length)
const customFolderCount = computed(() => filters.value.filter((item) => !!item.targetCustomFolderId).length)
const dialogTitle = computed(() => editingFilterId.value
  ? t('settings.mailFilters.dialog.edit')
  : t('settings.mailFilters.dialog.create'))
const customFolderOptions = computed(() => flattenMailFolderTree(mailFolders.value))
const selectedTargetValue = computed({
  get: () => {
    if (draft.targetCustomFolderId) {
      return `custom:${draft.targetCustomFolderId}`
    }
    return draft.targetFolder ? `system:${draft.targetFolder}` : ''
  },
  set: (value: string) => {
    if (!value) {
      draft.targetFolder = ''
      draft.targetCustomFolderId = ''
      return
    }
    if (value.startsWith('custom:')) {
      draft.targetCustomFolderId = value.slice('custom:'.length)
      draft.targetFolder = ''
      return
    }
    draft.targetFolder = value.slice('system:'.length) as MailFilterTargetFolder
    draft.targetCustomFolderId = ''
  }
})

async function loadWorkspace(): Promise<void> {
  loading.value = true
  try {
    const [nextFilters, nextLabels, nextFolders] = await Promise.all([listMailFilters(), listLabels(), listMailFolders()])
    filters.value = nextFilters
    labels.value = nextLabels
    mailFolders.value = nextFolders
  } catch (error) {
    ElMessage.error(resolveMessage(error, 'settings.mailFilters.messages.loadFailed'))
  } finally {
    loading.value = false
  }
}

function openCreateDialog(): void {
  editingFilterId.value = null
  Object.assign(draft, createMailFilterDraft())
  dialogVisible.value = true
}

function openEditDialog(filter: MailFilter): void {
  editingFilterId.value = filter.id
  Object.assign(draft, {
    name: filter.name,
    senderContains: filter.senderContains || '',
    subjectContains: filter.subjectContains || '',
    keywordContains: filter.keywordContains || '',
    targetFolder: filter.targetFolder || '',
    targetCustomFolderId: filter.targetCustomFolderId || '',
    labels: [...filter.labels],
    markRead: filter.markRead,
    enabled: filter.enabled
  })
  dialogVisible.value = true
}

async function submitDialog(): Promise<void> {
  try {
    const payload = buildMailFilterPayload(draft)
    saving.value = true
    if (editingFilterId.value) {
      await updateMailFilter(editingFilterId.value, payload)
      ElMessage.success(t('settings.mailFilters.messages.updated'))
    } else {
      await createMailFilter(payload)
      ElMessage.success(t('settings.mailFilters.messages.created'))
    }
    dialogVisible.value = false
    await loadWorkspace()
  } catch (error) {
    ElMessage.error(resolveValidationMessage(error))
  } finally {
    saving.value = false
  }
}

async function removeFilter(filter: MailFilter): Promise<void> {
  try {
    await ElMessageBox.confirm(
      t('settings.mailFilters.messages.deleteConfirm'),
      t('settings.mailFilters.actions.delete'),
      {
        type: 'warning',
        confirmButtonText: t('common.actions.delete'),
        cancelButtonText: t('common.actions.cancel')
      }
    )
  } catch {
    return
  }
  await deleteMailFilter(filter.id)
  ElMessage.success(t('settings.mailFilters.messages.deleted'))
  await loadWorkspace()
}

async function runPreview(): Promise<void> {
  try {
    previewing.value = true
    previewResult.value = await previewMailFilter({
      senderEmail: previewForm.senderEmail.trim(),
      subject: previewForm.subject.trim() || undefined,
      body: previewForm.body.trim() || undefined
    })
  } catch (error) {
    ElMessage.error(resolveMessage(error, 'settings.mailFilters.messages.previewFailed'))
  } finally {
    previewing.value = false
  }
}

function conditionSummary(filter: MailFilter): string {
  return describeMailFilterConditions(filter, t)
}

function actionSummary(filter: MailFilter): string {
  return describeMailFilterActions(filter, t)
}

function folderLabel(folder: MailFilterEffectiveFolder | null, customFolderName?: string | null): string {
  if (folder === 'CUSTOM') {
    return customFolderName || t('common.none')
  }
  if (!folder) {
    return t('common.none')
  }
  return t(resolveFolderKey(folder as MailFilterTargetFolder))
}

function resolveMessage(error: unknown, fallbackKey: string): string {
  return error instanceof Error ? error.message : t(fallbackKey)
}

function resolveValidationMessage(error: unknown): string {
  if (!(error instanceof Error)) {
    return t('settings.mailFilters.messages.saveFailed')
  }
  if (error.message === 'name') {
    return t('settings.mailFilters.errors.nameRequired')
  }
  if (error.message === 'conditions') {
    return t('settings.mailFilters.errors.conditionsRequired')
  }
  if (error.message === 'actions') {
    return t('settings.mailFilters.errors.actionsRequired')
  }
  if (error.message === 'target') {
    return t('settings.mailFilters.errors.targetRequired')
  }
  if (error.message === 'color') {
    return t('mailFolders.messages.invalidColor')
  }
  return error.message
}

onMounted(() => {
  void loadWorkspace()
})
</script>

<template>
  <section class="mm-card mail-filters-panel" v-loading="loading">
    <header class="panel-header">
      <div>
        <div class="eyebrow">{{ t('settings.mailFilters.eyebrow') }}</div>
        <h2 class="mm-section-subtitle">{{ t('settings.mailFilters.title') }}</h2>
        <p class="panel-copy">{{ t('settings.mailFilters.description') }}</p>
      </div>
      <div class="toolbar">
        <el-button plain @click="loadWorkspace">{{ t('settings.mailFilters.actions.refresh') }}</el-button>
        <el-button type="primary" @click="openCreateDialog">{{ t('settings.mailFilters.actions.create') }}</el-button>
      </div>
    </header>

    <el-alert
      :title="t('settings.mailFilters.noticeTitle')"
      :description="t('settings.mailFilters.noticeDescription')"
      type="info"
      show-icon
      :closable="false"
      class="notice"
    />

    <div class="metrics">
      <article class="metric-card">
        <span>{{ t('settings.mailFilters.metrics.total') }}</span>
        <strong>{{ filters.length }}</strong>
      </article>
      <article class="metric-card">
        <span>{{ t('settings.mailFilters.metrics.active') }}</span>
        <strong>{{ activeCount }}</strong>
      </article>
      <article class="metric-card">
        <span>{{ t('settings.mailFilters.metrics.archive') }}</span>
        <strong>{{ archiveCount }}</strong>
      </article>
      <article class="metric-card">
        <span>{{ t('settings.mailFilters.metrics.customFolders') }}</span>
        <strong>{{ customFolderCount }}</strong>
      </article>
    </div>

    <div class="workspace">
      <section class="ledger">
        <el-table :data="filters" :empty-text="t('settings.mailFilters.empty')">
          <el-table-column prop="name" :label="t('settings.mailFilters.columns.name')" min-width="200" />
          <el-table-column :label="t('settings.mailFilters.columns.conditions')" min-width="260">
            <template #default="{ row }">
              <span>{{ conditionSummary(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('settings.mailFilters.columns.actions')" min-width="240">
            <template #default="{ row }">
              <span>{{ actionSummary(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('settings.mailFilters.columns.status')" width="120">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">
                {{ t(row.enabled ? 'settings.mailFilters.status.enabled' : 'settings.mailFilters.status.disabled') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" :label="t('settings.mailFilters.columns.updatedAt')" min-width="170" />
          <el-table-column :label="t('settings.mailFilters.columns.operations')" width="180">
            <template #default="{ row }">
              <div class="operation-buttons">
                <el-button link type="primary" @click="openEditDialog(row)">
                  {{ t('settings.mailFilters.actions.edit') }}
                </el-button>
                <el-button link type="danger" @click="removeFilter(row)">
                  {{ t('settings.mailFilters.actions.delete') }}
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="preview-card">
        <div class="preview-head">
          <h3>{{ t('settings.mailFilters.preview.title') }}</h3>
          <p>{{ t('settings.mailFilters.preview.description') }}</p>
        </div>

        <el-form label-position="top">
          <el-form-item :label="t('settings.mailFilters.fields.senderContains')">
            <el-input
              v-model="previewForm.senderEmail"
              :placeholder="t('settings.mailFilters.placeholders.previewSender')"
            />
          </el-form-item>
          <el-form-item :label="t('settings.mailFilters.fields.subjectContains')">
            <el-input
              v-model="previewForm.subject"
              :placeholder="t('settings.mailFilters.placeholders.previewSubject')"
            />
          </el-form-item>
          <el-form-item :label="t('settings.mailFilters.fields.keywordContains')">
            <el-input
              v-model="previewForm.body"
              type="textarea"
              :rows="5"
              :placeholder="t('settings.mailFilters.placeholders.previewBody')"
            />
          </el-form-item>
          <el-button type="primary" :loading="previewing" @click="runPreview">
            {{ t('settings.mailFilters.actions.preview') }}
          </el-button>
        </el-form>

        <div v-if="previewResult" class="preview-result">
          <el-alert
            v-if="previewResult.blockedBySecurityRule"
            :title="t('settings.mailFilters.preview.blocked')"
            type="warning"
            show-icon
            :closable="false"
          />
          <el-descriptions :column="1" border>
            <el-descriptions-item :label="t('settings.mailFilters.preview.baseFolder')">
              {{ folderLabel(previewResult.baseFolder) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('settings.mailFilters.preview.effectiveFolder')">
              {{ folderLabel(previewResult.effectiveFolder, previewResult.effectiveCustomFolderName) }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('settings.mailFilters.preview.labels')">
              {{ previewResult.effectiveLabels.join(', ') || t('common.none') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('settings.mailFilters.preview.markRead')">
              {{ t(previewResult.markRead ? 'settings.mailFilters.preview.markReadYes' : 'settings.mailFilters.preview.markReadNo') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('settings.mailFilters.preview.matchedFilter')">
              {{ previewResult.matchedFilterName || t('settings.mailFilters.preview.none') }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('settings.mailFilters.preview.securityReason')">
              {{ previewResult.securityReason }}
            </el-descriptions-item>
            <el-descriptions-item :label="t('settings.mailFilters.preview.securityMatchedRule')">
              {{ previewResult.securityMatchedRule || t('common.none') }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </section>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="620px">
      <el-form label-position="top">
        <el-form-item :label="t('settings.mailFilters.fields.name')">
          <el-input v-model="draft.name" :placeholder="t('settings.mailFilters.placeholders.name')" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item :label="t('settings.mailFilters.fields.senderContains')">
            <el-input
              v-model="draft.senderContains"
              :placeholder="t('settings.mailFilters.placeholders.senderContains')"
            />
          </el-form-item>
          <el-form-item :label="t('settings.mailFilters.fields.subjectContains')">
            <el-input
              v-model="draft.subjectContains"
              :placeholder="t('settings.mailFilters.placeholders.subjectContains')"
            />
          </el-form-item>
        </div>
        <el-form-item :label="t('settings.mailFilters.fields.keywordContains')">
          <el-input
            v-model="draft.keywordContains"
            :placeholder="t('settings.mailFilters.placeholders.keywordContains')"
          />
        </el-form-item>
        <div class="form-grid">
          <el-form-item :label="t('settings.mailFilters.fields.targetFolder')">
            <el-select v-model="selectedTargetValue" clearable>
              <el-option-group :label="t('settings.mailFilters.groups.systemFolders')">
                <el-option
                  v-for="folder in MAIL_FILTER_TARGET_FOLDERS"
                  :key="folder"
                  :label="t(resolveFolderKey(folder))"
                  :value="`system:${folder}`"
                />
              </el-option-group>
              <el-option-group
                v-if="customFolderOptions.length"
                :label="t('settings.mailFilters.groups.customFolders')"
              >
                <el-option
                  v-for="folder in customFolderOptions"
                  :key="folder.id"
                  :label="folder.label"
                  :value="`custom:${folder.id}`"
                />
              </el-option-group>
            </el-select>
          </el-form-item>
          <el-form-item :label="t('settings.mailFilters.fields.labels')">
            <el-select v-model="draft.labels" multiple collapse-tags filterable>
              <el-option
                v-for="label in labels"
                :key="label.id"
                :label="label.name"
                :value="label.name"
              />
            </el-select>
          </el-form-item>
        </div>
        <div class="switch-grid">
          <label class="switch-card">
            <el-switch v-model="draft.markRead" />
            <span>{{ t('settings.mailFilters.fields.markRead') }}</span>
          </label>
          <label class="switch-card">
            <el-switch v-model="draft.enabled" />
            <span>{{ t('settings.mailFilters.fields.enabled') }}</span>
          </label>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.actions.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="submitDialog">{{ t('common.actions.save') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.mail-filters-panel {
  margin-top: 18px;
  padding: 24px;
  border: 1px solid rgba(91, 124, 250, 0.16);
  background:
    radial-gradient(circle at top right, rgba(91, 124, 250, 0.12), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 249, 255, 0.98));
  box-shadow: 0 24px 80px rgba(17, 24, 39, 0.08);
}

.panel-header,
.toolbar,
.metrics,
.workspace,
.preview-head,
.operation-buttons,
.switch-grid,
.switch-card {
  display: flex;
}

.panel-header,
.preview-head {
  justify-content: space-between;
  gap: 16px;
}

.toolbar,
.operation-buttons,
.switch-grid {
  gap: 10px;
}

.panel-copy {
  max-width: 720px;
  color: rgba(15, 23, 42, 0.72);
}

.notice {
  margin: 18px 0;
}

.metrics {
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.metric-card {
  min-width: 140px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.metric-card span {
  display: block;
  color: rgba(71, 85, 105, 0.92);
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin-top: 6px;
  font-size: 28px;
  line-height: 1.1;
}

.workspace {
  gap: 18px;
  align-items: flex-start;
}

.ledger {
  flex: 1 1 0;
  min-width: 0;
}

.preview-card {
  width: 360px;
  flex: 0 0 360px;
  padding: 18px;
  border-radius: 20px;
  background: rgba(10, 14, 29, 0.92);
  color: #eef2ff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.preview-card :deep(.el-form-item__label),
.preview-card p,
.preview-card h3 {
  color: inherit;
}

.preview-card :deep(.el-input__wrapper),
.preview-card :deep(.el-textarea__inner) {
  background: rgba(15, 23, 42, 0.8);
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.18) inset;
}

.preview-result {
  margin-top: 16px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.switch-card {
  flex: 1 1 0;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(244, 247, 255, 0.88);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #5b7cfa;
}

@media (max-width: 1080px) {
  .workspace {
    flex-direction: column;
  }

  .preview-card {
    width: 100%;
    flex-basis: auto;
  }
}

@media (max-width: 768px) {
  .mail-filters-panel {
    padding: 18px;
  }

  .panel-header {
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .switch-grid {
    flex-direction: column;
  }
}
</style>
