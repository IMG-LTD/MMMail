<script setup lang="ts">
import SystemStateCard from '@/shared/components/SystemStateCard.vue'
</script>

<template>
  <system-state-card
    :eyebrow="{ 'zh-CN': '系统', 'zh-TW': '系統', en: 'System' }"
    :title="{ 'zh-CN': '计划维护中', 'zh-TW': '排程維護中', en: 'Scheduled maintenance' }"
    :description="{ 'zh-CN': '平台维护期间，关键服务会暂时不可用。状态提示应保持平静且明确。', 'zh-TW': '平台維護期間，關鍵服務會暫時不可用。狀態提示應保持平靜且明確。', en: 'Critical services are temporarily unavailable while platform maintenance is in progress. Status messaging should stay calm and explicit.' }"
    accent="#e47b39"
    :primary-label="{ 'zh-CN': '打开 Suite', 'zh-TW': '開啟 Suite', en: 'Open Suite' }"
    :secondary-label="{ 'zh-CN': '系统状态', 'zh-TW': '系統狀態', en: 'System Status' }"
  />
</template>
