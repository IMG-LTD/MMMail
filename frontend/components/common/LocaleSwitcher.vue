<script setup lang="ts">
import { LOCALE_OPTIONS, type SupportedLocale } from '~/constants/i18n'
import { useI18n } from '~/composables/useI18n'
import { useLocalePreference } from '~/composables/useLocalePreference'

const props = withDefaults(defineProps<{
  size?: 'small' | 'default' | 'large'
}>(), {
  size: 'default'
})

const { locale, t } = useI18n()
const { syncing, applyLocaleSelection } = useLocalePreference()

async function onChange(value: SupportedLocale): Promise<void> {
  await applyLocaleSelection(value)
}
</script>

<template>
  <el-select
    :model-value="locale"
    :aria-label="t('topbar.localeAriaLabel')"
    :loading="syncing"
    :disabled="syncing"
    :size="props.size"
    class="locale-switcher"
    @update:model-value="onChange"
  >
    <el-option
      v-for="option in LOCALE_OPTIONS"
      :key="option.value"
      :label="option.label"
      :value="option.value"
    />
  </el-select>
</template>

<style scoped>
.locale-switcher {
  min-width: 132px;
}
</style>
