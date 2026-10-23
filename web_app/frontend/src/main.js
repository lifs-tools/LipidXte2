import { createApp } from 'vue'
import axios from 'axios'

// Global stylesheets first, so the page styles in the components can override Bootstrap's defaults.
import './assets/bootstrap.css'
import 'font-awesome/css/font-awesome.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)
app.config.globalProperties.$http = axios
app.use(router)
app.mount('#app')
