<script setup lang="ts">
import { onMounted } from 'vue'
import { NButton } from 'naive-ui'
import { lt, useLocaleText } from '@/locales'
import { usePublicShareFlow } from '@/shared/composables/usePublicShareFlow'

const { tr } = useLocaleText()
const shareFlow = usePublicShareFlow()

onMounted(() => {
  void shareFlow.loadCapabilities()
})
</script>

<template>
  <section class="public-shell page-shell share-drive">
    <article class="surface-card share-drive__hero">
      <span class="section-label">{{ tr(lt('安全共享访问', '安全共享存取', 'Secure share access')) }}</span>
      <h1 class="page-title">{{ tr(lt('共享文件夹访问', '共享資料夾存取', 'Shared folder access')) }}</h1>
      <p class="page-subtitle">{{ tr(lt('该链接仅限获批收件人使用。下载前请查看文件范围、有效期与水印策略。', '此連結僅限已批准收件者使用。下載前請查看檔案範圍、有效期與浮水印政策。', 'This link is restricted to approved recipients. Review file scope, expiration, and watermark policy before downloading.')) }}</p>

      <div class="share-drive__meta">
        <div class="metric-chip">{{ tr(lt('5 天后过期', '5 天後到期', 'Expires in 5 days')) }}</div>
        <div class="metric-chip">{{ tr(lt('已启用下载水印', '已啟用下載浮水印', 'Download watermark enabled')) }}</div>
        <div class="metric-chip">{{ tr(lt('2 个文件 · 1 个文件夹', '2 個檔案 · 1 個資料夾', '2 files · 1 folder')) }}</div>
      </div>

      <div class="share-drive__actions">
        <n-button type="primary" @click="shareFlow.unlock()">{{ tr(lt('打开共享文件', '開啟共享檔案', 'Open Shared Files')) }}</n-button>
        <n-button secondary>{{ tr(lt('查看策略', '查看政策', 'Review policy')) }}</n-button>
      </div>
    </article>
  </section>
</template>

<style scoped>
.public-shell {
  padding: 72px 0;
}

.share-drive__hero {
  padding: 30px;
}

.share-drive__meta,
.share-drive__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}
</style>
