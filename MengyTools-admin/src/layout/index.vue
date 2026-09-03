<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { usePermissionStore } from '@/store/permission'
import { type MenuNode } from '@/api/system'
import SideMenu from '@/components/SideMenu.vue'

const userStore = useUserStore()
const permissionStore = usePermissionStore()
const route = useRoute()
const router = useRouter()

// el-menu router 模式按 index 匹配当前激活项
const activeMenu = computed(() => route.path)

// 侧边栏伸缩
const collapsed = ref(false)
const asideWidth = computed(() => (collapsed.value ? '64px' : '220px'))
const toggleCollapse = () => (collapsed.value = !collapsed.value)

// 顶部面包屑：根据当前路由反查菜单树，得到 [一级菜单名, 二级菜单名, ...]
const breadcrumb = computed<string[]>(() => {
  const fullPath = route.path.replace(/^\//, '')
  if (!fullPath) return []
  const result: string[] = []
  const walk = (nodes: MenuNode[], parents: string[]): boolean => {
    for (const n of nodes) {
      const names = [...parents, n.menuName]
      if (n.menuType === 'C' && matchPath(n.path, fullPath)) {
        result.push(...names)
        return true
      }
      if (n.children?.length && walk(n.children, names)) return true
    }
    return false
  }
  walk(permissionStore.menus, [])
  return result
})

// 支持带 :param 的菜单 path 匹配（如 blog/article/edit/:id）
function matchPath(menuPath: string, fullPath: string): boolean {
  if (!menuPath) return false
  const a = menuPath.split('/')
  const b = fullPath.split('/').filter(Boolean)
  if (a.length !== b.length) return false
  return a.every((seg, i) => seg.startsWith(':') || seg === b[i])
}

const handleLogout = async () => {
  userStore.logout()
  router.replace('/login')
}
</script>

<template>
  <el-container class="app-layout">
    <el-aside :width="asideWidth" class="app-aside">
      <div class="logo">
        <span v-if="!collapsed">MengyTools</span>
        <span v-else>M</span>
      </div>
      <el-menu
        router
        :collapse="collapsed"
        :default-active="activeMenu"
        background-color="#001529"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        class="side-menu"
      >
        <el-menu-item index="/home">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        <SideMenu :menus="permissionStore.menus" />
      </el-menu>
      <!-- 伸缩按钮 -->
      <div class="collapse-btn" @click="toggleCollapse">
        <el-icon>
          <Fold v-if="!collapsed" />
          <Expand v-else />
        </el-icon>
        <span v-if="!collapsed" class="collapse-text">收起菜单</span>
      </div>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <el-breadcrumb v-if="breadcrumb.length" separator=">">
            <el-breadcrumb-item v-for="name in breadcrumb" :key="name">{{ name }}</el-breadcrumb-item>
          </el-breadcrumb>
          <span v-else class="title">管理后台</span>
        </div>
        <el-dropdown trigger="click">
          <span class="user">
            <el-icon style="vertical-align: middle"><UserFilled /></el-icon>
            {{ userStore.userInfo?.nickname || '管理员' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.app-layout {
  height: 100vh;
}
.app-aside {
  display: flex;
  flex-direction: column;
  background-color: #001529;
  transition: width 0.2s ease;
  .logo {
    height: 60px;
    line-height: 60px;
    text-align: center;
    color: #fff;
    font-size: 18px;
    font-weight: 600;
    overflow: hidden;
    white-space: nowrap;
  }
  .side-menu {
    border-right: none;
    flex: 1;
    overflow-x: hidden;
    overflow-y: auto;
    // 收起模式下隐藏展开箭头与子菜单文字，保留 hover 弹出
    &:not(.el-menu--collapse) {
      width: 100%;
    }
  }
  .collapse-btn {
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    color: #bfcbd9;
    cursor: pointer;
    border-top: 1px solid #1f2d3d;
    user-select: none;
    &:hover {
      color: #409eff;
      background-color: #1f2d3d;
    }
    .collapse-text {
      font-size: 13px;
    }
  }
}
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  border-bottom: 1px solid #eee;
  .title {
    font-weight: 600;
  }
  .user {
    cursor: pointer;
    color: #409eff;
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}
.app-main {
  background-color: #f0f2f5;
}
</style>
