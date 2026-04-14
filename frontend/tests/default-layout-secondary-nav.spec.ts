import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import type { MailAddressMode } from '~/types/api'

const authState: {
  isAuthenticated: boolean
  user: { displayName: string; email: string; mailAddressMode: MailAddressMode } | null
} = {
  isAuthenticated: true,
  user: {
    displayName: 'Member',
    email: 'member@example.com',
    mailAddressMode: 'PROTON_ADDRESS',
  },
}

const mailStore = {
  counts: {} as Record<string, number>,
  unreadCount: 0,
  customFolders: [] as Array<unknown>,
  updateStats: vi.fn((stats: { folderCounts: Record<string, number>; unreadCount: number }) => {
    mailStore.counts = stats.folderCounts
    mailStore.unreadCount = stats.unreadCount
  }),
  setCustomFolders: vi.fn((folders: Array<unknown>) => {
    mailStore.customFolders = folders
  }),
}

const orgAccessStore = {
  isProductEnabled: vi.fn((productKey: string) => productKey !== 'DOCS' && productKey !== 'SHEETS'),
  ensureLoaded: vi.fn(async () => undefined),
  hasScopes: false,
  activeOrgId: '',
  accessScopes: [],
  activeScope: null,
  setActiveOrgId: vi.fn(),
}

const fetchStatsMock = vi.fn(async () => ({
  folderCounts: { INBOX: 4 },
  unreadCount: 2,
}))
const listMailFoldersMock = vi.fn(async () => [])
const useKeyboardShortcutsMock = vi.fn()

vi.mock('~/stores/auth', () => ({
  useAuthStore: () => authState,
}))

vi.mock('~/stores/mail', () => ({
  useMailStore: () => mailStore,
}))

vi.mock('~/stores/org-access', () => ({
  useOrgAccessStore: () => orgAccessStore,
}))

vi.mock('~/composables/useMailApi', () => ({
  useMailApi: () => ({ fetchStats: fetchStatsMock }),
}))

vi.mock('~/composables/useMailFolderApi', () => ({
  useMailFolderApi: () => ({ listMailFolders: listMailFoldersMock }),
}))

vi.mock('~/composables/useKeyboardShortcuts', () => ({
  useKeyboardShortcuts: useKeyboardShortcutsMock,
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

async function mountDefaultLayout() {
  vi.resetModules()
  const { default: DefaultLayout } = await import('~/layouts/default.vue')
  return mount(DefaultLayout, {
    slots: {
      default: '<div>workspace</div>',
    },
    global: {
      stubs: {
        NuxtLink: {
          props: ['to'],
          template: '<a :href="to" :data-testid="$attrs[\'data-testid\']"><slot /></a>',
        },
        ShellNavLink: {
          props: ['to', 'label'],
          template: '<a :href="to" :data-testid="$attrs[\'data-testid\']">{{ label }}</a>',
        },
        ElInput: defineComponent({
          name: 'ElInput',
          props: {
            modelValue: { type: String, default: '' },
            placeholder: { type: String, default: '' },
            disabled: { type: Boolean, default: false },
          },
          template: '<input :value="modelValue" :placeholder="placeholder" :disabled="disabled">',
        }),
        ElBadge: defineComponent({
          name: 'ElBadge',
          props: {
            value: { type: [Number, String], default: undefined },
          },
          template: '<span class="badge-stub">{{ value }}</span>',
        }),
        OrgScopeSwitcher: true,
        LocaleSwitcher: true,
        GlobalCommandPalette: true,
      },
    },
  })
}

describe('default layout secondary navigation', () => {
  beforeEach(() => {
    authState.isAuthenticated = true
    authState.user = {
      displayName: 'Member',
      email: 'member@example.com',
      mailAddressMode: 'PROTON_ADDRESS',
    }
    mailStore.counts = {}
    mailStore.unreadCount = 0
    mailStore.customFolders = []
    mailStore.updateStats.mockClear()
    mailStore.setCustomFolders.mockClear()
    orgAccessStore.isProductEnabled.mockClear().mockImplementation((productKey: string) => productKey !== 'DOCS' && productKey !== 'SHEETS')
    orgAccessStore.ensureLoaded.mockClear().mockResolvedValue(undefined)
    orgAccessStore.setActiveOrgId.mockClear()
    fetchStatsMock.mockClear().mockResolvedValue({
      folderCounts: { INBOX: 4 },
      unreadCount: 2,
    })
    listMailFoldersMock.mockClear().mockResolvedValue([])
    useKeyboardShortcutsMock.mockClear()
    vi.stubGlobal('useRoute', () => ({ path: '/inbox', fullPath: '/inbox' }))
    vi.stubGlobal('navigateTo', vi.fn(async () => undefined))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('keeps mail overflow links visible while hiding disabled product links from the secondary rail', async () => {
    const wrapper = await mountDefaultLayout()

    await flushPromises()

    const secondaryRail = wrapper.get('.sidebar-secondary')

    expect(secondaryRail.get('[data-testid="default-nav-starred"]').attributes('href')).toBe('/starred')
    expect(secondaryRail.get('[data-testid="default-nav-labels"]').attributes('href')).toBe('/labels')
    expect(secondaryRail.get('[data-testid="default-nav-business"]').attributes('href')).toBe('/business')
    expect(secondaryRail.get('[data-testid="default-nav-organizations"]').attributes('href')).toBe('/organizations')
    expect(secondaryRail.find('[data-testid="default-nav-docs"]').exists()).toBe(false)
    expect(secondaryRail.find('[data-testid="default-nav-sheets"]').exists()).toBe(false)
  })
})
