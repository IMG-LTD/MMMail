<script setup lang="ts">
import { NButton, NInput, NTag } from 'naive-ui'
import { lt, useLocaleText } from '@/locales'

const { tr } = useLocaleText()

const steps = [
  lt('身份', '身分', 'Identity'),
  lt('密码', '密碼', 'Password'),
  lt('安全', '安全', 'Security')
]
</script>

<template>
  <section class="public-shell page-shell">
    <div class="register-card surface-card">
      <div class="register-card__header">
        <div>
          <span class="section-label">{{ tr(lt('引导流程', '引導流程', 'Onboarding flow')) }}</span>
          <h1 class="page-title">{{ tr(lt('创建机构工作区', '建立機構工作區', 'Create an institutional workspace')) }}</h1>
        </div>
        <n-tag round type="info" :bordered="false">{{ tr(lt('第 2 步 / 共 3 步', '第 2 步 / 共 3 步', 'Step 2 / 3')) }}</n-tag>
      </div>

      <div class="register-progress">
        <div
          v-for="(step, index) in steps"
          :key="index"
          class="register-progress__step"
          :class="{ 'register-progress__step--active': index === 1 }"
        >
          {{ tr(step) }}
        </div>
      </div>

      <div class="register-grid">
        <div class="register-grid__story">
          <p class="section-label">{{ tr(lt('账户安全', '帳戶安全', 'Account security')) }}</p>
          <h2>{{ tr(lt('设置一个能保护长期组织访问的密码。', '設定一個能保護長期組織存取的密碼。', 'Set a password that protects long-lived organizational access.')) }}</h2>
          <p class="page-subtitle">{{ tr(lt('该密码用于保护你的本地解密上下文。恢复包生成会在此步骤后立即开始。', '該密碼用於保護你的本機解密內容脈絡。復原包建立會在此步驟後立即開始。', 'The password protects your local decryption context. Recovery kit generation will follow immediately after this step.')) }}</p>
        </div>

        <form class="register-grid__form">
          <label>{{ tr(lt('工作区名称', '工作區名稱', 'Workspace name')) }}</label>
          <n-input :placeholder="tr(lt('MMMail 欧洲', 'MMMail 歐洲', 'MMMail Europe'))" />
          <label>{{ tr(lt('主管理员邮箱', '主要管理員郵箱', 'Primary admin email')) }}</label>
          <n-input placeholder="founder@company.eu" />
          <label>{{ tr(lt('密码', '密碼', 'Password')) }}</label>
          <n-input type="password" :placeholder="tr(lt('使用 16 位以上字符', '使用 16 位以上字元', 'Use 16+ characters'))" />
          <label>{{ tr(lt('确认密码', '確認密碼', 'Confirm password')) }}</label>
          <n-input type="password" :placeholder="tr(lt('再次输入密码', '再次輸入密碼', 'Repeat password'))" />

          <div class="strength-meter">
            <span />
            <span />
            <span />
            <span class="strength-meter__active" />
          </div>

          <div class="register-actions">
            <n-button secondary>{{ tr(lt('返回', '返回', 'Back')) }}</n-button>
            <n-button type="primary">{{ tr(lt('继续进入恢复包', '繼續進入復原包', 'Continue to Recovery Kit')) }}</n-button>
          </div>
        </form>
      </div>
    </div>
  </section>
</template>

<style scoped>
.public-shell {
  padding: 56px 0;
}

.register-card {
  padding: 32px;
}

.register-card__header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.register-progress {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin: 26px 0 32px;
}

.register-progress__step {
  min-height: 42px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  border: 1px solid var(--mm-border);
  background: var(--mm-card-muted);
  color: var(--mm-text-secondary);
}

.register-progress__step--active {
  background: #e9eef6;
  color: var(--mm-ink);
}

.register-grid {
  display: grid;
  grid-template-columns: 0.92fr 1.08fr;
  gap: 28px;
}

.register-grid__story h2 {
  max-width: 340px;
  margin: 8px 0 16px;
  font-size: 36px;
  line-height: 1;
  letter-spacing: -0.05em;
}

.register-grid__form {
  display: grid;
  gap: 8px;
}

label {
  margin-top: 8px;
  color: var(--mm-text-secondary);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.strength-meter {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 10px;
}

.strength-meter span {
  height: 8px;
  border-radius: 999px;
  background: #d8dde0;
}

.strength-meter__active {
  background: var(--mm-security);
}

.register-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
}

@media (max-width: 900px) {
  .register-grid {
    grid-template-columns: 1fr;
  }
}
</style>
