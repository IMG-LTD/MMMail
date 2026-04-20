<script setup lang="ts">
import { NButton, NInput } from 'naive-ui'
import { lt, useLocaleText } from '@/locales'

const { tr } = useLocaleText()
</script>

<template>
  <section class="public-shell page-shell share-page">
    <article class="surface-card share-page__auth">
      <span class="section-label">{{ tr(lt('安全邮件投递', '安全郵件投遞', 'Secure mail delivery')) }}</span>
      <h1 class="page-title">{{ tr(lt('打开受保护消息', '開啟受保護訊息', 'Open a protected message')) }}</h1>
      <p class="page-subtitle">{{ tr(lt('在你输入共享密码前，此投递会保持加密。附件会在验证后于本地解密。', '在你輸入共享密碼前，此投遞會保持加密。附件會在驗證後於本機解密。', 'This delivery remains encrypted until you enter the share password. Attachments are decrypted locally after verification.')) }}</p>

      <label>{{ tr(lt('密码', '密碼', 'Password')) }}</label>
      <n-input type="password" :placeholder="tr(lt('输入访问密码', '輸入存取密碼', 'Enter access password'))" />

      <div class="share-page__auth-actions">
        <n-button type="primary">{{ tr(lt('解密消息', '解密訊息', 'Decrypt Message')) }}</n-button>
        <n-button secondary>{{ tr(lt('查看投递规则', '查看投遞規則', 'Preview delivery rules')) }}</n-button>
      </div>
    </article>

    <article class="surface-card share-page__preview">
      <span class="section-label">{{ tr(lt('投递预览', '投遞預覽', 'Delivery preview')) }}</span>
      <h2>Project Phoenix: Final Asset Transfer</h2>
      <p class="page-subtitle">{{ tr(lt('安全运营 · 2026 年 10 月 15 日 · 2 个附件 · 加密投递', '安全營運 · 2026 年 10 月 15 日 · 2 個附件 · 加密投遞', 'Secure Operations · Oct 15, 2026 · 2 attachments · encrypted delivery')) }}</p>
      <div class="attachment-list">
        <div class="attachment-card">
          <strong>Phoenix_Schematics_v4.pdf</strong>
          <span>2.4 MB</span>
        </div>
        <div class="attachment-card">
          <strong>Node_Topology_Map.png</strong>
          <span>1.1 MB</span>
        </div>
      </div>
    </article>
  </section>
</template>

<style scoped>
.public-shell {
  padding: 64px 0;
}

.share-page {
  display: grid;
  grid-template-columns: 0.8fr 1.2fr;
  gap: 20px;
}

.share-page__auth,
.share-page__preview {
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

.share-page__auth-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}

.share-page__preview h2 {
  margin: 10px 0 12px;
  font-size: 32px;
  letter-spacing: -0.04em;
}

.attachment-list {
  display: grid;
  gap: 14px;
  margin-top: 24px;
}

.attachment-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px;
  border: 1px solid var(--mm-border);
  border-radius: 12px;
  background: var(--mm-card-muted);
}

.attachment-card span {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

@media (max-width: 900px) {
  .share-page {
    grid-template-columns: 1fr;
  }
}
</style>
