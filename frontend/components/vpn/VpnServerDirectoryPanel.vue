<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { VpnProtocol, VpnServerItem } from '~/types/vpn'
import { resolveVpnLoadTag } from '~/utils/vpn'

const props = defineProps<{
  servers: VpnServerItem[]
  selectedProtocol: VpnProtocol
  connectingServerId?: string
}>()

const emit = defineEmits<{
  'update:selectedProtocol': [value: VpnProtocol]
  connect: [serverId: string]
}>()

const { t } = useI18n()

const protocolOptions = computed(() => [
  { value: 'WIREGUARD', label: 'WireGuard' },
  { value: 'OPENVPN_UDP', label: 'OpenVPN UDP' },
  { value: 'OPENVPN_TCP', label: 'OpenVPN TCP' }
])
</script>

<template>
  <section class="mm-card vpn-panel vpn-directory-panel">
    <div class="panel-header">
      <div>
        <p class="panel-eyebrow">{{ t('vpn.panel.directory') }}</p>
        <h2>{{ t('vpn.panel.directory') }}</h2>
      </div>
      <div class="toolbar">
        <span>{{ t('vpn.directory.fields.protocol') }}</span>
        <el-select
          :model-value="props.selectedProtocol"
          class="protocol-select"
          @update:model-value="emit('update:selectedProtocol', $event as VpnProtocol)"
        >
          <el-option
            v-for="option in protocolOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </div>
    </div>

    <p class="panel-description">{{ t('vpn.directory.description') }}</p>

    <div v-if="props.servers.length > 0" class="server-grid">
      <article v-for="server in props.servers" :key="server.serverId" class="server-card">
        <div class="server-head">
          <div>
            <h3>{{ server.serverId }}</h3>
            <p>{{ t('vpn.directory.fields.location', { country: server.country, city: server.city }) }}</p>
          </div>
          <el-tag :type="server.status === 'ONLINE' ? 'success' : 'info'">{{ server.status }}</el-tag>
        </div>
        <div class="server-meta">
          <el-tag size="small" effect="plain">{{ t('vpn.directory.fields.tier', { value: server.tier }) }}</el-tag>
          <el-tag size="small" :type="resolveVpnLoadTag(server.loadPercent)">
            {{ t('vpn.directory.fields.load', { value: server.loadPercent }) }}
          </el-tag>
        </div>
        <el-button
          type="primary"
          :disabled="server.status !== 'ONLINE'"
          :loading="props.connectingServerId === server.serverId"
          @click="emit('connect', server.serverId)"
        >
          {{ t('vpn.directory.actions.connect') }}
        </el-button>
      </article>
    </div>
    <el-empty v-else :description="t('vpn.directory.empty')" />
  </section>
</template>

<style scoped>
.vpn-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.panel-eyebrow {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(83, 96, 123, 0.78);
}

.panel-header h2,
.server-head h3 {
  margin: 0;
}

.panel-description,
.server-head p {
  margin: 0;
  color: rgba(70, 81, 106, 0.78);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.protocol-select {
  width: 180px;
}

.server-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.server-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(91, 107, 141, 0.14);
  background:
    linear-gradient(180deg, rgba(248, 250, 255, 0.96), rgba(236, 241, 250, 0.96)),
    linear-gradient(135deg, rgba(15, 23, 42, 0.04), rgba(59, 130, 246, 0.03));
}

.server-head,
.server-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.server-meta {
  justify-content: flex-start;
  flex-wrap: wrap;
}

@media (max-width: 900px) {
  .panel-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .server-grid {
    grid-template-columns: 1fr;
  }

  .toolbar {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
  }

  .protocol-select {
    width: 100%;
  }
}
</style>
