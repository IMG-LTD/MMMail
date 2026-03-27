<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import type {
  PassIncomingSharedItemDetail,
  PassIncomingSharedItemSummary
} from '~/types/pass-business'
import { formatPassItemType, formatPassTime } from '~/utils/pass'

const props = defineProps<{
  items: PassIncomingSharedItemSummary[]
  selectedItemId: string
  detail: PassIncomingSharedItemDetail | null
  loading: boolean
  detailLoading: boolean
}>()

const emit = defineEmits<{
  refresh: []
  select: [itemId: string]
}>()

const { t } = useI18n()

const totalCount = computed(() => props.items.length)
const readOnlyCount = computed(() => props.items.filter((item) => item.readOnly).length)

async function copySecret(secret: string | null): Promise<void> {
  if (!secret) {
    return
  }
  try {
    await navigator.clipboard.writeText(secret)
    ElMessage.success(t('pass.sharing.incoming.messages.secretCopied'))
  } catch {
    ElMessage.error(t('pass.sharing.incoming.messages.copyFailed'))
  }
}
</script>

<template>
  <section class="incoming-panel">
    <div class="incoming-hero mm-card">
      <div>
        <p class="incoming-hero__eyebrow">{{ t('pass.sharing.incoming.eyebrow') }}</p>
        <h3>{{ t('pass.sharing.incoming.title') }}</h3>
        <p class="incoming-hero__description">{{ t('pass.sharing.incoming.description') }}</p>
      </div>
      <div class="incoming-hero__metrics">
        <article class="incoming-metric">
          <span>{{ t('pass.sharing.incoming.metrics.total') }}</span>
          <strong>{{ totalCount }}</strong>
        </article>
        <article class="incoming-metric">
          <span>{{ t('pass.sharing.incoming.metrics.readOnly') }}</span>
          <strong>{{ readOnlyCount }}</strong>
        </article>
      </div>
    </div>

    <section class="rail-card incoming-list-card" v-loading="loading">
      <header class="rail-head">
        <strong>{{ t('pass.sharing.incoming.title') }}</strong>
        <el-button plain @click="emit('refresh')">{{ t('common.actions.refresh') }}</el-button>
      </header>

      <el-empty v-if="!loading && items.length === 0" :description="t('pass.sharing.incoming.empty')" :image-size="64" />

      <div v-else class="incoming-list">
        <button
          v-for="item in items"
          :key="item.shareId"
          type="button"
          class="incoming-item"
          :class="{ active: item.itemId === selectedItemId }"
          @click="emit('select', item.itemId)"
        >
          <div class="incoming-item__header">
            <strong>{{ item.title }}</strong>
            <el-tag size="small" effect="plain">{{ formatPassItemType(item.itemType) }}</el-tag>
          </div>
          <p>{{ t('pass.sharing.incoming.owner', { value: item.ownerEmail || t('common.none') }) }}</p>
          <span>{{ t('pass.sharing.incoming.sourceVault', { value: item.sourceVaultName }) }}</span>
          <small>{{ t('pass.sharing.incoming.updatedAt', { value: formatPassTime(item.updatedAt) }) }}</small>
        </button>
      </div>
    </section>

    <section class="rail-card incoming-detail-card" v-loading="detailLoading">
      <header class="rail-head">
        <strong>{{ t('pass.sharing.incoming.detailTitle') }}</strong>
        <el-tag v-if="detail?.readOnly" size="small" effect="dark">{{ t('pass.sharing.incoming.readOnly') }}</el-tag>
      </header>

      <el-empty
        v-if="!detailLoading && !detail"
        :description="t('pass.sharing.incoming.selectHint')"
        :image-size="64"
      />

      <template v-else-if="detail">
        <div class="detail-summary">
          <h4>{{ detail.title }}</h4>
          <p>{{ formatPassItemType(detail.itemType) }}</p>
        </div>

        <div class="detail-grid">
          <article class="detail-row">
            <span>{{ t('pass.sharing.incoming.ownerLabel') }}</span>
            <strong>{{ detail.ownerEmail || t('common.none') }}</strong>
          </article>
          <article class="detail-row">
            <span>{{ t('pass.sharing.incoming.sourceVaultLabel') }}</span>
            <strong>{{ detail.sourceVaultName }}</strong>
          </article>
          <article class="detail-row">
            <span>{{ t('pass.sharing.incoming.website') }}</span>
            <strong>{{ detail.website || t('common.none') }}</strong>
          </article>
          <article class="detail-row">
            <span>{{ t('pass.sharing.incoming.username') }}</span>
            <strong>{{ detail.username || t('common.none') }}</strong>
          </article>
        </div>

        <article class="detail-secret">
          <div class="detail-secret__head">
            <span>{{ t('pass.sharing.incoming.secret') }}</span>
            <el-button plain size="small" :disabled="!detail.secretCiphertext" @click="copySecret(detail.secretCiphertext)">
              {{ t('pass.sharing.incoming.actions.copySecret') }}
            </el-button>
          </div>
          <code>{{ detail.secretCiphertext || t('common.none') }}</code>
        </article>

        <article class="detail-note">
          <span>{{ t('pass.sharing.incoming.note') }}</span>
          <p>{{ detail.note || t('common.none') }}</p>
        </article>

        <small class="detail-updated">{{ t('pass.sharing.incoming.updatedAt', { value: formatPassTime(detail.updatedAt) }) }}</small>
      </template>
    </section>
  </section>
</template>

<style scoped>
.incoming-panel,
.incoming-list,
.incoming-detail-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.incoming-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(180px, 0.8fr);
  gap: 14px;
  padding: 18px;
  background:
    radial-gradient(circle at top left, rgba(95, 247, 215, 0.18), transparent 26%),
    radial-gradient(circle at bottom right, rgba(142, 92, 255, 0.2), transparent 34%),
    linear-gradient(135deg, #08111f 0%, #10223a 52%, #0b2330 100%);
  color: #f4fbff;
}

.incoming-hero__eyebrow {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(214, 231, 255, 0.72);
}

.incoming-hero h3,
.incoming-hero__description,
.incoming-list-card strong,
.detail-summary h4,
.detail-summary p,
.detail-note p {
  margin: 0;
}

.incoming-hero__description,
.incoming-item p,
.incoming-item span,
.incoming-item small,
.detail-note p,
.detail-updated,
.detail-row span {
  color: #667085;
  line-height: 1.6;
}

.incoming-hero__description {
  color: rgba(236, 245, 255, 0.82);
}

.incoming-hero__metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.incoming-metric {
  padding: 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.08);
}

.incoming-metric span {
  display: block;
  font-size: 12px;
  color: rgba(226, 241, 255, 0.76);
}

.incoming-metric strong {
  display: block;
  margin-top: 10px;
  font-size: 22px;
}

.incoming-item {
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.95);
  padding: 14px;
  text-align: left;
  cursor: pointer;
}

.incoming-item.active {
  border-color: rgba(79, 70, 229, 0.32);
  box-shadow: 0 18px 40px rgba(79, 70, 229, 0.12);
}

.incoming-item__header,
.detail-secret__head,
.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.detail-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.detail-summary p {
  color: #667085;
}

.detail-grid {
  display: grid;
  gap: 10px;
}

.detail-row {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.95);
}

.detail-row strong,
.detail-secret code {
  color: #101828;
}

.detail-secret,
.detail-note {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(248, 250, 252, 0.95);
}

.detail-secret {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-secret code {
  word-break: break-all;
  font-family: 'JetBrains Mono', 'SFMono-Regular', monospace;
}

.detail-note {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

@media (max-width: 1080px) {
  .incoming-hero {
    grid-template-columns: 1fr;
  }

  .detail-summary,
  .detail-row,
  .detail-secret__head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
