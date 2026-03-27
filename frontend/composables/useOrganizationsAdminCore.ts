import { computed, reactive, ref } from 'vue'
import { useI18n } from '~/composables/useI18n'
import { useOrganizationApi } from '~/composables/useOrganizationApi'
import { useAuthStore } from '~/stores/auth'
import type {
  OrgAuditEvent,
  OrgIncomingInvite,
  OrgMember,
  OrgRole,
  OrgWorkspace
} from '~/types/api'
import type { OrganizationAuthenticationSecurity } from '~/types/organization-auth-security'
import type {
  OrgAdminConsoleSummary,
  OrgAuditSortDirection,
  OrgCustomDomain,
  OrgMailIdentity,
  OrgMemberSession,
  OrgMonitorStatus,
  OrgMemberProductAccess,
  OrgProductKey
} from '~/types/organization-admin'
import type { OrganizationPolicy, TwoFactorEnforcementLevel } from '~/types/organization-policy'
import { buildOrganizationSessionSummaryCards } from '~/utils/organization-monitor'
import {
  authenticationReminderCandidates,
  buildAuthenticationSecurityCards
} from '~/utils/organization-auth-security'
import {
  buildOrganizationInviteRoleOptions,
  buildOrganizationPolicyChips,
  buildOrganizationSummaryCards,
  filterOrganizationAuditEvents,
  isOrganizationManager
} from '~/utils/organization-admin'

export function useOrganizationsAdminCore() {
  const authStore = useAuthStore()
  const { t } = useI18n()
  const api = useOrganizationApi()

  const loading = reactive({
    organizations: false,
    members: false,
    incoming: false,
    policy: false,
    audit: false,
    summary: false,
    domains: false,
    identities: false,
    memberSessions: false,
    monitorStatus: false,
    productAccess: false,
    authSecurity: false,
    authSecurityReminder: false,
    createOrg: false,
    invite: false,
    savePolicy: false,
    memberMutationId: '',
    domainMutationId: '',
    identityMutationId: '',
    sessionMutationId: '',
    productMutationId: ''
  })

  const organizations = ref<OrgWorkspace[]>([])
  const selectedOrgId = ref('')
  const members = ref<OrgMember[]>([])
  const incomingInvites = ref<OrgIncomingInvite[]>([])
  const auditEvents = ref<OrgAuditEvent[]>([])
  const policy = ref<OrganizationPolicy | null>(null)
  const summary = ref<OrgAdminConsoleSummary | null>(null)
  const domains = ref<OrgCustomDomain[]>([])
  const mailIdentities = ref<OrgMailIdentity[]>([])
  const memberSessions = ref<OrgMemberSession[]>([])
  const monitorStatus = ref<OrgMonitorStatus | null>(null)
  const productAccessRows = ref<OrgMemberProductAccess[]>([])
  const authSecurity = ref<OrganizationAuthenticationSecurity | null>(null)
  const authSecurityReminderMemberIds = ref<string[]>([])

  const createOrgDialogVisible = ref(false)
  const createOrgForm = reactive({ name: '' })
  const inviteForm = reactive({ email: '', role: 'MEMBER' as Exclude<OrgRole, 'OWNER'> })
  const createDomainForm = reactive({ domain: '' })
  const createIdentityForm = reactive({ memberId: '', customDomainId: '', localPart: '', displayName: '' })
  const policyForm = reactive({
    allowedEmailDomainsText: '',
    memberLimit: 200,
    governanceReviewSlaHours: 24,
    adminCanInviteAdmin: false,
    adminCanRemoveAdmin: false,
    adminCanReviewGovernance: false,
    adminCanExecuteGovernance: false,
    requireDualReviewGovernance: false,
    twoFactorEnforcementLevel: 'OFF' as TwoFactorEnforcementLevel,
    twoFactorGracePeriodDays: 0
  })
  const auditFilter = reactive({
    eventType: '',
    actorEmail: '',
    keyword: '',
    limit: 100,
    fromDate: '',
    toDate: '',
    sortDirection: 'DESC' as OrgAuditSortDirection
  })
  const memberSessionFilter = reactive({
    memberEmail: '',
    limit: 60
  })
  const respondingInviteId = ref('')

  const selectedOrg = computed(() => organizations.value.find(item => item.id === selectedOrgId.value) || null)
  const selectedOrgRole = computed<OrgRole | null>(() => selectedOrg.value?.role || null)
  const currentUserId = computed(() => authStore.user?.id || '')
  const canManageProducts = computed(() => isOrganizationManager(selectedOrgRole.value))
  const canViewMemberSessions = computed(() => isOrganizationManager(selectedOrgRole.value))
  const canManageAuthenticationSecurity = computed(() => isOrganizationManager(selectedOrgRole.value))
  const canManageMemberRoles = computed(() => selectedOrgRole.value === 'OWNER')
  const canEditPolicy = computed(() => selectedOrgRole.value === 'OWNER')
  const canInvite = computed(() => isOrganizationManager(selectedOrgRole.value))
  const inviteRoleOptions = computed(() => buildOrganizationInviteRoleOptions(selectedOrgRole.value, policy.value))
  const summaryCards = computed(() => buildOrganizationSummaryCards(summary.value, t))
  const authSecurityCards = computed(() => buildAuthenticationSecurityCards(authSecurity.value, t))
  const memberSessionCards = computed(() => buildOrganizationSessionSummaryCards(memberSessions.value, t))
  const policyChips = computed(() => buildOrganizationPolicyChips(policy.value, t))
  const governanceAuditEvents = computed(() => filterOrganizationAuditEvents(auditEvents.value))

  function applyPolicyToForm(nextPolicy: OrganizationPolicy | null): void {
    if (!nextPolicy) {
      policyForm.allowedEmailDomainsText = ''
      policyForm.memberLimit = 200
      policyForm.governanceReviewSlaHours = 24
      policyForm.adminCanInviteAdmin = false
      policyForm.adminCanRemoveAdmin = false
      policyForm.adminCanReviewGovernance = false
      policyForm.adminCanExecuteGovernance = false
      policyForm.requireDualReviewGovernance = false
      policyForm.twoFactorEnforcementLevel = 'OFF'
      policyForm.twoFactorGracePeriodDays = 0
      return
    }
    policyForm.allowedEmailDomainsText = nextPolicy.allowedEmailDomains.join(', ')
    policyForm.memberLimit = nextPolicy.memberLimit
    policyForm.governanceReviewSlaHours = nextPolicy.governanceReviewSlaHours
    policyForm.adminCanInviteAdmin = nextPolicy.adminCanInviteAdmin
    policyForm.adminCanRemoveAdmin = nextPolicy.adminCanRemoveAdmin
    policyForm.adminCanReviewGovernance = nextPolicy.adminCanReviewGovernance
    policyForm.adminCanExecuteGovernance = nextPolicy.adminCanExecuteGovernance
    policyForm.requireDualReviewGovernance = nextPolicy.requireDualReviewGovernance
    policyForm.twoFactorEnforcementLevel = nextPolicy.twoFactorEnforcementLevel
    policyForm.twoFactorGracePeriodDays = nextPolicy.twoFactorGracePeriodDays
  }

  function syncIdentityFormSelections(): void {
    const firstMemberId = members.value[0]?.id || ''
    const verifiedDomainIds = domains.value.filter((item) => item.status === 'VERIFIED').map((item) => item.id)
    if (!createIdentityForm.memberId || !members.value.some((item) => item.id === createIdentityForm.memberId)) {
      createIdentityForm.memberId = firstMemberId
    }
    if (!createIdentityForm.customDomainId || !verifiedDomainIds.includes(createIdentityForm.customDomainId)) {
      createIdentityForm.customDomainId = verifiedDomainIds[0] || ''
    }
  }

  function syncMemberSessionsWithMembers(): void {
    if (members.value.length === 0) {
      return
    }
    const memberById = new Map(members.value.map(item => [item.id, item]))
    memberSessions.value = memberSessions.value
      .filter(item => memberById.has(item.memberId))
      .map((item) => {
        const member = memberById.get(item.memberId)!
        return {
          ...item,
          memberEmail: member.userEmail,
          role: member.role
        }
      })
  }

  function syncAuthenticationReminderSelection(): void {
    const allowed = new Set(authenticationReminderCandidates(authSecurity.value?.members || []))
    authSecurityReminderMemberIds.value = authSecurityReminderMemberIds.value.filter(memberId => allowed.has(memberId))
  }

  async function refreshOrganizations(): Promise<void> {
    loading.organizations = true
    try {
      organizations.value = await api.listOrganizations()
      if (!selectedOrgId.value && organizations.value.length > 0) {
        selectedOrgId.value = organizations.value[0].id
      }
      if (selectedOrgId.value && !organizations.value.some(item => item.id === selectedOrgId.value)) {
        selectedOrgId.value = organizations.value[0]?.id || ''
      }
    } finally {
      loading.organizations = false
    }
  }

  async function refreshMembers(): Promise<void> {
    if (!selectedOrgId.value) {
      members.value = []
      syncIdentityFormSelections()
      return
    }
    loading.members = true
    try {
      members.value = await api.listMembers(selectedOrgId.value)
      syncIdentityFormSelections()
      syncMemberSessionsWithMembers()
    } finally {
      loading.members = false
    }
  }

  async function refreshIncomingInvites(): Promise<void> {
    loading.incoming = true
    try {
      incomingInvites.value = await api.listIncomingInvites()
    } finally {
      loading.incoming = false
    }
  }

  async function refreshPolicy(): Promise<void> {
    if (!selectedOrgId.value) {
      policy.value = null
      applyPolicyToForm(null)
      return
    }
    loading.policy = true
    try {
      policy.value = await api.getOrgPolicy(selectedOrgId.value)
      applyPolicyToForm(policy.value)
    } finally {
      loading.policy = false
    }
  }

  async function refreshSummary(): Promise<void> {
    if (!selectedOrgId.value) {
      summary.value = null
      return
    }
    loading.summary = true
    try {
      summary.value = await api.getOrgAdminConsoleSummary(selectedOrgId.value)
    } finally {
      loading.summary = false
    }
  }

  async function refreshDomains(): Promise<void> {
    if (!selectedOrgId.value) {
      domains.value = []
      syncIdentityFormSelections()
      return
    }
    loading.domains = true
    try {
      domains.value = await api.listOrgCustomDomains(selectedOrgId.value)
      syncIdentityFormSelections()
    } finally {
      loading.domains = false
    }
  }

  async function refreshIdentities(): Promise<void> {
    if (!selectedOrgId.value) {
      mailIdentities.value = []
      return
    }
    loading.identities = true
    try {
      mailIdentities.value = await api.listOrgMailIdentities(selectedOrgId.value)
    } finally {
      loading.identities = false
    }
  }

  async function refreshMemberSessions(): Promise<void> {
    if (!selectedOrgId.value || !canViewMemberSessions.value) {
      memberSessions.value = []
      return
    }
    loading.memberSessions = true
    try {
      memberSessions.value = await api.listOrgMemberSessions(selectedOrgId.value, memberSessionFilter)
      syncMemberSessionsWithMembers()
    } finally {
      loading.memberSessions = false
    }
  }

  async function refreshMonitorStatus(): Promise<void> {
    if (!selectedOrgId.value || !canViewMemberSessions.value) {
      monitorStatus.value = null
      return
    }
    loading.monitorStatus = true
    try {
      monitorStatus.value = await api.getOrgMonitorStatus(selectedOrgId.value)
    } finally {
      loading.monitorStatus = false
    }
  }

  async function refreshProductAccess(): Promise<void> {
    if (!selectedOrgId.value) {
      productAccessRows.value = []
      return
    }
    loading.productAccess = true
    try {
      productAccessRows.value = await api.listOrgProductAccess(selectedOrgId.value)
    } finally {
      loading.productAccess = false
    }
  }

  async function refreshAudit(): Promise<void> {
    if (!selectedOrgId.value || !canViewMemberSessions.value) {
      auditEvents.value = []
      return
    }
    loading.audit = true
    try {
      auditEvents.value = await api.listOrgAuditEvents(selectedOrgId.value, auditFilter)
    } finally {
      loading.audit = false
    }
  }

  async function refreshAuthenticationSecurity(): Promise<void> {
    if (!selectedOrgId.value || !canManageAuthenticationSecurity.value) {
      authSecurity.value = null
      authSecurityReminderMemberIds.value = []
      return
    }
    loading.authSecurity = true
    try {
      authSecurity.value = await api.getOrgAuthenticationSecurity(selectedOrgId.value)
      syncAuthenticationReminderSelection()
    } finally {
      loading.authSecurity = false
    }
  }

  async function refreshCurrentOrganization(): Promise<void> {
    await Promise.all([
      refreshMembers(),
      refreshPolicy(),
      refreshSummary(),
      refreshDomains(),
      refreshIdentities(),
      refreshMemberSessions(),
      refreshMonitorStatus(),
      refreshProductAccess(),
      refreshAuthenticationSecurity(),
      refreshAudit()
    ])
  }

  async function bootstrapPage(): Promise<void> {
    await refreshOrganizations()
    await Promise.all([refreshIncomingInvites(), refreshCurrentOrganization()])
  }

  function updateProductRow(updatedRow: OrgMemberProductAccess): void {
    const index = productAccessRows.value.findIndex(item => item.memberId === updatedRow.memberId)
    if (index >= 0) {
      productAccessRows.value[index] = updatedRow
      return
    }
    productAccessRows.value.unshift(updatedRow)
  }

  function buildProductPayload(memberId: string, productKey: OrgProductKey, enabled: boolean) {
    const row = productAccessRows.value.find(item => item.memberId === memberId)
    if (!row) {
      return { products: [] }
    }
    return {
      products: row.products.map(item => ({
        productKey: item.productKey,
        accessState: item.productKey === productKey ? (enabled ? 'ENABLED' : 'DISABLED') : item.accessState
      }))
    }
  }

  return {
    api,
    loading,
    organizations,
    selectedOrgId,
    members,
    incomingInvites,
    auditEvents,
    policy,
    summary,
    domains,
    mailIdentities,
    memberSessions,
    monitorStatus,
    productAccessRows,
    authSecurity,
    authSecurityReminderMemberIds,
    createOrgDialogVisible,
    createOrgForm,
    inviteForm,
    createDomainForm,
    createIdentityForm,
    policyForm,
    auditFilter,
    memberSessionFilter,
    respondingInviteId,
    selectedOrg,
    selectedOrgRole,
    currentUserId,
    canManageProducts,
    canViewMemberSessions,
    canManageAuthenticationSecurity,
    canManageMemberRoles,
    canEditPolicy,
    canInvite,
    inviteRoleOptions,
    summaryCards,
    authSecurityCards,
    memberSessionCards,
    policyChips,
    governanceAuditEvents,
    applyPolicyToForm,
    syncIdentityFormSelections,
    refreshOrganizations,
    refreshMembers,
    refreshIncomingInvites,
    refreshPolicy,
    refreshSummary,
    refreshDomains,
    refreshIdentities,
    refreshMemberSessions,
    refreshMonitorStatus,
    refreshProductAccess,
    refreshAuthenticationSecurity,
    refreshAudit,
    refreshCurrentOrganization,
    bootstrapPage,
    updateProductRow,
    buildProductPayload
  }
}
