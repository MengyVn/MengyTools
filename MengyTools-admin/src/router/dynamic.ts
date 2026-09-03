import type { RouteRecordRaw } from 'vue-router'
import type { MenuNode } from '@/api/system'

// 预加载 src/views 下所有视图组件，key 形如 '/src/views/blog/article-list.vue'
const viewModules = import.meta.glob('/src/views/**/*.vue')

/**
 * 将后端菜单树转为 vue-router 动态路由（扁平模式）。
 * - M 目录：仅用于侧边栏分组，不生成路由
 * - C 菜单：作为 Layout 的直接子路由（path 相对于 Layout 根 '/'）
 * - F 按钮：不生成路由，仅用于按钮权限校验
 *
 * 组件映射：菜单 component 字段存相对路径（不含扩展名），
 * 例如 'blog/article-list' → /src/views/blog/article-list.vue
 */
export function generateRoutes(menus: MenuNode[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  const walk = (nodes: MenuNode[]) => {
    for (const node of nodes) {
      if (node.menuType === 'C' && node.path) {
        routes.push({
          path: node.path, // 相对 Layout('/')
          name: routeName(node),
          component: resolveComponent(node.component),
          meta: {
            title: node.menuName,
            icon: node.icon || undefined,
            permission: node.permission || undefined,
            hidden: node.visible === 0
          }
        })
      }
      if (node.children && node.children.length) {
        walk(node.children)
      }
    }
  }
  walk(menus)
  return routes
}

function resolveComponent(component: string) {
  if (!component) {
    return () => import('@/views/error/404.vue')
  }
  const key = `/src/views/${component}.vue`
  const loader = viewModules[key]
  if (!loader) {
    console.warn(`[dynamic] 未找到组件: ${component} (key=${key})`)
    return () => import('@/views/error/404.vue')
  }
  return loader as () => Promise<unknown>
}

/** 由菜单 path 生成唯一路由 name，'/' 与 ':' 替换为 '_' */
function routeName(node: MenuNode): string {
  return 'm_' + node.path.replace(/[/:]/g, '_')
}
