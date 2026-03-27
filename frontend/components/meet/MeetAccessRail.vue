<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { navigateTo } from '#app'
import type { MeetAccessOverview } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { meetAccessStateTagType, meetShouldShowActivateAction, meetShouldShowWaitlistAction } from '~/utils/meet-access'

interface EnterprisePayload {
  companyName: string
  requestedSeats: number
  note?: string
}

interface Props {
  overview: MeetAccessOverview | null
  loading: boolean
  pendingAction: '' | 'waitlist' | 'activate' | 'contact-sales'
}

const props = defineProps<Props>()
const emit = defineEmits<{
  refresh: []
  activate: []
  joinWaitlist: [note?: string]
  contactSales: [payload: EnterprisePayload]
}>()

const { t } = useI18n()

const form = reactive({
  waitlistNote: '',
  companyName: '',
  requestedSeats: 100,
  enterpriseNote: ''
})

watch(
  () => props.overview,
  (overview) => {
    form.companyName = overview?.companyName || ''
    form.requestedSeats = overview?.requestedSeats || 100
    form.enterpriseNote = overview?.requestNote || ''
  },
  { immediate: true },
)

const stateTagType = computed(() => meetAccessStateTagType(props.overview?.accessState))
const showWaitlistAction = computed(() => meetShouldShowWaitlistAction(props.overview))
const showActivateAction = computed(() => meetShouldShowActivateAction(props.overview))
const accessGranted = computed(() => props.overview?.accessGranted ?? false)

function onJoinWaitlist(): void {
  emit('joinWaitlist', form.waitlistNote.trim() || undefined)
}

function openSuite(): void {
  void navigateTo('/suite')
}

function onContactSales(): void {
  emit('contactSales', {
    companyName: form.companyName.trim(),
    requestedSeats: form.requestedSeats,
    note: form.enterpriseNote.trim() || undefined
  })
}
</script>

<template>
  <section class="mm-card meet-access-rail" v-loading="loading">
    <div class="meet-access-hero">
      <div class="meet-access-copy">
        <el-tag size="small" effect="plain" type="info">{{ t('meet.access.badge') }}</el-tag>
        <h1 class="meet-access-title">{{ t('meet.access.title') }}</h1>
        <p class="meet-access-description">{{ t('meet.access.description') }}</p>
        <div class="meet-access-features">
          <span class="meet-access-feature">{{ t('meet.access.feature.e2ee') }}</span>
          <span class="meet-access-feature">{{ t('meet.access.feature.noTracking') }}</span>
          <span class="meet-access-feature">{{ t('meet.access.feature.noInstall') }}</span>
        </div>
      </div>
      <div class="meet-access-metrics">
        <div class="meet-access-metric">
          <span>{{ t('meet.access.planLabel') }}</span>
          <strong>{{ overview?.planName || '-' }}</strong>
        </div>
        <div class="meet-access-metric">
          <span>{{ t('meet.access.stateLabel') }}</span>
          <el-tag :type="stateTagType" effect="plain">
            {{ t(`meet.access.state.${overview?.accessState || 'LOCKED'}`) }}
          </el-tag>
        </div>
        <div class="meet-access-metric">
          <span>{{ t('meet.access.recommendedLabel') }}</span>
          <strong>{{ t(`meet.access.recommended.${overview?.recommendedAction || 'JOIN_WAITLIST'}`) }}</strong>
        </div>
      </div>
    </div>

    <div class="meet-access-actions">
      <el-button plain @click="emit('refresh')">{{ t('meet.access.refresh') }}</el-button>
      <el-button v-if="showActivateAction" type="primary" :loading="pendingAction === 'activate'" @click="emit('activate')">
        {{ t('meet.access.activate') }}
      </el-button>
      <el-button v-else plain @click="openSuite">{{ t('meet.access.openSuite') }}</el-button>
    </div>

    <div class="meet-access-grid">
      <section class="meet-access-card">
        <h2>{{ t('meet.access.waitlist.title') }}</h2>
        <el-input
          v-model="form.waitlistNote"
          type="textarea"
          :rows="3"
          :placeholder="t('meet.access.waitlist.placeholder')"
          maxlength="512"
          show-word-limit
        />
        <div class="meet-access-card-footer">
          <el-tag v-if="overview?.waitlistRequested" type="warning" effect="plain">
            {{ t('meet.access.waitlist.done') }}
          </el-tag>
          <el-button
            v-if="showWaitlistAction"
            type="primary"
            :loading="pendingAction === 'waitlist'"
            @click="onJoinWaitlist"
          >
            {{ t('meet.access.waitlist.submit') }}
          </el-button>
        </div>
      </section>

      <section class="meet-access-card">
        <h2>{{ t('meet.access.enterprise.title') }}</h2>
        <el-form label-position="top">
          <el-form-item :label="t('meet.access.enterprise.company')">
            <el-input v-model="form.companyName" maxlength="128" />
          </el-form-item>
          <el-form-item :label="t('meet.access.enterprise.seats')">
            <el-input-number v-model="form.requestedSeats" :min="1" :max="50000" />
          </el-form-item>
          <el-form-item :label="t('meet.access.enterprise.note')">
            <el-input
              v-model="form.enterpriseNote"
              type="textarea"
              :rows="3"
              :placeholder="t('meet.access.enterprise.placeholder')"
              maxlength="512"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <div class="meet-access-card-footer">
          <el-tag v-if="overview?.salesContactRequested" type="info" effect="plain">
            {{ t('meet.access.enterprise.done') }}
          </el-tag>
          <el-button
            type="primary"
            plain
            :disabled="!form.companyName.trim()"
            :loading="pendingAction === 'contact-sales'"
            @click="onContactSales"
          >
            {{ t('meet.access.enterprise.submit') }}
          </el-button>
        </div>
      </section>
    </div>

    <el-alert
      v-if="accessGranted"
      type="success"
      :closable="false"
      :title="t('meet.access.activated')"
      show-icon
    />
  </section>
</template>

<style scoped>
.meet-access-rail {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 22px;
  background:
    radial-gradient(circle at top right, rgba(108, 92, 231, 0.18), transparent 34%),
    linear-gradient(145deg, rgba(15, 23, 42, 0.92), rgba(30, 41, 59, 0.96));
  color: #f8fafc;
}

.meet-access-hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.meet-access-copy {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 720px;
}

.meet-access-title {
  margin: 0;
  font-size: 32px;
  line-height: 1.1;
}

.meet-access-description {
  margin: 0;
  color: rgba(226, 232, 240, 0.88);
}

.meet-access-features {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.meet-access-feature {
  padding: 6px 12px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.28);
  font-size: 13px;
}

.meet-access-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(140px, 1fr));
  gap: 12px;
  min-width: min(100%, 420px);
}

.meet-access-metric {
  padding: 14px;
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.32);
  border: 1px solid rgba(148, 163, 184, 0.16);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meet-access-metric span {
  font-size: 12px;
  color: rgba(226, 232, 240, 0.72);
}

.meet-access-metric strong {
  font-size: 16px;
  color: #fff;
}

.meet-access-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.meet-access-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.meet-access-card {
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(148, 163, 184, 0.14);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.meet-access-card h2 {
  margin: 0;
  font-size: 18px;
}

.meet-access-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 960px) {
  .meet-access-grid,
  .meet-access-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
