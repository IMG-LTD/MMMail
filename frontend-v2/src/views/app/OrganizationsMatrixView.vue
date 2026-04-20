<script setup lang="ts">
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'

const tabs = ['Overview', 'Member Management', 'Product Access Matrix']
const products = ['Mail', 'Calendar', 'Drive', 'Pass', 'Docs', 'Sheets']

const units = [
  { name: 'Executive Board', access: [true, true, true, false, true, true] },
  { name: 'Core Management', access: [true, true, true, false, true, true] },
  { name: 'R&D / EMEA', access: [true, true, true, false, true, false] },
  { name: 'External Contractors', access: [true, false, false, false, false, false] }
]
</script>

<template>
  <section class="page-shell surface-grid organizations-page">
    <compact-page-header
      eyebrow="Organizations"
      title="Phoenix Initiative"
      description="Global strategic product access priorities and product visibility controls across EMEA and APAC regions."
      badge="Global Scope"
    >
      <button class="organizations-page__action" type="button">Edit Organization</button>
    </compact-page-header>

    <div class="organizations-tabs">
      <button
        v-for="tab in tabs"
        :key="tab"
        type="button"
        :class="{ 'organizations-tabs__active': tab === 'Product Access Matrix' }"
      >
        {{ tab }}
      </button>
    </div>

    <article class="surface-card organizations-alert">
      <span class="organizations-alert__dot" />
      <div>
        <strong>Global Product Visibility</strong>
        <p class="page-subtitle">
          Disabled products will be hidden organization-wide, and direct access attempts will be blocked. This setting overrides individual user preferences.
        </p>
      </div>
    </article>

    <article class="surface-card organizations-matrix">
      <div class="organizations-matrix__head">
        <span>Organizational Unit / Role</span>
        <span v-for="product in products" :key="product">{{ product }}</span>
      </div>

      <div v-for="unit in units" :key="unit.name" class="organizations-matrix__row">
        <strong>{{ unit.name }}</strong>
        <span
          v-for="(enabled, index) in unit.access"
          :key="`${unit.name}-${products[index]}`"
          class="organizations-matrix__cell"
        >
          <span
            class="organizations-matrix__toggle"
            :class="{ 'organizations-matrix__toggle--active': enabled }"
          >
            {{ enabled ? '✓' : '' }}
          </span>
        </span>
      </div>
    </article>

    <div class="organizations-page__footer">
      <button class="organizations-page__save" type="button">Save Changes</button>
    </div>
  </section>
</template>

<style scoped>
.organizations-page__action {
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  background: var(--mm-card);
}

.organizations-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-bottom: 4px;
  border-bottom: 1px solid var(--mm-border);
}

.organizations-tabs button {
  min-height: 36px;
  padding: 0 4px;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: var(--mm-text-secondary);
}

.organizations-tabs__active {
  border-bottom-color: var(--mm-primary) !important;
  color: var(--mm-ink) !important;
  font-weight: 600;
}

.organizations-alert {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  padding: 16px 18px;
  background: var(--mm-card-muted);
}

.organizations-alert__dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 999px;
  background: var(--mm-primary);
}

.organizations-alert strong {
  display: block;
  margin-bottom: 6px;
  color: var(--mm-ink);
}

.organizations-matrix {
  overflow: hidden;
}

.organizations-matrix__head,
.organizations-matrix__row {
  display: grid;
  grid-template-columns: minmax(220px, 1.4fr) repeat(6, minmax(72px, 0.8fr));
  gap: 12px;
  align-items: center;
  padding: 16px 18px;
}

.organizations-matrix__head {
  background: var(--mm-card-muted);
  color: var(--mm-text-secondary);
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.organizations-matrix__row {
  border-top: 1px solid var(--mm-border);
}

.organizations-matrix__row strong {
  font-size: 13px;
  color: var(--mm-ink);
}

.organizations-matrix__cell {
  display: flex;
  justify-content: center;
}

.organizations-matrix__toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border: 1px solid var(--mm-border-strong);
  border-radius: 4px;
  color: transparent;
  font-size: 11px;
  font-weight: 700;
}

.organizations-matrix__toggle--active {
  border-color: var(--mm-primary);
  background: var(--mm-primary);
  color: #fff;
}

.organizations-page__footer {
  display: flex;
  justify-content: end;
}

.organizations-page__save {
  min-height: 36px;
  padding: 0 16px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(180deg, var(--mm-primary) 0%, var(--mm-primary-pressed) 100%);
  color: #fff;
  font-weight: 600;
}

@media (max-width: 960px) {
  .organizations-matrix {
    overflow-x: auto;
  }

  .organizations-matrix__head,
  .organizations-matrix__row {
    min-width: 760px;
  }
}
</style>
