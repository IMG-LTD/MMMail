<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { OrgAuditEvent, OrgWorkspace } from '~/types/api'
import type {
  CreatePassAliasContactRequest,
  CreatePassMailboxRequest,
  CreatePassMailAliasRequest,
  PassAliasContact,
  PassBusinessPolicyForm,
  PassBusinessOverview,
  PassMailbox,
  PassBusinessPolicy,
  PassItemType,
  PassMailAlias,
  PassSecureLinkDashboardEntry,
  PassSecureLink,
  PassSharedVault,
  PassSharedVaultMember,
  PassVaultRole,
  PassWorkspaceItemDetail,
  PassWorkspaceItemSummary,
  PassWorkspaceMode,
  UpdatePassAliasContactRequest,
  UpdatePassMailAliasRequest,
  VerifyPassMailboxRequest
} from '~/types/pass-business'
import { useI18n } from '~/composables/useI18n'
import { useOrganizationApi } from '~/composables/useOrganizationApi'
import { usePassApi } from '~/composables/usePassApi'
import { usePassBusinessSharing } from '~/composables/usePassBusinessSharing'
import {
  buildGeneratorPayload,
  PASS_ITEM_TYPE_OPTIONS,
  buildAliasComposeQuery,
  canCreateSharedVault,
  canManagePassPolicy,
  buildDefaultSecureLinkExpiryValue,
  DEFAULT_SECURE_LINK_VIEWS,
  formatPassItemType,
  formatPassTime,
  formatVaultRole,
  generatorLengthBounds,
  generatorPresetFromPolicy,
  isSecureLinkExpiryDisabled,
  placeholderSecretFromPolicy,
  resolveSelectedVault,
  workspaceTitle
} from '~/utils/pass'

const SECRET_REQUIRED_TYPES = new Set<PassItemType>(['LOGIN', 'PASSWORD', 'CARD', 'PASSKEY'])
const DEFAULT_SHARED_LIST_LIMIT = 200

const {
  listItems,
  listMailboxes,
  createMailbox,
  verifyMailbox,
  setDefaultMailbox,
  deleteMailbox,
  listAliases,
  createAlias,
  updateAlias,
  enableAlias,
  disableAlias,
  deleteAlias,
  listAliasContacts,
  createAliasContact,
  updateAliasContact,
  deleteAliasContact,
  createItem,
  getItem,
  updateItem,
  deleteItem,
  favoriteItem,
  unFavoriteItem,
  generatePassword,
  getBusinessOverview,
  getBusinessPolicy,
  updateBusinessPolicy,
  listSharedVaults,
  createSharedVault,
  listSharedVaultMembers,
  addSharedVaultMember,
  removeSharedVaultMember,
  listSharedItems,
  createSharedItem,
  getSharedItem,
  updateSharedItem,
  deleteSharedItem,
  listActivity,
  listSecureLinks,
  listOrgSecureLinks,
  createSecureLink,
  revokeSecureLink
} = usePassApi()

const { listOrganizations } = useOrganizationApi()
const { t } = useI18n()
const {
  itemShares,
  incomingSharedItems,
  selectedIncomingItemId,
  incomingSharedItemDetail,
  loadingItemShares,
  loadingIncomingSharedItems,
  loadingIncomingSharedItemDetail,
  clearItemShares,
  clearIncomingShares,
  loadItemShares,
  loadIncomingShares,
  loadIncomingShareDetail,
  addItemShare,
  deleteItemShare
} = usePassBusinessSharing()

const workspaceMode = ref<PassWorkspaceMode>('PERSONAL')
const organizations = ref<OrgWorkspace[]>([])
const selectedOrgId = ref('')
const overview = ref<PassBusinessOverview | null>(null)
const policy = ref<PassBusinessPolicy | null>(null)
const sharedVaults = ref<PassSharedVault[]>([])
const selectedVaultId = ref('')
const sharedMembers = ref<PassSharedVaultMember[]>([])
const mailboxes = ref<PassMailbox[]>([])
const aliases = ref<PassMailAlias[]>([])
const selectedAliasId = ref('')
const aliasContacts = ref<PassAliasContact[]>([])
const activity = ref<OrgAuditEvent[]>([])
const items = ref<PassWorkspaceItemSummary[]>([])
const secureLinks = ref<PassSecureLink[]>([])
const organizationSecureLinks = ref<PassSecureLinkDashboardEntry[]>([])
const activeItemId = ref('')

const keyword = ref('')
const favoriteOnly = ref(false)
const itemTypeFilter = ref<'ALL' | PassItemType>('ALL')
const loading = ref(false)
const loadingActivity = ref(false)
const loadingSecureLinks = ref(false)
const loadingOrganizationSecureLinks = ref(false)
const itemShareMutationId = ref('')
const loadingAliases = ref(false)
const loadingMailboxes = ref(false)
const loadingAliasContacts = ref(false)
const aliasMutationId = ref('')
const mailboxMutationId = ref('')
const aliasContactMutationId = ref('')
const creatingItem = ref(false)
const creatingVault = ref(false)
const saving = ref(false)
const deleting = ref(false)
const togglingFavorite = ref(false)
const generating = ref(false)
const savingPolicy = ref(false)
const addingMember = ref(false)
const creatingLink = ref(false)
const secureLinkMutationId = ref('')
const passwordVisible = ref(false)
const generatorDialogVisible = ref(false)
const secureLinkDialogVisible = ref(false)
const createVaultDialogVisible = ref(false)
const itemShareEmail = ref('')

const editor = reactive({
  title: '',
  itemType: 'LOGIN' as PassItemType,
  website: '',
  username: '',
  secretCiphertext: '',
  note: '',
  favorite: false,
  ownerEmail: '',
  updatedAt: ''
})

const generatorForm = reactive(generatorPresetFromPolicy(null))

const policyForm = reactive<PassBusinessPolicyForm>({
  minimumPasswordLength: 14,
  maximumPasswordLength: 64,
  requireUppercase: true,
  requireDigits: true,
  requireSymbols: true,
  allowMemorablePasswords: true,
  allowExternalSharing: true,
  allowItemSharing: true,
  allowSecureLinks: true,
  allowMemberVaultCreation: false,
  allowExport: false,
  forceTwoFactor: true,
  allowPasskeys: true,
  allowAliases: true
})

const memberForm = reactive({
  email: '',
  role: 'MEMBER' as PassVaultRole
})

const secureLinkForm = reactive({
  maxViews: DEFAULT_SECURE_LINK_VIEWS,
  expiresAt: buildDefaultSecureLinkExpiryValue()
})

const vaultForm = reactive({
  name: '',
  description: ''
})

const selectedOrg = computed(() => organizations.value.find(item => item.id === selectedOrgId.value) || null)
const selectedVault = computed(() => resolveSelectedVault(sharedVaults.value, selectedVaultId.value))
const selectedAlias = computed(() => aliases.value.find(item => item.id === selectedAliasId.value) || null)
const hasActiveItem = computed(() => Boolean(activeItemId.value))
const isSharedMode = computed(() => workspaceMode.value === 'SHARED')
const currentOrgRole = computed(() => overview.value?.currentRole || selectedOrg.value?.role || null)
const canManagePolicy = computed(() => canManagePassPolicy(currentOrgRole.value))
const canCreateVaultAction = computed(() => canCreateSharedVault(currentOrgRole.value, policy.value))
const itemSharingBlocked = computed(() => isSharedMode.value && policy.value?.allowItemSharing === false)
const secureLinkPolicyBlocked = computed(() => isSharedMode.value && policy.value?.allowSecureLinks === false)
const secureLinkExternalBlocked = computed(() => isSharedMode.value && policy.value?.allowExternalSharing === false)
const activeItemTitle = computed(() => editor.title || items.value.find(item => item.id === activeItemId.value)?.title || '')
const generatorBounds = computed(() => generatorLengthBounds(isSharedMode.value ? policy.value : null))
const selectedItemTypeFilter = computed(() => (itemTypeFilter.value === 'ALL' ? undefined : itemTypeFilter.value))
const workspaceHeading = computed(() => workspaceTitle(workspaceMode.value))
const forwardTargetOptions = computed(() => mailboxes.value
  .filter(mailbox => mailbox.status === 'VERIFIED')
  .map((mailbox) => ({
    label: [
      mailbox.mailboxEmail,
      mailbox.defaultMailbox ? 'Default' : 'Verified',
      mailbox.primaryMailbox ? 'Primary' : ''
    ].filter(Boolean).join(' · '),
    value: mailbox.mailboxEmail
  })))

function applyPolicyForm(value: PassBusinessPolicy | null): void {
  if (!value) {
    policyForm.minimumPasswordLength = 14
    policyForm.maximumPasswordLength = 64
    policyForm.requireUppercase = true
    policyForm.requireDigits = true
    policyForm.requireSymbols = true
    policyForm.allowMemorablePasswords = true
    policyForm.allowExternalSharing = true
    policyForm.allowItemSharing = true
    policyForm.allowSecureLinks = true
    policyForm.allowMemberVaultCreation = false
    policyForm.allowExport = false
    policyForm.forceTwoFactor = true
    policyForm.allowPasskeys = true
    policyForm.allowAliases = true
    return
  }
  policyForm.minimumPasswordLength = value.minimumPasswordLength
  policyForm.maximumPasswordLength = value.maximumPasswordLength
  policyForm.requireUppercase = value.requireUppercase
  policyForm.requireDigits = value.requireDigits
  policyForm.requireSymbols = value.requireSymbols
  policyForm.allowMemorablePasswords = value.allowMemorablePasswords
  policyForm.allowExternalSharing = value.allowExternalSharing
  policyForm.allowItemSharing = value.allowItemSharing
  policyForm.allowSecureLinks = value.allowSecureLinks
  policyForm.allowMemberVaultCreation = value.allowMemberVaultCreation
  policyForm.allowExport = value.allowExport
  policyForm.forceTwoFactor = value.forceTwoFactor
  policyForm.allowPasskeys = value.allowPasskeys
  policyForm.allowAliases = value.allowAliases
}

function applyItemToEditor(detail: PassWorkspaceItemDetail): void {
  activeItemId.value = detail.id
  editor.title = detail.title
  editor.itemType = detail.itemType
  editor.website = detail.website || ''
  editor.username = detail.username || ''
  editor.secretCiphertext = detail.secretCiphertext || ''
  editor.note = detail.note || ''
  editor.favorite = detail.favorite
  editor.ownerEmail = detail.ownerEmail || ''
  editor.updatedAt = detail.updatedAt
}

function clearEditor(): void {
  activeItemId.value = ''
  editor.title = ''
  editor.itemType = selectedItemTypeFilter.value || 'LOGIN'
  editor.website = ''
  editor.username = ''
  editor.secretCiphertext = ''
  editor.note = ''
  editor.favorite = false
  editor.ownerEmail = ''
  editor.updatedAt = ''
  secureLinks.value = []
  itemShareEmail.value = ''
  clearItemShares()
}

function defaultSecretForType(itemType: PassItemType): string {
  return SECRET_REQUIRED_TYPES.has(itemType) ? placeholderSecretFromPolicy(policy.value) : ''
}

async function refreshOrganizations(): Promise<void> {
  organizations.value = await listOrganizations()
  if (!selectedOrgId.value && organizations.value.length > 0) {
    selectedOrgId.value = organizations.value[0].id
  }
  if (selectedOrgId.value && !organizations.value.some(item => item.id === selectedOrgId.value)) {
    selectedOrgId.value = organizations.value[0]?.id || ''
  }
}

function resolveSelectedAliasId(nextAliases: PassMailAlias[]): string {
  if (!nextAliases.length) {
    return ''
  }
  return nextAliases.some((item) => item.id === selectedAliasId.value)
    ? selectedAliasId.value
    : nextAliases[0].id
}

async function loadAliasContacts(aliasId = selectedAliasId.value): Promise<void> {
  if (!aliasId) {
    aliasContacts.value = []
    return
  }
  loadingAliasContacts.value = true
  try {
    aliasContacts.value = await listAliasContacts(aliasId)
  } finally {
    loadingAliasContacts.value = false
  }
}

async function loadAliasCenter(): Promise<void> {
  loadingAliases.value = true
  loadingMailboxes.value = true
  try {
    const [nextAliases, nextMailboxes] = await Promise.all([
      listAliases(),
      listMailboxes()
    ])
    aliases.value = nextAliases
    mailboxes.value = nextMailboxes
    selectedAliasId.value = resolveSelectedAliasId(nextAliases)
    await loadAliasContacts(selectedAliasId.value)
  } finally {
    loadingAliases.value = false
    loadingMailboxes.value = false
  }
}

async function loadPersonalItems(keepSelection = true): Promise<void> {
  const next = await listItems(keyword.value.trim(), favoriteOnly.value, 200, selectedItemTypeFilter.value)
  items.value = next
  if (!next.length) {
    clearEditor()
    return
  }
  if (!keepSelection || !activeItemId.value || !next.some(item => item.id === activeItemId.value)) {
    await selectItem(next[0].id)
  }
}

async function loadSharedWorkspace(keepSelection = true): Promise<void> {
  if (!selectedOrgId.value) {
    overview.value = null
    policy.value = null
    sharedVaults.value = []
    sharedMembers.value = []
    activity.value = []
    items.value = []
    organizationSecureLinks.value = []
    selectedVaultId.value = ''
    clearEditor()
    clearIncomingShares()
    return
  }
  const [nextOverview, nextPolicy, nextVaults] = await Promise.all([
    getBusinessOverview(selectedOrgId.value),
    getBusinessPolicy(selectedOrgId.value),
    listSharedVaults(selectedOrgId.value, keyword.value.trim())
  ])
  overview.value = nextOverview
  policy.value = nextPolicy
  applyPolicyForm(nextPolicy)
  sharedVaults.value = nextVaults
  await Promise.all([
    loadActivityFeed(),
    loadOrgSecureLinks(),
    loadIncomingShares({
      orgId: selectedOrgId.value,
      keyword: keyword.value.trim(),
      favoriteOnly: favoriteOnly.value,
      limit: DEFAULT_SHARED_LIST_LIMIT,
      itemType: selectedItemTypeFilter.value,
      keepSelection
    })
  ])
  if (!nextVaults.length) {
    selectedVaultId.value = ''
    sharedMembers.value = []
    items.value = []
    clearEditor()
    return
  }
  if (!keepSelection || !selectedVaultId.value || !nextVaults.some(item => item.id === selectedVaultId.value)) {
    selectedVaultId.value = nextVaults[0].id
  }
  await loadSelectedVault(keepSelection)
}

async function loadSelectedVault(keepSelection = true): Promise<void> {
  if (!selectedOrgId.value || !selectedVaultId.value) {
    sharedMembers.value = []
    items.value = []
    clearEditor()
    return
  }
  const [members, nextItems] = await Promise.all([
    listSharedVaultMembers(selectedOrgId.value, selectedVaultId.value),
    listSharedItems(
      selectedOrgId.value,
      selectedVaultId.value,
      keyword.value.trim(),
      favoriteOnly.value,
      DEFAULT_SHARED_LIST_LIMIT,
      selectedItemTypeFilter.value
    )
  ])
  sharedMembers.value = members
  items.value = nextItems
  if (!nextItems.length) {
    clearEditor()
    return
  }
  if (!keepSelection || !activeItemId.value || !nextItems.some(item => item.id === activeItemId.value)) {
    await selectItem(nextItems[0].id)
  }
}

async function loadOrgSecureLinks(): Promise<void> {
  if (!selectedOrgId.value || !isSharedMode.value) {
    organizationSecureLinks.value = []
    return
  }
  loadingOrganizationSecureLinks.value = true
  try {
    organizationSecureLinks.value = await listOrgSecureLinks(selectedOrgId.value)
  } finally {
    loadingOrganizationSecureLinks.value = false
  }
}

async function loadActivityFeed(): Promise<void> {
  if (!selectedOrgId.value) {
    activity.value = []
    return
  }
  loadingActivity.value = true
  try {
    activity.value = await listActivity(selectedOrgId.value, 30)
  } finally {
    loadingActivity.value = false
  }
}

async function loadSecureLinkList(itemId: string): Promise<void> {
  if (!selectedOrgId.value || !isSharedMode.value) {
    secureLinks.value = []
    return
  }
  loadingSecureLinks.value = true
  try {
    secureLinks.value = await listSecureLinks(selectedOrgId.value, itemId)
  } finally {
    loadingSecureLinks.value = false
  }
}

async function selectItem(itemId: string): Promise<void> {
  try {
    const detail = isSharedMode.value
      ? await getSharedItem(selectedOrgId.value, selectedVaultId.value, itemId)
      : await getItem(itemId)
    applyItemToEditor(detail)
    if (isSharedMode.value) {
      await Promise.all([
        loadSecureLinkList(itemId),
        loadItemShares(selectedOrgId.value, itemId)
      ])
      return
    }
    clearItemShares()
  } catch (error) {
    ElMessage.error((error as Error).message || 'Load pass item failed')
  }
}

async function refreshWorkspace(keepSelection = true): Promise<void> {
  loading.value = true
  try {
    if (isSharedMode.value) {
      await loadSharedWorkspace(keepSelection)
      return
    }
    organizationSecureLinks.value = []
    secureLinks.value = []
    clearIncomingShares()
    await Promise.all([loadPersonalItems(keepSelection), loadAliasCenter()])
  } catch (error) {
    ElMessage.error((error as Error).message || 'Load pass workspace failed')
  } finally {
    loading.value = false
  }
}

async function onWorkspaceModeChange(mode: PassWorkspaceMode): Promise<void> {
  workspaceMode.value = mode
  clearEditor()
  await refreshWorkspace(false)
}

async function onOrgChange(orgId: string): Promise<void> {
  selectedOrgId.value = orgId
  clearEditor()
  await refreshWorkspace(false)
}

async function onSelectVault(vaultId: string): Promise<void> {
  selectedVaultId.value = vaultId
  clearEditor()
  await loadSelectedVault(false)
}

async function onCreateItem(): Promise<void> {
  const itemType = selectedItemTypeFilter.value || 'LOGIN'
  creatingItem.value = true
  try {
    const suffix = new Date().toISOString().slice(0, 16).replace('T', ' ')
    const payload = {
      title: `${isSharedMode.value ? 'Shared' : 'Personal'} ${formatPassItemType(itemType)} ${suffix}`,
      itemType,
      website: '',
      username: '',
      secretCiphertext: defaultSecretForType(itemType),
      note: ''
    }
    const created = isSharedMode.value
      ? await createSharedItem(selectedOrgId.value, selectedVaultId.value, payload)
      : await createItem(payload)
    ElMessage.success(`${isSharedMode.value ? 'Shared item' : 'Credential'} created`)
    await refreshWorkspace(false)
    await selectItem(created.id)
  } catch (error) {
    ElMessage.error((error as Error).message || 'Create pass item failed')
  } finally {
    creatingItem.value = false
  }
}

async function onSave(): Promise<void> {
  if (!activeItemId.value) {
    ElMessage.warning('Select an item first')
    return
  }
  if (!editor.title.trim()) {
    ElMessage.warning('Title is required')
    return
  }
  if (SECRET_REQUIRED_TYPES.has(editor.itemType) && !editor.secretCiphertext.trim()) {
    ElMessage.warning('Secret is required for this item type')
    return
  }
  saving.value = true
  try {
    const payload = {
      title: editor.title.trim(),
      itemType: editor.itemType,
      website: editor.website.trim(),
      username: editor.username.trim(),
      secretCiphertext: editor.secretCiphertext,
      note: editor.note
    }
    const updated = isSharedMode.value
      ? await updateSharedItem(selectedOrgId.value, selectedVaultId.value, activeItemId.value, payload)
      : await updateItem(activeItemId.value, payload)
    applyItemToEditor(updated)
    ElMessage.success('Item saved')
    await refreshWorkspace(true)
  } catch (error) {
    ElMessage.error((error as Error).message || 'Save pass item failed')
  } finally {
    saving.value = false
  }
}

async function onDelete(): Promise<void> {
  if (!activeItemId.value) {
    ElMessage.warning('Select an item first')
    return
  }
  try {
    await ElMessageBox.confirm('Delete current pass item?', 'Delete Item', {
      type: 'warning',
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel'
    })
  } catch {
    return
  }
  deleting.value = true
  try {
    if (isSharedMode.value) {
      await deleteSharedItem(selectedOrgId.value, selectedVaultId.value, activeItemId.value)
    } else {
      await deleteItem(activeItemId.value)
    }
    ElMessage.success('Item deleted')
    await refreshWorkspace(false)
  } catch (error) {
    ElMessage.error((error as Error).message || 'Delete pass item failed')
  } finally {
    deleting.value = false
  }
}

async function onToggleFavorite(): Promise<void> {
  if (!activeItemId.value || isSharedMode.value) {
    return
  }
  togglingFavorite.value = true
  try {
    const next = editor.favorite ? await unFavoriteItem(activeItemId.value) : await favoriteItem(activeItemId.value)
    applyItemToEditor(next)
    ElMessage.success(next.favorite ? 'Added to favorites' : 'Removed from favorites')
    await refreshWorkspace(true)
  } catch (error) {
    ElMessage.error((error as Error).message || 'Toggle favorite failed')
  } finally {
    togglingFavorite.value = false
  }
}

function openGenerator(): void {
  Object.assign(generatorForm, generatorPresetFromPolicy(isSharedMode.value ? policy.value : null))
  generatorDialogVisible.value = true
}

async function onGenerateConfirm(): Promise<void> {
  generating.value = true
  try {
    const payload = buildGeneratorPayload(
      isSharedMode.value ? selectedOrgId.value : undefined,
      generatorForm,
      isSharedMode.value ? policy.value : null
    )
    const result = await generatePassword(payload)
    editor.secretCiphertext = result.password
    generatorDialogVisible.value = false
    ElMessage.success(t('pass.generator.messages.generated'))
  } catch (error) {
    ElMessage.error((error as Error).message || 'Generate password failed')
  } finally {
    generating.value = false
  }
}

async function onCreateItemShare(): Promise<void> {
  if (!selectedOrgId.value || !activeItemId.value || !itemShareEmail.value.trim()) {
    return
  }
  itemShareMutationId.value = 'create'
  try {
    await addItemShare(selectedOrgId.value, activeItemId.value, itemShareEmail.value.trim())
    itemShareEmail.value = ''
    await loadActivityFeed()
    ElMessage.success(t('pass.sharing.direct.messages.created'))
  } catch (error) {
    ElMessage.error((error as Error).message || 'Create item share failed')
  } finally {
    itemShareMutationId.value = ''
  }
}

async function onRemoveItemShare(shareId: string): Promise<void> {
  if (!selectedOrgId.value || !activeItemId.value) {
    return
  }
  itemShareMutationId.value = shareId
  try {
    await deleteItemShare(selectedOrgId.value, activeItemId.value, shareId)
    await loadActivityFeed()
    ElMessage.success(t('pass.sharing.direct.messages.removed'))
  } catch (error) {
    ElMessage.error((error as Error).message || 'Remove item share failed')
  } finally {
    itemShareMutationId.value = ''
  }
}

async function onRefreshIncomingShares(): Promise<void> {
  if (!selectedOrgId.value) {
    clearIncomingShares()
    return
  }
  try {
    await loadIncomingShares({
      orgId: selectedOrgId.value,
      keyword: keyword.value.trim(),
      favoriteOnly: favoriteOnly.value,
      limit: DEFAULT_SHARED_LIST_LIMIT,
      itemType: selectedItemTypeFilter.value,
      keepSelection: true
    })
  } catch (error) {
    ElMessage.error((error as Error).message || 'Load incoming item shares failed')
  }
}

async function onSelectIncomingItem(itemId: string): Promise<void> {
  if (!selectedOrgId.value) {
    return
  }
  try {
    await loadIncomingShareDetail(selectedOrgId.value, itemId)
  } catch (error) {
    ElMessage.error((error as Error).message || 'Load incoming shared item failed')
  }
}

async function onCopy(value: string, label: string): Promise<void> {
  if (!value) {
    ElMessage.warning(`${label} is empty`)
    return
  }
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success(`${label} copied`)
  } catch {
    ElMessage.error(`Copy ${label} failed`)
  }
}

async function onSavePolicy(): Promise<void> {
  if (!selectedOrgId.value || !canManagePolicy.value) {
    ElMessage.warning('Only OWNER or ADMIN can update Pass policy')
    return
  }
  savingPolicy.value = true
  try {
    const updated = await updateBusinessPolicy(selectedOrgId.value, { ...policyForm })
    policy.value = updated
    applyPolicyForm(updated)
    Object.assign(generatorForm, {
      ...generatorForm,
      length: Math.min(Math.max(generatorForm.length, updated.minimumPasswordLength), updated.maximumPasswordLength),
      memorable: updated.allowMemorablePasswords ? generatorForm.memorable : false
    })
    overview.value = await getBusinessOverview(selectedOrgId.value)
    ElMessage.success('Pass policy updated')
    await loadActivityFeed()
  } catch (error) {
    ElMessage.error((error as Error).message || 'Update pass policy failed')
  } finally {
    savingPolicy.value = false
  }
}

async function onCreateVault(): Promise<void> {
  if (!selectedOrgId.value) {
    ElMessage.warning('Select an organization first')
    return
  }
  creatingVault.value = true
  try {
    const created = await createSharedVault(selectedOrgId.value, {
      name: vaultForm.name.trim(),
      description: vaultForm.description.trim()
    })
    createVaultDialogVisible.value = false
    vaultForm.name = ''
    vaultForm.description = ''
    selectedVaultId.value = created.id
    await loadSharedWorkspace(false)
    ElMessage.success('Shared vault created')
  } catch (error) {
    ElMessage.error((error as Error).message || 'Create shared vault failed')
  } finally {
    creatingVault.value = false
  }
}

async function onAddMember(): Promise<void> {
  if (!selectedOrgId.value || !selectedVaultId.value) {
    ElMessage.warning('Select a shared vault first')
    return
  }
  addingMember.value = true
  try {
    await addSharedVaultMember(selectedOrgId.value, selectedVaultId.value, {
      email: memberForm.email.trim(),
      role: memberForm.role
    })
    memberForm.email = ''
    await loadSelectedVault(true)
    await loadActivityFeed()
    ElMessage.success('Vault member added')
  } catch (error) {
    ElMessage.error((error as Error).message || 'Add member failed')
  } finally {
    addingMember.value = false
  }
}

async function onRemoveMember(memberId: string): Promise<void> {
  if (!selectedOrgId.value || !selectedVaultId.value) {
    return
  }
  try {
    await ElMessageBox.confirm('Remove selected member from this vault?', 'Remove Member', {
      type: 'warning',
      confirmButtonText: 'Remove',
      cancelButtonText: 'Cancel'
    })
  } catch {
    return
  }
  try {
    await removeSharedVaultMember(selectedOrgId.value, selectedVaultId.value, memberId)
    await loadSelectedVault(true)
    await loadActivityFeed()
    ElMessage.success('Vault member removed')
  } catch (error) {
    ElMessage.error((error as Error).message || 'Remove member failed')
  }
}

function openSecureLinkDialog(): void {
  if (!isSharedMode.value || !activeItemId.value) {
    ElMessage.warning(t('pass.secureLinks.noItem'))
    return
  }
  if (secureLinkPolicyBlocked.value) {
    ElMessage.warning(t('pass.secureLinks.blocked'))
    return
  }
  if (secureLinkExternalBlocked.value) {
    ElMessage.warning(t('pass.secureLinks.externalBlocked'))
    return
  }
  resetSecureLinkForm()
  secureLinkDialogVisible.value = true
}

async function onCreateSecureLink(): Promise<void> {
  if (!selectedOrgId.value || !activeItemId.value) {
    return
  }
  creatingLink.value = true
  secureLinkMutationId.value = 'create'
  try {
    await createSecureLink(selectedOrgId.value, activeItemId.value, {
      maxViews: secureLinkForm.maxViews,
      expiresAt: secureLinkForm.expiresAt || null
    })
    secureLinkDialogVisible.value = false
    resetSecureLinkForm()
    await Promise.all([
      loadSecureLinkList(activeItemId.value),
      loadOrgSecureLinks(),
      loadActivityFeed()
    ])
    overview.value = await getBusinessOverview(selectedOrgId.value)
    ElMessage.success(t('pass.secureLinks.messages.created'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('pass.secureLinks.messages.createFailed'))
  } finally {
    creatingLink.value = false
    secureLinkMutationId.value = ''
  }
}

async function onSelectAlias(aliasId: string): Promise<void> {
  selectedAliasId.value = aliasId
  await loadAliasContacts(aliasId)
}

async function onCreateAliasContact(payload: CreatePassAliasContactRequest): Promise<void> {
  if (!selectedAliasId.value) {
    ElMessage.warning('Select an alias first')
    return
  }
  aliasContactMutationId.value = 'create'
  try {
    await createAliasContact(selectedAliasId.value, payload)
    await loadAliasContacts()
    ElMessage.success('Alias contact created')
  } catch (error) {
    ElMessage.error((error as Error).message || 'Create alias contact failed')
  } finally {
    aliasContactMutationId.value = ''
  }
}

async function onUpdateAliasContact(contactId: string, payload: UpdatePassAliasContactRequest): Promise<void> {
  if (!selectedAliasId.value) {
    ElMessage.warning('Select an alias first')
    return
  }
  aliasContactMutationId.value = `save:${contactId}`
  try {
    await updateAliasContact(selectedAliasId.value, contactId, payload)
    await loadAliasContacts()
    ElMessage.success('Alias contact updated')
  } catch (error) {
    ElMessage.error((error as Error).message || 'Update alias contact failed')
  } finally {
    aliasContactMutationId.value = ''
  }
}

async function onRemoveAliasContact(contactId: string): Promise<void> {
  if (!selectedAliasId.value) {
    return
  }
  try {
    await ElMessageBox.confirm('Delete this reverse alias contact permanently?', 'Delete Contact', {
      type: 'warning',
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel'
    })
  } catch {
    return
  }
  aliasContactMutationId.value = `delete:${contactId}`
  try {
    await deleteAliasContact(selectedAliasId.value, contactId)
    await loadAliasContacts()
    ElMessage.success('Alias contact deleted')
  } catch (error) {
    ElMessage.error((error as Error).message || 'Delete alias contact failed')
  } finally {
    aliasContactMutationId.value = ''
  }
}

async function onComposeAliasContact(reverseAliasEmail: string): Promise<void> {
  if (!selectedAlias.value) {
    ElMessage.warning('Select an alias first')
    return
  }
  await navigateTo({
    path: '/compose',
    query: buildAliasComposeQuery(selectedAlias.value.aliasEmail, reverseAliasEmail)
  })
}

async function onCopyAliasContact(reverseAliasEmail: string): Promise<void> {
  await onCopy(reverseAliasEmail, 'Reverse alias')
}

async function onCreateMailbox(payload: CreatePassMailboxRequest): Promise<void> {
  mailboxMutationId.value = 'create'
  try {
    await createMailbox(payload)
    await loadAliasCenter()
    ElMessage.success('Mailbox added, check the target inbox for the verification code')
  } catch (error) {
    ElMessage.error((error as Error).message || 'Add mailbox failed')
  } finally {
    mailboxMutationId.value = ''
  }
}

async function onVerifyMailbox(mailboxId: string, payload: VerifyPassMailboxRequest): Promise<void> {
  mailboxMutationId.value = `verify:${mailboxId}`
  try {
    await verifyMailbox(mailboxId, payload)
    await loadAliasCenter()
    ElMessage.success('Mailbox verified')
  } catch (error) {
    ElMessage.error((error as Error).message || 'Verify mailbox failed')
  } finally {
    mailboxMutationId.value = ''
  }
}

async function onSetDefaultMailbox(mailboxId: string): Promise<void> {
  mailboxMutationId.value = `default:${mailboxId}`
  try {
    await setDefaultMailbox(mailboxId)
    await loadAliasCenter()
    ElMessage.success('Default mailbox updated')
  } catch (error) {
    ElMessage.error((error as Error).message || 'Set default mailbox failed')
  } finally {
    mailboxMutationId.value = ''
  }
}

async function onRemoveMailbox(mailboxId: string): Promise<void> {
  try {
    await ElMessageBox.confirm('Delete this mailbox route permanently?', 'Delete Mailbox', {
      type: 'warning',
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel'
    })
  } catch {
    return
  }
  mailboxMutationId.value = `delete:${mailboxId}`
  try {
    await deleteMailbox(mailboxId)
    await loadAliasCenter()
    ElMessage.success('Mailbox deleted')
  } catch (error) {
    ElMessage.error((error as Error).message || 'Delete mailbox failed')
  } finally {
    mailboxMutationId.value = ''
  }
}

async function onCreateAlias(payload: CreatePassMailAliasRequest): Promise<void> {
  aliasMutationId.value = 'create'
  try {
    await createAlias(payload)
    await loadAliasCenter()
    ElMessage.success('Alias created')
  } catch (error) {
    ElMessage.error((error as Error).message || 'Create alias failed')
  } finally {
    aliasMutationId.value = ''
  }
}

async function onUpdateAlias(aliasId: string, payload: UpdatePassMailAliasRequest): Promise<void> {
  aliasMutationId.value = `save:${aliasId}`
  try {
    await updateAlias(aliasId, payload)
    await loadAliasCenter()
    ElMessage.success('Alias updated')
  } catch (error) {
    ElMessage.error((error as Error).message || 'Update alias failed')
  } finally {
    aliasMutationId.value = ''
  }
}

async function onEnableAlias(aliasId: string): Promise<void> {
  aliasMutationId.value = `enable:${aliasId}`
  try {
    await enableAlias(aliasId)
    await loadAliasCenter()
    ElMessage.success('Alias enabled')
  } catch (error) {
    ElMessage.error((error as Error).message || 'Enable alias failed')
  } finally {
    aliasMutationId.value = ''
  }
}

async function onDisableAlias(aliasId: string): Promise<void> {
  aliasMutationId.value = `disable:${aliasId}`
  try {
    await disableAlias(aliasId)
    await loadAliasCenter()
    ElMessage.success('Alias disabled')
  } catch (error) {
    ElMessage.error((error as Error).message || 'Disable alias failed')
  } finally {
    aliasMutationId.value = ''
  }
}

async function onRemoveAlias(aliasId: string): Promise<void> {
  try {
    await ElMessageBox.confirm('Delete this alias permanently?', 'Delete Alias', {
      type: 'warning',
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel'
    })
  } catch {
    return
  }
  aliasMutationId.value = `delete:${aliasId}`
  try {
    await deleteAlias(aliasId)
    await loadAliasCenter()
    ElMessage.success('Alias deleted')
  } catch (error) {
    ElMessage.error((error as Error).message || 'Delete alias failed')
  } finally {
    aliasMutationId.value = ''
  }
}

async function onRevokeLink(linkId: string): Promise<void> {
  if (!selectedOrgId.value || !activeItemId.value) {
    return
  }
  secureLinkMutationId.value = linkId
  try {
    await revokeSecureLink(selectedOrgId.value, linkId)
    await Promise.all([
      loadSecureLinkList(activeItemId.value),
      loadOrgSecureLinks(),
      loadActivityFeed()
    ])
    overview.value = await getBusinessOverview(selectedOrgId.value)
    ElMessage.success(t('pass.secureLinks.messages.revoked'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('pass.secureLinks.messages.revokeFailed'))
  } finally {
    secureLinkMutationId.value = ''
  }
}

async function onFocusSecureLinkItem(payload: { itemId: string; sharedVaultId: string }): Promise<void> {
  if (selectedVaultId.value !== payload.sharedVaultId) {
    selectedVaultId.value = payload.sharedVaultId
    clearEditor()
    await loadSelectedVault(false)
  }
  await selectItem(payload.itemId)
}

function resetSecureLinkForm(): void {
  secureLinkForm.maxViews = DEFAULT_SECURE_LINK_VIEWS
  secureLinkForm.expiresAt = buildDefaultSecureLinkExpiryValue()
}

async function onCopySecureLink(publicUrl: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(publicUrl)
    ElMessage.success(t('pass.secureLinks.messages.linkCopied'))
  } catch {
    ElMessage.error(t('pass.secureLinks.messages.copyFailed'))
  }
}

function onOpenSecureLink(publicUrl: string): void {
  window.open(publicUrl, '_blank', 'noopener,noreferrer')
}

onMounted(async () => {
  await refreshOrganizations()
  await refreshWorkspace(false)
})
</script>

<template>
  <section class="pass-page">
    <PassWorkspaceHero
      :workspace-mode="workspaceMode"
      :organizations="organizations"
      :selected-org-id="selectedOrgId"
      :overview="overview"
      :selected-vault-name="selectedVault?.name || ''"
      :loading="loading"
      @update:workspace-mode="onWorkspaceModeChange"
      @update:selected-org-id="onOrgChange"
      @refresh="refreshWorkspace(true)"
    />

    <section class="mm-card pass-command-bar">
      <el-input v-model="keyword" placeholder="Search items or vaults" @keyup.enter="refreshWorkspace(false)" />
      <el-select v-model="itemTypeFilter" class="filter-select">
        <el-option label="All types" value="ALL" />
        <el-option v-for="option in PASS_ITEM_TYPE_OPTIONS" :key="option.value" :label="option.label" :value="option.value" />
      </el-select>
      <el-switch v-model="favoriteOnly" inline-prompt active-text="Fav" inactive-text="All" @change="refreshWorkspace(false)" />
      <el-button :loading="loading" @click="refreshWorkspace(false)">Search</el-button>
      <el-button type="primary" :loading="creatingItem" :disabled="isSharedMode && !selectedVaultId" @click="onCreateItem">New Item</el-button>
      <el-button v-if="isSharedMode" plain :disabled="!canCreateVaultAction" @click="createVaultDialogVisible = true">New Vault</el-button>
    </section>

    <div class="pass-grid">
      <PassSidebarPanel
        :mode="workspaceMode"
        :items="!isSharedMode ? items : []"
        :shared-vaults="isSharedMode ? sharedVaults : []"
        :active-item-id="activeItemId"
        :active-vault-id="selectedVaultId"
        @select-item="selectItem"
        @select-vault="onSelectVault"
      />

      <section class="mm-card pass-center">
        <header class="section-head">
          <div>
            <p class="section-eyebrow">{{ workspaceHeading }}</p>
            <h2>{{ isSharedMode ? selectedVault?.name || 'Select a shared vault' : 'Credential Editor' }}</h2>
          </div>
          <span class="section-note">{{ isSharedMode ? `${items.length} shared items` : `${items.length} personal items` }}</span>
        </header>

        <template v-if="isSharedMode && selectedVault">
          <section class="vault-context">
            <div>
              <strong>{{ selectedVault.name }}</strong>
              <p>{{ selectedVault.description || 'No vault description yet.' }}</p>
            </div>
            <div class="vault-stats">
              <span>{{ selectedVault.memberCount }} members</span>
              <span>{{ selectedVault.itemCount }} items</span>
              <span>{{ formatVaultRole(selectedVault.accessRole) }}</span>
            </div>
          </section>

          <section class="member-strip">
            <div class="member-form">
              <el-input v-model="memberForm.email" placeholder="Add org member by email" />
              <el-select v-model="memberForm.role" class="role-select">
                <el-option label="Member" value="MEMBER" />
                <el-option label="Vault Manager" value="MANAGER" />
              </el-select>
              <el-button :loading="addingMember" @click="onAddMember">Add Member</el-button>
            </div>
            <div class="member-chips">
              <span v-for="member in sharedMembers" :key="member.id" class="member-chip">
                {{ member.userEmail }} · {{ formatVaultRole(member.role) }}
                <button type="button" class="chip-remove" @click="onRemoveMember(member.id)">×</button>
              </span>
            </div>
          </section>

          <section class="shared-items-board">
            <div class="shared-item-list">
              <button
                v-for="item in items"
                :key="item.id"
                type="button"
                class="shared-item-card"
                :class="{ active: item.id === activeItemId }"
                @click="selectItem(item.id)"
              >
                <div class="card-row">
                  <strong>{{ item.title }}</strong>
                  <span>{{ formatPassItemType(item.itemType) }}</span>
                </div>
                <div class="card-row muted">
                  <span>{{ item.username || item.website || 'No identity field' }}</span>
                  <span>{{ item.secureLinkCount }} links</span>
                </div>
              </button>
              <el-empty v-if="items.length === 0" description="No shared items yet. Create the first one to onboard your team." />
            </div>

            <section class="editor-card">
              <template v-if="hasActiveItem">
                <div class="editor-grid">
                  <el-input v-model="editor.title" placeholder="Title" maxlength="128" />
                  <el-select v-model="editor.itemType">
                    <el-option v-for="option in PASS_ITEM_TYPE_OPTIONS" :key="option.value" :label="option.label" :value="option.value" />
                  </el-select>
                  <el-input v-model="editor.website" placeholder="Website / service" maxlength="255" />
                  <el-input v-model="editor.username" placeholder="Username / email / alias" maxlength="254" />
                </div>
                <div class="secret-row">
                  <el-input v-model="editor.secretCiphertext" :type="passwordVisible ? 'text' : 'password'" placeholder="Secret / credential" />
                  <el-button @click="passwordVisible = !passwordVisible">{{ passwordVisible ? 'Hide' : 'Show' }}</el-button>
                  <el-button @click="onCopy(editor.secretCiphertext, 'Secret')">Copy</el-button>
                  <el-button type="primary" plain @click="openGenerator">Generate</el-button>
                </div>
                <el-input v-model="editor.note" type="textarea" :rows="10" maxlength="2000" show-word-limit placeholder="Notes / recovery guidance / owner context" />
                <div class="editor-foot">
                  <div>
                    <span>Updated: {{ formatPassTime(editor.updatedAt) }}</span>
                    <span v-if="editor.ownerEmail"> · Owner: {{ editor.ownerEmail }}</span>
                  </div>
                  <div class="editor-actions">
                    <el-button plain :disabled="!policy?.allowSecureLinks" @click="openSecureLinkDialog">Secure Link</el-button>
                    <el-button type="primary" :loading="saving" @click="onSave">Save</el-button>
                    <el-button type="danger" :loading="deleting" @click="onDelete">Delete</el-button>
                  </div>
                </div>
              </template>
              <el-empty v-else description="Select or create a shared item to start editing." />
            </section>
          </section>

          <PassSecureLinksDashboard
            :links="organizationSecureLinks"
            :loading="loadingOrganizationSecureLinks"
            :mutation-id="secureLinkMutationId"
            :shared-mode="isSharedMode"
            @copy="onCopySecureLink"
            @open="onOpenSecureLink"
            @revoke="onRevokeLink"
            @focus-item="onFocusSecureLinkItem"
          />
        </template>

        <template v-else>
          <PassMailboxPanel
            :mailboxes="mailboxes"
            :loading="loadingMailboxes"
            :mutation-id="mailboxMutationId"
            @create="onCreateMailbox"
            @verify="onVerifyMailbox"
            @set-default="onSetDefaultMailbox"
            @remove="onRemoveMailbox"
          />

          <PassAliasCenter
            :aliases="aliases"
            :forward-target-options="forwardTargetOptions"
            :loading="loadingAliases"
            :mutation-id="aliasMutationId"
            @create="onCreateAlias"
            @update="onUpdateAlias"
            @enable="onEnableAlias"
            @disable="onDisableAlias"
            @remove="onRemoveAlias"
            @select="onSelectAlias"
          />

          <PassAliasContactsPanel
            :alias="selectedAlias"
            :contacts="aliasContacts"
            :loading="loadingAliasContacts"
            :mutation-id="aliasContactMutationId"
            @create="onCreateAliasContact"
            @update="onUpdateAliasContact"
            @remove="onRemoveAliasContact"
            @compose="onComposeAliasContact"
            @copy="onCopyAliasContact"
          />

          <section class="editor-card personal-editor">
            <template v-if="hasActiveItem">
              <div class="editor-grid">
                <el-input v-model="editor.title" placeholder="Title" maxlength="128" />
                <el-select v-model="editor.itemType">
                  <el-option v-for="option in PASS_ITEM_TYPE_OPTIONS" :key="option.value" :label="option.label" :value="option.value" />
                </el-select>
                <el-input v-model="editor.website" placeholder="Website (optional)" maxlength="255" />
                <el-input v-model="editor.username" placeholder="Username / Email" maxlength="254" />
              </div>
              <div class="secret-row">
                <el-input v-model="editor.secretCiphertext" :type="passwordVisible ? 'text' : 'password'" placeholder="Password or secret" maxlength="512" />
                <el-button @click="passwordVisible = !passwordVisible">{{ passwordVisible ? 'Hide' : 'Show' }}</el-button>
                <el-button @click="onCopy(editor.secretCiphertext, 'Secret')">Copy</el-button>
                <el-button type="primary" plain @click="openGenerator">Generate</el-button>
              </div>
              <el-input v-model="editor.note" type="textarea" :rows="12" maxlength="2000" show-word-limit placeholder="Notes" />
              <div class="editor-foot">
                <span>Updated: {{ formatPassTime(editor.updatedAt) }}</span>
                <div class="editor-actions">
                  <el-button :loading="togglingFavorite" @click="onToggleFavorite">{{ editor.favorite ? 'Unfavorite' : 'Favorite' }}</el-button>
                  <el-button type="primary" :loading="saving" @click="onSave">Save</el-button>
                  <el-button type="danger" :loading="deleting" @click="onDelete">Delete</el-button>
                </div>
              </div>
            </template>
            <el-empty v-else description="Create or select a credential to start editing." />
          </section>
        </template>
      </section>

      <aside class="mm-card pass-rail">
        <PassBusinessPolicyPanel
          :form="policyForm"
          :policy-updated-at="policy?.updatedAt || null"
          :disabled="!isSharedMode || !canManagePolicy"
          :loading="savingPolicy"
          @save="onSavePolicy"
        />

        <PassItemSharePanel
          :active-item-id="activeItemId"
          :item-title="activeItemTitle"
          :shares="itemShares"
          :draft-email="itemShareEmail"
          :loading="loadingItemShares"
          :mutation-id="itemShareMutationId"
          :policy-blocked="itemSharingBlocked"
          @update:draft-email="itemShareEmail = $event"
          @create="onCreateItemShare"
          @remove="onRemoveItemShare"
        />

        <PassSecureLinkPanel
          :active-item-id="activeItemId"
          :item-title="activeItemTitle"
          :links="secureLinks"
          :loading="loadingSecureLinks"
          :mutation-id="secureLinkMutationId"
          :shared-mode="isSharedMode"
          :policy-blocked="secureLinkPolicyBlocked"
          :external-blocked="secureLinkExternalBlocked"
          @create="openSecureLinkDialog"
          @copy="onCopySecureLink"
          @open="onOpenSecureLink"
          @revoke="onRevokeLink"
        />

        <PassIncomingItemSharesPanel
          :items="incomingSharedItems"
          :selected-item-id="selectedIncomingItemId"
          :detail="incomingSharedItemDetail"
          :loading="loadingIncomingSharedItems"
          :detail-loading="loadingIncomingSharedItemDetail"
          @refresh="onRefreshIncomingShares"
          @select="onSelectIncomingItem"
        />

        <section class="rail-card activity-card">
          <header class="rail-head">
            <strong>Vault Activity</strong>
            <span>{{ activity.length }}</span>
          </header>
          <div class="activity-list">
            <article v-for="event in activity" :key="event.id" class="activity-row">
              <strong>{{ event.eventType }}</strong>
              <p>{{ event.detail }}</p>
              <span>{{ formatPassTime(event.createdAt) }}</span>
            </article>
            <el-empty v-if="!loadingActivity && activity.length === 0" description="No pass activity yet" />
          </div>
        </section>
      </aside>
    </div>
  </section>

  <el-dialog v-model="createVaultDialogVisible" title="Create Shared Vault" width="440px">
    <div class="dialog-grid">
      <el-input v-model="vaultForm.name" placeholder="Vault name" />
      <el-input v-model="vaultForm.description" type="textarea" :rows="4" placeholder="Describe what this vault is for" />
    </div>
    <template #footer>
      <el-button @click="createVaultDialogVisible = false">Cancel</el-button>
      <el-button type="primary" :loading="creatingVault" @click="onCreateVault">Create Vault</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="generatorDialogVisible" :title="t('pass.generator.title')" width="420px">
    <div class="dialog-grid">
      <el-input-number
        v-model="generatorForm.length"
        :min="generatorBounds.min"
        :max="generatorBounds.max"
        :label="t('pass.generator.fields.length')"
      />
      <span class="dialog-hint">{{ t('pass.generator.hints.lengthRange', { min: generatorBounds.min, max: generatorBounds.max }) }}</span>
      <el-checkbox v-model="generatorForm.memorable">
        {{ t('pass.generator.fields.memorable') }}
      </el-checkbox>
      <span v-if="generatorForm.memorable || (isSharedMode && policy?.allowMemorablePasswords === false)" class="dialog-hint">
        {{ t('pass.generator.hints.memorable') }}
      </span>
      <el-checkbox v-model="generatorForm.includeUppercase" :disabled="generatorForm.memorable || Boolean(policy?.requireUppercase)">
        {{ t('pass.generator.fields.uppercase') }}
      </el-checkbox>
      <el-checkbox v-model="generatorForm.includeDigits" :disabled="generatorForm.memorable || Boolean(policy?.requireDigits)">
        {{ t('pass.generator.fields.digits') }}
      </el-checkbox>
      <el-checkbox v-model="generatorForm.includeSymbols" :disabled="generatorForm.memorable || Boolean(policy?.requireSymbols)">
        {{ t('pass.generator.fields.symbols') }}
      </el-checkbox>
    </div>
    <template #footer>
      <el-button @click="generatorDialogVisible = false">Cancel</el-button>
      <el-button type="primary" :loading="generating" @click="onGenerateConfirm">{{ t('pass.generator.actions.generate') }}</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="secureLinkDialogVisible" :title="t('pass.secureLinks.dialog.title')" width="420px">
    <div class="dialog-grid">
      <span class="dialog-hint">{{ t('pass.secureLinks.dialog.description') }}</span>
      <el-input-number
        v-model="secureLinkForm.maxViews"
        :min="1"
        :max="1000"
        :label="t('pass.secureLinks.dialog.maxViews')"
      />
      <el-date-picker
        v-model="secureLinkForm.expiresAt"
        type="datetime"
        format="YYYY-MM-DD HH:mm:ss"
        value-format="YYYY-MM-DDTHH:mm:ss"
        :placeholder="t('pass.secureLinks.dialog.expiresAtPlaceholder')"
        :disabled-date="isSecureLinkExpiryDisabled"
      />
      <span class="dialog-hint">{{ t('pass.secureLinks.dialog.expiresAtHint') }}</span>
    </div>
    <template #footer>
      <el-button @click="secureLinkDialogVisible = false">{{ t('common.actions.cancel') }}</el-button>
      <el-button type="primary" :loading="creatingLink" @click="onCreateSecureLink">{{ t('pass.secureLinks.actions.create') }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.pass-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.pass-command-bar,
.pass-center,
.pass-rail {
  border-radius: 26px;
}

.pass-command-bar {
  padding: 16px;
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px auto auto auto auto;
  gap: 10px;
  align-items: center;
}

.filter-select {
  width: 100%;
}

.pass-grid {
  display: grid;
  grid-template-columns: 320px minmax(0, 1.4fr) minmax(320px, 0.9fr);
  gap: 14px;
  align-items: start;
}

.pass-center,
.pass-rail {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-head,
.rail-head,
.card-row,
.editor-foot,
.vault-context,
.vault-stats,
.link-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.section-eyebrow {
  margin: 0 0 6px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 11px;
  color: #7d89a1;
}

.section-head h2 {
  margin: 0;
}

.section-note,
.card-row.muted,
.editor-foot,
.rail-head span,
.vault-context p,
.activity-row p,
.activity-row span {
  color: #667085;
  font-size: 12px;
}

.vault-context,
.member-strip,
.editor-card,
.rail-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 20px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.86);
}

.vault-context strong,
.rail-card strong {
  color: #0f172a;
}

.vault-context p {
  margin: 8px 0 0;
}

.member-strip,
.dialog-grid,
.editor-grid,
.activity-list,
.secure-link-list {
  display: grid;
  gap: 12px;
}

.member-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 170px auto;
  gap: 10px;
}

.role-select {
  width: 100%;
}

.member-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.member-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(17, 24, 39, 0.06);
  color: #344054;
  font-size: 12px;
}

.chip-remove {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #98a2b3;
}

.shared-items-board {
  display: grid;
  grid-template-columns: minmax(260px, 0.72fr) minmax(0, 1fr);
  gap: 14px;
}

.shared-item-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.shared-item-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.95);
  padding: 14px;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
}

.shared-item-card.active {
  border-color: rgba(79, 70, 229, 0.32);
  box-shadow: 0 18px 40px rgba(79, 70, 229, 0.14);
}

.editor-card,
.personal-editor {
  min-height: 520px;
}

.editor-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.secret-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto;
  gap: 8px;
}

.editor-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.activity-row,
.secure-link-card {
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.95);
  padding: 12px;
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.activity-row strong,
.secure-link-card strong {
  color: #101828;
}

.activity-row p {
  margin: 6px 0;
}

.dialog-hint {
  color: #667085;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 1320px) {
  .pass-grid {
    grid-template-columns: 280px minmax(0, 1fr);
  }

  .pass-rail {
    grid-column: 1 / -1;
  }
}

@media (max-width: 1080px) {
  .pass-command-bar {
    grid-template-columns: 1fr 1fr;
  }

  .pass-grid,
  .shared-items-board,
  .member-form,
  .editor-grid,
  .secret-row {
    grid-template-columns: 1fr;
  }
}
</style>
