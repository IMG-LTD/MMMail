<template>
  <section class="suite-section-stack" data-testid="suite-section-boundary">
    <SuiteReleaseBoundaryPanel />
  </section>
</template>

<script setup lang="ts">
import SuiteReleaseBoundaryPanel from '~/components/suite/SuiteReleaseBoundaryPanel.vue'
</script>

<style scoped>
.suite-section-stack {
  display: grid;
  gap: 16px;
}
</style>
