<script setup lang="ts">
import HostedBadge from '@/design-system/components/HostedBadge.vue'
import PremiumGate from '@/design-system/components/PremiumGate.vue'
import ProductAccessGate from '@/design-system/components/ProductAccessGate.vue'
import { lt, useLocaleText } from '@/locales'
import PublicSurfaceFrame from './PublicSurfaceFrame.vue'

const { tr } = useLocaleText()

function requestAccess() {
  window.dispatchEvent(new CustomEvent('mmmail:request-product-access', { detail: { productKey: 'command-center' } }))
}

function requestUpgrade() {
  window.dispatchEvent(new CustomEvent('mmmail:request-premium-upgrade', { detail: { productKey: 'command-center' } }))
}
</script>

<template>
  <PublicSurfaceFrame
    :eyebrow="lt('访问控制', '存取控制', 'Access control')"
    :title="lt('当前范围未启用此产品。', '目前範圍未啟用此產品。', 'This product is not enabled for your current scope.')"
    :description="lt('产品、订阅、角色和托管能力必须在公开边界中清楚说明，而不是在流程中静默失败。', '產品、訂閱、角色和代管能力必須在公開邊界中清楚說明，而不是在流程中靜默失敗。', 'Product, subscription, role, and hosted boundaries are stated publicly instead of failing silently inside a flow.')"
  >
    <section class="blocked-page">
      <article class="surface-card blocked-card">
        <div class="blocked-card__header">
          <span class="section-label">{{ tr(lt('边界详情', '邊界詳情', 'Boundary detail')) }}</span>
          <HostedBadge :label="tr(lt('Hosted optional', '代管選用', 'Hosted optional'))" />
        </div>
        <ProductAccessGate
          :enabled="false"
          product-key="Command Center"
          :title="tr(lt('此产品未在当前工作区启用', '此產品未在目前工作區啟用', 'This product is not enabled in the current workspace'))"
          :description="tr(lt('请联系组织所有者启用产品访问，或切换到有权限的工作区。', '請聯絡組織擁有者啟用產品存取，或切換到有權限的工作區。', 'Ask an organization owner to enable product access, or switch to a workspace with permission.'))"
          :action-label="tr(lt('申请访问', '申請存取', 'Request access'))"
          @request-access="requestAccess"
        />
      </article>

      <article class="surface-card blocked-card">
        <span class="section-label">{{ tr(lt('高级能力', '進階能力', 'Advanced capability')) }}</span>
        <PremiumGate
          :allowed="false"
          :title="tr(lt('Premium 能力需要升级', 'Premium 能力需要升級', 'Premium capability requires upgrade'))"
          :description="tr(lt('高级自动化、审计和托管分析属于 Premium 或 Hosted 边界。', '進階自動化、稽核和代管分析屬於 Premium 或 Hosted 邊界。', 'Advanced automation, audit, and hosted analytics belong to Premium or Hosted boundaries.'))"
          :action-label="tr(lt('查看升级路径', '查看升級路徑', 'View upgrade path'))"
          @upgrade="requestUpgrade"
        />
      </article>
    </section>
  </PublicSurfaceFrame>
</template>

<style scoped>
.blocked-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 18px;
}

.blocked-card {
  display: grid;
  align-content: start;
  gap: 18px;
  padding: 24px;
}

.blocked-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 900px) {
  .blocked-page {
    grid-template-columns: 1fr;
  }
}
</style>
