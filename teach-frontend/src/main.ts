import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { installAntDesign } from './plugins/ant-design'
import 'ant-design-vue/dist/reset.css'
import './assets/teacher-ui.css'
import './assets/responsive.css'
import axios from 'axios' // 1. 引入 axios

// --- 2. 关键配置：允许跨域携带 Cookie (SessionID) ---
axios.defaults.withCredentials = true;

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
installAntDesign(app)
app.mount('#app')
