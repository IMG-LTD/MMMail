<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { VpnSessionItem } from '~/types/vpn'
import { formatVpnDuration, formatVpnTimestamp } from '~/utils/vpn'

const props = defineProps<{
  history: VpnSessionItem[]
}>()

const { t } = useI18n()
</script>

<template>
  <section class="mm-card vpn-panel vpn-history-panel">
    <div>
      <p class="panel-eyebrow">{{ t('vpn.panel.history') }}</p>
      <h2>{{ t('vpn.panel.history') }}</h2>
    </div>

    <el-table v-if="props.history.length > 0" :data="props.history" stripe>
      <el-table-column prop="serverId" :label="t('vpn.history.fields.server')" min-width="130" />
      <el-table-column :label="t('vpn.history.fields.location')" min-width="180">
        <template #default="{ row }">
          {{ row.serverCountry }} / {{ row.serverCity }}
        </template>
      </el-table-column>
      <el-table-column :label="t('vpn.history.fields.profile')" min-width="160">
        <template #default="{ row }">
          {{ row.profileName || '—' }}
        </template>
      </el-table-column>
      <el-table-column :label="t('vpn.history.fields.source')" min-width="140">
        <template #default="{ row }">
          {{ t(`vpn.source.${row.connectionSource}`) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('vpn.history.fields.netshield')" min-width="180">
        <template #default="{ row }">
          {{ t(`vpn.netshield.${row.netshieldMode}`) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('vpn.history.fields.killSwitch')" min-width="120">
        <template #default="{ row }">
          {{ row.killSwitchEnabled ? t('vpn.session.values.enabled') : t('vpn.session.values.disabled') }}
        </template>
      </el-table-column>
      <el-table-column :label="t('vpn.history.fields.connectedAt')" min-width="170">
        <template #default="{ row }">
          {{ formatVpnTimestamp(row.connectedAt) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('vpn.history.fields.disconnectedAt')" min-width="170">
        <template #default="{ row }">
          {{ formatVpnTimestamp(row.disconnectedAt) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('vpn.history.fields.duration')" min-width="120">
        <template #default="{ row }">
          {{ formatVpnDuration(row.durationSeconds) }}
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else :description="t('vpn.history.empty')" />
  </section>
</template>

<style scoped>
.vpn-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-eyebrow {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(83, 96, 123, 0.78);
}

h2 {
  margin: 0;
}
</style>
