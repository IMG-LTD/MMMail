<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { SuiteBillingOverview, SuiteOfferCode } from '~/types/suite-lumo'

interface Props {
  overview: SuiteBillingOverview | null
  selectedOfferCode: SuiteOfferCode | null
  restoreLatestDraft: () => Promise<void>
}

const props = defineProps<Props>()
const { t } = useI18n()
</script>

<template>
  <section class="mm-card billing-overview-panel">
    <div class="overview-head">
      <div>
        <p class="eyebrow">{{ t('suite.billing.overview.badge') }}</p>
        <h2 class="mm-section-title">{{ t('suite.billing.overview.title') }}</h2>
        <p class="mm-muted">{{ t('suite.billing.overview.subtitle') }}</p>
      </div>
      <el-button
        v-if="props.overview?.latestDraft"
        plain
        @click="void props.restoreLatestDraft()"
      >
        {{ t('suite.billing.overview.restoreDraft') }}
      </el-button>
    </div>

    <div class="overview-grid">
      <article class="metric-card">
        <span class="metric-label">{{ t('suite.billing.overview.activePlan') }}</span>
        <strong class="metric-value">{{ props.overview?.activePlanName || '—' }}</strong>
        <span class="metric-meta">{{ props.overview?.activePlanCode || '—' }}</span>
      </article>
      <article class="metric-card">
        <span class="metric-label">{{ t('suite.billing.overview.selfServeCount') }}</span>
        <strong class="metric-value">{{ props.overview?.selfServeOfferCodes.length ?? 0 }}</strong>
        <span class="metric-meta">{{ t('suite.billing.checkoutMode.SELF_SERVE') }}</span>
      </article>
      <article class="metric-card">
        <span class="metric-label">{{ t('suite.billing.overview.contactSalesCount') }}</span>
        <strong class="metric-value">{{ props.overview?.contactSalesOfferCodes.length ?? 0 }}</strong>
        <span class="metric-meta">{{ t('suite.billing.checkoutMode.CONTACT_SALES') }}</span>
      </article>
    </div>

    <div class="draft-card">
      <template v-if="props.overview?.latestDraft">
        <div class="draft-head">
          <div>
            <h3 class="draft-title">{{ t('suite.billing.overview.latestDraft') }}</h3>
            <p class="mm-muted">
              {{ props.overview.latestDraft.offerName }} · {{ props.overview.latestDraft.billingCycle }}
            </p>
          </div>
          <div class="draft-tags">
            <el-tag effect="plain">{{ props.overview.latestDraft.offerCode }}</el-tag>
            <el-tag
              :type="props.selectedOfferCode === props.overview.latestDraft.offerCode ? 'success' : 'info'"
              effect="plain"
            >
              {{ props.selectedOfferCode === props.overview.latestDraft.offerCode
                ? t('suite.billing.overview.currentWorkspace')
                : t('suite.billing.overview.savedSnapshot') }}
            </el-tag>
          </div>
        </div>
        <dl class="draft-meta">
          <div>
            <dt>{{ t('suite.billing.overview.seatCount') }}</dt>
            <dd>{{ props.overview.latestDraft.seatCount }}</dd>
          </div>
          <div>
            <dt>{{ t('suite.billing.overview.quoteStatus') }}</dt>
            <dd>{{ t(`suite.billing.quoteStatus.${props.overview.latestDraft.quoteStatus}`) }}</dd>
          </div>
          <div>
            <dt>{{ t('suite.billing.overview.updatedAt') }}</dt>
            <dd>{{ props.overview.latestDraft.updatedAt }}</dd>
          </div>
        </dl>
      </template>
      <el-empty
        v-else
        :description="t('suite.billing.overview.emptyDraft')"
        :image-size="70"
      />
    </div>
  </section>
</template>

<style scoped>
.billing-overview-panel {
  --suite-billing-accent: #6d78ff;
  --suite-billing-ink: #11203d;
  --suite-billing-muted: #66738f;
  --suite-billing-border: rgba(98, 114, 188, 0.14);
  padding: 24px;
  border: 1px solid var(--suite-billing-border);
  background:
    radial-gradient(circle at top left, rgba(109, 120, 255, 0.16), transparent 32%),
    linear-gradient(180deg, rgba(247, 249, 255, 0.98), rgba(255, 255, 255, 0.97));
}

.overview-head,
.draft-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 6px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 12px;
  color: var(--suite-billing-accent);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.metric-card,
.draft-card {
  border-radius: 20px;
  border: 1px solid var(--suite-billing-border);
  background: rgba(255, 255, 255, 0.86);
}

.metric-card {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.metric-label,
.draft-meta dt {
  color: var(--suite-billing-muted);
  font-size: 13px;
}

.metric-value {
  font-size: 28px;
  color: var(--suite-billing-ink);
}

.metric-meta {
  color: var(--suite-billing-accent);
  font-weight: 600;
}

.draft-card {
  margin-top: 16px;
  padding: 18px;
}

.draft-title {
  margin: 0;
  color: var(--suite-billing-ink);
}

.draft-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.draft-meta {
  margin: 16px 0 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.draft-meta dd {
  margin: 4px 0 0;
  color: var(--suite-billing-ink);
  font-weight: 600;
}

@media (max-width: 960px) {
  .overview-grid,
  .draft-meta {
    grid-template-columns: 1fr;
  }
}
</style>
