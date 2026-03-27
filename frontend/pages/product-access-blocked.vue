<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import { useAuthStore } from '~/stores/auth'
import { useOrgAccessStore } from '~/stores/org-access'
import {
  ACCOUNT_MAIL_ADDRESS_REQUIRED_REASON,
  buildAuthenticatorRecoveryQuery,
  ORG_TWO_FACTOR_REASON,
  resolveBlockedRecoveryContext
} from '~/utils/org-access-recovery'
import {
  resolveHomeRoute,
  resolveProductLabelKey,
  resolveProductKeyFromPath
} from '~/utils/org-product-access'
import type { OrgProductKey } from '~/types/organization-admin'

const route = useRoute()
const authStore = useAuthStore()
const orgAccessStore = useOrgAccessStore()
const { t } = useI18n()
const recoveryContext = computed(() => resolveBlockedRecoveryContext(route.query))

const productKey = computed<OrgProductKey | null>(() => {
  return recoveryContext.value.productKey
    || resolveProductKeyFromPath(`/${String(route.query.productKey || '').toLowerCase()}`)
})

const productLabel = computed(() => {
  if (!productKey.value) {
    return ''
  }
  return t(resolveProductLabelKey(productKey.value))
})

const organizationLabel = computed(() => (
  recoveryContext.value.orgName
  || orgAccessStore.activeScope?.orgName
  || t('orgAccess.scope.personal')
))
const isTwoFactorBlocked = computed(() => recoveryContext.value.reason === ORG_TWO_FACTOR_REASON)
const isAccountBlocked = computed(() => recoveryContext.value.reason === ACCOUNT_MAIL_ADDRESS_REQUIRED_REASON)
const availableHomeRoute = computed(() => resolveHomeRoute(orgAccessStore.isProductEnabled, authStore.user?.mailAddressMode))
const pageTitle = computed(() => {
  const params = {
    product: productLabel.value || t('orgAccess.blocked.productFallback'),
    organization: organizationLabel.value
  }
  return isTwoFactorBlocked.value
    ? t('orgAccess.blocked.twoFactorTitle', params)
    : isAccountBlocked.value
      ? t('externalAccount.blocked.title', params)
      : t('orgAccess.blocked.title', params)
})
const pageDescription = computed(() => (
  isTwoFactorBlocked.value
    ? t('orgAccess.blocked.twoFactorDescription')
    : isAccountBlocked.value
      ? t('externalAccount.blocked.description')
      : t('orgAccess.blocked.description')
))
const eyebrow = computed(() => (
  isTwoFactorBlocked.value
    ? t('orgAccess.blocked.twoFactorEyebrow')
    : isAccountBlocked.value
      ? t('externalAccount.blocked.eyebrow')
      : t('orgAccess.blocked.eyebrow')
))
const currentAccountModeLabel = computed(() => (
  authStore.user?.mailAddressMode === 'EXTERNAL_ACCOUNT'
    ? t('externalAccount.settings.mode.externalAccount')
    : t('externalAccount.settings.mode.protonAddress')
))

useHead(() => ({
  title: pageTitle.value
}))

async function switchToPersonalScope(target = availableHomeRoute.value): Promise<void> {
  orgAccessStore.setPersonalScope()
  await navigateTo(target)
}

async function setupTwoFactor(): Promise<void> {
  orgAccessStore.setPersonalScope()
  await navigateTo({
    path: '/authenticator',
    query: buildAuthenticatorRecoveryQuery(recoveryContext.value)
  })
}

async function openSettings(): Promise<void> {
  await navigateTo('/settings')
}
</script>

<template>
  <div class="mm-page blocked-page">
    <section class="mm-card blocked-card">
      <p class="blocked-eyebrow">{{ eyebrow }}</p>
      <h1 class="blocked-title">{{ pageTitle }}</h1>
      <p class="blocked-description">{{ pageDescription }}</p>

      <div class="blocked-meta">
        <span class="blocked-meta-label">{{ t('orgAccess.blocked.activeScope') }}</span>
        <el-tag effect="plain" type="info">{{ organizationLabel }}</el-tag>
        <template v-if="isAccountBlocked">
          <span class="blocked-meta-label">{{ t('externalAccount.blocked.accountMode') }}</span>
          <el-tag effect="plain" type="warning">{{ currentAccountModeLabel }}</el-tag>
        </template>
      </div>

      <div class="blocked-actions">
        <el-button v-if="isTwoFactorBlocked" type="primary" @click="setupTwoFactor">
          {{ t('orgAccess.blocked.twoFactorSetup') }}
        </el-button>
        <el-button v-else-if="isAccountBlocked" type="primary" @click="openSettings">
          {{ t('externalAccount.blocked.openSettings') }}
        </el-button>
        <el-button v-else type="primary" @click="switchToPersonalScope(recoveryContext.from)">
          {{ t('orgAccess.blocked.switchPersonal') }}
        </el-button>
        <el-button v-if="isTwoFactorBlocked" @click="switchToPersonalScope()">
          {{ t('orgAccess.blocked.twoFactorStayPersonal') }}
        </el-button>
        <el-button v-else-if="isAccountBlocked" @click="navigateTo(availableHomeRoute)">
          {{ t('externalAccount.blocked.goAvailableProducts') }}
        </el-button>
        <el-button v-else @click="navigateTo('/organizations')">
          {{ t('orgAccess.blocked.goOrganizations') }}
        </el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.blocked-page {
  display: flex;
  justify-content: center;
  padding-top: 48px;
}

.blocked-card {
  width: min(720px, 100%);
  padding: 32px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(15, 110, 110, 0.08), transparent 42%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(245, 249, 250, 0.96));
}

.blocked-eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--mm-primary-dark);
}

.blocked-title {
  margin: 0;
  font-size: 32px;
  line-height: 1.15;
}

.blocked-description {
  margin: 0;
  color: var(--mm-muted);
  line-height: 1.6;
}

.blocked-meta,
.blocked-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.blocked-meta-label {
  font-size: 13px;
  color: var(--mm-muted);
}
</style>
