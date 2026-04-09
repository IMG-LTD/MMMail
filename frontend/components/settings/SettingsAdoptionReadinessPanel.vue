<script setup lang="ts">
import { computed } from 'vue'
import { COMMUNITY_V1_ADOPTION_CHECKLIST_ITEMS } from '~/constants/module-maturity'
import { useI18n } from '~/composables/useI18n'
import { resolveApiBase } from '~/utils/api-base'

interface AdoptionTrack {
  code: string
  titleKey: string
  descriptionKey: string
  checkpointKeys: readonly string[]
  primaryHref: string
  primaryExternal: boolean
  primaryActionKey: string
  secondaryHref: string
  secondaryExternal: boolean
  secondaryActionKey: string
}

interface AdoptionCardAction {
  key: string
  href: string
  external: boolean
  actionKey: string
  primary?: boolean
}

interface AdoptionCard {
  code: string
  titleKey: string
  descriptionKey: string
  metaKey: string
  metaParams?: Record<string, string>
  actions: readonly AdoptionCardAction[]
}

const runtimeConfig = useRuntimeConfig()
const { t } = useI18n()

const apiGuidePath = '/self-hosted/api.html'
const adoptionGuideUrl = '/self-hosted/adoption.html'
const selfHostedInstallGuideUrl = '/self-hosted/install.html'
const selfHostedRunbookUrl = '/self-hosted/runbook.html'
const teamEnablementGuideUrl = '/self-hosted/team.html'
const identityReadinessGuideUrl = '/self-hosted/identity.html'

const apiBase = computed(() => resolveApiBase(runtimeConfig.public.apiBase))
const normalizedApiBase = computed(() => apiBase.value.replace(/\/+$/, ''))
const apiGuideUrl = computed(() => `${apiGuidePath}?${new URLSearchParams({ apiBase: normalizedApiBase.value }).toString()}`)
const swaggerUiUrl = computed(() => new URL('/swagger-ui.html', normalizedApiBase.value).toString())
const openApiJsonUrl = computed(() => new URL('/v3/api-docs', normalizedApiBase.value).toString())
const checklistItems = COMMUNITY_V1_ADOPTION_CHECKLIST_ITEMS
const launchTracks = computed<readonly AdoptionTrack[]>(() => [
  {
    code: 'ADMIN_BASELINE',
    titleKey: 'settings.adoption.tracks.admin.title',
    descriptionKey: 'settings.adoption.tracks.admin.description',
    checkpointKeys: ['settings.adoption.tracks.admin.checkpoints.keys', 'settings.adoption.tracks.admin.checkpoints.boundary'],
    primaryHref: '/settings#settings-mail-e2ee-panel',
    primaryExternal: false,
    primaryActionKey: 'settings.adoption.tracks.admin.primary',
    secondaryHref: '/suite?section=boundary',
    secondaryExternal: false,
    secondaryActionKey: 'settings.adoption.tracks.admin.secondary'
  },
  {
    code: 'TEAM_HANDOFF',
    titleKey: 'settings.adoption.tracks.team.title',
    descriptionKey: 'settings.adoption.tracks.team.description',
    checkpointKeys: ['settings.adoption.tracks.team.checkpoints.overview', 'settings.adoption.tracks.team.checkpoints.pass'],
    primaryHref: '/suite?section=overview',
    primaryExternal: false,
    primaryActionKey: 'settings.adoption.tracks.team.primary',
    secondaryHref: teamEnablementGuideUrl,
    secondaryExternal: true,
    secondaryActionKey: 'settings.adoption.tracks.team.secondary'
  },
  {
    code: 'IDENTITY_READINESS',
    titleKey: 'settings.adoption.tracks.identity.title',
    descriptionKey: 'settings.adoption.tracks.identity.description',
    checkpointKeys: ['settings.adoption.tracks.identity.checkpoints.scope', 'settings.adoption.tracks.identity.checkpoints.readiness'],
    primaryHref: identityReadinessGuideUrl,
    primaryExternal: true,
    primaryActionKey: 'settings.adoption.tracks.identity.primary',
    secondaryHref: teamEnablementGuideUrl,
    secondaryExternal: true,
    secondaryActionKey: 'settings.adoption.tracks.identity.secondary'
  },
  {
    code: 'DEVELOPER_HANDOFF',
    titleKey: 'settings.adoption.tracks.developer.title',
    descriptionKey: 'settings.adoption.tracks.developer.description',
    checkpointKeys: ['settings.adoption.tracks.developer.checkpoints.adoption', 'settings.adoption.tracks.developer.checkpoints.api'],
    primaryHref: adoptionGuideUrl,
    primaryExternal: true,
    primaryActionKey: 'settings.adoption.tracks.developer.primary',
    secondaryHref: apiGuideUrl.value,
    secondaryExternal: true,
    secondaryActionKey: 'settings.adoption.tracks.developer.secondary'
  }
])

const resourceCards = computed<readonly AdoptionCard[]>(() => [
  {
    code: 'guide',
    titleKey: 'settings.adoption.cards.adoption.title',
    descriptionKey: 'settings.adoption.cards.adoption.description',
    metaKey: 'settings.adoption.cards.adoption.meta',
    actions: [
      { key: 'settings-adoption-guide-link', href: adoptionGuideUrl, external: true, actionKey: 'settings.adoption.cards.adoption.primary', primary: true },
      { key: 'settings-adoption-mainline-link', href: '/suite?section=overview', external: false, actionKey: 'settings.adoption.cards.adoption.secondary' }
    ]
  },
  {
    code: 'team',
    titleKey: 'settings.adoption.cards.team.title',
    descriptionKey: 'settings.adoption.cards.team.description',
    metaKey: 'settings.adoption.cards.team.meta',
    actions: [
      { key: 'settings-adoption-team-guide-link', href: teamEnablementGuideUrl, external: true, actionKey: 'settings.adoption.cards.team.primary', primary: true },
      { key: 'settings-adoption-team-pass-link', href: '/pass', external: false, actionKey: 'settings.adoption.cards.team.secondary' }
    ]
  },
  {
    code: 'identity',
    titleKey: 'settings.adoption.cards.identity.title',
    descriptionKey: 'settings.adoption.cards.identity.description',
    metaKey: 'settings.adoption.cards.identity.meta',
    actions: [
      { key: 'settings-adoption-identity-guide-link', href: identityReadinessGuideUrl, external: true, actionKey: 'settings.adoption.cards.identity.primary', primary: true },
      { key: 'settings-adoption-identity-team-link', href: teamEnablementGuideUrl, external: true, actionKey: 'settings.adoption.cards.identity.secondary' }
    ]
  },
  {
    code: 'api',
    titleKey: 'settings.adoption.cards.api.title',
    descriptionKey: 'settings.adoption.cards.api.description',
    metaKey: 'settings.adoption.cards.api.meta',
    metaParams: { value: normalizedApiBase.value },
    actions: [
      { key: 'settings-adoption-api-guide-link', href: apiGuideUrl.value, external: true, actionKey: 'settings.adoption.cards.api.guide', primary: true },
      { key: 'settings-adoption-swagger-link', href: swaggerUiUrl.value, external: true, actionKey: 'settings.adoption.cards.api.primary' },
      { key: 'settings-adoption-openapi-link', href: openApiJsonUrl.value, external: true, actionKey: 'settings.adoption.cards.api.secondary' }
    ]
  },
  {
    code: 'self-hosted',
    titleKey: 'settings.adoption.cards.selfHosted.title',
    descriptionKey: 'settings.adoption.cards.selfHosted.description',
    metaKey: 'settings.adoption.cards.selfHosted.meta',
    actions: [
      { key: 'settings-adoption-install-guide-link', href: selfHostedInstallGuideUrl, external: true, actionKey: 'settings.adoption.cards.selfHosted.primary', primary: true },
      { key: 'settings-adoption-runbook-link', href: selfHostedRunbookUrl, external: true, actionKey: 'settings.adoption.cards.selfHosted.secondary' }
    ]
  }
])

function resolveCardMeta(card: AdoptionCard): string {
  return card.metaParams ? t(card.metaKey, card.metaParams) : t(card.metaKey)
}
</script>

<template>
  <section class="mm-card adoption-panel" data-testid="settings-adoption-panel">
    <div class="adoption-panel__copy">
      <span class="adoption-panel__eyebrow">{{ t('settings.adoption.eyebrow') }}</span>
      <h2 class="mm-section-title">{{ t('settings.adoption.title') }}</h2>
      <p>{{ t('settings.adoption.description') }}</p>
    </div>

    <article class="adoption-panel__checklist" data-testid="settings-adoption-checklist">
      <div class="adoption-panel__card-copy">
        <strong>{{ t('settings.adoption.checklist.title') }}</strong>
        <p>{{ t('settings.adoption.checklist.description') }}</p>
      </div>
      <div class="adoption-panel__checklist-list">
        <div
          v-for="item in checklistItems"
          :key="item.code"
          class="adoption-panel__checklist-item"
          :data-testid="`settings-adoption-checklist-${item.code.toLowerCase()}`"
        >
          <div>
            <strong>{{ t(item.titleKey) }}</strong>
            <p>{{ t(item.descriptionKey) }}</p>
          </div>
          <a
            v-if="item.external"
            class="adoption-panel__action adoption-panel__action--primary"
            :data-testid="`settings-adoption-checklist-link-${item.code.toLowerCase()}`"
            :href="item.href"
            target="_blank"
            rel="noopener noreferrer"
          >
            {{ t(item.actionKey) }}
          </a>
          <NuxtLink
            v-else
            class="adoption-panel__action adoption-panel__action--primary"
            :data-testid="`settings-adoption-checklist-link-${item.code.toLowerCase()}`"
            :to="item.href"
          >
            {{ t(item.actionKey) }}
          </NuxtLink>
        </div>
      </div>
    </article>

    <article class="adoption-panel__tracks" data-testid="settings-adoption-tracks">
      <div class="adoption-panel__card-copy">
        <strong>{{ t('settings.adoption.tracks.title') }}</strong>
        <p>{{ t('settings.adoption.tracks.description') }}</p>
      </div>
      <div class="adoption-panel__track-grid">
        <section
          v-for="track in launchTracks"
          :key="track.code"
          class="adoption-panel__track"
          :data-testid="`settings-adoption-track-${track.code.toLowerCase()}`"
        >
          <div class="adoption-panel__card-copy">
            <strong>{{ t(track.titleKey) }}</strong>
            <p>{{ t(track.descriptionKey) }}</p>
          </div>
          <ul class="adoption-panel__track-list">
            <li v-for="checkpointKey in track.checkpointKeys" :key="checkpointKey">
              {{ t(checkpointKey) }}
            </li>
          </ul>
          <div class="adoption-panel__actions">
            <a
              v-if="track.primaryExternal"
              class="adoption-panel__action adoption-panel__action--primary"
              :data-testid="`settings-adoption-track-primary-${track.code.toLowerCase()}`"
              :href="track.primaryHref"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ t(track.primaryActionKey) }}
            </a>
            <NuxtLink
              v-else
              class="adoption-panel__action adoption-panel__action--primary"
              :data-testid="`settings-adoption-track-primary-${track.code.toLowerCase()}`"
              :to="track.primaryHref"
            >
              {{ t(track.primaryActionKey) }}
            </NuxtLink>

            <a
              v-if="track.secondaryExternal"
              class="adoption-panel__action"
              :data-testid="`settings-adoption-track-secondary-${track.code.toLowerCase()}`"
              :href="track.secondaryHref"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ t(track.secondaryActionKey) }}
            </a>
            <NuxtLink
              v-else
              class="adoption-panel__action"
              :data-testid="`settings-adoption-track-secondary-${track.code.toLowerCase()}`"
              :to="track.secondaryHref"
            >
              {{ t(track.secondaryActionKey) }}
            </NuxtLink>
          </div>
        </section>
      </div>
    </article>

    <div class="adoption-panel__grid">
      <article
        v-for="card in resourceCards"
        :key="card.code"
        class="adoption-panel__card"
        :data-testid="`settings-adoption-${card.code}-card`"
      >
        <div class="adoption-panel__card-copy">
          <strong>{{ t(card.titleKey) }}</strong>
          <p>{{ t(card.descriptionKey) }}</p>
        </div>
        <div class="adoption-panel__actions">
          <template v-for="action in card.actions" :key="action.key">
            <a
              v-if="action.external"
              :class="['adoption-panel__action', { 'adoption-panel__action--primary': action.primary }]"
              :data-testid="action.key"
              :href="action.href"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ t(action.actionKey) }}
            </a>
            <NuxtLink
              v-else
              :class="['adoption-panel__action', { 'adoption-panel__action--primary': action.primary }]"
              :data-testid="action.key"
              :to="action.href"
            >
              {{ t(action.actionKey) }}
            </NuxtLink>
          </template>
        </div>
        <p class="adoption-panel__meta" :data-testid="`settings-adoption-${card.code}-meta`">
          {{ resolveCardMeta(card) }}
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
  border: 1px solid rgba(12, 90, 90, 0.12);
  background:
    radial-gradient(circle at top right, rgba(18, 126, 120, 0.1), transparent 40%),
    linear-gradient(180deg, rgba(250, 253, 252, 0.98), rgba(255, 255, 255, 0.96));
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

.adoption-panel__checklist {
  display: grid;
  gap: 14px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(15, 110, 110, 0.14);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 32px rgba(15, 79, 75, 0.06);
}

.adoption-panel__tracks {
  display: grid;
  gap: 14px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(15, 110, 110, 0.14);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 32px rgba(15, 79, 75, 0.04);
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

.adoption-panel__checklist-list {
  display: grid;
  gap: 10px;
}

.adoption-panel__track-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.adoption-panel__track {
  display: grid;
  gap: 12px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid rgba(12, 90, 90, 0.1);
  background: rgba(12, 90, 90, 0.04);
}

.adoption-panel__track-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 18px;
  color: var(--mm-muted);
  line-height: 1.6;
}

.adoption-panel__checklist-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(12, 90, 90, 0.1);
  background: rgba(12, 90, 90, 0.04);
}

.adoption-panel__checklist-item p {
  margin: 8px 0 0;
  color: var(--mm-muted);
  line-height: 1.6;
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

@media (max-width: 900px) {
  .adoption-panel__checklist-item {
    flex-direction: column;
  }
}
</style>
