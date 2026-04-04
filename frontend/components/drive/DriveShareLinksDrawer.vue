<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDriveApi } from '~/composables/useDriveApi'
import { useDriveFileE2ee } from '~/composables/useDriveFileE2ee'
import { useI18n } from '~/composables/useI18n'
import type { DriveCollaboratorShare, DriveItem, DriveShareLink, DriveSharePermission } from '~/types/api'
import { getDriveCollaboratorStatusI18nKey } from '~/utils/drive-collaboration'
import { getDriveShareProtectionI18nKey, getDriveShareStatusI18nKey } from '~/utils/drive-share'

const props = defineProps<{
  modelValue: boolean
  item: DriveItem | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  changed: []
}>()

const { t } = useI18n()
const {
  listShares,
  createShare,
  createEncryptedPublicShare,
  updateShare,
  revokeShare,
  downloadFile,
  listCollaboratorShares,
  createCollaboratorShare,
  updateCollaboratorShare,
  removeCollaboratorShare
} = useDriveApi()
const { encryptPublicShareFile } = useDriveFileE2ee()

const drawerVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
const isEncryptedSingleFile = computed(() => Boolean(
  props.item
  && props.item.itemType === 'FILE'
  && props.item.e2ee?.enabled,
))
const collaboratorSharingSupported = computed(() => !isEncryptedSingleFile.value)
const publicShareHintKey = computed(() => (
  isEncryptedSingleFile.value
    ? 'drive.shareDrawer.e2ee.publicHint'
    : 'drive.shareDrawer.passwordHint'
))

const publicSharesLoading = ref(false)
const collaboratorLoading = ref(false)
const publicMutating = ref(false)
const collaboratorSubmitting = ref(false)
const collaboratorMutationId = ref('')
const manageDialogVisible = ref(false)

const shareList = ref<DriveShareLink[]>([])
const collaboratorList = ref<DriveCollaboratorShare[]>([])

const shareForm = reactive({
  permission: 'VIEW' as DriveSharePermission,
  expiresAt: '',
  password: ''
})

const collaboratorForm = reactive({
  email: '',
  permission: 'VIEW' as DriveSharePermission
})

const manageForm = reactive({
  shareId: '',
  permission: 'VIEW' as DriveSharePermission,
  expiresAt: '',
  password: '',
  clearPassword: false
})

const activeManageShare = computed(() => shareList.value.find((share) => share.id === manageForm.shareId) || null)

watch(
  () => [props.modelValue, props.item?.id] as const,
  ([visible, itemId]) => {
    if (!visible || !itemId) {
      return
    }
    resetPublicShareForm()
    resetCollaboratorForm()
    void loadAllShareData()
  },
  { immediate: true }
)

watch(
  isEncryptedSingleFile,
  (encrypted) => {
    if (encrypted) {
      shareForm.permission = 'VIEW'
    }
  },
  { immediate: true },
)

function resetPublicShareForm(): void {
  shareForm.permission = 'VIEW'
  shareForm.expiresAt = ''
  shareForm.password = ''
}

function resetCollaboratorForm(): void {
  collaboratorForm.email = ''
  collaboratorForm.permission = 'VIEW'
}

function resetManageForm(): void {
  manageForm.shareId = ''
  manageForm.permission = 'VIEW'
  manageForm.expiresAt = ''
  manageForm.password = ''
  manageForm.clearPassword = false
}

function formatTime(value: string | null): string {
  return value || t('common.none')
}

function buildPublicShareUrl(token: string): string {
  if (typeof window !== 'undefined') {
    return `${window.location.origin}/public/drive/shares/${token}`
  }
  return `/public/drive/shares/${token}`
}

function getPublicShareStatusLabel(status: string): string {
  return t(getDriveShareStatusI18nKey(status))
}

function getPublicShareStatusTagType(status: string): 'success' | 'info' {
  return status === 'ACTIVE' ? 'success' : 'info'
}

function getProtectionLabel(passwordProtected: boolean): string {
  return t(getDriveShareProtectionI18nKey(passwordProtected))
}

function getProtectionTagType(passwordProtected: boolean): 'warning' | 'info' {
  return passwordProtected ? 'warning' : 'info'
}

function getCollaboratorStatusLabel(status: string): string {
  return t(getDriveCollaboratorStatusI18nKey(status))
}

function getCollaboratorStatusTagType(status: string): 'warning' | 'success' | 'info' | 'danger' {
  if (status === 'NEEDS_ACTION') {
    return 'warning'
  }
  if (status === 'ACCEPTED') {
    return 'success'
  }
  if (status === 'DECLINED') {
    return 'info'
  }
  return 'danger'
}

async function loadAllShareData(): Promise<void> {
  await loadPublicShares()
  if (collaboratorSharingSupported.value) {
    await loadCollaboratorShareList()
    return
  }
  collaboratorList.value = []
}

async function loadPublicShares(): Promise<void> {
  if (!props.item) {
    return
  }
  publicSharesLoading.value = true
  try {
    shareList.value = await listShares(props.item.id)
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.loadSharesFailed'))
  } finally {
    publicSharesLoading.value = false
  }
}

async function loadCollaboratorShareList(): Promise<void> {
  if (!props.item) {
    return
  }
  collaboratorLoading.value = true
  try {
    collaboratorList.value = await listCollaboratorShares(props.item.id)
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.collaboration.owner.messages.loadFailed'))
  } finally {
    collaboratorLoading.value = false
  }
}

async function copyToClipboard(value: string, successKey: string): Promise<void> {
  if (typeof navigator === 'undefined' || !navigator.clipboard) {
    ElMessage.warning(t('drive.messages.clipboardUnavailable'))
    return
  }
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success(t(successKey))
  } catch {
    ElMessage.error(t('drive.messages.copyFailed'))
  }
}

async function onCreateCollaboratorInvite(): Promise<void> {
  if (!props.item || !collaboratorForm.email.trim()) {
    return
  }
  if (!collaboratorSharingSupported.value) {
    ElMessage.warning(t('drive.shareDrawer.e2ee.collaboratorUnavailable'))
    return
  }
  collaboratorSubmitting.value = true
  try {
    await createCollaboratorShare(props.item.id, {
      targetEmail: collaboratorForm.email.trim(),
      permission: collaboratorForm.permission
    })
    resetCollaboratorForm()
    ElMessage.success(t('drive.collaboration.owner.messages.invited'))
    await loadCollaboratorShareList()
    emit('changed')
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.collaboration.owner.messages.inviteFailed'))
  } finally {
    collaboratorSubmitting.value = false
  }
}

async function onUpdateCollaboratorPermission(shareId: string, permission: DriveSharePermission): Promise<void> {
  if (!props.item) {
    return
  }
  collaboratorMutationId.value = shareId
  try {
    await updateCollaboratorShare(props.item.id, shareId, { permission })
    ElMessage.success(t('drive.collaboration.owner.messages.permissionUpdated'))
    await loadCollaboratorShareList()
    emit('changed')
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.collaboration.owner.messages.permissionUpdateFailed'))
  } finally {
    collaboratorMutationId.value = ''
  }
}

async function onRemoveCollaboratorInvite(shareId: string): Promise<void> {
  if (!props.item) {
    return
  }
  collaboratorMutationId.value = shareId
  try {
    await removeCollaboratorShare(props.item.id, shareId)
    ElMessage.success(t('drive.collaboration.owner.messages.revoked'))
    await loadCollaboratorShareList()
    emit('changed')
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.collaboration.owner.messages.revokeFailed'))
  } finally {
    collaboratorMutationId.value = ''
  }
}

function openManageDialog(share: DriveShareLink): void {
  if (isManageDisabled(share)) {
    ElMessage.warning(t('drive.shareDrawer.e2ee.manageUnavailable'))
    return
  }
  manageForm.shareId = share.id
  manageForm.permission = share.permission
  manageForm.expiresAt = share.expiresAt || ''
  manageForm.password = ''
  manageForm.clearPassword = false
  manageDialogVisible.value = true
}

function isManageDisabled(share: DriveShareLink): boolean {
  return share.status !== 'ACTIVE' || Boolean(share.e2ee?.enabled)
}

function normalizeSharePassword(): string {
  const value = shareForm.password.trim()
  if (!value) {
    throw new Error(t('drive.shareDrawer.e2ee.passwordRequired'))
  }
  return value
}

function ensureEncryptedSharePermission(): void {
  if (shareForm.permission !== 'VIEW') {
    throw new Error(t('drive.shareDrawer.e2ee.onlyViewAllowed'))
  }
}

async function requestOwnerPassphrase(): Promise<string> {
  const { value } = await ElMessageBox.prompt(
    t('drive.messages.e2eePassphrasePromptInput'),
    t('drive.messages.e2eePassphrasePromptTitle'),
    {
      confirmButtonText: t('common.actions.confirm'),
      cancelButtonText: t('common.actions.cancel'),
      inputType: 'password',
      inputValidator: (rawValue: string) => {
        if (rawValue.trim()) {
          return true
        }
        return t('drive.messages.e2eePassphraseRequired')
      },
    },
  )
  return value.trim()
}

function isPromptCancelled(error: unknown): boolean {
  return error === 'cancel' || error === 'close'
}

async function createStandardPublicShare(itemId: string): Promise<void> {
  await createShare(itemId, {
    permission: shareForm.permission,
    expiresAt: shareForm.expiresAt || undefined,
    password: shareForm.password.trim() || undefined,
  })
}

async function createEncryptedPublicShareLink(item: DriveItem): Promise<void> {
  ensureEncryptedSharePermission()
  const sharePassword = normalizeSharePassword()
  const ownerPassphrase = await requestOwnerPassphrase()
  const ownerCiphertext = await downloadFile(item.id)
  const reEncryptedShare = await encryptPublicShareFile(ownerCiphertext, item, ownerPassphrase, sharePassword)
  await createEncryptedPublicShare(item.id, {
    permission: 'VIEW',
    expiresAt: shareForm.expiresAt || undefined,
    password: sharePassword,
    encryptedFile: reEncryptedShare.file,
    e2ee: reEncryptedShare.e2ee,
  })
}

async function onCreatePublicShare(): Promise<void> {
  if (!props.item) {
    return
  }
  publicMutating.value = true
  try {
    if (isEncryptedSingleFile.value) {
      await createEncryptedPublicShareLink(props.item)
    } else {
      await createStandardPublicShare(props.item.id)
    }
    resetPublicShareForm()
    ElMessage.success(t('drive.messages.shareCreated'))
    await loadPublicShares()
    emit('changed')
  } catch (error) {
    if (isPromptCancelled(error)) {
      return
    }
    ElMessage.error((error as Error).message || t('drive.messages.shareCreateFailed'))
  } finally {
    publicMutating.value = false
  }
}

async function onUpdatePublicShare(): Promise<void> {
  if (!manageForm.shareId) {
    return
  }
  if (activeManageShare.value?.e2ee?.enabled) {
    ElMessage.warning(t('drive.shareDrawer.e2ee.manageUnavailable'))
    return
  }
  publicMutating.value = true
  try {
    await updateShare(manageForm.shareId, {
      permission: manageForm.permission,
      expiresAt: manageForm.expiresAt || null,
      password: manageForm.password.trim() || undefined,
      clearPassword: manageForm.clearPassword || undefined
    })
    manageDialogVisible.value = false
    resetManageForm()
    ElMessage.success(t('drive.messages.shareUpdated'))
    await loadPublicShares()
    emit('changed')
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.shareUpdateFailed'))
  } finally {
    publicMutating.value = false
  }
}

async function onRevokePublicShare(share: DriveShareLink): Promise<void> {
  publicMutating.value = true
  try {
    await revokeShare(share.id)
    ElMessage.success(t('drive.messages.shareRevoked'))
    await loadPublicShares()
    emit('changed')
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.shareRevokeFailed'))
  } finally {
    publicMutating.value = false
  }
}

function onDrawerClosed(): void {
  shareList.value = []
  collaboratorList.value = []
  resetPublicShareForm()
  resetCollaboratorForm()
  resetManageForm()
  manageDialogVisible.value = false
}
</script>

<template>
  <el-drawer
    v-model="drawerVisible"
    :title="t('drive.shareDrawer.title')"
    size="900px"
    @closed="onDrawerClosed"
  >
    <div v-if="item" class="share-panel">
      <div class="share-hero mm-card">
        <div>
          <p class="share-hero__eyebrow">{{ t('drive.shareDrawer.title') }}</p>
          <h3>{{ item.name }}</h3>
          <p class="share-hero__meta">{{ t('drive.shareDrawer.item', { name: item.name, type: item.itemType === 'FILE' ? t('drive.search.types.file') : t('drive.search.types.folder') }) }}</p>
        </div>
        <div class="share-hero__tags">
          <el-tag type="success" effect="plain">{{ t('drive.collaboration.owner.badge') }}</el-tag>
          <el-tag type="info" effect="dark">{{ t('drive.shareDrawer.publicBadge') }}</el-tag>
        </div>
      </div>

      <div v-if="collaboratorSharingSupported" class="share-create mm-card">
        <div class="share-section__head">
          <div>
            <p class="share-section__eyebrow">{{ t('drive.collaboration.owner.badge') }}</p>
            <h4>{{ t('drive.collaboration.owner.title') }}</h4>
            <p class="share-create__hint">{{ t('drive.collaboration.owner.description') }}</p>
          </div>
          <el-tag type="success" effect="plain">{{ collaboratorList.length }}</el-tag>
        </div>

        <div class="share-create__fields share-create__fields--owner">
          <el-input
            v-model.trim="collaboratorForm.email"
            :placeholder="t('drive.collaboration.owner.emailPlaceholder')"
          />
          <el-select v-model="collaboratorForm.permission">
            <el-option :label="t('docs.share.view')" value="VIEW" />
            <el-option :label="t('docs.share.edit')" value="EDIT" />
          </el-select>
          <el-button type="primary" :loading="collaboratorSubmitting" @click="onCreateCollaboratorInvite">
            {{ t('drive.collaboration.owner.actions.invite') }}
          </el-button>
        </div>

        <el-empty
          v-if="!collaboratorLoading && collaboratorList.length === 0"
          :description="t('drive.collaboration.owner.empty')"
          :image-size="64"
        />

        <el-table v-else :data="collaboratorList" v-loading="collaboratorLoading" row-key="shareId" style="width: 100%">
          <el-table-column :label="t('drive.collaboration.owner.columns.collaborator')" min-width="220">
            <template #default="scope">
              <div class="collaborator-cell">
                <strong>{{ scope.row.collaboratorDisplayName || scope.row.collaboratorEmail }}</strong>
                <small>{{ scope.row.collaboratorEmail }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="t('drive.collaboration.owner.columns.permission')" width="160">
            <template #default="scope">
              <el-select
                :model-value="scope.row.permission"
                :disabled="scope.row.responseStatus === 'REVOKED'"
                @change="onUpdateCollaboratorPermission(scope.row.shareId, $event)"
              >
                <el-option :label="t('docs.share.view')" value="VIEW" />
                <el-option :label="t('docs.share.edit')" value="EDIT" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column :label="t('drive.collaboration.owner.columns.status')" width="140">
            <template #default="scope">
              <el-tag :type="getCollaboratorStatusTagType(scope.row.responseStatus)" effect="plain">
                {{ getCollaboratorStatusLabel(scope.row.responseStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('drive.collaboration.owner.columns.updatedAt')" min-width="170">
            <template #default="scope">
              <span>{{ formatTime(scope.row.updatedAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('drive.table.columns.actions')" width="140" fixed="right">
            <template #default="scope">
              <el-button
                type="danger"
                text
                :loading="collaboratorMutationId === scope.row.shareId"
                :disabled="scope.row.responseStatus === 'REVOKED'"
                @click="onRemoveCollaboratorInvite(scope.row.shareId)"
              >
                {{ t('drive.collaboration.owner.actions.revoke') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-else class="share-create mm-card" data-testid="drive-share-collaborator-disabled">
        <div class="share-section__head">
          <div>
            <p class="share-section__eyebrow">{{ t('drive.collaboration.owner.badge') }}</p>
            <h4>{{ t('drive.collaboration.owner.title') }}</h4>
          </div>
          <el-tag type="warning" effect="plain">{{ t('drive.shareDrawer.e2ee.collaboratorBadge') }}</el-tag>
        </div>
        <el-alert
          :title="t('drive.shareDrawer.e2ee.collaboratorUnavailable')"
          type="warning"
          :closable="false"
          show-icon
        />
      </div>

      <div class="share-create mm-card">
        <div class="share-section__head">
          <div>
            <p class="share-section__eyebrow">{{ t('drive.shareDrawer.publicBadge') }}</p>
            <h4>{{ t('drive.shareDrawer.publicTitle') }}</h4>
            <p class="share-create__hint">{{ t(publicShareHintKey) }}</p>
          </div>
        </div>
        <el-alert
          v-if="isEncryptedSingleFile"
          :title="t('drive.shareDrawer.e2ee.onlyViewAllowed')"
          type="info"
          :closable="false"
          show-icon
        />

        <div class="share-create__fields">
          <el-select v-model="shareForm.permission">
            <el-option :label="t('docs.share.view')" value="VIEW" />
            <el-option :label="t('docs.share.edit')" value="EDIT" :disabled="isEncryptedSingleFile" />
          </el-select>
          <el-date-picker
            v-model="shareForm.expiresAt"
            type="datetime"
            :placeholder="t('drive.shareDrawer.expiresAtPlaceholder')"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
          <el-input
            v-model.trim="shareForm.password"
            data-testid="drive-share-password"
            show-password
            :placeholder="t(isEncryptedSingleFile ? 'drive.shareDrawer.e2ee.passwordPlaceholder' : 'drive.shareDrawer.passwordPlaceholder')"
          >
            <template #prepend>{{ t('drive.shareDrawer.fields.password') }}</template>
          </el-input>
        </div>
        <el-button data-testid="drive-share-create-public" type="primary" :loading="publicMutating" @click="onCreatePublicShare">
          {{ t('drive.shareDrawer.createLink') }}
        </el-button>
      </div>

      <el-table :data="shareList" v-loading="publicSharesLoading" row-key="id" style="width: 100%">
        <el-table-column :label="t('drive.shareDrawer.columns.permission')" width="110">
          <template #default="scope">
            <span>{{ scope.row.permission === 'EDIT' ? t('docs.share.edit') : t('docs.share.view') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('drive.shareDrawer.columns.status')" width="128">
          <template #default="scope">
            <el-tag :type="getPublicShareStatusTagType(scope.row.status)">{{ getPublicShareStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('drive.shareDrawer.columns.protection')" width="120">
          <template #default="scope">
            <div class="share-actions">
              <el-tag :type="getProtectionTagType(scope.row.passwordProtected)" effect="plain">
                {{ getProtectionLabel(scope.row.passwordProtected) }}
              </el-tag>
              <el-tag v-if="scope.row.e2ee?.enabled" type="info" effect="plain">
                {{ t('drive.table.badges.e2ee') }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="token" :label="t('drive.shareDrawer.columns.token')" min-width="200" />
        <el-table-column :label="t('drive.shareDrawer.columns.publicLink')" min-width="260">
          <template #default="scope">
            <span class="token-url">{{ buildPublicShareUrl(scope.row.token) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('drive.shareDrawer.columns.expiresAt')" min-width="170">
          <template #default="scope">
            <span>{{ formatTime(scope.row.expiresAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('drive.table.columns.actions')" width="288" fixed="right">
          <template #default="scope">
            <div class="share-actions">
              <el-button type="primary" text @click="copyToClipboard(scope.row.token, 'drive.messages.shareTokenCopied')">
                {{ t('drive.shareDrawer.actions.copyToken') }}
              </el-button>
              <el-button type="primary" text @click="copyToClipboard(buildPublicShareUrl(scope.row.token), 'drive.messages.shareLinkCopied')">
                {{ t('drive.shareDrawer.actions.copyLink') }}
              </el-button>
              <el-button
                :data-testid="`drive-share-manage-${scope.row.id}`"
                type="primary"
                text
                :disabled="isManageDisabled(scope.row)"
                @click="openManageDialog(scope.row)"
              >
                {{ t('drive.shareDrawer.actions.manage') }}
              </el-button>
              <el-button
                type="danger"
                text
                :disabled="scope.row.status !== 'ACTIVE'"
                @click="onRevokePublicShare(scope.row)"
              >
                {{ t('drive.shareDrawer.actions.revoke') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="manageDialogVisible"
      :title="t('drive.shareDrawer.manage.title')"
      width="560px"
      append-to-body
    >
      <div class="manage-panel">
        <p class="muted">{{ t('drive.shareDrawer.manage.description') }}</p>
        <div v-if="activeManageShare" class="manage-status">
          <el-tag :type="getPublicShareStatusTagType(activeManageShare.status)">{{ getPublicShareStatusLabel(activeManageShare.status) }}</el-tag>
          <el-tag :type="getProtectionTagType(activeManageShare.passwordProtected)" effect="plain">
            {{ getProtectionLabel(activeManageShare.passwordProtected) }}
          </el-tag>
        </div>
        <div class="manage-fields">
          <el-select v-model="manageForm.permission">
            <el-option :label="t('docs.share.view')" value="VIEW" />
            <el-option :label="t('docs.share.edit')" value="EDIT" :disabled="Boolean(activeManageShare?.e2ee?.enabled)" />
          </el-select>
          <el-date-picker
            v-model="manageForm.expiresAt"
            type="datetime"
            :placeholder="t('drive.shareDrawer.expiresAtPlaceholder')"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
          <el-input
            v-model.trim="manageForm.password"
            show-password
            :disabled="manageForm.clearPassword"
            :placeholder="t('drive.shareDrawer.passwordPlaceholder')"
          >
            <template #prepend>{{ t('drive.shareDrawer.fields.password') }}</template>
          </el-input>
          <el-checkbox v-model="manageForm.clearPassword">{{ t('drive.shareDrawer.manage.clearPassword') }}</el-checkbox>
        </div>
        <p class="share-create__hint">{{ t('drive.shareDrawer.manage.passwordHint') }}</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="manageDialogVisible = false">{{ t('common.actions.cancel') }}</el-button>
          <el-button type="primary" :loading="publicMutating" @click="onUpdatePublicShare">{{ t('common.actions.saveChanges') }}</el-button>
        </span>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<style scoped>
.share-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.share-hero,
.share-create {
  border: 1px solid rgba(108, 124, 255, 0.16);
  background:
    radial-gradient(circle at top left, rgba(108, 124, 255, 0.16), transparent 38%),
    linear-gradient(145deg, rgba(248, 250, 255, 0.96), rgba(241, 245, 255, 0.9));
}

.share-hero {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 20px;
}

.share-hero__eyebrow,
.share-section__eyebrow {
  margin: 0 0 8px;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #6a73f5;
}

.share-hero h3,
.share-section__head h4,
.share-hero__meta,
.share-create__hint {
  margin: 0;
}

.share-hero h3 {
  font-size: 22px;
  color: #19233d;
}

.share-hero__meta,
.share-create__hint,
.manage-status,
.muted {
  color: var(--mm-muted);
}

.share-hero__tags,
.share-section__head,
.share-actions,
.manage-status {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.share-section__head {
  justify-content: space-between;
  align-items: flex-start;
}

.share-create {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
}

.share-create__fields,
.manage-fields {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.share-create__fields--owner {
  grid-template-columns: minmax(0, 1.45fr) 160px 120px;
}

.manage-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.token-url {
  word-break: break-all;
  color: #4753c7;
}

.collaborator-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.collaborator-cell strong,
.collaborator-cell small {
  line-height: 1.3;
}

.collaborator-cell small {
  color: var(--mm-muted);
}

@media (max-width: 960px) {
  .share-create__fields,
  .share-create__fields--owner,
  .manage-fields {
    grid-template-columns: 1fr;
  }
}
</style>
