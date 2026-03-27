<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type {
  ContactDuplicateGroup,
  ContactGroup,
  ContactImportResult,
  ContactItem,
  CreateContactRequest,
  UpdateContactRequest
} from '~/types/api'
import { useContactApi } from '~/composables/useContactApi'

const contactsLoading = ref(false)
const groupsLoading = ref(false)
const contacts = ref<ContactItem[]>([])
const groups = ref<ContactGroup[]>([])
const groupMembers = ref<ContactItem[]>([])
const duplicates = ref<ContactDuplicateGroup[]>([])
const selectedGroupId = ref('')
const selectedMemberIds = ref<string[]>([])
const keyword = ref('')
const favoriteOnly = ref(false)

const importCsvText = ref('')
const importMergeDuplicates = ref(true)
const importResult = ref<ContactImportResult | null>(null)

const createForm = reactive({
  displayName: '',
  email: '',
  note: ''
})

const editDialogVisible = ref(false)
const editingContactId = ref('')
const editForm = reactive({
  displayName: '',
  email: '',
  note: ''
})

const createGroupForm = reactive({
  name: '',
  description: ''
})

const groupEditForm = reactive({
  name: '',
  description: ''
})

const selectedGroup = computed(() => groups.value.find(group => group.id === selectedGroupId.value) || null)

const {
  listContacts,
  createContact,
  updateContact,
  deleteContact,
  favoriteContact,
  unfavoriteContact,
  importContactsCsv,
  exportContacts,
  listDuplicateContacts,
  mergeDuplicateContacts,
  listGroups,
  createGroup,
  updateGroup,
  deleteGroup,
  listGroupMembers,
  addGroupMembers,
  removeGroupMember
} = useContactApi()

async function loadContacts(): Promise<void> {
  contactsLoading.value = true
  try {
    contacts.value = await listContacts(keyword.value, favoriteOnly.value)
  } finally {
    contactsLoading.value = false
  }
}

async function loadGroups(): Promise<void> {
  groupsLoading.value = true
  try {
    groups.value = await listGroups()
    if (selectedGroupId.value) {
      const exists = groups.value.some(group => group.id === selectedGroupId.value)
      if (!exists) {
        selectedGroupId.value = ''
        groupMembers.value = []
      }
    }
  } finally {
    groupsLoading.value = false
  }
}

async function loadGroupMembers(groupId: string): Promise<void> {
  groupMembers.value = await listGroupMembers(groupId)
}

async function loadDuplicates(): Promise<void> {
  duplicates.value = await listDuplicateContacts()
}

async function refreshData(): Promise<void> {
  await Promise.all([loadContacts(), loadGroups(), loadDuplicates()])
  if (selectedGroupId.value) {
    await loadGroupMembers(selectedGroupId.value)
  }
}

async function onSearch(): Promise<void> {
  await loadContacts()
}

async function onCreate(): Promise<void> {
  if (!createForm.displayName.trim() || !createForm.email.trim()) {
    ElMessage.warning('Display name and email are required')
    return
  }

  const payload: CreateContactRequest = {
    displayName: createForm.displayName.trim(),
    email: createForm.email.trim(),
    note: createForm.note.trim() || undefined
  }
  await createContact(payload)
  ElMessage.success('Contact created')
  createForm.displayName = ''
  createForm.email = ''
  createForm.note = ''
  await refreshData()
}

function openEdit(contact: ContactItem): void {
  editingContactId.value = contact.id
  editForm.displayName = contact.displayName
  editForm.email = contact.email
  editForm.note = contact.note || ''
  editDialogVisible.value = true
}

async function submitEdit(): Promise<void> {
  if (!editingContactId.value) {
    return
  }
  if (!editForm.displayName.trim() || !editForm.email.trim()) {
    ElMessage.warning('Display name and email are required')
    return
  }

  const payload: UpdateContactRequest = {
    displayName: editForm.displayName.trim(),
    email: editForm.email.trim(),
    note: editForm.note.trim() || undefined
  }
  await updateContact(editingContactId.value, payload)
  ElMessage.success('Contact updated')
  editDialogVisible.value = false
  await refreshData()
}

async function removeContact(contactId: string): Promise<void> {
  try {
    await ElMessageBox.confirm('Delete this contact?', 'Delete Confirm', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
  } catch {
    return
  }

  await deleteContact(contactId)
  ElMessage.success('Contact deleted')
  await refreshData()
}

async function toggleFavorite(contact: ContactItem): Promise<void> {
  if (contact.isFavorite) {
    await unfavoriteContact(contact.id)
    ElMessage.success('Removed from favorites')
  } else {
    await favoriteContact(contact.id)
    ElMessage.success('Marked as favorite')
  }
  await refreshData()
}

async function onCreateGroup(): Promise<void> {
  if (!createGroupForm.name.trim()) {
    ElMessage.warning('Group name is required')
    return
  }
  await createGroup({
    name: createGroupForm.name.trim(),
    description: createGroupForm.description.trim() || undefined
  })
  ElMessage.success('Group created')
  createGroupForm.name = ''
  createGroupForm.description = ''
  await loadGroups()
}

async function onSelectGroup(group: ContactGroup): Promise<void> {
  selectedGroupId.value = group.id
  groupEditForm.name = group.name
  groupEditForm.description = group.description || ''
  selectedMemberIds.value = []
  await loadGroupMembers(group.id)
}

async function onSaveGroupEdit(): Promise<void> {
  if (!selectedGroupId.value) {
    return
  }
  if (!groupEditForm.name.trim()) {
    ElMessage.warning('Group name is required')
    return
  }
  await updateGroup(selectedGroupId.value, {
    name: groupEditForm.name.trim(),
    description: groupEditForm.description.trim() || undefined
  })
  ElMessage.success('Group updated')
  await loadGroups()
}

async function onDeleteGroup(groupId: string): Promise<void> {
  try {
    await ElMessageBox.confirm('Delete this group?', 'Delete Group', {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning'
    })
  } catch {
    return
  }
  await deleteGroup(groupId)
  ElMessage.success('Group deleted')
  if (selectedGroupId.value === groupId) {
    selectedGroupId.value = ''
    groupMembers.value = []
  }
  await loadGroups()
}

async function onAddGroupMembers(): Promise<void> {
  if (!selectedGroupId.value || selectedMemberIds.value.length === 0) {
    ElMessage.warning('Select contacts first')
    return
  }
  await addGroupMembers(selectedGroupId.value, { contactIds: selectedMemberIds.value })
  ElMessage.success('Group members updated')
  selectedMemberIds.value = []
  await Promise.all([loadGroups(), loadGroupMembers(selectedGroupId.value)])
}

async function onRemoveGroupMember(contactId: string): Promise<void> {
  if (!selectedGroupId.value) {
    return
  }
  await removeGroupMember(selectedGroupId.value, contactId)
  ElMessage.success('Member removed')
  await Promise.all([loadGroups(), loadGroupMembers(selectedGroupId.value)])
}

async function onImportCsv(): Promise<void> {
  if (!importCsvText.value.trim()) {
    ElMessage.warning('Paste CSV content first')
    return
  }
  importResult.value = await importContactsCsv(importCsvText.value, importMergeDuplicates.value)
  ElMessage.success('Contacts imported')
  await refreshData()
}

function downloadText(filename: string, content: string, type: string): void {
  const blob = new Blob([content], { type })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  URL.revokeObjectURL(url)
}

async function onExport(format: 'csv' | 'vcard'): Promise<void> {
  const text = await exportContacts(format)
  const extension = format === 'csv' ? 'csv' : 'vcf'
  const mime = format === 'csv' ? 'text/csv;charset=utf-8' : 'text/vcard;charset=utf-8'
  downloadText(`mmmail-contacts.${extension}`, text, mime)
  ElMessage.success(`Exported ${format.toUpperCase()}`)
}

async function onMergeDuplicate(group: ContactDuplicateGroup): Promise<void> {
  if (group.contacts.length < 2) {
    return
  }
  const primary = group.contacts[0]
  const duplicateIds = group.contacts.slice(1).map(contact => contact.id)
  try {
    await ElMessageBox.confirm(
      `Merge ${group.contacts.length} contacts for key "${group.signature}" into "${primary.displayName}"?`,
      'Merge Duplicates',
      {
        confirmButtonText: 'Merge',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  await mergeDuplicateContacts(primary.id, duplicateIds)
  ElMessage.success('Duplicate contacts merged')
  await refreshData()
}

onMounted(async () => {
  await refreshData()
})
</script>

<template>
  <div class="mm-page">
    <section class="mm-card panel">
      <h1 class="mm-section-title">Contacts</h1>
      <div class="filters">
        <el-input v-model="keyword" placeholder="Search by name or email" clearable @keyup.enter="onSearch" />
        <el-switch v-model="favoriteOnly" active-text="Favorites only" @change="onSearch" />
        <el-button type="primary" @click="onSearch">Search</el-button>
      </div>

      <div class="create-grid">
        <el-input v-model="createForm.displayName" placeholder="Display name" />
        <el-input v-model="createForm.email" placeholder="Email" />
        <el-input v-model="createForm.note" placeholder="Note (optional)" />
        <el-button type="success" @click="onCreate">Create Contact</el-button>
      </div>

      <el-table :data="contacts" v-loading="contactsLoading" style="width: 100%">
        <el-table-column prop="displayName" label="Name" min-width="160" />
        <el-table-column prop="email" label="Email" min-width="220" />
        <el-table-column label="Favorite" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.isFavorite" size="small" type="warning" effect="plain">Favorite</el-tag>
            <span v-else class="muted">No</span>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="Updated At" min-width="180" />
        <el-table-column label="Actions" width="260">
          <template #default="scope">
            <el-button type="primary" text @click="openEdit(scope.row)">Edit</el-button>
            <el-button type="warning" text @click="toggleFavorite(scope.row)">
              {{ scope.row.isFavorite ? 'Unfavorite' : 'Favorite' }}
            </el-button>
            <el-button type="danger" text @click="removeContact(scope.row.id)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="mm-card panel two-col">
      <div>
        <h2 class="mm-section-title">Groups</h2>
        <div class="group-create-grid">
          <el-input v-model="createGroupForm.name" placeholder="Group name" />
          <el-input v-model="createGroupForm.description" placeholder="Description (optional)" />
          <el-button type="success" @click="onCreateGroup">Create Group</el-button>
        </div>
        <el-table :data="groups" v-loading="groupsLoading" style="width: 100%" @row-click="onSelectGroup">
          <el-table-column prop="name" label="Group" min-width="160" />
          <el-table-column prop="memberCount" label="Members" width="100" />
          <el-table-column prop="updatedAt" label="Updated At" min-width="170" />
          <el-table-column label="Actions" width="120">
            <template #default="scope">
              <el-button type="danger" text @click.stop="onDeleteGroup(scope.row.id)">Delete</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div>
        <h2 class="mm-section-title">Group Members</h2>
        <div v-if="selectedGroup" class="group-editor">
          <el-input v-model="groupEditForm.name" placeholder="Group name" />
          <el-input v-model="groupEditForm.description" placeholder="Description" />
          <el-button type="primary" @click="onSaveGroupEdit">Save Group</el-button>
        </div>
        <div v-else class="muted">Select a group to manage members.</div>

        <div v-if="selectedGroup" class="member-selector">
          <el-select v-model="selectedMemberIds" filterable multiple collapse-tags placeholder="Select contacts to add">
            <el-option v-for="contact in contacts" :key="contact.id" :label="`${contact.displayName} <${contact.email}>`" :value="contact.id" />
          </el-select>
          <el-button type="primary" @click="onAddGroupMembers">Add Members</el-button>
        </div>

        <el-table :data="groupMembers" style="width: 100%" v-if="selectedGroup">
          <el-table-column prop="displayName" label="Name" min-width="150" />
          <el-table-column prop="signature" label="Duplicate Key" min-width="220" />
          <el-table-column label="Actions" width="110">
            <template #default="scope">
              <el-button type="danger" text @click="onRemoveGroupMember(scope.row.id)">Remove</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <section class="mm-card panel two-col">
      <div>
        <h2 class="mm-section-title">Import / Export</h2>
        <el-input
          v-model="importCsvText"
          type="textarea"
          :rows="8"
          placeholder="displayName,email,note,isFavorite&#10;Alice,alice@mmmail.local,,true"
        />
        <div class="import-actions">
          <el-switch v-model="importMergeDuplicates" active-text="Merge duplicates" />
          <el-button type="primary" @click="onImportCsv">Import CSV</el-button>
          <el-button @click="onExport('csv')">Export CSV</el-button>
          <el-button @click="onExport('vcard')">Export vCard</el-button>
        </div>
        <div v-if="importResult" class="import-result">
          <el-tag type="success">Created: {{ importResult.created }}</el-tag>
          <el-tag type="success">Updated: {{ importResult.updated }}</el-tag>
          <el-tag type="warning">Skipped: {{ importResult.skipped }}</el-tag>
          <el-tag>Invalid: {{ importResult.invalid }}</el-tag>
          <el-tag>Total: {{ importResult.totalRows }}</el-tag>
        </div>
      </div>

      <div>
        <h2 class="mm-section-title">Duplicates</h2>
        <el-table :data="duplicates" style="width: 100%">
          <el-table-column prop="signature" label="Duplicate Key" min-width="220" />
          <el-table-column prop="count" label="Count" width="90" />
          <el-table-column label="Contacts" min-width="240">
            <template #default="scope">
              <span>{{ scope.row.contacts.map((item: ContactItem) => item.displayName).join(', ') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Actions" width="120">
            <template #default="scope">
              <el-button type="warning" text @click="onMergeDuplicate(scope.row)">Merge</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <el-dialog v-model="editDialogVisible" title="Edit Contact" width="560px">
      <div class="edit-grid">
        <el-form-item label="Display Name">
          <el-input v-model="editForm.displayName" />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="editForm.email" />
        </el-form-item>
        <el-form-item label="Note">
          <el-input v-model="editForm.note" type="textarea" :rows="3" />
        </el-form-item>
      </div>
      <template #footer>
        <el-button @click="editDialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="submitEdit">Save</el-button>
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
