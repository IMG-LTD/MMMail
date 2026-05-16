<script setup lang="ts">
import { computed, ref } from "vue";
import { NAvatar, NCheckbox, NList, NListItem, NSpace, NTag, NThing, NVirtualList } from "naive-ui";
import { useLocaleText } from "@/locales";
import type { EntityListItem } from "../types";

const VIRTUAL_SCROLL_THRESHOLD = 200;

const props = withDefaults(
  defineProps<{
    items: readonly EntityListItem[];
    selectedId?: string;
    selectable?: "single" | "multiple" | false;
    virtualScroll?: boolean;
    itemHeight?: number;
    loading?: boolean;
    empty?: boolean;
  }>(),
  {
    empty: false,
    itemHeight: 56,
    loading: false,
    selectable: false,
    selectedId: undefined,
    virtualScroll: false,
  },
);

const emit = defineEmits<{
  "update:selectedId": [id: string];
  select: [id: string];
  action: [id: string, actionId: string];
}>();

const { tr } = useLocaleText();
const activeIndex = ref(0);
const useVirtual = computed(
  () => props.virtualScroll || props.items.length >= VIRTUAL_SCROLL_THRESHOLD,
);
const virtualItems = computed(() => [...props.items]);

function selectItem(item: EntityListItem) {
  if (item.disabled) {
    return;
  }

  emit("update:selectedId", item.id);
  emit("select", item.id);
}

function move(delta: number) {
  const nextIndex = Math.min(Math.max(activeIndex.value + delta, 0), props.items.length - 1);
  activeIndex.value = nextIndex;
  const item = props.items[nextIndex];

  if (item) {
    selectItem(item);
  }
}
</script>

<template>
  <NList
    class="v211-entity-list"
    role="listbox"
    :aria-busy="loading"
    :aria-multiselectable="selectable === 'multiple' ? 'true' : undefined"
    tabindex="0"
    @keydown.down.prevent="move(1)"
    @keydown.up.prevent="move(-1)"
  >
    <NVirtualList v-if="useVirtual" :items="virtualItems" :item-size="itemHeight">
      <template #default="{ item }">
        <NListItem
          :key="item.id"
          role="option"
          :aria-selected="item.id === selectedId"
          :aria-label="item.ariaLabel ? tr(item.ariaLabel) : tr(item.title)"
          @click="selectItem(item)"
        >
          <NThing :title="tr(item.title)" :description="item.meta ? tr(item.meta) : undefined">
            <template #avatar>
              <NAvatar :src="item.avatar">{{ tr(item.title).slice(0, 1) }}</NAvatar>
            </template>
          </NThing>
        </NListItem>
      </template>
    </NVirtualList>

    <NListItem
      v-for="item in useVirtual ? [] : items"
      :key="item.id"
      role="option"
      :aria-selected="item.id === selectedId"
      :aria-label="item.ariaLabel ? tr(item.ariaLabel) : tr(item.title)"
      @click="selectItem(item)"
    >
      <template v-if="selectable === 'multiple'" #prefix>
        <NCheckbox :checked="item.id === selectedId" />
      </template>
      <NThing :title="tr(item.title)" :description="item.meta ? tr(item.meta) : undefined">
        <template #avatar>
          <NAvatar :src="item.avatar">{{ tr(item.title).slice(0, 1) }}</NAvatar>
        </template>
        <NSpace v-if="item.tags?.length" size="small">
          <NTag v-for="tag in item.tags" :key="tag.id ?? tr(tag.label)" size="small">
            {{ tr(tag.label) }}
          </NTag>
        </NSpace>
      </NThing>
    </NListItem>
  </NList>
</template>
