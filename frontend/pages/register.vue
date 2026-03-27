<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useAuthApi } from '~/composables/useAuthApi'
import { useI18n } from '~/composables/useI18n'
import { useAuthStore } from '~/stores/auth'
import { useOrgAccessStore } from '~/stores/org-access'
import { resolveHomeRoute } from '~/utils/org-product-access'

definePageMeta({ layout: 'blank' })

const form = reactive({ email: '', password: '', displayName: '' })
const loading = ref(false)
const errorMessage = ref('')
const { register } = useAuthApi()
const authStore = useAuthStore()
const orgAccessStore = useOrgAccessStore()
const { t } = useI18n()

async function onSubmit(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  try {
    await register(form.email, form.password, form.displayName)
    await orgAccessStore.ensureLoaded()
    await navigateTo(resolveHomeRoute(orgAccessStore.isProductEnabled, authStore.user?.mailAddressMode))
  } catch (error: unknown) {
    errorMessage.value = error instanceof Error ? error.message : t('auth.messages.registrationFailed')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-card mm-card">
    <h1>{{ t('auth.register.title') }}</h1>
    <el-form label-position="top" @submit.prevent="onSubmit">
      <el-form-item :label="t('auth.fields.displayName')">
        <el-input v-model="form.displayName" />
      </el-form-item>
      <el-form-item :label="t('auth.fields.email')">
        <el-input v-model="form.email" type="email" autocomplete="username" />
      </el-form-item>
      <el-form-item :label="t('auth.fields.password')">
        <el-input v-model="form.password" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-alert v-if="errorMessage" :closable="false" type="error" :title="errorMessage" />
      <el-button type="primary" native-type="submit" :loading="loading" class="submit-btn">{{ t('auth.actions.register') }}</el-button>
      <div class="link-row">
        <NuxtLink to="/login">{{ t('auth.actions.backToLogin') }}</NuxtLink>
      </div>
    </el-form>
  </section>
</template>

<style scoped>
.auth-card {
  width: 460px;
  padding: 24px;
}

.submit-btn {
  width: 100%;
  margin-top: 12px;
}

.link-row {
  margin-top: 12px;
  text-align: center;
}
</style>
