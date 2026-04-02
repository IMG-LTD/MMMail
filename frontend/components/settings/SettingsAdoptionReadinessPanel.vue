<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import { resolveApiBase } from '~/utils/api-base'

const runtimeConfig = useRuntimeConfig()
const { t } = useI18n()

const selfHostedInstallGuideUrl = '/self-hosted/install.html'
const selfHostedRunbookUrl = '/self-hosted/runbook.html'

const apiBase = computed(() => resolveApiBase(runtimeConfig.public.apiBase))
const swaggerUiUrl = computed(() => new URL('/swagger-ui.html', apiBase.value).toString())
const openApiJsonUrl = computed(() => new URL('/v3/api-docs', apiBase.value).toString())
</script>

<template>
  <section class="mm-card adoption-panel" data-testid="settings-adoption-panel">
    <div class="adoption-panel__copy">
      <span class="adoption-panel__eyebrow">{{ t('settings.adoption.eyebrow') }}</span>
      <h2 class="mm-section-title">{{ t('settings.adoption.title') }}</h2>
      <p>{{ t('settings.adoption.description') }}</p>
    </div>

    <div class="adoption-panel__grid">
      <article class="adoption-panel__card" data-testid="settings-adoption-api-card">
        <div class="adoption-panel__card-copy">
          <strong>{{ t('settings.adoption.cards.api.title') }}</strong>
          <p>{{ t('settings.adoption.cards.api.description') }}</p>
        </div>
        <div class="adoption-panel__actions">
          <a
            class="adoption-panel__action adoption-panel__action--primary"
            data-testid="settings-adoption-swagger-link"
            :href="swaggerUiUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            {{ t('settings.adoption.cards.api.primary') }}
          </a>
          <a
            class="adoption-panel__action"
            data-testid="settings-adoption-openapi-link"
            :href="openApiJsonUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            {{ t('settings.adoption.cards.api.secondary') }}
          </a>
        </div>
        <p class="adoption-panel__meta" data-testid="settings-adoption-api-meta">
          {{ t('settings.adoption.cards.api.meta', { value: apiBase }) }}
        </p>
      </article>

      <article class="adoption-panel__card" data-testid="settings-adoption-self-hosted-card">
        <div class="adoption-panel__card-copy">
          <strong>{{ t('settings.adoption.cards.selfHosted.title') }}</strong>
          <p>{{ t('settings.adoption.cards.selfHosted.description') }}</p>
        </div>
        <div class="adoption-panel__actions">
          <a
            class="adoption-panel__action adoption-panel__action--primary"
            data-testid="settings-adoption-install-guide-link"
            :href="selfHostedInstallGuideUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            {{ t('settings.adoption.cards.selfHosted.primary') }}
          </a>
          <a
            class="adoption-panel__action"
            data-testid="settings-adoption-runbook-link"
            :href="selfHostedRunbookUrl"
            target="_blank"
            rel="noopener noreferrer"
          >
            {{ t('settings.adoption.cards.selfHosted.secondary') }}
          </a>
        </div>
        <p class="adoption-panel__meta" data-testid="settings-adoption-self-hosted-meta">
          {{ t('settings.adoption.cards.selfHosted.meta') }}
        </p>
      </article>
    </div>

    <el-alert
      :closable="false"
      type="info"
      data-testid="settings-adoption-boundary"
      :title="t('settings.adoption.boundaryTitle')"
      :description="t('settings.adoption.boundaryDescription')"
    />
  </section>
</template>

<style scoped>
.adoption-panel {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.adoption-panel__copy {
  display: grid;
  gap: 8px;
}

.adoption-panel__copy p {
  margin: 0;
  color: var(--mm-muted);
  line-height: 1.6;
}

.adoption-panel__eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--mm-accent, #0c5a5a);
}

.adoption-panel__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}

.adoption-panel__card {
  display: grid;
  gap: 12px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(12, 90, 90, 0.05);
  border: 1px solid rgba(12, 90, 90, 0.12);
}

.adoption-panel__card-copy {
  display: grid;
  gap: 8px;
}

.adoption-panel__card-copy p,
.adoption-panel__meta {
  margin: 0;
  color: var(--mm-muted);
  line-height: 1.6;
}

.adoption-panel__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.adoption-panel__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(12, 90, 90, 0.18);
  color: var(--mm-text);
  text-decoration: none;
  background: #fff;
}

.adoption-panel__action--primary {
  color: #fff;
  background: var(--mm-accent, #0c5a5a);
  border-color: transparent;
}
</style>
