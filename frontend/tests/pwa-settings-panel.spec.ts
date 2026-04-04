import { computed, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const installAppMock = vi.fn()
const requestNotificationPermissionMock = vi.fn()
const refreshWebPushStatusMock = vi.fn()
const subscribeCurrentBrowserMock = vi.fn()
const unsubscribeCurrentBrowserMock = vi.fn()
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
const webPushState = ref<'idle' | 'loading' | 'subscribed' | 'not-subscribed' | 'unsupported' | 'disabled' | 'missing-vapid' | 'error'>('not-subscribed')
const webPushError = ref<string | null>(null)
const webPushSupported = ref(true)
const webPushServerStatus = ref({
  enabled: true,
  deliveryScope: 'MAIL_INBOX',
  vapidPublicKey: 'test-vapid-public-key',
  message: null as string | null
})

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

vi.mock('~/composables/usePwaWebPush', () => ({
  usePwaWebPush: () => ({
    webPushState: computed(() => webPushState.value),
    webPushError: computed(() => webPushError.value),
    webPushServerStatus: computed(() => webPushServerStatus.value),
    webPushSupported: computed(() => webPushSupported.value),
    refreshWebPushStatus: refreshWebPushStatusMock,
    subscribeCurrentBrowser: subscribeCurrentBrowserMock,
    unsubscribeCurrentBrowser: unsubscribeCurrentBrowserMock
  })
}))

describe('SettingsPwaPanel', () => {
  beforeEach(() => {
    installAppMock.mockReset()
    requestNotificationPermissionMock.mockReset()
    refreshWebPushStatusMock.mockReset()
    subscribeCurrentBrowserMock.mockReset()
    unsubscribeCurrentBrowserMock.mockReset()
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
    webPushState.value = 'not-subscribed'
    webPushError.value = null
    webPushSupported.value = true
    webPushServerStatus.value = {
      enabled: true,
      deliveryScope: 'MAIL_INBOX',
      vapidPublicKey: 'test-vapid-public-key',
      message: null
    }
    refreshWebPushStatusMock.mockResolvedValue('not-subscribed')
    subscribeCurrentBrowserMock.mockResolvedValue(undefined)
    unsubscribeCurrentBrowserMock.mockResolvedValue(undefined)
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
    expect(panel.get('[data-testid="settings-pwa-push-status"]').text()).toContain('settings.pwa.status.pushNotSubscribed')
    expect(refreshWebPushStatusMock).toHaveBeenCalledTimes(1)
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

  it('requests permission, subscribes push, and shows success feedback', async () => {
    requestNotificationPermissionMock.mockResolvedValue('granted')

    const panel = await mountPanel()
    await panel.get('[data-testid="settings-pwa-subscribe-button"]').trigger('click')
    await flushPromises()

    expect(requestNotificationPermissionMock).toHaveBeenCalledTimes(1)
    expect(subscribeCurrentBrowserMock).toHaveBeenCalledTimes(1)
    expect(refreshWebPushStatusMock).toHaveBeenCalledTimes(2)
    expect(messageSuccessMock).toHaveBeenCalledWith('settings.pwa.messages.pushSubscribed')
  })

  it('shows blocked feedback when notification permission is denied', async () => {
    requestNotificationPermissionMock.mockResolvedValue('denied')

    const panel = await mountPanel()
    await panel.get('[data-testid="settings-pwa-subscribe-button"]').trigger('click')
    await flushPromises()

    expect(subscribeCurrentBrowserMock).not.toHaveBeenCalled()
    expect(messageWarningMock).toHaveBeenCalledWith('settings.pwa.messages.notificationsDenied')
  })

  it('unsubscribes push and shows success feedback', async () => {
    webPushState.value = 'subscribed'
    notificationPermission.value = 'granted'

    const panel = await mountPanel()
    await panel.get('[data-testid="settings-pwa-unsubscribe-button"]').trigger('click')
    await flushPromises()

    expect(unsubscribeCurrentBrowserMock).toHaveBeenCalledTimes(1)
    expect(messageSuccessMock).toHaveBeenCalledWith('settings.pwa.messages.pushUnsubscribed')
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
