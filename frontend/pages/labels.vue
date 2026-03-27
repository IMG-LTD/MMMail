<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { useLabelApi } from '~/composables/useLabelApi'
import { useMailFolderApi } from '~/composables/useMailFolderApi'
import { useMailStore } from '~/stores/mail'
import type { LabelItem } from '~/types/api'
import type { MailFolderDraft, MailFolderNode } from '~/types/mail-folders'
import {
  buildMailFolderPayload,
  createMailFolderDraft,
  findMailFolderNode,
  flattenMailFolderTree
} from '~/utils/mail-folders'

const { t } = useI18n()
const mailStore = useMailStore()
const loading = ref(false)
const savingFolder = ref(false)
const folderDialogVisible = ref(false)
const editingFolderId = ref<string | null>(null)
const mailFolders = ref<MailFolderNode[]>([])
const labels = ref<LabelItem[]>([])

const folderDraft = reactive<MailFolderDraft>(createMailFolderDraft())
const labelForm = reactive({
  name: '',
  color: '#0F6E6E'
})

const flatFolders = computed(() => flattenMailFolderTree(mailFolders.value))
const rootFolders = computed(() => mailFolders.value)
const parentOptions = computed(() => flatFolders.value.filter((item) => item.depth === 0))
const folderCount = computed(() => flatFolders.value.length)
const subfolderCount = computed(() => flatFolders.value.filter((item) => item.depth > 0).length)
const unreadCount = computed(() => flatFolders.value.reduce((total, item) => total + item.unreadCount, 0))
const dialogTitle = computed(() => editingFolderId.value
  ? t('mailFolders.dialog.edit')
  : t('mailFolders.dialog.create'))

const { listLabels, createLabel, deleteLabel } = useLabelApi()
const { listMailFolders, createMailFolder, updateMailFolder, deleteMailFolder } = useMailFolderApi()

async function loadWorkspace(): Promise<void> {
  loading.value = true
  try {
    const [nextFolders, nextLabels] = await Promise.all([listMailFolders(), listLabels()])
    mailFolders.value = nextFolders
    labels.value = nextLabels
    mailStore.setCustomFolders(nextFolders)
  } catch (error) {
    ElMessage.error(resolveMessage(error, 'mailFolders.messages.loadFailed'))
  } finally {
    loading.value = false
  }
}

function openCreateFolder(parentId = ''): void {
  editingFolderId.value = null
  Object.assign(folderDraft, createMailFolderDraft(parentId))
  folderDialogVisible.value = true
}

function openEditFolder(folderId: string): void {
  const folder = findMailFolderNode(mailFolders.value, folderId)
  if (!folder) {
    return
  }
  editingFolderId.value = folder.id
  Object.assign(folderDraft, {
    name: folder.name,
    color: folder.color,
    parentId: folder.parentId || '',
    notificationsEnabled: folder.notificationsEnabled
  })
  folderDialogVisible.value = true
}

async function submitFolder(): Promise<void> {
  try {
    savingFolder.value = true
    const payload = buildMailFolderPayload(folderDraft)
    if (editingFolderId.value) {
      await updateMailFolder(editingFolderId.value, {
        name: payload.name,
        color: payload.color,
        notificationsEnabled: payload.notificationsEnabled
      })
      ElMessage.success(t('mailFolders.messages.folderUpdated'))
    } else {
      await createMailFolder(payload)
      ElMessage.success(t('mailFolders.messages.folderCreated'))
    }
    folderDialogVisible.value = false
    await loadWorkspace()
  } catch (error) {
    ElMessage.error(resolveFolderMessage(error))
  } finally {
    savingFolder.value = false
  }
}

async function removeFolder(folderId: string): Promise<void> {
  try {
    await ElMessageBox.confirm(
      t('mailFolders.messages.deleteFolderConfirm'),
      t('mailFolders.actions.delete'),
      {
        type: 'warning',
        confirmButtonText: t('common.actions.delete'),
        cancelButtonText: t('common.actions.cancel')
      }
    )
  } catch {
    return
  }

  await deleteMailFolder(folderId)
  ElMessage.success(t('mailFolders.messages.folderDeleted'))
  await loadWorkspace()
}

async function addLabel(): Promise<void> {
  if (!labelForm.name.trim()) {
    ElMessage.warning(t('mailFolders.messages.nameRequired'))
    return
  }
  await createLabel(labelForm.name.trim(), labelForm.color)
  labelForm.name = ''
  await loadWorkspace()
}

async function removeLabel(id: number): Promise<void> {
  await deleteLabel(id)
  ElMessage.success(t('mailFolders.messages.labelDeleted'))
  await loadWorkspace()
}

function resolveFolderMessage(error: unknown): string {
  if (!(error instanceof Error)) {
    return t('mailFolders.messages.loadFailed')
  }
  if (error.message === 'name') {
    return t('mailFolders.messages.nameRequired')
  }
  if (error.message === 'color') {
    return t('mailFolders.messages.invalidColor')
  }
  return error.message
}

function resolveMessage(error: unknown, fallbackKey: string): string {
  return error instanceof Error ? error.message : t(fallbackKey)
}

onMounted(() => {
  void loadWorkspace()
})
</script>

<template>
  <div class="mm-page folders-labels-page" v-loading="loading">
    <section class="hero mm-card">
      <div>
        <div class="eyebrow">{{ t('mailFolders.page.eyebrow') }}</div>
        <h1 class="mm-section-title">{{ t('mailFolders.page.title') }}</h1>
        <p class="hero-copy">{{ t('mailFolders.page.description') }}</p>
      </div>
      <div class="hero-actions">
        <el-button plain @click="loadWorkspace">{{ t('mailFolders.actions.refresh') }}</el-button>
        <el-button type="primary" @click="openCreateFolder()">{{ t('mailFolders.actions.newFolder') }}</el-button>
      </div>
    </section>

    <section class="metrics">
      <article class="metric-card">
        <span>{{ t('mailFolders.metrics.folders') }}</span>
        <strong>{{ folderCount }}</strong>
      </article>
      <article class="metric-card">
        <span>{{ t('mailFolders.metrics.subfolders') }}</span>
        <strong>{{ subfolderCount }}</strong>
      </article>
      <article class="metric-card">
        <span>{{ t('mailFolders.metrics.labels') }}</span>
        <strong>{{ labels.length }}</strong>
      </article>
      <article class="metric-card">
        <span>{{ t('mailFolders.metrics.unread') }}</span>
        <strong>{{ unreadCount }}</strong>
      </article>
    </section>

    <section class="workspace">
      <article class="mm-card folder-panel">
        <header class="section-head">
          <div>
            <h2 class="mm-section-subtitle">{{ t('mailFolders.sections.folders') }}</h2>
          </div>
          <el-button type="primary" plain @click="openCreateFolder()">{{ t('mailFolders.actions.newFolder') }}</el-button>
        </header>

        <el-empty v-if="!rootFolders.length" :description="t('mailFolders.empty.folders')" />

        <div v-else class="folder-grid">
          <article v-for="folder in rootFolders" :key="folder.id" class="folder-card">
            <header class="folder-card-head">
              <div class="folder-title-wrap">
                <span class="folder-swatch" :style="{ backgroundColor: folder.color }" />
                <div>
                  <div class="folder-title">{{ folder.name }}</div>
                  <div class="folder-meta">
                    <span>{{ t('mailFolders.folder.root') }}</span>
                    <span>·</span>
                    <span>{{ t(folder.notificationsEnabled ? 'mailFolders.folder.notificationsOn' : 'mailFolders.folder.notificationsOff') }}</span>
                  </div>
                </div>
              </div>
              <el-tag type="info" effect="plain">{{ t('mailFolders.folder.mailCount', { count: folder.totalCount }) }}</el-tag>
            </header>

            <div class="folder-stat-row">
              <el-tag effect="plain">{{ t('mailFolders.folder.childCount', { count: folder.children.length }) }}</el-tag>
              <el-tag type="success" effect="plain">{{ folder.unreadCount }}</el-tag>
            </div>

            <div class="folder-actions">
              <el-button text type="primary" @click="navigateTo(`/folders/${folder.id}`)">{{ t('mailFolders.actions.openFolder') }}</el-button>
              <el-button text @click="openCreateFolder(folder.id)">{{ t('mailFolders.actions.newSubfolder') }}</el-button>
              <el-button text @click="openEditFolder(folder.id)">{{ t('mailFolders.actions.edit') }}</el-button>
              <el-button text type="danger" @click="removeFolder(folder.id)">{{ t('mailFolders.actions.delete') }}</el-button>
            </div>

            <div v-if="folder.children.length" class="child-list">
              <button
                v-for="child in folder.children"
                :key="child.id"
                class="child-row"
                type="button"
                @click="navigateTo(`/folders/${child.id}`)"
              >
                <span class="child-main">
                  <span class="folder-swatch folder-swatch--small" :style="{ backgroundColor: child.color }" />
                  <span>{{ child.name }}</span>
                </span>
                <span class="child-count">{{ child.unreadCount }}</span>
              </button>
            </div>
          </article>
        </div>
      </article>

      <article class="mm-card label-panel">
        <header class="section-head">
          <h2 class="mm-section-subtitle">{{ t('mailFolders.sections.labels') }}</h2>
        </header>

        <div class="label-create-row">
          <el-input v-model="labelForm.name" :placeholder="t('mailFolders.placeholders.labelName')" />
          <el-color-picker v-model="labelForm.color" />
          <el-button type="primary" @click="addLabel">{{ t('mailFolders.actions.addLabel') }}</el-button>
        </div>

        <el-empty v-if="!labels.length" :description="t('mailFolders.empty.labels')" />

        <el-table v-else :data="labels" style="width: 100%">
          <el-table-column prop="name" :label="t('mailFolders.fields.labelName')" />
          <el-table-column :label="t('mailFolders.fields.color')">
            <template #default="{ row }">
              <span class="folder-swatch folder-swatch--small" :style="{ backgroundColor: row.color }" />
              {{ row.color }}
            </template>
          </el-table-column>
          <el-table-column :label="t('common.actions.delete')" width="120">
            <template #default="{ row }">
              <el-button type="danger" text @click="removeLabel(row.id)">{{ t('mailFolders.actions.deleteLabel') }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </article>
    </section>

    <el-dialog v-model="folderDialogVisible" :title="dialogTitle" width="520px">
      <el-form label-position="top">
        <el-form-item :label="t('mailFolders.fields.name')">
          <el-input v-model="folderDraft.name" :placeholder="t('mailFolders.placeholders.folderName')" />
        </el-form-item>
        <div class="dialog-grid">
          <el-form-item :label="t('mailFolders.fields.color')">
            <el-color-picker v-model="folderDraft.color" />
          </el-form-item>
          <el-form-item v-if="!editingFolderId" :label="t('mailFolders.fields.parent')">
            <el-select v-model="folderDraft.parentId" clearable :placeholder="t('mailFolders.placeholders.parent')">
              <el-option
                v-for="folder in parentOptions"
                :key="folder.id"
                :label="folder.name"
                :value="folder.id"
              />
            </el-select>
          </el-form-item>
        </div>
        <label class="notification-switch">
          <span>{{ t('mailFolders.fields.notifications') }}</span>
          <el-switch v-model="folderDraft.notificationsEnabled" />
        </label>
      </el-form>
      <template #footer>
        <el-button @click="folderDialogVisible = false">{{ t('common.actions.cancel') }}</el-button>
        <el-button type="primary" :loading="savingFolder" @click="submitFolder">{{ t('mailFolders.actions.saveFolder') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.folders-labels-page {
  display: grid;
  gap: 18px;
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  padding: 26px;
  border: 1px solid rgba(91, 124, 250, 0.14);
  background:
    radial-gradient(circle at top right, rgba(91, 124, 250, 0.14), transparent 28%),
    linear-gradient(145deg, rgba(247, 249, 255, 0.98), rgba(255, 255, 255, 0.96));
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.08);
}

.eyebrow {
  margin-bottom: 10px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #5b7cfa;
}

.hero-copy {
  max-width: 720px;
  color: rgba(15, 23, 42, 0.72);
}

.hero-actions,
.metrics,
.section-head,
.folder-card-head,
.folder-stat-row,
.folder-actions,
.notification-switch {
  display: flex;
}

.hero-actions,
.folder-actions {
  gap: 10px;
}

.metrics {
  gap: 12px;
  flex-wrap: wrap;
}

.metric-card {
  min-width: 160px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.metric-card span {
  display: block;
  color: rgba(71, 85, 105, 0.92);
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  font-size: 30px;
  line-height: 1.1;
}

.workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(320px, 0.9fr);
  gap: 18px;
}

.folder-panel,
.label-panel {
  padding: 22px;
}

.section-head {
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.folder-grid {
  display: grid;
  gap: 14px;
}

.folder-card {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 255, 0.9));
}

.folder-card-head {
  justify-content: space-between;
  gap: 12px;
}

.folder-title-wrap {
  display: flex;
  gap: 12px;
}

.folder-title {
  font-size: 18px;
  font-weight: 700;
}

.folder-meta {
  display: flex;
  gap: 6px;
  margin-top: 6px;
  color: rgba(100, 116, 139, 0.9);
  font-size: 13px;
}

.folder-stat-row {
  gap: 10px;
  margin: 14px 0;
}

.folder-actions {
  flex-wrap: wrap;
}

.child-list {
  display: grid;
  gap: 8px;
  margin-top: 14px;
}

.child-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 0;
  border-radius: 14px;
  background: rgba(244, 247, 255, 0.9);
  cursor: pointer;
}

.child-main {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.child-count {
  min-width: 28px;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(91, 124, 250, 0.12);
  color: #3650d4;
  font-size: 12px;
  text-align: center;
}

.label-create-row {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 10px;
  margin-bottom: 14px;
}

.folder-swatch {
  width: 14px;
  height: 14px;
  border-radius: 999px;
  box-shadow: 0 0 0 1px rgba(15, 23, 42, 0.08);
}

.folder-swatch--small {
  width: 10px;
  height: 10px;
  display: inline-block;
  margin-right: 8px;
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.notification-switch {
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(244, 247, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

@media (max-width: 980px) {
  .hero,
  .workspace {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
  }
}

@media (max-width: 720px) {
  .label-create-row,
  .dialog-grid {
    grid-template-columns: 1fr;
  }
}
</style>
