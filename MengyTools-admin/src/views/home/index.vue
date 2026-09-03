<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
  getDashboardSummary,
  getRecentArticles,
  type DashboardSummary,
  type RecentArticle
} from '@/api'

const userStore = useUserStore()
const router = useRouter()

const summary = ref<DashboardSummary | null>(null)
const recentList = ref<RecentArticle[]>([])
const loading = ref(false)

const statusTag = (s?: number) => {
  if (s === 0) return { type: 'info', text: '草稿' }
  if (s === 1) return { type: 'success', text: '已发布' }
  if (s === 2) return { type: 'warning', text: '定时发布' }
  return { type: 'info', text: '-' }
}

// 快捷入口：按当前用户权限过滤（超管 permissions 为 ['*']）
const hasPermission = (p: string) => {
  const perms = userStore.userInfo?.permissions || []
  return perms.includes('*') || perms.includes(p)
}
const quickLinks = computed(() =>
  [
    { title: '文章管理', icon: 'Edit', path: '/blog/article', permission: 'article:list' },
    { title: '博客分类', icon: 'Files', path: '/blog/category', permission: 'blog:category:list' },
    { title: '导航站点', icon: 'Link', path: '/nav/site', permission: 'nav:list' },
    { title: '导航分类', icon: 'FolderOpened', path: '/nav/category', permission: 'nav:category:list' },
    { title: '用户管理', icon: 'User', path: '/system/user', permission: 'user:list' },
    { title: '角色管理', icon: 'UserFilled', path: '/system/role', permission: 'role:list' },
    { title: '菜单管理', icon: 'Menu', path: '/system/menu', permission: 'menu:list' }
  ].filter(l => hasPermission(l.permission))
)

const goEdit = (row: RecentArticle) => router.push(`/blog/article/edit/${row.id}`)

const fetchData = async () => {
  loading.value = true
  try {
    const [s, r] = await Promise.all([getDashboardSummary(), getRecentArticles()])
    summary.value = s
    recentList.value = r.records || []
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <!-- 欢迎 -->
    <el-card shadow="never" class="welcome-card">
      <div class="welcome">
        <div>
          <h2>欢迎回来，{{ userStore.userInfo?.nickname || '管理员' }}</h2>
          <p class="sub">这是 MengyTools 数字资产平台管理端，以下是站点概览。</p>
        </div>
        <el-button type="primary" @click="router.push('/blog/article/edit/new')">
          <el-icon style="margin-right: 4px"><Plus /></el-icon>
          写文章
        </el-button>
      </div>
    </el-card>

    <!-- 统计卡 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">文章总数</div>
          <div class="stat-value">{{ summary?.articleTotal ?? '-' }}</div>
          <div class="stat-sub">
            已发布 {{ summary?.articlePublished ?? 0 }} · 草稿 {{ summary?.articleDraft ?? 0 }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">博客分类</div>
          <div class="stat-value">{{ summary?.categoryTotal ?? '-' }}</div>
          <div class="stat-sub">共 {{ summary?.categoryTotal ?? 0 }} 个分类</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">导航站点</div>
          <div class="stat-value">{{ summary?.navSiteTotal ?? '-' }}</div>
          <div class="stat-sub">分属 {{ summary?.navCategoryTotal ?? 0 }}个分类</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-label">工具卡片</div>
          <div class="stat-value">{{ summary?.toolTotal ?? '-' }}</div>
          <div class="stat-sub">注册用户 {{ summary?.userTotal ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近文章 + 快捷入口 -->
    <el-row :gutter="16" class="content-row">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <span>最近文章</span>
              <el-button text type="primary" @click="router.push('/blog/article')">
                查看全部
              </el-button>
            </div>
          </template>
          <el-table v-if="recentList.length" :data="recentList" size="large">
            <el-table-column label="标题" min-width="220">
              <template #default="{ row }">
                <el-link type="primary" :underline="false" @click="goEdit(row)">
                  <el-tag v-if="row.isTop" type="danger" size="small" effect="dark">置顶</el-tag>
                  {{ row.title }}
                </el-link>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status).type as any" size="small">
                  {{ statusTag(row.status).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="viewCount" label="阅读量" width="90" />
            <el-table-column label="创建时间" width="110">
              <template #default="{ row }">{{ (row.createTime || '').slice(0, 10) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="还没有文章，点右上角「写文章」开始创作" :image-size="80" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never" class="quick-card">
          <template #header>
            <div class="card-head"><span>快捷入口</span></div>
          </template>
          <div class="quick-grid">
            <div
              v-for="link in quickLinks"
              :key="link.path"
              class="quick-item"
              @click="router.push(link.path)"
            >
              <el-icon :size="20" class="quick-icon">
                <component :is="link.icon" />
              </el-icon>
              <span>{{ link.title }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  padding: 4px 0;
}
.welcome-card {
  margin-bottom: 16px;
  .welcome {
    display: flex;
    align-items: center;
    justify-content: space-between;
    h2 {
      margin: 0 0 6px;
      font-size: 20px;
    }
    .sub {
      margin: 0;
      color: #909399;
      font-size: 13px;
    }
  }
}
.stat-row {
  margin-bottom: 16px;
  .stat-label {
    color: #909399;
    font-size: 13px;
  }
  .stat-value {
    font-size: 30px;
    font-weight: 600;
    margin: 8px 0 4px;
  }
  .stat-sub {
    color: #c0c4cc;
    font-size: 12px;
  }
}
.content-row {
  .card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: 600;
  }
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  .quick-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 18px 8px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;
    font-size: 13px;
    color: #606266;
    &:hover {
      border-color: #409eff;
      color: #409eff;
      transform: translateY(-2px);
    }
    .quick-icon {
      color: #409eff;
    }
  }
}
</style>
