<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { SimpleLoginOverview } from '~/types/simplelogin'
import { buildSimpleLoginHealthChips } from '~/utils/simplelogin'

const props = defineProps<{
  overview: SimpleLoginOverview | null
  hasOrganizations: boolean
  canManageDomains: boolean
}>()
const { t } = useI18n()
const readinessNotes = computed(() => buildSimpleLoginHealthChips(props.overview, t))
</script>

<template>
  <aside class="knowledge-rail">
    <article class="rail-card">
      <p class="rail-eyebrow">{{ t('simplelogin.rail.scopeEyebrow') }}</p>
      <h3>{{ t('simplelogin.rail.scopeTitle') }}</h3>
      <ul>
        <li>{{ t('simplelogin.rail.scope.aliases') }}</li>
        <li>{{ t('simplelogin.rail.scope.reverseContacts') }}</li>
        <li>{{ t('simplelogin.rail.scope.customDomains') }}</li>
        <li>{{ t('simplelogin.rail.scope.relayControls') }}</li>
      </ul>
    </article>

    <article class="rail-card accent">
      <p class="rail-eyebrow">{{ t('simplelogin.rail.healthEyebrow') }}</p>
      <div class="chips">
        <span v-for="note in readinessNotes" :key="note" class="chip">{{ note }}</span>
      </div>
    </article>

    <article class="rail-card">
      <p class="rail-eyebrow">{{ t('simplelogin.rail.limitsEyebrow') }}</p>
      <ul>
        <li>{{ t('simplelogin.rail.limits.dns') }}</li>
        <li>{{ t('simplelogin.rail.limits.delivery') }}</li>
        <li>{{ t('simplelogin.rail.limits.clients') }}</li>
        <li v-if="!hasOrganizations">{{ t('simplelogin.rail.limits.noOrg') }}</li>
        <li v-else-if="!canManageDomains">{{ t('simplelogin.rail.limits.orgVisible') }}</li>
        <li v-else>{{ t('simplelogin.rail.limits.configurable') }}</li>
      </ul>
    </article>
  </aside>
</template>

<style scoped>
.knowledge-rail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rail-card {
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.08);
}

.rail-card.accent {
  background: linear-gradient(180deg, rgba(7, 18, 30, 0.96), rgba(17, 37, 59, 0.96));
  color: #f8fbff;
}

.rail-card h3,
.rail-card p,
.rail-card ul {
  margin: 0;
}

.rail-card ul {
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rail-eyebrow {
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 11px;
  color: #0f766e;
}

.accent .rail-eyebrow {
  color: rgba(173, 236, 255, 0.84);
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
  font-size: 12px;
}
</style>
