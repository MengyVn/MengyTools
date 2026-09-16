<script setup lang="ts">
/**
 * 社区 - 举报处理（P2 只读视图；处理动作在 P3 与操作留痕一起上线）。
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listCommunityReports, handleCommunityReport, type CommunityReport } from '@/api/community'

/**
 * el-table 插槽的 row 类型是 Element Plus 的 DefaultRow，它既不是 any 也无法窄化成业务实体
 * （全项目既有代码同样卡在这里，累积了 25 个同类类型错误）。
 * 这里把 row 显式放宽成 any，并在函数体内立刻取字段，避免把噪音带进新代码。
 */
type Row = any

const loading = ref(false)
const list = ref<CommunityReport[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, status: undefined as number | undefined })

/** 状态下拉：不设「全部」选项（避免 value=undefined 的类型问题），清空即全部 */
const statusOptions = [
  { label: '待处理', value: 0 },
  { label: '举报成立', value: 1 },
  { label: '已驳回', value: 2 }
]

const statusTag = (s: number) => {
  if (s === 0) return { type: 'warning', text: '待处理' }
  if (s === 1) return { type: 'danger', text: '举报成立' }
  if (s === 2) return { type: 'info', text: '已驳回' }
  return { type: 'info', text: '-' }
}

const typeText = (t: string) =>
  t === 'comment' ? '评论' : t === 'article' ? '文章' : t === 'user' ? '用户' : t

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listCommunityReports({ page: query.page, size: query.size, status: query.status })
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
  query.status = undefined
  handleSearch()
}

// ==================== 处理弹窗 ====================
const dialogVisible = ref(false)
const submitting = ref(false)
const current = ref<CommunityReport | null>(null)
const form = reactive({ status: 1, note: '', blockComment: true })

const openHandle = (row: Row) => {
  current.value = row
  form.status = 1
  form.note = ''
  form.blockComment = row.targetType === 'comment'
  dialogVisible.value = true
}

const submitHandle = async () => {
  if (!current.value) return
  submitting.value = true
  try {
    await handleCommunityReport(current.value.id, form.status, form.note, form.status === 1 && form.blockComment)
    ElMessage.success(form.status === 1 ? '已判定成立' : '已驳回')
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="community-report-list">
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
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head">
          <span>举报列表</span>
          <span class="hint">处理结果会写入操作日志</span>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="对象" width="90">
          <template #default="{ row }">
            {{ typeText(row.targetType) }} #{{ row.targetId }}
          </template>
        </el-table-column>
        <el-table-column prop="targetExcerpt" label="被举报内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="reporterName" label="举报人" width="110" show-overflow-tooltip />
        <el-table-column prop="reason" label="原因" width="120" show-overflow-tooltip />
        <el-table-column prop="detail" label="补充说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status).type as any">
              {{ statusTag(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handlerName" label="处理人" width="110" show-overflow-tooltip />
        <el-table-column prop="handleNote" label="处理说明" min-width="160" show-overflow-tooltip />
        <el-table-column prop="handleTime" label="处理时间" width="160" />
        <el-table-column prop="createTime" label="举报时间" width="160" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openHandle(row)">
              {{ row.status === 0 ? '处理' : '复核' }}
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

    <el-dialog v-model="dialogVisible" title="处理举报" width="460px">
      <el-form label-width="90px">
        <el-form-item label="被举报内容">
          <span class="target">{{ current?.targetExcerpt || `#${current?.targetId}` }}</span>
        </el-form-item>
        <el-form-item label="处理结果">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">举报成立</el-radio>
            <el-radio :value="2">驳回举报</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.status === 1 && current?.targetType === 'comment'" label="连带处理">
          <el-checkbox v-model="form.blockComment">同时屏蔽被举报评论</el-checkbox>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input v-model="form.note" type="textarea" :rows="3" maxlength="255" placeholder="会记入操作日志" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitHandle">提交</el-button>
      </template>
    </el-dialog>
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
.target {
  color: #606266;
}
</style>
