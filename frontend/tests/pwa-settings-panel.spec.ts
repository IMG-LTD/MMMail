import { computed, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const installAppMock = vi.fn()
const requestNotificationPermissionMock = vi.fn()
const messageSuccessMock = vi.fn()
const messageWarningMock = vi.fn()

const installPromptAvailable = ref(false)
const installSupported = ref(true)
const isInstalled = ref(false)
const isOnline = ref(true)
const notificationsSupported = ref(true)
const notificationPermission = ref<'default' | 'granted' | 'denied' | 'unsupported'>('default')
const registerError = ref<string | null>(null)
const serviceWorkerRegistered = ref(true)
const serviceWorkerSupported = ref(true)

vi.mock('element-plus', () => ({
  ElMessage: {
    success: messageSuccessMock,
    warning: messageWarningMock
  }
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string) => key
  })
}))

vi.mock('~/composables/usePwaInstall', () => ({
  usePwaInstall: () => ({
    installPromptAvailable: computed(() => installPromptAvailable.value),
    installSupported: computed(() => installSupported.value),
    isInstalled: computed(() => isInstalled.value),
    isOnline: computed(() => isOnline.value),
    notificationsSupported: computed(() => notificationsSupported.value),
    notificationPermission: computed(() => notificationPermission.value),
    registerError: computed(() => registerError.value),
    serviceWorkerRegistered: computed(() => serviceWorkerRegistered.value),
    serviceWorkerSupported: computed(() => serviceWorkerSupported.value),
    installApp: installAppMock,
    requestNotificationPermission: requestNotificationPermissionMock
  })
}))

describe('SettingsPwaPanel', () => {
  beforeEach(() => {
    installAppMock.mockReset()
    requestNotificationPermissionMock.mockReset()
    messageSuccessMock.mockReset()
    messageWarningMock.mockReset()
    installPromptAvailable.value = false
    installSupported.value = true
    isInstalled.value = false
    isOnline.value = true
    notificationsSupported.value = true
    notificationPermission.value = 'default'
    registerError.value = null
    serviceWorkerRegistered.value = true
    serviceWorkerSupported.value = true
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('renders PWA readiness states', async () => {
    installPromptAvailable.value = true
    isOnline.value = false

    const panel = await mountPanel()

    expect(panel.get('[data-testid="settings-pwa-install-status"]').text()).toContain('settings.pwa.status.installReady')
    expect(panel.get('[data-testid="settings-pwa-service-worker-status"]').text()).toContain('settings.pwa.status.serviceWorkerReady')
    expect(panel.get('[data-testid="settings-pwa-connection-status"]').text()).toContain('settings.pwa.status.offline')
    expect(panel.get('[data-testid="settings-pwa-notification-status"]').text()).toContain('settings.pwa.status.notificationsDefault')
  })

  it('runs install flow and shows success feedback', async () => {
    installPromptAvailable.value = true
    installAppMock.mockResolvedValue('accepted')

    const panel = await mountPanel()
    await panel.get('[data-testid="settings-pwa-install-button"]').trigger('click')
    await flushPromises()

    expect(installAppMock).toHaveBeenCalledTimes(1)
    expect(messageSuccessMock).toHaveBeenCalledWith('settings.pwa.messages.installAccepted')
  })

  it('requests notification permission and shows blocked feedback', async () => {
    requestNotificationPermissionMock.mockResolvedValue('denied')

    const panel = await mountPanel()
    await panel.get('[data-testid="settings-pwa-notification-button"]').trigger('click')
    await flushPromises()

    expect(requestNotificationPermissionMock).toHaveBeenCalledTimes(1)
    expect(messageWarningMock).toHaveBeenCalledWith('settings.pwa.messages.notificationsDenied')
  })
})

async function mountPanel() {
  const { default: SettingsPwaPanel } = await import('~/components/settings/SettingsPwaPanel.vue')
  return mount(SettingsPwaPanel, {
    global: {
      stubs: {
        ElAlert: {
          props: ['title', 'description'],
          template: '<div><span>{{ title }}</span><span>{{ description }}</span></div>'
        },
        ElButton: {
          props: ['disabled', 'type'],
          emits: ['click'],
          template: `<button :disabled="disabled" @click="$emit('click')"><slot /></button>`
        }
      }
    }
  })
}
