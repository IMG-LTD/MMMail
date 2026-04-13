import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick, Suspense } from 'vue'
import type { MailAddressMode } from '~/types/api'

const authState: { isAuthenticated: boolean; needsSessionRefresh: boolean; user: { mailAddressMode: MailAddressMode } | null } = {
  isAuthenticated: false,
  needsSessionRefresh: false,
  user: null,
}
const ensureLoadedMock = vi.fn()
const refreshSessionMock = vi.fn(async () => true)
const useAuthApiMock = vi.fn(() => ({ refreshSession: refreshSessionMock }))
const navigateToMock = vi.fn(async () => undefined)

type SupportedTestLocale = 'en' | 'zh-CN'
type MockI18nModule = {
  __marketingMessages: Record<SupportedTestLocale, Record<string, string>>
  __setMockLocale: (locale: SupportedTestLocale) => void
}

vi.mock('~/stores/auth', () => ({
  useAuthStore: () => authState,
}))

vi.mock('~/stores/org-access', () => ({
  useOrgAccessStore: () => ({ ensureLoaded: ensureLoadedMock, isProductEnabled: () => true }),
}))

vi.mock('~/composables/useAuthApi', () => ({
  useAuthApi: useAuthApiMock,
}))

vi.mock('~/composables/useI18n', async () => {
  const { computed, ref } = await import('vue')
  const locale = ref<SupportedTestLocale>('en')
  const messages: Record<SupportedTestLocale, Record<string, string>> = {
    en: {
      'marketing.hero.eyebrow': 'MMMail Community Edition',
      'marketing.hero.title': 'Modern privacy-first collaboration',
      'marketing.hero.subtitle': 'Keep mail, calendar, drive, and pass inside one open stack.',
      'marketing.hero.primary': 'Open MMMail',
      'marketing.hero.secondary': 'See release boundaries',
      'marketing.trust.deploy.title': 'Deploy on your own infrastructure',
      'marketing.trust.deploy.description': 'Run the community edition where your team already operates.',
      'marketing.trust.boundary.title': 'Boundary-first release',
      'marketing.trust.boundary.description': 'See exactly what is GA, Beta, Preview, and Hosted-only.',
      'marketing.trust.ops.title': 'Practice the runbook before launch',
      'marketing.trust.ops.description': 'Rehearse the operational path before inviting everyone in.',
      'marketing.mainline.title': 'One suite, four workflows',
      'marketing.mainline.mail.name': 'Mail',
      'marketing.mainline.mail': 'Keep every mailbox inside your control plane.',
      'marketing.mainline.calendar.name': 'Calendar',
      'marketing.mainline.calendar': 'Coordinate shared availability without leaving the suite.',
      'marketing.mainline.drive.name': 'Drive',
      'marketing.mainline.drive': 'Store documents where your operators already monitor them.',
      'marketing.mainline.pass.name': 'Pass',
      'marketing.mainline.pass': 'Share access with auditable controls.',
      'marketing.shots.mail.title': 'Mail workspace',
      'marketing.shots.mail.caption': 'Monitor queues, delivery, and mailbox health together.',
      'marketing.shots.suite.title': 'Suite overview',
      'marketing.shots.suite.caption': 'Switch between products without losing your place.',
      'marketing.shots.health.title': 'Operations health',
      'marketing.shots.health.caption': 'Review service health before users notice drift.',
    },
    'zh-CN': {
      'marketing.hero.eyebrow': 'MMMail 社区版',
      'marketing.hero.title': '面向现代团队的隐私协作套件',
      'marketing.hero.subtitle': '将邮件、日历、网盘与密码管理保留在同一套开放栈内。',
      'marketing.hero.primary': '打开 MMMail',
      'marketing.hero.secondary': '查看发布边界',
      'marketing.trust.deploy.title': '部署在你自己的基础设施上',
      'marketing.trust.deploy.description': '把社区版运行在团队现有的环境中。',
      'marketing.trust.boundary.title': '先讲清边界',
      'marketing.trust.boundary.description': '明确区分 GA、Beta、Preview 和 Hosted-only。',
      'marketing.trust.ops.title': '上线前先演练运行手册',
      'marketing.trust.ops.description': '在邀请所有人之前先走通运维路径。',
      'marketing.mainline.title': '一套系统，四条主线',
      'marketing.mainline.mail.name': '邮件',
      'marketing.mainline.mail': '让每一个邮箱都留在你的控制平面内。',
      'marketing.mainline.calendar.name': '日历',
      'marketing.mainline.calendar': '无需离开套件即可协调共享日程。',
      'marketing.mainline.drive.name': '网盘',
      'marketing.mainline.drive': '把文档存放在运维已监控的边界之内。',
      'marketing.mainline.pass.name': '密码库',
      'marketing.mainline.pass': '以可审计的方式共享访问凭据。',
      'marketing.shots.mail.title': '邮件工作台',
      'marketing.shots.mail.caption': '统一查看队列、投递情况与邮箱健康状态。',
      'marketing.shots.suite.title': '套件总览',
      'marketing.shots.suite.caption': '在各产品之间切换也不会丢失上下文。',
      'marketing.shots.health.title': '运维健康概览',
      'marketing.shots.health.caption': '在用户察觉漂移前先审查服务健康状态。',
    },
  }

  return {
    __marketingMessages: messages,
    __setMockLocale(value: SupportedTestLocale) {
      locale.value = value
    },
    useI18n: () => ({
      locale: computed(() => locale.value),
      t: (key: string) => messages[locale.value][key] ?? key,
    }),
  }
})

vi.mock('~/utils/org-product-access', () => ({
  resolveHomeRoute: () => '/inbox',
}))

async function getMockI18nModule(): Promise<MockI18nModule> {
  return await import('~/composables/useI18n') as unknown as MockI18nModule
}

async function mountPublicHome() {
  const { default: Page } = await import('~/pages/index.vue')
  return mount(defineComponent({
    render: () => h(Suspense, null, {
      default: () => h(Page),
    }),
  }), {
    global: {
      stubs: {
        NuxtLink: {
          props: ['to'],
          template: '<a :href="to" data-router-link="true"><slot /></a>',
        },
      },
    },
  })
}

function readSource(path: string) {
  return readFileSync(resolve(process.cwd(), path), 'utf8')
}

describe('public home page', () => {
  beforeEach(async () => {
    vi.stubGlobal('definePageMeta', vi.fn())
    vi.stubGlobal('navigateTo', navigateToMock)
    vi.stubGlobal('useHead', vi.fn())
    authState.isAuthenticated = false
    authState.needsSessionRefresh = false
    authState.user = null
    ensureLoadedMock.mockReset()
    refreshSessionMock.mockReset().mockResolvedValue(true)
    useAuthApiMock.mockClear()
    navigateToMock.mockReset().mockImplementation(async () => undefined)
    const { __setMockLocale } = await getMockI18nModule()
    __setMockLocale('en')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders the signed-out landing page CTAs and translated mainline heading', async () => {
    const { __marketingMessages } = await getMockI18nModule()
    const wrapper = await mountPublicHome()

    await flushPromises()

    const hero = wrapper.get('[data-testid="marketing-hero"]')
    const loginLink = hero.get('a[href="/login"]')
    const boundaryLink = hero.get('a[href="/boundary"]')

    expect(navigateToMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain(__marketingMessages.en['marketing.hero.title'])
    expect(loginLink.text()).toContain(__marketingMessages.en['marketing.hero.primary'])
    expect(boundaryLink.text()).toContain(__marketingMessages.en['marketing.hero.secondary'])
    expect(wrapper.get('[data-testid="marketing-mainline-flow"] h2').text()).toBe(__marketingMessages.en['marketing.mainline.title'])
  })

  it('uses a non-default layout so signed-out visitors do not mount the authenticated shell', () => {
    const source = readSource('pages/index.vue')

    expect(source).toContain("definePageMeta({ layout: 'blank' })")
  })

  it('defines a public boundary page on the blank layout', () => {
    const source = readSource('pages/boundary.vue')

    expect(source).toContain("definePageMeta({ layout: 'blank' })")
    expect(source).toContain('SuiteReleaseBoundaryPanel')
  })

  it('redirects authenticated users to their resolved home route without refreshing an active session', async () => {
    authState.isAuthenticated = true
    authState.user = { mailAddressMode: 'PROTON_ADDRESS' }

    await mountPublicHome()
    await flushPromises()

    expect(useAuthApiMock).not.toHaveBeenCalled()
    expect(refreshSessionMock).not.toHaveBeenCalled()
    expect(ensureLoadedMock).toHaveBeenCalledTimes(1)
    expect(navigateToMock).toHaveBeenCalledWith('/inbox')
  })

  it('refreshes restored sessions before redirecting from the public landing page', async () => {
    authState.isAuthenticated = true
    authState.needsSessionRefresh = true
    authState.user = { mailAddressMode: 'PROTON_ADDRESS' }

    await mountPublicHome()
    await flushPromises()

    expect(refreshSessionMock).toHaveBeenCalledTimes(1)
    expect(ensureLoadedMock).toHaveBeenCalledTimes(1)
    expect(navigateToMock).toHaveBeenCalledWith('/inbox')
  })

  it('keeps the public landing page when restored session refresh fails', async () => {
    const { __marketingMessages } = await getMockI18nModule()
    authState.isAuthenticated = true
    authState.needsSessionRefresh = true
    authState.user = { mailAddressMode: 'PROTON_ADDRESS' }
    refreshSessionMock.mockResolvedValueOnce(false)

    const wrapper = await mountPublicHome()
    await flushPromises()

    expect(refreshSessionMock).toHaveBeenCalledTimes(1)
    expect(ensureLoadedMock).not.toHaveBeenCalled()
    expect(navigateToMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain(__marketingMessages.en['marketing.hero.title'])
  })

  it('renders self-hosted trust links as plain anchors instead of router links', async () => {
    const wrapper = await mountPublicHome()

    await flushPromises()

    const installLink = wrapper.get('a[href="/self-hosted/install.html"]')
    const runbookLink = wrapper.get('a[href="/self-hosted/runbook.html"]')
    const boundaryLink = wrapper.get('a[href="/boundary"]')

    expect(installLink.attributes('data-router-link')).toBeUndefined()
    expect(runbookLink.attributes('data-router-link')).toBeUndefined()
    expect(boundaryLink.attributes('data-router-link')).toBe('true')
    expect(installLink.attributes('href')).toBe('/self-hosted/install.html')
    expect(runbookLink.attributes('href')).toBe('/self-hosted/runbook.html')
    expect(boundaryLink.attributes('href')).toBe('/boundary')
  })

  it('keeps document links as plain anchors when query or hash is present', async () => {
    const { default: MarketingTrustGrid } = await import('~/components/marketing/MarketingTrustGrid.vue')
    const wrapper = mount(MarketingTrustGrid, {
      props: {
        cards: [
          { title: 'Install', description: 'Install docs', href: '/self-hosted/install.html?lang=en', mode: 'document' },
          { title: 'Runbook', description: 'Runbook docs', href: '/self-hosted/runbook.html#ops', mode: 'document' },
          { title: 'Boundary', description: 'Boundary docs', href: '/boundary', mode: 'route' },
        ],
      },
      global: {
        stubs: {
          NuxtLink: {
            props: ['to'],
            template: '<a :href="to" data-router-link="true"><slot /></a>',
          },
        },
      },
    })

    const installLink = wrapper.get('a[href="/self-hosted/install.html?lang=en"]')
    const runbookLink = wrapper.get('a[href="/self-hosted/runbook.html#ops"]')
    const boundaryLink = wrapper.get('a[href="/boundary"]')

    expect(installLink.attributes('data-router-link')).toBeUndefined()
    expect(runbookLink.attributes('data-router-link')).toBeUndefined()
    expect(boundaryLink.attributes('data-router-link')).toBe('true')
  })

  it('keeps route links on NuxtLink even when html appears in query or hash', async () => {
    const { default: MarketingTrustGrid } = await import('~/components/marketing/MarketingTrustGrid.vue')
    const wrapper = mount(MarketingTrustGrid, {
      props: {
        cards: [
          { title: 'Redirect', description: 'Redirect docs', href: '/suite?redirect=/self-hosted/install.html', mode: 'route' },
          { title: 'Hash', description: 'Hash docs', href: '/suite#install.html', mode: 'route' },
        ],
      },
      global: {
        stubs: {
          NuxtLink: {
            props: ['to'],
            template: '<a :href="to" data-router-link="true"><slot /></a>',
          },
        },
      },
    })

    expect(wrapper.get('a[href="/suite?redirect=/self-hosted/install.html"]').attributes('data-router-link')).toBe('true')
    expect(wrapper.get('a[href="/suite#install.html"]').attributes('data-router-link')).toBe('true')
  })

  it('updates translated marketing sections when locale changes after mount', async () => {
    const { __marketingMessages, __setMockLocale } = await getMockI18nModule()
    const wrapper = await mountPublicHome()
    const flowNames = () => wrapper.findAll('[data-testid="marketing-mainline-flow"] strong').map(node => node.text())

    await flushPromises()

    expect(wrapper.text()).toContain(__marketingMessages.en['marketing.hero.eyebrow'])
    expect(wrapper.text()).toContain(__marketingMessages.en['marketing.hero.title'])
    expect(wrapper.text()).toContain(__marketingMessages.en['marketing.trust.deploy.title'])
    expect(flowNames()).toEqual([
      __marketingMessages.en['marketing.mainline.mail.name'],
      __marketingMessages.en['marketing.mainline.calendar.name'],
      __marketingMessages.en['marketing.mainline.drive.name'],
      __marketingMessages.en['marketing.mainline.pass.name'],
    ])
    expect(wrapper.text()).toContain(__marketingMessages.en['marketing.mainline.mail'])
    expect(wrapper.text()).toContain(__marketingMessages.en['marketing.shots.mail.title'])

    __setMockLocale('zh-CN')
    await nextTick()
    await flushPromises()

    expect(wrapper.text()).toContain(__marketingMessages['zh-CN']['marketing.hero.eyebrow'])
    expect(wrapper.text()).toContain(__marketingMessages['zh-CN']['marketing.hero.title'])
    expect(wrapper.text()).toContain(__marketingMessages['zh-CN']['marketing.trust.deploy.title'])
    expect(flowNames()).toEqual([
      __marketingMessages['zh-CN']['marketing.mainline.mail.name'],
      __marketingMessages['zh-CN']['marketing.mainline.calendar.name'],
      __marketingMessages['zh-CN']['marketing.mainline.drive.name'],
      __marketingMessages['zh-CN']['marketing.mainline.pass.name'],
    ])
    expect(wrapper.text()).toContain(__marketingMessages['zh-CN']['marketing.mainline.mail'])
    expect(wrapper.text()).toContain(__marketingMessages['zh-CN']['marketing.shots.mail.title'])
    expect(wrapper.get('[data-testid="marketing-mainline-flow"] h2').text()).toBe(__marketingMessages['zh-CN']['marketing.mainline.title'])
    expect(wrapper.text()).not.toContain(__marketingMessages.en['marketing.hero.eyebrow'])
    expect(wrapper.text()).not.toContain(__marketingMessages.en['marketing.hero.title'])
    expect(wrapper.text()).not.toContain(__marketingMessages.en['marketing.trust.deploy.title'])
    expect(wrapper.text()).not.toContain(__marketingMessages.en['marketing.mainline.mail'])
    expect(wrapper.text()).not.toContain(__marketingMessages.en['marketing.shots.mail.title'])
  })
})
