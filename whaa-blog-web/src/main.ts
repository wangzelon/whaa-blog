import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createHead } from '@vueuse/head'
import App from './App.vue'
import router from './router'
import './styles/global.css'

createApp(App).use(createPinia()).use(createHead()).use(router).mount('#app')
