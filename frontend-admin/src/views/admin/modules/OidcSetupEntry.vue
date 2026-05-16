<script setup lang="ts">
import { NAlert, NButton, NCard, NSpace } from 'naive-ui';
import EntitlementGate from '@/components/access/EntitlementGate.vue';

defineOptions({
  name: 'OidcSetupEntry'
});

const oidcEntitlement = {
  requiredEdition: 'BUSINESS',
  currentEdition: 'UNKNOWN',
  upgradeAction: 'contact-sales'
} as const;
</script>

<template>
  <NCard class="card-wrapper" :title="$t('page.oidc.title')">
    <div data-testid="oidc-a11y-scope">
      <EntitlementGate :requires="['oidc.sso']" fallback="contact-sales" :backend-entitlement="oidcEntitlement">
        <NSpace vertical :size="12">
          <NAlert type="warning" :title="$t('page.oidc.unavailableTitle')">
            {{ $t('page.oidc.unavailableDescription') }}
          </NAlert>
          <NButton disabled>
            {{ $t('page.oidc.configure') }}
          </NButton>
        </NSpace>
      </EntitlementGate>
    </div>
  </NCard>
</template>
