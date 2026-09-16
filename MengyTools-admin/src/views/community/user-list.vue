<script setup lang="ts">
/**
 * 社区 - 用户治理（P2 只读视图）。
 * 展示评论数、账号状态与禁言到期时间；禁言/封禁动作在 P3 上线（含操作留痕）。
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCommunityUsers, muteCommunityUser, banCommunityUser, type CommunityUserAdmin } from '@/api/community'

/**
 * el-table 插槽的 row 类型是 Element Plus 的 DefaultRow，它既不是 any 也无法窄化成业务实体
 * （全项目既有代码同样卡在这里，累积了 25 个同类类型错误）。
 * 这里把 row 显式放宽成 any，并在函数体内立刻取字段，避免把噪音带进新代码。
 */
type Row = any

const loading = ref(false)
const list = ref<CommunityUserAdmin[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '', onlyCommented: true })

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listCommunityUsers({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      onlyCommented: query.onlyCommented
    })
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
  query.keyword = ''
  query.onlyCommented = true
  handleSearch()
}

/**
 * 禁言状态：未到期的禁言标红，过期视为已解除。
 * 参数用宽松类型：el-table 插槽的 row 是 Element Plus 的 DefaultRow。
 */
const muteState = (row: Row) => {
  if (!row.muteUntil) return null
  const until = new Date(String(row.muteUntil).replace(' ', 'T')).getTime()
  if (Number.isNaN(until) || until <= Date.now()) return null
  return row.muteUntil
}

// ==================== 治理动作 ====================
/** 禁言：输入分钟数；输入 0 表示解除 */
const handleMute = async (row: Row) => {
  const res = await ElMessageBox.prompt(
    `对「${row.nickname || row.username}」禁言多少分钟？输入 0 表示解除禁言。`,
    '禁言',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: '60',
      inputValidator: (v: string) => (/^\d+$/.test(String(v).trim()) ? true : '请输入非负整数分钟数')
    }
  ).catch(() => null)
  if (!res) return
  const minutes = Number(res.value)
  await muteCommunityUser(row.id, minutes, minutes > 0 ? `禁言 ${minutes} 分钟` : '解除禁言')
  ElMessage.success(minutes > 0 ? '已禁言' : '已解除禁言')
  fetchList()
}

/** 封禁 / 解封 */
const handleBan = async (row: Row) => {
  const banned = row.status === 1
  await ElMessageBox.confirm(
    banned
      ? `确定封禁「${row.nickname || row.username}」？封禁后该账号无法登录与发言。`
      : `确定解封「${row.nickname || row.username}」？`,
    banned ? '封禁' : '解封',
    { type: 'warning' }
  )
  await banCommunityUser(row.id, banned, banned ? '违规封禁' : '解除封禁')
  ElMessage.success(banned ? '已封禁' : '已解封')
  fetchList()
}

onMounted(fetchList)
</script>

<template>
  <div class="community-user-list">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="登录名 / 昵称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="范围">
          <el-switch v-model="query.onlyCommented" active-text="仅看过评论的用户" @change="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head">
          <span>用户列表</span>
          <span class="hint">禁言与封禁都会写入操作日志</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="登录名" width="130" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" width="130" show-overflow-tooltip />
        <el-table-column prop="signature" label="签名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="commentCount" label="评论数" width="90" />
        <el-table-column label="账号" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="禁言" width="170">
          <template #default="{ row }">
            <el-tag v-if="muteState(row)" size="small" type="warning">至 {{ muteState(row) }}</el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="warning" @click="handleMute(row)">禁言</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="handleBan(row)">
              {{ row.status === 1 ? '封禁' : '解封' }}
            </el-button>
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
.table-card {
  margin-bottom: 12px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.hint {
  color: #909399;
  font-size: 12px;
}
.muted {
  color: #c0c4cc;
}
</style>
