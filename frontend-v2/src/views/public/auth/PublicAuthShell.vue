<script setup lang="ts">
import { computed } from "vue";
import { RouterLink } from "vue-router";
import { NTag } from "naive-ui";
import { lt, useLocaleText } from "@/locales";
import { publicAuthSecurityItems, publicAuthWorkspaceItems } from "./login-view-helpers";
import LoginLegalBar from "./LoginLegalBar.vue";
import "./login-view.css";

const props = defineProps<{
  mode: "login" | "register";
}>();

const { tr } = useLocaleText();

const pageCopy = computed(() =>
  props.mode === "login"
    ? {
        label: lt("安全入口", "安全入口", "Secure access"),
        title: lt(
          "进入你的 MMMail 工作区",
          "進入你的 MMMail 工作區",
          "Enter your MMMail workspace",
        ),
        description: lt(
          "使用工作邮箱继续进入邮件、云盘、文档和管理工具。",
          "使用工作郵箱繼續進入郵件、雲端硬碟、文件和管理工具。",
          "Use your work email to continue into mail, drive, docs, and admin tools.",
        ),
      }
    : {
        label: lt("工作区设置", "工作區設定", "Workspace setup"),
        title: lt("创建机构访问入口", "建立機構存取入口", "Create an institutional access point"),
        description: lt(
          "先建立主管理员和工作区，随后进入应用内完成安全配置。",
          "先建立主要管理員和工作區，隨後進入應用內完成安全設定。",
          "Create the primary administrator and workspace, then finish security setup inside the app.",
        ),
      },
);

function isActive(mode: "login" | "register") {
  return props.mode === mode;
}
</script>

<template>
  <div class="public-auth-page">
    <section class="public-auth-shell" :class="`public-auth-shell--${mode}`">
      <aside class="public-auth-rail surface-card">
        <div class="public-auth-brand">
          <span class="section-label">MMMail</span>
          <h1>{{ tr(pageCopy.title) }}</h1>
          <p>{{ tr(pageCopy.description) }}</p>
        </div>

        <nav
          class="auth-mode-switch"
          :aria-label="tr(lt('认证入口', '驗證入口', 'Authentication entry'))"
        >
          <RouterLink
            class="auth-mode-switch__item"
            :class="{ 'auth-mode-switch__item--active': isActive('login') }"
            :aria-current="isActive('login') ? 'page' : undefined"
            to="/login"
          >
            {{ tr(lt("登录", "登入", "Sign in")) }}
          </RouterLink>
          <RouterLink
            class="auth-mode-switch__item"
            :class="{ 'auth-mode-switch__item--active': isActive('register') }"
            :aria-current="isActive('register') ? 'page' : undefined"
            to="/register"
          >
            {{ tr(lt("创建账户", "建立帳戶", "Create account")) }}
          </RouterLink>
        </nav>

        <div class="public-auth-rail__modules">
          <span class="section-label">{{
            tr(lt("工作台模块", "工作台模組", "Workspace modules"))
          }}</span>
          <div class="public-auth-module-list">
            <span
              v-for="item in publicAuthWorkspaceItems"
              :key="tr(item.label)"
              class="public-auth-module"
            >
              <strong>{{ item.shortLabel }}</strong>
              {{ tr(item.label) }}
            </span>
          </div>
        </div>
      </aside>

      <main class="public-auth-main surface-card" :aria-label="tr(pageCopy.label)">
        <slot />
      </main>

      <aside class="public-auth-context surface-card">
        <div class="public-auth-context__head">
          <span class="section-label">{{ tr(lt("访问状态", "存取狀態", "Access state")) }}</span>
          <NTag type="success" round :bordered="false">{{
            tr(lt("自托管就绪", "自託管就緒", "Self-host ready"))
          }}</NTag>
        </div>

        <div class="public-auth-status-list">
          <article
            v-for="item in publicAuthSecurityItems"
            :key="tr(item.label)"
            class="public-auth-status"
          >
            <span>{{ item.shortLabel }}</span>
            <div>
              <strong>{{ tr(item.label) }}</strong>
              <p>{{ tr(item.description) }}</p>
            </div>
          </article>
        </div>

        <div class="public-auth-context__support">
          <RouterLink to="/boundary">{{
            tr(lt("查看部署边界", "查看部署邊界", "View deployment boundary"))
          }}</RouterLink>
          <RouterLink to="/boundary">{{
            tr(lt("服务状态", "服務狀態", "Service status"))
          }}</RouterLink>
        </div>
      </aside>
    </section>

    <LoginLegalBar />
  </div>
</template>
