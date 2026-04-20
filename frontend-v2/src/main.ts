import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createDiscreteApi } from 'naive-ui'
import App from '@/app/App.vue'
import { router } from '@/app/router'
import '@/styles/global.css'

const app = createApp(App)
const pinia = createPinia()

createDiscreteApi(['message'])

app.use(pinia)
app.use(router)
app.mount('#app')
