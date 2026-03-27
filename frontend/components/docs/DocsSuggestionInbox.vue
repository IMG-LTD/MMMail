<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { DocsReviewMode, DocsNoteSuggestion } from '~/types/docs'
import { buildDocsSuggestionCounts, getDocsSuggestionTagType } from '~/utils/docs-suggestions'

const props = defineProps<{
  suggestions: DocsNoteSuggestion[]
  selectedExcerpt: string
  reviewMode: DocsReviewMode
  canCreate: boolean
  canResolve: boolean
  submitting: boolean
  busySuggestionId: string
}>()

const emit = defineEmits<{
  (event: 'create', payload: { replacementText: string }): void
  (event: 'accept', suggestionId: string): void
  (event: 'reject', suggestionId: string): void
}>()

const { t } = useI18n()
const replacementText = ref('')

const counts = computed(() => buildDocsSuggestionCounts(props.suggestions))

function submit(): void {
  emit('create', { replacementText: replacementText.value })
  replacementText.value = ''
}

function clearDraft(): void {
  replacementText.value = ''
}

function formatTime(value: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function replacementLabel(value: string): string {
  return value || t('docs.suggestions.deleteMarker')
}
</script>

<template>
  <section class="suggestion-inbox">
    <div class="rail-title">{{ t('docs.suggestions.title') }}</div>
    <p class="rail-copy">{{ t('docs.suggestions.subtitle') }}</p>

    <div class="metrics-row">
      <div class="metric-chip">
        <span>{{ t('docs.suggestions.metrics.pending') }}</span>
        <strong>{{ counts.pending }}</strong>
      </div>
      <div class="metric-chip">
        <span>{{ t('docs.suggestions.metrics.accepted') }}</span>
        <strong>{{ counts.accepted }}</strong>
      </div>
      <div class="metric-chip">
        <span>{{ t('docs.suggestions.metrics.rejected') }}</span>
        <strong>{{ counts.rejected }}</strong>
      </div>
    </div>

    <div class="compose-card" :class="{ 'compose-card--disabled': props.reviewMode !== 'SUGGEST' || !props.canCreate }">
      <div class="selection-pill">{{ t('docs.suggestions.selection') }} · {{ props.selectedExcerpt || t('docs.common.none') }}</div>
      <el-input
        v-model="replacementText"
        type="textarea"
        :rows="3"
        maxlength="2000"
        show-word-limit
        :placeholder="t('docs.suggestions.replacementPlaceholder')"
        :disabled="props.reviewMode !== 'SUGGEST' || !props.canCreate"
      />
      <div class="compose-actions">
        <el-button @click="clearDraft">{{ t('docs.suggestions.clear') }}</el-button>
        <el-button
          type="primary"
          :loading="props.submitting"
          :disabled="props.reviewMode !== 'SUGGEST' || !props.canCreate"
          @click="submit"
        >
          {{ t('docs.suggestions.submit') }}
        </el-button>
      </div>
      <p class="rail-meta">{{ t('docs.suggestions.modeHint') }}</p>
    </div>

    <div v-if="props.suggestions.length" class="suggestion-list">
      <article v-for="item in props.suggestions" :key="item.suggestionId" class="suggestion-item">
        <div class="suggestion-head">
          <div>
            <div class="suggestion-author">{{ item.authorDisplayName || item.authorEmail }}</div>
            <div class="suggestion-time">{{ t('docs.suggestions.createdAt', { value: formatTime(item.createdAt) }) }}</div>
          </div>
          <el-tag :type="getDocsSuggestionTagType(item.status)">{{ t(`docs.suggestions.status.${item.status.toLowerCase()}`) }}</el-tag>
        </div>
        <div class="suggestion-body">
          <div class="suggestion-line">
            <span>{{ t('docs.suggestions.from', { value: item.originalText }) }}</span>
          </div>
          <div class="suggestion-line suggestion-line--target">
            <span>{{ t('docs.suggestions.to', { value: replacementLabel(item.replacementText) }) }}</span>
          </div>
        </div>
        <div v-if="item.resolvedAt" class="rail-meta">
          {{ t('docs.suggestions.resolvedAt', { value: formatTime(item.resolvedAt) }) }}
          <span v-if="item.resolvedByDisplayName || item.resolvedByEmail">
            · {{ t('docs.suggestions.resolvedBy', { value: item.resolvedByDisplayName || item.resolvedByEmail || '' }) }}
          </span>
        </div>
        <div v-if="item.status === 'PENDING' && props.canResolve" class="suggestion-actions">
          <el-button type="primary" text :loading="props.busySuggestionId === item.suggestionId" @click="emit('accept', item.suggestionId)">
            {{ t('docs.suggestions.accept') }}
          </el-button>
          <el-button type="danger" text :loading="props.busySuggestionId === item.suggestionId" @click="emit('reject', item.suggestionId)">
            {{ t('docs.suggestions.reject') }}
          </el-button>
        </div>
      </article>
    </div>
    <el-empty v-else :description="t('docs.suggestions.empty')" :image-size="70" />
  </section>
</template>

<style scoped>
.suggestion-inbox,
.compose-card,
.suggestion-list,
.suggestion-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rail-title {
  font-size: 0.92rem;
  font-weight: 800;
}

.rail-copy,
.rail-meta {
  color: rgba(15, 23, 42, 0.68);
  font-size: 0.82rem;
  line-height: 1.5;
}

.selection-pill {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: rgba(30, 41, 59, 0.84);
  font-size: 0.8rem;
}

.metrics-row,
.compose-actions,
.suggestion-head,
.suggestion-actions {
  display: flex;
  gap: 10px;
}

.metrics-row,
.suggestion-head {
  justify-content: space-between;
  align-items: center;
}

.metric-chip {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 72px;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(148, 163, 184, 0.12);
}

.metric-chip span,
.suggestion-time {
  color: rgba(15, 23, 42, 0.65);
  font-size: 0.78rem;
}

.metric-chip strong,
.suggestion-author {
  font-size: 0.94rem;
  font-weight: 700;
}

.compose-card {
  padding: 14px;
  border-radius: 20px;
  border: 1px solid rgba(13, 17, 23, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92) 0%, rgba(241, 245, 249, 0.72) 100%);
}

.compose-card--disabled {
  opacity: 0.82;
}

.compose-actions,
.suggestion-actions {
  justify-content: flex-end;
}

.suggestion-item {
  padding: 14px;
  border-radius: 20px;
  border: 1px solid rgba(13, 17, 23, 0.08);
  background: rgba(255, 255, 255, 0.76);
}

.suggestion-body {
  display: grid;
  gap: 8px;
}

.suggestion-line {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(226, 232, 240, 0.55);
  font-size: 0.88rem;
}

.suggestion-line--target {
  background: rgba(191, 219, 254, 0.42);
}
</style>
