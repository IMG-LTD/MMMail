<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type {
  VpnNetShieldMode,
  VpnProfileDraft,
  VpnProfileItem,
  VpnProfileRoutingMode,
  VpnProtocol
} from '~/types/vpn'
import { resolveVpnTargetSummary } from '~/utils/vpn'

const props = defineProps<{
  profiles: VpnProfileItem[]
  draft: VpnProfileDraft
  dialogVisible: boolean
  editingProfileId: string
  saving?: boolean
  quickConnectProfileId?: string
  deleteProfileId?: string
}>()

const emit = defineEmits<{
  'update:draft': [draft: VpnProfileDraft]
  create: []
  edit: [profileId: string]
  delete: [profileId: string]
  quickConnect: [profileId: string]
  close: []
  submit: []
}>()

const { t } = useI18n()

const routingOptions = computed(() => ['FASTEST', 'COUNTRY', 'SERVER'] as VpnProfileRoutingMode[])
const protocolOptions = computed(() => ['WIREGUARD', 'OPENVPN_UDP', 'OPENVPN_TCP'] as VpnProtocol[])
const netshieldOptions = computed(() => ['OFF', 'BLOCK_MALWARE', 'BLOCK_MALWARE_ADS_TRACKERS'] as VpnNetShieldMode[])
const dialogTitle = computed(() => props.editingProfileId ? t('vpn.profile.dialog.edit') : t('vpn.profile.dialog.create'))

function patchDraft(patch: Partial<VpnProfileDraft>): void {
  emit('update:draft', { ...props.draft, ...patch })
}

function onRoutingModeChange(value: VpnProfileRoutingMode): void {
  patchDraft({
    routingMode: value,
    targetServerId: value === 'SERVER' ? props.draft.targetServerId : '',
    targetCountry: value === 'COUNTRY' ? props.draft.targetCountry : ''
  })
}
</script>

<template>
  <section class="mm-card vpn-panel vpn-profiles-panel">
    <div class="panel-header">
      <div>
        <p class="panel-eyebrow">{{ t('vpn.panel.profiles') }}</p>
        <h2>{{ t('vpn.panel.profiles') }}</h2>
        <p class="panel-description">{{ t('vpn.profiles.description') }}</p>
      </div>
      <el-button type="primary" @click="emit('create')">
        {{ t('vpn.profiles.actions.create') }}
      </el-button>
    </div>

    <div v-if="props.profiles.length > 0" class="profile-grid">
      <article v-for="profile in props.profiles" :key="profile.profileId" class="profile-card">
        <div class="profile-head">
          <div>
            <h3>{{ profile.name }}</h3>
            <p>{{ resolveVpnTargetSummary(profile) }}</p>
          </div>
          <el-tag size="small" effect="plain">{{ profile.protocol }}</el-tag>
        </div>
        <div class="profile-pills">
          <span class="profile-pill">{{ t(`vpn.netshield.${profile.netshieldMode}`) }}</span>
          <span class="profile-pill">{{ profile.secureCoreEnabled ? 'Secure Core' : 'Standard' }}</span>
          <span class="profile-pill">
            {{ profile.killSwitchEnabled ? t('vpn.session.values.enabled') : t('vpn.session.values.disabled') }}
          </span>
        </div>
        <div class="profile-actions">
          <el-button text @click="emit('edit', profile.profileId)">{{ t('vpn.profiles.actions.edit') }}</el-button>
          <el-button
            type="primary"
            text
            :loading="props.quickConnectProfileId === profile.profileId"
            @click="emit('quickConnect', profile.profileId)"
          >
            {{ t('vpn.profiles.actions.quickConnect') }}
          </el-button>
          <el-button
            type="danger"
            text
            :loading="props.deleteProfileId === profile.profileId"
            @click="emit('delete', profile.profileId)"
          >
            {{ t('vpn.profiles.actions.delete') }}
          </el-button>
        </div>
      </article>
    </div>
    <el-empty v-else :description="t('vpn.profiles.empty')" />

    <el-dialog :model-value="props.dialogVisible" :title="dialogTitle" width="640px" @close="emit('close')">
      <div class="dialog-grid">
        <label class="dialog-field">
          <span>{{ t('vpn.profile.fields.name') }}</span>
          <el-input
            :model-value="props.draft.name"
            :placeholder="t('vpn.profile.placeholders.name')"
            @update:model-value="patchDraft({ name: String($event || '') })"
          />
        </label>

        <label class="dialog-field">
          <span>{{ t('vpn.profile.fields.protocol') }}</span>
          <el-select :model-value="props.draft.protocol" @update:model-value="patchDraft({ protocol: $event as VpnProtocol })">
            <el-option v-for="option in protocolOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </label>

        <label class="dialog-field">
          <span>{{ t('vpn.profile.fields.routingMode') }}</span>
          <el-select :model-value="props.draft.routingMode" @update:model-value="onRoutingModeChange($event as VpnProfileRoutingMode)">
            <el-option
              v-for="option in routingOptions"
              :key="option"
              :label="t(`vpn.profile.routing.${option}`)"
              :value="option"
            />
          </el-select>
        </label>

        <label v-if="props.draft.routingMode === 'SERVER'" class="dialog-field">
          <span>{{ t('vpn.profile.fields.targetServerId') }}</span>
          <el-input
            :model-value="props.draft.targetServerId"
            :placeholder="t('vpn.profile.placeholders.server')"
            @update:model-value="patchDraft({ targetServerId: String($event || '') })"
          />
        </label>

        <label v-if="props.draft.routingMode === 'COUNTRY'" class="dialog-field">
          <span>{{ t('vpn.profile.fields.targetCountry') }}</span>
          <el-input
            :model-value="props.draft.targetCountry"
            :placeholder="t('vpn.profile.placeholders.country')"
            @update:model-value="patchDraft({ targetCountry: String($event || '') })"
          />
        </label>

        <label class="dialog-field">
          <span>{{ t('vpn.profile.fields.netshield') }}</span>
          <el-select :model-value="props.draft.netshieldMode" @update:model-value="patchDraft({ netshieldMode: $event as VpnNetShieldMode })">
            <el-option
              v-for="option in netshieldOptions"
              :key="option"
              :label="t(`vpn.netshield.${option}`)"
              :value="option"
            />
          </el-select>
        </label>

        <label class="dialog-field switch-field">
          <span>{{ t('vpn.profile.fields.secureCore') }}</span>
          <el-switch
            :model-value="props.draft.secureCoreEnabled"
            @update:model-value="patchDraft({ secureCoreEnabled: Boolean($event) })"
          />
        </label>

        <label class="dialog-field switch-field">
          <span>{{ t('vpn.profile.fields.killSwitch') }}</span>
          <el-switch
            :model-value="props.draft.killSwitchEnabled"
            @update:model-value="patchDraft({ killSwitchEnabled: Boolean($event) })"
          />
        </label>
      </div>

      <template #footer>
        <div class="dialog-actions">
          <el-button @click="emit('close')">{{ t('vpn.profile.actions.cancel') }}</el-button>
          <el-button type="primary" :loading="props.saving" @click="emit('submit')">
            {{ t('vpn.profile.actions.save') }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.vpn-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-header,
.profile-head,
.profile-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.panel-eyebrow {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(83, 96, 123, 0.78);
}

.panel-description,
.profile-head p {
  margin: 0;
  color: rgba(70, 81, 106, 0.78);
}

.panel-header h2,
.profile-head h3 {
  margin: 0;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.profile-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(7, 15, 28, 0.02);
  border: 1px solid rgba(91, 107, 141, 0.14);
}

.profile-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.profile-pill {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  border: 1px solid rgba(37, 99, 235, 0.12);
  font-size: 12px;
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.dialog-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dialog-field span {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: rgba(55, 65, 81, 0.74);
}

.switch-field {
  justify-content: flex-end;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 900px) {
  .profile-grid,
  .dialog-grid {
    grid-template-columns: 1fr;
  }

  .panel-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .profile-actions {
    flex-wrap: wrap;
  }
}
</style>
