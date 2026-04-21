<script setup lang="ts">
import { NButton } from 'naive-ui'
import { lt, useLocaleText } from '@/locales'

const { tr } = useLocaleText()
</script>

<template>
  <section class="public-shell page-shell blocked-page">
    <article class="surface-card blocked-card">
      <span class="section-label">{{ tr(lt('访问控制', '存取控制', 'Access control')) }}</span>
      <h1 class="page-title">{{ tr(lt('当前范围未启用此产品。', '目前範圍未啟用此產品。', 'This product is not enabled for your current scope.')) }}</h1>
      <p class="page-subtitle">{{ tr(lt('当前工作区、组织策略或订阅边界阻止了此界面的访问。请切换范围或联系管理员。', '目前工作區、組織政策或訂閱邊界阻止了此介面的存取。請切換範圍或聯絡管理員。', 'The workspace, organization policy, or subscription boundary currently prevents access to this surface. Switch to a different scope or contact your administrator.')) }}</p>

      <div class="blocked-card__actions">
        <n-button type="primary">{{ tr(lt('切换到个人范围', '切換到個人範圍', 'Switch to Personal Scope')) }}</n-button>
        <n-button secondary>{{ tr(lt('申请访问', '申請存取', 'Request Access')) }}</n-button>
      </div>

      <div class="blocked-card__meta">
        <div>
          <span class="section-label">{{ tr(lt('策略', '政策', 'Policy')) }}</span>
          <strong>{{ tr(lt('企业策略已禁用此产品', '企業政策已停用此產品', 'Business policy disabled this product')) }}</strong>
        </div>
        <div>
          <span class="section-label">{{ tr(lt('下一步', '下一步', 'Next step')) }}</span>
          <strong>{{ tr(lt('查看边界或联系组织所有者', '查看邊界或聯絡組織擁有者', 'Review boundary or ask an organization owner')) }}</strong>
        </div>
      </div>
    </article>
  </section>
</template>

<style scoped>
.public-shell {
  padding: 80px 0;
}

.blocked-card {
  padding: 36px;
}

.blocked-card__actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.blocked-card__meta {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-top: 26px;
  padding-top: 22px;
  border-top: 1px solid var(--mm-border);
}

strong {
  display: block;
  margin-top: 8px;
  font-size: 14px;
  color: var(--mm-ink);
}

@media (max-width: 720px) {
  .blocked-card__actions,
  .blocked-card__meta {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
