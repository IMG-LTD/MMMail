<script setup lang="ts">
import { computed } from 'vue';
import type { Component } from 'vue';
import { getPaletteColorByNumber, mixColor } from '@sa/color';
import { loginModuleRecord } from '@/constants/app';
import { useAppStore } from '@/store/modules/app';
import { useThemeStore } from '@/store/modules/theme';
import { $t } from '@/locales';
import PwdLogin from './modules/pwd-login.vue';
import CodeLogin from './modules/code-login.vue';
import Register from './modules/register.vue';
import ResetPwd from './modules/reset-pwd.vue';
import BindWechat from './modules/bind-wechat.vue';

const COLOR_WHITE = '#ffffff';
const COLOR_INK = '#0f172a';
const LIGHT_SHELL_BLEND_RATIO = 0.04;
const DARK_SHELL_BLEND_RATIO = 0.24;
const LIGHT_BRAND_PALETTE_NUMBER = 600 as const;
const DARK_BRAND_PALETTE_NUMBER = 700 as const;
const LIGHT_SURFACE_COLOR = '#ffffff';
const DARK_SURFACE_COLOR = '#18181c';
const LIGHT_BORDER_COLOR = 'rgba(15, 23, 42, 0.08)';
const DARK_BORDER_COLOR = 'rgba(255, 255, 255, 0.08)';

interface Props {
  /** The login module */
  module?: UnionKey.LoginModule;
}

const props = defineProps<Props>();

const appStore = useAppStore();
const themeStore = useThemeStore();

interface LoginModule {
  label: App.I18n.I18nKey;
  component: Component;
}

const moduleMap: Record<UnionKey.LoginModule, LoginModule> = {
  'pwd-login': { label: loginModuleRecord['pwd-login'], component: PwdLogin },
  'code-login': { label: loginModuleRecord['code-login'], component: CodeLogin },
  register: { label: loginModuleRecord.register, component: Register },
  'reset-pwd': { label: loginModuleRecord['reset-pwd'], component: ResetPwd },
  'bind-wechat': { label: loginModuleRecord['bind-wechat'], component: BindWechat }
};

const activeModule = computed(() => moduleMap[props.module || 'pwd-login']);

const brandColor = computed(() =>
  getPaletteColorByNumber(
    themeStore.themeColor,
    themeStore.darkMode ? DARK_BRAND_PALETTE_NUMBER : LIGHT_BRAND_PALETTE_NUMBER
  )
);

const authShellStyle = computed(() => {
  const baseColor = themeStore.darkMode ? COLOR_INK : COLOR_WHITE;
  const ratio = themeStore.darkMode ? DARK_SHELL_BLEND_RATIO : LIGHT_SHELL_BLEND_RATIO;

  return { backgroundColor: mixColor(baseColor, themeStore.themeColor, ratio) };
});

const authSurfaceStyle = computed(() => ({
  backgroundColor: themeStore.darkMode ? DARK_SURFACE_COLOR : LIGHT_SURFACE_COLOR,
  borderColor: themeStore.darkMode ? DARK_BORDER_COLOR : LIGHT_BORDER_COLOR
}));

const authBrandStyle = computed(() => ({ backgroundColor: brandColor.value }));
</script>

<template>
  <div class="auth-shell size-full overflow-auto p-24px lt-sm:p-16px" :style="authShellStyle">
    <div class="min-h-full flex-center">
      <section
        class="grid w-full max-w-1120px grid-cols-2 overflow-hidden border rd-8px shadow-sm lt-md:grid-cols-1"
        :style="authSurfaceStyle"
      >
        <aside
          class="auth-brand-panel min-h-620px flex flex-col justify-between p-40px text-white lt-md:min-h-auto lt-md:gap-28px lt-md:p-28px"
          :style="authBrandStyle"
        >
          <div class="flex-y-center gap-14px">
            <SystemLogo class="size-56px lt-sm:size-46px" />
            <div>
              <h1 class="m-0 text-26px font-600 lt-sm:text-22px">{{ $t('system.title') }}</h1>
              <p class="m-0 pt-4px text-14px opacity-75">{{ $t('page.login.common.loginOrRegister') }}</p>
            </div>
          </div>

          <div class="max-w-460px">
            <h2 class="m-0 text-36px font-600 leading-tight lt-sm:text-28px">
              {{ $t('page.login.common.headline') }}
            </h2>
            <p class="m-0 pt-16px text-16px leading-7 opacity-82">
              {{ $t('page.login.common.subtitle') }}
            </p>
          </div>

          <div class="flex flex-wrap gap-10px">
            <NTag round :bordered="false">{{ $t('route.mail') }}</NTag>
            <NTag round :bordered="false">{{ $t('route.drive') }}</NTag>
            <NTag round :bordered="false">{{ $t('route.calendar') }}</NTag>
            <NTag round :bordered="false">{{ $t('route.admin') }}</NTag>
          </div>
        </aside>

        <main class="auth-form-panel flex flex-col justify-center p-40px lt-sm:p-24px">
          <div class="mx-auto w-full max-w-430px">
            <div class="mb-28px flex-y-center justify-end gap-12px">
              <ThemeSchemaSwitch
                :theme-schema="themeStore.themeScheme"
                :show-tooltip="false"
                class="text-20px lt-sm:text-18px"
                @switch="themeStore.toggleThemeScheme"
              />
              <LangSwitch
                v-if="themeStore.header.multilingual.visible"
                :lang="appStore.locale"
                :lang-options="appStore.localeOptions"
                :show-tooltip="false"
                @change-lang="appStore.changeLocale"
              />
            </div>

            <header class="mb-24px">
              <p class="m-0 text-13px text-primary font-600">{{ $t('page.login.common.loginOrRegister') }}</p>
              <h2 class="m-0 pt-8px text-28px font-600 lt-sm:text-24px">{{ $t(activeModule.label) }}</h2>
            </header>

            <Transition :name="themeStore.page.animateMode" mode="out-in" appear>
              <component :is="activeModule.component" />
            </Transition>
          </div>
        </main>
      </section>
    </div>
  </div>
</template>

<style scoped></style>
