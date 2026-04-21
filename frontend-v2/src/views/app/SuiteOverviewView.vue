<script setup lang="ts">
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'

const tabs = ['Overview', 'Plans', 'Billing', 'Operations', 'Boundary']

const products = [
  ['Mail', '1,204', 'Unread'],
  ['Calendar', '4', 'Today'],
  ['Drive', '45GB', 'Storage'],
  ['Pass', '3', 'Audit']
]
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      eyebrow="Suite"
      title="Suite Overview"
      description="Mail-first operating summary filtered to the products enabled in the current scope."
    />

    <div class="suite-tabs">
      <button v-for="tab in tabs" :key="tab" type="button" :class="{ 'suite-tabs__active': tab === 'Overview' }">
        {{ tab }}
      </button>
    </div>

    <div class="suite-grid">
      <article class="surface-card suite-mainline">
        <span class="section-label">Core workflow integration</span>
        <div class="suite-mainline__chain">
          <div class="suite-mainline__node">Mail</div>
          <span>→</span>
          <div class="suite-mainline__node">Calendar</div>
          <span>→</span>
          <div class="suite-mainline__node">Drive</div>
          <span>→</span>
          <div class="suite-mainline__node">Pass</div>
        </div>
      </article>

      <aside class="suite-side">
        <article class="surface-card suite-side__card">
          <span class="section-label">Global metrics</span>
          <div class="suite-side__row"><span>Storage used</span><strong>45GB / 100GB</strong></div>
          <div class="suite-side__row"><span>Active sessions</span><strong>3</strong></div>
          <div class="suite-side__row"><span>Security score</span><strong class="suite-side__score">92 / 100</strong></div>
        </article>
        <article class="surface-card suite-side__card">
          <span class="section-label">Intelligence</span>
          <strong>Storage optimization</strong>
          <p class="page-subtitle">We noticed 5.0GB of duplicate attachments in your Mail storage. These can be safely reclaimed.</p>
          <button type="button">Review duplicates</button>
        </article>
      </aside>
    </div>

    <div class="suite-products">
      <article v-for="[name, value, label] in products" :key="name" class="surface-card suite-product">
        <span class="section-label">{{ name }}</span>
        <strong>{{ value }}</strong>
        <p class="page-subtitle">{{ label }}</p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.suite-tabs {
  display: flex;
  gap: 18px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--mm-border);
}

.suite-tabs button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.suite-tabs__active {
  color: var(--mm-ink) !important;
  font-weight: 600;
}

.suite-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 16px;
}

.suite-mainline {
  padding: 24px;
}

.suite-mainline__chain {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 26px;
}

.suite-mainline__node {
  display: inline-grid;
  place-items: center;
  min-width: 108px;
  min-height: 78px;
  border: 1px solid var(--mm-border);
  border-radius: 14px;
  background: #fafbfc;
  font-size: 13px;
  font-weight: 600;
}

.suite-side {
  display: grid;
  gap: 16px;
}

.suite-side__card {
  padding: 18px;
}

.suite-side__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 12px;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.suite-side__score {
  color: #2a8a57;
}

.suite-side__card button {
  min-height: 30px;
  margin-top: 14px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 8px;
  background: #fff;
}

.suite-products {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.suite-product {
  padding: 18px;
}

.suite-product strong {
  display: block;
  margin: 10px 0 6px;
  font-size: 24px;
  line-height: 1;
}

@media (max-width: 960px) {
  .suite-grid,
  .suite-products {
    grid-template-columns: 1fr;
  }

  .suite-mainline__chain {
    flex-wrap: wrap;
  }
}
</style>
