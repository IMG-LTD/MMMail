<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { SuiteProductItem } from '~/types/api'
import { suiteProductStatusTagType } from '~/utils/suite-plans'

interface Props {
  products: SuiteProductItem[]
}

defineProps<Props>()
const { t } = useI18n()
</script>

<template>
  <section class="mm-card suite-panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-title">{{ t('suite.plans.productHubTitle') }}</h2>
        <p class="mm-muted">{{ t('suite.plans.productHubSubtitle') }}</p>
      </div>
    </div>

    <div class="product-grid">
      <article v-for="item in products" :key="item.code" class="product-card">
        <div class="product-head">
          <h3 class="mm-section-subtitle">{{ item.name }}</h3>
          <el-tag :type="suiteProductStatusTagType(item.status)">
            {{ t(`suite.productStatus.${item.status}`) }}
          </el-tag>
        </div>
        <p class="mm-muted">{{ item.description }}</p>
        <div class="product-meta">
          <span>{{ t('suite.plans.productCategory') }}: {{ item.category }}</span>
          <span>
            {{ t('suite.plans.productPlanAccess') }}:
            {{ t(item.enabledByPlan ? 'suite.plans.planAccess.enabled' : 'suite.plans.planAccess.disabled') }}
          </span>
        </div>
        <div class="highlight-list">
          <el-tag v-for="highlight in item.highlights" :key="highlight" size="small" type="info">
            {{ highlight }}
          </el-tag>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.suite-panel {
  padding: 20px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.product-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.product-card {
  border-radius: 16px;
  padding: 16px;
  border: 1px solid rgba(89, 105, 162, 0.12);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 249, 255, 0.94));
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.product-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
}

.product-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  font-size: 13px;
  color: #64708a;
}

.highlight-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 1120px) {
  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
