<script setup lang="ts">
import { computed } from 'vue'
import type { ContactItem } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useContactsWorkspace } from '~/composables/useContactsWorkspace'

const IMPORT_TEXT_ROWS = 8
const EDIT_NOTE_ROWS = 3

const { t } = useI18n()
const workspace = useContactsWorkspace()
const contactOptions = computed(() => workspace.contacts.value)
const importSummary = computed(() => workspace.importResult.value)
</script>

<template>
  <div class="mm-page">
    <section class="mm-card panel">
      <h1 class="mm-section-title">{{ t('contacts.title') }}</h1>
      <div class="filters">
        <el-input
          v-model="workspace.keyword"
          :placeholder="t('contacts.filters.keywordPlaceholder')"
          clearable
          @keyup.enter="workspace.onSearch"
        />
        <el-switch
          v-model="workspace.favoriteOnly"
          :active-text="t('contacts.filters.favoritesOnly')"
          @change="workspace.onSearch"
        />
        <el-button type="primary" @click="workspace.onSearch">{{ t('contacts.actions.search') }}</el-button>
      </div>

      <div class="create-grid">
        <el-input v-model="workspace.createForm.displayName" :placeholder="t('contacts.fields.displayName')" />
        <el-input v-model="workspace.createForm.email" :placeholder="t('contacts.fields.email')" />
        <el-input v-model="workspace.createForm.note" :placeholder="t('contacts.fields.noteOptional')" />
        <el-button type="success" @click="workspace.onCreate">{{ t('contacts.actions.createContact') }}</el-button>
      </div>

      <el-table
        :data="workspace.contacts"
        :empty-text="t('contacts.empty.contacts')"
        v-loading="workspace.contactsLoading"
        style="width: 100%"
      >
        <el-table-column prop="displayName" :label="t('contacts.table.name')" min-width="160" />
        <el-table-column prop="email" :label="t('contacts.table.email')" min-width="220" />
        <el-table-column :label="t('contacts.table.favorite')" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.isFavorite" size="small" type="warning" effect="plain">
              {{ t('contacts.table.favoriteYes') }}
            </el-tag>
            <span v-else class="muted">{{ t('contacts.table.favoriteNo') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" :label="t('contacts.table.updatedAt')" min-width="180" />
        <el-table-column :label="t('contacts.table.actions')" width="260">
          <template #default="scope">
            <el-button type="primary" text @click="workspace.openEdit(scope.row)">{{ t('contacts.actions.edit') }}</el-button>
            <el-button type="warning" text @click="workspace.toggleFavorite(scope.row)">
              {{ scope.row.isFavorite ? t('contacts.actions.unfavorite') : t('contacts.actions.favorite') }}
            </el-button>
            <el-button type="danger" text @click="workspace.removeContact(scope.row.id)">{{ t('contacts.actions.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="mm-card panel two-col">
      <div>
        <h2 class="mm-section-title">{{ t('contacts.groups.title') }}</h2>
        <div class="group-create-grid">
          <el-input v-model="workspace.createGroupForm.name" :placeholder="t('contacts.fields.groupName')" />
          <el-input
            v-model="workspace.createGroupForm.description"
            :placeholder="t('contacts.fields.groupDescriptionOptional')"
          />
          <el-button type="success" @click="workspace.onCreateGroup">{{ t('contacts.actions.createGroup') }}</el-button>
        </div>
        <el-table
          :data="workspace.groups"
          :empty-text="t('contacts.empty.groups')"
          v-loading="workspace.groupsLoading"
          style="width: 100%"
          @row-click="workspace.onSelectGroup"
        >
          <el-table-column prop="name" :label="t('contacts.groups.table.group')" min-width="160" />
          <el-table-column prop="memberCount" :label="t('contacts.groups.table.members')" width="100" />
          <el-table-column prop="updatedAt" :label="t('contacts.table.updatedAt')" min-width="170" />
          <el-table-column :label="t('contacts.table.actions')" width="120">
            <template #default="scope">
              <el-button type="danger" text @click.stop="workspace.onDeleteGroup(scope.row.id)">
                {{ t('contacts.actions.delete') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div>
        <h2 class="mm-section-title">{{ t('contacts.groups.membersTitle') }}</h2>
        <div v-if="workspace.selectedGroup" class="group-editor">
          <el-input v-model="workspace.groupEditForm.name" :placeholder="t('contacts.fields.groupName')" />
          <el-input v-model="workspace.groupEditForm.description" :placeholder="t('contacts.fields.groupDescription')" />
          <el-button type="primary" @click="workspace.onSaveGroupEdit">{{ t('contacts.actions.saveGroup') }}</el-button>
        </div>
        <div v-else class="muted">{{ t('contacts.groups.emptySelection') }}</div>

        <div v-if="workspace.selectedGroup" class="member-selector">
          <el-select
            v-model="workspace.selectedMemberIds"
            filterable
            multiple
            collapse-tags
            :placeholder="t('contacts.fields.selectContactsToAdd')"
          >
            <el-option
              v-for="contact in contactOptions"
              :key="contact.id"
              :label="`${contact.displayName} <${contact.email}>`"
              :value="contact.id"
            />
          </el-select>
          <el-button type="primary" @click="workspace.onAddGroupMembers">{{ t('contacts.actions.addMembers') }}</el-button>
        </div>

        <el-table
          v-if="workspace.selectedGroup"
          :data="workspace.groupMembers"
          :empty-text="t('contacts.empty.groupMembers')"
          style="width: 100%"
        >
          <el-table-column prop="displayName" :label="t('contacts.table.name')" min-width="150" />
          <el-table-column prop="signature" :label="t('contacts.duplicates.duplicateKey')" min-width="220" />
          <el-table-column :label="t('contacts.table.actions')" width="110">
            <template #default="scope">
              <el-button type="danger" text @click="workspace.onRemoveGroupMember(scope.row.id)">
                {{ t('contacts.actions.delete') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <section class="mm-card panel two-col">
      <div>
        <h2 class="mm-section-title">{{ t('contacts.importExport.title') }}</h2>
        <el-input
          v-model="workspace.importCsvText"
          type="textarea"
          :rows="IMPORT_TEXT_ROWS"
          :placeholder="t('contacts.importExport.csvPlaceholder')"
        />
        <div class="import-actions">
          <el-switch
            v-model="workspace.importMergeDuplicates"
            :active-text="t('contacts.importExport.mergeDuplicates')"
          />
          <el-button type="primary" @click="workspace.onImportCsv">{{ t('contacts.actions.importCsv') }}</el-button>
          <el-button @click="workspace.onExport('csv')">{{ t('contacts.actions.exportCsv') }}</el-button>
          <el-button @click="workspace.onExport('vcard')">{{ t('contacts.actions.exportVcard') }}</el-button>
        </div>
        <div v-if="importSummary" class="import-result">
          <el-tag type="success">{{ t('contacts.importExport.created') }}: {{ importSummary.created }}</el-tag>
          <el-tag type="success">{{ t('contacts.importExport.updated') }}: {{ importSummary.updated }}</el-tag>
          <el-tag type="warning">{{ t('contacts.importExport.skipped') }}: {{ importSummary.skipped }}</el-tag>
          <el-tag>{{ t('contacts.importExport.invalid') }}: {{ importSummary.invalid }}</el-tag>
          <el-tag>{{ t('contacts.importExport.total') }}: {{ importSummary.totalRows }}</el-tag>
        </div>
      </div>

      <div>
        <h2 class="mm-section-title">{{ t('contacts.duplicates.title') }}</h2>
        <el-table :data="workspace.duplicates" :empty-text="t('contacts.empty.duplicates')" style="width: 100%">
          <el-table-column prop="signature" :label="t('contacts.duplicates.duplicateKey')" min-width="220" />
          <el-table-column prop="count" :label="t('contacts.duplicates.count')" width="90" />
          <el-table-column :label="t('contacts.duplicates.contacts')" min-width="240">
            <template #default="scope">
              <span>{{ scope.row.contacts.map((item: ContactItem) => item.displayName).join(', ') }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('contacts.table.actions')" width="120">
            <template #default="scope">
              <el-button type="warning" text @click="workspace.onMergeDuplicate(scope.row)">
                {{ t('contacts.actions.merge') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <el-dialog v-model="workspace.editDialogVisible" :title="t('contacts.dialog.editTitle')" width="560px">
      <div class="edit-grid">
        <el-form-item :label="t('contacts.dialog.displayName')">
          <el-input v-model="workspace.editForm.displayName" />
        </el-form-item>
        <el-form-item :label="t('contacts.dialog.email')">
          <el-input v-model="workspace.editForm.email" />
        </el-form-item>
        <el-form-item :label="t('contacts.dialog.note')">
          <el-input v-model="workspace.editForm.note" type="textarea" :rows="EDIT_NOTE_ROWS" />
        </el-form-item>
      </div>
      <template #footer>
        <el-button @click="workspace.closeEditDialog">{{ t('contacts.actions.cancel') }}</el-button>
        <el-button type="primary" @click="workspace.submitEdit">{{ t('contacts.actions.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.panel {
  padding: 18px;
}

.filters {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 8px;
  margin-bottom: 12px;
}

.create-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr auto;
  gap: 8px;
  margin-bottom: 14px;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.group-create-grid {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 8px;
  margin-bottom: 12px;
}

.group-editor,
.member-selector,
.import-actions,
.import-result {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.edit-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.muted {
  color: var(--mm-muted);
}

@media (max-width: 1024px) {
  .filters,
  .create-grid,
  .group-create-grid,
  .two-col {
    grid-template-columns: 1fr;
  }
}
</style>
