import { computed, ref } from 'vue'
import { useSuiteApi } from '~/composables/useSuiteApi'
import type {
  SuiteWebPushStatus,
  SuiteWebPushSubscribeRequest,
  SuiteWebPushUnsubscribeRequest
} from '~/types/api'

export type PwaWebPushState =
  | 'idle'
  | 'loading'
  | 'subscribed'
  | 'not-subscribed'
  | 'unsupported'
  | 'disabled'
  | 'missing-vapid'
  | 'error'

interface SuiteWebPushApiPort {
  getWebPushStatus: () => Promise<SuiteWebPushStatus>
  subscribeWebPush: (payload: SuiteWebPushSubscribeRequest) => Promise<void>
  unsubscribeWebPush: (payload: SuiteWebPushUnsubscribeRequest) => Promise<void>
}

const DEFAULT_SW_SCOPE = '/sw.js'
const SUPPORTED_PUSH_ENCODING = 'aes128gcm'
const FALLBACK_PUSH_ENCODING = 'aesgcm'

const webPushState = ref<PwaWebPushState>('idle')
const webPushError = ref<string | null>(null)
const webPushEndpoint = ref<string | null>(null)
const webPushServerStatus = ref<SuiteWebPushStatus | null>(null)

function isClientRuntime(): boolean {
  return typeof window !== 'undefined' && typeof navigator !== 'undefined'
}

function supportsWebPush(): boolean {
  if (!isClientRuntime()) {
    return false
  }

  return 'PushManager' in window && 'serviceWorker' in navigator
}

function resolveApiPort(input?: SuiteWebPushApiPort): SuiteWebPushApiPort {
  if (input) {
    return input
  }

  return useSuiteApi()
}

async function resolveServiceWorkerRegistration(): Promise<ServiceWorkerRegistration> {
  if (!('serviceWorker' in navigator)) {
    throw new Error('Service Worker is not supported in this browser')
  }

  const scopeRegistration = await navigator.serviceWorker.getRegistration(DEFAULT_SW_SCOPE)
  if (scopeRegistration) {
    return scopeRegistration
  }

  if (!navigator.serviceWorker.ready) {
    throw new Error('Service Worker ready state is unavailable')
  }

  return navigator.serviceWorker.ready
}

function resolveContentEncoding(): string {
  const managerWithEncoding = PushManager as unknown as { supportedContentEncodings?: string[] }
  const supported = managerWithEncoding.supportedContentEncodings ?? []
  if (supported.includes(SUPPORTED_PUSH_ENCODING)) {
    return SUPPORTED_PUSH_ENCODING
  }

  return FALLBACK_PUSH_ENCODING
}

function base64ToArrayBuffer(base64Value: string): ArrayBuffer {
  const normalized = base64Value.replace(/-/g, '+').replace(/_/g, '/')
  const padding = '='.repeat((4 - (normalized.length % 4)) % 4)
  const binary = atob(normalized + padding)
  const bytes = new Uint8Array(new ArrayBuffer(binary.length))
  for (let index = 0; index < binary.length; index++) {
    bytes[index] = binary.charCodeAt(index)
  }
  return bytes.buffer as ArrayBuffer
}

function buildSubscribePayload(subscription: PushSubscription): SuiteWebPushSubscribeRequest {
  const subscriptionJson = subscription.toJSON()
  const p256dh = subscriptionJson.keys?.p256dh
  const auth = subscriptionJson.keys?.auth
  if (!p256dh || !auth) {
    throw new Error('Push subscription keys are missing')
  }

  return {
    endpoint: subscription.endpoint,
    p256dh,
    auth,
    contentEncoding: resolveContentEncoding(),
    userAgent: typeof navigator.userAgent === 'string' ? navigator.userAgent : undefined
  }
}

function setState(nextState: PwaWebPushState, error: string | null = null): void {
  webPushState.value = nextState
  webPushError.value = error
}

async function fetchServerStatus(apiPort: SuiteWebPushApiPort): Promise<SuiteWebPushStatus> {
  const status = await apiPort.getWebPushStatus()
  webPushServerStatus.value = status
  return status
}

async function refreshWebPushStatusInternal(apiPort: SuiteWebPushApiPort): Promise<PwaWebPushState> {
  if (!supportsWebPush()) {
    setState('unsupported')
    return webPushState.value
  }

  setState('loading')
  const status = await fetchServerStatus(apiPort)
  if (!status.enabled) {
    webPushEndpoint.value = null
    setState('disabled', status.message ?? null)
    return webPushState.value
  }

  if (!status.vapidPublicKey) {
    webPushEndpoint.value = null
    setState('missing-vapid', status.message ?? null)
    return webPushState.value
  }

  const registration = await resolveServiceWorkerRegistration()
  const subscription = await registration.pushManager.getSubscription()
  webPushEndpoint.value = subscription?.endpoint ?? null
  setState(subscription ? 'subscribed' : 'not-subscribed')
  return webPushState.value
}

async function subscribeCurrentBrowserInternal(apiPort: SuiteWebPushApiPort): Promise<void> {
  if (!supportsWebPush()) {
    throw new Error('Web Push is not supported in this browser')
  }

  setState('loading')
  const status = webPushServerStatus.value ?? await fetchServerStatus(apiPort)
  if (!status.enabled) {
    throw new Error(status.message || 'Web Push is disabled on the server')
  }
  if (!status.vapidPublicKey) {
    throw new Error(status.message || 'Missing VAPID public key')
  }

  const registration = await resolveServiceWorkerRegistration()
  const existing = await registration.pushManager.getSubscription()
  const subscription = existing ?? await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: base64ToArrayBuffer(status.vapidPublicKey)
  })
  await apiPort.subscribeWebPush(buildSubscribePayload(subscription))
  webPushEndpoint.value = subscription.endpoint
  setState('subscribed')
}

async function unsubscribeCurrentBrowserInternal(apiPort: SuiteWebPushApiPort): Promise<void> {
  if (!supportsWebPush()) {
    throw new Error('Web Push is not supported in this browser')
  }

  setState('loading')
  const registration = await resolveServiceWorkerRegistration()
  const subscription = await registration.pushManager.getSubscription()
  if (!subscription) {
    webPushEndpoint.value = null
    setState('not-subscribed')
    return
  }

  await apiPort.unsubscribeWebPush({ endpoint: subscription.endpoint })
  const unsubscribed = await subscription.unsubscribe()
  if (!unsubscribed) {
    throw new Error('Browser rejected push unsubscription')
  }

  webPushEndpoint.value = null
  setState('not-subscribed')
}

export function resetPwaWebPushStateForTests(): void {
  webPushState.value = 'idle'
  webPushError.value = null
  webPushEndpoint.value = null
  webPushServerStatus.value = null
}

export function usePwaWebPush(apiPort?: SuiteWebPushApiPort) {
  const resolvedApiPort = resolveApiPort(apiPort)

  async function refreshWebPushStatus(): Promise<PwaWebPushState> {
    try {
      return await refreshWebPushStatusInternal(resolvedApiPort)
    } catch (error) {
      setState('error', error instanceof Error ? error.message : 'Refresh web push status failed')
      throw error
    }
  }

  async function subscribeCurrentBrowser(): Promise<void> {
    try {
      await subscribeCurrentBrowserInternal(resolvedApiPort)
    } catch (error) {
      setState('error', error instanceof Error ? error.message : 'Subscribe web push failed')
      throw error
    }
  }

  async function unsubscribeCurrentBrowser(): Promise<void> {
    try {
      await unsubscribeCurrentBrowserInternal(resolvedApiPort)
    } catch (error) {
      setState('error', error instanceof Error ? error.message : 'Unsubscribe web push failed')
      throw error
    }
  }

  return {
    webPushState: computed(() => webPushState.value),
    webPushError: computed(() => webPushError.value),
    webPushEndpoint: computed(() => webPushEndpoint.value),
    webPushServerStatus: computed(() => webPushServerStatus.value),
    webPushSupported: computed(() => supportsWebPush()),
    refreshWebPushStatus,
    subscribeCurrentBrowser,
    unsubscribeCurrentBrowser
  }
}
