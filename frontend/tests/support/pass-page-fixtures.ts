import { shallowMount, type VueWrapper } from '@vue/test-utils'
import { defineComponent, ref } from 'vue'
import { vi } from 'vitest'

export const navigateToMock = vi.fn(async () => undefined)
export const useHeadMock = vi.fn()
export const definePageMetaMock = vi.fn()
export const messageErrorMock = vi.fn()
export const messageSuccessMock = vi.fn()
export const messageWarningMock = vi.fn()
export const confirmMock = vi.fn(async () => undefined)
export const clipboardWriteTextMock = vi.fn(async () => undefined)
export const windowOpenMock = vi.fn()

export const routeState = {
  params: { token: 'public-token' },
  query: {} as Record<string, unknown>,
}

export const listOrganizationsMock = vi.fn()
export const passApiMock = {
  listItems: vi.fn(),
  createItem: vi.fn(),
  getItem: vi.fn(),
  updateItem: vi.fn(),
  deleteItem: vi.fn(),
  favoriteItem: vi.fn(),
  unFavoriteItem: vi.fn(),
  listMailboxes: vi.fn(),
  createMailbox: vi.fn(),
  verifyMailbox: vi.fn(),
  setDefaultMailbox: vi.fn(),
  deleteMailbox: vi.fn(),
  listAliases: vi.fn(),
  createAlias: vi.fn(),
  updateAlias: vi.fn(),
  enableAlias: vi.fn(),
  disableAlias: vi.fn(),
  deleteAlias: vi.fn(),
  listAliasContacts: vi.fn(),
  createAliasContact: vi.fn(),
  updateAliasContact: vi.fn(),
  deleteAliasContact: vi.fn(),
  generatePassword: vi.fn(),
  getBusinessOverview: vi.fn(),
  getBusinessPolicy: vi.fn(),
  updateBusinessPolicy: vi.fn(),
  listSharedVaults: vi.fn(),
  createSharedVault: vi.fn(),
  listSharedVaultMembers: vi.fn(),
  addSharedVaultMember: vi.fn(),
  removeSharedVaultMember: vi.fn(),
  listSharedItems: vi.fn(),
  createSharedItem: vi.fn(),
  getSharedItem: vi.fn(),
  updateSharedItem: vi.fn(),
  deleteSharedItem: vi.fn(),
  listActivity: vi.fn(),
  listSecureLinks: vi.fn(),
  listOrgSecureLinks: vi.fn(),
  createSecureLink: vi.fn(),
  revokeSecureLink: vi.fn(),
  getPublicSecureLink: vi.fn(),
  getPersonalMonitor: vi.fn(),
  getSharedMonitor: vi.fn(),
  excludePersonalMonitorItem: vi.fn(),
  includePersonalMonitorItem: vi.fn(),
  excludeSharedMonitorItem: vi.fn(),
  includeSharedMonitorItem: vi.fn(),
  upsertPersonalItemTwoFactor: vi.fn(),
  deletePersonalItemTwoFactor: vi.fn(),
  generatePersonalItemTwoFactorCode: vi.fn(),
  upsertSharedItemTwoFactor: vi.fn(),
  deleteSharedItemTwoFactor: vi.fn(),
  generateSharedItemTwoFactorCode: vi.fn(),
}

const itemShares = ref<any[]>([])
const incomingSharedItems = ref<any[]>([])
const selectedIncomingItemId = ref('')
const incomingSharedItemDetail = ref<any | null>(null)
const loadingItemShares = ref(false)
const loadingIncomingSharedItems = ref(false)
const loadingIncomingSharedItemDetail = ref(false)

export const loadItemSharesMock = vi.fn(async () => {
  itemShares.value = [{ id: 'share-1', collaboratorEmail: 'auditor@mmmail.local', readOnly: true }]
})

export const loadIncomingSharesMock = vi.fn(async () => {
  incomingSharedItems.value = [{ itemId: 'shared-item-1', title: 'Incoming Shared Item' }]
  selectedIncomingItemId.value = 'shared-item-1'
  incomingSharedItemDetail.value = buildIncomingSharedItemDetail('shared-item-1')
})

export const loadIncomingShareDetailMock = vi.fn(async (_orgId?: string, itemId?: string) => {
  selectedIncomingItemId.value = itemId || ''
  incomingSharedItemDetail.value = buildIncomingSharedItemDetail(itemId || '')
})

export const addItemShareMock = vi.fn(async (_orgId: string, _itemId: string, email: string) => {
  itemShares.value = [{ id: 'share-2', collaboratorEmail: email, readOnly: true }]
})

export const deleteItemShareMock = vi.fn(async () => {
  itemShares.value = []
})

export const passBusinessSharingFixture = {
  itemShares,
  incomingSharedItems,
  selectedIncomingItemId,
  incomingSharedItemDetail,
  loadingItemShares,
  loadingIncomingSharedItems,
  loadingIncomingSharedItemDetail,
  clearItemShares: () => {
    itemShares.value = []
  },
  clearIncomingShares: () => {
    incomingSharedItems.value = []
    selectedIncomingItemId.value = ''
    incomingSharedItemDetail.value = null
  },
  loadItemShares: loadItemSharesMock,
  loadIncomingShares: loadIncomingSharesMock,
  loadIncomingShareDetail: loadIncomingShareDetailMock,
  addItemShare: addItemShareMock,
  deleteItemShare: deleteItemShareMock,
}

vi.mock('element-plus', () => ({
  ElMessage: {
    error: messageErrorMock,
    success: messageSuccessMock,
    warning: messageWarningMock,
  },
  ElMessageBox: {
    confirm: confirmMock,
  },
}))

vi.mock('~/composables/usePassApi', () => ({
  usePassApi: () => passApiMock,
}))

vi.mock('~/composables/useOrganizationApi', () => ({
  useOrganizationApi: () => ({
    listOrganizations: listOrganizationsMock,
  }),
}))

vi.mock('~/composables/usePassBusinessSharing', () => ({
  usePassBusinessSharing: () => passBusinessSharingFixture,
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    locale: { value: 'en' },
    t: (key: string, params?: Record<string, string | number>) => {
      if (!params) {
        return key
      }
      return Object.entries(params).reduce(
        (result, [paramKey, value]) => result.replace(`{${paramKey}}`, String(value)),
        key,
      )
    },
  }),
}))

const ElButton = defineComponent({
  name: 'ElButton',
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
})

const ElInput = defineComponent({
  name: 'ElInput',
  props: {
    modelValue: { type: [String, Number], default: '' },
    type: { type: String, default: 'text' },
  },
  emits: ['update:modelValue', 'keyup.enter'],
  template: `
    <textarea
      v-if="type === 'textarea'"
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
    />
    <input
      v-else
      :value="modelValue"
      @input="$emit('update:modelValue', $event.target.value)"
      @keyup.enter="$emit('keyup.enter')"
    />
  `,
})

const ElSelect = defineComponent({
  name: 'ElSelect',
  props: { modelValue: { type: [String, Array], default: '' } },
  emits: ['update:modelValue'],
  template: '<select @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
})

const ElOption = defineComponent({
  name: 'ElOption',
  props: { label: { type: String, default: '' }, value: { type: [String, Number], default: '' } },
  template: '<option :value="value">{{ label }}</option>',
})

const ElSwitch = defineComponent({
  name: 'ElSwitch',
  props: { modelValue: { type: Boolean, default: false } },
  emits: ['update:modelValue', 'change'],
  template: '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked); $emit(\'change\', $event.target.checked)">',
})

const ElEmpty = defineComponent({
  name: 'ElEmpty',
  props: { description: { type: String, default: '' } },
  template: '<div>{{ description }}</div>',
})

const ElDialog = defineComponent({
  name: 'ElDialog',
  props: { modelValue: { type: Boolean, default: false }, title: { type: String, default: '' } },
  emits: ['update:modelValue'],
  template: `
    <div v-if="modelValue" class="el-dialog-stub">
      <header>{{ title }}</header>
      <slot />
      <slot name="footer" />
    </div>
  `,
})

const ElInputNumber = defineComponent({
  name: 'ElInputNumber',
  props: { modelValue: { type: Number, default: 0 } },
  emits: ['update:modelValue'],
  template: '<input type="number" :value="modelValue" @input="$emit(\'update:modelValue\', Number($event.target.value))">',
})

const ElDatePicker = defineComponent({
  name: 'ElDatePicker',
  props: { modelValue: { type: [String, Date], default: '' } },
  emits: ['update:modelValue'],
  template: '<input type="datetime-local" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)">',
})

const ElCheckbox = defineComponent({
  name: 'ElCheckbox',
  props: { modelValue: { type: Boolean, default: false } },
  emits: ['update:modelValue'],
  template: '<label><input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)"><slot /></label>',
})

const ElAlert = defineComponent({
  name: 'ElAlert',
  props: { title: { type: String, default: '' } },
  template: '<div>{{ title }}</div>',
})

const ElTag = defineComponent({
  name: 'ElTag',
  template: '<span><slot /></span>',
})

function namedStub(name: string) {
  return defineComponent({
    name,
    template: '<div><slot /></div>',
  })
}

export const passStubs = {
  ElButton,
  ElInput,
  ElSelect,
  ElOption,
  ElSwitch,
  ElEmpty,
  ElDialog,
  ElInputNumber,
  ElDatePicker,
  ElCheckbox,
  ElAlert,
  ElTag,
  PassWorkspaceHero: namedStub('PassWorkspaceHero'),
  PassSidebarPanel: namedStub('PassSidebarPanel'),
  PassMailboxPanel: namedStub('PassMailboxPanel'),
  PassAliasCenter: namedStub('PassAliasCenter'),
  PassAliasContactsPanel: namedStub('PassAliasContactsPanel'),
  PassBusinessPolicyPanel: namedStub('PassBusinessPolicyPanel'),
  PassItemSharePanel: namedStub('PassItemSharePanel'),
  PassSecureLinkPanel: namedStub('PassSecureLinkPanel'),
  PassIncomingItemSharesPanel: namedStub('PassIncomingItemSharesPanel'),
  PassSecureLinksDashboard: namedStub('PassSecureLinksDashboard'),
  PassMonitorHero: namedStub('PassMonitorHero'),
  PassMonitorSectionPanel: namedStub('PassMonitorSectionPanel'),
  PassMonitorTwoFactorDialog: namedStub('PassMonitorTwoFactorDialog'),
}

const BASE_TIME = '2026-04-02T10:00:00'

function buildTwoFactor(enabled: boolean, overrides: Record<string, unknown> = {}) {
  return {
    enabled,
    issuer: enabled ? 'MMMail' : null,
    accountName: enabled ? 'owner@mmmail.local' : null,
    algorithm: enabled ? 'SHA1' : null,
    digits: enabled ? 6 : null,
    periodSeconds: enabled ? 30 : null,
    updatedAt: enabled ? '2026-04-02T10:05:00' : null,
    ...overrides,
  }
}

function buildPersonalItemDetail() {
  return {
    id: 'item-1',
    title: 'Primary Login',
    itemType: 'LOGIN',
    website: 'https://app.example.com',
    username: 'owner@mmmail.local',
    secretCiphertext: 'Secret#123',
    note: 'Primary note',
    favorite: false,
    ownerEmail: 'owner@mmmail.local',
    updatedAt: BASE_TIME,
    twoFactor: buildTwoFactor(false),
  }
}

function buildSharedItemDetail() {
  return {
    ...buildPersonalItemDetail(),
    id: 'shared-item-1',
    title: 'Shared Login',
    website: 'https://shared.example.com',
    username: 'shared@example.com',
  }
}

function buildIncomingSharedItemDetail(itemId: string) {
  return {
    itemId,
    title: 'Incoming Shared Item',
    readOnly: true,
    username: 'auditor@mmmail.local',
    secretCiphertext: 'ciphertext',
    note: 'Shared note',
  }
}

function applyPersonalWorkspaceMocks(detail: ReturnType<typeof buildPersonalItemDetail>): void {
  passApiMock.listItems.mockResolvedValue([{
    id: detail.id,
    title: detail.title,
    itemType: detail.itemType,
    website: detail.website,
    username: detail.username,
    favorite: false,
    updatedAt: BASE_TIME,
    secureLinkCount: 0,
  }])
  passApiMock.createItem.mockResolvedValue({ ...detail, id: 'item-2', title: 'New Item' })
  passApiMock.getItem.mockResolvedValue(detail)
  passApiMock.updateItem.mockResolvedValue({ ...detail, note: 'Updated note' })
  passApiMock.deleteItem.mockResolvedValue(undefined)
  passApiMock.favoriteItem.mockResolvedValue({ ...detail, favorite: true })
  passApiMock.unFavoriteItem.mockResolvedValue({ ...detail, favorite: false })
  passApiMock.generatePassword.mockResolvedValue({ password: 'Generated#123', policyApplied: true })
}

function applyMailboxMocks(): void {
  passApiMock.listMailboxes.mockResolvedValue([{ id: 'mailbox-1', mailboxEmail: 'owner@mmmail.local', status: 'VERIFIED', defaultMailbox: true, primaryMailbox: true }])
  passApiMock.createMailbox.mockResolvedValue({ id: 'mailbox-2', mailboxEmail: 'relay@mmmail.local', status: 'PENDING', defaultMailbox: false, primaryMailbox: false })
  passApiMock.verifyMailbox.mockResolvedValue({ id: 'mailbox-2', mailboxEmail: 'relay@mmmail.local', status: 'VERIFIED', defaultMailbox: false, primaryMailbox: false })
  passApiMock.setDefaultMailbox.mockResolvedValue({ id: 'mailbox-2', mailboxEmail: 'relay@mmmail.local', status: 'VERIFIED', defaultMailbox: true, primaryMailbox: false })
  passApiMock.deleteMailbox.mockResolvedValue(undefined)
}

function applyAliasMocks(): void {
  passApiMock.listAliases.mockResolvedValue([{ id: 'alias-1', aliasEmail: 'ops@passmail.mmmail.local', title: 'Ops', note: null, forwardToEmail: 'owner@mmmail.local', forwardToEmails: ['owner@mmmail.local'], status: 'ENABLED', createdAt: BASE_TIME, updatedAt: BASE_TIME }])
  passApiMock.createAlias.mockResolvedValue({ id: 'alias-2', aliasEmail: 'relay@passmail.mmmail.local', title: 'Relay', note: null, forwardToEmail: 'relay@mmmail.local', forwardToEmails: ['relay@mmmail.local'], status: 'ENABLED', createdAt: BASE_TIME, updatedAt: BASE_TIME })
  passApiMock.updateAlias.mockResolvedValue({ id: 'alias-1', aliasEmail: 'ops@passmail.mmmail.local', title: 'Ops Updated', note: null, forwardToEmail: 'owner@mmmail.local', forwardToEmails: ['owner@mmmail.local'], status: 'ENABLED', createdAt: BASE_TIME, updatedAt: '2026-04-02T10:10:00' })
  passApiMock.enableAlias.mockResolvedValue({ id: 'alias-1', status: 'ENABLED' })
  passApiMock.disableAlias.mockResolvedValue({ id: 'alias-1', status: 'DISABLED' })
  passApiMock.deleteAlias.mockResolvedValue(undefined)
  passApiMock.listAliasContacts.mockResolvedValue([{ id: 'contact-1', targetEmail: 'target@example.com', reverseAliasEmail: 'reply@reply.passmail.mmmail.local', displayName: 'Target', note: '', createdAt: BASE_TIME, updatedAt: BASE_TIME }])
  passApiMock.createAliasContact.mockResolvedValue({ id: 'contact-2', targetEmail: 'new@example.com', reverseAliasEmail: 'new@reply.passmail.mmmail.local', displayName: 'New', note: '', createdAt: BASE_TIME, updatedAt: BASE_TIME })
  passApiMock.updateAliasContact.mockResolvedValue({ id: 'contact-1', targetEmail: 'target@example.com', reverseAliasEmail: 'reply@reply.passmail.mmmail.local', displayName: 'Target Updated', note: '', createdAt: BASE_TIME, updatedAt: '2026-04-02T10:05:00' })
  passApiMock.deleteAliasContact.mockResolvedValue(undefined)
}

function applySharedWorkspaceMocks(sharedDetail: ReturnType<typeof buildSharedItemDetail>): void {
  passApiMock.getBusinessOverview.mockResolvedValue({ orgId: 'org-1', currentRole: 'OWNER', sharedVaultCount: 1, memberCount: 2, itemCount: 1, secureLinkCount: 1 })
  passApiMock.getBusinessPolicy.mockResolvedValue({ orgId: 'org-1', minimumPasswordLength: 12, maximumPasswordLength: 20, requireUppercase: true, requireDigits: true, requireSymbols: true, allowMemorablePasswords: false, allowExternalSharing: true, allowItemSharing: true, allowSecureLinks: true, allowMemberVaultCreation: true, allowExport: false, forceTwoFactor: true, allowPasskeys: true, allowAliases: true, updatedAt: BASE_TIME })
  passApiMock.updateBusinessPolicy.mockResolvedValue({ orgId: 'org-1', minimumPasswordLength: 12, maximumPasswordLength: 20, requireUppercase: true, requireDigits: true, requireSymbols: true, allowMemorablePasswords: false, allowExternalSharing: true, allowItemSharing: true, allowSecureLinks: true, allowMemberVaultCreation: true, allowExport: false, forceTwoFactor: true, allowPasskeys: true, allowAliases: true, updatedAt: '2026-04-02T10:10:00' })
  passApiMock.listSharedVaults.mockResolvedValue([{ id: 'vault-1', name: 'Release Vault', description: 'Shared credentials', memberCount: 2, itemCount: 1, accessRole: 'OWNER' }])
  passApiMock.createSharedVault.mockResolvedValue({ id: 'vault-2', name: 'New Vault', description: 'New shared vault', memberCount: 1, itemCount: 0, accessRole: 'OWNER' })
  passApiMock.listSharedVaultMembers.mockResolvedValue([{ id: 'member-1', userEmail: 'owner@mmmail.local', role: 'OWNER' }])
  passApiMock.addSharedVaultMember.mockResolvedValue({ id: 'member-2', userEmail: 'member@mmmail.local', role: 'MEMBER' })
  passApiMock.removeSharedVaultMember.mockResolvedValue(undefined)
  passApiMock.listSharedItems.mockResolvedValue([{ id: sharedDetail.id, title: sharedDetail.title, itemType: sharedDetail.itemType, website: sharedDetail.website, username: sharedDetail.username, favorite: false, updatedAt: BASE_TIME, secureLinkCount: 1 }])
  passApiMock.createSharedItem.mockResolvedValue(sharedDetail)
  passApiMock.getSharedItem.mockResolvedValue(sharedDetail)
  passApiMock.updateSharedItem.mockResolvedValue({ ...sharedDetail, note: 'Shared note updated' })
  passApiMock.deleteSharedItem.mockResolvedValue(undefined)
  passApiMock.listActivity.mockResolvedValue([{ id: 'event-1', eventType: 'PASS_ITEM_CREATE', detail: 'Created shared item', createdAt: BASE_TIME }])
  passApiMock.listSecureLinks.mockResolvedValue([{ id: 'link-1', itemId: sharedDetail.id, sharedVaultId: 'vault-1', token: 'public-token', publicUrl: 'https://mmmail.local/share/pass/public-token', maxViews: 5, currentViews: 1, expiresAt: '2026-05-01T00:00:00', revokedAt: null, createdAt: BASE_TIME, active: true }])
  passApiMock.listOrgSecureLinks.mockResolvedValue([{ id: 'link-1', itemId: sharedDetail.id, sharedVaultId: 'vault-1', sharedVaultName: 'Release Vault', itemTitle: sharedDetail.title, publicUrl: 'https://mmmail.local/share/pass/public-token', status: 'ACTIVE', createdAt: BASE_TIME, expiresAt: '2026-05-01T00:00:00', currentViews: 1, maxViews: 5 }])
  passApiMock.createSecureLink.mockResolvedValue({ id: 'link-2', itemId: sharedDetail.id, sharedVaultId: 'vault-1', token: 'public-token-2', publicUrl: 'https://mmmail.local/share/pass/public-token-2', maxViews: 7, currentViews: 0, expiresAt: '2026-05-02T00:00:00', revokedAt: null, createdAt: BASE_TIME, active: true })
  passApiMock.revokeSecureLink.mockResolvedValue(undefined)
  passApiMock.getPublicSecureLink.mockResolvedValue({ itemId: sharedDetail.id, title: sharedDetail.title, itemType: sharedDetail.itemType, sharedVaultName: 'Release Vault', currentViews: 1, maxViews: 5, remainingViews: 4, expiresAt: '2026-05-01T00:00:00', website: sharedDetail.website, username: sharedDetail.username, secretCiphertext: 'ciphertext', note: 'Shared note' })
}

function applyMonitorMocks(detail: ReturnType<typeof buildPersonalItemDetail>, sharedDetail: ReturnType<typeof buildSharedItemDetail>): void {
  passApiMock.getPersonalMonitor.mockResolvedValue({ scopeType: 'PERSONAL', totalItemCount: 1, trackedItemCount: 1, weakPasswordCount: 1, reusedPasswordCount: 0, inactiveTwoFactorCount: 1, excludedItemCount: 0, weakPasswords: [{ id: detail.id, title: detail.title, scopeType: 'PERSONAL', excluded: false, canToggleExclusion: true, canManageTwoFactor: true, inactiveTwoFactor: true, twoFactor: buildTwoFactor(false) }], reusedPasswords: [], inactiveTwoFactorItems: [{ id: detail.id, title: detail.title, scopeType: 'PERSONAL', excluded: false, canToggleExclusion: true, canManageTwoFactor: true, inactiveTwoFactor: true, twoFactor: buildTwoFactor(false) }], excludedItems: [] })
  passApiMock.getSharedMonitor.mockResolvedValue({ scopeType: 'SHARED', orgId: 'org-1', currentRole: 'OWNER', totalItemCount: 1, trackedItemCount: 1, weakPasswordCount: 0, reusedPasswordCount: 0, inactiveTwoFactorCount: 1, excludedItemCount: 0, weakPasswords: [], reusedPasswords: [], inactiveTwoFactorItems: [{ id: sharedDetail.id, title: sharedDetail.title, scopeType: 'SHARED', orgId: 'org-1', excluded: false, canToggleExclusion: true, canManageTwoFactor: true, inactiveTwoFactor: true, twoFactor: buildTwoFactor(false) }], excludedItems: [] })
  passApiMock.excludePersonalMonitorItem.mockResolvedValue(undefined)
  passApiMock.includePersonalMonitorItem.mockResolvedValue(undefined)
  passApiMock.excludeSharedMonitorItem.mockResolvedValue(undefined)
  passApiMock.includeSharedMonitorItem.mockResolvedValue(undefined)
  passApiMock.upsertPersonalItemTwoFactor.mockResolvedValue({ ...detail, twoFactor: buildTwoFactor(true) })
  passApiMock.deletePersonalItemTwoFactor.mockResolvedValue(detail)
  passApiMock.generatePersonalItemTwoFactorCode.mockResolvedValue({ code: '123456', expiresAt: '2026-04-02T10:06:00' })
  passApiMock.upsertSharedItemTwoFactor.mockResolvedValue({ ...sharedDetail, twoFactor: buildTwoFactor(true, { accountName: 'shared@example.com' }) })
  passApiMock.deleteSharedItemTwoFactor.mockResolvedValue(sharedDetail)
  passApiMock.generateSharedItemTwoFactorCode.mockResolvedValue({ code: '654321', expiresAt: '2026-04-02T10:06:00' })
}

export function resetSharingState(): void {
  itemShares.value = [{ id: 'share-1', collaboratorEmail: 'auditor@mmmail.local', readOnly: true }]
  incomingSharedItems.value = [{ itemId: 'shared-item-1', title: 'Incoming Shared Item' }]
  selectedIncomingItemId.value = 'shared-item-1'
  incomingSharedItemDetail.value = buildIncomingSharedItemDetail('shared-item-1')
}

export function resetPassApiMocks(): void {
  const detail = buildPersonalItemDetail()
  const sharedDetail = buildSharedItemDetail()
  listOrganizationsMock.mockResolvedValue([{ id: 'org-1', name: 'Secure Org', role: 'OWNER' }])
  applyPersonalWorkspaceMocks(detail)
  applyMailboxMocks()
  applyAliasMocks()
  applySharedWorkspaceMocks(sharedDetail)
  applyMonitorMocks(detail, sharedDetail)
}

export function setupPassPageGlobals(): void {
  vi.stubGlobal('navigateTo', navigateToMock)
  vi.stubGlobal('useHead', useHeadMock)
  vi.stubGlobal('definePageMeta', definePageMetaMock)
  vi.stubGlobal('useRoute', () => routeState)
  Object.defineProperty(globalThis.navigator, 'clipboard', {
    configurable: true,
    value: {
      writeText: clipboardWriteTextMock,
    },
  })
  Object.defineProperty(globalThis.window, 'open', {
    configurable: true,
    value: windowOpenMock,
  })
}

export function findButton(wrapper: VueWrapper<any>, label: string) {
  const button = wrapper.findAll('button').find((item) => item.text().includes(label))
  if (!button) {
    throw new Error(`Button not found: ${label}`)
  }
  return button
}

export async function mountPassPage() {
  const { default: PassPage } = await import('~/pages/pass.vue')
  return shallowMount(PassPage, {
    global: {
      stubs: passStubs,
    },
  })
}

export async function mountPassMonitorPage() {
  const { default: PassMonitorPage } = await import('~/pages/pass-monitor.vue')
  return shallowMount(PassMonitorPage, {
    global: {
      stubs: passStubs,
    },
  })
}

export async function mountPublicSharePage() {
  const { default: PublicSharePage } = await import('~/pages/share/pass/[token].vue')
  return shallowMount(PublicSharePage, {
    global: {
      stubs: passStubs,
    },
  })
}
