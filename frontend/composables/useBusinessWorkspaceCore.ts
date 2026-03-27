import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { OrgMember, OrgWorkspace } from '~/types/api'
import type {
  OrgBusinessOverview,
  OrgTeamSpace,
  OrgTeamSpaceAccessRole,
  OrgTeamSpaceActivity,
  OrgTeamSpaceActivityCategory,
  OrgTeamSpaceFileVersion,
  OrgTeamSpaceItem,
  OrgTeamSpaceItemType,
  OrgTeamSpaceMember,
  OrgTeamSpaceTrashItem
} from '~/types/business'
import { useOrganizationApi } from '~/composables/useOrganizationApi'
import { useOrgBusinessApi } from '~/composables/useOrgBusinessApi'
import { useI18n } from '~/composables/useI18n'
import { formatBusinessBytes, formatBusinessTime, isBusinessManager } from '~/utils/business'

interface TrailItem {
  id: string | null
  name: string
}

const DEFAULT_STORAGE_MB = 10240
const ITEM_LIST_LIMIT = 200
const MEMBER_ACTIVITY_LIMIT = 24
const VERSION_LIMIT = 50
const TRASH_LIMIT = 100

export function createBusinessWorkspaceCore() {
  const { t } = useI18n()
  const { listOrganizations, listMembers } = useOrganizationApi()
  const {
    getBusinessOverview,
    listTeamSpaces,
    createTeamSpace,
    listTeamSpaceItems,
    createTeamSpaceFolder,
    uploadTeamSpaceFile,
    downloadTeamSpaceFile,
    listTeamSpaceMembers,
    addTeamSpaceMember,
    updateTeamSpaceMemberRole,
    removeTeamSpaceMember,
    listTeamSpaceFileVersions,
    uploadTeamSpaceFileVersion,
    restoreTeamSpaceFileVersion,
    deleteTeamSpaceItem,
    listTeamSpaceTrashItems,
    restoreTeamSpaceTrashItem,
    purgeTeamSpaceTrashItem,
    listTeamSpaceActivity
  } = useOrgBusinessApi()

  const loading = reactive({
    organizations: false,
    overview: false,
    items: false,
    createTeamSpace: false,
    createFolder: false,
    upload: false,
    downloadItemId: '',
    members: false,
    memberMutationId: '',
    addMember: false,
    activity: false,
    versions: false,
    versionMutationId: '',
    versionUpload: false,
    trash: false,
    trashMutationId: ''
  })

  const organizations = ref<OrgWorkspace[]>([])
  const organizationMembers = ref<OrgMember[]>([])
  const selectedOrgId = ref('')
  const overview = ref<OrgBusinessOverview | null>(null)
  const teamSpaces = ref<OrgTeamSpace[]>([])
  const selectedTeamSpaceId = ref('')
  const items = ref<OrgTeamSpaceItem[]>([])
  const currentParentId = ref<string | null>(null)
  const trail = ref<TrailItem[]>([{ id: null, name: t('drive.root') }])
  const keyword = ref('')
  const itemTypeFilter = ref<OrgTeamSpaceItemType | ''>('')

  const teamSpaceMembers = ref<OrgTeamSpaceMember[]>([])
  const activityItems = ref<OrgTeamSpaceActivity[]>([])
  const activityCategory = ref<OrgTeamSpaceActivityCategory | ''>('')
  const versionTargetItem = ref<OrgTeamSpaceItem | null>(null)
  const versionItems = ref<OrgTeamSpaceFileVersion[]>([])
  const trashItems = ref<OrgTeamSpaceTrashItem[]>([])

  const createTeamSpaceDialogVisible = ref(false)
  const createFolderDialogVisible = ref(false)
  const versionDrawerVisible = ref(false)
  const trashDrawerVisible = ref(false)

  const createTeamSpaceForm = reactive({
    name: '',
    description: '',
    storageLimitMb: DEFAULT_STORAGE_MB
  })

  const createFolderForm = reactive({
    name: ''
  })

  const addMemberForm = reactive<{ userEmail: string; role: OrgTeamSpaceAccessRole }>({
    userEmail: '',
    role: 'VIEWER'
  })

  const activeTeamSpace = computed(() => teamSpaces.value.find(item => item.id === selectedTeamSpaceId.value) || null)
  const canManageTeamSpaces = computed(() => isBusinessManager(overview.value?.currentRole))
  const currentSpaceRole = computed(() => activeTeamSpace.value?.currentAccessRole || null)
  const canWriteCurrentSpace = computed(() => activeTeamSpace.value?.canWrite ?? false)
  const canManageCurrentSpace = computed(() => activeTeamSpace.value?.canManage ?? false)
  const currentFolderLabel = computed(() => trail.value[trail.value.length - 1]?.name || t('drive.root'))
  const readOnlyReason = computed(() => {
    if (!activeTeamSpace.value || canWriteCurrentSpace.value) {
      return ''
    }
    return t('business.messages.readOnlyReason')
  })
  const candidateOrgMembers = computed(() => {
    const currentEmails = new Set(teamSpaceMembers.value.map(item => item.userEmail.toLowerCase()))
    return organizationMembers.value.filter(item => item.status === 'ACTIVE' && !currentEmails.has(item.userEmail.toLowerCase()))
  })
  const summaryCards = computed(() => buildSummaryCards(overview.value, t))
  const policyChips = computed(() => buildPolicyChips(overview.value, t))

  async function bootstrapPage(): Promise<void> {
    loading.organizations = true
    try {
      const nextOrganizations = await listOrganizations()
      organizations.value = nextOrganizations
      if (!nextOrganizations.length) {
        clearBusinessState()
        return
      }
      if (!nextOrganizations.some(item => item.id === selectedOrgId.value)) {
        selectedOrgId.value = nextOrganizations[0].id
      }
      await loadBusinessWorkspace(selectedOrgId.value, null)
    } catch (error) {
      showError(error, t('business.errors.loadWorkspace'))
    } finally {
      loading.organizations = false
    }
  }

  function clearBusinessState(): void {
    selectedOrgId.value = ''
    overview.value = null
    teamSpaces.value = []
    selectedTeamSpaceId.value = ''
    organizationMembers.value = []
    items.value = []
    teamSpaceMembers.value = []
    activityItems.value = []
    versionTargetItem.value = null
    versionItems.value = []
    trashItems.value = []
    resetTrail()
  }

  async function loadBusinessWorkspace(orgId: string, preferredTeamSpaceId: string | null): Promise<void> {
    if (!orgId) {
      clearBusinessState()
      return
    }
    loading.overview = true
    try {
      const [nextOverview, nextTeamSpaces, nextOrgMembers] = await Promise.all([
        getBusinessOverview(orgId),
        listTeamSpaces(orgId),
        listMembers(orgId)
      ])
      overview.value = nextOverview
      teamSpaces.value = nextTeamSpaces
      organizationMembers.value = nextOrgMembers
      await applyTeamSpaceSelection(preferredTeamSpaceId)
    } catch (error) {
      showError(error, t('business.errors.loadOverview'))
    } finally {
      loading.overview = false
    }
  }

  async function applyTeamSpaceSelection(preferredTeamSpaceId: string | null): Promise<void> {
    selectedTeamSpaceId.value = pickTeamSpaceId(preferredTeamSpaceId)
    resetTrail()
    versionTargetItem.value = null
    versionItems.value = []
    trashItems.value = []
    if (!selectedTeamSpaceId.value) {
      items.value = []
      teamSpaceMembers.value = []
      activityItems.value = []
      return
    }
    await Promise.all([loadCurrentItems(), loadSidePanels()])
  }

  function pickTeamSpaceId(preferredTeamSpaceId: string | null): string {
    if (preferredTeamSpaceId && teamSpaces.value.some(item => item.id === preferredTeamSpaceId)) {
      return preferredTeamSpaceId
    }
    if (selectedTeamSpaceId.value && teamSpaces.value.some(item => item.id === selectedTeamSpaceId.value)) {
      return selectedTeamSpaceId.value
    }
    return teamSpaces.value[0]?.id || ''
  }

  function resetTrail(): void {
    currentParentId.value = null
    trail.value = [{ id: null, name: t('drive.root') }]
  }

  async function loadCurrentItems(): Promise<void> {
    if (!selectedOrgId.value || !selectedTeamSpaceId.value) {
      items.value = []
      return
    }
    loading.items = true
    try {
      items.value = await listTeamSpaceItems(selectedOrgId.value, selectedTeamSpaceId.value, {
        parentId: currentParentId.value,
        keyword: keyword.value.trim(),
        itemType: itemTypeFilter.value,
        limit: ITEM_LIST_LIMIT
      })
    } catch (error) {
      showError(error, t('business.errors.loadItems'))
    } finally {
      loading.items = false
    }
  }

  async function loadSidePanels(): Promise<void> {
    await Promise.all([loadMembers(), loadActivity()])
  }

  async function loadMembers(): Promise<void> {
    if (!selectedOrgId.value || !selectedTeamSpaceId.value || !canManageCurrentSpace.value) {
      teamSpaceMembers.value = []
      return
    }
    loading.members = true
    try {
      teamSpaceMembers.value = await listTeamSpaceMembers(selectedOrgId.value, selectedTeamSpaceId.value)
    } catch (error) {
      showError(error, t('business.errors.loadMembers'))
    } finally {
      loading.members = false
    }
  }

  async function loadActivity(): Promise<void> {
    if (!selectedOrgId.value || !selectedTeamSpaceId.value) {
      activityItems.value = []
      return
    }
    loading.activity = true
    try {
      activityItems.value = await listTeamSpaceActivity(selectedOrgId.value, selectedTeamSpaceId.value, {
        category: activityCategory.value,
        limit: MEMBER_ACTIVITY_LIMIT
      })
    } catch (error) {
      showError(error, t('business.errors.loadActivity'))
    } finally {
      loading.activity = false
    }
  }

  async function loadVersions(itemId: string): Promise<void> {
    if (!selectedOrgId.value || !selectedTeamSpaceId.value) {
      versionItems.value = []
      return
    }
    loading.versions = true
    try {
      versionItems.value = await listTeamSpaceFileVersions(selectedOrgId.value, selectedTeamSpaceId.value, itemId, VERSION_LIMIT)
    } catch (error) {
      showError(error, t('business.errors.loadVersions'))
    } finally {
      loading.versions = false
    }
  }

  async function loadTrashItems(): Promise<void> {
    if (!selectedOrgId.value || !selectedTeamSpaceId.value || !canManageCurrentSpace.value) {
      trashItems.value = []
      return
    }
    loading.trash = true
    try {
      trashItems.value = await listTeamSpaceTrashItems(selectedOrgId.value, selectedTeamSpaceId.value, TRASH_LIMIT)
    } catch (error) {
      showError(error, t('business.errors.loadTrash'))
    } finally {
      loading.trash = false
    }
  }

  async function onOrganizationChange(orgId: string): Promise<void> {
    if (!orgId) {
      clearBusinessState()
      return
    }
    selectedOrgId.value = orgId
    await loadBusinessWorkspace(orgId, null)
  }

  async function onTeamSpaceChange(teamSpaceId: string): Promise<void> {
    if (!teamSpaceId) {
      selectedTeamSpaceId.value = ''
      items.value = []
      return
    }
    selectedTeamSpaceId.value = teamSpaceId
    resetTrail()
    await Promise.all([loadCurrentItems(), loadSidePanels()])
  }

  async function onRefreshWorkspace(): Promise<void> {
    if (!selectedOrgId.value) {
      return
    }
    await loadBusinessWorkspace(selectedOrgId.value, selectedTeamSpaceId.value || null)
  }

  async function onSearchItems(): Promise<void> {
    await loadCurrentItems()
  }

  function onKeywordChange(value: string): void {
    keyword.value = value
  }

  function onItemTypeFilterChange(value: OrgTeamSpaceItemType | ''): void {
    itemTypeFilter.value = value
  }

  async function onOpenFolder(item: OrgTeamSpaceItem): Promise<void> {
    if (item.itemType !== 'FOLDER') {
      return
    }
    currentParentId.value = item.id
    trail.value = [...trail.value, { id: item.id, name: item.name }]
    await loadCurrentItems()
  }

  async function onNavigateTrail(index: number): Promise<void> {
    const target = trail.value[index]
    if (!target) {
      return
    }
    trail.value = trail.value.slice(0, index + 1)
    currentParentId.value = target.id
    await loadCurrentItems()
  }

  return {
    loading,
    organizations,
    organizationMembers,
    selectedOrgId,
    overview,
    teamSpaces,
    selectedTeamSpaceId,
    items,
    currentParentId,
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
    createTeamSpace,
    createTeamSpaceFolder,
    uploadTeamSpaceFile,
    downloadTeamSpaceFile,
    addTeamSpaceMember,
    updateTeamSpaceMemberRole,
    removeTeamSpaceMember,
    uploadTeamSpaceFileVersion,
    restoreTeamSpaceFileVersion,
    deleteTeamSpaceItem,
    restoreTeamSpaceTrashItem,
    purgeTeamSpaceTrashItem,
    bootstrapPage,
    clearBusinessState,
    loadBusinessWorkspace,
    loadCurrentItems,
    loadMembers,
    loadActivity,
    loadVersions,
    loadTrashItems,
    onOrganizationChange,
    onTeamSpaceChange,
    onRefreshWorkspace,
    onSearchItems,
    onKeywordChange,
    onItemTypeFilterChange,
    onOpenFolder,
    onNavigateTrail
  }
}

function showError(error: unknown, fallback: string): void {
  ElMessage.error((error as Error).message || fallback)
}

function buildSummaryCards(
  source: OrgBusinessOverview | null,
  t: (key: string, params?: Record<string, string | number>) => string
): Array<{ label: string; value: string; hint: string }> {
  if (!source) {
    return []
  }
  return [
    {
      label: t('business.summary.members'),
      value: String(source.memberCount),
      hint: t('business.summary.membersHint', { count: source.adminCount })
    },
    {
      label: t('business.summary.pendingInvites'),
      value: String(source.pendingInviteCount),
      hint: t('business.summary.pendingInvitesHint')
    },
    {
      label: t('business.summary.teamSpaces'),
      value: String(source.teamSpaceCount),
      hint: t('business.summary.teamSpacesHint')
    },
    {
      label: t('business.summary.storageAllocated'),
      value: `${formatBusinessBytes(source.storageBytes)} / ${formatBusinessBytes(source.storageLimitBytes)}`,
      hint: t('business.summary.storageHint', { time: formatBusinessTime(source.generatedAt) })
    }
  ]
}

function buildPolicyChips(
  source: OrgBusinessOverview | null,
  t: (key: string, params?: Record<string, string | number>) => string
): string[] {
  if (!source) {
    return []
  }
  const domainLabel = source.allowedEmailDomains.length > 0
    ? t('business.policy.domains', { domains: source.allowedEmailDomains.join(', ') })
    : t('business.policy.domainsUnrestricted')
  const dualReviewLabel = source.requireDualReviewGovernance
    ? t('business.policy.dualReviewRequired')
    : t('business.policy.singleReviewAllowed')
  return [domainLabel, t('business.policy.governanceSla', { hours: source.governanceReviewSlaHours }), dualReviewLabel]
}
