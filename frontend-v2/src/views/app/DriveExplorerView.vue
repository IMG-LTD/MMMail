<script setup lang="ts">
const leftNav = ['My Drive', 'Shared with me', 'Recent', 'Starred', 'Trash']

const files = [
  ['Q3_Strategy_Deck.pdf', '2.4 MB', 'Oct 24'],
  ['handoff-spec.doc', '1.6 MB', 'Oct 20'],
  ['crew-k.xlsx', '450 KB', 'Oct 15'],
  ['mi', '12 KB', 'Oct 12']
]

const properties = [
  ['Type', 'PDF Document'],
  ['Size', '2.4 MB'],
  ['Location', 'My Drive / Q3 Planning'],
  ['Owner', 'Sarah Jenkins']
]
</script>

<template>
  <section class="drive-page">
    <div class="drive-page__layout">
      <aside class="drive-nav">
        <button class="drive-nav__primary" type="button">+ New</button>
        <div class="drive-nav__group">
          <button
            v-for="item in leftNav"
            :key="item"
            type="button"
            :class="{ 'drive-nav__item--active': item === 'My Drive' }"
          >
            {{ item }}
          </button>
        </div>

        <div class="drive-nav__storage">
          <strong>11.9 GB / 15 GB</strong>
          <span class="drive-nav__meter"><span /></span>
          <span>Storage Full</span>
        </div>
      </aside>

      <div class="drive-main">
        <header class="drive-main__toolbar">
          <div class="drive-main__chips">
            <span class="metric-chip">My Drive</span>
            <span class="metric-chip">93 Live Planning</span>
            <span class="metric-chip">End-to-end Encrypted</span>
          </div>
          <div class="drive-main__actions">
            <button type="button">Grid</button>
            <button type="button">Sort</button>
          </div>
        </header>

        <div class="drive-main__table-head">
          <span>Name</span>
          <span>Owner</span>
          <span>Size</span>
          <span>Modified</span>
        </div>

        <div class="drive-main__table">
          <div
            v-for="file in files"
            :key="file[0]"
            class="drive-main__row"
            :class="{ 'drive-main__row--active': file[0] === 'Q3_Strategy_Deck.pdf' }"
          >
            <strong>{{ file[0] }}</strong>
            <span>me</span>
            <span>{{ file[1] }}</span>
            <span>{{ file[2] }}</span>
          </div>
        </div>
      </div>

      <aside class="drive-detail">
        <div class="drive-detail__head">
          <strong>Project_Proposal.pdf</strong>
          <span>×</span>
        </div>

        <div class="drive-detail__preview">
          <span>Preview</span>
        </div>

        <div class="drive-detail__tabs">
          <button class="drive-detail__tabs--active" type="button">Details</button>
          <button type="button">Activity</button>
        </div>

        <div class="drive-detail__properties">
          <div v-for="[label, value] in properties" :key="label">
            <span class="section-label">{{ label }}</span>
            <strong>{{ value }}</strong>
          </div>
        </div>

        <article class="drive-detail__card">
          <span class="section-label">Shared With</span>
          <strong>Sarah Jenkins</strong>
          <span>Editor</span>
          <button type="button">Manage Access</button>
        </article>

        <article class="drive-detail__card">
          <span class="section-label">Version</span>
          <strong>Version 3 (Current)</strong>
          <span>Today, 10:42 AM</span>
        </article>
      </aside>

      <div class="drive-toast surface-card">
        <span class="section-label">1 Upload Complete</span>
        <strong>Q4 Strategy_Deck.pdf</strong>
        <span>Encrypted & uploaded</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.drive-page {
  min-height: calc(100vh - 56px);
  background: var(--mm-card);
}

.drive-page__layout {
  position: relative;
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr) 280px;
  min-height: calc(100vh - 56px);
}

.drive-nav,
.drive-detail {
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 18px 16px;
  background: var(--mm-side-surface);
}

.drive-nav {
  border-right: 1px solid var(--mm-border);
}

.drive-detail {
  border-left: 1px solid var(--mm-border);
  background: var(--mm-card);
}

.drive-nav__primary,
.drive-main__actions button,
.drive-detail__card button {
  min-height: 36px;
  padding: 0 14px;
  border-radius: 10px;
}

.drive-nav__primary {
  border: 0;
  background: linear-gradient(180deg, #111827 0%, #0f172a 100%);
  color: #fff;
  font-weight: 600;
}

.drive-nav__group {
  display: grid;
  gap: 6px;
}

.drive-nav__group button {
  min-height: 34px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: var(--mm-text-secondary);
  text-align: left;
}

.drive-nav__item--active {
  background: var(--mm-accent-soft) !important;
  border-color: var(--mm-accent-border) !important;
  color: var(--mm-primary) !important;
  font-weight: 600;
}

.drive-nav__storage {
  display: grid;
  gap: 8px;
  margin-top: auto;
  font-size: 12px;
  color: var(--mm-text-secondary);
}

.drive-nav__meter {
  display: flex;
  height: 6px;
  border-radius: 999px;
  background: var(--mm-card-muted);
  overflow: hidden;
}

.drive-nav__meter span {
  width: 76%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--mm-primary) 0%, var(--mm-primary-pressed) 100%);
}

.drive-main {
  display: grid;
  grid-template-rows: auto auto 1fr;
  padding: 18px 20px 72px;
}

.drive-main__toolbar,
.drive-main__table-head,
.drive-main__row {
  display: grid;
  grid-template-columns: 1.4fr 0.8fr 0.7fr 0.7fr;
  gap: 16px;
  align-items: center;
}

.drive-main__toolbar {
  grid-template-columns: minmax(0, 1fr) auto;
  margin-bottom: 14px;
}

.drive-main__chips,
.drive-main__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.drive-main__actions button,
.drive-detail__card button {
  border: 1px solid var(--mm-border);
  background: var(--mm-card);
}

.drive-main__table-head {
  padding: 10px 0;
  color: var(--mm-text-secondary);
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.drive-main__table {
  display: grid;
  align-content: start;
}

.drive-main__row {
  padding: 14px 0;
  border-top: 1px solid var(--mm-border);
}

.drive-main__row strong {
  color: var(--mm-ink);
}

.drive-main__row span {
  color: var(--mm-text-secondary);
  font-size: 13px;
}

.drive-main__row--active {
  padding-left: 10px;
  padding-right: 10px;
  margin: 0 -10px;
  border-radius: 12px;
  background: var(--mm-accent-soft);
  border-top-color: transparent;
}

.drive-detail__head,
.drive-detail__tabs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.drive-detail__preview {
  display: grid;
  place-items: center;
  min-height: 170px;
  border: 1px solid var(--mm-border);
  border-radius: 16px;
  background: linear-gradient(180deg, #dfe6ef 0%, #edf1f6 100%);
  color: #6c7886;
}

.drive-detail__tabs {
  justify-content: start;
  gap: 8px;
}

.drive-detail__tabs button {
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--mm-border);
  border-radius: 999px;
  background: var(--mm-card);
}

.drive-detail__tabs--active {
  border-color: var(--mm-accent-border) !important;
  background: var(--mm-accent-soft) !important;
  color: var(--mm-primary);
}

.drive-detail__properties {
  display: grid;
  gap: 14px;
}

.drive-detail__properties strong,
.drive-detail__card strong {
  display: block;
  margin-top: 6px;
  color: var(--mm-ink);
}

.drive-detail__card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid var(--mm-border);
  border-radius: 14px;
  background: var(--mm-card-muted);
}

.drive-detail__card span:last-child {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.drive-toast {
  position: absolute;
  right: 308px;
  bottom: 18px;
  display: grid;
  gap: 4px;
  min-width: 220px;
  padding: 14px 16px;
}

.drive-toast strong {
  color: var(--mm-ink);
}

.drive-toast span:last-child {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

@media (max-width: 1120px) {
  .drive-page__layout {
    grid-template-columns: 200px minmax(0, 1fr);
  }

  .drive-detail {
    display: none;
  }

  .drive-toast {
    right: 24px;
  }
}

@media (max-width: 820px) {
  .drive-page__layout {
    grid-template-columns: 1fr;
  }

  .drive-nav {
    border-right: 0;
    border-bottom: 1px solid var(--mm-border);
  }

  .drive-main {
    overflow-x: auto;
  }

  .drive-main__table-head,
  .drive-main__row {
    min-width: 640px;
  }
}
</style>
