const buildDir = process.env.NUXT_BUILD_DIR || '.nuxt'

export default defineNuxtConfig({
  compatibilityDate: '2025-01-15',
  ssr: false,
  buildDir,
  devtools: { enabled: false },
  modules: ['@pinia/nuxt', '@unocss/nuxt'],
  components: [
    {
      path: '~/components',
      pathPrefix: false
    }
  ],
  css: ['~/assets/styles/main.css', 'element-plus/dist/index.css'],
  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE || 'http://localhost:8080',
      authCsrfCookieName: process.env.NUXT_PUBLIC_AUTH_CSRF_COOKIE_NAME || 'MMMAIL_CSRF_TOKEN',
      enablePreviewModules: process.env.NUXT_PUBLIC_ENABLE_PREVIEW_MODULES === 'true'
    }
  },
  typescript: {
    strict: true,
    typeCheck: true
  }
})
