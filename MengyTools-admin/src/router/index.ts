import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'
import { usePermissionStore } from '@/store/permission'

// 静态基础路由：登录 / 404 / 布局根(含静态首页)
// 其余业务菜单由后端返回，经 generateRoutes 动态 addRoute 注入到 Layout 下。
const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layout/index.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { hidden: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: staticRoutes
})

/**
 * 重置路由表为静态路由（退出登录 / 切换账号时清除动态路由）。
 * vue-router 4 官方推荐方式：用全新 router 的 matcher 覆盖。
 */
export function resetRouter() {
  const newRouter = createRouter({
    history: createWebHistory(),
    routes: staticRoutes
  })
  ;(router as unknown as { matcher: unknown }).matcher = newRouter.matcher
}

// 全局前置守卫：未登录跳登录；已登录首访时拉取用户信息 + 动态路由
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  const permissionStore = usePermissionStore()
  document.title = to.meta.title ? `${to.meta.title} - MengyTools` : 'MengyTools'

  // 登录页：已登录直接进后台
  if (to.path === '/login') {
    if (userStore.token) {
      next({ path: '/' })
      return
    }
    next()
    return
  }

  // 未登录
  if (!userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 已登录：确保用户信息 + 动态路由就绪
  try {
    if (!userStore.userInfo) {
      await userStore.fetchUserInfo()
    }
    if (!permissionStore.loaded) {
      const dynamicRoutes = await permissionStore.fetchMenus()
      dynamicRoutes.forEach(r => router.addRoute('Layout', r))
      // 动态路由注入后需按 path 重新解析当前目标。
      // 注意：不能直接 next({ ...to })——刷新时 to 已被 catch-all 匹配为
      // NotFound，展开会把 name:'NotFound' 带过去（name 优先于 path），
      // 导致重新导航仍落在 404 页。必须只保留 path/query/hash。
      next({ path: to.path, query: to.query, hash: to.hash, replace: true })
      return
    }
    next()
  } catch {
    userStore.logout()
    permissionStore.reset()
    next({ path: '/login', query: { redirect: to.fullPath } })
  }
})

export default router
