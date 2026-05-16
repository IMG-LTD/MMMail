import { computed, ref } from 'vue';
import { REG_PHONE } from '@/constants/reg';
import { $t } from '@/locales';

export function useCaptcha() {
  const loading = ref(false);
  const isCounting = ref(false);

  const label = computed(() => {
    return $t('page.login.codeLogin.getCode');
  });

  function isPhoneValid(phone: string) {
    if (phone.trim() === '') {
      window.$message?.error?.($t('form.phone.required'));

      return false;
    }

    if (!REG_PHONE.test(phone)) {
      window.$message?.error?.($t('form.phone.invalid'));

      return false;
    }

    return true;
  }

  function getCaptcha(phone: string) {
    const valid = isPhoneValid(phone);

    if (!valid || loading.value) {
      return;
    }

    window.$message?.error?.($t('page.login.codeLogin.unavailable'));
  }

  return {
    label,
    isCounting,
    loading,
    getCaptcha
  };
}
