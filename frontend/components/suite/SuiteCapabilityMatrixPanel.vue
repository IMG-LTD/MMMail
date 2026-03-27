<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { SuitePlan } from '~/types/suite-lumo'

interface ProductColumn {
  code: string
  name: string
}

interface Props {
  plans: SuitePlan[]
  productColumns: ProductColumn[]
}

defineProps<Props>()
const { t } = useI18n()
</script>

<template>
  <section class="mm-card suite-panel">
    <h2 class="mm-section-title">{{ t('suite.plans.matrixTitle') }}</h2>
    <el-table :data="plans" style="width: 100%; margin-top: 14px">
      <el-table-column :label="t('suite.plans.matrixPlan')" min-width="220" fixed="left">
        <template #default="scope">
          <div class="plan-cell">
            <strong>{{ scope.row.name }}</strong>
            <div class="plan-cell-meta">
              <el-tag effect="plain" size="small">{{ scope.row.code }}</el-tag>
              <el-tag effect="plain" size="small">
                {{ t(`suite.plans.segment.${scope.row.segment}.title`) }}
              </el-tag>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        v-for="product in productColumns"
        :key="product.code"
        :label="product.name"
        min-width="120"
      >
        <template #default="scope">
          <el-tag :type="scope.row.enabledProducts.includes(product.code) ? 'success' : 'info'">
            {{ t(scope.row.enabledProducts.includes(product.code) ? 'suite.plans.matrix.included' : 'suite.plans.matrix.excluded') }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
.suite-panel {
  padding: 20px;
}

.plan-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.plan-cell-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
