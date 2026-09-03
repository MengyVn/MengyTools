import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { getUserMenus, type MenuNode } from '@/api/system'
import { generateRoutes } from '@/router/dynamic'

/**
 * 权限 store：当前用户菜单树 + 由菜单生成的动态路由。
 * 登录后由路由守卫触发 fetchMenus，再 router.addRoute 注入。
 */
export const usePermissionStore = defineStore('permission', () => {
  const menus = ref<MenuNode[]>([])
  const routes = ref<RouteRecordRaw[]>([])
  const loaded = ref(false)

  const fetchMenus = async () => {
    menus.value = await getUserMenus()
    routes.value = generateRoutes(menus.value)
    loaded.value = true
    return routes.value
  }

  const reset = () => {
    menus.value = []
    routes.value = []
    loaded.value = false
  }

  return { menus, routes, loaded, fetchMenus, reset }
})
