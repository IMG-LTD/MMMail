<script setup lang="ts">
import LoginFormPanel from "./auth/LoginFormPanel.vue";
import PublicAuthShell from "./auth/PublicAuthShell.vue";

// Locale wiring lives in the public/auth child components through useLocaleText and lt().
</script>

<template>
  <PublicAuthShell mode="login">
    <LoginFormPanel />
  </PublicAuthShell>
</template>
