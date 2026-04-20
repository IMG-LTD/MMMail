<script setup lang="ts">
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, useLocaleText } from '@/locales'

const { tr } = useLocaleText()
const issueColumns = [
  {
    id: 'weak-passwords',
    title: lt('弱密码', '弱密碼', 'Weak passwords'),
    count: '12',
    items: [
      ['adobe.com', 'user.design@helio.com'],
      ['amazon.cn', 'user.design@helio.com']
    ]
  },
  {
    id: 'reused-passwords',
    title: lt('重复使用的密码', '重複使用的密碼', 'Reused passwords'),
    count: '4',
    items: [
      ['netflix.com', 'notify@studio.com'],
      ['hulu.com', 'team@helio.com']
    ]
  },
  {
    id: 'missing-2fa',
    title: lt('缺少 2FA', '缺少 2FA', 'Missing 2FA'),
    count: '7',
    items: [
      ['dropbox.com', 'user.design@helio.com'],
      ['github.com', 'dev_account']
    ]
  }
]
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      :eyebrow="lt('密码库', '密碼庫', 'Pass')"
      :title="lt('安全监控', '安全監控', 'Security Monitor')"
      :description="lt('优先处理弱密码、重复密码和缺少二次验证的凭据。', '優先處理弱密碼、重複密碼和缺少第二因素驗證的憑證。', 'Weak, reused, and missing-factor credentials prioritized for immediate remediation.')"
      :badge="lt('Beta', 'Beta', 'Beta')"
      badge-tone="beta"
    />

    <article class="surface-card pass-monitor__hero">
      <div class="pass-monitor__score">
        <span class="section-label">{{ tr(lt('全局安全指标', '全域安全指標', 'Global security metric')) }}</span>
        <strong>82 <small>/ 100</small></strong>
        <p class="page-subtitle">{{ tr(lt('保险库完整性稳定，但已识别出的弱点与跨域重复凭据仍需立即处理。', '保險庫完整性穩定，但已識別出的弱點與跨網域重複憑證仍需立即處理。', 'Vault integrity is stable. Immediate attention is required for identified weak architectural points and redundant credentials across multiple domains.')) }}</p>
      </div>
      <div class="pass-monitor__rails">
        <div class="pass-monitor__rail">
          <span>{{ tr(lt('保险库加密', '保險庫加密', 'Vault encryption')) }}</span>
          <strong>100%</strong>
        </div>
        <div class="pass-monitor__bar"><span style="width: 100%" /></div>
        <div class="pass-monitor__rail">
          <span>{{ tr(lt('凭据健康度', '憑證健康度', 'Credential health')) }}</span>
          <strong>82%</strong>
        </div>
        <div class="pass-monitor__bar"><span style="width: 82%" /></div>
      </div>
    </article>

    <div class="pass-monitor__grid">
      <article v-for="column in issueColumns" :key="column.id" class="surface-card pass-monitor__column">
        <div class="pass-monitor__column-head">
          <span class="section-label">{{ tr(column.title) }}</span>
          <strong>{{ column.count }}</strong>
        </div>
        <div v-for="[domain, username] in column.items" :key="domain" class="pass-monitor__row">
          <div>
            <strong>{{ domain }}</strong>
            <p>{{ username }}</p>
          </div>
          <button type="button">{{ tr(lt('修复', '修復', 'Fix')) }}</button>
        </div>
        <a class="pass-monitor__link" href="/pass">{{ tr(lt('查看全部', '查看全部', 'View all')) }}</a>
      </article>
    </div>
  </section>
</template>

<style scoped>
.pass-monitor__hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 24px;
  padding: 24px;
}

.pass-monitor__score {
  padding-left: 18px;
  border-left: 3px solid var(--mm-pass);
}

.pass-monitor__score strong {
  display: block;
  margin-top: 12px;
  font-size: 64px;
  line-height: 0.92;
  letter-spacing: -0.06em;
  color: var(--mm-pass);
}

.pass-monitor__score small {
  font-size: 22px;
  color: var(--mm-text-secondary);
}

.pass-monitor__rails {
  align-self: center;
  display: grid;
  gap: 10px;
}

.pass-monitor__rail {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.pass-monitor__bar {
  height: 4px;
  border-radius: 999px;
  background: #eceff1;
}

.pass-monitor__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #ff8b38 0%, #ff7d1a 100%);
}

.pass-monitor__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.pass-monitor__column {
  padding: 18px;
}

.pass-monitor__column-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pass-monitor__column-head strong {
  color: var(--mm-pass);
}

.pass-monitor__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--mm-border);
}

.pass-monitor__row p {
  margin: 4px 0 0;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.pass-monitor__row button {
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid var(--mm-border);
  border-radius: 8px;
  background: #fff;
}

.pass-monitor__link {
  display: inline-flex;
  margin-top: 14px;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

@media (max-width: 960px) {
  .pass-monitor__hero,
  .pass-monitor__grid {
    grid-template-columns: 1fr;
  }
}
</style>
