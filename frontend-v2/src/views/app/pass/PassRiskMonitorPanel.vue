<script setup lang="ts">
import { NButton } from "naive-ui";
import type { PassMonitorItem } from "@/service/api/pass";
import { resolveMonitorFlags } from "./pass-view-helpers";

defineProps<{
  actionError: string;
  activeRisk: PassMonitorItem | null;
  items: PassMonitorItem[];
}>();

defineEmits<{
  openRisk: [id: string];
  retry: [];
}>();
</script>

<template>
  <aside class="pass-risk-monitor-panel">
    <header>
      <span class="section-label">Risk monitor</span>
      <strong>{{ items.length }} signals</strong>
    </header>
    <NButton
      v-for="item in items"
      :key="item.id"
      class="pass-risk-trigger"
      native-type="button"
      @click="$emit('openRisk', item.id)"
    >
      <strong>{{ item.title || "Untitled item" }}</strong>
      <span>{{ resolveMonitorFlags(item) || "Tracked" }}</span>
    </NButton>
    <NButton
      v-if="!items.length"
      class="pass-risk-trigger"
      native-type="button"
      @click="$emit('openRisk', '')"
    >
      <strong>Risk review</strong>
      <span>No runtime signals loaded</span>
    </NButton>
    <section class="pass-risk-detail">
      <strong>{{ activeRisk?.title || "Risk detail" }}</strong>
      <p>
        {{
          activeRisk
            ? resolveMonitorFlags(activeRisk)
            : "Select a risk alert to inspect remediation."
        }}
      </p>
      <p v-if="actionError" class="pass-action-error">{{ actionError }}</p>
      <NButton class="pass-action-retry" native-type="button" @click="$emit('retry')"
        >Retry remediation</NButton
      >
    </section>
  </aside>
</template>
