<script setup lang="ts">
import type {
  AuthenticatorAlgorithm,
  AuthenticatorCodePayload,
  AuthenticatorEntrySummary
} from '~/types/api'
import { useI18n } from '~/composables/useI18n'

interface AuthenticatorEditorState {
  issuer: string
  accountName: string
  secretCiphertext: string
  algorithm: AuthenticatorAlgorithm
  digits: number
  periodSeconds: number
  updatedAt: string
}

interface AuthenticatorCodeState extends AuthenticatorCodePayload {
  lastRefreshedAt: string
}

interface Props {
  entries: AuthenticatorEntrySummary[]
  activeEntryId: string
  editor: AuthenticatorEditorState
  codePanel: AuthenticatorCodeState
  hasActiveEntry: boolean
  countdownPercent: number
  loadingCode: boolean
  saving: boolean
  deleting: boolean
  formatTime: (value: string) => string
}

defineProps<Props>()
const emit = defineEmits<{
  selectEntry: [entryId: string]
  refreshCode: []
  copyCode: []
  save: []
  delete: []
}>()

const { t } = useI18n()
</script>

<template>
  <div class="auth-body">
    <aside class="auth-list mm-card">
      <div class="auth-list-title">{{ t('authenticator.list.title') }}</div>
      <button
        v-for="entry in entries"
        :key="entry.id"
        type="button"
        class="auth-item"
        :class="{ active: entry.id === activeEntryId }"
        @click="emit('selectEntry', entry.id)"
      >
        <div class="auth-item-head">
          <span class="auth-item-issuer">{{ entry.issuer }}</span>
          <span class="auth-item-digits">{{ entry.digits }}d</span>
        </div>
        <span class="auth-item-account">{{ entry.accountName }}</span>
        <span class="auth-item-meta">{{ entry.algorithm }} · {{ entry.periodSeconds }}s</span>
      </button>
      <div v-if="entries.length === 0" class="auth-empty">{{ t('authenticator.list.empty') }}</div>
    </aside>

    <section class="auth-editor mm-card">
      <template v-if="hasActiveEntry">
        <el-form label-position="top" class="editor-grid">
          <el-form-item :label="t('authenticator.editor.issuer')">
            <el-input v-model="editor.issuer" maxlength="128" />
          </el-form-item>
          <el-form-item :label="t('authenticator.editor.accountName')">
            <el-input v-model="editor.accountName" maxlength="254" />
          </el-form-item>
          <el-form-item :label="t('authenticator.editor.secret')">
            <el-input
              v-model="editor.secretCiphertext"
              maxlength="512"
              type="textarea"
              :rows="3"
            />
          </el-form-item>
          <div class="editor-inline">
            <el-form-item :label="t('authenticator.editor.algorithm')" class="editor-inline__item">
              <el-select v-model="editor.algorithm">
                <el-option label="SHA1" value="SHA1" />
                <el-option label="SHA256" value="SHA256" />
                <el-option label="SHA512" value="SHA512" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('authenticator.editor.digits')" class="editor-inline__item">
              <el-input-number v-model="editor.digits" :min="6" :max="8" />
            </el-form-item>
            <el-form-item :label="t('authenticator.editor.period')" class="editor-inline__item">
              <el-input-number v-model="editor.periodSeconds" :min="15" :max="120" />
            </el-form-item>
          </div>
        </el-form>

        <div class="code-panel">
          <div class="code-head">
            <div>
              <div class="code-label">{{ t('authenticator.code.label') }}</div>
              <div class="code-value">{{ codePanel.code }}</div>
            </div>
            <div class="code-actions">
              <el-button :loading="loadingCode" @click="emit('refreshCode')">
                {{ t('authenticator.code.refresh') }}
              </el-button>
              <el-button type="primary" plain @click="emit('copyCode')">
                {{ t('authenticator.code.copy') }}
              </el-button>
            </div>
          </div>
          <el-progress :percentage="countdownPercent" :stroke-width="12" :show-text="false" />
          <div class="code-meta">
            <span>{{ t('authenticator.code.expiresIn', { count: codePanel.expiresInSeconds }) }}</span>
            <span>{{ t('authenticator.code.window', { period: codePanel.periodSeconds, digits: codePanel.digits }) }}</span>
            <span>{{ t('authenticator.code.updated', { value: formatTime(codePanel.lastRefreshedAt) }) }}</span>
          </div>
        </div>

        <div class="auth-meta">
          <span>{{ t('authenticator.meta.updatedAt', { value: formatTime(editor.updatedAt) }) }}</span>
          <div class="auth-actions">
            <el-button type="primary" :loading="saving" @click="emit('save')">
              {{ t('authenticator.actions.save') }}
            </el-button>
            <el-button type="danger" :loading="deleting" @click="emit('delete')">
              {{ t('authenticator.actions.delete') }}
            </el-button>
          </div>
        </div>
      </template>
      <div v-else class="auth-empty">{{ t('authenticator.empty') }}</div>
    </section>
  </div>
</template>

<style scoped>
.auth-body {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  min-height: 620px;
  width: 100%;
}

.auth-list,
.auth-editor,
.editor-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.auth-list,
.auth-editor {
  padding: 16px;
}

.auth-item-head,
.code-head,
.code-actions,
.auth-meta,
.auth-actions,
.editor-inline {
  display: flex;
  gap: 12px;
}

.auth-list-title,
.code-label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--mm-muted);
}

.auth-item {
  width: 100%;
  padding: 12px;
  border: 1px solid rgba(15, 110, 110, 0.14);
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(240, 247, 248, 0.92));
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 6px;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.auth-item:hover,
.auth-item.active {
  border-color: rgba(15, 110, 110, 0.52);
  box-shadow: 0 20px 40px rgba(15, 110, 110, 0.12);
  transform: translateY(-1px);
}

.auth-item-head,
.code-head,
.auth-meta {
  justify-content: space-between;
  align-items: center;
}

.auth-item-issuer {
  font-weight: 600;
  color: #13222a;
}

.auth-item-digits,
.auth-item-account,
.auth-item-meta,
.code-meta,
.auth-empty,
.auth-meta {
  color: #62737b;
}

.auth-item-digits,
.auth-item-account,
.auth-item-meta,
.code-meta,
.auth-empty {
  font-size: 12px;
}

.editor-inline {
  align-items: flex-start;
}

.editor-inline__item {
  flex: 1 1 0;
  min-width: 0;
}

.code-panel {
  padding: 16px;
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(16, 24, 32, 0.02), rgba(16, 24, 32, 0.05)),
    rgba(248, 250, 252, 0.96);
  border: 1px solid rgba(15, 110, 110, 0.14);
}

.code-value {
  font-size: clamp(2rem, 4vw, 2.6rem);
  line-height: 1;
  letter-spacing: 0.18em;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  color: #0f172a;
}

.code-actions,
.auth-actions {
  flex-wrap: wrap;
}

.code-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

@media (max-width: 1100px) {
  .auth-body {
    grid-template-columns: 1fr;
  }

  .code-meta {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 840px) {
  .code-head,
  .auth-meta,
  .editor-inline {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
