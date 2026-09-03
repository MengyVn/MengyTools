<script setup lang="ts">
import type { MenuNode } from '@/api/system'

defineProps<{
  menus: MenuNode[]
}>()
</script>

<template>
  <template v-for="node in menus" :key="node.id">
    <!-- 隐藏节点不渲染 -->
    <template v-if="node.visible === 1">
      <!-- M 目录：分组（递归子菜单） -->
      <el-sub-menu v-if="node.menuType === 'M' && node.children?.length" :index="node.path">
        <template #title>
          <el-icon v-if="node.icon"><component :is="node.icon" /></el-icon>
          <span>{{ node.menuName }}</span>
        </template>
        <SideMenu :menus="node.children" />
      </el-sub-menu>
      <!-- C 菜单：可点击跳转 -->
      <el-menu-item v-else-if="node.menuType === 'C'" :index="'/' + node.path">
        <el-icon v-if="node.icon"><component :is="node.icon" /></el-icon>
        <template #title>{{ node.menuName }}</template>
      </el-menu-item>
      <!-- F 按钮：不渲染 -->
    </template>
  </template>
</template>
