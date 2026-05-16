<script lang="ts" setup>
import { computed, ref, shallowRef } from 'vue';
import { useRouter } from 'vue-router';
import { onKeyStroke, useDebounceFn } from '@vueuse/core';
import { useAppStore } from '@/store/modules/app';
import { readSearchSuggestions } from '@/service/api/search';
import { $t } from '@/locales';
import SearchResult from './search-result.vue';
import { EmptyState, ErrorState } from '@/components/feedback';
import SearchFooter from './search-footer.vue';

defineOptions({ name: 'SearchModal' });

const DEFAULT_SEARCH_LIMIT = 8;
const SEARCH_DEBOUNCE_MS = 150;
const CLOSE_DELAY_MS = 200;

const router = useRouter();
const appStore = useAppStore();

const isMobile = computed(() => appStore.isMobile);

const keyword = ref('');
const activePath = ref('');
const loading = ref(false);
const searchError = ref('');
const resultOptions = shallowRef<Api.Search.Suggestion[]>([]);

const handleSearch = useDebounceFn(search, SEARCH_DEBOUNCE_MS);

const visible = defineModel<boolean>('show', { required: true });

async function search() {
  const trimKeyword = keyword.value.trim();

  if (!trimKeyword) {
    resetSearch();
    return;
  }

  loading.value = true;
  searchError.value = '';

  const { data, error } = await readSearchSuggestions({ q: trimKeyword });

  loading.value = false;

  if (error) {
    searchError.value = $t('common.error');
    resultOptions.value = [];
    activePath.value = '';
    return;
  }

  resultOptions.value = data.slice(0, DEFAULT_SEARCH_LIMIT);
  activePath.value = resultOptions.value[0]?.path ?? '';
}

function resetSearch() {
  searchError.value = '';
  resultOptions.value = [];
  activePath.value = '';
}

function handleClose() {
  // handle with setTimeout to prevent user from seeing some operations
  setTimeout(() => {
    visible.value = false;
    resetSearch();
    keyword.value = '';
  }, CLOSE_DELAY_MS);
}

/** key up */
function handleUp() {
  const { length } = resultOptions.value;
  if (length === 0) return;

  const index = getActivePathIndex();
  if (index === -1) return;

  const activeIndex = index === 0 ? length - 1 : index - 1;

  activePath.value = resultOptions.value[activeIndex].path;
}

/** key down */
function handleDown() {
  const { length } = resultOptions.value;
  if (length === 0) return;

  const index = getActivePathIndex();
  if (index === -1) return;

  const activeIndex = index === length - 1 ? 0 : index + 1;

  activePath.value = resultOptions.value[activeIndex].path;
}

function getActivePathIndex() {
  return resultOptions.value.findIndex(item => item.path === activePath.value);
}

/** key enter */
function handleEnter() {
  if (activePath.value === '') {
    openSearchPage();
    return;
  }
  handleClose();
  router.push(activePath.value);
}

function openSearchPage() {
  const trimKeyword = keyword.value.trim();
  if (!trimKeyword) return;

  handleClose();
  router.push({ path: '/search', query: { q: trimKeyword } });
}

function registerShortcut() {
  onKeyStroke('Escape', handleClose);
  onKeyStroke('Enter', handleEnter);
  onKeyStroke('ArrowUp', handleUp);
  onKeyStroke('ArrowDown', handleDown);
}

registerShortcut();
</script>

<template>
  <NModal
    v-model:show="visible"
    :segmented="{ footer: 'soft' }"
    :closable="false"
    preset="card"
    auto-focus
    footer-style="padding: 0; margin: 0"
    class="fixed left-0 right-0"
    :class="[isMobile ? 'size-full top-0px rounded-0' : 'w-630px top-50px']"
    @after-leave="handleClose"
  >
    <NInputGroup>
      <NInput v-model:value="keyword" clearable :placeholder="$t('common.keywordSearch')" @input="handleSearch">
        <template #prefix>
          <icon-uil-search class="text-15px text-#c2c2c2" />
        </template>
      </NInput>
      <NButton v-if="isMobile" type="primary" ghost @click="handleClose">{{ $t('common.cancel') }}</NButton>
    </NInputGroup>

    <div class="mt-20px">
      <NSpin v-if="loading" />
      <ErrorState v-else-if="searchError" :description="searchError" :retryable="false" compact />
      <EmptyState v-else-if="resultOptions.length === 0" compact />
      <SearchResult v-else v-model:path="activePath" :options="resultOptions" @enter="handleEnter" />
    </div>
    <template #footer>
      <SearchFooter v-if="!isMobile" />
    </template>
  </NModal>
</template>

<style lang="scss" scoped></style>
