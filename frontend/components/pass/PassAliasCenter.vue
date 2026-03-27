<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { CreatePassMailAliasRequest, PassMailAlias, UpdatePassMailAliasRequest } from '~/types/pass-business'
import {
  formatAliasRouteSummary,
  formatPassAliasStatus,
  formatPassTime,
  resolveAliasRouteEmails
} from '~/utils/pass'

interface ForwardTargetOption {
  label: string
  value: string
}

const props = withDefaults(defineProps<{
  aliases: PassMailAlias[]
  forwardTargetOptions: ForwardTargetOption[]
  loading?: boolean
  mutationId?: string
  selectedAliasId?: string
}>(), {
  aliases: () => [],
  forwardTargetOptions: () => [],
  loading: false,
  mutationId: '',
  selectedAliasId: ''
})

const emit = defineEmits<{
  create: [payload: CreatePassMailAliasRequest]
  update: [aliasId: string, payload: UpdatePassMailAliasRequest]
  enable: [aliasId: string]
  disable: [aliasId: string]
  remove: [aliasId: string]
  select: [aliasId: string]
}>()
const { t } = useI18n()

const activeAliasId = ref('')
const createForm = reactive({
  title: '',
  note: '',
  prefix: '',
  forwardToEmails: [] as string[]
})
const editForm = reactive({
  title: '',
  note: '',
  forwardToEmails: [] as string[]
})

const activeAlias = computed(() => props.aliases.find(item => item.id === activeAliasId.value) || null)
const enabledCount = computed(() => props.aliases.filter(item => item.status === 'ENABLED').length)
const disabledCount = computed(() => props.aliases.filter(item => item.status === 'DISABLED').length)
const hasForwardTargets = computed(() => props.forwardTargetOptions.length > 0)

function defaultForwardTargets(): string[] {
  return props.forwardTargetOptions[0] ? [props.forwardTargetOptions[0].value] : []
}

function normalizeForwardTargets(values: string[] | undefined): string[] {
  if (!values?.length) {
    return []
  }
  return Array.from(new Set(values.filter(Boolean)))
}

function aliasRoutes(alias: PassMailAlias | null): string[] {
  return resolveAliasRouteEmails(alias)
}

function formatAliasStatus(status: string): string {
  return formatPassAliasStatus(status, t)
}

function formatRouteSummary(alias: PassMailAlias | null): string {
  return formatAliasRouteSummary(alias, t)
}

watch(
  () => props.forwardTargetOptions,
  (options) => {
    if (!createForm.forwardToEmails.length && options.length > 0) {
      createForm.forwardToEmails = defaultForwardTargets()
    }
    if (!editForm.forwardToEmails.length && options.length > 0 && !activeAlias.value) {
      editForm.forwardToEmails = defaultForwardTargets()
    }
  },
  { immediate: true, deep: true }
)

watch(
  () => props.aliases,
  (aliases) => {
    if (!aliases.length) {
      activeAliasId.value = ''
      editForm.title = ''
      editForm.note = ''
      editForm.forwardToEmails = defaultForwardTargets()
      return
    }
    if (!activeAliasId.value || !aliases.some(item => item.id === activeAliasId.value)) {
      activeAliasId.value = aliases[0].id
    }
  },
  { immediate: true, deep: true }
)

watch(activeAlias, (alias) => {
  if (!alias) {
    return
  }
  editForm.title = alias.title
  editForm.note = alias.note || ''
  editForm.forwardToEmails = normalizeForwardTargets(aliasRoutes(alias))
}, { immediate: true })

watch(
  () => props.selectedAliasId,
  (aliasId) => {
    if (!aliasId) {
      return
    }
    if (!props.aliases.some((item) => item.id === aliasId)) {
      return
    }
    if (activeAliasId.value !== aliasId) {
      activeAliasId.value = aliasId
    }
  },
  { immediate: true }
)

watch(
  activeAliasId,
  (aliasId) => {
    emit('select', aliasId)
  },
  { immediate: true }
)

function resetCreateForm(): void {
  createForm.title = ''
  createForm.note = ''
  createForm.prefix = ''
  createForm.forwardToEmails = defaultForwardTargets()
}

function onCreate(): void {
  emit('create', {
    title: createForm.title.trim(),
    note: createForm.note.trim() || undefined,
    prefix: createForm.prefix.trim() || undefined,
    forwardToEmails: normalizeForwardTargets(createForm.forwardToEmails)
  })
  resetCreateForm()
}

function onSave(): void {
  if (!activeAlias.value) {
    return
  }
  emit('update', activeAlias.value.id, {
    title: editForm.title.trim(),
    note: editForm.note.trim() || undefined,
    forwardToEmails: normalizeForwardTargets(editForm.forwardToEmails)
  })
}
</script>

<template>
  <section class="alias-center">
    <header class="alias-head">
      <div>
        <p class="alias-eyebrow">{{ t('pass.aliasCenter.eyebrow') }}</p>
        <h3>{{ t('pass.aliasCenter.title') }}</h3>
        <p class="alias-subtitle">{{ t('pass.aliasCenter.subtitle') }}</p>
      </div>
      <div class="alias-metrics">
        <article class="metric-pill">
          <strong>{{ aliases.length }}</strong>
          <span>{{ t('pass.aliasCenter.metrics.total') }}</span>
        </article>
        <article class="metric-pill success">
          <strong>{{ enabledCount }}</strong>
          <span>{{ t('pass.aliasCenter.metrics.enabled') }}</span>
        </article>
        <article class="metric-pill muted">
          <strong>{{ disabledCount }}</strong>
          <span>{{ t('pass.aliasCenter.metrics.disabled') }}</span>
        </article>
      </div>
    </header>

    <div class="alias-grid">
      <section class="alias-card create-card">
        <header class="card-head">
          <strong>{{ t('pass.aliasCenter.create.title') }}</strong>
          <span>{{ t('pass.aliasCenter.create.targets', { count: forwardTargetOptions.length }) }}</span>
        </header>
        <div class="form-grid">
          <el-input v-model="createForm.title" maxlength="128" :placeholder="t('pass.aliasCenter.fields.title')" />
          <el-input v-model="createForm.prefix" maxlength="64" :placeholder="t('pass.aliasCenter.fields.prefix')" />
          <el-select
            v-model="createForm.forwardToEmails"
            multiple
            collapse-tags
            collapse-tags-tooltip
            :disabled="!hasForwardTargets"
            :placeholder="t('pass.aliasCenter.fields.forwardTargets')"
          >
            <el-option v-for="option in forwardTargetOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
          <el-input v-model="createForm.note" type="textarea" :rows="4" maxlength="2000" show-word-limit :placeholder="t('pass.aliasCenter.fields.note')" />
        </div>
        <div class="card-actions">
          <el-button :disabled="!hasForwardTargets" :loading="mutationId === 'create'" type="primary" @click="onCreate">{{ t('pass.aliasCenter.actions.create') }}</el-button>
        </div>
      </section>

      <section class="alias-card list-card">
        <header class="card-head">
          <strong>{{ t('pass.aliasCenter.list.title') }}</strong>
          <span>{{ loading ? t('pass.aliasCenter.list.loading') : aliases.length }}</span>
        </header>
        <div class="alias-list">
          <button
            v-for="alias in aliases"
            :key="alias.id"
            type="button"
            class="alias-row"
            :class="{ active: alias.id === activeAliasId }"
            @click="activeAliasId = alias.id"
          >
            <div class="card-head compact">
              <strong>{{ alias.title }}</strong>
              <span class="status-pill" :class="alias.status === 'ENABLED' ? 'enabled' : 'disabled'">{{ formatAliasStatus(alias.status) }}</span>
            </div>
            <p>{{ alias.aliasEmail }}</p>
            <div class="route-chip-row">
              <span class="route-count">{{ t('pass.aliasCenter.list.routeCount', { count: aliasRoutes(alias).length }) }}</span>
              <span v-for="route in aliasRoutes(alias).slice(0, 2)" :key="route" class="route-chip">{{ route }}</span>
              <span v-if="aliasRoutes(alias).length > 2" class="route-chip muted">+{{ aliasRoutes(alias).length - 2 }}</span>
            </div>
            <div class="alias-meta">
              <span>{{ formatRouteSummary(alias) }}</span>
              <span>{{ formatPassTime(alias.updatedAt) }}</span>
            </div>
          </button>
          <el-empty v-if="!loading && aliases.length === 0" :description="t('pass.aliasCenter.list.empty')" />
        </div>
      </section>
    </div>

    <section class="alias-card editor-card">
      <header class="card-head">
        <strong>{{ activeAlias ? t('pass.aliasCenter.editor.editTitle') : t('pass.aliasCenter.editor.detailTitle') }}</strong>
        <span v-if="activeAlias">{{ activeAlias.aliasEmail }}</span>
      </header>
      <template v-if="activeAlias">
        <div class="form-grid">
          <el-input v-model="editForm.title" maxlength="128" :placeholder="t('pass.aliasCenter.fields.title')" />
          <el-select
            v-model="editForm.forwardToEmails"
            multiple
            collapse-tags
            collapse-tags-tooltip
            :disabled="!hasForwardTargets"
            :placeholder="t('pass.aliasCenter.fields.forwardTargets')"
          >
            <el-option v-for="option in forwardTargetOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
          <el-input v-model="editForm.note" type="textarea" :rows="4" maxlength="2000" show-word-limit :placeholder="t('pass.aliasCenter.editor.note')" />
        </div>
        <div class="route-chip-row details">
          <span class="route-count">{{ t('pass.aliasCenter.editor.routesActive', { count: aliasRoutes(activeAlias).length }) }}</span>
          <span v-for="route in aliasRoutes(activeAlias)" :key="route" class="route-chip">{{ route }}</span>
        </div>
        <div class="alias-meta split">
          <span>{{ t('pass.aliasCenter.editor.createdAt', { value: formatPassTime(activeAlias.createdAt) }) }}</span>
          <span>{{ t('pass.aliasCenter.editor.status', { value: formatAliasStatus(activeAlias.status) }) }}</span>
        </div>
        <div class="card-actions wrap">
          <el-button :loading="mutationId === `save:${activeAlias.id}`" type="primary" @click="onSave">{{ t('pass.aliasCenter.actions.save') }}</el-button>
          <el-button
            v-if="activeAlias.status === 'ENABLED'"
            :loading="mutationId === `disable:${activeAlias.id}`"
            plain
            @click="emit('disable', activeAlias.id)"
          >{{ t('pass.aliasCenter.actions.disable') }}</el-button>
          <el-button
            v-else
            :loading="mutationId === `enable:${activeAlias.id}`"
            plain
            @click="emit('enable', activeAlias.id)"
          >{{ t('pass.aliasCenter.actions.enable') }}</el-button>
          <el-button :loading="mutationId === `delete:${activeAlias.id}`" type="danger" @click="emit('remove', activeAlias.id)">{{ t('pass.aliasCenter.actions.delete') }}</el-button>
        </div>
      </template>
      <el-empty v-else :description="t('pass.aliasCenter.editor.empty')" />
    </section>
  </section>
</template>

<style scoped>
.alias-center {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.alias-head,
.card-head,
.alias-meta,
.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.alias-eyebrow {
  margin: 0 0 6px;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 11px;
  color: #7c6cff;
}

.alias-head h3,
.alias-subtitle {
  margin: 0;
}

.alias-subtitle,
.alias-meta,
.card-head span,
.alias-row p,
.route-count {
  color: #667085;
  font-size: 12px;
}

.alias-metrics {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.metric-pill,
.alias-card,
.alias-row {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
}

.metric-pill {
  min-width: 92px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metric-pill strong {
  font-size: 24px;
  color: #101828;
}

.metric-pill.success {
  background: rgba(16, 185, 129, 0.08);
}

.metric-pill.muted {
  background: rgba(100, 116, 139, 0.08);
}

.alias-grid {
  display: grid;
  grid-template-columns: minmax(280px, 0.8fr) minmax(0, 1.2fr);
  gap: 14px;
}

.alias-card {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-grid,
.alias-list {
  display: grid;
  gap: 12px;
}

.alias-row {
  padding: 14px;
  text-align: left;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.alias-row.active,
.alias-row:hover {
  transform: translateY(-1px);
  border-color: rgba(124, 108, 255, 0.28);
  box-shadow: 0 16px 34px rgba(91, 78, 180, 0.12);
}

.compact {
  align-items: flex-start;
}

.status-pill {
  border-radius: 999px;
  padding: 4px 8px;
  font-size: 11px;
}

.status-pill.enabled {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.status-pill.disabled {
  background: rgba(148, 163, 184, 0.18);
  color: #475467;
}

.route-chip-row {
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

.route-chip.muted {
  background: rgba(148, 163, 184, 0.16);
  color: #475467;
}

.route-chip-row.details {
  margin-top: -2px;
}

.alias-meta.split,
.card-actions.wrap {
  flex-wrap: wrap;
}

@media (max-width: 960px) {
  .alias-grid {
    grid-template-columns: 1fr;
  }
}
</style>
