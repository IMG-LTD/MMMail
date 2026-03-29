import { computed, onMounted, reactive, ref } from 'vue'
import type { ComputedRef, Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useContactApi } from '~/composables/useContactApi'
import { useI18n } from '~/composables/useI18n'
import type {
  ContactDuplicateGroup,
  ContactGroup,
  ContactImportResult,
  ContactItem,
  CreateContactRequest,
  UpdateContactRequest
} from '~/types/api'

const CSV_EXPORT_EXTENSION = 'csv'
const VCARD_EXPORT_EXTENSION = 'vcf'
const CSV_EXPORT_MIME = 'text/csv;charset=utf-8'
const VCARD_EXPORT_MIME = 'text/vcard;charset=utf-8'
const CONTACT_EXPORT_FILENAME = 'mmmail-contacts'

interface ContactFormState {
  displayName: string
  email: string
  note: string
}

interface ContactGroupFormState {
  name: string
  description: string
}

interface ContactsWorkspaceState {
  contactsLoading: Ref<boolean>
  groupsLoading: Ref<boolean>
  contacts: Ref<ContactItem[]>
  groups: Ref<ContactGroup[]>
  groupMembers: Ref<ContactItem[]>
  duplicates: Ref<ContactDuplicateGroup[]>
  selectedGroupId: Ref<string>
  selectedMemberIds: Ref<string[]>
  keyword: Ref<string>
  favoriteOnly: Ref<boolean>
  importCsvText: Ref<string>
  importMergeDuplicates: Ref<boolean>
  importResult: Ref<ContactImportResult | null>
  editDialogVisible: Ref<boolean>
  editingContactId: Ref<string>
  createForm: ContactFormState
  editForm: ContactFormState
  createGroupForm: ContactGroupFormState
  groupEditForm: ContactGroupFormState
  selectedGroup: ComputedRef<ContactGroup | null>
}

type ContactApi = ReturnType<typeof useContactApi>
type TranslateFn = ReturnType<typeof useI18n>['t']

interface ContactsWorkspaceContext {
  state: ContactsWorkspaceState
  contactApi: ContactApi
  t: TranslateFn
}

function resetContactForm(form: ContactFormState): void {
  form.displayName = ''
  form.email = ''
  form.note = ''
}

function resetGroupForm(form: ContactGroupFormState): void {
  form.name = ''
  form.description = ''
}

function trimOptional(value: string): string | undefined {
  const normalized = value.trim()
  return normalized || undefined
}

function isDialogDismissed(error: unknown): boolean {
  return error === 'cancel' || error === 'close'
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

function exportConfig(format: 'csv' | 'vcard') {
  if (format === 'csv') {
    return {
      extension: CSV_EXPORT_EXTENSION,
      label: CSV_EXPORT_EXTENSION.toUpperCase(),
      mime: CSV_EXPORT_MIME
    }
  }

  return {
    extension: VCARD_EXPORT_EXTENSION,
    label: 'VCARD',
    mime: VCARD_EXPORT_MIME
  }
}

function createContactsWorkspaceState(): ContactsWorkspaceState {
  const groups = ref<ContactGroup[]>([])
  const selectedGroupId = ref('')

  return {
    contactsLoading: ref(false),
    groupsLoading: ref(false),
    contacts: ref<ContactItem[]>([]),
    groups,
    groupMembers: ref<ContactItem[]>([]),
    duplicates: ref<ContactDuplicateGroup[]>([]),
    selectedGroupId,
    selectedMemberIds: ref<string[]>([]),
    keyword: ref(''),
    favoriteOnly: ref(false),
    importCsvText: ref(''),
    importMergeDuplicates: ref(true),
    importResult: ref<ContactImportResult | null>(null),
    editDialogVisible: ref(false),
    editingContactId: ref(''),
    createForm: reactive({ displayName: '', email: '', note: '' }),
    editForm: reactive({ displayName: '', email: '', note: '' }),
    createGroupForm: reactive({ name: '', description: '' }),
    groupEditForm: reactive({ name: '', description: '' }),
    selectedGroup: computed(() => groups.value.find((group) => group.id === selectedGroupId.value) || null)
  }
}

async function loadContacts(context: ContactsWorkspaceContext): Promise<void> {
  const { state, contactApi } = context
  state.contactsLoading.value = true
  try {
    state.contacts.value = await contactApi.listContacts(state.keyword.value, state.favoriteOnly.value)
  } finally {
    state.contactsLoading.value = false
  }
}

async function loadGroups(context: ContactsWorkspaceContext): Promise<void> {
  const { state, contactApi } = context
  state.groupsLoading.value = true
  try {
    state.groups.value = await contactApi.listGroups()
    if (!state.selectedGroupId.value) {
      return
    }
    const exists = state.groups.value.some((group) => group.id === state.selectedGroupId.value)
    if (exists) {
      return
    }
    state.selectedGroupId.value = ''
    state.groupMembers.value = []
  } finally {
    state.groupsLoading.value = false
  }
}

async function loadGroupMembers(context: ContactsWorkspaceContext, groupId: string): Promise<void> {
  context.state.groupMembers.value = await context.contactApi.listGroupMembers(groupId)
}

async function loadDuplicates(context: ContactsWorkspaceContext): Promise<void> {
  context.state.duplicates.value = await context.contactApi.listDuplicateContacts()
}

async function refreshData(context: ContactsWorkspaceContext): Promise<void> {
  await Promise.all([loadContacts(context), loadGroups(context), loadDuplicates(context)])
  if (!context.state.selectedGroupId.value) {
    return
  }
  await loadGroupMembers(context, context.state.selectedGroupId.value)
}

async function onCreate(context: ContactsWorkspaceContext): Promise<void> {
  const { state, contactApi, t } = context
  if (!state.createForm.displayName.trim() || !state.createForm.email.trim()) {
    ElMessage.warning(t('contacts.messages.contactRequired'))
    return
  }

  const payload: CreateContactRequest = {
    displayName: state.createForm.displayName.trim(),
    email: state.createForm.email.trim(),
    note: trimOptional(state.createForm.note)
  }
  await contactApi.createContact(payload)
  ElMessage.success(t('contacts.messages.contactCreated'))
  resetContactForm(state.createForm)
  await refreshData(context)
}

function openEdit(context: ContactsWorkspaceContext, contact: ContactItem): void {
  const { state } = context
  state.editingContactId.value = contact.id
  state.editForm.displayName = contact.displayName
  state.editForm.email = contact.email
  state.editForm.note = contact.note || ''
  state.editDialogVisible.value = true
}

async function submitEdit(context: ContactsWorkspaceContext): Promise<void> {
  const { state, contactApi, t } = context
  if (!state.editingContactId.value) {
    return
  }
  if (!state.editForm.displayName.trim() || !state.editForm.email.trim()) {
    ElMessage.warning(t('contacts.messages.contactRequired'))
    return
  }

  const payload: UpdateContactRequest = {
    displayName: state.editForm.displayName.trim(),
    email: state.editForm.email.trim(),
    note: trimOptional(state.editForm.note)
  }
  await contactApi.updateContact(state.editingContactId.value, payload)
  ElMessage.success(t('contacts.messages.contactUpdated'))
  state.editDialogVisible.value = false
  await refreshData(context)
}

async function confirmDeleteContact(context: ContactsWorkspaceContext): Promise<boolean> {
  const { t } = context
  try {
    await ElMessageBox.confirm(
      t('contacts.confirm.deleteContactMessage'),
      t('contacts.confirm.deleteContactTitle'),
      {
        confirmButtonText: t('contacts.confirm.deleteAction'),
        cancelButtonText: t('contacts.actions.cancel'),
        type: 'warning'
      }
    )
    return true
  } catch (error) {
    if (isDialogDismissed(error)) {
      return false
    }
    throw error
  }
}

async function removeContact(context: ContactsWorkspaceContext, contactId: string): Promise<void> {
  if (!await confirmDeleteContact(context)) {
    return
  }
  await context.contactApi.deleteContact(contactId)
  ElMessage.success(context.t('contacts.messages.contactDeleted'))
  await refreshData(context)
}

async function toggleFavorite(context: ContactsWorkspaceContext, contact: ContactItem): Promise<void> {
  if (contact.isFavorite) {
    await context.contactApi.unfavoriteContact(contact.id)
    ElMessage.success(context.t('contacts.messages.favoriteRemoved'))
  } else {
    await context.contactApi.favoriteContact(contact.id)
    ElMessage.success(context.t('contacts.messages.favoriteAdded'))
  }
  await refreshData(context)
}

async function onCreateGroup(context: ContactsWorkspaceContext): Promise<void> {
  const { state, contactApi, t } = context
  if (!state.createGroupForm.name.trim()) {
    ElMessage.warning(t('contacts.messages.groupNameRequired'))
    return
  }

  await contactApi.createGroup({
    name: state.createGroupForm.name.trim(),
    description: trimOptional(state.createGroupForm.description)
  })
  ElMessage.success(t('contacts.messages.groupCreated'))
  resetGroupForm(state.createGroupForm)
  await loadGroups(context)
}

async function onSelectGroup(context: ContactsWorkspaceContext, group: ContactGroup): Promise<void> {
  const { state } = context
  state.selectedGroupId.value = group.id
  state.groupEditForm.name = group.name
  state.groupEditForm.description = group.description || ''
  state.selectedMemberIds.value = []
  await loadGroupMembers(context, group.id)
}

async function onSaveGroupEdit(context: ContactsWorkspaceContext): Promise<void> {
  const { state, contactApi, t } = context
  if (!state.selectedGroupId.value) {
    return
  }
  if (!state.groupEditForm.name.trim()) {
    ElMessage.warning(t('contacts.messages.groupNameRequired'))
    return
  }

  await contactApi.updateGroup(state.selectedGroupId.value, {
    name: state.groupEditForm.name.trim(),
    description: trimOptional(state.groupEditForm.description)
  })
  ElMessage.success(t('contacts.messages.groupUpdated'))
  await loadGroups(context)
}

async function confirmDeleteGroup(context: ContactsWorkspaceContext): Promise<boolean> {
  const { t } = context
  try {
    await ElMessageBox.confirm(
      t('contacts.confirm.deleteGroupMessage'),
      t('contacts.confirm.deleteGroupTitle'),
      {
        confirmButtonText: t('contacts.confirm.deleteAction'),
        cancelButtonText: t('contacts.actions.cancel'),
        type: 'warning'
      }
    )
    return true
  } catch (error) {
    if (isDialogDismissed(error)) {
      return false
    }
    throw error
  }
}

async function onDeleteGroup(context: ContactsWorkspaceContext, groupId: string): Promise<void> {
  if (!await confirmDeleteGroup(context)) {
    return
  }
  const { state, contactApi, t } = context
  await contactApi.deleteGroup(groupId)
  ElMessage.success(t('contacts.messages.groupDeleted'))
  if (state.selectedGroupId.value === groupId) {
    state.selectedGroupId.value = ''
    state.groupMembers.value = []
  }
  await loadGroups(context)
}

async function onAddGroupMembers(context: ContactsWorkspaceContext): Promise<void> {
  const { state, contactApi, t } = context
  if (!state.selectedGroupId.value || state.selectedMemberIds.value.length === 0) {
    ElMessage.warning(t('contacts.messages.selectContactsFirst'))
    return
  }

  await contactApi.addGroupMembers(state.selectedGroupId.value, {
    contactIds: state.selectedMemberIds.value
  })
  ElMessage.success(t('contacts.messages.groupMembersUpdated'))
  state.selectedMemberIds.value = []
  await Promise.all([loadGroups(context), loadGroupMembers(context, state.selectedGroupId.value)])
}

async function onRemoveGroupMember(context: ContactsWorkspaceContext, contactId: string): Promise<void> {
  const { state, contactApi, t } = context
  if (!state.selectedGroupId.value) {
    return
  }

  await contactApi.removeGroupMember(state.selectedGroupId.value, contactId)
  ElMessage.success(t('contacts.messages.memberRemoved'))
  await Promise.all([loadGroups(context), loadGroupMembers(context, state.selectedGroupId.value)])
}

async function onImportCsv(context: ContactsWorkspaceContext): Promise<void> {
  const { state, contactApi, t } = context
  if (!state.importCsvText.value.trim()) {
    ElMessage.warning(t('contacts.messages.csvRequired'))
    return
  }

  state.importResult.value = await contactApi.importContactsCsv(
    state.importCsvText.value,
    state.importMergeDuplicates.value
  )
  ElMessage.success(t('contacts.messages.contactsImported'))
  await refreshData(context)
}

async function onExport(context: ContactsWorkspaceContext, format: 'csv' | 'vcard'): Promise<void> {
  const exportedText = await context.contactApi.exportContacts(format)
  const config = exportConfig(format)
  downloadText(`${CONTACT_EXPORT_FILENAME}.${config.extension}`, exportedText, config.mime)
  ElMessage.success(context.t('contacts.messages.contactsExported', { format: config.label }))
}

async function confirmMergeDuplicate(
  context: ContactsWorkspaceContext,
  group: ContactDuplicateGroup
): Promise<boolean> {
  const primary = group.contacts[0]
  try {
    await ElMessageBox.confirm(
      context.t('contacts.confirm.mergeMessage', {
        count: group.contacts.length,
        primary: primary.displayName,
        signature: group.signature
      }),
      context.t('contacts.confirm.mergeTitle'),
      {
        confirmButtonText: context.t('contacts.confirm.mergeAction'),
        cancelButtonText: context.t('contacts.actions.cancel'),
        type: 'warning'
      }
    )
    return true
  } catch (error) {
    if (isDialogDismissed(error)) {
      return false
    }
    throw error
  }
}

async function onMergeDuplicate(
  context: ContactsWorkspaceContext,
  group: ContactDuplicateGroup
): Promise<void> {
  if (group.contacts.length < 2 || !await confirmMergeDuplicate(context, group)) {
    return
  }

  const primary = group.contacts[0]
  const duplicateIds = group.contacts.slice(1).map((contact) => contact.id)
  await context.contactApi.mergeDuplicateContacts(primary.id, duplicateIds)
  ElMessage.success(context.t('contacts.messages.duplicatesMerged'))
  await refreshData(context)
}

function bindContactActions(context: ContactsWorkspaceContext) {
  return {
    onSearch: () => loadContacts(context),
    onCreate: () => onCreate(context),
    openEdit: (contact: ContactItem) => openEdit(context, contact),
    closeEditDialog: () => {
      context.state.editDialogVisible.value = false
    },
    submitEdit: () => submitEdit(context),
    removeContact: (contactId: string) => removeContact(context, contactId),
    toggleFavorite: (contact: ContactItem) => toggleFavorite(context, contact)
  }
}

function bindGroupActions(context: ContactsWorkspaceContext) {
  return {
    onCreateGroup: () => onCreateGroup(context),
    onSelectGroup: (group: ContactGroup) => onSelectGroup(context, group),
    onSaveGroupEdit: () => onSaveGroupEdit(context),
    onDeleteGroup: (groupId: string) => onDeleteGroup(context, groupId),
    onAddGroupMembers: () => onAddGroupMembers(context),
    onRemoveGroupMember: (contactId: string) => onRemoveGroupMember(context, contactId)
  }
}

function bindImportActions(context: ContactsWorkspaceContext) {
  return {
    onImportCsv: () => onImportCsv(context),
    onExport: (format: 'csv' | 'vcard') => onExport(context, format),
    onMergeDuplicate: (group: ContactDuplicateGroup) => onMergeDuplicate(context, group)
  }
}

export function useContactsWorkspace() {
  const { t } = useI18n()
  const state = createContactsWorkspaceState()
  const context: ContactsWorkspaceContext = {
    state,
    contactApi: useContactApi(),
    t
  }

  onMounted(async () => {
    await refreshData(context)
  })

  return {
    ...state,
    ...bindContactActions(context),
    ...bindGroupActions(context),
    ...bindImportActions(context)
  }
}
