<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { useSheetsWorkbench } from '~/composables/useSheetsWorkbench'
import type { SheetsTemplatePreset } from '~/utils/sheets-collaboration'
import type { SuiteCollaborationEvent } from '~/types/api'
import type { SheetsIncomingShare, SheetsPermission, SheetsScopeFilter } from '~/types/sheets'

definePageMeta({
  layout: 'default'
})

const { t } = useI18n()
const {
  activeWorkbook,
  activeSheet,
  activeCell,
  localGrid,
  loadingList,
  loadingDetail,
  creating,
  importing,
  exporting,
  refreshing,
  saving,
  sheetBusy,
  busyWorkbookId,
  conflictMessage,
  dirtyCount,
  workbookCount,
  savedGrid,
  computedGrid,
  supportedImportFormats,
  supportedExportFormats,
  activeWorkbookId,
  activeWorkbookForHero,
  activeCellLabel,
  activeCellPresentation,
  formulaPreviewHint,
  localFormulaCellCount,
  toolsBusy,
  searchQuery,
  searchMatchCount,
  searchMatchKeys,
  frozenRowCount,
  frozenColCount,
  lastImported,
  lastExport,
  workbookEvents,
  collaborationLoading,
  collaborationError,
  creatingTemplateCode,
  templatePresets,
  workspaceView,
  scopeFilter,
  filteredWorkbooks,
  shares,
  incomingShares,
  versions,
  inviteEmail,
  invitePermission,
  sharesLoading,
  incomingLoading,
  versionsLoading,
  shareSubmitting,
  shareMutationId,
  incomingMutationId,
  versionMutationId,
  versionDrawerVisible,
  pendingIncomingCount,
  canManageShares,
  canRestoreVersions,
  selectWorkbook,
  createWorkbookFromTemplate,
  onCreateWorkbook,
  onImportWorkbook,
  onExportWorkbook,
  onRenameWorkbook,
  onDeleteWorkbook,
  onCreateSheet,
  onRenameSheet,
  onDeleteSheet,
  onSelectSheet,
  onCellSelect,
  onCellChange,
  onFormulaChange,
  onSaveWorkbook,
  onSortSheet,
  onFreezeRowsToActiveCell,
  onFreezeColsToActiveCell,
  onClearFreeze,
  updateSearchQuery,
  onRefreshWorkspace,
  refreshIncomingShares,
  submitShare,
  updateSharePermission,
  removeShare,
  respondIncomingShare,
  openIncomingWorkbook,
  openVersionHistory,
  restoreVersion
} = useSheetsWorkbench()

const workspaceViewOptions = computed(() => [
  { label: t('sheets.filters.library'), value: 'WORKBOOKS' },
  {
    label: pendingIncomingCount.value > 0
      ? `${t('sheets.filters.incoming')} (${pendingIncomingCount.value})`
      : t('sheets.filters.incoming'),
    value: 'INCOMING_SHARES'
  }
])

const scopeFilterOptions = computed<Array<{ label: string; value: SheetsScopeFilter }>>(() => [
  { label: t('sheets.filters.allScopes'), value: 'ALL' },
  { label: t('sheets.filters.owned'), value: 'OWNED' },
  { label: t('sheets.filters.shared'), value: 'SHARED' }
])

function formatTime(value: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '—'
}

function getPermissionLabel(permission: SheetsPermission | undefined): string {
  if (permission === 'EDIT') {
    return t('sheets.share.permission.edit')
  }
  if (permission === 'VIEW') {
    return t('sheets.share.permission.view')
  }
  return t('sheets.share.permission.owner')
}

async function openCollaborationEvent(item: SuiteCollaborationEvent): Promise<void> {
  await navigateTo(item.routePath || '/sheets')
}

async function onCreateTemplate(template: SheetsTemplatePreset): Promise<void> {
  try {
    await createWorkbookFromTemplate(template, t(template.presetTitleKey))
    ElMessage.success(t('sheets.messages.templateCreated'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('sheets.messages.templateCreateFailed'))
  }
}

async function onOpenIncomingWorkbook(item: SheetsIncomingShare): Promise<void> {
  await openIncomingWorkbook(item)
}
</script>

<template>
  <div class="mm-page sheets-page">
    <SheetsWorkspaceHero
      :workbook="activeWorkbookForHero"
      :workbook-count="workbookCount"
      :dirty-count="dirtyCount"
      :saving="saving"
      :refreshing="refreshing"
      :creating="creating"
      :last-exported-at="lastExport?.exportedAt || null"
      @create="onCreateWorkbook"
      @refresh="onRefreshWorkspace"
      @save="onSaveWorkbook"
    />

    <el-alert
      v-if="conflictMessage"
      class="sheets-alert"
      type="warning"
      :closable="false"
      :title="conflictMessage"
      show-icon
    />

    <section class="workspace-toolbar mm-card">
      <div>
        <p>{{ t('sheets.filters.library') }}</p>
        <h2>{{ t('sheets.share.title') }}</h2>
      </div>
      <div class="workspace-toolbar__controls">
        <el-segmented v-model="workspaceView" :options="workspaceViewOptions" />
        <el-segmented
          v-if="workspaceView === 'WORKBOOKS'"
          v-model="scopeFilter"
          :options="scopeFilterOptions"
        />
        <el-badge :value="pendingIncomingCount" :hidden="pendingIncomingCount === 0">
          <el-button plain @click="refreshIncomingShares">{{ t('sheets.meta.refreshIncoming') }}</el-button>
        </el-badge>
      </div>
    </section>

    <section class="workspace-grid">
      <SheetsWorkbookSidebar
        v-if="workspaceView === 'WORKBOOKS'"
        :workbooks="filteredWorkbooks"
        :active-workbook-id="activeWorkbookId"
        :loading="loadingList"
        :busy-workbook-id="busyWorkbookId"
        @select="selectWorkbook($event, true)"
        @rename="onRenameWorkbook"
        @delete="onDeleteWorkbook"
        @create="onCreateWorkbook"
      />

      <SheetsIncomingSharesPanel
        v-else
        :items="incomingShares"
        :loading="incomingLoading"
        :mutation-id="incomingMutationId"
        @refresh="refreshIncomingShares"
        @respond="respondIncomingShare"
        @open="onOpenIncomingWorkbook"
      />

      <div class="workspace-main">
        <div v-if="activeWorkbookForHero" class="workspace-meta mm-card">
          <div>
            <p>{{ t('sheets.meta.currentWorkbookLabel') }}</p>
            <h2>{{ activeWorkbookForHero.title }}</h2>
          </div>
          <div class="workspace-meta__chips">
            <span>{{ t('sheets.meta.scope', { value: activeWorkbookForHero.scope === 'OWNED' ? t('sheets.filters.owned') : t('sheets.filters.shared') }) }}</span>
            <span>{{ t('sheets.meta.permission', { value: getPermissionLabel(activeWorkbookForHero.permission) }) }}</span>
            <span v-if="activeWorkbookForHero.scope === 'SHARED'">
              {{ t('sheets.meta.owner', { value: activeWorkbookForHero.ownerDisplayName || activeWorkbookForHero.ownerEmail }) }}
            </span>
            <span v-else>{{ t('sheets.meta.collaborators', { count: activeWorkbookForHero.collaboratorCount }) }}</span>
            <span>{{ activeWorkbookForHero.canEdit ? t('sheets.meta.editAccess') : t('sheets.meta.readOnly') }}</span>
            <span>{{ t('sheets.meta.activeSheet', { value: activeSheet?.name || t('common.none') }) }}</span>
            <span>{{ t('sheets.meta.sheetCount', { count: activeWorkbookForHero.sheetCount }) }}</span>
            <span>{{ t('sheets.meta.filledCells', { count: activeWorkbookForHero.filledCellCount }) }}</span>
            <span>{{ t('sheets.meta.formulas', { count: activeWorkbookForHero.formulaCellCount }) }}</span>
            <span>{{ t('sheets.meta.errors', { count: activeWorkbookForHero.computedErrorCount }) }}</span>
            <span>{{ t('sheets.meta.updatedAt', { value: formatTime(activeWorkbookForHero.updatedAt) }) }}</span>
            <el-badge :value="pendingIncomingCount" :hidden="pendingIncomingCount === 0">
              <span>{{ t('sheets.filters.incoming') }}</span>
            </el-badge>
            <el-button
              v-if="canRestoreVersions"
              type="warning"
              plain
              size="small"
              @click="openVersionHistory"
            >
              {{ t('sheets.meta.openVersions') }}
            </el-button>
          </div>
        </div>

        <div class="workspace-content">
          <div class="workspace-core">
            <SheetsWorkbookTabs
              :workbook="activeWorkbook"
              :busy="sheetBusy || saving || loadingDetail"
              :can-manage-sheets="activeWorkbook?.permission === 'OWNER'"
              @select-sheet="onSelectSheet"
              @create-sheet="onCreateSheet"
              @rename-sheet="onRenameSheet"
              @delete-sheet="onDeleteSheet"
            />

            <SheetsDataToolsPanel
              :active-cell="activeCell"
              :search-query="searchQuery"
              :match-count="searchMatchCount"
              :frozen-row-count="frozenRowCount"
              :frozen-col-count="frozenColCount"
              :dirty-count="dirtyCount"
              :busy="toolsBusy || saving || sheetBusy"
              :loading="loadingDetail"
              :can-manage="activeWorkbook?.permission === 'OWNER'"
              @update:search-query="updateSearchQuery"
              @sort="onSortSheet"
              @freeze-rows="onFreezeRowsToActiveCell"
              @freeze-cols="onFreezeColsToActiveCell"
              @clear-freeze="onClearFreeze"
            />

            <SheetsFormulaPanel
              :has-selection="!!activeCellPresentation"
              :active-cell-label="activeCellLabel"
              :raw-value="activeCellPresentation?.rawValue || ''"
              :computed-value="activeCellPresentation?.computedValue || ''"
              :preview-hint="formulaPreviewHint"
              :dirty="activeCellPresentation?.isDirty || false"
              :saving="saving"
              :readonly="!activeWorkbook?.canEdit"
              :formula-cell-count="localFormulaCellCount"
              :computed-error-count="activeWorkbook?.computedErrorCount || 0"
              @update="onFormulaChange"
            />

            <SheetsGridEditor
              :grid="localGrid"
              :computed-grid="computedGrid"
              :saved-grid="savedGrid"
              :active-cell="activeCell"
              :frozen-row-count="frozenRowCount"
              :frozen-col-count="frozenColCount"
              :search-match-keys="searchMatchKeys"
              :loading="loadingDetail"
              :saving="saving || toolsBusy"
              :readonly="!activeWorkbook?.canEdit"
              @select-cell="onCellSelect"
              @cell-change="onCellChange"
            />
          </div>

          <div class="workspace-rail">
            <SheetsShareManager
              :workbook="activeWorkbook"
              :shares="shares"
              :loading="sharesLoading"
              :submitting="shareSubmitting"
              :mutation-id="shareMutationId"
              :can-manage="canManageShares"
              v-model:invite-email="inviteEmail"
              v-model:invite-permission="invitePermission"
              @submit="submitShare"
              @update-permission="updateSharePermission"
              @remove="removeShare"
            />

            <SheetsCollaborationRail
              :events="workbookEvents"
              :loading="collaborationLoading"
              :error-message="collaborationError"
              :creating-template-code="creatingTemplateCode"
              :templates="templatePresets"
              @create-template="onCreateTemplate"
              @open-event="openCollaborationEvent"
            />

            <SheetsImportExportPanel
              :workbook="activeWorkbook"
              :supported-import-formats="supportedImportFormats"
              :supported-export-formats="supportedExportFormats"
              :last-imported="lastImported"
              :last-export="lastExport"
              :importing="importing"
              :exporting="exporting"
              @import="onImportWorkbook"
              @export="onExportWorkbook"
            />

            <SheetsInsightRail
              :workbook-count="workbookCount"
              :workbook="activeWorkbook"
              :dirty-count="dirtyCount"
              :supported-import-formats="supportedImportFormats"
              :supported-export-formats="supportedExportFormats"
            />
          </div>
        </div>
      </div>
    </section>

    <SheetsVersionHistoryDrawer
      v-model="versionDrawerVisible"
      :workbook="activeWorkbook"
      :items="versions"
      :loading="versionsLoading"
      :mutation-id="versionMutationId"
      :can-restore="canRestoreVersions"
      @restore="restoreVersion"
    />
  </div>
</template>

<style scoped>
.sheets-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.sheets-alert {
  margin-top: -2px;
}

.workspace-toolbar,
.workspace-toolbar__controls {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.workspace-toolbar {
  align-items: center;
  padding: 16px 18px;
}

.workspace-toolbar p,
.workspace-toolbar h2 {
  margin: 0;
}

.workspace-toolbar p {
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--mm-muted);
}

.workspace-toolbar h2 {
  margin-top: 6px;
  font-size: 22px;
}

.workspace-toolbar__controls {
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(300px, 340px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.workspace-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.workspace-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
}

.workspace-meta p {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--mm-muted);
}

.workspace-meta h2 {
  margin: 8px 0 0;
  font-size: 22px;
}

.workspace-meta__chips {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  align-items: center;
}

.workspace-meta__chips span,
.workspace-meta__chips :deep(.el-badge__content) {
  padding: 8px 12px;
}

.workspace-meta__chips span {
  border-radius: 999px;
  background: rgba(15, 110, 110, 0.08);
  color: var(--mm-primary-dark);
  font-size: 12px;
}

.workspace-content {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(320px, 0.9fr);
  gap: 16px;
  align-items: start;
}

.workspace-core,
.workspace-rail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (max-width: 1260px) {
  .workspace-grid,
  .workspace-content {
    grid-template-columns: 1fr;
  }

  .workspace-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .workspace-toolbar__controls {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .workspace-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .workspace-meta__chips {
    justify-content: flex-start;
  }
}
</style>
