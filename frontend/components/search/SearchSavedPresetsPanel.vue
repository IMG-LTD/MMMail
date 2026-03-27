<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { SearchPreset } from '~/types/api'
import { buildSearchUsageText } from '~/utils/search-workspace'

const props = defineProps<{
  items: SearchPreset[]
  loading?: boolean
}>()

const emit = defineEmits<{
  apply: [presetId: string]
  edit: [preset: SearchPreset]
  togglePin: [preset: SearchPreset]
  remove: [presetId: string]
}>()

const { locale, t } = useI18n()
const pinnedPresets = computed(() => props.items.filter(item => item.isPinned))
const otherPresets = computed(() => props.items.filter(item => !item.isPinned))
</script>

<template>
  <section class="mm-card panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-subtitle">{{ t('search.presets.title') }}</h2>
        <span class="saved-tip">{{ t('search.presets.tip') }}</span>
      </div>
    </div>
    <el-skeleton :loading="loading" animated :rows="2">
      <template #default>
        <div v-if="items.length === 0" class="empty">{{ t('search.presets.empty') }}</div>
        <div v-else class="saved-list">
          <div v-if="pinnedPresets.length > 0" class="list-section-title">{{ t('search.presets.pinned') }}</div>
          <div v-for="item in pinnedPresets" :key="item.id" class="saved-item pinned-item">
            <div class="saved-main">
              <div class="saved-name">
                {{ item.name }}
                <el-tag size="small" type="warning" effect="plain">{{ t('search.presets.pinned') }}</el-tag>
              </div>
              <div class="saved-meta">{{ buildSearchUsageText(item, locale, t) }}</div>
            </div>
            <div class="saved-actions">
              <el-button size="small" type="primary" text @click="emit('apply', item.id)">{{ t('search.presets.apply') }}</el-button>
              <el-button size="small" type="primary" text @click="emit('edit', item)">{{ t('search.presets.edit') }}</el-button>
              <el-button size="small" type="warning" text @click="emit('togglePin', item)">{{ t('search.presets.unpin') }}</el-button>
              <el-button size="small" type="danger" text @click="emit('remove', item.id)">{{ t('common.actions.delete') }}</el-button>
            </div>
          </div>

          <div v-if="otherPresets.length > 0" class="list-section-title">{{ t('search.presets.others') }}</div>
          <div v-for="item in otherPresets" :key="item.id" class="saved-item">
            <div class="saved-main">
              <div class="saved-name">{{ item.name }}</div>
              <div class="saved-meta">{{ buildSearchUsageText(item, locale, t) }}</div>
            </div>
            <div class="saved-actions">
              <el-button size="small" type="primary" text @click="emit('apply', item.id)">{{ t('search.presets.apply') }}</el-button>
              <el-button size="small" type="primary" text @click="emit('edit', item)">{{ t('search.presets.edit') }}</el-button>
              <el-button size="small" type="warning" text @click="emit('togglePin', item)">{{ t('search.presets.pin') }}</el-button>
              <el-button size="small" type="danger" text @click="emit('remove', item.id)">{{ t('common.actions.delete') }}</el-button>
            </div>
          </div>
        </div>
      </template>
    </el-skeleton>
  </section>
</template>

<style scoped>
.panel {
  padding: 16px;
}

.panel-head {
  margin-bottom: 10px;
}

.saved-tip {
  color: var(--mm-muted);
  font-size: 12px;
}

.saved-list {
  display: grid;
  gap: 8px;
}

.list-section-title {
  margin-top: 2px;
  font-size: 12px;
  color: var(--mm-muted);
  font-weight: 600;
}

.saved-item {
  border: 1px solid var(--mm-line);
  border-radius: 10px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pinned-item {
  border-color: #f2d18b;
  background: #fffaf0;
}

.saved-name {
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.saved-meta,
.empty {
  margin-top: 2px;
  font-size: 12px;
  color: var(--mm-muted);
}

.saved-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

@media (max-width: 1080px) {
  .saved-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
