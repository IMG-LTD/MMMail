<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type {
  PassSecureLinkDashboardEntry,
  PassSecureLinkFilter
} from '~/types/pass-business'
import {
  formatPassTime,
  secureLinkMatchesFilter,
  secureLinkStatusKey,
  secureLinkStatusTone
} from '~/utils/pass'

const props = defineProps<{
  links: PassSecureLinkDashboardEntry[]
  loading: boolean
  mutationId: string
  sharedMode: boolean
}>()

const emit = defineEmits<{
  copy: [publicUrl: string]
  open: [publicUrl: string]
  revoke: [linkId: string]
  focusItem: [payload: { itemId: string; sharedVaultId: string }]
}>()

const { t } = useI18n()

const statusFilter = ref<PassSecureLinkFilter>('ALL')
const keyword = ref('')

const statusOptions = computed(() => {
  const filters: PassSecureLinkFilter[] = ['ALL', 'ACTIVE', 'REVOKED', 'EXPIRED', 'SPENT']
  return filters.map((value) => ({
    value,
    label: t(`pass.secureLinks.dashboard.filters.${value.toLowerCase()}`),
    count: value === 'ALL'
      ? props.links.length
      : props.links.filter(link => link.status === value).length
  }))
})

const metrics = computed(() => ({
  total: props.links.length,
  active: props.links.filter(link => link.status === 'ACTIVE').length,
  revoked: props.links.filter(link => link.status === 'REVOKED').length,
  expired: props.links.filter(link => link.status === 'EXPIRED').length,
  spent: props.links.filter(link => link.status === 'SPENT').length
}))

const filteredLinks = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return props.links.filter((link) => {
    if (!secureLinkMatchesFilter(link, statusFilter.value)) {
      return false
    }
    if (!normalizedKeyword) {
      return true
    }
    return [
      link.itemTitle,
      link.sharedVaultName,
      link.itemWebsite,
      link.itemUsername,
      link.publicUrl
    ].some(value => value?.toLowerCase().includes(normalizedKeyword))
  })
})

function onFocusItem(link: PassSecureLinkDashboardEntry): void {
  if (!link.itemId || !link.sharedVaultId) {
    return
  }
  emit('focusItem', {
    itemId: link.itemId,
    sharedVaultId: link.sharedVaultId
  })
}
</script>

<template>
  <section v-if="sharedMode" class="mm-card secure-links-dashboard" v-loading="loading">
    <header class="secure-links-dashboard__head">
      <div>
        <p class="secure-links-dashboard__eyebrow">{{ t('pass.secureLinks.dashboard.eyebrow') }}</p>
        <h3>{{ t('pass.secureLinks.dashboard.title') }}</h3>
        <p class="secure-links-dashboard__description">{{ t('pass.secureLinks.dashboard.description') }}</p>
      </div>
      <el-tag size="large" effect="dark" type="primary">{{ metrics.total }}</el-tag>
    </header>

    <div class="secure-links-dashboard__metrics">
      <article class="metric-card">
        <span>{{ t('pass.secureLinks.dashboard.metrics.total') }}</span>
        <strong>{{ metrics.total }}</strong>
      </article>
      <article class="metric-card">
        <span>{{ t('pass.secureLinks.dashboard.metrics.active') }}</span>
        <strong>{{ metrics.active }}</strong>
      </article>
      <article class="metric-card">
        <span>{{ t('pass.secureLinks.dashboard.metrics.revoked') }}</span>
        <strong>{{ metrics.revoked }}</strong>
      </article>
      <article class="metric-card">
        <span>{{ t('pass.secureLinks.dashboard.metrics.expired') }}</span>
        <strong>{{ metrics.expired }}</strong>
      </article>
      <article class="metric-card">
        <span>{{ t('pass.secureLinks.dashboard.metrics.spent') }}</span>
        <strong>{{ metrics.spent }}</strong>
      </article>
    </div>

    <div class="secure-links-dashboard__filters">
      <el-select v-model="statusFilter" class="secure-links-dashboard__select">
        <el-option
          v-for="option in statusOptions"
          :key="option.value"
          :label="`${option.label} · ${option.count}`"
          :value="option.value"
        />
      </el-select>
      <el-input
        v-model="keyword"
        clearable
        :placeholder="t('pass.secureLinks.dashboard.searchPlaceholder')"
      />
    </div>

    <div class="secure-links-dashboard__list">
      <article v-for="link in filteredLinks" :key="link.id" class="secure-links-dashboard__card">
        <div class="secure-links-dashboard__row">
          <div class="secure-links-dashboard__identity">
            <strong>{{ link.itemTitle || t('common.none') }}</strong>
            <span>{{ link.sharedVaultName || t('common.none') }}</span>
          </div>
          <el-tag size="small" effect="dark" :type="secureLinkStatusTone(link)">
            {{ t(secureLinkStatusKey(link)) }}
          </el-tag>
        </div>

        <div class="secure-links-dashboard__row secure-links-dashboard__detail">
          <span>{{ link.itemUsername || link.itemWebsite || t('common.none') }}</span>
          <span>{{ t('pass.secureLinks.viewsUsed', { current: link.currentViews, max: link.maxViews }) }}</span>
        </div>

        <code class="secure-links-dashboard__url">{{ link.publicUrl }}</code>

        <div class="secure-links-dashboard__meta">
          <span>{{ t('pass.secureLinks.createdAt', { value: formatPassTime(link.createdAt) }) }}</span>
          <span>{{ t('pass.secureLinks.expiresAt', { value: formatPassTime(link.expiresAt) }) }}</span>
        </div>

        <div class="secure-links-dashboard__actions">
          <el-button size="small" @click="emit('copy', link.publicUrl)">{{ t('pass.secureLinks.actions.copy') }}</el-button>
          <el-button size="small" plain @click="emit('open', link.publicUrl)">{{ t('pass.secureLinks.actions.open') }}</el-button>
          <el-button size="small" plain @click="onFocusItem(link)">{{ t('pass.secureLinks.dashboard.actions.focusItem') }}</el-button>
          <el-button
            size="small"
            type="danger"
            plain
            :disabled="!link.active"
            :loading="mutationId === link.id"
            @click="emit('revoke', link.id)"
          >
            {{ t('pass.secureLinks.actions.revoke') }}
          </el-button>
        </div>
      </article>

      <el-empty v-if="!loading && filteredLinks.length === 0" :description="t('pass.secureLinks.dashboard.empty')" :image-size="64" />
    </div>
  </section>
</template>

<style scoped>
.secure-links-dashboard,
.secure-links-dashboard__list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.secure-links-dashboard__head,
.secure-links-dashboard__row,
.secure-links-dashboard__actions {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.secure-links-dashboard__eyebrow,
.secure-links-dashboard__description,
.secure-links-dashboard__identity span,
.secure-links-dashboard__detail,
.secure-links-dashboard__meta {
  margin: 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.6;
}

.secure-links-dashboard__head h3 {
  margin: 6px 0;
}

.secure-links-dashboard__metrics,
.secure-links-dashboard__filters,
.secure-links-dashboard__meta {
  display: grid;
  gap: 12px;
}

.secure-links-dashboard__metrics {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.metric-card,
.secure-links-dashboard__card {
  border-radius: 20px;
  padding: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(248, 250, 252, 0.95);
}

.metric-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.metric-card strong,
.secure-links-dashboard__identity strong {
  color: #101828;
}

.secure-links-dashboard__filters {
  grid-template-columns: 240px minmax(0, 1fr);
}

.secure-links-dashboard__select {
  width: 100%;
}

.secure-links-dashboard__card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.secure-links-dashboard__identity {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.secure-links-dashboard__detail {
  justify-content: space-between;
}

.secure-links-dashboard__url {
  display: block;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.05);
  color: #344054;
  font-size: 12px;
  word-break: break-all;
}

.secure-links-dashboard__meta {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.secure-links-dashboard__actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

@media (max-width: 1080px) {
  .secure-links-dashboard__metrics,
  .secure-links-dashboard__filters,
  .secure-links-dashboard__meta {
    grid-template-columns: 1fr;
  }

  .secure-links-dashboard__head,
  .secure-links-dashboard__row,
  .secure-links-dashboard__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
