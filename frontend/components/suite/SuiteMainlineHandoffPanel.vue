<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { MainlineCollaborationEvent } from '~/utils/collaboration'
import type { MainlineHandoffRun, MainlineHandoffStageStatus } from '~/utils/mainline-handoff'

const props = defineProps<{
  run: MainlineHandoffRun
  loading: boolean
}>()

const { t } = useI18n()

const summaryStatus = computed(() => {
  if (props.run.completed) {
    return 'completed'
  }
  if (props.run.started) {
    return 'active'
  }
  return 'idle'
})

const currentStageLabel = computed(() => translateProduct(props.run.currentStage))
const nextStageLabel = computed(() => translateProduct(props.run.nextStage, true))
const latestEvidenceLabel = computed(() => formatEvidence(props.run.latestEvent))
const anchorEvidenceLabel = computed(() => formatEvidence(props.run.anchorEvent))

const summaryText = computed(() => {
  if (summaryStatus.value === 'completed' && props.run.currentStage) {
    return t('suite.sectionOverview.handoff.summary.completed', {
      product: currentStageLabel.value
    })
  }
  if (summaryStatus.value === 'active' && props.run.currentStage) {
    return t('suite.sectionOverview.handoff.summary.active', {
      product: currentStageLabel.value
    })
  }
  return t('suite.sectionOverview.handoff.summary.idle')
})

function translateProduct(productCode: string | null, allowEmpty = false): string {
  if (!productCode) {
    return allowEmpty
      ? t('suite.sectionOverview.handoff.noneNext')
      : t('suite.sectionOverview.handoff.noneCurrent')
  }
  return t(`organizations.products.${productCode}`)
}

function statusLabel(status: MainlineHandoffStageStatus): string {
  return t(`suite.sectionOverview.handoff.status.${status.toLowerCase()}`)
}

function formatEvidence(item: MainlineCollaborationEvent | null): string {
  if (!item) {
    return t('suite.sectionOverview.handoff.noEvidence')
  }
  return `${item.title} · ${formatTime(item.createdAt)}`
}

function formatTime(value: string): string {
  if (!value) {
    return '—'
  }
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<template>
  <section
    class="handoff-panel mm-card"
    data-testid="suite-mainline-handoff-panel"
    :data-status="summaryStatus"
  >
    <div class="handoff-panel__copy">
      <span class="handoff-panel__eyebrow">{{ t('suite.sectionOverview.handoff.eyebrow') }}</span>
      <h2 class="mm-section-title">{{ t('suite.sectionOverview.handoff.title') }}</h2>
      <p class="mm-muted">{{ t('suite.sectionOverview.handoff.description') }}</p>
    </div>

    <div class="handoff-panel__hero">
      <div class="handoff-panel__summary">
        <span class="handoff-panel__summary-pill" :data-status="summaryStatus">
          {{ t(`suite.sectionOverview.handoff.summaryState.${summaryStatus}`) }}
        </span>
        <h3 class="handoff-panel__summary-title" data-testid="suite-mainline-handoff-summary">
          {{ summaryText }}
        </h3>
        <p class="handoff-panel__summary-copy">
          {{ t('suite.sectionOverview.handoff.summaryDescription') }}
        </p>
      </div>

      <dl class="handoff-panel__metrics">
        <div>
          <dt>{{ t('suite.sectionOverview.handoff.currentLabel') }}</dt>
          <dd>{{ currentStageLabel }}</dd>
        </div>
        <div>
          <dt>{{ t('suite.sectionOverview.handoff.nextLabel') }}</dt>
          <dd>{{ nextStageLabel }}</dd>
        </div>
        <div>
          <dt>{{ t('suite.sectionOverview.handoff.evidenceLabel') }}</dt>
          <dd>{{ latestEvidenceLabel }}</dd>
        </div>
        <div>
          <dt>{{ t('suite.sectionOverview.handoff.startedLabel') }}</dt>
          <dd>{{ anchorEvidenceLabel }}</dd>
        </div>
      </dl>
    </div>

    <div
      v-if="loading"
      class="handoff-panel__loading"
      data-testid="suite-mainline-handoff-loading"
    >
      {{ t('suite.sectionOverview.handoff.loading') }}
    </div>

    <div v-else class="handoff-panel__grid">
      <article
        v-for="stage in run.stages"
        :key="stage.productCode"
        class="handoff-panel__card"
        :data-testid="`suite-mainline-handoff-stage-${stage.productCode.toLowerCase()}`"
        :data-status="stage.status.toLowerCase()"
      >
        <div class="handoff-panel__card-head">
          <span class="handoff-panel__product">{{ t(`organizations.products.${stage.productCode}`) }}</span>
          <span class="handoff-panel__badge" :data-status="stage.status.toLowerCase()">
            {{ statusLabel(stage.status) }}
          </span>
        </div>
        <h3 class="handoff-panel__title">{{ t(stage.titleKey) }}</h3>
        <p class="handoff-panel__description">{{ t(stage.descriptionKey) }}</p>
        <p class="handoff-panel__evidence">
          {{ formatEvidence(stage.evidence) }}
        </p>
        <NuxtLink class="handoff-panel__action" :to="stage.evidence?.routePath || stage.route">
          {{ t(stage.actionKey) }}
        </NuxtLink>
      </article>
    </div>

    <div
      v-if="run.recentItems.length > 0"
      class="handoff-panel__signals"
      data-testid="suite-mainline-handoff-signals"
    >
      <span class="handoff-panel__signals-label">{{ t('suite.sectionOverview.handoff.recentTitle') }}</span>
      <div class="handoff-panel__signals-list">
        <NuxtLink
          v-for="item in run.recentItems"
          :key="item.eventId"
          class="handoff-panel__signal-chip"
          :to="item.routePath || '/suite'"
          :data-testid="`suite-mainline-handoff-signal-${item.eventId}`"
        >
          <strong>{{ t(`organizations.products.${item.productCode}`) }}</strong>
          <span>{{ item.title }}</span>
        </NuxtLink>
      </div>
    </div>
  </section>
</template>

<style scoped>
.handoff-panel {
  display: grid;
  gap: 16px;
  padding: 20px;
  border: 1px solid rgba(14, 92, 86, 0.14);
  background:
    radial-gradient(circle at top left, rgba(19, 112, 118, 0.16), transparent 42%),
    linear-gradient(160deg, rgba(244, 250, 252, 0.98), rgba(255, 255, 255, 0.96));
}

.handoff-panel__copy,
.handoff-panel__summary {
  display: grid;
  gap: 8px;
}

.handoff-panel__eyebrow,
.handoff-panel__product,
.handoff-panel__signals-label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #0f6e6e;
}

.handoff-panel__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(280px, 0.85fr);
  gap: 14px;
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(12, 68, 79, 0.96), rgba(11, 49, 68, 0.96));
  color: #f3fbff;
}

.handoff-panel__summary-pill,
.handoff-panel__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.handoff-panel__summary-pill[data-status='active'],
.handoff-panel__badge[data-status='active'] {
  background: rgba(80, 214, 175, 0.18);
  color: #8ff5d5;
}

.handoff-panel__summary-pill[data-status='completed'],
.handoff-panel__badge[data-status='done'] {
  background: rgba(95, 176, 255, 0.18);
  color: #a8d6ff;
}

.handoff-panel__summary-pill[data-status='idle'],
.handoff-panel__badge[data-status='pending'],
.handoff-panel__badge[data-status='next'] {
  background: rgba(255, 255, 255, 0.12);
  color: rgba(234, 244, 255, 0.92);
}

.handoff-panel__summary-title,
.handoff-panel__summary-copy {
  margin: 0;
}

.handoff-panel__summary-title {
  font-size: clamp(24px, 3vw, 32px);
}

.handoff-panel__summary-copy {
  color: rgba(234, 244, 255, 0.78);
  line-height: 1.6;
}

.handoff-panel__metrics {
  display: grid;
  gap: 12px;
  margin: 0;
}

.handoff-panel__metrics div {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
}

.handoff-panel__metrics dt {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(208, 230, 255, 0.74);
}

.handoff-panel__metrics dd {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
}

.handoff-panel__loading {
  padding: 18px;
  border-radius: 18px;
  border: 1px dashed rgba(15, 110, 110, 0.22);
  color: #45656a;
}

.handoff-panel__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.handoff-panel__card {
  display: grid;
  gap: 10px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(15, 110, 110, 0.14);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 34px rgba(15, 79, 75, 0.06);
}

.handoff-panel__card[data-status='active'] {
  border-color: rgba(15, 110, 110, 0.3);
  box-shadow: 0 18px 38px rgba(15, 79, 75, 0.1);
}

.handoff-panel__card[data-status='next'] {
  background: linear-gradient(180deg, rgba(247, 252, 251, 0.98), rgba(255, 255, 255, 0.96));
}

.handoff-panel__card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.handoff-panel__title,
.handoff-panel__description,
.handoff-panel__evidence {
  margin: 0;
}

.handoff-panel__title {
  font-size: 18px;
  color: #133736;
}

.handoff-panel__description,
.handoff-panel__evidence {
  line-height: 1.6;
  color: var(--mm-muted);
}

.handoff-panel__evidence {
  font-size: 13px;
}

.handoff-panel__action,
.handoff-panel__signal-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  text-decoration: none;
}

.handoff-panel__action {
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #0c5a5a, #0f7a74);
}

.handoff-panel__signals {
  display: grid;
  gap: 10px;
}

.handoff-panel__signals-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.handoff-panel__signal-chip {
  color: #134241;
  background: rgba(15, 110, 110, 0.08);
}

.handoff-panel__signal-chip span,
.handoff-panel__signal-chip strong {
  display: block;
}

.handoff-panel__signal-chip span {
  color: #45656a;
}

@media (max-width: 1120px) {
  .handoff-panel__hero {
    grid-template-columns: 1fr;
  }

  .handoff-panel__grid {
    grid-template-columns: 1fr;
  }
}
</style>
