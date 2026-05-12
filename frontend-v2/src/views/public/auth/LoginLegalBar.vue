<script setup lang="ts">
import { lt, useLocaleText } from '@/locales'

const { tr } = useLocaleText()
</script>

<template>
  <footer class="login-screen__legal">
    <span>{{ tr(lt('© 2026 MMMAIL AG. 瑞士隐私保障。', '© 2026 MMMAIL AG. 瑞士隱私保障。', '© 2026 MMMAIL AG. Swiss privacy guaranteed.')) }}</span>
    <div>
      <a href="/boundary">{{ tr(lt('隐私', '隱私', 'Privacy')) }}</a>
      <a href="/boundary">{{ tr(lt('边界', '邊界', 'Boundary')) }}</a>
      <a href="/boundary">{{ tr(lt('状态', '狀態', 'Status')) }}</a>
      <span>{{ tr(lt('支持 EN / 简 / 繁', '支援 EN / 簡 / 繁', 'Supports EN / 简 / 繁')) }}</span>
    </div>
  </footer>
</template>
