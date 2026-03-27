<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { VpnDefaultConnectionMode, VpnNetShieldMode, VpnProfileItem, VpnSettingsDraft } from '~/types/vpn'

const props = defineProps<{
  draft: VpnSettingsDraft
  profiles: VpnProfileItem[]
  saving?: boolean
}>()

const emit = defineEmits<{
  'update:draft': [draft: VpnSettingsDraft]
  save: []
}>()

const { t } = useI18n()

const defaultModeOptions = computed(() => ['FASTEST', 'RANDOM', 'LAST_CONNECTION', 'PROFILE'] as VpnDefaultConnectionMode[])
const netshieldOptions = computed(() => ['OFF', 'BLOCK_MALWARE', 'BLOCK_MALWARE_ADS_TRACKERS'] as VpnNetShieldMode[])

function patchDraft(patch: Partial<VpnSettingsDraft>): void {
  emit('update:draft', { ...props.draft, ...patch })
}

function onModeChange(value: VpnDefaultConnectionMode): void {
  patchDraft({
    defaultConnectionMode: value,
    defaultProfileId: value === 'PROFILE' ? props.draft.defaultProfileId : ''
  })
}
</script>

<template>
  <section class="mm-card vpn-panel vpn-privacy-panel">
    <div>
      <p class="panel-eyebrow">{{ t('vpn.panel.policy') }}</p>
      <h2>{{ t('vpn.panel.policy') }}</h2>
      <p class="panel-description">{{ t('vpn.policy.description') }}</p>
    </div>

    <div class="policy-grid">
      <label class="policy-field">
        <span>{{ t('vpn.policy.fields.defaultMode') }}</span>
        <el-select :model-value="props.draft.defaultConnectionMode" @update:model-value="onModeChange($event as VpnDefaultConnectionMode)">
          <el-option
            v-for="option in defaultModeOptions"
            :key="option"
            :label="t(`vpn.policy.mode.${option}`)"
            :value="option"
          />
        </el-select>
      </label>

      <label class="policy-field">
        <span>{{ t('vpn.policy.fields.defaultProfile') }}</span>
        <el-select
          :model-value="props.draft.defaultProfileId"
          :disabled="props.draft.defaultConnectionMode !== 'PROFILE'"
          clearable
          @update:model-value="patchDraft({ defaultProfileId: String($event || '') })"
        >
          <el-option v-for="profile in props.profiles" :key="profile.profileId" :label="profile.name" :value="profile.profileId" />
        </el-select>
      </label>

      <label class="policy-field">
        <span>{{ t('vpn.policy.fields.netshield') }}</span>
        <el-select :model-value="props.draft.netshieldMode" @update:model-value="patchDraft({ netshieldMode: $event as VpnNetShieldMode })">
          <el-option
            v-for="option in netshieldOptions"
            :key="option"
            :label="t(`vpn.netshield.${option}`)"
            :value="option"
          />
        </el-select>
      </label>

      <label class="policy-field policy-toggle">
        <span>{{ t('vpn.policy.fields.killSwitch') }}</span>
        <el-switch
          :model-value="props.draft.killSwitchEnabled"
          @update:model-value="patchDraft({ killSwitchEnabled: Boolean($event) })"
        />
      </label>
    </div>

    <div class="boundary">
      <strong>{{ t('vpn.policy.boundaryTitle') }}</strong>
      <p>{{ t('vpn.policy.boundaryBody') }}</p>
    </div>

    <div class="panel-actions">
      <el-button type="primary" :loading="props.saving" @click="emit('save')">
        {{ t('vpn.policy.actions.save') }}
      </el-button>
    </div>
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

h2,
.panel-description,
.boundary p {
  margin: 0;
}

.panel-description,
.boundary p {
  color: rgba(70, 81, 106, 0.78);
}

.policy-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.policy-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.policy-field span,
.boundary strong {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: rgba(55, 65, 81, 0.74);
}

.policy-toggle {
  justify-content: flex-end;
}

.boundary {
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(15, 23, 42, 0.04);
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.panel-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .policy-grid {
    grid-template-columns: 1fr;
  }
}
</style>
