<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { SuiteCollaborationEvent } from '~/types/api'
import type { MainlineCollaborationEvent } from '~/utils/collaboration'
import { buildMainlineHandoffRun } from '~/utils/mainline-handoff'

const props = defineProps<{
  items: SuiteCollaborationEvent[]
  handoffItems: MainlineCollaborationEvent[]
  loading: boolean
  creatingDoc: boolean
  creatingSheet: boolean
  ownerE2eeReady: boolean | null
  ownerE2eeLoading: boolean
  ownerE2eeError: string
}>()

const emit = defineEmits<{
  createDoc: []
  createSheet: []
  openItem: [item: SuiteCollaborationEvent]
  openPass: []
}>()

const { t } = useI18n()

const handoffRun = computed(() => buildMainlineHandoffRun(props.handoffItems))
const currentStageLabel = computed(() => resolveProductLabel(handoffRun.value.currentStage))
const nextStageLabel = computed(() => resolveProductLabel(handoffRun.value.nextStage, true))
const latestEvidenceLabel = computed(() => formatEvidence(handoffRun.value.latestEvent))

const ownerE2eeState = computed(() => {
  if (props.ownerE2eeLoading) {
    return 'loading'
  }
  if (props.ownerE2eeError) {
    return 'error'
  }
  return props.ownerE2eeReady ? 'ready' : 'pending'
})

function resolveProductLabel(productCode: string | null, allowEmpty = false): string {
  if (!productCode) {
    return allowEmpty
      ? t('suite.sectionOverview.handoff.noneNext')
      : t('suite.sectionOverview.handoff.noneCurrent')
  }
  return t(`organizations.products.${productCode}`)
}

function formatEvidence(item: MainlineCollaborationEvent | null): string {
  if (!item) {
    return t('suite.sectionOverview.handoff.noEvidence')
  }
  return `${item.title} · ${formatTime(item.createdAt)}`
}

function ownerE2eeLabel(): string {
  if (ownerE2eeState.value === 'loading') {
    return t('drive.launcher.readiness.e2ee.loading')
  }
  if (ownerE2eeState.value === 'error') {
    return props.ownerE2eeError
  }
  return props.ownerE2eeReady
    ? t('drive.launcher.readiness.e2ee.ready')
    : t('drive.launcher.readiness.e2ee.pending')
}

function formatTime(value: string): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '—'
}
</script>

<template>
  <section class="launchpad mm-card">
    <div class="launchpad-copy">
      <p class="eyebrow">{{ t('drive.launcher.badge') }}</p>
      <h2>{{ t('drive.launcher.title') }}</h2>
      <p class="description">{{ t('drive.launcher.description') }}</p>

      <div class="handoff" data-testid="drive-launchpad-handoff">
        <div class="handoff__copy">
          <span class="handoff__label">{{ t('drive.launcher.handoffTitle') }}</span>
          <strong>{{ currentStageLabel }}</strong>
          <p>{{ latestEvidenceLabel }}</p>
        </div>

        <dl class="handoff__meta">
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
        </dl>
      </div>

      <div class="readiness" data-testid="drive-launchpad-readiness">
        <article
          class="readiness__card"
          data-testid="drive-launchpad-readiness-e2ee"
          :data-state="ownerE2eeState"
        >
          <span class="readiness__label">{{ t('drive.launcher.readiness.e2ee.title') }}</span>
          <strong>{{ ownerE2eeLabel() }}</strong>
        </article>

        <article
          class="readiness__card"
          data-testid="drive-launchpad-readiness-share"
          data-state="ready"
        >
          <span class="readiness__label">{{ t('drive.launcher.readiness.share.title') }}</span>
          <strong>{{ t('drive.launcher.readiness.share.ready') }}</strong>
          <p>{{ t('drive.launcher.readiness.share.note') }}</p>
        </article>

        <article
          class="readiness__card"
          data-testid="drive-launchpad-readiness-pass"
          data-state="ready"
        >
          <span class="readiness__label">{{ t('drive.launcher.readiness.pass.title') }}</span>
          <strong>{{ t('drive.launcher.readiness.pass.ready') }}</strong>
          <p>{{ t('drive.launcher.readiness.pass.note') }}</p>
          <el-button plain @click="emit('openPass')">{{ t('drive.launcher.openPass') }}</el-button>
        </article>
      </div>

      <div class="actions">
        <el-button type="primary" :loading="creatingDoc" @click="emit('createDoc')">
          {{ t('drive.launcher.newDoc') }}
        </el-button>
        <el-button type="success" plain :loading="creatingSheet" @click="emit('createSheet')">
          {{ t('drive.launcher.newSheet') }}
        </el-button>
      </div>
    </div>

    <div class="launchpad-events">
      <div class="events-head">
        <span>{{ t('drive.launcher.recentTitle') }}</span>
        <strong>{{ items.length }}</strong>
      </div>
      <el-skeleton v-if="loading" :rows="3" animated />
      <el-empty
        v-else-if="items.length === 0"
        :description="t('drive.launcher.empty')"
        :image-size="68"
      />
      <button
        v-for="item in items.slice(0, 4)"
        v-else
        :key="item.eventId"
        class="event-item"
        type="button"
        @click="emit('openItem', item)"
      >
        <div>
          <strong>{{ item.title }}</strong>
          <span>{{ item.summary }}</span>
        </div>
        <small>{{ formatTime(item.createdAt) }}</small>
      </button>
    </div>
  </section>
</template>

<style scoped>
.launchpad {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.95fr);
  gap: 18px;
  padding: 20px;
  background:
    radial-gradient(circle at top left, rgba(116, 141, 255, 0.16), transparent 22%),
    radial-gradient(circle at bottom right, rgba(41, 182, 138, 0.16), transparent 24%),
    linear-gradient(135deg, rgba(9, 61, 79, 0.98), rgba(7, 39, 56, 0.98));
  color: #f5fbff;
}

.launchpad-copy,
.launchpad-events {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(208, 230, 255, 0.76);
}

.launchpad-copy h2,
.description,
.handoff__copy p,
.readiness__card p {
  margin: 0;
}

.launchpad-copy h2 {
  font-size: clamp(26px, 3vw, 36px);
}

.description {
  color: rgba(234, 244, 255, 0.8);
  line-height: 1.7;
}

.handoff,
.readiness {
  display: grid;
  gap: 12px;
}

.handoff {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.08);
}

.handoff__copy,
.handoff__meta,
.handoff__meta div {
  display: grid;
  gap: 6px;
}

.handoff__label,
.readiness__label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: rgba(208, 230, 255, 0.76);
}

.handoff__copy strong,
.readiness__card strong {
  font-size: 18px;
}

.handoff__copy p,
.handoff__meta dd,
.readiness__card p {
  color: rgba(234, 244, 255, 0.78);
  line-height: 1.6;
}

.handoff__meta {
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
}

.handoff__meta dt,
.handoff__meta dd {
  margin: 0;
}

.readiness {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.readiness__card {
  display: grid;
  gap: 8px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
}

.readiness__card[data-state='ready'] {
  border-color: rgba(95, 196, 157, 0.28);
}

.readiness__card[data-state='error'] {
  border-color: rgba(255, 133, 133, 0.3);
}

.readiness__card[data-state='loading'] {
  border-color: rgba(137, 197, 255, 0.28);
}

.actions,
.events-head {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.events-head {
  justify-content: space-between;
}

.events-head span {
  color: rgba(208, 230, 255, 0.76);
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-size: 11px;
}

.events-head strong {
  display: inline-flex;
  min-width: 28px;
  height: 28px;
  justify-content: center;
  align-items: center;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
}

.event-item {
  width: 100%;
  text-align: left;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
  color: inherit;
  cursor: pointer;
}

.event-item strong,
.event-item span,
.event-item small {
  display: block;
}

.event-item span,
.event-item small {
  color: rgba(234, 244, 255, 0.74);
}

@media (max-width: 1080px) {
  .launchpad {
    grid-template-columns: 1fr;
  }
}
</style>
