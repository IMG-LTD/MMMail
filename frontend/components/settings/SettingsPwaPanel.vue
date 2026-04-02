<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { usePwaInstall } from '~/composables/usePwaInstall'

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

const canInstall = computed(() => installSupported.value && installPromptAvailable.value && !isInstalled.value)
const canRequestNotifications = computed(() => notificationsSupported.value && notificationPermission.value === 'default')

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

async function onEnableNotifications(): Promise<void> {
  const permission = await requestNotificationPermission()
  if (permission === 'granted') {
    ElMessage.success(t('settings.pwa.messages.notificationsGranted'))
    return
  }

  if (permission === 'denied') {
    ElMessage.warning(t('settings.pwa.messages.notificationsDenied'))
    return
  }

  ElMessage.warning(t('settings.pwa.messages.notificationsUnavailable'))
}
</script>

<template>
  <section class="mm-card pwa-panel" data-testid="settings-pwa-panel">
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
        data-testid="settings-pwa-notification-button"
        :disabled="!canRequestNotifications"
        @click="onEnableNotifications"
      >
        {{ t('settings.pwa.actions.notifications') }}
      </el-button>
    </div>

    <el-alert
      data-testid="settings-pwa-boundary"
      :closable="false"
      type="info"
      :title="t('settings.pwa.boundaryTitle')"
      :description="t('settings.pwa.boundaryDescription')"
    />

    <p
      v-if="registerError"
      class="pwa-panel__error"
      data-testid="settings-pwa-register-error"
    >
      {{ registerError }}
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

.pwa-panel__error {
  margin: 0;
  color: #b42318;
  font-size: 13px;
}
</style>
