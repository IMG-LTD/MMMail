<script setup lang="ts">
import { computed, reactive } from 'vue';
import { fetchRegister } from '@/service/api/auth';
import { useAuthStore } from '@/store/modules/auth';
import { useRouterPush } from '@/hooks/common/router';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';

defineOptions({
  name: 'Register'
});

const authStore = useAuthStore();
const { redirectFromLogin, toggleLoginModule } = useRouterPush();
const { formRef, validate } = useNaiveForm();

interface FormModel {
  displayName: string;
  email: string;
  password: string;
  confirmPassword: string;
}

const model: FormModel = reactive({
  displayName: '',
  email: '',
  password: '',
  confirmPassword: ''
});

const rules = computed<Record<keyof FormModel, App.Global.FormRule[]>>(() => {
  const { formRules, createConfirmPwdRule } = useFormRules();

  return {
    displayName: formRules.userName,
    email: formRules.email,
    password: formRules.pwd,
    confirmPassword: createConfirmPwdRule(model.password)
  };
});

async function handleSubmit() {
  await validate();
  const { data, error } = await fetchRegister(model.displayName, model.email, model.password);

  if (!error) {
    await authStore.applyAuthPayload(data);
    await redirectFromLogin();
  }
}
</script>

<template>
  <NForm ref="formRef" :model="model" :rules="rules" size="large" :show-label="false" @keyup.enter="handleSubmit">
    <NFormItem path="displayName">
      <NInput
        v-model:value="model.displayName"
        :aria-label="$t('page.login.common.userNamePlaceholder')"
        :placeholder="$t('page.login.common.userNamePlaceholder')"
      />
    </NFormItem>
    <NFormItem path="email">
      <NInput
        v-model:value="model.email"
        :aria-label="$t('page.login.common.emailPlaceholder')"
        :placeholder="$t('page.login.common.emailPlaceholder')"
      />
    </NFormItem>
    <NFormItem path="password">
      <NInput
        v-model:value="model.password"
        :aria-label="$t('page.login.common.passwordPlaceholder')"
        type="password"
        show-password-on="click"
        :placeholder="$t('page.login.common.passwordPlaceholder')"
      />
    </NFormItem>
    <NFormItem path="confirmPassword">
      <NInput
        v-model:value="model.confirmPassword"
        :aria-label="$t('page.login.common.confirmPasswordPlaceholder')"
        type="password"
        show-password-on="click"
        :placeholder="$t('page.login.common.confirmPasswordPlaceholder')"
      />
    </NFormItem>
    <NSpace vertical :size="18" class="w-full">
      <NButton type="primary" size="large" round block @click="handleSubmit">
        {{ $t('page.login.register.submit') }}
      </NButton>
      <NButton size="large" round block @click="toggleLoginModule('pwd-login')">
        {{ $t('page.login.common.back') }}
      </NButton>
    </NSpace>
  </NForm>
</template>

<style scoped></style>
