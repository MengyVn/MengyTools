<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  listArticles,
  deleteArticle,
  toggleArticleTop,
  type ArticleListItem,
  type ArticleQuery
} from '@/api/blog'

const router = useRouter()
const loading = ref(false)
const list = ref<ArticleListItem[]>([])
const total = ref(0)
const query = reactive<ArticleQuery>({ page: 1, size: 10, title: '', status: undefined, categoryId: undefined })

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 },
  { label: '定时发布', value: 2 }
]
const statusTag = (s?: number) => {
  if (s === 0) return { type: 'info', text: '草稿' }
  if (s === 1) return { type: 'success', text: '已发布' }
  if (s === 2) return { type: 'warning', text: '定时发布' }
  return { type: 'info', text: '-' }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listArticles({ ...query, page: query.page, size: query.size })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.page = 1
  fetchList()
}
const handleReset = () => {
  query.title = ''
  query.status = undefined
  query.categoryId = undefined
  handleSearch()
}

const handleEdit = (row: ArticleListItem) => {
  router.push(`/blog/article/edit/${row.id}`)
}
const handleCreate = () => {
  router.push('/blog/article/edit/new')
}

const handleDelete = async (row: ArticleListItem) => {
  await ElMessageBox.confirm(`确定删除文章「${row.title}」吗？`, '提示', { type: 'warning' })
  await deleteArticle(row.id)
  ElMessage.success('已删除')
  fetchList()
}

const handleToggleTop = async (row: ArticleListItem) => {
  await toggleArticleTop(row.id, !row.isTop)
  ElMessage.success(row.isTop ? '已取消置顶' : '已置顶')
  fetchList()
}

onMounted(fetchList)
</script>

<template>
  <div class="article-list">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="标题">
          <el-input v-model="query.title" placeholder="文章标题" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="o in statusOptions" :key="String(o.value)" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head">
          <span>文章列表</span>
          <el-button type="primary" @click="handleCreate">+ 新建文章</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="标题" min-width="240">
          <template #default="{ row }">
            <span class="title-cell">
              <el-tag v-if="row.isTop" type="danger" size="small" effect="dark">置顶</el-tag>
              {{ row.title }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">{{ row.categoryName || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status).type as any" size="small">
              {{ statusTag(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="阅读量" width="90" />
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ row.publishTime?.replace('T', ' ').slice(0, 16) || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text size="small" @click="handleToggleTop(row)">
              {{ row.isTop ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.article-list {
  padding: 4px 0;
}
.search-card {
  margin-bottom: 16px;
}
.table-card {
  .card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: 600;
  }
}
.title-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
