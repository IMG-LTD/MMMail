<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { SuiteSubscription } from '~/types/suite-lumo'
import type { SuitePlanUsageRow, SuiteUpgradeSummary } from '~/utils/suite-plans'
import { formatUsageValue, usagePercent } from '~/utils/suite-plans'

interface Props {
  subscription: SuiteSubscription | null
  usageRows: SuitePlanUsageRow[]
  showDriveEntityUsage: boolean
  upgradeSummary: SuiteUpgradeSummary
  resolveStatusLabel: (status: string) => string
}

const props = defineProps<Props>()
const { t } = useI18n()

const upgradePathLabel = computed(() => {
  if (props.upgradeSummary.availableUpgradeNames.length === 0) {
    return t('suite.hero.upgradeNone')
  }
  return t('suite.hero.upgradePaths', {
    plans: props.upgradeSummary.availableUpgradeNames.join(' / ')
  })
})

const currentPlanMeta = computed(() => {
  if (!props.subscription) {
    return null
  }
  return {
    segmentLabel: t(`suite.plans.segment.${props.subscription.plan.segment}.title`),
    priceLabel: resolvePriceLabel(
      props.subscription.plan.priceMode,
      props.subscription.plan.priceValue
    )
  }
})

function resolvePriceLabel(priceMode: string, priceValue: string | null): string {
  if (priceMode === 'FREE') {
    return t('suite.plans.price.free')
  }
  if (priceMode === 'FROM' && priceValue) {
    return t('suite.plans.price.from', { value: priceValue })
  }
  if (priceMode === 'PER_USER') {
    return t('suite.plans.price.perUser')
  }
  if (priceMode === 'CONTACT_SALES') {
    return t('suite.plans.price.contactSales')
  }
  return t('suite.plans.price.addOn')
}
</script>

<template>
  <section class="mm-card suite-hero">
    <div class="suite-hero-copy">
      <span class="suite-hero-badge">{{ t('suite.hero.badge') }}</span>
      <h1 class="suite-hero-title">{{ t('page.suite.title') }}</h1>
      <p class="suite-hero-subtitle">{{ t('suite.hero.subtitle') }}</p>
    </div>

    <div class="suite-hero-grid">
      <article v-if="props.subscription" class="hero-card">
        <h2 class="mm-section-subtitle">{{ t('suite.hero.currentPlan') }}</h2>
        <div class="hero-plan-name">
          <strong>{{ props.subscription.planName }}</strong>
          <el-tag type="primary">{{ props.subscription.planCode }}</el-tag>
          <el-tag effect="plain">{{ currentPlanMeta?.segmentLabel }}</el-tag>
          <el-tag effect="plain" type="warning">{{ currentPlanMeta?.priceLabel }}</el-tag>
        </div>
        <div class="hero-meta">
          <span>{{ t('suite.hero.status') }}: {{ props.resolveStatusLabel(props.subscription.status) }}</span>
          <span>{{ t('suite.hero.updatedAt') }}: {{ props.subscription.updatedAt }}</span>
        </div>
      </article>

      <article class="hero-card">
        <h2 class="mm-section-subtitle">{{ t('suite.hero.usageOverview') }}</h2>
        <div v-if="props.usageRows.length > 0" class="usage-list">
          <div v-for="row in props.usageRows" :key="row.key" class="usage-item">
            <div class="usage-meta">
              <span>{{ t(`suite.plans.usage.${row.key}`) }}</span>
              <span>{{ formatUsageValue(row.count, row.unit) }} / {{ formatUsageValue(row.limit, row.unit) }}</span>
            </div>
            <el-progress :percentage="usagePercent(row.count, row.limit)" :stroke-width="12" />
          </div>
        </div>
        <p v-else class="mm-muted">{{ t('suite.hero.noQuotaMetrics') }}</p>
        <div v-if="props.showDriveEntityUsage && props.subscription" class="hero-meta">
          <span>{{ t('suite.hero.driveFiles') }}: {{ props.subscription.usage.driveFileCount }}</span>
          <span>{{ t('suite.hero.driveFolders') }}: {{ props.subscription.usage.driveFolderCount }}</span>
        </div>
      </article>

      <article class="hero-card upgrade-card">
        <h2 class="mm-section-subtitle">{{ t('suite.hero.upgradeTitle') }}</h2>
        <p class="mm-muted">{{ t('suite.hero.upgradeDescription') }}</p>
        <div class="coverage">
          {{ t('suite.hero.upgradeCoverage', {
            enabled: props.upgradeSummary.enabledProductCount,
            total: props.upgradeSummary.totalProductCount
          }) }}
        </div>
        <p class="upgrade-paths">{{ upgradePathLabel }}</p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.suite-hero {
  padding: 24px;
  border: 1px solid rgba(68, 114, 255, 0.18);
  background:
    radial-gradient(circle at top right, rgba(83, 115, 255, 0.16), transparent 32%),
    radial-gradient(circle at bottom left, rgba(65, 214, 255, 0.12), transparent 34%),
    linear-gradient(145deg, rgba(246, 248, 255, 0.98), rgba(255, 255, 255, 0.95));
  box-shadow: 0 24px 60px rgba(34, 52, 102, 0.1);
}

.suite-hero-copy {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.suite-hero-badge {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #4b5bdd;
}

.suite-hero-title {
  margin: 0;
  font-size: clamp(2rem, 3vw, 2.8rem);
  line-height: 1.08;
  color: #18233f;
}

.suite-hero-subtitle {
  margin: 0;
  max-width: 66ch;
  line-height: 1.65;
  color: #5b6680;
}

.suite-hero-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.hero-card {
  border-radius: 18px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(88, 104, 160, 0.12);
}

.hero-plan-name {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.hero-meta {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  font-size: 13px;
  color: #64708a;
}

.usage-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.usage-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
  font-size: 13px;
}

.coverage {
  margin-top: 14px;
  font-size: 24px;
  font-weight: 700;
  color: #202a47;
}

.upgrade-paths {
  margin: 10px 0 0;
  color: #4e5b78;
  line-height: 1.6;
}

@media (max-width: 1120px) {
  .suite-hero-grid {
    grid-template-columns: 1fr;
  }
}
</style>
