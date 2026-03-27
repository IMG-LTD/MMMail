<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { SuiteCollaborationEvent } from '~/types/api'

const props = defineProps<{
  items: SuiteCollaborationEvent[]
  loading: boolean
  creatingDoc: boolean
  creatingSheet: boolean
}>()

const emit = defineEmits<{
  createDoc: []
  createSheet: []
  openItem: [item: SuiteCollaborationEvent]
}>()

const { t } = useI18n()

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
      <div class="actions">
        <el-button type="primary" :loading="creatingDoc" @click="emit('createDoc')">{{ t('drive.launcher.newDoc') }}</el-button>
        <el-button type="success" plain :loading="creatingSheet" @click="emit('createSheet')">{{ t('drive.launcher.newSheet') }}</el-button>
      </div>
    </div>

    <div class="launchpad-events">
      <div class="events-head">
        <span>{{ t('drive.launcher.recentTitle') }}</span>
        <strong>{{ items.length }}</strong>
      </div>
      <el-skeleton v-if="loading" :rows="3" animated />
      <el-empty v-else-if="items.length === 0" :description="t('drive.launcher.empty')" :image-size="68" />
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
.description {
  margin: 0;
}

.launchpad-copy h2 {
  font-size: clamp(26px, 3vw, 36px);
}

.description {
  color: rgba(234, 244, 255, 0.8);
  line-height: 1.7;
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
