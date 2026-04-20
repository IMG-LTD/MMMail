<script setup lang="ts">
import { lt, useLocaleText } from '@/locales'

const { tr } = useLocaleText()

const valuePoints = [
  ['⌂', lt('默认私密', '預設私密', 'Private by default')],
  ['⟡', lt('支持自托管', '支援自託管', 'Self-host ready')],
  ['⇄', lt('为团队流转而生', '為團隊流轉而生', 'Built for team flow')]
]
</script>

<template>
  <section class="login-screen">
    <div class="login-screen__body">
      <div class="login-screen__panel login-screen__panel--story">
        <span class="section-label">MMMAIL</span>
        <h1>{{ tr(lt('面向专注型企业的安全沟通。', '面向專注型企業的安全溝通。', 'Secure communication for focused enterprise.')) }}</h1>
        <p>{{ tr(lt('一个以瑞士工程方法打造的生产力环境，服务于重视隐私、深度工作与完整数据主权的组织。', '一個以瑞士工程方法打造的生產力環境，服務於重視隱私、深度工作與完整資料主權的組織。', 'A Swiss-engineered productivity environment designed for privacy-first organizations who value calm, deep work and total data sovereignty.')) }}</p>

        <div class="value-grid">
          <article v-for="([icon, point], index) in valuePoints" :key="index" class="value-tile">
            <span class="value-tile__icon">{{ icon }}</span>
            <span>{{ tr(point) }}</span>
          </article>
        </div>

        <div class="hero-surface">
          <div class="scene-lamp">
            <span class="scene-lamp__base" />
            <span class="scene-lamp__arm scene-lamp__arm--primary" />
            <span class="scene-lamp__arm scene-lamp__arm--secondary" />
            <span class="scene-lamp__head" />
          </div>
          <div class="scene-desk">
            <span class="scene-desk__top" />
            <span class="scene-desk__leg scene-desk__leg--left" />
            <span class="scene-desk__leg scene-desk__leg--right" />
          </div>
          <div class="scene-monitor">
            <span class="scene-monitor__screen" />
            <span class="scene-monitor__stand" />
          </div>
          <div class="scene-chair">
            <span class="scene-chair__back" />
            <span class="scene-chair__seat" />
            <span class="scene-chair__leg" />
          </div>
          <span class="scene-paper scene-paper--a" />
          <span class="scene-paper scene-paper--b" />
        </div>
      </div>

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
    </div>

    <footer class="login-screen__legal">
      <span>{{ tr(lt('© 2026 MMMAIL AG. 瑞士隐私保障。', '© 2026 MMMAIL AG. 瑞士隱私保障。', '© 2026 MMMAIL AG. Swiss privacy guaranteed.')) }}</span>
      <div>
        <a href="/boundary">{{ tr(lt('隐私', '隱私', 'Privacy')) }}</a>
        <a href="/boundary">{{ tr(lt('边界', '邊界', 'Boundary')) }}</a>
        <a href="/boundary">{{ tr(lt('状态', '狀態', 'Status')) }}</a>
        <span>{{ tr(lt('支持 EN / 简 / 繁', '支援 EN / 簡 / 繁', 'Supports EN / 简 / 繁')) }}</span>
      </div>
    </footer>
  </section>
</template>

<style scoped>
.login-screen {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.login-screen__body {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  flex: 1;
}

.login-screen__panel {
  padding: 34px 38px 28px;
}

.login-screen__panel--story {
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--mm-border);
}

h1 {
  max-width: 480px;
  margin: 20px 0 18px;
  font-size: clamp(60px, 4.6vw, 76px);
  line-height: 0.92;
  letter-spacing: -0.06em;
}

h2 {
  margin: 10px 0 24px;
  font-size: 32px;
  line-height: 1;
  letter-spacing: -0.05em;
}

p {
  max-width: 460px;
  margin: 0;
  color: var(--mm-text-secondary);
  font-size: 13px;
  line-height: 1.85;
}

.value-grid {
  display: grid;
  gap: 10px;
  margin-top: 46px;
}

.value-tile {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: fit-content;
  min-height: 44px;
  padding: 0 14px;
  background: var(--mm-card);
  border: 1px solid var(--mm-border);
  border-radius: 14px;
  font-size: 11px;
  letter-spacing: 0.02em;
  color: var(--mm-text-secondary);
}

.value-tile__icon {
  width: 20px;
  height: 20px;
  display: inline-grid;
  place-items: center;
  border: 1px solid var(--mm-border);
  border-radius: 6px;
  color: var(--mm-ink);
  font-size: 10px;
}

.hero-surface {
  position: relative;
  margin-top: auto;
  min-height: 302px;
  border: 1px solid var(--mm-border-strong);
  border-radius: 16px;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.62), transparent 25%),
    linear-gradient(135deg, #d5dbe1 0%, #8e959c 100%);
  overflow: hidden;
}

.scene-desk__top,
.scene-desk__leg,
.scene-monitor__screen,
.scene-monitor__stand,
.scene-chair__back,
.scene-chair__seat,
.scene-chair__leg,
.scene-paper,
.scene-lamp__base,
.scene-lamp__arm,
.scene-lamp__head {
  position: absolute;
  display: block;
}

.scene-desk__top {
  right: 32px;
  bottom: 70px;
  left: 32px;
  height: 16px;
  border-radius: 10px;
  background: rgba(244, 246, 248, 0.96);
  box-shadow: 0 18px 36px rgba(31, 41, 55, 0.16);
}

.scene-desk__leg {
  bottom: 16px;
  width: 10px;
  height: 92px;
  background: rgba(248, 249, 250, 0.9);
}

.scene-desk__leg--left {
  left: 72px;
  transform: rotate(18deg);
}

.scene-desk__leg--right {
  right: 72px;
  transform: rotate(-18deg);
}

.scene-monitor__screen {
  left: 160px;
  bottom: 92px;
  width: 102px;
  height: 64px;
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(78, 86, 94, 0.96) 0%, rgba(41, 47, 55, 0.98) 100%);
  box-shadow: 0 12px 24px rgba(31, 41, 55, 0.18);
}

.scene-monitor__stand {
  left: 204px;
  bottom: 70px;
  width: 16px;
  height: 24px;
  border-radius: 8px;
  background: rgba(233, 237, 241, 0.95);
}

.scene-chair__back {
  left: 236px;
  bottom: 76px;
  width: 44px;
  height: 98px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(90, 96, 104, 0.96) 0%, rgba(54, 58, 63, 0.98) 100%);
  box-shadow: 0 14px 24px rgba(31, 41, 55, 0.16);
}

.scene-chair__seat {
  left: 222px;
  bottom: 56px;
  width: 70px;
  height: 22px;
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(77, 83, 90, 0.96) 0%, rgba(51, 56, 62, 0.98) 100%);
}

.scene-chair__leg {
  left: 254px;
  bottom: 18px;
  width: 8px;
  height: 42px;
  border-radius: 999px;
  background: rgba(236, 239, 242, 0.9);
}

.scene-lamp__base {
  left: 82px;
  bottom: 74px;
  width: 26px;
  height: 6px;
  border-radius: 999px;
  background: rgba(245, 247, 248, 0.94);
}

.scene-lamp__arm {
  left: 92px;
  height: 5px;
  border-radius: 999px;
  background: rgba(248, 249, 250, 0.96);
  transform-origin: left center;
}

.scene-lamp__arm--primary {
  bottom: 98px;
  width: 56px;
  transform: rotate(-24deg);
}

.scene-lamp__arm--secondary {
  left: 142px;
  bottom: 124px;
  width: 40px;
  transform: rotate(18deg);
}

.scene-lamp__head {
  left: 178px;
  bottom: 129px;
  width: 22px;
  height: 14px;
  border-radius: 10px 10px 12px 12px;
  background: rgba(250, 250, 250, 0.98);
}

.scene-paper {
  bottom: 80px;
  width: 34px;
  height: 8px;
  border-radius: 4px;
  background: rgba(250, 250, 250, 0.85);
}

.scene-paper--a {
  left: 290px;
  transform: rotate(12deg);
}

.scene-paper--b {
  left: 340px;
  width: 28px;
  transform: rotate(-8deg);
}

.login-screen__panel--form {
  display: flex;
  flex-direction: column;
}

.login-actions,
.signin-block__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.login-actions {
  font-size: 12px;
  color: var(--mm-text-secondary);
}

.outline-link {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 11px;
  background: #fff;
}

.signin-block {
  max-width: 420px;
  width: 100%;
  margin: auto auto 64px;
}

label {
  display: inline-block;
  margin: 18px 0 10px;
  color: var(--mm-text-secondary);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
}

.field {
  width: 100%;
  height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 12px;
  background: #fff;
  color: var(--mm-text);
  outline: none;
}

.field:focus {
  border-color: var(--mm-ink);
  box-shadow: 0 0 0 2px rgba(31, 41, 55, 0.08);
}

.primary-action {
  width: 100%;
  height: 36px;
  margin-top: 2px;
  border: 0;
  border-radius: 10px;
  background: #1f2937;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
}

.section-divider {
  position: relative;
  margin: 28px 0 18px;
  text-align: center;
  color: #7b8284;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.section-divider::before,
.section-divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 38%;
  height: 1px;
  background: var(--mm-border);
}

.section-divider::before {
  left: 0;
}

.section-divider::after {
  right: 0;
}

.ghost-action {
  width: 100%;
  min-height: 40px;
  border: 1px solid var(--mm-border);
  border-radius: 12px;
  color: var(--mm-text-secondary);
  background: var(--mm-card);
  font-size: 13px;
}

.mfa-strip {
  display: flex;
  align-items: start;
  justify-content: space-between;
  margin-top: 26px;
}

.mfa-strip strong {
  color: #7b8284;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.mfa-strip p {
  margin-top: 8px;
  max-width: 240px;
  font-size: 13px;
}

.otp-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
  margin-top: 14px;
}

.otp-grid span {
  display: grid;
  place-items: center;
  height: 48px;
  border: 1px solid var(--mm-border);
  border-radius: 12px;
  background: var(--mm-card);
  font-size: 22px;
}

.signin-block__row--muted {
  margin-top: 16px;
  font-size: 12px;
}

.login-screen__legal {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
  padding: 0 10px 0 12px;
  border-top: 1px solid var(--mm-border);
  color: #7b8284;
  font-size: 9px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.login-screen__legal div {
  display: flex;
  gap: 18px;
}

@media (max-width: 980px) {
  .login-screen__body {
    grid-template-columns: 1fr;
  }

  .login-screen__panel--story {
    border-right: 0;
    border-bottom: 1px solid var(--mm-border);
  }

  .login-screen__legal {
    gap: 12px;
    flex-direction: column;
    padding: 12px;
  }
}
</style>
