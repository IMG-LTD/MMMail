<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { PassSecureLink } from '~/types/pass-business'
import {
  formatPassTime,
  isSecureLinkActive,
  secureLinkStatusKey,
  secureLinkStatusTone
} from '~/utils/pass'

const props = defineProps<{
  activeItemId: string
  itemTitle: string
  links: PassSecureLink[]
  loading: boolean
  mutationId: string
  sharedMode: boolean
  policyBlocked: boolean
  externalBlocked: boolean
}>()

const emit = defineEmits<{
  create: []
  copy: [publicUrl: string]
  open: [publicUrl: string]
  revoke: [linkId: string]
}>()

const { t } = useI18n()

const hasItem = computed(() => Boolean(props.activeItemId))
const totalCount = computed(() => (hasItem.value && props.sharedMode ? props.links.length : 0))
const blockedKey = computed(() => {
  if (!props.sharedMode) {
    return 'pass.secureLinks.sharedOnly'
  }
  if (!hasItem.value) {
    return 'pass.secureLinks.noItem'
  }
  if (props.policyBlocked) {
    return 'pass.secureLinks.blocked'
  }
  if (props.externalBlocked) {
    return 'pass.secureLinks.externalBlocked'
  }
  return ''
})
const canCreate = computed(() => hasItem.value && props.sharedMode && !props.policyBlocked && !props.externalBlocked)
</script>

<template>
  <section class="rail-card secure-links-card" v-loading="loading">
    <header class="rail-head">
      <div>
        <strong>{{ t('pass.secureLinks.title') }}</strong>
        <p class="secure-links-card__description">{{ t('pass.secureLinks.description') }}</p>
      </div>
      <div class="secure-links-card__actions">
        <span>{{ totalCount }}</span>
        <el-button
          type="primary"
          plain
          :disabled="!canCreate"
          :loading="mutationId === 'create'"
          @click="emit('create')"
        >
          {{ t('pass.secureLinks.actions.create') }}
        </el-button>
      </div>
    </header>

    <template v-if="sharedMode && hasItem">
      <div class="secure-links-context">
        <span>{{ t('pass.secureLinks.itemContext') }}</span>
        <strong>{{ itemTitle }}</strong>
      </div>

      <p v-if="blockedKey" class="secure-links-card__hint">{{ t(blockedKey) }}</p>

      <div v-if="links.length > 0" class="secure-links-list">
        <article v-for="link in links" :key="link.id" class="secure-link-card">
          <div class="secure-link-card__header">
            <el-tag size="small" effect="dark" :type="secureLinkStatusTone(link)">
              {{ t(secureLinkStatusKey(link)) }}
            </el-tag>
            <strong>{{ t('pass.secureLinks.viewsUsed', { current: link.currentViews, max: link.maxViews }) }}</strong>
          </div>

          <code class="secure-link-card__url">{{ link.publicUrl }}</code>

          <div class="secure-link-card__meta">
            <span>{{ t('pass.secureLinks.createdAt', { value: formatPassTime(link.createdAt) }) }}</span>
            <span>{{ t('pass.secureLinks.expiresAt', { value: formatPassTime(link.expiresAt) }) }}</span>
            <span>{{ t('pass.secureLinks.remaining', { value: link.maxViews - link.currentViews }) }}</span>
          </div>

          <div class="secure-link-card__actions">
            <el-button size="small" @click="emit('copy', link.publicUrl)">{{ t('pass.secureLinks.actions.copy') }}</el-button>
            <el-button size="small" plain @click="emit('open', link.publicUrl)">{{ t('pass.secureLinks.actions.open') }}</el-button>
            <el-button
              size="small"
              type="danger"
              plain
              :disabled="!isSecureLinkActive(link)"
              :loading="mutationId === link.id"
              @click="emit('revoke', link.id)"
            >
              {{ t('pass.secureLinks.actions.revoke') }}
            </el-button>
          </div>
        </article>
      </div>
      <el-empty v-else :description="t('pass.secureLinks.empty')" :image-size="64" />
    </template>

    <el-empty v-else :description="t(blockedKey)" :image-size="64" />
  </section>
</template>

<style scoped>
.secure-links-card,
.secure-links-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.secure-links-card__description,
.secure-links-card__hint,
.secure-link-card__meta span,
.secure-links-context span {
  margin: 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.6;
}

.secure-links-card__actions,
.secure-link-card__header,
.secure-link-card__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.secure-links-card__actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.secure-links-context,
.secure-link-card {
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.95);
  padding: 14px;
}

.secure-links-context {
  display: flex;
  flex-direction: column;
  gap: 4px;
  background: rgba(79, 70, 229, 0.06);
}

.secure-links-context strong,
.secure-link-card strong {
  color: #101828;
}

.secure-link-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.secure-link-card__header {
  justify-content: space-between;
}

.secure-link-card__url {
  display: block;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.05);
  color: #344054;
  font-size: 12px;
  word-break: break-all;
}

.secure-link-card__meta {
  display: grid;
  gap: 6px;
}

.secure-link-card__actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

@media (max-width: 1080px) {
  .secure-links-card__actions,
  .secure-link-card__header,
  .secure-link-card__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
