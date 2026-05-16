<script setup lang="ts">
import { NAlert, NButton, NInput } from "naive-ui";
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { lt, useLocaleText } from "@/locales";
import { login as loginApi } from "@/service/api/auth";
import { useAuthStore } from "@/store/modules/auth";

const { tr } = useLocaleText();
const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();
const email = ref("");
const errorMessage = ref("");
const password = ref("");
const submitting = ref(false);

async function submitLogin() {
  errorMessage.value = "";
  if (!email.value.trim() || !password.value) {
    errorMessage.value = tr(
      lt("请输入邮箱和密码。", "請輸入郵箱和密碼。", "Enter your email and password."),
    );
    return;
  }

  submitting.value = true;
  try {
    const session = await loginApi({ email: email.value.trim(), password: password.value });
    authStore.applySession(session);
    await router.push(resolvePostAuthRedirect());
  } catch (error) {
    errorMessage.value = resolveErrorMessage(error);
  } finally {
    submitting.value = false;
  }
}

function resolvePostAuthRedirect() {
  const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "";
  return redirect.startsWith("/") && !redirect.startsWith("//") ? redirect : "/workspace";
}

function resolveErrorMessage(error: unknown) {
  return error instanceof Error && error.message
    ? error.message
    : tr(
        lt(
          "登录失败，请检查输入后重试。",
          "登入失敗，請檢查輸入後重試。",
          "Sign-in failed. Check your input and try again.",
        ),
      );
}
</script>

<template>
  <section class="public-auth-form-panel">
    <div class="auth-form-header">
      <span class="section-label">{{ tr(lt("登录入口", "登入入口", "Login portal")) }}</span>
      <h2>{{ tr(lt("继续进入工作台", "繼續進入工作台", "Continue to workspace")) }}</h2>
      <p>
        {{
          tr(
            lt(
              "输入主管理员或成员邮箱，验证成功后会回到请求的工作区页面。",
              "輸入主要管理員或成員郵箱，驗證成功後會回到請求的工作區頁面。",
              "Enter an administrator or member email. After verification, you return to the requested workspace page.",
            ),
          )
        }}
      </p>
    </div>

    <form class="public-auth-form" @submit.prevent="submitLogin()">
      <label for="login-email">{{ tr(lt("工作邮箱", "工作郵箱", "Work email")) }}</label>
      <NInput
        v-model:value="email"
        size="large"
        placeholder="alex.stein@enterprise.ch"
        :input-props="{
          id: 'login-email',
          'aria-label': tr(lt('工作邮箱', '工作郵箱', 'Work email')),
        }"
      />

      <div class="signin-block__row">
        <label for="login-password">{{ tr(lt("密码", "密碼", "Password")) }}</label>
        <a href="/boundary">{{ tr(lt("忘记密码？", "忘記密碼？", "Forgot password?")) }}</a>
      </div>
      <NInput
        v-model:value="password"
        size="large"
        type="password"
        placeholder="••••••••"
        :input-props="{ id: 'login-password', 'aria-label': tr(lt('密码', '密碼', 'Password')) }"
      />

      <NAlert v-if="errorMessage" type="error" :show-icon="false">{{ errorMessage }}</NAlert>
      <NButton class="primary-action" type="primary" attr-type="submit" :loading="submitting">
        {{ tr(lt("继续", "繼續", "Continue")) }}
      </NButton>

      <div class="section-divider">
        {{ tr(lt("企业单点登录", "企業單一登入", "Enterprise SSO")) }}
      </div>
      <NButton class="ghost-action" attr-type="button">{{
        tr(lt("单点登录（OIDC）", "單一登入（OIDC）", "Single Sign-On (OIDC)"))
      }}</NButton>
      <div class="signin-block__row signin-block__row--muted">
        <a href="/register">{{ tr(lt("创建账户", "建立帳戶", "Create Account")) }}</a>
        <a href="/boundary">{{ tr(lt("需要帮助？", "需要協助？", "Need help?")) }}</a>
      </div>
    </form>
  </section>
</template>
