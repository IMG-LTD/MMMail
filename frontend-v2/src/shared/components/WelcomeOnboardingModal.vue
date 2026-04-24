<script setup lang="ts">
import { computed, ref } from 'vue'
import { NButton, NCard, NModal, NStep, NSteps } from 'naive-ui'
import { useRouter } from 'vue-router'
import { lt, useLocaleText } from '@/locales'
import { onboardingQuickStartSteps } from '@/shared/content/onboarding-steps'
import { useOnboardingStore } from '@/store/modules/onboarding'

const router = useRouter()
const onboardingStore = useOnboardingStore()
const { tr } = useLocaleText()

const currentIndex = ref(0)

const currentStep = computed(() => onboardingQuickStartSteps[currentIndex.value] ?? onboardingQuickStartSteps[0])
const currentStepNumber = computed(() => currentIndex.value + 1)
const isFirstStep = computed(() => currentIndex.value === 0)
const isLastStep = computed(() => currentIndex.value === onboardingQuickStartSteps.length - 1)
const modalTitle = computed(() => tr(lt('欢迎使用 MMMail', '歡迎使用 MMMail', 'Welcome to MMMail')))
const stepCountText = computed(() =>
  tr(
    lt(
      `第 ${currentStepNumber.value} 步 / 共 ${onboardingQuickStartSteps.length} 步`,
      `第 ${currentStepNumber.value} 步 / 共 ${onboardingQuickStartSteps.length} 步`,
      `Step ${currentStepNumber.value} of ${onboardingQuickStartSteps.length}`
    )
  )
)

function goBack() {
  if (currentIndex.value > 0) {
    currentIndex.value -= 1
  }
}

function goNext() {
  if (!isLastStep.value) {
    currentIndex.value += 1
  }
}

function skipOnboarding() {
  onboardingStore.skipGuide()
  currentIndex.value = 0
}

function completeOnboarding() {
  const targetPath = currentStep.value.targetPath
  onboardingStore.completeGuide()
  currentIndex.value = 0
  void router.push(targetPath)
}
</script>

<template>
  <n-modal :show="onboardingStore.isGuideOpen" :mask-closable="false" :close-on-esc="false" preset="card">
    <n-card
      class="welcome-onboarding-modal"
      :bordered="false"
      role="dialog"
      aria-modal="true"
      aria-labelledby="welcome-onboarding-title"
    >
      <template #header>
        <div class="welcome-onboarding-modal__header">
          <div>
            <span class="section-label">{{ tr(lt('快速开始', '快速開始', 'Quick start')) }}</span>
            <h2 id="welcome-onboarding-title">{{ modalTitle }}</h2>
          </div>
          <span class="welcome-onboarding-modal__count">{{ stepCountText }}</span>
        </div>
      </template>

      <div class="welcome-onboarding-modal__body">
        <n-steps :current="currentStepNumber" vertical>
          <n-step
            v-for="step in onboardingQuickStartSteps"
            :key="step.id"
            :title="tr(step.title)"
            :description="tr(step.description)"
          />
        </n-steps>

        <section class="welcome-onboarding-modal__active-step" aria-live="polite">
          <span class="section-label">{{ tr(lt('当前步骤', '目前步驟', 'Current step')) }}</span>
          <h3>{{ tr(currentStep.title) }}</h3>
          <p>{{ tr(currentStep.description) }}</p>
        </section>
      </div>

      <template #footer>
        <div class="welcome-onboarding-modal__actions">
          <n-button quaternary @click="skipOnboarding">
            {{ tr(lt('跳过指南', '略過指南', 'Skip guide')) }}
          </n-button>
          <div class="welcome-onboarding-modal__step-actions">
            <n-button :disabled="isFirstStep" @click="goBack">
              {{ tr(lt('上一步', '上一步', 'Back')) }}
            </n-button>
            <n-button v-if="!isLastStep" type="primary" @click="goNext">
              {{ tr(lt('下一步', '下一步', 'Next')) }}
            </n-button>
            <n-button v-else type="primary" @click="completeOnboarding">
              {{ tr(currentStep.actionLabel) }}
            </n-button>
          </div>
        </div>
      </template>
    </n-card>
  </n-modal>
</template>

<style scoped>
.welcome-onboarding-modal {
  width: min(720px, calc(100vw - 32px));
}

.welcome-onboarding-modal__header,
.welcome-onboarding-modal__actions,
.welcome-onboarding-modal__step-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.welcome-onboarding-modal__header,
.welcome-onboarding-modal__actions {
  justify-content: space-between;
}

.welcome-onboarding-modal__header h2,
.welcome-onboarding-modal__active-step h3 {
  margin: 6px 0 0;
  color: var(--mm-ink);
  letter-spacing: -0.04em;
}

.welcome-onboarding-modal__header h2 {
  font-size: 24px;
}

.welcome-onboarding-modal__count {
  flex: none;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.welcome-onboarding-modal__body {
  display: grid;
  grid-template-columns: minmax(240px, 0.9fr) minmax(0, 1.1fr);
  gap: 24px;
  align-items: start;
}

.welcome-onboarding-modal__active-step {
  display: grid;
  gap: 10px;
  padding: 18px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius);
  background: var(--mm-card-muted);
}

.welcome-onboarding-modal__active-step h3 {
  font-size: 20px;
}

.welcome-onboarding-modal__active-step p {
  margin: 0;
  color: var(--mm-text-secondary);
  line-height: 1.7;
}

@media (max-width: 760px) {
  .welcome-onboarding-modal__header,
  .welcome-onboarding-modal__actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .welcome-onboarding-modal__body {
    grid-template-columns: 1fr;
  }

  .welcome-onboarding-modal__step-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
