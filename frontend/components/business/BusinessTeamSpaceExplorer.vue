<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { OrgTeamSpace, OrgTeamSpaceAccessRole, OrgTeamSpaceItem, OrgTeamSpaceItemType } from '~/types/business'
import { formatBusinessBytes, formatBusinessTime } from '~/utils/business'

interface TrailItem {
  id: string | null
  name: string
}

const props = defineProps<{
  activeTeamSpace: OrgTeamSpace | null
  selectedTeamSpaceId: string
  items: OrgTeamSpaceItem[]
  loadingItems: boolean
  loadingUpload: boolean
  loadingDownloadItemId: string
  keyword: string
  itemTypeFilter: OrgTeamSpaceItemType | ''
  trail: TrailItem[]
  currentFolderLabel: string
  accessRole: OrgTeamSpaceAccessRole | null
  canWrite: boolean
  canManage: boolean
  readOnlyReason: string
}>()

const emit = defineEmits<{
  refresh: []
  'open-create-folder': []
  upload: []
  search: []
  'update:keyword': [keyword: string]
  'update:item-type-filter': [itemType: OrgTeamSpaceItemType | '']
  'open-folder': [item: OrgTeamSpaceItem]
  'navigate-trail': [index: number]
  download: [item: OrgTeamSpaceItem]
  'open-version': [item: OrgTeamSpaceItem]
  'delete-item': [item: OrgTeamSpaceItem]
}>()
const { t } = useI18n()

function itemTypeTag(itemType: OrgTeamSpaceItemType): 'success' | 'info' {
  return itemType === 'FOLDER' ? 'success' : 'info'
}
</script>

<template>
  <article class="mm-card workspace-panel">
    <div class="panel-head workspace-panel__head">
      <div>
        <div class="title-row">
          <h2 class="mm-section-title">{{ activeTeamSpace?.name || t('business.workspace.fallbackTitle') }}</h2>
          <el-tag v-if="accessRole" effect="dark">{{ accessRole }}</el-tag>
        </div>
        <p class="mm-muted">{{ activeTeamSpace?.description || t('business.workspace.fallbackDescription') }}</p>
      </div>
      <div class="tool-actions">
        <el-button v-if="selectedTeamSpaceId" :loading="loadingItems" @click="emit('refresh')">{{ t('business.workspace.refreshList') }}</el-button>
        <el-button v-if="selectedTeamSpaceId" type="primary" plain :disabled="!canWrite" @click="emit('open-create-folder')">{{ t('business.workspace.newFolder') }}</el-button>
        <el-button v-if="selectedTeamSpaceId" type="primary" :loading="loadingUpload" :disabled="!canWrite" @click="emit('upload')">{{ t('business.workspace.uploadFile') }}</el-button>
      </div>
    </div>

    <div v-if="readOnlyReason" class="read-only-banner">
      {{ readOnlyReason }}
    </div>

    <div v-if="selectedTeamSpaceId" class="workspace-toolbar">
      <div class="breadcrumbs">
        <button
          v-for="(crumb, index) in trail"
          :key="`${crumb.id || 'root'}-${index}`"
          type="button"
          class="crumb"
          @click="emit('navigate-trail', index)"
        >
          {{ crumb.name }}
        </button>
      </div>
      <div class="workspace-filters">
        <el-input :model-value="keyword" :placeholder="t('business.workspace.searchCurrentFolder')" @update:model-value="emit('update:keyword', $event)" @keyup.enter="emit('search')" />
        <el-select :model-value="itemTypeFilter" :placeholder="t('business.workspace.type')" clearable @update:model-value="emit('update:item-type-filter', $event)">
          <el-option :label="t('business.workspace.typeAll')" value="" />
          <el-option :label="t('business.workspace.typeFolder')" value="FOLDER" />
          <el-option :label="t('business.workspace.typeFile')" value="FILE" />
        </el-select>
        <el-button type="primary" @click="emit('search')">{{ t('mailbox.actions.search') }}</el-button>
      </div>
    </div>

    <el-empty v-if="!selectedTeamSpaceId" :description="t('business.workspace.selectTeamSpace')" />
    <el-empty v-else-if="items.length === 0 && !loadingItems" :description="t('business.workspace.noItems', { name: currentFolderLabel })" />
    <el-table v-else v-loading="loadingItems" :data="items" class="workspace-table">
      <el-table-column :label="t('business.workspace.columns.name')" min-width="280">
        <template #default="scope">
          <button v-if="scope.row.itemType === 'FOLDER'" type="button" class="item-link" @click="emit('open-folder', scope.row)">
            {{ scope.row.name }}
          </button>
          <span v-else>{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('business.workspace.columns.type')" width="120">
        <template #default="scope">
          <el-tag :type="itemTypeTag(scope.row.itemType)">{{ scope.row.itemType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('business.workspace.columns.owner')" min-width="180">
        <template #default="scope">{{ scope.row.ownerEmail || '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('business.workspace.columns.size')" width="140">
        <template #default="scope">
          {{ scope.row.itemType === 'FILE' ? formatBusinessBytes(scope.row.sizeBytes) : '-' }}
        </template>
      </el-table-column>
      <el-table-column :label="t('business.workspace.columns.updatedAt')" min-width="180">
        <template #default="scope">{{ formatBusinessTime(scope.row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column :label="t('business.workspace.columns.actions')" width="220">
        <template #default="scope">
          <div class="row-actions">
            <el-button
              v-if="scope.row.itemType === 'FILE'"
              size="small"
              link
              :loading="loadingDownloadItemId === scope.row.id"
              @click="emit('download', scope.row)"
            >
              {{ t('business.workspace.actions.download') }}
            </el-button>
            <el-button v-if="scope.row.itemType === 'FILE'" size="small" link @click="emit('open-version', scope.row)">{{ t('business.workspace.actions.versions') }}</el-button>
            <el-button v-if="canWrite" size="small" link type="danger" @click="emit('delete-item', scope.row)">{{ t('business.workspace.actions.delete') }}</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </article>
</template>

<style scoped>
.workspace-panel {
  padding: 20px;
}

.panel-head,
.workspace-panel__head,
.workspace-toolbar,
.workspace-filters,
.breadcrumbs,
.tool-actions,
.title-row,
.row-actions {
  display: flex;
  gap: 12px;
}

.panel-head,
.workspace-panel__head,
.workspace-toolbar {
  justify-content: space-between;
}

.workspace-panel__head,
.workspace-toolbar,
.row-actions,
.title-row {
  align-items: center;
}

.workspace-toolbar,
.breadcrumbs,
.row-actions {
  flex-wrap: wrap;
}

.workspace-toolbar {
  margin-bottom: 16px;
}

.read-only-banner {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(250, 173, 20, 0.12);
  color: #8a5b00;
  font-weight: 600;
}

.crumb,
.item-link {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--mm-primary-dark);
  cursor: pointer;
  font: inherit;
}

.crumb {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 110, 110, 0.08);
}

.item-link {
  font-weight: 600;
}

.mm-muted {
  color: var(--mm-muted);
}

@media (max-width: 768px) {
  .panel-head,
  .workspace-panel__head,
  .workspace-toolbar,
  .workspace-filters,
  .tool-actions,
  .title-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
