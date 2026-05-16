<script lang="ts" setup>
import { useThemeStore } from '@/store/modules/theme';

defineOptions({ name: 'SearchResult' });

interface Props {
  options: Api.Search.Suggestion[];
}

defineProps<Props>();

interface Emits {
  (e: 'enter'): void;
}

const emit = defineEmits<Emits>();

const theme = useThemeStore();

const active = defineModel<string>('path', { required: true });

async function handleMouseEnter(item: Api.Search.Suggestion) {
  active.value = item.path;
}

function handleTo(item: Api.Search.Suggestion) {
  active.value = item.path;
  emit('enter');
}
</script>

<template>
  <NScrollbar>
    <div class="pb-12px">
      <template v-for="item in options" :key="`${item.moduleType}-${item.resourceId}`">
        <div
          class="mt-8px min-h-64px flex-y-center cursor-pointer justify-between gap-12px rounded-4px bg-#e5e7eb px-14px py-10px dark:bg-dark"
          :style="{
            background: item.path === active ? theme.themeColor : '',
            color: item.path === active ? '#fff' : ''
          }"
          @click="handleTo(item)"
          @mouseenter="handleMouseEnter(item)"
        >
          <div class="min-w-0 flex-1">
            <div class="flex-y-center gap-8px">
              <NTag size="small" :bordered="false">{{ item.moduleType }}</NTag>
              <span class="truncate font-500">{{ item.title }}</span>
            </div>
            <p class="m-0 mt-4px truncate text-12px opacity-75">{{ item.path }}</p>
          </div>
          <icon-ant-design-enter-outlined class="icon mr-3px p-2px text-20px" />
        </div>
      </template>
    </div>
  </NScrollbar>
</template>

<style lang="scss" scoped></style>
