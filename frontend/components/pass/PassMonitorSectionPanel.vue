<script setup lang="ts">
import type { PassMonitorItem } from '~/types/pass-business'
import type { PassMonitorSection } from '~/utils/pass-monitor'
import { useI18n } from '~/composables/useI18n'

const props = defineProps<{
  section: PassMonitorSection
  loadingItemId?: string
}>()

const emit = defineEmits<{
  toggle: [item: PassMonitorItem]
  manageTwoFactor: [item: PassMonitorItem]
}>()

const { t } = useI18n()

function itemTypeLabel(itemType: string): string {
  return t(`pass.monitor.itemType.${itemType}`)
}

function scopeLabel(scopeType: string): string {
  return scopeType === 'SHARED'
    ? t('pass.monitor.labels.shared')
    : t('pass.monitor.labels.personal')
}

function actionLabel(item: PassMonitorItem): string {
  return item.excluded
    ? t('pass.monitor.actions.include')
    : t('pass.monitor.actions.exclude')
}

function manageActionLabel(item: PassMonitorItem): string {
  return item.twoFactor.enabled
    ? t('pass.monitor.actions.manageTwoFactor')
    : t('pass.monitor.actions.setupTwoFactor')
}
</script>

<template>
  <section class="monitor-section">
    <header class="section-header">
      <div>
        <h2>{{ section.title }}</h2>
        <p>{{ section.description }}</p>
      </div>
      <el-tag effect="dark" round>{{ section.items.length }}</el-tag>
    </header>

    <el-empty v-if="section.items.length === 0" :description="section.emptyText" />

    <div v-else class="item-list">
      <article v-for="item in section.items" :key="`${section.key}-${item.id}`" class="item-card">
        <div class="item-topline">
          <div>
            <h3>{{ item.title }}</h3>
            <div class="item-meta">
              <span>{{ scopeLabel(item.scopeType) }}</span>
              <span>{{ itemTypeLabel(item.itemType) }}</span>
              <span>{{ t('pass.monitor.labels.updatedAt', { value: item.updatedAt }) }}</span>
            </div>
          </div>
          <div class="item-tags">
            <el-tag v-if="item.weakPassword" type="danger" round>{{ t('pass.monitor.labels.weak') }}</el-tag>
            <el-tag v-if="item.reusedPassword" type="warning" round>{{ t('pass.monitor.labels.reused') }}</el-tag>
            <el-tag v-if="item.twoFactor.enabled" type="success" round>
              {{ t('pass.monitor.labels.twoFactorReady') }}
            </el-tag>
            <el-tag v-else-if="item.inactiveTwoFactor" type="warning" effect="dark" round>
              {{ t('pass.monitor.labels.inactiveTwoFactor') }}
            </el-tag>
            <el-tag v-if="item.excluded" type="info" round>{{ t('pass.monitor.labels.excluded') }}</el-tag>
            <el-tag v-if="!item.canToggleExclusion" type="info" effect="plain" round>
              {{ t('pass.monitor.labels.viewOnly') }}
            </el-tag>
          </div>
        </div>

        <div class="item-body">
          <p>
            <strong>{{ t('pass.monitor.labels.website') }}:</strong>
            {{ item.website || t('pass.monitor.labels.noWebsite') }}
          </p>
          <p>
            <strong>{{ t('pass.monitor.labels.username') }}:</strong>
            {{ item.username || t('pass.monitor.labels.noUsername') }}
          </p>
          <p v-if="item.sharedVaultName">
            <strong>{{ t('pass.monitor.labels.vault', { value: item.sharedVaultName }) }}</strong>
          </p>
          <p v-if="item.reusedPassword">
            {{ t('pass.monitor.labels.reusedCount', { value: item.reusedGroupSize }) }}
          </p>
          <p v-if="item.twoFactor.enabled">
            <strong>{{ t('pass.monitor.labels.twoFactor') }}:</strong>
            {{ item.twoFactor.issuer || item.title }} · {{ item.twoFactor.accountName || t('pass.monitor.labels.noUsername') }}
          </p>
        </div>

        <div v-if="item.canManageTwoFactor || item.canToggleExclusion" class="item-actions">
          <el-button
            v-if="item.canManageTwoFactor"
            size="small"
            type="primary"
            plain
            @click="emit('manageTwoFactor', item)"
          >
            {{ manageActionLabel(item) }}
          </el-button>
          <el-button
            v-if="item.canToggleExclusion"
            size="small"
            :loading="loadingItemId === item.id"
            @click="emit('toggle', item)"
          >
            {{ actionLabel(item) }}
          </el-button>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.monitor-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(143, 160, 210, 0.18);
  box-shadow: 0 18px 48px rgba(38, 58, 103, 0.08);
}

.section-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.section-header h2,
.item-card h3 {
  margin: 0;
}

.section-header p,
.item-body p {
  margin: 0;
  color: #52607a;
}

.item-list {
  display: grid;
  gap: 12px;
}

.item-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(243, 246, 255, 0.9), rgba(250, 252, 255, 0.98));
  border: 1px solid rgba(132, 148, 198, 0.16);
}

.item-topline,
.item-tags,
.item-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
  font-size: 12px;
  color: #70809d;
}

.item-body {
  display: grid;
  gap: 8px;
}

@media (max-width: 720px) {
  .section-header,
  .item-topline {
    flex-direction: column;
  }
}
</style>
