<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { usePwaInstall } from '~/composables/usePwaInstall'
import { usePwaWebPush } from '~/composables/usePwaWebPush'

const {
  installPromptAvailable,
  installSupported,
  isInstalled,
  isOnline,
  notificationsSupported,
  notificationPermission,
  registerError,
  serviceWorkerRegistered,
  serviceWorkerSupported,
  installApp,
  requestNotificationPermission
} = usePwaInstall()
const {
  webPushState,
  webPushError,
  webPushServerStatus,
  webPushSupported,
  refreshWebPushStatus,
  subscribeCurrentBrowser,
  unsubscribeCurrentBrowser
} = usePwaWebPush()
const { t } = useI18n()

const installStatusLabel = computed(() => {
  if (isInstalled.value) {
    return t('settings.pwa.status.installed')
  }

  if (installPromptAvailable.value) {
    return t('settings.pwa.status.installReady')
  }

  return t('settings.pwa.status.installUnavailable')
})

const serviceWorkerStatusLabel = computed(() => {
  if (!serviceWorkerSupported.value) {
    return t('settings.pwa.status.serviceWorkerUnsupported')
  }

  if (serviceWorkerRegistered.value) {
    return t('settings.pwa.status.serviceWorkerReady')
  }

  return t('settings.pwa.status.serviceWorkerPending')
})

const connectionStatusLabel = computed(() => (
  isOnline.value ? t('settings.pwa.status.online') : t('settings.pwa.status.offline')
))

const notificationStatusLabel = computed(() => {
  if (!notificationsSupported.value) {
    return t('settings.pwa.status.notificationsUnsupported')
  }

  if (notificationPermission.value === 'granted') {
    return t('settings.pwa.status.notificationsGranted')
  }

  if (notificationPermission.value === 'denied') {
    return t('settings.pwa.status.notificationsDenied')
  }

  return t('settings.pwa.status.notificationsDefault')
})

const webPushStatusLabel = computed(() => {
  if (!webPushSupported.value) {
    return t('settings.pwa.status.pushUnsupported')
  }

  if (webPushState.value === 'subscribed') {
    return t('settings.pwa.status.pushSubscribed')
  }

  if (webPushState.value === 'not-subscribed') {
    return t('settings.pwa.status.pushNotSubscribed')
  }

  if (webPushState.value === 'disabled') {
    return t('settings.pwa.status.pushDisabled')
  }

  if (webPushState.value === 'missing-vapid') {
    return t('settings.pwa.status.pushMissingVapid')
  }

  if (webPushState.value === 'loading') {
    return t('settings.pwa.status.pushLoading')
  }

  if (webPushState.value === 'error') {
    return t('settings.pwa.status.pushError')
  }

  return t('settings.pwa.status.pushIdle')
})

const pushHintLabel = computed(() => webPushServerStatus.value?.message || '')
const canInstall = computed(() => installSupported.value && installPromptAvailable.value && !isInstalled.value)
const canSubscribePush = computed(() => {
  if (!webPushSupported.value || webPushState.value === 'loading') {
    return false
  }

  if (webPushState.value === 'subscribed' || webPushState.value === 'disabled' || webPushState.value === 'missing-vapid') {
    return false
  }

  return notificationsSupported.value && notificationPermission.value !== 'denied'
})
const canUnsubscribePush = computed(() => webPushState.value === 'subscribed')
const panelErrors = computed(() => [registerError.value, webPushError.value].filter(Boolean))

async function onInstall(): Promise<void> {
  const outcome = await installApp()
  if (outcome === 'accepted') {
    ElMessage.success(t('settings.pwa.messages.installAccepted'))
    return
  }

  if (outcome === 'dismissed') {
    ElMessage.warning(t('settings.pwa.messages.installDismissed'))
    return
  }

  ElMessage.warning(t('settings.pwa.messages.installUnavailable'))
}

async function onSubscribePush(): Promise<void> {
  try {
    const permission = notificationPermission.value === 'granted'
      ? 'granted'
      : await requestNotificationPermission()

    if (permission === 'denied') {
      ElMessage.warning(t('settings.pwa.messages.notificationsDenied'))
      return
    }

    if (permission !== 'granted') {
      ElMessage.warning(t('settings.pwa.messages.notificationsUnavailable'))
      return
    }

    await subscribeCurrentBrowser()
    await refreshWebPushStatus()
    ElMessage.success(t('settings.pwa.messages.pushSubscribed'))
  } catch (error) {
    const fallbackMessage = t('settings.pwa.messages.pushSubscribeFailed')
    ElMessage.warning(error instanceof Error && error.message ? error.message : fallbackMessage)
  }
}

async function onUnsubscribePush(): Promise<void> {
  try {
    await unsubscribeCurrentBrowser()
    await refreshWebPushStatus()
    ElMessage.success(t('settings.pwa.messages.pushUnsubscribed'))
  } catch (error) {
    const fallbackMessage = t('settings.pwa.messages.pushUnsubscribeFailed')
    ElMessage.warning(error instanceof Error && error.message ? error.message : fallbackMessage)
  }
}

onMounted(() => {
  void refreshWebPushStatus().catch((error) => {
    const fallbackMessage = t('settings.pwa.messages.pushStatusFailed')
    ElMessage.warning(error instanceof Error && error.message ? error.message : fallbackMessage)
  })
})
</script>

<template>
  <section id="settings-pwa-panel" class="mm-card pwa-panel" data-testid="settings-pwa-panel">
    <div class="pwa-panel__copy">
      <span class="pwa-panel__eyebrow">{{ t('settings.pwa.eyebrow') }}</span>
      <h2 class="mm-section-title">{{ t('settings.pwa.title') }}</h2>
      <p>{{ t('settings.pwa.description') }}</p>
    </div>

    <div class="pwa-panel__grid">
      <div class="pwa-panel__metric" data-testid="settings-pwa-install-status">
        <span>{{ t('settings.pwa.labels.install') }}</span>
        <strong>{{ installStatusLabel }}</strong>
      </div>
      <div class="pwa-panel__metric" data-testid="settings-pwa-service-worker-status">
        <span>{{ t('settings.pwa.labels.serviceWorker') }}</span>
        <strong>{{ serviceWorkerStatusLabel }}</strong>
      </div>
      <div class="pwa-panel__metric" data-testid="settings-pwa-connection-status">
        <span>{{ t('settings.pwa.labels.connection') }}</span>
        <strong>{{ connectionStatusLabel }}</strong>
      </div>
      <div class="pwa-panel__metric" data-testid="settings-pwa-notification-status">
        <span>{{ t('settings.pwa.labels.notifications') }}</span>
        <strong>{{ notificationStatusLabel }}</strong>
      </div>
      <div class="pwa-panel__metric" data-testid="settings-pwa-push-status">
        <span>{{ t('settings.pwa.labels.push') }}</span>
        <strong>{{ webPushStatusLabel }}</strong>
      </div>
    </div>

    <div class="pwa-panel__actions">
      <el-button
        type="primary"
        data-testid="settings-pwa-install-button"
        :disabled="!canInstall"
        @click="onInstall"
      >
        {{ t('settings.pwa.actions.install') }}
      </el-button>
      <el-button
        data-testid="settings-pwa-subscribe-button"
        :disabled="!canSubscribePush"
        @click="onSubscribePush"
      >
        {{ t('settings.pwa.actions.subscribePush') }}
      </el-button>
      <el-button
        data-testid="settings-pwa-unsubscribe-button"
        :disabled="!canUnsubscribePush"
        @click="onUnsubscribePush"
      >
        {{ t('settings.pwa.actions.unsubscribePush') }}
      </el-button>
    </div>

    <p
      v-if="pushHintLabel"
      class="pwa-panel__hint"
      data-testid="settings-pwa-push-hint"
    >
      {{ pushHintLabel }}
    </p>

    <el-alert
      data-testid="settings-pwa-boundary"
      :closable="false"
      type="info"
      :title="t('settings.pwa.boundaryTitle')"
      :description="t('settings.pwa.boundaryDescription')"
    />

    <p v-for="(panelError, index) in panelErrors" :key="index" class="pwa-panel__error" data-testid="settings-pwa-error">
      {{ panelError }}
    </p>
  </section>
</template>

<style scoped>
.pwa-panel {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.pwa-panel__copy {
  display: grid;
  gap: 8px;
}

.pwa-panel__copy p {
  margin: 0;
  color: var(--mm-muted);
  line-height: 1.6;
}

.pwa-panel__eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--mm-accent, #0c5a5a);
}

.pwa-panel__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.pwa-panel__metric {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(12, 90, 90, 0.05);
  border: 1px solid rgba(12, 90, 90, 0.12);
}

.pwa-panel__metric span {
  font-size: 12px;
  color: var(--mm-muted);
}

.pwa-panel__metric strong {
  font-size: 15px;
}

.pwa-panel__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.pwa-panel__hint {
  margin: 0;
  color: var(--mm-muted);
  font-size: 13px;
}

.pwa-panel__error {
  margin: 0;
  color: #b42318;
  font-size: 13px;
}
</style>
