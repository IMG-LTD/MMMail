<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { NButton, NCard, NDataTable, NForm, NFormItem, NGi, NGrid, NInput, NSpace, NStatistic, NTag } from 'naive-ui';
import { createPassItem, listPassItems, listPassVaults, readPassMonitor } from '@/service/api';
import { $t } from '@/locales';

defineOptions({ name: 'Pass' });

const vaults = ref<Api.Pass.Vault[]>([]);
const items = ref<Api.Pass.Item[]>([]);
const monitor = ref<Api.Pass.Monitor | null>(null);
const itemModel = reactive({ itemType: 'LOGIN', note: '', secretCiphertext: '', title: '', username: '', website: '' });

const columns = computed(() => [
  { title: $t('page.pass.title'), key: 'title' },
  { title: $t('page.pass.website'), key: 'website' },
  { title: $t('page.pass.username'), key: 'username' },
  { title: $t('page.drive.updatedAt'), key: 'updatedAt' }
]);

async function loadPass() {
  const [vaultResult, itemResult, monitorResult] = await Promise.all([
    listPassVaults(),
    listPassItems(),
    readPassMonitor()
  ]);

  if (!vaultResult.error) {
    vaults.value = vaultResult.data;
  }

  if (!itemResult.error) {
    items.value = itemResult.data;
  }

  if (!monitorResult.error) {
    monitor.value = monitorResult.data;
  }
}

async function submitItem() {
  const { error } = await createPassItem({ ...itemModel });

  if (!error) {
    await loadPass();
  }
}

onMounted(loadPass);
</script>

<template>
  <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
    <NGi span="24 m:16">
      <NCard class="card-wrapper" :title="$t('route.pass')">
        <NSpace class="mb-12px">
          <NTag v-for="vault in vaults" :key="vault.id">{{ vault.name }}: {{ vault.itemCount }}</NTag>
        </NSpace>
        <NDataTable :columns="columns" :data="items" />
      </NCard>
    </NGi>
    <NGi span="24 m:8">
      <NSpace vertical :size="16">
        <NCard class="card-wrapper" :title="$t('page.pass.monitor')">
          <NStatistic :label="$t('page.pass.title')" :value="monitor?.totalItemCount || 0" />
          <NTag class="mt-12px" type="warning">{{ monitor?.weakPasswordCount || 0 }}</NTag>
        </NCard>
        <NCard class="card-wrapper" :title="$t('page.pass.create')">
          <NForm :model="itemModel" label-placement="top">
            <NFormItem path="title" :label="$t('page.pass.title')">
              <NInput v-model:value="itemModel.title" />
            </NFormItem>
            <NFormItem path="website" :label="$t('page.pass.website')">
              <NInput v-model:value="itemModel.website" />
            </NFormItem>
            <NFormItem path="username" :label="$t('page.pass.username')">
              <NInput v-model:value="itemModel.username" />
            </NFormItem>
            <NFormItem path="secretCiphertext" :label="$t('page.pass.secret')">
              <NInput v-model:value="itemModel.secretCiphertext" type="password" />
            </NFormItem>
            <NButton type="primary" @click="submitItem">{{ $t('page.pass.create') }}</NButton>
          </NForm>
        </NCard>
      </NSpace>
    </NGi>
  </NGrid>
</template>
