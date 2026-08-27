import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { setupNotificationNavigation } from './utils/notificationNavigation'
import './styles/base.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)

setupNotificationNavigation(router)

app.mount('#app')
