<script setup lang="ts">
/**
 * 社区 - 评论管理（审核视图）。
 * P2：只读列表 + 待办统计（审核动作与留痕在 P3 连同写入链路一起上线）。
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listCommunityComments,
  getCommunityStats,
  auditComment,
  deleteCommunityComment,
  type CommunityCommentAdmin,
  type CommunityStats
} from '@/api/community'

/**
 * el-table 插槽的 row 类型是 Element Plus 的 DefaultRow，它既不是 any 也无法窄化成业务实体
 * （全项目既有代码同样卡在这里，累积了 25 个同类类型错误）。
 * 这里把 row 显式放宽成 any，并在函数体内立刻取字段，避免把噪音带进新代码。
 */
type Row = any

const loading = ref(false)
const list = ref<CommunityCommentAdmin[]>([])
const total = ref(0)
const stats = ref<CommunityStats | null>(null)

const query = reactive({
  page: 1,
  size: 10,
  status: undefined as number | undefined,
  articleId: undefined as number | undefined,
  keyword: ''
})

/** 状态下拉：不设「全部」选项（避免 value=undefined 的类型问题），清空即全部 */
const statusOptions = [
  { label: '待审', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已屏蔽', value: 2 }
]

const statusTag = (s: number) => {
  if (s === 0) return { type: 'warning', text: '待审' }
  if (s === 1) return { type: 'success', text: '已发布' }
  if (s === 2) return { type: 'danger', text: '已屏蔽' }
  return { type: 'info', text: '-' }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listCommunityComments({
      page: query.page,
      size: query.size,
      status: query.status,
      articleId: query.articleId,
      keyword: query.keyword || undefined
    })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    stats.value = await getCommunityStats()
  } catch {
    stats.value = null
  }
}

const handleSearch = () => {
  query.page = 1
  fetchList()
  fetchStats()
}

const handleReset = () => {
  query.status = undefined
  query.articleId = undefined
  query.keyword = ''
  handleSearch()
}

// ==================== 治理动作 ====================
const acting = ref(false)

/** 审核：通过 / 屏蔽（通过无需理由，屏蔽记录理由） */
const handleAudit = async (row: Row, status: number) => {
  let note = '审核通过'
  if (status !== 1) {
    const res = await ElMessageBox.prompt('请填写屏蔽理由（会记入操作日志）', '屏蔽评论', {
      confirmButtonText: '确定屏蔽',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：广告内容 / 与主题无关',
      inputValidator: (v: string) => (v && v.trim() ? true : '请填写理由')
    }).catch(() => null)
    if (!res) return
    note = res.value
  }
  acting.value = true
  try {
    await auditComment(row.id, status, note)
    ElMessage.success(status === 1 ? '已通过' : '已屏蔽')
    fetchList()
    fetchStats()
  } finally {
    acting.value = false
  }
}

const handleDelete = async (row: Row) => {
  const res = await ElMessageBox.prompt('删除后不可恢复（顶层评论会连带删除其回复），请填写理由', '删除评论', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：违规内容',
    inputValidator: (v: string) => (v && v.trim() ? true : '请填写理由')
  }).catch(() => null)
  if (!res) return
  acting.value = true
  try {
    await deleteCommunityComment(row.id, res.value)
    ElMessage.success('已删除')
    fetchList()
    fetchStats()
  } finally {
    acting.value = false
  }
}

onMounted(() => {
  fetchList()
  fetchStats()
})
</script>

<template>
  <div class="community-comment-list">
    <el-card shadow="never" class="stats-card">
      <div class="stats">
        <div class="stat">
          <span class="stat-value warning">{{ stats?.commentPending ?? '-' }}</span>
          <span class="stat-label">待审评论</span>
        </div>
        <div class="stat">
          <span class="stat-value">{{ stats?.commentPublished ?? '-' }}</span>
          <span class="stat-label">已发布</span>
        </div>
        <div class="stat">
          <span class="stat-value">{{ stats?.commentBlocked ?? '-' }}</span>
          <span class="stat-label">已屏蔽</span>
        </div>
        <div class="stat">
          <span class="stat-value danger">{{ stats?.reportPending ?? '-' }}</span>
          <span class="stat-label">待处理举报</span>
        </div>
        <div class="stats-note">所有治理动作都会写入操作日志，可在「操作日志」页追溯。</div>
      </div>
    </el-card>

    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="o in statusOptions"
              :key="String(o.value)"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="文章ID">
          <el-input v-model.number="query.articleId" placeholder="文章ID" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="评论内容 / 昵称 / 文章标题"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head"><span>评论列表</span></div>
      </template>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="所属文章" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.articleTitle || `文章 #${row.articleId}` }}
          </template>
        </el-table-column>
        <el-table-column label="楼层" width="80">
          <template #default="{ row }">
            <span v-if="row.floor">{{ row.floor }}</span>
            <el-tag v-else size="small" type="info">回复</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="authorName" label="作者" width="110" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status).type as any">
              {{ statusTag(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="likeCount" label="赞" width="60" />
        <el-table-column prop="ip" label="来源IP" width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 1" link type="success" :disabled="acting" @click="handleAudit(row, 1)">
              通过
            </el-button>
            <el-button v-if="row.status !== 2" link type="warning" :disabled="acting" @click="handleAudit(row, 2)">
              屏蔽
            </el-button>
            <el-button link type="danger" :disabled="acting" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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
    </el-card>
  </div>
</template>

<style scoped>
.search-card,
.table-card,
.stats-card {
  margin-bottom: 12px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stats {
  display: flex;
  align-items: center;
  gap: 32px;
  flex-wrap: wrap;
}
.stat {
  display: flex;
  flex-direction: column;
}
.stat-value {
  font-size: 20px;
  font-weight: 600;
  line-height: 1.3;
}
.stat-value.warning {
  color: #e6a23c;
}
.stat-value.danger {
  color: #f56c6c;
}
.stat-label {
  color: #909399;
  font-size: 12px;
}
.stats-note {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}
</style>
