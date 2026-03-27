<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from '~/composables/useI18n'
import { useBusinessWorkspace } from '~/composables/useBusinessWorkspace'

const { t } = useI18n()

useHead(() => ({
  title: t('page.business.title')
}))

const {
  loading,
  organizations,
  selectedOrgId,
  overview,
  teamSpaces,
  selectedTeamSpaceId,
  items,
  trail,
  keyword,
  itemTypeFilter,
  teamSpaceMembers,
  activityItems,
  activityCategory,
  versionTargetItem,
  versionItems,
  trashItems,
  createTeamSpaceDialogVisible,
  createFolderDialogVisible,
  versionDrawerVisible,
  trashDrawerVisible,
  createTeamSpaceForm,
  createFolderForm,
  addMemberForm,
  activeTeamSpace,
  canManageTeamSpaces,
  currentSpaceRole,
  canWriteCurrentSpace,
  canManageCurrentSpace,
  currentFolderLabel,
  readOnlyReason,
  candidateOrgMembers,
  summaryCards,
  policyChips,
  bootstrapPage,
  onOrganizationChange,
  onTeamSpaceChange,
  onRefreshWorkspace,
  onSearchItems,
  onKeywordChange,
  onItemTypeFilterChange,
  onOpenFolder,
  onNavigateTrail,
  openCreateTeamSpaceDialog,
  resetCreateTeamSpaceForm,
  onCreateTeamSpace,
  openCreateFolderDialog,
  resetCreateFolderForm,
  onCreateFolder,
  onUploadFileSelected: handleUploadFile,
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
  onRemoveMember,
  loadCurrentItems,
  loadActivity,
  loadTrashItems
} = useBusinessWorkspace()

const uploadInputRef = ref<HTMLInputElement | null>(null)

function triggerUploadPicker(): void {
  uploadInputRef.value?.click()
}

async function onUploadFileSelected(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement | null
  const file = input?.files?.[0] || null
  await handleUploadFile(file)
  if (input) {
    input.value = ''
  }
}

onMounted(() => {
  void bootstrapPage()
})
</script>

<template>
  <div class="mm-page business-page">
    <BusinessOverviewHero
      :organizations="organizations"
      :selected-org-id="selectedOrgId"
      :loading="loading.overview || loading.organizations"
      :can-manage-team-spaces="canManageTeamSpaces"
      :summary-cards="summaryCards"
      :policy-chips="policyChips"
      @change-org="onOrganizationChange"
      @refresh="onRefreshWorkspace"
      @create-team-space="openCreateTeamSpaceDialog"
    />

    <section v-if="organizations.length === 0" class="mm-card empty-state">
      <el-empty :description="t('business.empty.noOrganization')" />
    </section>

    <section v-else class="business-grid">
      <BusinessTeamSpacesPanel
        :team-spaces="teamSpaces"
        :selected-team-space-id="selectedTeamSpaceId"
        :current-role="overview?.currentRole"
        @select="onTeamSpaceChange"
      />
      <BusinessTeamSpaceExplorer
        :active-team-space="activeTeamSpace"
        :selected-team-space-id="selectedTeamSpaceId"
        :items="items"
        :loading-items="loading.items"
        :loading-upload="loading.upload"
        :loading-download-item-id="loading.downloadItemId"
        :keyword="keyword"
        :item-type-filter="itemTypeFilter"
        :trail="trail"
        :current-folder-label="currentFolderLabel"
        :access-role="currentSpaceRole"
        :can-write="canWriteCurrentSpace"
        :can-manage="canManageCurrentSpace"
        :read-only-reason="readOnlyReason"
        @refresh="loadCurrentItems"
        @open-create-folder="openCreateFolderDialog"
        @upload="triggerUploadPicker"
        @search="onSearchItems"
        @update:keyword="onKeywordChange"
        @update:item-type-filter="onItemTypeFilterChange"
        @open-folder="onOpenFolder"
        @navigate-trail="onNavigateTrail"
        @download="onDownloadItem"
        @open-version="openVersionDrawer"
        @delete-item="onDeleteItem"
      />
      <BusinessTeamSpaceControlPanel
        :active-team-space="activeTeamSpace"
        :current-access-role="currentSpaceRole"
        :can-manage-current-space="canManageCurrentSpace"
        :candidate-org-members="candidateOrgMembers"
        :team-space-members="teamSpaceMembers"
        :loading-members="loading.members || loading.addMember"
        :member-mutation-id="loading.memberMutationId"
        :add-member-user-email="addMemberForm.userEmail"
        :add-member-role="addMemberForm.role"
        :activities="activityItems"
        :activity-category="activityCategory"
        :loading-activity="loading.activity"
        :read-only-reason="readOnlyReason"
        @update:add-member-user-email="addMemberForm.userEmail = $event"
        @update:add-member-role="addMemberForm.role = $event"
        @add-member="onAddMember"
        @update-member-role="onUpdateMemberRole"
        @remove-member="onRemoveMember"
        @update:activity-category="onActivityCategoryChange"
        @refresh-activity="loadActivity"
        @open-trash="openTrashDrawer"
      />
    </section>

    <input ref="uploadInputRef" class="hidden-input" type="file" @change="onUploadFileSelected" />

    <BusinessTeamSpaceVersionsDrawer
      v-model="versionDrawerVisible"
      :target-item="versionTargetItem"
      :versions="versionItems"
      :loading-versions="loading.versions"
      :version-mutation-id="loading.versionMutationId"
      :version-upload-loading="loading.versionUpload"
      :can-write="canWriteCurrentSpace"
      :can-manage="canManageCurrentSpace"
      @upload-version="onUploadVersion"
      @restore-version="onRestoreVersion"
    />

    <BusinessTeamSpaceTrashDrawer
      v-model="trashDrawerVisible"
      :items="trashItems"
      :loading="loading.trash"
      :mutation-id="loading.trashMutationId"
      :can-manage="canManageCurrentSpace"
      @refresh="loadTrashItems"
      @restore="onRestoreTrashItem"
      @purge="onPurgeTrashItem"
    />

    <el-dialog v-model="createTeamSpaceDialogVisible" :title="t('business.dialog.teamSpace.title')" width="520px" @closed="resetCreateTeamSpaceForm">
      <el-form label-position="top">
        <el-form-item :label="t('business.dialog.teamSpace.fields.name')">
          <el-input v-model="createTeamSpaceForm.name" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('business.dialog.teamSpace.fields.description')">
          <el-input v-model="createTeamSpaceForm.description" type="textarea" :rows="4" maxlength="256" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('business.dialog.teamSpace.fields.storageLimitMb')">
          <el-input-number v-model="createTeamSpaceForm.storageLimitMb" :min="1" :max="102400" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createTeamSpaceDialogVisible = false">{{ t('common.actions.cancel') }}</el-button>
        <el-button type="primary" :loading="loading.createTeamSpace" @click="onCreateTeamSpace">{{ t('common.actions.create') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createFolderDialogVisible" :title="t('business.dialog.folder.title')" width="420px" @closed="resetCreateFolderForm">
      <el-form label-position="top">
        <el-form-item :label="t('business.dialog.folder.fields.name')">
          <el-input v-model="createFolderForm.name" maxlength="128" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createFolderDialogVisible = false">{{ t('common.actions.cancel') }}</el-button>
        <el-button type="primary" :loading="loading.createFolder" @click="onCreateFolder">{{ t('common.actions.create') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.business-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.business-grid {
  display: grid;
  grid-template-columns: minmax(280px, 0.9fr) minmax(0, 1.3fr) minmax(320px, 0.95fr);
  gap: 16px;
}

.empty-state {
  padding: 20px;
}

.hidden-input {
  display: none;
}

@media (max-width: 1440px) {
  .business-grid {
    grid-template-columns: 1fr;
  }
}
</style>
