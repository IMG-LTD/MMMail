<script setup lang="ts">
const vaults = ['Personal', 'Acme Corp', 'Family']
const categories = ['Logins', 'Cards', 'Secure Notes']

const entries = [
  ['Google', 'user@example.com'],
  ['GitHub', 'dev_ninja'],
  ['Chase Bank', 'Personal Checking']
]

const fields = [
  ['Username', 'dev_ninja'],
  ['Password', '••••••••••••'],
  ['One-time Password', '284 491']
]
</script>

<template>
  <section class="pass-page">
    <div class="pass-page__layout">
      <aside class="pass-rail">
        <div class="pass-rail__brand">
          <span class="section-label">Enterprise Suite</span>
          <strong>Vaults</strong>
        </div>

        <div class="pass-rail__group">
          <span class="section-label">Vaults</span>
          <button
            v-for="vault in vaults"
            :key="vault"
            type="button"
            :class="{ 'pass-rail__item--active': vault === 'Personal' }"
          >
            {{ vault }}
          </button>
        </div>

        <div class="pass-rail__group">
          <span class="section-label">Categories</span>
          <button
            v-for="category in categories"
            :key="category"
            type="button"
            :class="{ 'pass-rail__item--active': category === 'Logins' }"
          >
            {{ category }}
          </button>
        </div>

        <button class="pass-rail__primary" type="button">+ New Item</button>
      </aside>

      <section class="pass-vault-list">
        <header class="pass-vault-list__head">
          <strong>Personal Vault</strong>
          <span>42 items</span>
        </header>

        <article
          v-for="entry in entries"
          :key="entry[0]"
          class="pass-vault-list__item"
          :class="{ 'pass-vault-list__item--active': entry[0] === 'GitHub' }"
        >
          <span class="pass-vault-list__avatar">{{ entry[0][0] }}</span>
          <div>
            <strong>{{ entry[0] }}</strong>
            <span>{{ entry[1] }}</span>
          </div>
          <span>›</span>
        </article>
      </section>

      <section class="pass-records">
        <header class="pass-records__head">
          <span class="section-label">Selected</span>
          <strong>GitHub</strong>
          <span>github.com</span>
        </header>

        <article class="pass-records__card">
          <span class="section-label">Tags</span>
          <div class="pass-records__tags">
            <span>Work</span>
            <span>Development</span>
          </div>
        </article>

        <article class="pass-records__card">
          <span class="section-label">Notes</span>
          <p>
            Used for the main production repository. Contact Justin if credentials need rotation. Last rotated Oct 12, 2023.
          </p>
        </article>
      </section>

      <aside class="pass-detail">
        <div class="pass-detail__head">
          <span class="pass-detail__icon">GH</span>
          <div>
            <strong>GitHub</strong>
            <span>github.com</span>
          </div>
        </div>

        <div class="pass-detail__field" v-for="field in fields" :key="field[0]">
          <span class="section-label">{{ field[0] }}</span>
          <strong>{{ field[1] }}</strong>
        </div>

        <article class="pass-detail__note">
          <span class="section-label">Secure Notes</span>
          <p>
            Used for the main production repository. Contact Justin if credentials need rotation. Last rotated Oct 12, 2023.
          </p>
        </article>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.pass-page {
  min-height: calc(100vh - 56px);
  background: var(--mm-card);
}

.pass-page__layout {
  display: grid;
  grid-template-columns: 190px 220px 240px minmax(280px, 1fr);
  min-height: calc(100vh - 56px);
}

.pass-rail,
.pass-vault-list,
.pass-records,
.pass-detail {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 18px 16px;
}

.pass-rail,
.pass-vault-list,
.pass-records {
  border-right: 1px solid var(--mm-border);
}

.pass-rail {
  background: var(--mm-side-surface);
}

.pass-rail__brand,
.pass-vault-list__head,
.pass-records__head {
  display: grid;
  gap: 6px;
}

.pass-rail__group {
  display: grid;
  gap: 6px;
}

.pass-rail__group button,
.pass-rail__primary {
  min-height: 34px;
  padding: 0 10px;
  border-radius: 10px;
}

.pass-rail__group button {
  border: 1px solid transparent;
  background: transparent;
  color: var(--mm-text-secondary);
  text-align: left;
}

.pass-rail__item--active {
  border-color: var(--mm-accent-border) !important;
  background: var(--mm-accent-soft) !important;
  color: var(--mm-primary) !important;
  font-weight: 600;
}

.pass-rail__primary {
  margin-top: auto;
  border: 0;
  background: linear-gradient(180deg, #1f2937 0%, #111827 100%);
  color: #fff;
  font-weight: 600;
}

.pass-vault-list__head span,
.pass-records__head span,
.pass-vault-list__item span:last-child {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.pass-vault-list__item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 14px;
  border: 1px solid var(--mm-border);
  border-radius: 14px;
  background: var(--mm-card);
}

.pass-vault-list__item--active {
  border-color: var(--mm-pass);
  background: rgba(228, 123, 57, 0.08);
}

.pass-vault-list__avatar,
.pass-detail__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: rgba(228, 123, 57, 0.18);
  color: var(--mm-pass);
  font-weight: 700;
}

.pass-vault-list__item strong,
.pass-detail__head strong,
.pass-detail__field strong {
  display: block;
  color: var(--mm-ink);
}

.pass-vault-list__item div span,
.pass-detail__head span:last-child {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.pass-records__card,
.pass-detail__field,
.pass-detail__note {
  display: grid;
  gap: 10px;
  padding: 16px;
  border: 1px solid var(--mm-border);
  border-radius: 14px;
  background: var(--mm-card);
}

.pass-records__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pass-records__tags span {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--mm-card-muted);
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.pass-records__card p,
.pass-detail__note p {
  margin: 0;
  color: var(--mm-text-secondary);
  font-size: 13px;
  line-height: 1.65;
}

.pass-detail {
  background: linear-gradient(180deg, rgba(248, 249, 250, 0.7) 0%, var(--mm-card) 100%);
}

.pass-detail__head {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

@media (max-width: 1180px) {
  .pass-page__layout {
    grid-template-columns: 190px 220px minmax(0, 1fr);
  }

  .pass-records {
    display: none;
  }
}

@media (max-width: 900px) {
  .pass-page__layout {
    grid-template-columns: 1fr;
  }

  .pass-rail,
  .pass-vault-list {
    border-right: 0;
    border-bottom: 1px solid var(--mm-border);
  }
}
</style>
