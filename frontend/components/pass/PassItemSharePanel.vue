<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { PassItemShare } from '~/types/pass-business'
import { formatPassTime } from '~/utils/pass'

const props = defineProps<{
  activeItemId: string
  itemTitle: string
  shares: PassItemShare[]
  draftEmail: string
  loading: boolean
  mutationId: string
  policyBlocked: boolean
}>()

const emit = defineEmits<{
  'update:draft-email': [value: string]
  create: []
  remove: [shareId: string]
}>()

const { t } = useI18n()

const hasItem = computed(() => Boolean(props.activeItemId))
const canSubmit = computed(() => hasItem.value && props.draftEmail.trim().length > 0)
</script>

<template>
  <section class="rail-card item-share-card" v-loading="loading">
    <header class="rail-head">
      <div>
        <strong>{{ t('pass.sharing.direct.title') }}</strong>
        <p class="item-share-card__description">{{ t('pass.sharing.direct.description') }}</p>
      </div>
      <span>{{ shares.length }}</span>
    </header>

    <template v-if="hasItem">
      <div class="share-context">
        <span>{{ t('pass.sharing.direct.itemContext') }}</span>
        <strong>{{ itemTitle }}</strong>
      </div>

      <div class="share-form">
        <el-input
          :model-value="draftEmail"
          :placeholder="t('pass.sharing.direct.fields.email')"
          @update:model-value="emit('update:draft-email', $event)"
        />
        <el-button
          type="primary"
          :disabled="!canSubmit"
          :loading="mutationId === 'create'"
          @click="emit('create')"
        >
          {{ t('pass.sharing.direct.actions.share') }}
        </el-button>
      </div>

      <p v-if="policyBlocked" class="item-share-card__hint">{{ t('pass.sharing.direct.blocked') }}</p>

      <div v-if="shares.length > 0" class="share-list">
        <article v-for="share in shares" :key="share.id" class="share-row">
          <div>
            <strong>{{ share.collaboratorEmail }}</strong>
            <p>{{ t('pass.sharing.direct.sharedBy', { value: share.createdByEmail || t('common.none') }) }}</p>
            <span>{{ t('pass.sharing.direct.updatedAt', { value: formatPassTime(share.updatedAt) }) }}</span>
          </div>
          <el-button
            size="small"
            type="danger"
            plain
            :loading="mutationId === share.id"
            @click="emit('remove', share.id)"
          >
            {{ t('pass.sharing.direct.actions.remove') }}
          </el-button>
        </article>
      </div>
      <el-empty v-else :description="t('pass.sharing.direct.empty')" :image-size="64" />
    </template>

    <el-empty v-else :description="t('pass.sharing.direct.noItem')" :image-size="64" />
  </section>
</template>

<style scoped>
.item-share-card,
.share-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-share-card__description,
.item-share-card__hint,
.share-row p,
.share-row span,
.share-context span {
  margin: 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.6;
}

.share-context {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(99, 102, 241, 0.06);
}

.share-form,
.share-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.share-row {
  justify-content: space-between;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(248, 250, 252, 0.95);
}

.share-row strong {
  color: #101828;
}

@media (max-width: 1080px) {
  .share-form,
  .share-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
