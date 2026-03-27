import { ElMessage, ElMessageBox } from 'element-plus'
import type { createBusinessWorkspaceCore } from '~/composables/useBusinessWorkspaceCore'
import { useI18n } from '~/composables/useI18n'
import type { OrgTeamSpaceAccessRole, OrgTeamSpaceItem, OrgTeamSpaceTrashItem } from '~/types/business'

type BusinessWorkspaceCore = ReturnType<typeof createBusinessWorkspaceCore>

export function useBusinessWorkspaceActions(workspace: BusinessWorkspaceCore) {
  const { t } = useI18n()

  function openCreateTeamSpaceDialog(): void {
    workspace.createTeamSpaceDialogVisible.value = true
  }

  function resetCreateTeamSpaceForm(): void {
    workspace.createTeamSpaceForm.name = ''
    workspace.createTeamSpaceForm.description = ''
    workspace.createTeamSpaceForm.storageLimitMb = 10240
  }

  async function onCreateTeamSpace(): Promise<void> {
    if (!workspace.selectedOrgId.value) {
      ElMessage.warning(t('business.messages.selectOrganizationFirst'))
      return
    }
    const name = workspace.createTeamSpaceForm.name.trim()
    if (!name) {
      ElMessage.warning(t('business.messages.teamSpaceNameRequired'))
      return
    }
    workspace.loading.createTeamSpace = true
    try {
      const created = await workspace.createTeamSpace(workspace.selectedOrgId.value, {
        name,
        description: workspace.createTeamSpaceForm.description.trim() || undefined,
        storageLimitMb: workspace.createTeamSpaceForm.storageLimitMb
      })
      workspace.createTeamSpaceDialogVisible.value = false
      resetCreateTeamSpaceForm()
      ElMessage.success(t('business.messages.teamSpaceCreated'))
      await workspace.loadBusinessWorkspace(workspace.selectedOrgId.value, created.id)
    } catch (error) {
      showError(error, t('business.errors.createTeamSpace'))
    } finally {
      workspace.loading.createTeamSpace = false
    }
  }

  function openCreateFolderDialog(): void {
    if (!workspace.canWriteCurrentSpace.value) {
      ElMessage.warning(workspace.readOnlyReason.value || t('business.messages.teamSpaceReadOnly'))
      return
    }
    workspace.createFolderDialogVisible.value = true
  }

  function resetCreateFolderForm(): void {
    workspace.createFolderForm.name = ''
  }

  async function onCreateFolder(): Promise<void> {
    if (!workspace.selectedOrgId.value || !workspace.selectedTeamSpaceId.value || !workspace.canWriteCurrentSpace.value) {
      return
    }
    const name = workspace.createFolderForm.name.trim()
    if (!name) {
      ElMessage.warning(t('business.messages.folderNameRequired'))
      return
    }
    workspace.loading.createFolder = true
    try {
      await workspace.createTeamSpaceFolder(workspace.selectedOrgId.value, workspace.selectedTeamSpaceId.value, {
        name,
        parentId: workspace.currentParentId.value
      })
      workspace.createFolderDialogVisible.value = false
      resetCreateFolderForm()
      ElMessage.success(t('business.messages.folderCreated'))
      await workspace.onRefreshWorkspace()
    } catch (error) {
      showError(error, t('business.errors.createFolder'))
    } finally {
      workspace.loading.createFolder = false
    }
  }

  async function onUploadFileSelected(file: File | null): Promise<void> {
    if (!file || !workspace.selectedOrgId.value || !workspace.selectedTeamSpaceId.value || !workspace.canWriteCurrentSpace.value) {
      return
    }
    workspace.loading.upload = true
    try {
      await workspace.uploadTeamSpaceFile(
        workspace.selectedOrgId.value,
        workspace.selectedTeamSpaceId.value,
        file,
      workspace.currentParentId.value
      )
      ElMessage.success(t('business.messages.fileUploaded'))
      await workspace.onRefreshWorkspace()
    } catch (error) {
      showError(error, t('business.errors.upload'))
    } finally {
      workspace.loading.upload = false
    }
  }

  async function onDownloadItem(item: OrgTeamSpaceItem): Promise<void> {
    if (!workspace.selectedOrgId.value || !workspace.selectedTeamSpaceId.value || item.itemType !== 'FILE') {
      return
    }
    workspace.loading.downloadItemId = item.id
    try {
      const file = await workspace.downloadTeamSpaceFile(workspace.selectedOrgId.value, workspace.selectedTeamSpaceId.value, item.id)
      saveDownloadedFile(file.blob, file.fileName)
    } catch (error) {
      showError(error, t('business.errors.download'))
    } finally {
      workspace.loading.downloadItemId = ''
    }
  }

  async function onDeleteItem(item: OrgTeamSpaceItem): Promise<void> {
    if (!workspace.selectedOrgId.value || !workspace.selectedTeamSpaceId.value || !workspace.canWriteCurrentSpace.value) {
      return
    }
    try {
      await ElMessageBox.confirm(
        t('business.confirm.deleteItemMessage', { name: item.name }),
        t('business.confirm.deleteItemTitle'),
        { type: 'warning' }
      )
      await workspace.deleteTeamSpaceItem(workspace.selectedOrgId.value, workspace.selectedTeamSpaceId.value, item.id)
      ElMessage.success(t('business.messages.itemMovedToTrash'))
      await workspace.onRefreshWorkspace()
    } catch (error) {
      if (isUserCancel(error)) {
        return
      }
      showError(error, t('business.errors.deleteItem'))
    }
  }

  async function openVersionDrawer(item: OrgTeamSpaceItem): Promise<void> {
    workspace.versionTargetItem.value = item
    workspace.versionDrawerVisible.value = true
    await workspace.loadVersions(item.id)
  }

  async function onUploadVersion(file: File): Promise<void> {
    if (!workspace.selectedOrgId.value || !workspace.selectedTeamSpaceId.value || !workspace.versionTargetItem.value || !workspace.canWriteCurrentSpace.value) {
      return
    }
    workspace.loading.versionUpload = true
    try {
      await workspace.uploadTeamSpaceFileVersion(
        workspace.selectedOrgId.value,
        workspace.selectedTeamSpaceId.value,
        workspace.versionTargetItem.value.id,
        file
      )
      ElMessage.success(t('business.messages.newVersionUploaded'))
      await Promise.all([
        workspace.onRefreshWorkspace(),
        workspace.loadVersions(workspace.versionTargetItem.value.id),
        workspace.loadActivity()
      ])
    } catch (error) {
      showError(error, t('business.errors.uploadVersion'))
    } finally {
      workspace.loading.versionUpload = false
    }
  }

  async function onRestoreVersion(versionId: string): Promise<void> {
    if (!workspace.selectedOrgId.value || !workspace.selectedTeamSpaceId.value || !workspace.versionTargetItem.value || !workspace.canManageCurrentSpace.value) {
      return
    }
    workspace.loading.versionMutationId = versionId
    try {
      await workspace.restoreTeamSpaceFileVersion(
        workspace.selectedOrgId.value,
        workspace.selectedTeamSpaceId.value,
        workspace.versionTargetItem.value.id,
        versionId
      )
      ElMessage.success(t('business.messages.versionRestored'))
      await Promise.all([
        workspace.onRefreshWorkspace(),
        workspace.loadVersions(workspace.versionTargetItem.value.id),
        workspace.loadActivity()
      ])
    } catch (error) {
      showError(error, t('business.errors.restoreVersion'))
    } finally {
      workspace.loading.versionMutationId = ''
    }
  }

  async function openTrashDrawer(): Promise<void> {
    workspace.trashDrawerVisible.value = true
    await workspace.loadTrashItems()
  }

  async function onRestoreTrashItem(item: OrgTeamSpaceTrashItem): Promise<void> {
    if (!workspace.selectedOrgId.value || !workspace.selectedTeamSpaceId.value || !workspace.canManageCurrentSpace.value) {
      return
    }
    workspace.loading.trashMutationId = item.id
    try {
      await workspace.restoreTeamSpaceTrashItem(workspace.selectedOrgId.value, workspace.selectedTeamSpaceId.value, item.id)
      ElMessage.success(t('business.messages.trashItemRestored'))
      await Promise.all([workspace.onRefreshWorkspace(), workspace.loadTrashItems()])
    } catch (error) {
      showError(error, t('business.errors.restoreTrash'))
    } finally {
      workspace.loading.trashMutationId = ''
    }
  }

  async function onPurgeTrashItem(item: OrgTeamSpaceTrashItem): Promise<void> {
    if (!workspace.selectedOrgId.value || !workspace.selectedTeamSpaceId.value || !workspace.canManageCurrentSpace.value) {
      return
    }
    try {
      await ElMessageBox.confirm(
        t('business.confirm.purgeItemMessage', { name: item.name }),
        t('business.confirm.purgeItemTitle'),
        { type: 'warning' }
      )
      workspace.loading.trashMutationId = item.id
      await workspace.purgeTeamSpaceTrashItem(workspace.selectedOrgId.value, workspace.selectedTeamSpaceId.value, item.id)
      ElMessage.success(t('business.messages.trashItemPurged'))
      await Promise.all([workspace.onRefreshWorkspace(), workspace.loadTrashItems()])
    } catch (error) {
      if (isUserCancel(error)) {
        return
      }
      showError(error, t('business.errors.purgeTrash'))
    } finally {
      workspace.loading.trashMutationId = ''
    }
  }

  async function onActivityCategoryChange(value: BusinessWorkspaceCore['activityCategory']['value']): Promise<void> {
    workspace.activityCategory.value = value
    await workspace.loadActivity()
  }

  async function onAddMember(): Promise<void> {
    if (!workspace.selectedOrgId.value || !workspace.selectedTeamSpaceId.value || !workspace.canManageCurrentSpace.value) {
      return
    }
    if (!workspace.addMemberForm.userEmail.trim()) {
      ElMessage.warning(t('business.messages.selectMemberFirst'))
      return
    }
    workspace.loading.addMember = true
    try {
      await workspace.addTeamSpaceMember(workspace.selectedOrgId.value, workspace.selectedTeamSpaceId.value, {
        userEmail: workspace.addMemberForm.userEmail.trim(),
        role: workspace.addMemberForm.role
      })
      workspace.addMemberForm.userEmail = ''
      workspace.addMemberForm.role = 'VIEWER'
      ElMessage.success(t('business.messages.memberAdded'))
      await Promise.all([workspace.loadMembers(), workspace.loadActivity(), workspace.onRefreshWorkspace()])
    } catch (error) {
      showError(error, t('business.errors.addMember'))
    } finally {
      workspace.loading.addMember = false
    }
  }

  async function onUpdateMemberRole(memberId: string, role: OrgTeamSpaceAccessRole): Promise<void> {
    if (!workspace.selectedOrgId.value || !workspace.selectedTeamSpaceId.value || !workspace.canManageCurrentSpace.value) {
      return
    }
    workspace.loading.memberMutationId = memberId
    try {
      await workspace.updateTeamSpaceMemberRole(workspace.selectedOrgId.value, workspace.selectedTeamSpaceId.value, memberId, { role })
      ElMessage.success(t('business.messages.memberRoleUpdated'))
      await Promise.all([workspace.loadMembers(), workspace.loadActivity(), workspace.onRefreshWorkspace()])
    } catch (error) {
      showError(error, t('business.errors.updateMemberRole'))
    } finally {
      workspace.loading.memberMutationId = ''
    }
  }

  async function onRemoveMember(memberId: string, userEmail: string): Promise<void> {
    if (!workspace.selectedOrgId.value || !workspace.selectedTeamSpaceId.value || !workspace.canManageCurrentSpace.value) {
      return
    }
    try {
      await ElMessageBox.confirm(
        t('business.confirm.removeMemberMessage', { email: userEmail }),
        t('business.confirm.removeMemberTitle'),
        { type: 'warning' }
      )
      workspace.loading.memberMutationId = memberId
      await workspace.removeTeamSpaceMember(workspace.selectedOrgId.value, workspace.selectedTeamSpaceId.value, memberId)
      ElMessage.success(t('business.messages.memberRemoved'))
      await Promise.all([workspace.loadMembers(), workspace.loadActivity(), workspace.onRefreshWorkspace()])
    } catch (error) {
      if (isUserCancel(error)) {
        return
      }
      showError(error, t('business.errors.removeMember'))
    } finally {
      workspace.loading.memberMutationId = ''
    }
  }

  return {
    openCreateTeamSpaceDialog,
    resetCreateTeamSpaceForm,
    onCreateTeamSpace,
    openCreateFolderDialog,
    resetCreateFolderForm,
    onCreateFolder,
    onUploadFileSelected,
    onDownloadItem,
    onDeleteItem,
    openVersionDrawer,
    onUploadVersion,
    onRestoreVersion,
    openTrashDrawer,
    onRestoreTrashItem,
    onPurgeTrashItem,
    onActivityCategoryChange,
    onAddMember,
    onUpdateMemberRole,
    onRemoveMember
  }
}

function showError(error: unknown, fallback: string): void {
  ElMessage.error((error as Error).message || fallback)
}

function saveDownloadedFile(blob: Blob, fileName: string): void {
  const blobUrl = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = blobUrl
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  URL.revokeObjectURL(blobUrl)
}

function isUserCancel(error: unknown): boolean {
  return String(error || '').toLowerCase().includes('cancel')
}
