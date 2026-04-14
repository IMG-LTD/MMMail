<script setup lang="ts">
import SuiteReleaseBoundaryPanel from '~/components/suite/SuiteReleaseBoundaryPanel.vue'
import { useI18n } from '~/composables/useI18n'

definePageMeta({ layout: 'blank' })

const { t } = useI18n()

useHead(() => ({ title: t('community.boundary.title') }))
</script>

<template>
  <div class="boundary-page">
    <SuiteReleaseBoundaryPanel />
  </div>
</template>

<style scoped>
.boundary-page {
  width: min(1120px, 100%);
  margin: 0 auto;
}
</style>
