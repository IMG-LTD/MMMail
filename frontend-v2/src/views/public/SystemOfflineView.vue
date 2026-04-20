<script setup lang="ts">
import SystemStateCard from '@/shared/components/SystemStateCard.vue'
</script>

<template>
  <system-state-card
    :eyebrow="{ 'zh-CN': '系统', 'zh-TW': '系統', en: 'System' }"
    :title="{ 'zh-CN': '离线模式', 'zh-TW': '離線模式', en: 'Offline mode' }"
    :description="{ 'zh-CN': '你当前处于离线状态。邮件发送、同步和实时协作会暂停，直到网络恢复。', 'zh-TW': '你目前處於離線狀態。郵件傳送、同步與即時協作會暫停，直到連線恢復。', en: 'You are currently offline. Mail send, sync, and live collaboration are paused until connectivity resumes.' }"
    accent="#2e8bcc"
    :primary-label="{ 'zh-CN': '打开 Suite', 'zh-TW': '開啟 Suite', en: 'Open Suite' }"
    :secondary-label="{ 'zh-CN': '查看边界', 'zh-TW': '查看邊界', en: 'View Boundary' }"
  />
</template>
