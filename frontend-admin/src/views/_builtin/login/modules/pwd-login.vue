<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { loginModuleRecord } from '@/constants/app';
import { REG_EMAIL, REG_PWD } from '@/constants/reg';
import { fetchLogin } from '@/service/api/auth';
import { useAuthStore } from '@/store/modules/auth';
import { useRouterPush } from '@/hooks/common/router';
import { $t } from '@/locales';

defineOptions({
  name: 'PwdLogin'
});

const authStore = useAuthStore();
const { redirectFromLogin, toggleLoginModule } = useRouterPush();
const validationMessage = ref('');
const submitting = ref(false);
const submitDisabled = computed(() => submitting.value || authStore.loginLoading);

interface FormModel {
  email: string;
  password: string;
}

const model: FormModel = reactive({
  email: '',
  password: '123456'
});

async function handleSubmit() {
  const message = validateModel();
  if (message || submitDisabled.value) {
    validationMessage.value = message;
    return;
  }

  submitting.value = true;
  const { data, error } = await fetchLogin(model.email, model.password).finally(() => {
    submitting.value = false;
  });

  if (error) {
    authStore.handleLoginFailure(error);
    return;
  }

  if (!error) {
    await authStore.applyAuthPayload(data);
    await redirectFromLogin();
  }
}

function validateModel() {
  if (!model.email.trim()) {
    return $t('form.email.required');
  }
  if (!REG_EMAIL.test(model.email)) {
    return $t('form.email.invalid');
  }
  if (!model.password) {
    return $t('form.pwd.required');
  }
  if (!REG_PWD.test(model.password)) {
    return $t('form.pwd.invalid');
  }
  return '';
}

function clearValidation() {
  validationMessage.value = '';
}

type AccountKey = 'super' | 'admin' | 'user';

interface Account {
  key: AccountKey;
  label: string;
  userName: string;
  password: string;
}

const accounts = computed<Account[]>(() => [
  {
    key: 'super',
    label: $t('page.login.pwdLogin.superAdmin'),
    userName: 'super@mmmail.local',
    password: '123456'
  },
  {
    key: 'admin',
    label: $t('page.login.pwdLogin.admin'),
    userName: 'admin@mmmail.local',
    password: '123456'
  },
  {
    key: 'user',
    label: $t('page.login.pwdLogin.user'),
    userName: 'user@mmmail.local',
    password: '123456'
  }
]);

async function handleAccountLogin(account: Account) {
  await authStore.login(account.userName, account.password);
}
</script>

<template>
  <form class="auth-native-form" @submit.prevent="handleSubmit">
    <div v-if="authStore.loginLockMessage" class="auth-alert auth-alert-error" role="alert">
      <strong>{{ $t('page.login.security.lockTitle') }}</strong>
      <span>{{ authStore.loginLockMessage }}</span>
    </div>

    <div v-if="authStore.loginRisk?.secondFactorRequired" class="auth-alert auth-alert-warning" role="alert">
      <strong>{{ $t('page.login.security.title') }}</strong>
      <span>{{ $t('page.login.security.secondFactorRequired') }}</span>
    </div>

    <label class="auth-field">
      <span>{{ $t('page.login.common.emailPlaceholder') }}</span>
      <input
        v-model="model.email"
        class="auth-input"
        :aria-label="$t('page.login.common.emailPlaceholder')"
        autocomplete="email"
        inputmode="email"
        :placeholder="$t('page.login.common.emailPlaceholder')"
        type="email"
        @input="clearValidation"
      />
    </label>

    <label class="auth-field">
      <span>{{ $t('page.login.common.passwordPlaceholder') }}</span>
      <input
        v-model="model.password"
        class="auth-input"
        :aria-label="$t('page.login.common.passwordPlaceholder')"
        autocomplete="current-password"
        :placeholder="$t('page.login.common.passwordPlaceholder')"
        type="password"
        @input="clearValidation"
      />
    </label>

    <p v-if="validationMessage" class="auth-form-error" role="alert">{{ validationMessage }}</p>

    <div class="auth-form-row">
      <label class="auth-check">
        <input type="checkbox" />
        <span>{{ $t('page.login.pwdLogin.rememberMe') }}</span>
      </label>
      <button class="auth-link-button" type="button" @click="toggleLoginModule('reset-pwd')">
        {{ $t('page.login.pwdLogin.forgetPassword') }}
      </button>
    </div>

    <button class="auth-primary-button" type="submit" :aria-busy="submitting" :disabled="submitDisabled">
      <span>{{ $t('common.confirm') }}</span>
    </button>

    <div class="auth-secondary-actions">
      <button type="button" @click="toggleLoginModule('code-login')">
        {{ $t(loginModuleRecord['code-login']) }}
      </button>
      <button type="button" @click="toggleLoginModule('register')">
        {{ $t(loginModuleRecord.register) }}
      </button>
    </div>

    <div class="auth-divider" role="separator">
      <span>{{ $t('page.login.pwdLogin.otherAccountLogin') }}</span>
    </div>

    <div class="auth-account-grid">
      <button v-for="item in accounts" :key="item.key" type="button" @click="handleAccountLogin(item)">
        {{ item.label }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.auth-native-form {
  display: grid;
  gap: 18px;
}

.auth-alert {
  display: grid;
  gap: 6px;
  border: 1px solid;
  border-radius: 8px;
  padding: 12px 14px;
  line-height: 1.45;
}

.auth-alert strong {
  color: var(--n-text-color-1, #111827);
  font-size: 14px;
  font-weight: 700;
}

.auth-alert span {
  color: var(--n-text-color-2, #4b5563);
  font-size: 13px;
}

.auth-alert-error {
  border-color: rgba(239, 68, 68, 0.34);
  background: rgba(254, 242, 242, 0.88);
}

.auth-alert-warning {
  border-color: rgba(245, 158, 11, 0.36);
  background: rgba(255, 251, 235, 0.9);
}

.auth-field {
  display: grid;
  gap: 8px;
  color: var(--n-text-color-2, #4b5563);
  font-size: 13px;
  font-weight: 650;
}

.auth-input {
  width: 100%;
  min-height: 46px;
  border: 1px solid var(--n-border-color, #d7dde8);
  border-radius: 8px;
  background: var(--n-color, #ffffff);
  color: var(--n-text-color-1, #111827);
  font-size: 15px;
  outline: none;
  padding: 0 14px;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.auth-input:focus {
  border-color: var(--n-primary-color, #2563eb);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.14);
}

.auth-form-error {
  margin: -4px 0 0;
  color: var(--n-error-color, #dc2626);
  font-size: 13px;
}

.auth-form-row,
.auth-secondary-actions,
.auth-account-grid {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auth-form-row {
  justify-content: space-between;
}

.auth-check {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--n-text-color-2, #4b5563);
  cursor: pointer;
  font-size: 14px;
}

.auth-check input {
  width: 16px;
  height: 16px;
  accent-color: var(--n-primary-color, #2563eb);
}

.auth-link-button,
.auth-secondary-actions button,
.auth-account-grid button,
.auth-primary-button {
  min-height: 42px;
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 650;
}

.auth-link-button {
  background: transparent;
  color: var(--n-primary-color, #2563eb);
  padding: 0;
}

.auth-primary-button {
  width: 100%;
  min-height: 46px;
  background: var(--n-primary-color, #2563eb);
  color: #ffffff;
  font-size: 15px;
}

.auth-primary-button:disabled {
  cursor: progress;
  opacity: 0.68;
}

.auth-secondary-actions button {
  flex: 1 1 0;
  background: var(--n-color, #ffffff);
  border-color: var(--n-border-color, #d7dde8);
  color: var(--n-text-color-1, #111827);
  padding: 0 12px;
}

.auth-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--n-text-color-3, #6b7280);
  font-size: 13px;
}

.auth-divider::before,
.auth-divider::after {
  flex: 1;
  height: 1px;
  background: var(--n-divider-color, #e5e7eb);
  content: '';
}

.auth-account-grid {
  justify-content: center;
}

.auth-account-grid button {
  background: var(--n-primary-color, #2563eb);
  color: #ffffff;
  padding: 0 14px;
}

@media (max-width: 520px) {
  .auth-form-row,
  .auth-secondary-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .auth-link-button {
    align-self: flex-start;
  }

  .auth-account-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .auth-account-grid button {
    padding: 0 8px;
  }
}
</style>
