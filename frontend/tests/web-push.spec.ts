import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { resetPwaWebPushStateForTests, usePwaWebPush } from '~/composables/usePwaWebPush'

const getWebPushStatusMock = vi.fn()
const subscribeWebPushMock = vi.fn()
const unsubscribeWebPushMock = vi.fn()

const getSubscriptionMock = vi.fn()
const subscribeMock = vi.fn()

let currentSubscription: PushSubscription | null = null
let unsubscribeMock = vi.fn()

vi.mock('~/composables/useSuiteApi', () => ({
  useSuiteApi: () => ({
    getWebPushStatus: getWebPushStatusMock,
    subscribeWebPush: subscribeWebPushMock,
    unsubscribeWebPush: unsubscribeWebPushMock
  })
}))

function createPushSubscription(endpoint: string): PushSubscription {
  unsubscribeMock = vi.fn(async () => true)
  return {
    endpoint,
    expirationTime: null,
    options: { userVisibleOnly: true } as PushSubscriptionOptions,
    getKey: () => null,
    toJSON: () => ({
      endpoint,
      keys: {
        p256dh: 'test-p256dh',
        auth: 'test-auth'
      }
    }),
    unsubscribe: unsubscribeMock
  } as unknown as PushSubscription
}

function installPushRuntime(): void {
  Object.defineProperty(window, 'PushManager', {
    value: class PushManagerMock {},
    configurable: true
  })
  Object.defineProperty(window.PushManager, 'supportedContentEncodings', {
    value: ['aes128gcm'],
    configurable: true
  })

  Object.defineProperty(window.navigator, 'serviceWorker', {
    value: {
      getRegistration: vi.fn(async () => ({
        pushManager: {
          getSubscription: getSubscriptionMock,
          subscribe: subscribeMock
        }
      })),
      ready: Promise.resolve({
        pushManager: {
          getSubscription: getSubscriptionMock,
          subscribe: subscribeMock
        }
      })
    },
    configurable: true
  })
}

async function mountHost() {
  const Host = defineComponent({
    setup() {
      return usePwaWebPush()
    },
    template: '<div />'
  })

  return mount(Host)
}

describe('usePwaWebPush', () => {
  beforeEach(() => {
    resetPwaWebPushStateForTests()
    getWebPushStatusMock.mockReset()
    subscribeWebPushMock.mockReset()
    unsubscribeWebPushMock.mockReset()
    getSubscriptionMock.mockReset()
    subscribeMock.mockReset()

    currentSubscription = null
    getSubscriptionMock.mockImplementation(async () => currentSubscription)
    subscribeMock.mockImplementation(async () => {
      currentSubscription = createPushSubscription('https://push.example/subscription-1')
      return currentSubscription
    })

    getWebPushStatusMock.mockResolvedValue({
      enabled: true,
      deliveryScope: 'MAIL_INBOX',
      vapidPublicKey: 'dGVzdC12YXBpZC1wdWJsaWMta2V5',
      message: null
    })
    subscribeWebPushMock.mockResolvedValue(undefined)
    unsubscribeWebPushMock.mockResolvedValue(undefined)

    installPushRuntime()
  })

  afterEach(() => {
    resetPwaWebPushStateForTests()
    Reflect.deleteProperty(window, 'PushManager')
    Reflect.deleteProperty(window.navigator, 'serviceWorker')
  })

  it('refreshes status to not-subscribed when browser has no local subscription', async () => {
    const wrapper = await mountHost()
    await flushPromises()

    await expect(wrapper.vm.refreshWebPushStatus()).resolves.toBe('not-subscribed')
    expect(wrapper.vm.webPushState).toBe('not-subscribed')
    expect(getWebPushStatusMock).toHaveBeenCalledTimes(1)
  })

  it('subscribes current browser and sends payload to backend contract', async () => {
    const wrapper = await mountHost()
    await flushPromises()

    await wrapper.vm.subscribeCurrentBrowser()

    expect(subscribeMock).toHaveBeenCalledTimes(1)
    expect(subscribeWebPushMock).toHaveBeenCalledWith({
      endpoint: 'https://push.example/subscription-1',
      p256dh: 'test-p256dh',
      auth: 'test-auth',
      contentEncoding: 'aes128gcm',
      userAgent: navigator.userAgent
    })
    expect(wrapper.vm.webPushState).toBe('subscribed')
  })

  it('unsubscribes current browser and notifies backend with endpoint', async () => {
    currentSubscription = createPushSubscription('https://push.example/subscription-2')
    const wrapper = await mountHost()
    await flushPromises()

    await wrapper.vm.unsubscribeCurrentBrowser()

    expect(unsubscribeWebPushMock).toHaveBeenCalledWith({
      endpoint: 'https://push.example/subscription-2'
    })
    expect(unsubscribeMock).toHaveBeenCalledTimes(1)
    expect(wrapper.vm.webPushState).toBe('not-subscribed')
  })
})
