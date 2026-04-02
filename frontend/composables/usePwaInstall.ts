import { computed, ref } from 'vue'

export type PwaInstallOutcome = 'accepted' | 'dismissed' | 'unavailable'
export type PwaNotificationPermissionState = NotificationPermission | 'unsupported'

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>
}

const installPromptEvent = ref<BeforeInstallPromptEvent | null>(null)
const serviceWorkerState = ref<'idle' | 'registered' | 'unsupported' | 'error'>('idle')
const notificationPermission = ref<PwaNotificationPermissionState>('unsupported')
const isInstalled = ref(false)
const isOnline = ref(true)
const registerError = ref<string | null>(null)

let bootstrapped = false

function isClientEnvironment(): boolean {
  return typeof window !== 'undefined' && typeof navigator !== 'undefined'
}

function isBeforeInstallPromptEvent(event: Event): event is BeforeInstallPromptEvent {
  return typeof (event as BeforeInstallPromptEvent).prompt === 'function'
    && typeof (event as BeforeInstallPromptEvent).userChoice?.then === 'function'
}

function readInstalledState(): boolean {
  if (!isClientEnvironment()) {
    return false
  }

  const standaloneNavigator = navigator as Navigator & { standalone?: boolean }
  return window.matchMedia('(display-mode: standalone)').matches || standaloneNavigator.standalone === true
}

function readNotificationPermission(): PwaNotificationPermissionState {
  if (!isClientEnvironment() || typeof Notification === 'undefined') {
    return 'unsupported'
  }

  return Notification.permission
}

function refreshRuntimeState(): void {
  if (!isClientEnvironment()) {
    return
  }

  isInstalled.value = readInstalledState()
  isOnline.value = navigator.onLine
  notificationPermission.value = readNotificationPermission()
}

async function registerServiceWorker(): Promise<void> {
  if (!isClientEnvironment() || !('serviceWorker' in navigator)) {
    serviceWorkerState.value = 'unsupported'
    return
  }

  try {
    await navigator.serviceWorker.register('/sw.js')
    serviceWorkerState.value = 'registered'
    registerError.value = null
  } catch (error) {
    serviceWorkerState.value = 'error'
    registerError.value = error instanceof Error ? error.message : 'Failed to register service worker'
  }
}

function handleBeforeInstallPrompt(event: Event): void {
  if (!isBeforeInstallPromptEvent(event)) {
    return
  }

  event.preventDefault()
  installPromptEvent.value = event
}

function handleAppInstalled(): void {
  installPromptEvent.value = null
  isInstalled.value = true
}

function handleConnectionChange(): void {
  refreshRuntimeState()
}

function removeWindowListeners(): void {
  if (!isClientEnvironment()) {
    return
  }

  window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt as EventListener)
  window.removeEventListener('appinstalled', handleAppInstalled)
  window.removeEventListener('online', handleConnectionChange)
  window.removeEventListener('offline', handleConnectionChange)
}

export function bootstrapPwaInstall(): void {
  if (bootstrapped || !isClientEnvironment()) {
    return
  }

  bootstrapped = true
  refreshRuntimeState()
  void registerServiceWorker()
  window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt as EventListener)
  window.addEventListener('appinstalled', handleAppInstalled)
  window.addEventListener('online', handleConnectionChange)
  window.addEventListener('offline', handleConnectionChange)
}

export async function installPwaApp(): Promise<PwaInstallOutcome> {
  const prompt = installPromptEvent.value
  if (!prompt) {
    return 'unavailable'
  }

  await prompt.prompt()
  const choice = await prompt.userChoice
  if (choice.outcome === 'accepted') {
    isInstalled.value = true
    installPromptEvent.value = null
  }

  return choice.outcome
}

export async function requestPwaNotificationPermission(): Promise<PwaNotificationPermissionState> {
  if (!isClientEnvironment() || typeof Notification === 'undefined') {
    notificationPermission.value = 'unsupported'
    return 'unsupported'
  }

  const permission = await Notification.requestPermission()
  notificationPermission.value = permission
  return permission
}

export function resetPwaInstallStateForTests(): void {
  removeWindowListeners()
  installPromptEvent.value = null
  serviceWorkerState.value = 'idle'
  notificationPermission.value = 'unsupported'
  isInstalled.value = false
  isOnline.value = true
  registerError.value = null
  bootstrapped = false
}

export function usePwaInstall() {
  bootstrapPwaInstall()

  return {
    installPromptAvailable: computed(() => installPromptEvent.value !== null),
    installSupported: computed(() => isClientEnvironment()),
    isInstalled: computed(() => isInstalled.value),
    isOnline: computed(() => isOnline.value),
    notificationsSupported: computed(() => notificationPermission.value !== 'unsupported'),
    notificationPermission: computed(() => notificationPermission.value),
    registerError: computed(() => registerError.value),
    serviceWorkerRegistered: computed(() => serviceWorkerState.value === 'registered'),
    serviceWorkerSupported: computed(() => serviceWorkerState.value !== 'unsupported'),
    installApp: installPwaApp,
    requestNotificationPermission: requestPwaNotificationPermission
  }
}
