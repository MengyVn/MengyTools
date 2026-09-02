<script setup lang="ts">
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
</script>

<template>
  <el-card>
    <h2>欢迎回来，{{ userStore.userInfo?.nickname || '管理员' }}</h2>
    <p>这是 MengyTools 数字资产平台管理端首页。</p>
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">文章数</div>
          <div class="stat-value">0</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">导航站点</div>
          <div class="stat-value">0</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">工具卡片</div>
          <div class="stat-value">0</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">今日 PV</div>
          <div class="stat-value">0</div>
        </el-card>
      </el-col>
    </el-row>
  </el-card>
</template>

<style scoped lang="scss">
.stat-row {
  margin-top: 20px;
  .stat-label {
    color: #909399;
    font-size: 14px;
  }
  .stat-value {
    font-size: 28px;
    font-weight: 600;
    margin-top: 8px;
  }
}
</style>
