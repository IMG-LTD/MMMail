<script setup lang="ts">
import { lt, useLocaleText } from '@/locales'
import { loginValuePoints } from './login-view-helpers'

const { tr } = useLocaleText()
</script>

<template>
  <div class="login-screen__panel login-screen__panel--story">
    <span class="section-label">MMMail</span>
    <h1>{{ tr(lt('面向专注型企业的安全沟通。', '面向專注型企業的安全溝通。', 'Secure communication for focused enterprise.')) }}</h1>
    <p>{{ tr(lt('一个以瑞士工程方法打造的生产力环境，服务于重视隐私、深度工作与完整数据主权的组织。', '一個以瑞士工程方法打造的生產力環境，服務於重視隱私、深度工作與完整資料主權的組織。', 'A Swiss-engineered productivity environment designed for privacy-first organizations who value calm, deep work and total data sovereignty.')) }}</p>

    <div class="value-grid">
      <article v-for="([icon, point], index) in loginValuePoints" :key="index" class="value-tile">
        <span class="value-tile__icon">{{ icon }}</span>
        <span>{{ tr(point) }}</span>
      </article>
    </div>

    <div class="hero-surface" aria-hidden="true">
      <div class="scene-lamp">
        <span class="scene-lamp__base" />
        <span class="scene-lamp__arm scene-lamp__arm--primary" />
        <span class="scene-lamp__arm scene-lamp__arm--secondary" />
        <span class="scene-lamp__head" />
      </div>
      <div class="scene-desk">
        <span class="scene-desk__top" />
        <span class="scene-desk__leg scene-desk__leg--left" />
        <span class="scene-desk__leg scene-desk__leg--right" />
      </div>
      <div class="scene-monitor">
        <span class="scene-monitor__screen" />
        <span class="scene-monitor__stand" />
      </div>
      <div class="scene-chair">
        <span class="scene-chair__back" />
        <span class="scene-chair__seat" />
        <span class="scene-chair__leg" />
      </div>
      <span class="scene-paper scene-paper--a" />
      <span class="scene-paper scene-paper--b" />
    </div>
  </div>
</template>
