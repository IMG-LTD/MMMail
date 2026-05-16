import { ref } from 'vue';
import { defineStore } from 'pinia';
import { SetupStoreId } from '@/enum';
import { localStg } from '@/utils/storage';

export const useOrgStore = defineStore(SetupStoreId.Org, () => {
  const currentOrgId = ref(localStg.get('currentOrgId') || '');

  function setCurrentOrgId(orgId: string) {
    currentOrgId.value = orgId;

    if (orgId) {
      localStg.set('currentOrgId', orgId);
    } else {
      localStg.remove('currentOrgId');
    }
  }

  function resetOrgScope() {
    setCurrentOrgId('');
  }

  return {
    currentOrgId,
    setCurrentOrgId,
    resetOrgScope
  };
});
