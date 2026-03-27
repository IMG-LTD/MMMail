<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { PassBusinessPolicyForm } from '~/types/pass-business'
import { formatPassTime } from '~/utils/pass'

const props = defineProps<{
  form: PassBusinessPolicyForm
  policyUpdatedAt: string | null
  disabled: boolean
  loading: boolean
}>()

const emit = defineEmits<{
  save: []
}>()

const { t } = useI18n()

const lengthMin = computed(() => Math.max(props.form.minimumPasswordLength, 8))
</script>

<template>
  <section class="rail-card policy-card">
    <header class="rail-head">
      <div>
        <strong>{{ t('pass.policies.title') }}</strong>
        <p class="policy-card__description">{{ t('pass.policies.description') }}</p>
      </div>
      <span>{{ formatPassTime(policyUpdatedAt) }}</span>
    </header>

    <el-form label-position="top" class="policy-form">
      <div class="policy-grid">
        <el-form-item :label="t('pass.policies.fields.minimumPasswordLength')">
          <el-input-number v-model="form.minimumPasswordLength" :min="8" :max="64" :disabled="disabled" />
        </el-form-item>
        <el-form-item :label="t('pass.policies.fields.maximumPasswordLength')">
          <el-input-number v-model="form.maximumPasswordLength" :min="lengthMin" :max="64" :disabled="disabled" />
        </el-form-item>
      </div>

      <div class="switch-grid">
        <el-switch v-model="form.requireUppercase" :disabled="disabled" inline-prompt :active-text="t('pass.policies.switch.on')" :inactive-text="t('pass.policies.switch.off')" />
        <span>{{ t('pass.policies.fields.requireUppercase') }}</span>

        <el-switch v-model="form.requireDigits" :disabled="disabled" inline-prompt :active-text="t('pass.policies.switch.on')" :inactive-text="t('pass.policies.switch.off')" />
        <span>{{ t('pass.policies.fields.requireDigits') }}</span>

        <el-switch v-model="form.requireSymbols" :disabled="disabled" inline-prompt :active-text="t('pass.policies.switch.on')" :inactive-text="t('pass.policies.switch.off')" />
        <span>{{ t('pass.policies.fields.requireSymbols') }}</span>

        <el-switch v-model="form.allowMemorablePasswords" :disabled="disabled" inline-prompt :active-text="t('pass.policies.switch.on')" :inactive-text="t('pass.policies.switch.off')" />
        <span>{{ t('pass.policies.fields.allowMemorablePasswords') }}</span>

        <el-switch v-model="form.allowExternalSharing" :disabled="disabled" inline-prompt :active-text="t('pass.policies.switch.on')" :inactive-text="t('pass.policies.switch.off')" />
        <span>{{ t('pass.policies.fields.allowExternalSharing') }}</span>

        <el-switch v-model="form.allowItemSharing" :disabled="disabled" inline-prompt :active-text="t('pass.policies.switch.on')" :inactive-text="t('pass.policies.switch.off')" />
        <span>{{ t('pass.policies.fields.allowItemSharing') }}</span>

        <el-switch v-model="form.allowSecureLinks" :disabled="disabled" inline-prompt :active-text="t('pass.policies.switch.on')" :inactive-text="t('pass.policies.switch.off')" />
        <span>{{ t('pass.policies.fields.allowSecureLinks') }}</span>

        <el-switch v-model="form.allowMemberVaultCreation" :disabled="disabled" inline-prompt :active-text="t('pass.policies.switch.on')" :inactive-text="t('pass.policies.switch.off')" />
        <span>{{ t('pass.policies.fields.allowMemberVaultCreation') }}</span>

        <el-switch v-model="form.allowExport" :disabled="disabled" inline-prompt :active-text="t('pass.policies.switch.on')" :inactive-text="t('pass.policies.switch.off')" />
        <span>{{ t('pass.policies.fields.allowExport') }}</span>

        <el-switch v-model="form.forceTwoFactor" :disabled="disabled" inline-prompt :active-text="t('pass.policies.switch.on')" :inactive-text="t('pass.policies.switch.off')" />
        <span>{{ t('pass.policies.fields.forceTwoFactor') }}</span>

        <el-switch v-model="form.allowPasskeys" :disabled="disabled" inline-prompt :active-text="t('pass.policies.switch.on')" :inactive-text="t('pass.policies.switch.off')" />
        <span>{{ t('pass.policies.fields.allowPasskeys') }}</span>

        <el-switch v-model="form.allowAliases" :disabled="disabled" inline-prompt :active-text="t('pass.policies.switch.on')" :inactive-text="t('pass.policies.switch.off')" />
        <span>{{ t('pass.policies.fields.allowAliases') }}</span>
      </div>

      <div class="policy-actions">
        <span class="policy-note">{{ disabled ? t('pass.policies.boundary.sharedOnly') : t('pass.policies.hint') }}</span>
        <el-button type="primary" :disabled="disabled" :loading="loading" @click="emit('save')">
          {{ t('pass.policies.actions.save') }}
        </el-button>
      </div>
    </el-form>
  </section>
</template>

<style scoped>
.policy-card,
.policy-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.policy-card__description,
.policy-note {
  margin: 6px 0 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.6;
}

.policy-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.switch-grid {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px 12px;
  align-items: center;
}

.switch-grid span {
  color: #344054;
  font-size: 13px;
}

.policy-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 1080px) {
  .policy-grid,
  .policy-actions {
    grid-template-columns: 1fr;
  }

  .policy-actions {
    display: grid;
  }
}
</style>
