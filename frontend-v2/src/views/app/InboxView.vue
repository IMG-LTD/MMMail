<script setup lang="ts">
const filters = ['All', 'Unread']

const messages = [
  {
    sender: 'Secure Operations',
    subject: 'Project Phoenix: Final Asset Transfer',
    preview: 'The encrypted payload has been verified. Please review the attached schematics before final sync at 14:00.',
    time: '10:42 AM',
    active: true
  },
  {
    sender: 'Dr. Amir Horne',
    subject: 'Q3 Audit Reports - Action Required',
    preview: 'Fix completed in staging. There are a few discrepancies in the secondary ledger we need to resolve.',
    time: '09:41 AM'
  },
  {
    sender: 'System Alerts',
    subject: 'Weekly Security Digest',
    preview: 'No unauthorized access attempts detected. Three devices registered to your workspace.',
    time: 'Yesterday'
  },
  {
    sender: 'Elena Rostova',
    subject: 'Design System v2',
    preview: 'The token lock model is live. I have attached the latest Figma component library for review.',
    time: 'Oct 14'
  }
]

const attachments = [
  ['Phoenix_Schematics_v4.pdf', '2.4 MB', '#e96666'],
  ['Node_Topology_Map.png', '1.1 MB', '#cfe1ff']
]
</script>

<template>
  <section class="mail-page">
    <div class="mail-page__toolbar">
      <div class="mail-page__toolbar-left">
        <button
          v-for="filter in filters"
          :key="filter"
          type="button"
          class="filter-chip"
          :class="{ 'filter-chip--active': filter === 'All' }"
        >
          {{ filter }}
        </button>
        <button type="button" class="filter-chip">Sort ▾</button>
      </div>
      <div class="mail-page__toolbar-right">
        <button type="button">⇄</button>
        <button type="button">↻</button>
        <button type="button">⋯</button>
      </div>
    </div>

    <article class="mail-workspace">
      <section class="mail-list">
        <article
          v-for="message in messages"
          :key="message.subject"
          class="mail-row"
          :class="{ 'mail-row--active': message.active }"
        >
          <span class="mail-row__flag">□</span>
          <div class="mail-row__content">
            <div class="mail-row__header">
              <strong>{{ message.sender }}</strong>
              <time>{{ message.time }}</time>
            </div>
            <p class="mail-row__subject">{{ message.subject }}</p>
            <p class="mail-row__preview">{{ message.preview }}</p>
          </div>
        </article>
      </section>

      <section class="mail-detail">
        <div class="mail-detail__tools">
          <div class="mail-detail__tool-group">
            <button type="button">⌂</button>
            <button type="button">⟲</button>
            <button type="button">↶</button>
          </div>
          <div class="mail-detail__tool-group">
            <button type="button">🏷</button>
            <button type="button">⋯</button>
          </div>
        </div>

        <div class="mail-detail__trust">Only you and the recipient can read this message.</div>

        <header class="mail-detail__header">
          <div>
            <h1>Project Phoenix: Final Asset Transfer</h1>
            <div class="mail-detail__meta">
              <span class="mail-detail__avatar">S</span>
              <div>
                <strong>Secure Operations</strong>
                <p>&lt;ops@proton.secure-mail&gt;</p>
              </div>
            </div>
          </div>
          <div class="mail-detail__stamp">
            <span>Oct 05, 2026</span>
            <span>10:42 AM</span>
          </div>
        </header>

        <div class="mail-detail__body">
          <p>Team,</p>
          <p>
            The encrypted payload has been verified across all nodes. Please review the attached
            schematics before the final sync at 14:00 CEST. Ensure your private keys are updated in
            the local keystore, legacy keys will be deprecated post-sync.
          </p>
          <p>
            Do not forward these documents outside the secure perimeter. Best regards, Operations
            Director, Phoenix Initiative.
          </p>
        </div>

        <section class="mail-detail__attachments">
          <span class="section-label">2 Attachments</span>
          <div class="mail-detail__attachment-grid">
            <article v-for="[name, size, color] in attachments" :key="name" class="attachment-card">
              <span class="attachment-card__thumb" :style="{ background: color }" />
              <div>
                <strong>{{ name }}</strong>
                <p>{{ size }}</p>
              </div>
            </article>
          </div>
        </section>

        <section class="mail-reply">
          <textarea placeholder="Reply..." />
          <div class="mail-reply__footer">
            <div class="mail-reply__tools">
              <button type="button">B</button>
              <button type="button">Z</button>
              <button type="button">∞</button>
            </div>
            <button type="button" class="mail-reply__send">Send ▷</button>
          </div>
        </section>
      </section>
    </article>
  </section>
</template>

<style scoped>
.mail-page {
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 56px);
  background: #fff;
}

.mail-page__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 44px;
  padding: 0 14px;
  border-bottom: 1px solid var(--mm-border);
}

.mail-page__toolbar-left,
.mail-page__toolbar-right,
.mail-detail__tool-group,
.mail-reply__tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-chip,
.mail-page__toolbar-right button,
.mail-detail__tool-group button,
.mail-reply__tools button {
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.filter-chip--active {
  border-color: var(--mm-border);
  background: #f5f6f8;
  color: var(--mm-ink);
}

.mail-workspace {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  min-height: calc(100vh - 100px);
}

.mail-list {
  display: grid;
  align-content: start;
  border-right: 1px solid var(--mm-border);
}

.mail-row {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--mm-border);
}

.mail-row--active {
  background: #eef4fc;
}

.mail-row__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.mail-row__header strong {
  font-size: 13px;
}

.mail-row__subject,
.mail-row__preview,
.mail-detail__meta p,
.mail-detail__body p,
.attachment-card p {
  margin: 0;
}

.mail-row__subject {
  margin-top: 4px;
  font-size: 13px;
  color: var(--mm-ink);
}

.mail-row__preview {
  margin-top: 4px;
  color: var(--mm-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

time {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.mail-detail {
  padding: 14px 18px 18px;
}

.mail-detail__tools,
.mail-detail__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
}

.mail-detail__trust {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  margin-top: 14px;
  padding: 0 14px;
  border: 1px solid #bfd9c8;
  border-radius: 999px;
  background: #edf8f0;
  color: #37634a;
  font-size: 12px;
}

.mail-detail__header {
  gap: 16px;
  margin-top: 22px;
}

.mail-detail__header h1 {
  margin: 0 0 14px;
  font-size: 22px;
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.mail-detail__meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mail-detail__avatar {
  display: inline-grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: #4e6fd4;
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.mail-detail__meta strong {
  display: block;
  font-size: 12px;
}

.mail-detail__meta p,
.mail-detail__stamp {
  color: var(--mm-text-secondary);
  font-size: 11px;
}

.mail-detail__stamp {
  display: grid;
  gap: 4px;
  justify-items: end;
}

.mail-detail__body {
  margin-top: 22px;
  color: #46505a;
  font-size: 13px;
  line-height: 1.7;
}

.mail-detail__body p + p {
  margin-top: 12px;
}

.mail-detail__attachments {
  margin-top: 18px;
}

.mail-detail__attachment-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 10px;
}

.attachment-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--mm-border);
  border-radius: 12px;
  background: #fafafa;
}

.attachment-card__thumb {
  width: 28px;
  height: 36px;
  border-radius: 6px;
}

.attachment-card p {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.mail-reply {
  margin-top: 20px;
  border: 1px solid var(--mm-border);
  border-radius: 12px;
  overflow: hidden;
}

.mail-reply textarea {
  width: 100%;
  min-height: 108px;
  padding: 14px;
  border: 0;
  resize: vertical;
  font: inherit;
  outline: none;
}

.mail-reply__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 42px;
  padding: 0 10px;
  border-top: 1px solid var(--mm-border);
}

.mail-reply__send {
  min-height: 28px;
  padding: 0 12px;
  border: 0;
  border-radius: 8px;
  background: #356ae7;
  color: #fff;
  font-size: 12px;
}

@media (max-width: 1120px) {
  .mail-workspace {
    grid-template-columns: 320px minmax(0, 1fr);
  }
}

@media (max-width: 900px) {
  .mail-workspace {
    grid-template-columns: 1fr;
  }

  .mail-list {
    border-right: 0;
    border-bottom: 1px solid var(--mm-border);
  }

  .mail-detail__attachment-grid {
    grid-template-columns: 1fr;
  }
}
</style>
