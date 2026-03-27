import type { AxiosInstance } from 'axios'

declare module '#app' {
  interface NuxtApp {
    $apiClient: AxiosInstance
  }
}

declare module 'nuxt/app' {
  interface NuxtLayouts {
    'public-pass': Record<string, never>
  }
}

declare module 'vue' {
  interface ComponentCustomProperties {
    $apiClient: AxiosInstance
  }
}

export {}
