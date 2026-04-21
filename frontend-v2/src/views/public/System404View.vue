<script setup lang="ts">
import SystemStateCard from '@/shared/components/SystemStateCard.vue'
</script>

<template>
  <system-state-card
    :eyebrow="{ 'zh-CN': '系统', 'zh-TW': '系統', en: 'System' }"
    :title="{ 'zh-CN': '页面不存在', 'zh-TW': '頁面不存在', en: 'Page not found' }"
    :description="{ 'zh-CN': '当前范围内已无法解析此地址。请通过共享壳层返回有效工作区。', 'zh-TW': '目前範圍內已無法解析此位址。請透過共享殼層返回有效工作區。', en: 'The address no longer resolves in the current scope. Use the shared shell to return to a valid workspace.' }"
    accent="#566070"
    :primary-label="{ 'zh-CN': '打开 Suite', 'zh-TW': '開啟 Suite', en: 'Open Suite' }"
    :secondary-label="{ 'zh-CN': '查看边界', 'zh-TW': '查看邊界', en: 'Read Boundary' }"
  />
</template>
