<script setup lang="ts">
type ChartCardStatus = 'neutral' | 'success' | 'info' | 'warning' | 'danger'

withDefaults(
  defineProps<{
    description?: string
    error?: string
    loading?: boolean
    status?: ChartCardStatus
    summary: string
    title: string
    trend?: string
    value?: string
  }>(),
  {
    description: undefined,
    error: undefined,
    loading: false,
    status: 'neutral',
    trend: undefined,
    value: undefined
  }
)

// vue-data-ui remains the preferred chart renderer for module-specific slots.
</script>

<template>
  <section class="chart-card" :class="[`chart-card--${status}`, { 'chart-card--loading': loading }]" :aria-label="summary">
    <header class="chart-card__header">
      <div>
        <p v-if="description" class="chart-card__description">{{ description }}</p>
        <h3>{{ title }}</h3>
      </div>
      <div v-if="value || trend" class="chart-card__metric">
        <strong v-if="value">{{ value }}</strong>
        <span v-if="trend">{{ trend }}</span>
      </div>
    </header>
    <p class="chart-card__summary">{{ summary }}</p>
    <div class="chart-card__slot" :aria-busy="loading">
      <slot v-if="!loading && !error" />
      <p v-else-if="loading" class="chart-card__loading">Loading chart data</p>
      <p v-else class="chart-card__error">{{ error }}</p>
    </div>
    <footer v-if="$slots.actions || $slots.range" class="chart-card__footer">
      <slot name="range" />
      <slot name="actions" />
    </footer>
  </section>
</template>

<style scoped>
.chart-card {
  display: grid;
  gap: 14px;
  min-height: 260px;
  padding: 18px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-md);
  background: var(--mm-surface);
  box-shadow: var(--mm-shadow-sm);
}

.chart-card__header,
.chart-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.chart-card__description,
.chart-card__summary {
  margin: 0;
  color: var(--mm-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.chart-card__header h3 {
  margin: 3px 0 0;
  color: var(--mm-text-primary);
  font-size: 15px;
  letter-spacing: 0;
}

.chart-card__metric {
  display: grid;
  justify-items: end;
  gap: 2px;
}

.chart-card__metric strong {
  font-size: 20px;
  line-height: 1;
}

.chart-card__metric span {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.chart-card__slot {
  display: grid;
  place-items: center;
  min-height: 150px;
  border-radius: var(--mm-radius-sm);
  background: var(--mm-surface-soft);
}

.chart-card__loading,
.chart-card__error {
  margin: 0;
  color: var(--mm-text-secondary);
  font-size: 13px;
}

.chart-card__error {
  color: var(--mm-danger);
}

.chart-card--success {
  border-color: color-mix(in srgb, var(--mm-success) 22%, var(--mm-border));
}

.chart-card--info {
  border-color: color-mix(in srgb, var(--mm-info) 22%, var(--mm-border));
}

.chart-card--warning {
  border-color: color-mix(in srgb, var(--mm-warning) 24%, var(--mm-border));
}

.chart-card--danger {
  border-color: color-mix(in srgb, var(--mm-danger) 22%, var(--mm-border));
}

.chart-card--loading .chart-card__slot {
  background: repeating-linear-gradient(
    90deg,
    var(--mm-surface-soft),
    var(--mm-surface-soft) 16px,
    var(--mm-surface-muted) 16px,
    var(--mm-surface-muted) 32px
  );
}
</style>
