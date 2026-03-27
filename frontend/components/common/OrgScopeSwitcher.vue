<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { OrgAccessScope } from '~/types/org-access'

const props = defineProps<{
  modelValue: string
  scopes: OrgAccessScope[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const { t } = useI18n()

const options = computed(() => [
  {
    value: '',
    label: t('orgAccess.scope.personal')
  },
  ...props.scopes.map(scope => ({
    value: scope.orgId,
    label: t('orgAccess.scope.optionRole', {
      name: scope.orgName,
      role: t(`organizations.roles.${scope.role}`)
    })
  }))
])
</script>

<template>
  <div class="org-scope-switcher">
    <span class="org-scope-label">{{ t('orgAccess.scope.label') }}</span>
    <el-select
      :model-value="props.modelValue"
      class="org-scope-select"
      :placeholder="t('orgAccess.scope.placeholder')"
      @update:model-value="emit('update:modelValue', String($event || ''))"
    >
      <el-option
        v-for="option in options"
        :key="option.value || 'personal'"
        :label="option.label"
        :value="option.value"
      />
    </el-select>
  </div>
</template>

<style scoped>
.org-scope-switcher {
  display: flex;
  align-items: center;
  gap: 8px;
}

.org-scope-label {
  font-size: 12px;
  color: rgba(247, 251, 253, 0.84);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.org-scope-select {
  min-width: 228px;
}
</style>
