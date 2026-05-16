<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import CommandPalette, {
  type CommandPaletteGroup,
  type CommandPaletteItem,
} from "@/design-system/components/CommandPalette.vue";
import { lt, useLocaleText } from "@/locales";
import { shellNavGroups } from "@/layouts/modules/shell-nav";
import { useShellStore } from "@/store/modules/shell";

const router = useRouter();
const shellStore = useShellStore();
const { tr } = useLocaleText();
const query = ref("");

const commandGroups = computed<CommandPaletteGroup[]>(() => {
  return shellNavGroups.map((group, groupIndex) => ({
    id: `shell-${groupIndex}`,
    label: tr(group.title),
    items: group.items.map((item) => ({
      id: item.key,
      label: tr(item.label),
      path: item.canonicalPath || item.path,
      restriction: resolveRestriction(item.maturity),
      description: group.caption
        ? tr(group.caption)
        : tr(lt("打开模块", "開啟模組", "Open module")),
    })),
  }));
});

const recentItems = computed<CommandPaletteItem[]>(() => {
  return commandGroups.value.flatMap((group) => group.items).slice(0, 5);
});

function resolveRestriction(maturity: string | undefined): CommandPaletteItem["restriction"] {
  return maturity === "preview" ? "preview" : undefined;
}

function updateVisibility(value: boolean) {
  if (!value) {
    shellStore.closeCommandPalette();
  }
}

function executeCommand(item: { path?: string }) {
  if (item.path) {
    void router.push(item.path);
  }
  shellStore.closeCommandPalette();
}
</script>

<template>
  <CommandPalette
    :groups="commandGroups"
    :query="query"
    :recent-items="recentItems"
    :show="shellStore.commandPaletteOpen"
    :placeholder="
      tr(
        lt(
          '搜索邮件、文档、日程、联系人...',
          '搜尋郵件、文件、日程、聯絡人...',
          'Search mail, docs, calendar, contacts...',
        ),
      )
    "
    @close="shellStore.closeCommandPalette()"
    @execute="executeCommand"
    @update:query="query = $event"
    @update:show="updateVisibility"
  />
</template>
