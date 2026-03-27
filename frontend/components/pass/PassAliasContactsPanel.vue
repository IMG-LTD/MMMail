<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type {
  CreatePassAliasContactRequest,
  PassAliasContact,
  PassMailAlias,
  UpdatePassAliasContactRequest
} from '~/types/pass-business'
import { formatPassTime } from '~/utils/pass'

const props = withDefaults(defineProps<{
  alias: PassMailAlias | null
  contacts: PassAliasContact[]
  loading?: boolean
  mutationId?: string
}>(), {
  alias: null,
  contacts: () => [],
  loading: false,
  mutationId: ''
})

const emit = defineEmits<{
  create: [payload: CreatePassAliasContactRequest]
  update: [contactId: string, payload: UpdatePassAliasContactRequest]
  remove: [contactId: string]
  compose: [reverseAliasEmail: string]
  copy: [reverseAliasEmail: string]
}>()
const { t } = useI18n()

const activeContactId = ref('')
const createForm = reactive({
  targetEmail: '',
  displayName: '',
  note: ''
})
const editForm = reactive({
  targetEmail: '',
  displayName: '',
  note: ''
})

const activeContact = computed(() => props.contacts.find((item) => item.id === activeContactId.value) || null)
const hasAlias = computed(() => Boolean(props.alias))

watch(
  () => props.alias?.id,
  () => {
    activeContactId.value = props.contacts[0]?.id || ''
  },
  { immediate: true }
)

watch(
  () => props.contacts,
  (contacts) => {
    if (!contacts.length) {
      activeContactId.value = ''
      editForm.targetEmail = ''
      editForm.displayName = ''
      editForm.note = ''
      return
    }
    if (!activeContactId.value || !contacts.some((item) => item.id === activeContactId.value)) {
      activeContactId.value = contacts[0].id
    }
  },
  { immediate: true, deep: true }
)

watch(
  activeContact,
  (contact) => {
    if (!contact) {
      return
    }
    editForm.targetEmail = contact.targetEmail
    editForm.displayName = contact.displayName || ''
    editForm.note = contact.note || ''
  },
  { immediate: true }
)

function resetCreateForm(): void {
  createForm.targetEmail = ''
  createForm.displayName = ''
  createForm.note = ''
}

function onCreate(): void {
  emit('create', {
    targetEmail: createForm.targetEmail.trim(),
    displayName: createForm.displayName.trim() || undefined,
    note: createForm.note.trim() || undefined
  })
  resetCreateForm()
}

function onSave(): void {
  if (!activeContact.value) {
    return
  }
  emit('update', activeContact.value.id, {
    targetEmail: editForm.targetEmail.trim(),
    displayName: editForm.displayName.trim() || undefined,
    note: editForm.note.trim() || undefined
  })
}
</script>

<template>
  <section class="contacts-shell">
    <header class="contacts-head">
      <div>
        <p class="contacts-eyebrow">{{ t('pass.aliasContacts.eyebrow') }}</p>
        <h3>{{ t('pass.aliasContacts.title') }}</h3>
        <p class="contacts-subtitle">
          <template v-if="alias">
            {{ t('pass.aliasContacts.subtitle.withAlias', { email: alias.aliasEmail }) }}
          </template>
          <template v-else>
            {{ t('pass.aliasContacts.subtitle.withoutAlias') }}
          </template>
        </p>
      </div>
      <div class="contacts-metrics">
        <article class="metric-pill">
          <strong>{{ contacts.length }}</strong>
          <span>{{ t('pass.aliasContacts.metrics.contacts') }}</span>
        </article>
        <article class="metric-pill accent">
          <strong>{{ alias ? t('pass.aliasContacts.metrics.ready') : t('pass.aliasContacts.metrics.idle') }}</strong>
          <span>{{ alias ? t('pass.aliasContacts.metrics.aliasLinked') : t('pass.aliasContacts.metrics.noAlias') }}</span>
        </article>
      </div>
    </header>

    <div class="contacts-grid">
      <section class="contacts-card create-card">
        <header class="card-head">
          <strong>{{ t('pass.aliasContacts.create.title') }}</strong>
          <span>{{ t('pass.aliasContacts.create.boundary') }}</span>
        </header>
        <div class="form-grid">
          <el-input v-model="createForm.targetEmail" :disabled="!hasAlias" maxlength="254" :placeholder="t('pass.aliasContacts.fields.targetEmail')" />
          <el-input v-model="createForm.displayName" :disabled="!hasAlias" maxlength="128" :placeholder="t('pass.aliasContacts.fields.displayNameOptional')" />
          <el-input
            v-model="createForm.note"
            :disabled="!hasAlias"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            :placeholder="t('pass.aliasContacts.fields.note')"
          />
        </div>
        <div class="card-actions">
          <el-button :disabled="!hasAlias" :loading="mutationId === 'create'" type="primary" @click="onCreate">{{ t('pass.aliasContacts.actions.create') }}</el-button>
        </div>
      </section>

      <section class="contacts-card list-card">
        <header class="card-head">
          <strong>{{ t('pass.aliasContacts.list.title') }}</strong>
          <span>{{ loading ? t('pass.aliasContacts.list.loading') : contacts.length }}</span>
        </header>
        <div class="contacts-list">
          <button
            v-for="contact in contacts"
            :key="contact.id"
            type="button"
            class="contact-row"
            :class="{ active: contact.id === activeContactId }"
            @click="activeContactId = contact.id"
          >
            <div class="card-head compact">
              <strong>{{ contact.displayName || contact.targetEmail }}</strong>
              <span>{{ formatPassTime(contact.updatedAt) }}</span>
            </div>
            <p>{{ contact.targetEmail }}</p>
            <code>{{ contact.reverseAliasEmail }}</code>
          </button>
          <el-empty v-if="!loading && contacts.length === 0" :description="t('pass.aliasContacts.list.empty')" />
        </div>
      </section>
    </div>

    <section class="contacts-card editor-card">
      <header class="card-head">
        <strong>{{ activeContact ? t('pass.aliasContacts.editor.editTitle') : t('pass.aliasContacts.editor.detailTitle') }}</strong>
        <span v-if="activeContact">{{ activeContact.reverseAliasEmail }}</span>
      </header>
      <template v-if="activeContact">
        <div class="form-grid">
          <el-input v-model="editForm.targetEmail" maxlength="254" :placeholder="t('pass.aliasContacts.fields.targetEmail')" />
          <el-input v-model="editForm.displayName" maxlength="128" :placeholder="t('pass.aliasContacts.fields.displayName')" />
          <el-input v-model="editForm.note" type="textarea" :rows="4" maxlength="500" show-word-limit :placeholder="t('pass.aliasContacts.fields.noteShort')" />
        </div>
        <div class="reverse-alias-box">
          <span class="box-label">{{ t('pass.aliasContacts.editor.reverseAlias') }}</span>
          <code>{{ activeContact.reverseAliasEmail }}</code>
        </div>
        <div class="contact-meta">
          <span>{{ t('pass.aliasContacts.editor.createdAt', { value: formatPassTime(activeContact.createdAt) }) }}</span>
          <span>{{ t('pass.aliasContacts.editor.updatedAt', { value: formatPassTime(activeContact.updatedAt) }) }}</span>
        </div>
        <div class="card-actions wrap">
          <el-button :loading="mutationId === `save:${activeContact.id}`" type="primary" @click="onSave">{{ t('pass.aliasCenter.actions.save') }}</el-button>
          <el-button plain @click="emit('compose', activeContact.reverseAliasEmail)">{{ t('pass.aliasContacts.actions.compose') }}</el-button>
          <el-button plain @click="emit('copy', activeContact.reverseAliasEmail)">{{ t('pass.aliasContacts.actions.copyReverseAlias') }}</el-button>
          <el-button :loading="mutationId === `delete:${activeContact.id}`" type="danger" @click="emit('remove', activeContact.id)">{{ t('pass.aliasContacts.actions.delete') }}</el-button>
        </div>
      </template>
      <el-empty v-else :description="t('pass.aliasContacts.editor.empty')" />
    </section>
  </section>
</template>

<style scoped>
.contacts-shell {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.contacts-head,
.card-head,
.card-actions,
.contact-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.contacts-eyebrow {
  margin: 0 0 6px;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 11px;
  color: #0f766e;
}

.contacts-head h3,
.contacts-subtitle {
  margin: 0;
}

.contacts-subtitle,
.card-head span,
.contact-row p,
.contact-meta {
  color: #667085;
  font-size: 12px;
}

.contacts-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.metric-pill,
.contacts-card,
.contact-row,
.reverse-alias-box {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
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

.metric-pill.accent {
  background: linear-gradient(135deg, rgba(20, 184, 166, 0.12), rgba(59, 130, 246, 0.12));
}

.contacts-grid {
  display: grid;
  grid-template-columns: minmax(280px, 0.85fr) minmax(0, 1.15fr);
  gap: 14px;
}

.contacts-card {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-grid,
.contacts-list {
  display: grid;
  gap: 12px;
}

.contact-row {
  padding: 14px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.contact-row:hover,
.contact-row.active {
  transform: translateY(-1px);
  border-color: rgba(15, 118, 110, 0.24);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.contact-row p,
.contact-row code {
  margin: 0;
}

.reverse-alias-box {
  padding: 14px;
  display: grid;
  gap: 6px;
  background: linear-gradient(135deg, rgba(20, 184, 166, 0.08), rgba(59, 130, 246, 0.08));
}

.box-label {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  color: #0f766e;
}

.wrap {
  flex-wrap: wrap;
}

@media (max-width: 1024px) {
  .contacts-grid {
    grid-template-columns: 1fr;
  }

  .contacts-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
