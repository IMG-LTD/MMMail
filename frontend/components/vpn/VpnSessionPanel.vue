<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { VpnSessionItem } from '~/types/vpn'
import { formatVpnDuration, formatVpnTimestamp } from '~/utils/vpn'

const props = defineProps<{
  currentSession: VpnSessionItem | null
  liveDurationSeconds: number
  disconnecting?: boolean
}>()

const emit = defineEmits<{
  disconnect: []
}>()

const { t } = useI18n()
</script>

<template>
  <section class="mm-card vpn-panel vpn-session-panel">
    <div class="panel-header">
      <div>
        <p class="panel-eyebrow">{{ t('vpn.panel.session') }}</p>
        <h2>{{ t('vpn.panel.session') }}</h2>
      </div>
      <el-button
        type="danger"
        :disabled="!props.currentSession || props.currentSession.status !== 'CONNECTED'"
        :loading="props.disconnecting"
        @click="emit('disconnect')"
      >
        {{ t('vpn.session.actions.disconnect') }}
      </el-button>
    </div>

    <div v-if="props.currentSession" class="session-grid">
      <article class="session-card">
        <span>{{ t('vpn.session.fields.status') }}</span>
        <strong>{{ props.currentSession.status }}</strong>
      </article>
      <article class="session-card">
        <span>{{ t('vpn.session.fields.server') }}</span>
        <strong>{{ props.currentSession.serverId }}</strong>
      </article>
      <article class="session-card">
        <span>{{ t('vpn.session.fields.location') }}</span>
        <strong>{{ props.currentSession.serverCountry }} / {{ props.currentSession.serverCity }}</strong>
      </article>
      <article class="session-card">
        <span>{{ t('vpn.session.fields.profile') }}</span>
        <strong>{{ props.currentSession.profileName || '—' }}</strong>
      </article>
      <article class="session-card">
        <span>{{ t('vpn.session.fields.protocol') }}</span>
        <strong>{{ props.currentSession.protocol }}</strong>
      </article>
      <article class="session-card">
        <span>{{ t('vpn.session.fields.source') }}</span>
        <strong>{{ t(`vpn.source.${props.currentSession.connectionSource}`) }}</strong>
      </article>
      <article class="session-card">
        <span>{{ t('vpn.session.fields.netshield') }}</span>
        <strong>{{ t(`vpn.netshield.${props.currentSession.netshieldMode}`) }}</strong>
      </article>
      <article class="session-card">
        <span>{{ t('vpn.session.fields.killSwitch') }}</span>
        <strong>{{ props.currentSession.killSwitchEnabled ? t('vpn.session.values.enabled') : t('vpn.session.values.disabled') }}</strong>
      </article>
      <article class="session-card">
        <span>{{ t('vpn.session.fields.connectedAt') }}</span>
        <strong>{{ formatVpnTimestamp(props.currentSession.connectedAt) }}</strong>
      </article>
      <article class="session-card">
        <span>{{ t('vpn.session.fields.duration') }}</span>
        <strong>{{ formatVpnDuration(props.liveDurationSeconds) }}</strong>
      </article>
    </div>
    <el-empty v-else :description="t('vpn.session.empty')" />
  </section>
</template>

<style scoped>
.vpn-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.vpn-session-panel {
  min-height: 100%;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.panel-eyebrow {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(83, 96, 123, 0.78);
}

.panel-header h2 {
  margin: 0;
  font-size: 22px;
}

.session-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.session-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(11, 18, 32, 0.96), rgba(20, 31, 54, 0.92));
  color: #eff6ff;
  border: 1px solid rgba(95, 115, 162, 0.22);
}

.session-card span {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: rgba(180, 195, 224, 0.72);
}

.session-card strong {
  font-size: 16px;
}

@media (max-width: 900px) {
  .session-grid {
    grid-template-columns: 1fr;
  }
}
</style>
