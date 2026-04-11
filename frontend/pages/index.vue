<script setup lang="ts">
import { computed } from 'vue'
import MarketingHero from '~/components/marketing/MarketingHero.vue'
import MarketingMainlineFlow from '~/components/marketing/MarketingMainlineFlow.vue'
import MarketingScreenshots from '~/components/marketing/MarketingScreenshots.vue'
import MarketingTrustGrid from '~/components/marketing/MarketingTrustGrid.vue'
import { useI18n } from '~/composables/useI18n'
import { useAuthStore } from '~/stores/auth'
import { useOrgAccessStore } from '~/stores/org-access'
import { resolveHomeRoute } from '~/utils/org-product-access'

const authStore = useAuthStore()
const orgAccessStore = useOrgAccessStore()
const { t } = useI18n()

useHead(() => ({ title: t('marketing.hero.title') }))

if (authStore.isAuthenticated) {
  await orgAccessStore.ensureLoaded()
  await navigateTo(resolveHomeRoute(orgAccessStore.isProductEnabled, authStore.user?.mailAddressMode))
}

const trustCards = computed(() => [
  { title: t('marketing.trust.deploy.title'), description: t('marketing.trust.deploy.description'), href: '/self-hosted/install.html' },
  { title: t('marketing.trust.boundary.title'), description: t('marketing.trust.boundary.description'), href: '/suite?section=boundary' },
  { title: t('marketing.trust.ops.title'), description: t('marketing.trust.ops.description'), href: '/self-hosted/runbook.html' },
])

const flowItems = computed(() => [
  { name: 'Mail', description: t('marketing.mainline.mail') },
  { name: 'Calendar', description: t('marketing.mainline.calendar') },
  { name: 'Drive', description: t('marketing.mainline.drive') },
  { name: 'Pass', description: t('marketing.mainline.pass') },
])

const screenshotCards = computed(() => [
  { title: t('marketing.shots.mail.title'), caption: t('marketing.shots.mail.caption') },
  { title: t('marketing.shots.suite.title'), caption: t('marketing.shots.suite.caption') },
  { title: t('marketing.shots.health.title'), caption: t('marketing.shots.health.caption') },
])
</script>

<template>
  <div class="marketing-page">
    <MarketingHero
      :title="t('marketing.hero.title')"
      :subtitle="t('marketing.hero.subtitle')"
      :primary-label="t('marketing.hero.primary')"
      :secondary-label="t('marketing.hero.secondary')"
    />
    <MarketingTrustGrid :cards="trustCards" />
    <MarketingMainlineFlow :items="flowItems" />
    <MarketingScreenshots :cards="screenshotCards" />
  </div>
</template>
