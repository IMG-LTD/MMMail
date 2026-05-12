<script setup lang="ts">
import { lt, useLocaleText } from '@/locales'

const { tr } = useLocaleText()
</script>

<template>
  <div class="login-screen__panel login-screen__panel--form">
    <div class="login-actions">
      <a href="/boundary">{{ tr(lt('支持', '支援', 'Support')) }}</a>
      <a class="outline-link" href="/register">{{ tr(lt('创建账户', '建立帳戶', 'Create Account')) }}</a>
    </div>

    <div class="signin-block">
      <span class="section-label">{{ tr(lt('登录入口', '登入入口', 'Login portal')) }}</span>
      <h2>{{ tr(lt('登录 MMMail', '登入 MMMail', 'Sign in to MMMail')) }}</h2>

      <label>{{ tr(lt('工作邮箱', '工作郵箱', 'Work email')) }}</label>
      <input class="field" type="email" placeholder="alex.stein@enterprise.ch" />

      <div class="signin-block__row">
        <label>{{ tr(lt('密码', '密碼', 'Password')) }}</label>
        <a href="/boundary">{{ tr(lt('忘记密码？', '忘記密碼？', 'Forgot password?')) }}</a>
      </div>
      <input class="field" type="password" placeholder="••••••••" />

      <button class="primary-action" type="button">{{ tr(lt('继续', '繼續', 'Continue')) }}</button>
      <div class="section-divider">{{ tr(lt('企业单点登录', '企業單一登入', 'Enterprise SSO')) }}</div>
      <button class="ghost-action" type="button">{{ tr(lt('单点登录（OIDC）', '單一登入（OIDC）', 'Single Sign-On (OIDC)')) }}</button>

      <div class="mfa-strip">
        <div>
          <span class="section-label">{{ tr(lt('双重验证', '雙重驗證', 'Two-factor authentication')) }}</span>
          <p>{{ tr(lt('输入你可信设备上的一次性验证码。', '輸入你可信裝置上的一次性驗證碼。', 'Enter one code from your trusted device.')) }}</p>
        </div>
        <strong>{{ tr(lt('步骤 2 / 2', '步驟 2 / 2', 'Step 2 / 2')) }}</strong>
      </div>

      <div class="otp-grid">
        <span v-for="cell in 6" :key="cell">•</span>
      </div>

      <div class="signin-block__row signin-block__row--muted">
        <a href="/boundary">{{ tr(lt('使用其他设备', '使用其他裝置', 'Use another device')) }}</a>
        <a href="/boundary">{{ tr(lt('需要帮助？', '需要協助？', 'Need help?')) }}</a>
      </div>
    </div>
  </div>
</template>
