import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// 全量引入 Element Plus 样式（含 ElMessage/ElMessageBox 等 service API 的样式）
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { permission } from './directives/permission'
import './styles/index.css'

const app = createApp(App)

// 注册 Element Plus 图标（按需，组件本体由 unplugin 自动导入）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册按钮权限指令
app.directive('permission', permission)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)

app.mount('#app')
