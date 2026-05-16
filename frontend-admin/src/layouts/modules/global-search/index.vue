<script lang="ts" setup>
import { onKeyStroke } from '@vueuse/core';
import { useBoolean } from '@sa/hooks';
import { $t } from '@/locales';
import SearchModal from './components/search-modal.vue';

defineOptions({ name: 'GlobalSearch' });

const SEARCH_SHORTCUT_KEY = 'k';

const { bool: show, setTrue, toggle } = useBoolean();

function handleShortcut(event: KeyboardEvent) {
  if (!event.ctrlKey && !event.metaKey) {
    return;
  }

  event.preventDefault();
  setTrue();
}

onKeyStroke(SEARCH_SHORTCUT_KEY, handleShortcut);
</script>

<template>
  <ButtonIcon :tooltip-content="$t('common.search')" @click="toggle">
    <icon-uil-search />
  </ButtonIcon>
  <SearchModal v-model:show="show" />
</template>

<style lang="scss" scoped></style>
