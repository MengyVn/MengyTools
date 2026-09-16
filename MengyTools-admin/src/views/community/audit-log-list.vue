<script setup lang="ts">
/**
 * 社区 - 治理操作日志：审核/屏蔽/删除/禁言/封禁的留痕，用于追溯与申诉。
 * 日志只增不改，P3 上线治理动作后本页数据自动增长。
 */
import { ref, reactive, onMounted } from 'vue'
import { listCommunityAuditLogs, AUDIT_ACTIONS, type CommunityAuditLog } from '@/api/community'

const loading = ref(false)
const list = ref<CommunityAuditLog[]>([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  action: undefined as string | undefined,
  targetType: undefined as string | undefined,
  targetId: undefined as number | undefined
})

const targetTypes = [
  { label: '评论', value: 'comment' },
  { label: '文章', value: 'article' },
  { label: '用户', value: 'user' },
  { label: '举报', value: 'report' }
]

const actionText = (a: string) => AUDIT_ACTIONS.find((x) => x.value === a)?.label || a

const actionTag = (a: string) => {
  if (a === 'comment.audit') return 'primary'
  if (a === 'comment.delete') return 'danger'
  if (a === 'report.handle') return 'warning'
  if (a === 'user.mute') return 'warning'
  if (a === 'user.ban') return 'danger'
  return 'info'
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listCommunityAuditLogs({
      page: query.page,
      size: query.size,
      action: query.action,
      targetType: query.targetType,
      targetId: query.targetId
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
  query.action = undefined
  query.targetType = undefined
  query.targetId = undefined
  handleSearch()
}

onMounted(fetchList)
</script>

<template>
  <div class="community-audit-log-list">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="动作">
          <el-select v-model="query.action" placeholder="全部" clearable style="width: 150px">
            <el-option v-for="a in AUDIT_ACTIONS" :key="a.value" :label="a.label" :value="a.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="对象类型">
          <el-select v-model="query.targetType" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="t in targetTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="对象ID">
          <el-input v-model.number="query.targetId" placeholder="对象ID" clearable style="width: 110px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head"><span>操作日志</span></div>
      </template>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="operatorName" label="操作人" width="120" show-overflow-tooltip />
        <el-table-column label="动作" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="actionTag(row.action) as any">{{ actionText(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="对象" width="130">
          <template #default="{ row }">{{ row.targetType }} #{{ row.targetId }}</template>
        </el-table-column>
        <el-table-column label="变更" min-width="200">
          <template #default="{ row }">
            <span class="change">{{ row.beforeValue || '—' }}</span>
            <span class="arrow">→</span>
            <span class="change">{{ row.afterValue || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="ip" label="来源IP" width="130" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="160" />
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
.change {
  color: #606266;
}
.arrow {
  margin: 0 6px;
  color: #c0c4cc;
}
</style>
