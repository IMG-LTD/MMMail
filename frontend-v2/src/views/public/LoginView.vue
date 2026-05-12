<script setup lang="ts">
import LoginBrandPanel from './auth/LoginBrandPanel.vue'
import LoginFormPanel from './auth/LoginFormPanel.vue'
import LoginLegalBar from './auth/LoginLegalBar.vue'
import './auth/login-view.css'

// Locale wiring lives in the public/auth child components through useLocaleText and lt().
</script>

<template>
  <section class="login-screen">
    <div class="login-screen__body">
      <LoginBrandPanel />
      <LoginFormPanel />
    </div>
    <LoginLegalBar />
  </section>
</template>
