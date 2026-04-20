<script setup lang="ts">
import SystemStateCard from '@/shared/components/SystemStateCard.vue'
</script>

<template>
  <system-state-card
    :eyebrow="{ 'zh-CN': '系统', 'zh-TW': '系統', en: 'System' }"
    :title="{ 'zh-CN': '服务器暂时中断', 'zh-TW': '伺服器暫時中斷', en: 'Temporary server interruption' }"
    :description="{ 'zh-CN': '当前操作未能完成。请从共享壳层重试，或查看边界文档了解当前服务状态。', 'zh-TW': '目前操作未能完成。請從共享殼層重試，或查看邊界文件了解目前服務狀態。', en: 'The current operation could not be completed. Retry from the shared shell or consult the boundary document for current service posture.' }"
    accent="#ac3434"
    :primary-label="{ 'zh-CN': '返回 Suite', 'zh-TW': '返回 Suite', en: 'Return to Suite' }"
    :secondary-label="{ 'zh-CN': '系统状态', 'zh-TW': '系統狀態', en: 'System Status' }"
  />
</template>
