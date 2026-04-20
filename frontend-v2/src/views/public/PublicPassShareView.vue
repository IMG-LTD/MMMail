<script setup lang="ts">
import { onMounted } from 'vue'
import { NButton, NInput } from 'naive-ui'
import { lt, useLocaleText } from '@/locales'
import { usePublicShareFlow } from '@/shared/composables/usePublicShareFlow'

const { tr } = useLocaleText()
const shareFlow = usePublicShareFlow()
const sharePassword = shareFlow.password
const shareLoading = shareFlow.loading

onMounted(() => {
  void shareFlow.loadCapabilities()
})
</script>

<template>
  <section class="public-shell page-shell share-pass">
    <article class="surface-card share-pass__auth">
      <span class="section-label">{{ tr(lt('受保护访问', '受保護存取', 'Protected access')) }}</span>
      <h1 class="page-title">{{ tr(lt('打开安全链接', '開啟安全連結', 'Open a secure link')) }}</h1>
      <p class="page-subtitle">
        {{ tr(lt('该链接会在密码验证后解锁单个秘密项。下载、复制和再次分享都会按当前策略记录审计轨迹。', '此連結會在密碼驗證後解鎖單一秘密項目。下載、複製與再次分享都會依目前政策留下稽核軌跡。', 'This link unlocks a single secret item after password verification. Download, copy, and resharing attempts are audited under the active policy.')) }}
      </p>

      <label>{{ tr(lt('访问密码', '存取密碼', 'Access password')) }}</label>
      <n-input
        v-model:value="sharePassword"
        type="password"
        :placeholder="tr(lt('输入共享密码', '輸入共享密碼', 'Enter share password'))"
      />

      <div class="share-pass__actions">
        <n-button type="primary" :loading="shareLoading" @click="shareFlow.unlock()">{{ tr(lt('解锁条目', '解鎖項目', 'Unlock item')) }}</n-button>
        <n-button secondary>{{ tr(lt('查看访问策略', '查看存取政策', 'Review access policy')) }}</n-button>
      </div>
    </article>

    <article class="surface-card share-pass__preview">
      <span class="section-label">{{ tr(lt('链接摘要', '連結摘要', 'Link summary')) }}</span>
      <h2>{{ tr(lt('共享保险库便签', '共享保險庫便箋', 'Shared Vault Note')) }}</h2>
      <p class="page-subtitle">
        {{ tr(lt('36 小时后过期 · 单一收件者 · 解锁前禁止复制', '36 小時後到期 · 單一收件者 · 解鎖前禁止複製', 'Expires in 36 hours · Single recipient · Copy blocked until unlock')) }}
      </p>

      <div class="share-pass__meta">
        <div class="metric-chip">{{ tr(lt('邮箱验证已启用', '信箱驗證已啟用', 'Mailbox verification enabled')) }}</div>
        <div class="metric-chip">{{ tr(lt('下载后自动吊销', '下載後自動吊銷', 'Auto revoke after download')) }}</div>
        <div class="metric-chip">{{ tr(lt('审计日志已记录', '稽核日誌已記錄', 'Audit logging enabled')) }}</div>
      </div>
    </article>
  </section>
</template>

<style scoped>
.public-shell {
  padding: 64px 0;
}

.share-pass {
  display: grid;
  grid-template-columns: 0.85fr 1.15fr;
  gap: 20px;
}

.share-pass__auth,
.share-pass__preview {
  padding: 28px;
}

label {
  display: block;
  margin: 20px 0 10px;
  color: var(--mm-text-secondary);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.share-pass__actions,
.share-pass__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}

.share-pass__preview h2 {
  margin: 10px 0 12px;
  font-size: 32px;
  letter-spacing: -0.04em;
  color: var(--mm-pass);
}

@media (max-width: 900px) {
  .share-pass {
    grid-template-columns: 1fr;
  }
}
</style>
