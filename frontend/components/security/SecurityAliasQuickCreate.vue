<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import type { CreatePassMailAliasRequest, PassMailAlias } from '~/types/pass-business'
import {
  formatAliasRouteSummary,
  formatPassAliasStatus,
  formatPassTime,
  limitRecentAliases,
  resolveAliasRouteEmails,
  resolvePreferredAliasRouteSelection
} from '~/utils/pass'

interface ForwardTargetOption {
  label: string
  value: string
}

const props = withDefaults(defineProps<{
  aliases: PassMailAlias[]
  forwardTargetOptions: ForwardTargetOption[]
  currentUserEmail?: string
  loading?: boolean
  mutationId?: string
}>(), {
  aliases: () => [],
  forwardTargetOptions: () => [],
  currentUserEmail: '',
  loading: false,
  mutationId: ''
})

const emit = defineEmits<{
  create: [payload: CreatePassMailAliasRequest]
  jump: []
}>()

const form = reactive({
  title: '',
  note: '',
  prefix: '',
  forwardToEmails: [] as string[]
})

const recentAliases = computed(() => limitRecentAliases(props.aliases, 3))
const hasForwardTargets = computed(() => props.forwardTargetOptions.length > 0)

function defaultForwardTargets(): string[] {
  return resolvePreferredAliasRouteSelection(
    props.forwardTargetOptions.map(option => option.value),
    props.currentUserEmail
  )
}

function normalizeForwardTargets(values: string[] | undefined): string[] {
  if (!values?.length) {
    return []
  }
  return Array.from(new Set(values.filter(Boolean)))
}

watch(
  () => [props.forwardTargetOptions, props.currentUserEmail],
  () => {
    if (!form.forwardToEmails.length && props.forwardTargetOptions.length > 0) {
      form.forwardToEmails = defaultForwardTargets()
    }
  },
  { immediate: true, deep: true }
)

function onCreate(): void {
  emit('create', {
    title: form.title.trim(),
    note: form.note.trim() || undefined,
    prefix: form.prefix.trim() || undefined,
    forwardToEmails: normalizeForwardTargets(form.forwardToEmails)
  })
  form.title = ''
  form.note = ''
  form.prefix = ''
  form.forwardToEmails = defaultForwardTargets()
}
</script>

<template>
  <section class="security-alias-panel">
    <div class="quick-create-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Mail Security Center</p>
          <h2 class="mm-section-title">Create and copy alias</h2>
        </div>
        <el-button text type="primary" @click="emit('jump')">All aliases</el-button>
      </div>
      <p class="subtitle">Create a hide-my-email alias from Security Center, route it to your current mailbox by default, and copy it immediately for sign-up flows.</p>
      <div class="form-grid">
        <el-input v-model="form.title" maxlength="128" placeholder="Alias title" />
        <el-input v-model="form.prefix" maxlength="64" placeholder="Prefix (optional)" />
        <el-select
          v-model="form.forwardToEmails"
          multiple
          collapse-tags
          collapse-tags-tooltip
          :disabled="!hasForwardTargets"
          placeholder="Route to mailbox(es)"
        >
          <el-option v-for="option in forwardTargetOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-input v-model="form.note" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="Usage note" />
      </div>
      <div class="panel-actions">
        <el-button :disabled="!hasForwardTargets" :loading="mutationId === 'create'" type="primary" @click="onCreate">Create and copy alias</el-button>
        <span class="hint">{{ forwardTargetOptions.length }} verified mailboxes available</span>
      </div>
    </div>

    <div class="recent-card">
      <div class="panel-head compact">
        <div>
          <p class="eyebrow">Recent</p>
          <h3 class="mm-section-subtitle">Last three aliases</h3>
        </div>
        <span class="hint">{{ loading ? 'Loading' : recentAliases.length }}</span>
      </div>
      <div class="recent-list">
        <article v-for="alias in recentAliases" :key="alias.id" class="recent-item">
          <div class="recent-row">
            <strong>{{ alias.title }}</strong>
            <el-tag :type="alias.status === 'ENABLED' ? 'success' : 'info'" size="small">{{ formatPassAliasStatus(alias.status) }}</el-tag>
          </div>
          <p>{{ alias.aliasEmail }}</p>
          <div class="route-row">
            <span class="route-count">{{ resolveAliasRouteEmails(alias).length }} routes</span>
            <span v-for="route in resolveAliasRouteEmails(alias).slice(0, 2)" :key="route" class="route-chip">{{ route }}</span>
          </div>
          <div class="recent-meta">
            <span>{{ formatAliasRouteSummary(alias) }}</span>
            <span>{{ formatPassTime(alias.updatedAt) }}</span>
          </div>
        </article>
        <el-empty v-if="!loading && recentAliases.length === 0" description="No aliases created yet." />
      </div>
    </div>
  </section>
</template>

<style scoped>
.security-alias-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 16px;
}

.quick-create-card,
.recent-card,
.recent-item {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
}

.quick-create-card,
.recent-card {
  padding: 18px;
}

.panel-head,
.panel-actions,
.recent-row,
.recent-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.panel-head.compact {
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 6px;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 11px;
  color: #7c6cff;
}

.subtitle,
.hint,
.recent-item p,
.recent-meta,
.route-count {
  color: #667085;
  font-size: 12px;
}

.subtitle,
.recent-item p {
  margin: 0;
}

.form-grid,
.recent-list {
  display: grid;
  gap: 12px;
}

.route-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.route-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(124, 108, 255, 0.1);
  color: #5b4eb4;
  font-size: 11px;
}

.recent-item {
  padding: 14px;
  display: grid;
  gap: 8px;
}

@media (max-width: 960px) {
  .security-alias-panel {
    grid-template-columns: 1fr;
  }

  .panel-head,
  .panel-actions,
  .recent-row,
  .recent-meta {
    flex-wrap: wrap;
  }
}
</style>
