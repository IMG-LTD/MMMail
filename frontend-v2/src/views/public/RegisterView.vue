<script setup lang="ts">
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { NAlert, NButton, NInput, NTag } from "naive-ui";
import { lt, useLocaleText } from "@/locales";
import { register as registerApi } from "@/service/api/auth";
import { useAuthStore } from "@/store/modules/auth";
import PublicAuthShell from "./auth/PublicAuthShell.vue";

const PASSWORD_STRENGTH_SEGMENTS = 4;
const MIN_PASSWORD_LENGTH = 12;

const { tr } = useLocaleText();
const authStore = useAuthStore();
const router = useRouter();
const confirmPassword = ref("");
const email = ref("");
const errorMessage = ref("");
const password = ref("");
const submitting = ref(false);
const workspaceName = ref("");

const passwordStrengthScore = computed(() => calculatePasswordStrength(password.value));
const registrationProgressItems = computed(() => [
  {
    key: "identity",
    label: lt("身份", "身分", "Identity"),
    complete: Boolean(workspaceName.value.trim() && email.value.trim()),
  },
  {
    key: "credentials",
    label: lt("凭据", "憑據", "Credentials"),
    complete: passwordStrengthScore.value >= 2,
  },
  {
    key: "confirmation",
    label: lt("确认", "確認", "Confirmation"),
    complete: Boolean(confirmPassword.value && password.value === confirmPassword.value),
  },
]);

async function submitRegistration() {
  errorMessage.value = validateRegistration();
  if (errorMessage.value) {
    return;
  }

  submitting.value = true;
  try {
    const session = await registerApi({
      displayName: workspaceName.value.trim(),
      email: email.value.trim(),
      password: password.value,
    });
    authStore.applySession(session);
    await router.push("/workspace");
  } catch (error) {
    errorMessage.value = resolveErrorMessage(error);
  } finally {
    submitting.value = false;
  }
}

function validateRegistration() {
  if (!workspaceName.value.trim() || !email.value.trim() || !password.value) {
    return tr(
      lt(
        "请填写工作区、邮箱和密码。",
        "請填寫工作區、郵箱和密碼。",
        "Enter workspace, email, and password.",
      ),
    );
  }
  if (password.value !== confirmPassword.value) {
    return tr(lt("两次输入的密码不一致。", "兩次輸入的密碼不一致。", "Passwords do not match."));
  }
  return "";
}

function calculatePasswordStrength(value: string) {
  const checks = [
    value.length >= MIN_PASSWORD_LENGTH,
    /[a-z]/.test(value) && /[A-Z]/.test(value),
    /\d/.test(value),
    /[^A-Za-z0-9]/.test(value),
  ];
  return checks.filter(Boolean).length;
}

function resolveErrorMessage(error: unknown) {
  return error instanceof Error && error.message
    ? error.message
    : tr(
        lt(
          "创建账户失败，请检查输入后重试。",
          "建立帳戶失敗，請檢查輸入後重試。",
          "Account creation failed. Check your input and try again.",
        ),
      );
}

function openLogin() {
  void router.push("/login");
}
</script>

<template>
  <PublicAuthShell mode="register">
    <section class="public-auth-form-panel">
      <div class="auth-form-header">
        <span class="section-label">{{ tr(lt("创建账户", "建立帳戶", "Create account")) }}</span>
        <h2>{{ tr(lt("设置工作区访问", "設定工作區存取", "Set workspace access")) }}</h2>
        <p>
          {{
            tr(
              lt(
                "创建主管理员后会直接进入工作台，后续安全策略在设置中心继续完成。",
                "建立主要管理員後會直接進入工作台，後續安全策略在設定中心繼續完成。",
                "After creating the primary administrator, you enter the workspace and continue security policy setup in Settings.",
              ),
            )
          }}
        </p>
      </div>

      <div class="register-progress" :aria-label="tr(lt('设置进度', '設定進度', 'Setup progress'))">
        <NTag
          v-for="item in registrationProgressItems"
          :key="item.key"
          round
          :type="item.complete ? 'success' : 'default'"
          :bordered="!item.complete"
        >
          {{ tr(item.label) }}
        </NTag>
      </div>

      <form class="public-auth-form" @submit.prevent="submitRegistration()">
        <label for="register-workspace">{{
          tr(lt("工作区名称", "工作區名稱", "Workspace name"))
        }}</label>
        <NInput
          v-model:value="workspaceName"
          size="large"
          :placeholder="tr(lt('MMMail 欧洲', 'MMMail 歐洲', 'MMMail Europe'))"
          :input-props="{
            id: 'register-workspace',
            'aria-label': tr(lt('工作区名称', '工作區名稱', 'Workspace name')),
          }"
        />

        <label for="register-email">{{
          tr(lt("主管理员邮箱", "主要管理員郵箱", "Primary admin email"))
        }}</label>
        <NInput
          v-model:value="email"
          size="large"
          placeholder="founder@company.eu"
          :input-props="{
            id: 'register-email',
            'aria-label': tr(lt('主管理员邮箱', '主要管理員郵箱', 'Primary admin email')),
          }"
        />

        <label for="register-password">{{ tr(lt("密码", "密碼", "Password")) }}</label>
        <NInput
          v-model:value="password"
          size="large"
          type="password"
          :placeholder="
            tr(
              lt(
                '至少 12 位，包含大小写、数字或符号',
                '至少 12 位，包含大小寫、數字或符號',
                'At least 12 characters with mixed case, numbers, or symbols',
              ),
            )
          "
          :input-props="{
            id: 'register-password',
            'aria-label': tr(lt('密码', '密碼', 'Password')),
          }"
        />

        <div
          class="register-strength"
          :aria-label="tr(lt('密码强度', '密碼強度', 'Password strength'))"
        >
          <span
            v-for="segment in PASSWORD_STRENGTH_SEGMENTS"
            :key="segment"
            class="register-strength__segment"
            :class="{ 'register-strength__segment--active': segment <= passwordStrengthScore }"
          />
        </div>

        <label for="register-confirm-password">{{
          tr(lt("确认密码", "確認密碼", "Confirm password"))
        }}</label>
        <NInput
          v-model:value="confirmPassword"
          size="large"
          type="password"
          :placeholder="tr(lt('再次输入密码', '再次輸入密碼', 'Repeat password'))"
          :input-props="{
            id: 'register-confirm-password',
            'aria-label': tr(lt('确认密码', '確認密碼', 'Confirm password')),
          }"
        />

        <NAlert v-if="errorMessage" type="error" :show-icon="false">{{ errorMessage }}</NAlert>
        <div class="public-auth-actions">
          <NButton attr-type="button" secondary @click="openLogin()">{{
            tr(lt("返回登录", "返回登入", "Back to sign in"))
          }}</NButton>
          <NButton type="primary" attr-type="submit" :loading="submitting">{{
            tr(lt("创建账户", "建立帳戶", "Create Account"))
          }}</NButton>
        </div>
      </form>
    </section>
  </PublicAuthShell>
</template>

<style scoped>
.register-progress {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}

.register-strength {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.register-strength__segment {
  height: 6px;
  border-radius: 999px;
  background: var(--mm-border-strong);
}

.register-strength__segment--active {
  background: var(--mm-security);
}
</style>
