<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { DriveItem, DrivePreviewKind, PublicDriveShareMetadata } from '~/types/api'
import { useDriveApi } from '~/composables/useDriveApi'
import { useI18n } from '~/composables/useI18n'
import { useAuthStore } from '~/stores/auth'
import {
  getDriveShareProtectionI18nKey,
  getDriveShareStatusI18nKey,
  resolvePublicDriveShareErrorKey
} from '~/utils/drive-share'

definePageMeta({
  layout: 'public-drive'
})

const route = useRoute()
const authStore = useAuthStore()
const { t } = useI18n()
const {
  getPublicShareMetadata,
  listPublicShareItems,
  downloadPublicShareFile,
  previewPublicShareFile,
  downloadPublicShareItem,
  previewPublicShareItem,
  uploadPublicShareFile,
  saveSharedWithMe
} = useDriveApi()

const token = computed(() => String(route.params.token || ''))
const loading = ref(false)
const folderLoading = ref(false)
const downloading = ref(false)
const previewLoading = ref(false)
const uploadLoading = ref(false)
const saveForLaterLoading = ref(false)
const saveForLaterDone = ref(false)
const metadata = ref<PublicDriveShareMetadata | null>(null)
const pageError = ref('')
const folderError = ref('')
const sharePassword = ref('')
const unlocked = ref(false)
const currentFolderId = ref<string | null>(null)
const folderItems = ref<DriveItem[]>([])
const folderTrail = ref<DriveItem[]>([])
const selectedFile = ref<DriveItem | null>(null)
const uploadInput = ref<HTMLInputElement | null>(null)
const previewKind = ref<DrivePreviewKind>('UNSUPPORTED')
const previewBlobUrl = ref('')
const previewText = ref('')
const previewMimeType = ref('')
const previewTruncated = ref(false)
const previewMessage = ref('')

const isFolderShare = computed(() => metadata.value?.itemType === 'FOLDER')
const statusType = computed(() => (metadata.value?.status === 'ACTIVE' ? 'success' : 'info'))
const protectionType = computed(() => (metadata.value?.passwordProtected ? 'warning' : 'info'))
const canInteract = computed(() => metadata.value?.status === 'ACTIVE' && !loading.value)
const canAccessContents = computed(() => canInteract.value && (!metadata.value?.passwordProtected || unlocked.value))
const canUpload = computed(() => isFolderShare.value && canAccessContents.value && metadata.value?.permission === 'EDIT')
const isAuthenticated = computed(() => authStore.isAuthenticated)
const canSaveForLater = computed(() => {
  if (!metadata.value || !isAuthenticated.value || saveForLaterDone.value) {
    return false
  }
  if (metadata.value.permission !== 'VIEW') {
    return false
  }
  return !metadata.value.passwordProtected || unlocked.value
})
const saveForLaterHint = computed(() => {
  if (!metadata.value) {
    return t('drive.sharedWithMe.hints.ready')
  }
  if (saveForLaterDone.value) {
    return t('drive.sharedWithMe.hints.saved')
  }
  if (!isAuthenticated.value) {
    return t('drive.sharedWithMe.hints.signIn')
  }
  if (metadata.value.permission !== 'VIEW') {
    return t('drive.sharedWithMe.hints.onlyViewLinks')
  }
  if (metadata.value.passwordProtected && !unlocked.value) {
    return t('drive.sharedWithMe.hints.unlockFirst')
  }
  return t('drive.sharedWithMe.hints.ready')
})
const currentPassword = computed(() => sharePassword.value.trim() || undefined)
const metadataMimeType = computed(() => metadata.value?.mimeType || 'application/octet-stream')
const pageTitle = computed(() => t(isFolderShare.value ? 'drive.publicShare.folder.title' : 'drive.publicShare.title'))
const pageSubtitle = computed(() => t(isFolderShare.value ? 'drive.publicShare.folder.subtitle' : 'drive.publicShare.subtitle'))
const displayName = computed(() => {
  if (metadata.value?.itemName) {
    return metadata.value.itemName
  }
  return t(isFolderShare.value ? 'drive.publicShare.folder.fallbackName' : 'drive.publicShare.fallbackName')
})
const folderPermissionKey = computed(() => {
  return metadata.value?.permission === 'EDIT'
    ? 'drive.publicShare.folder.permission.edit'
    : 'drive.publicShare.folder.permission.view'
})
const pathSegments = computed(() => {
  const root = {
    id: metadata.value?.itemId || 'root',
    label: displayName.value
  }
  return [root, ...folderTrail.value.map((item) => ({ id: item.id, label: item.name }))]
})

function formatBytes(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`
  }
  const units = ['KB', 'MB', 'GB', 'TB']
  let value = bytes / 1024
  let index = 0
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index += 1
  }
  return `${value.toFixed(value >= 10 ? 1 : 2)} ${units[index]}`
}

function formatTime(value: string | null): string {
  return value || t('common.none')
}

function getItemTypeLabel(itemType: string | null | undefined): string {
  if (itemType === 'FOLDER') {
    return t('drive.search.types.folder')
  }
  if (itemType === 'FILE') {
    return t('drive.search.types.file')
  }
  return itemType || t('common.none')
}

function getPermissionLabel(permission: string | null | undefined): string {
  if (permission === 'EDIT') {
    return t('drive.publicShare.permissions.edit')
  }
  if (permission === 'VIEW') {
    return t('drive.publicShare.permissions.view')
  }
  return permission || t('common.none')
}

function saveBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

function clearPreviewState(): void {
  previewKind.value = 'UNSUPPORTED'
  previewText.value = ''
  previewMimeType.value = ''
  previewTruncated.value = false
  previewMessage.value = ''
  selectedFile.value = null
  if (previewBlobUrl.value) {
    URL.revokeObjectURL(previewBlobUrl.value)
    previewBlobUrl.value = ''
  }
}

function clearFolderState(): void {
  currentFolderId.value = metadata.value?.itemId || null
  folderItems.value = []
  folderTrail.value = []
  folderError.value = ''
}

function resolvePreviewKind(mimeType: string): DrivePreviewKind {
  const normalized = mimeType.toLowerCase()
  if (normalized.startsWith('text/') || normalized.includes('json') || normalized.includes('xml')) {
    return 'TEXT'
  }
  if (normalized.startsWith('image/')) {
    return 'IMAGE'
  }
  if (normalized === 'application/pdf') {
    return 'PDF'
  }
  return 'UNSUPPORTED'
}

function resolveErrorMessage(error: unknown, fallbackKey: string): string {
  const message = error instanceof Error ? error.message : ''
  const translatedKey = resolvePublicDriveShareErrorKey(message)
  return translatedKey ? t(translatedKey) : t(fallbackKey)
}

async function loadMetadata(): Promise<void> {
  if (!token.value) {
    pageError.value = t('drive.publicShare.errors.linkUnavailable')
    return
  }
  loading.value = true
  saveForLaterDone.value = false
  pageError.value = ''
  clearPreviewState()
  clearFolderState()
  try {
    metadata.value = await getPublicShareMetadata(token.value)
    unlocked.value = !metadata.value.passwordProtected
    if (metadata.value.itemType === 'FOLDER') {
      currentFolderId.value = metadata.value.itemId
      if (unlocked.value) {
        await loadCurrentFolderItems()
      }
    }
  } catch (error) {
    metadata.value = null
    unlocked.value = false
    pageError.value = resolveErrorMessage(error, 'drive.publicShare.errors.metadataFailed')
  } finally {
    loading.value = false
  }
}

async function loadCurrentFolderItems(): Promise<void> {
  if (!metadata.value || metadata.value.itemType !== 'FOLDER') {
    return
  }
  folderLoading.value = true
  folderError.value = ''
  try {
    folderItems.value = await listPublicShareItems(token.value, currentFolderId.value, currentPassword.value)
  } catch (error) {
    folderItems.value = []
    folderError.value = resolveErrorMessage(error, 'drive.publicShare.errors.listFailed')
    unlocked.value = false
  } finally {
    folderLoading.value = false
  }
}

async function onDownloadRootFile(): Promise<void> {
  if (!canInteract.value) {
    ElMessage.warning(t('drive.publicShare.errors.linkUnavailable'))
    return
  }
  downloading.value = true
  try {
    const file = await downloadPublicShareFile(token.value, currentPassword.value)
    unlocked.value = true
    saveBlob(file.blob, file.fileName)
    ElMessage.success(t('drive.publicShare.messages.downloadStarted'))
  } catch (error) {
    unlocked.value = false
    ElMessage.error(resolveErrorMessage(error, 'drive.publicShare.errors.downloadFailed'))
  } finally {
    downloading.value = false
  }
}

async function previewRootFile(): Promise<void> {
  if (!canInteract.value) {
    ElMessage.warning(t('drive.publicShare.errors.linkUnavailable'))
    return
  }
  previewLoading.value = true
  clearPreviewState()
  try {
    const file = await previewPublicShareFile(token.value, currentPassword.value)
    applyPreviewResult(file, metadata.value?.itemName || displayName.value)
    unlocked.value = true
  } catch (error) {
    unlocked.value = false
    previewMessage.value = resolveErrorMessage(error, 'drive.publicShare.errors.previewFailed')
  } finally {
    previewLoading.value = false
  }
}

function applyPreviewResult(
  file: { blob: Blob; mimeType: string; truncated: boolean },
  fileName: string
): Promise<void> {
  previewMimeType.value = file.mimeType
  previewKind.value = resolvePreviewKind(file.mimeType)
  previewTruncated.value = file.truncated
  previewMessage.value = ''
  selectedFile.value = {
    id: fileName,
    parentId: null,
    itemType: 'FILE',
    name: fileName,
    mimeType: file.mimeType,
    sizeBytes: 0,
    shareCount: 0,
    createdAt: '',
    updatedAt: ''
  }
  if (previewKind.value === 'TEXT') {
    return file.blob.text().then((value) => {
      previewText.value = value
    })
  }
  if (previewKind.value === 'IMAGE' || previewKind.value === 'PDF') {
    previewBlobUrl.value = URL.createObjectURL(file.blob)
    return Promise.resolve()
  }
  previewMessage.value = t('drive.publicShare.preview.unsupported')
  return Promise.resolve()
}

async function onUnlock(): Promise<void> {
  if (!metadata.value?.passwordProtected) {
    unlocked.value = true
    if (isFolderShare.value) {
      await loadCurrentFolderItems()
      ElMessage.success(t('drive.publicShare.messages.folderLoaded'))
      return
    }
    ElMessage.success(t('drive.publicShare.messages.unlockReady'))
    return
  }
  if (!sharePassword.value.trim()) {
    ElMessage.warning(t('drive.publicShare.errors.passwordRequired'))
    return
  }
  if (isFolderShare.value) {
    await loadCurrentFolderItems()
    if (!folderError.value) {
      unlocked.value = true
      ElMessage.success(t('drive.publicShare.messages.folderLoaded'))
    }
    return
  }
  await previewRootFile()
  if (unlocked.value) {
    ElMessage.success(t('drive.publicShare.messages.unlockReady'))
  }
}

function clearPassword(): void {
  sharePassword.value = ''
  unlocked.value = !metadata.value?.passwordProtected
  clearPreviewState()
  if (isFolderShare.value) {
    clearFolderState()
    if (unlocked.value) {
      void loadCurrentFolderItems()
    }
  }
}

async function onPreviewItem(item: DriveItem): Promise<void> {
  previewLoading.value = true
  clearPreviewState()
  try {
    const file = await previewPublicShareItem(token.value, item.id, currentPassword.value)
    await applyPreviewResult(file, item.name)
    selectedFile.value = item
  } catch (error) {
    previewMessage.value = resolveErrorMessage(error, 'drive.publicShare.errors.previewFailed')
  } finally {
    previewLoading.value = false
  }
}

async function onDownloadItem(item: DriveItem): Promise<void> {
  downloading.value = true
  try {
    const file = await downloadPublicShareItem(token.value, item.id, currentPassword.value)
    saveBlob(file.blob, file.fileName)
    ElMessage.success(t('drive.publicShare.messages.downloadStarted'))
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'drive.publicShare.errors.downloadFailed'))
  } finally {
    downloading.value = false
  }
}

async function openFolder(item: DriveItem): Promise<void> {
  currentFolderId.value = item.id
  folderTrail.value = [...folderTrail.value, item]
  clearPreviewState()
  await loadCurrentFolderItems()
}

async function openPath(index: number): Promise<void> {
  if (!metadata.value) {
    return
  }
  if (index === 0) {
    folderTrail.value = []
    currentFolderId.value = metadata.value.itemId
  } else {
    folderTrail.value = folderTrail.value.slice(0, index)
    currentFolderId.value = folderTrail.value[index - 1]?.id || metadata.value.itemId
  }
  clearPreviewState()
  await loadCurrentFolderItems()
}

function pickUploadFile(): void {
  uploadInput.value?.click()
}

async function onUploadChange(event: Event): Promise<void> {
  const target = event.target as HTMLInputElement | null
  const file = target?.files?.[0]
  if (!file) {
    return
  }
  uploadLoading.value = true
  try {
    await uploadPublicShareFile(token.value, file, currentFolderId.value, currentPassword.value)
    ElMessage.success(t('drive.publicShare.messages.uploaded'))
    await loadCurrentFolderItems()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'drive.publicShare.errors.uploadFailed'))
  } finally {
    if (target) {
      target.value = ''
    }
    uploadLoading.value = false
  }
}

async function onSaveForLater(): Promise<void> {
  if (!isAuthenticated.value) {
    await navigateTo('/login')
    return
  }
  saveForLaterLoading.value = true
  try {
    await saveSharedWithMe(token.value, currentPassword.value)
    saveForLaterDone.value = true
    ElMessage.success(t('drive.publicShare.messages.savedForLater'))
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'drive.sharedWithMe.messages.saveFailed'))
  } finally {
    saveForLaterLoading.value = false
  }
}

async function openSharedWithMeWorkspace(): Promise<void> {
  await navigateTo('/drive?view=shared-with-me')
}

async function onSignInToSave(): Promise<void> {
  await navigateTo('/login')
}

watch(
  token,
  () => {
    void loadMetadata()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  clearPreviewState()
})
</script>

<template>
  <div class="public-share-page">
    <section class="public-share-shell mm-card">
      <el-skeleton :loading="loading" animated :rows="8">
        <template #default>
          <el-alert v-if="pageError" :title="pageError" type="error" :closable="false" show-icon class="page-alert" />

          <template v-else-if="metadata">
            <header class="public-share-head">
              <div>
                <p class="eyebrow">{{ t('drive.publicShare.eyebrow') }}</p>
                <h1>{{ displayName }}</h1>
                <p class="description">{{ pageSubtitle }}</p>
              </div>
              <div class="head-tags">
                <el-tag :type="statusType" effect="plain">{{ t(getDriveShareStatusI18nKey(metadata.status)) }}</el-tag>
                <el-tag :type="protectionType" effect="plain">
                  {{ t(getDriveShareProtectionI18nKey(metadata.passwordProtected)) }}
                </el-tag>
                <el-tag effect="plain">{{ pageTitle }}</el-tag>
              </div>
            </header>

            <div class="meta-grid">
              <div class="meta-row">
                <span class="meta-label">{{ isFolderShare ? t('drive.publicShare.folder.title') : t('drive.publicShare.fields.file') }}</span>
                <span class="meta-value">{{ displayName }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">{{ t('drive.publicShare.fields.type') }}</span>
                <span class="meta-value">{{ getItemTypeLabel(metadata.itemType) }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">{{ t('drive.publicShare.fields.size') }}</span>
                <span class="meta-value">{{ formatBytes(metadata.sizeBytes) }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">{{ t('drive.publicShare.fields.permission') }}</span>
                <span class="meta-value">{{ getPermissionLabel(metadata.permission) }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">{{ t('drive.publicShare.fields.status') }}</span>
                <span class="meta-value">
                  <el-tag :type="statusType" effect="plain">{{ t(getDriveShareStatusI18nKey(metadata.status)) }}</el-tag>
                </span>
              </div>
              <div class="meta-row">
                <span class="meta-label">{{ t('drive.publicShare.fields.protection') }}</span>
                <span class="meta-value">
                  <el-tag :type="protectionType" effect="plain">
                    {{ t(getDriveShareProtectionI18nKey(metadata.passwordProtected)) }}
                  </el-tag>
                </span>
              </div>
              <div class="meta-row">
                <span class="meta-label">{{ t('drive.publicShare.fields.expiresAt') }}</span>
                <span class="meta-value">{{ formatTime(metadata.expiresAt) }}</span>
              </div>
              <div class="meta-row">
                <span class="meta-label">{{ t('drive.publicShare.fields.token') }}</span>
                <span class="meta-value token-text">{{ token }}</span>
              </div>
            </div>

            <section class="save-panel mm-card">
              <div>
                <h2>{{ t('drive.sharedWithMe.saveCard.title') }}</h2>
                <p>{{ saveForLaterHint }}</p>
              </div>
              <div class="save-panel__actions">
                <el-button v-if="!isAuthenticated" type="primary" plain @click="onSignInToSave">
                  {{ t('drive.sharedWithMe.actions.signInToSave') }}
                </el-button>
                <template v-else>
                  <el-button type="primary" :loading="saveForLaterLoading" :disabled="!canSaveForLater" @click="onSaveForLater">
                    {{ saveForLaterDone ? t('drive.sharedWithMe.actions.saved') : t('drive.sharedWithMe.actions.save') }}
                  </el-button>
                  <el-button v-if="saveForLaterDone" @click="openSharedWithMeWorkspace">
                    {{ t('drive.sharedWithMe.actions.openWorkspace') }}
                  </el-button>
                </template>
              </div>
            </section>

            <section v-if="metadata.passwordProtected" class="password-panel mm-card">
              <div>
                <h2>{{ t('drive.publicShare.password.title') }}</h2>
                <p>{{ t('drive.publicShare.password.description') }}</p>
              </div>
              <div class="password-actions">
                <el-input v-model.trim="sharePassword" show-password :placeholder="t('drive.publicShare.password.placeholder')" />
                <el-button type="primary" :loading="previewLoading || folderLoading" @click="onUnlock">
                  {{ t('drive.publicShare.actions.unlock') }}
                </el-button>
                <el-button @click="clearPassword">{{ t('drive.publicShare.actions.clearPassword') }}</el-button>
              </div>
              <el-alert
                v-if="unlocked"
                :title="t(isFolderShare ? 'drive.publicShare.messages.folderLoaded' : 'drive.publicShare.password.unlocked')"
                type="success"
                :closable="false"
                show-icon
              />
            </section>

            <template v-if="isFolderShare">
              <el-alert :title="t(folderPermissionKey)" type="info" :closable="false" show-icon />

              <section class="folder-toolbar mm-card">
                <div class="path-block">
                  <span class="meta-label">{{ t('drive.publicShare.folder.path') }}</span>
                  <div class="path-trail">
                    <el-button
                      v-for="(segment, index) in pathSegments"
                      :key="segment.id"
                      text
                      type="primary"
                      @click="openPath(index)"
                    >
                      {{ segment.label }}
                    </el-button>
                  </div>
                </div>
                <div class="toolbar-actions">
                  <el-button v-if="folderTrail.length" text @click="openPath(0)">
                    {{ t('drive.publicShare.folder.backToRoot') }}
                  </el-button>
                  <el-button type="primary" :disabled="!canUpload" :loading="uploadLoading" @click="pickUploadFile">
                    {{ t('drive.publicShare.actions.upload') }}
                  </el-button>
                  <input ref="uploadInput" class="hidden-input" type="file" @change="onUploadChange">
                </div>
              </section>

              <el-alert v-if="folderError" :title="folderError" type="error" :closable="false" show-icon class="page-alert" />

              <section class="folder-grid">
                <section class="folder-list-card mm-card" v-loading="folderLoading">
                  <el-table :data="folderItems" empty-text=" " stripe>
                    <el-table-column prop="name" :label="t('drive.publicShare.folder.columns.name')" min-width="220">
                      <template #default="scope">
                        <div class="item-name">
                          <span class="item-type-pill" :class="scope.row.itemType === 'FOLDER' ? 'folder' : 'file'">
                            {{ getItemTypeLabel(scope.row.itemType) }}
                          </span>
                          <strong>{{ scope.row.name }}</strong>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column prop="itemType" :label="t('drive.publicShare.folder.columns.type')" width="110">
                      <template #default="scope">{{ getItemTypeLabel(scope.row.itemType) }}</template>
                    </el-table-column>
                    <el-table-column prop="sizeBytes" :label="t('drive.publicShare.folder.columns.size')" width="120">
                      <template #default="scope">{{ formatBytes(scope.row.sizeBytes) }}</template>
                    </el-table-column>
                    <el-table-column prop="updatedAt" :label="t('drive.publicShare.folder.columns.updatedAt')" min-width="180">
                      <template #default="scope">{{ formatTime(scope.row.updatedAt) }}</template>
                    </el-table-column>
                    <el-table-column :label="t('drive.publicShare.folder.columns.actions')" min-width="220">
                      <template #default="scope">
                        <div class="row-actions">
                          <el-button v-if="scope.row.itemType === 'FOLDER'" text type="primary" @click="openFolder(scope.row)">
                            {{ t('drive.publicShare.folder.openFolder') }}
                          </el-button>
                          <template v-else>
                            <el-button text type="primary" @click="onPreviewItem(scope.row)">
                              {{ t('drive.publicShare.actions.preview') }}
                            </el-button>
                            <el-button text type="success" @click="onDownloadItem(scope.row)">
                              {{ t('drive.publicShare.actions.download') }}
                            </el-button>
                          </template>
                        </div>
                      </template>
                    </el-table-column>
                    <template #empty>
                      <el-empty :description="t('drive.publicShare.folder.empty')" />
                    </template>
                  </el-table>
                </section>

                <section class="preview-card mm-card" v-loading="previewLoading">
                  <header class="preview-head">
                    <div>
                      <strong>{{ selectedFile?.name || t('drive.publicShare.preview.idle') }}</strong>
                      <p>{{ previewMimeType || metadataMimeType }}</p>
                    </div>
                    <el-tag v-if="previewTruncated" type="warning" size="small">
                      {{ t('drive.publicShare.preview.truncated') }}
                    </el-tag>
                  </header>
                  <pre v-if="previewKind === 'TEXT'" class="preview-text">{{ previewText }}</pre>
                  <div v-else-if="previewKind === 'IMAGE'" class="preview-image-wrap">
                    <img :src="previewBlobUrl" :alt="selectedFile?.name || displayName" class="preview-image">
                  </div>
                  <iframe
                    v-else-if="previewKind === 'PDF'"
                    :src="previewBlobUrl"
                    class="preview-pdf"
                    title="public-folder-pdf-preview"
                  />
                  <el-alert
                    v-else
                    :title="previewMessage || t('drive.publicShare.preview.idle')"
                    type="info"
                    :closable="false"
                    show-icon
                  />
                </section>
              </section>
            </template>

            <template v-else>
              <div class="actions-row">
                <el-button type="primary" :loading="previewLoading" :disabled="!canInteract" @click="previewRootFile">
                  {{ t('drive.publicShare.actions.preview') }}
                </el-button>
                <el-button type="success" :loading="downloading" :disabled="!canInteract" @click="onDownloadRootFile">
                  {{ t('drive.publicShare.actions.download') }}
                </el-button>
              </div>

              <section class="preview-card mm-card" v-loading="previewLoading">
                <header class="preview-head">
                  <div>
                    <strong>{{ displayName }}</strong>
                    <p>{{ previewMimeType || metadataMimeType }}</p>
                  </div>
                  <el-tag v-if="previewTruncated" type="warning" size="small">
                    {{ t('drive.publicShare.preview.truncated') }}
                  </el-tag>
                </header>
                <pre v-if="previewKind === 'TEXT'" class="preview-text">{{ previewText }}</pre>
                <div v-else-if="previewKind === 'IMAGE'" class="preview-image-wrap">
                  <img :src="previewBlobUrl" :alt="displayName" class="preview-image">
                </div>
                <iframe v-else-if="previewKind === 'PDF'" :src="previewBlobUrl" class="preview-pdf" title="public-pdf-preview" />
                <el-alert
                  v-else
                  :title="previewMessage || t('drive.publicShare.preview.idle')"
                  type="info"
                  :closable="false"
                  show-icon
                />
              </section>
            </template>
          </template>
        </template>
      </el-skeleton>
    </section>
  </div>
</template>

<style scoped>
.public-share-page {
  padding: clamp(16px, 3vw, 32px);
}

.public-share-shell {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  background:
    radial-gradient(circle at top left, rgba(108, 124, 255, 0.12), transparent 28%),
    radial-gradient(circle at bottom right, rgba(34, 197, 94, 0.12), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 249, 255, 0.96));
}

.public-share-head,
.password-actions,
.actions-row,
.preview-head,
.folder-toolbar,
.toolbar-actions,
.path-trail,
.row-actions {
  display: flex;
  gap: 12px;
}

.public-share-head,
.preview-head,
.folder-toolbar {
  justify-content: space-between;
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #5d67f1;
}

.public-share-head h1,
.description,
.password-panel h2,
.password-panel p,
.preview-head p {
  margin: 0;
}

.description,
.preview-head p,
.page-alert,
.meta-label {
  color: var(--mm-muted);
}

.head-tags,
.save-panel,
.save-panel__actions,
.password-panel,
.password-actions,
.toolbar-actions,
.path-trail,
.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.meta-grid,
.preview-card,
.save-panel,
.password-panel,
.folder-toolbar,
.folder-list-card {
  border: 1px solid rgba(109, 122, 255, 0.14);
  border-radius: 18px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.82);
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
}

.meta-row {
  display: grid;
  gap: 6px;
}

.meta-value,
.token-text {
  color: var(--mm-text);
  word-break: break-all;
}

.path-block {
  display: grid;
  gap: 8px;
}

.folder-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  gap: 18px;
}

.item-name {
  display: flex;
  align-items: center;
  gap: 10px;
}

.item-type-pill {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.item-type-pill.folder {
  background: rgba(93, 103, 241, 0.12);
  color: #4f46e5;
}

.item-type-pill.file {
  background: rgba(15, 118, 110, 0.12);
  color: #0f766e;
}

.preview-card {
  min-height: 320px;
}

.preview-text {
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  max-height: 420px;
  overflow: auto;
}

.preview-image-wrap {
  display: flex;
  justify-content: center;
}

.preview-image,
.preview-pdf {
  max-width: 100%;
  width: 100%;
  border-radius: 12px;
}

.preview-pdf {
  min-height: 420px;
  border: none;
}

.hidden-input {
  display: none;
}

@media (max-width: 900px) {
  .meta-grid,
  .folder-grid {
    grid-template-columns: 1fr;
  }
}
</style>
