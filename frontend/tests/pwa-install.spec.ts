import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import {
  resetPwaInstallStateForTests,
  usePwaInstall,
  type PwaNotificationPermissionState
} from '~/composables/usePwaInstall'

const registerMock = vi.fn()

let notificationPermissionState: PwaNotificationPermissionState = 'default'
let requestPermissionMock = vi.fn()

function installNotificationMock(): void {
  class NotificationMock {
    static requestPermission = requestPermissionMock
  }

  Object.defineProperty(NotificationMock, 'permission', {
    get: () => notificationPermissionState
  })

  Object.defineProperty(globalThis, 'Notification', {
    value: NotificationMock,
    configurable: true
  })
}

function setNavigatorOnline(value: boolean): void {
  Object.defineProperty(window.navigator, 'onLine', {
    value,
    configurable: true
  })
}

function setServiceWorkerSupport(enabled: boolean): void {
  if (enabled) {
    Object.defineProperty(window.navigator, 'serviceWorker', {
      value: { register: registerMock },
      configurable: true
    })
    return
  }

  Reflect.deleteProperty(window.navigator, 'serviceWorker')
}

function installMatchMediaMock(): void {
  Object.defineProperty(window, 'matchMedia', {
    value: vi.fn().mockReturnValue({ matches: false }),
    configurable: true
  })
}

async function mountHost() {
  const Host = defineComponent({
    setup() {
      return usePwaInstall()
    },
    template: '<div />'
  })

  return mount(Host)
}

describe('usePwaInstall', () => {
  beforeEach(() => {
    resetPwaInstallStateForTests()
    registerMock.mockReset()
    registerMock.mockResolvedValue({})
    requestPermissionMock = vi.fn(async () => {
      notificationPermissionState = 'granted'
      return 'granted'
    })
    notificationPermissionState = 'default'
    installMatchMediaMock()
    installNotificationMock()
    setNavigatorOnline(true)
    setServiceWorkerSupport(true)
  })

  afterEach(() => {
    resetPwaInstallStateForTests()
    Reflect.deleteProperty(globalThis, 'Notification')
  })

  it('registers the service worker and completes accepted install flow', async () => {
    const wrapper = await mountHost()
    await flushPromises()

    expect(registerMock).toHaveBeenCalledWith('/sw.js')
    expect(wrapper.vm.serviceWorkerRegistered).toBe(true)

    const promptEvent = new Event('beforeinstallprompt') as Event & {
      prompt: ReturnType<typeof vi.fn>
      userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>
    }
    promptEvent.prompt = vi.fn().mockResolvedValue(undefined)
    promptEvent.userChoice = Promise.resolve({ outcome: 'accepted', platform: 'web' })
    window.dispatchEvent(promptEvent)
    await flushPromises()

    expect(wrapper.vm.installPromptAvailable).toBe(true)
    await expect(wrapper.vm.installApp()).resolves.toBe('accepted')
    expect(wrapper.vm.isInstalled).toBe(true)
  })

  it('tracks connectivity and notification permission changes', async () => {
    setNavigatorOnline(false)
    const wrapper = await mountHost()
    await flushPromises()

    expect(wrapper.vm.isOnline).toBe(false)
    await expect(wrapper.vm.requestNotificationPermission()).resolves.toBe('granted')
    expect(wrapper.vm.notificationPermission).toBe('granted')

    setNavigatorOnline(true)
    window.dispatchEvent(new Event('online'))
    await flushPromises()

    expect(wrapper.vm.isOnline).toBe(true)
  })
})
