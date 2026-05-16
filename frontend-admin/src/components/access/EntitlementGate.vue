<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/store/modules/auth';
import { $t } from '@/locales';

defineOptions({
  name: 'EntitlementGate'
});

interface BackendEntitlementDetail {
  requiredEdition?: string;
  currentEdition?: string;
  upgradeAction?: string;
}

interface Props {
  roles?: string[];
  role?: string;
  requires?: string[];
  anyOf?: string[];
  featureFlag?: string;
  orgRequired?: boolean;
  fallback?: Api.Auth.AccessFallback;
  backendEntitlement?: BackendEntitlementDetail | null;
  requiredEdition?: string;
  currentEdition?: string;
  upgradeAction?: string;
}

const props = withDefaults(defineProps<Props>(), {
  roles: () => [],
  role: '',
  requires: () => [],
  anyOf: () => [],
  featureFlag: '',
  orgRequired: false,
  fallback: 'upgrade',
  backendEntitlement: null,
  requiredEdition: '',
  currentEdition: '',
  upgradeAction: ''
});

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();

const primaryActionDestinations: Record<Api.Auth.AccessFallback, string> = {
  upgrade: '/admin/billing',
  'contact-sales': '/admin/billing',
  trial: '/admin/billing',
  forbidden: '/home'
};

const secondaryActionDestinations: Record<Api.Auth.AccessFallback, string> = {
  upgrade: '/pricing',
  'contact-sales': '/home',
  trial: '/home',
  forbidden: '/home'
};

const mergedAccessMeta = computed<Api.Auth.AccessDecisionMeta>(() => ({
  roles: props.roles.length ? props.roles : route.meta.roles || [],
  role: props.role || route.meta.role || '',
  requires: props.requires.length ? props.requires : route.meta.requires || [],
  anyOf: props.anyOf.length ? props.anyOf : route.meta.anyOf || [],
  featureFlag: props.featureFlag || route.meta.featureFlag || '',
  orgRequired: props.orgRequired || Boolean(route.meta.orgRequired),
  fallback: props.fallback || route.meta.fallback || 'upgrade'
}));

const canAccess = computed(() => authStore.canAccess(mergedAccessMeta.value));
const fallbackType = computed<Api.Auth.AccessFallback>(() => resolveFallbackType());
const fallbackTitleKey = computed(() => `page.accessGate.${fallbackType.value}.title` as App.I18n.I18nKey);
const fallbackDescriptionKey = computed(() => `page.accessGate.${fallbackType.value}.description` as App.I18n.I18nKey);
const primaryActionKey = computed(() => `page.accessGate.${fallbackType.value}.primary` as App.I18n.I18nKey);
const secondaryActionKey = computed(() => `page.accessGate.${fallbackType.value}.secondary` as App.I18n.I18nKey);
const backendEntitlement = computed(() => ({
  requiredEdition: props.requiredEdition || props.backendEntitlement?.requiredEdition || '',
  currentEdition: props.currentEdition || props.backendEntitlement?.currentEdition || '',
  upgradeAction: props.upgradeAction || props.backendEntitlement?.upgradeAction || ''
}));
const backendEntitlementRows = computed(() =>
  [
    {
      key: 'requiredEdition',
      labelKey: 'page.accessGate.backendEntitlement.requiredEdition' as App.I18n.I18nKey,
      value: backendEntitlement.value.requiredEdition
    },
    {
      key: 'currentEdition',
      labelKey: 'page.accessGate.backendEntitlement.currentEdition' as App.I18n.I18nKey,
      value: backendEntitlement.value.currentEdition
    },
    {
      key: 'upgradeAction',
      labelKey: 'page.accessGate.backendEntitlement.upgradeAction' as App.I18n.I18nKey,
      value: backendEntitlement.value.upgradeAction
    }
  ].filter(row => row.value)
);
const hasBackendEntitlement = computed(() => backendEntitlementRows.value.length > 0);

function resolveFallbackType(): Api.Auth.AccessFallback {
  if (mergedAccessMeta.value.orgRequired && !authStore.currentOrgId) {
    return 'forbidden';
  }
  if (mergedAccessMeta.value.role && !authStore.userInfo.roles.includes(mergedAccessMeta.value.role)) {
    return 'forbidden';
  }
  return mergedAccessMeta.value.fallback || 'upgrade';
}

function handlePrimaryAction() {
  router.push(primaryActionDestinations[fallbackType.value]);
}

function handleSecondaryAction() {
  if (fallbackType.value === 'forbidden') {
    window.$message?.info($t('page.accessGate.forbidden.secondary'));
    return;
  }
  router.push(secondaryActionDestinations[fallbackType.value]);
}
</script>

<template>
  <slot v-if="canAccess" />
  <slot v-else :name="fallbackType">
    <NResult
      :status="fallbackType === 'forbidden' ? '403' : 'info'"
      :title="$t(fallbackTitleKey)"
      :description="$t(fallbackDescriptionKey)"
    >
      <template #footer>
        <dl v-if="hasBackendEntitlement" class="entitlement-gate__backend-entitlement">
          <div v-for="row in backendEntitlementRows" :key="row.key" class="entitlement-gate__detail-row">
            <dt>{{ $t(row.labelKey) }}</dt>
            <dd>{{ row.value }}</dd>
          </div>
        </dl>
        <NSpace justify="center">
          <NButton type="primary" @click="handlePrimaryAction">
            {{ $t(primaryActionKey) }}
          </NButton>
          <NButton @click="handleSecondaryAction">
            {{ $t(secondaryActionKey) }}
          </NButton>
        </NSpace>
      </template>
    </NResult>
  </slot>
</template>

<style scoped>
.entitlement-gate__backend-entitlement {
  display: grid;
  gap: 8px;
  margin: 0 0 16px;
  padding: 12px;
  border: 1px solid var(--n-border-color, #e5e7eb);
  border-radius: 8px;
  text-align: left;
}

.entitlement-gate__detail-row {
  display: grid;
  grid-template-columns: minmax(120px, 0.34fr) 1fr;
  gap: 12px;
  align-items: start;
}

.entitlement-gate__detail-row dt {
  color: var(--n-text-color-3, #6b7280);
}

.entitlement-gate__detail-row dd {
  min-width: 0;
  margin: 0;
  word-break: break-word;
}
</style>
