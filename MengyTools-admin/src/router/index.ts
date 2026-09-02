import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'content/blog',
        name: 'ArticleList',
        component: () => import('@/views/blog/article-list.vue'),
        meta: { title: '文章管理', icon: 'Edit' }
      },
      {
        path: 'content/blog/edit/:id',
        name: 'ArticleEdit',
        component: () => import('@/views/blog/article-edit.vue'),
        meta: { title: '编辑文章', hidden: true }
      },
      {
        path: 'content/blog-category',
        name: 'BlogCategory',
        component: () => import('@/views/blog/category-list.vue'),
        meta: { title: '博客分类', icon: 'Files' }
      },
      {
        path: 'content/nav-category',
        name: 'NavCategory',
        component: () => import('@/views/nav/category-list.vue'),
        meta: { title: '导航分类', icon: 'FolderOpened' }
      },
      {
        path: 'content/nav-site',
        name: 'NavSite',
        component: () => import('@/views/nav/site-list.vue'),
        meta: { title: '导航站点', icon: 'Link' }
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
  routes
})

// 全局前置守卫：未登录跳转登录
router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  document.title = to.meta.title ? `${to.meta.title} - MengyTools` : 'MengyTools'
  if (to.path === '/login') {
    // 已登录用户访问登录页，直接进后台
    if (userStore.token) {
      next({ path: '/' })
      return
    }
    next()
    return
  }
  if (!userStore.token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }
  next()
})

export default router
