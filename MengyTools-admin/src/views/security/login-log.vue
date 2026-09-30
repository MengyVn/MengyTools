<script setup lang="ts">
/**
 * 安全审计 - 登录日志
 *
 * 记录每一次登录尝试（成功/失败），含 IP、归属地、浏览器、时间与结果说明。
 * 「近期」= 最近 7 天，「全部」= 全量；结果里可直接一键封禁/解封该 IP。
 */
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listLoginLogs,
  listIpBans,
  clearLoginLogs,
  createIpBan,
  releaseIpBan,
  securityOverview,
  BAN_DURATIONS,
  fmtTime,
  type LoginLog,
  type SecurityOverview
} from '@/api/security'

const loading = ref(false)
const list = ref<LoginLog[]>([])
const total = ref(0)
const overview = ref<SecurityOverview | null>(null)
/** 已封禁 IP 集合：用于把按钮显示成「封禁」还是「解封」 */
const bannedIps = ref<Set<string>>(new Set())

const query = reactive({
  page: 1,
  size: 20,
  range: 'recent' as 'recent' | 'all',
  username: '',
  ip: '',
  status: undefined as number | undefined
})

const banDialog = reactive({
  visible: false,
  ip: '',
  reason: '',
  minutes: 0
})

const isSelfIp = computed(() => !!overview.value && banDialog.ip === overview.value.currentIp)

const fetchOverview = async () => {
  try {
    overview.value = await securityOverview()
  } catch {
    /* 概览失败不影响主列表 */
  }
}

const fetchBannedIps = async () => {
  try {
    const res = await listIpBans({ page: 1, size: 100, state: 'active' })
    bannedIps.value = new Set(res.records.map((x) => x.ip))
  } catch {
    /* 忽略 */
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listLoginLogs({
      page: query.page,
      size: query.size,
      range: query.range,
      username: query.username || undefined,
      ip: query.ip || undefined,
      status: query.status
    })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const refreshAll = () => {
  fetchList()
  fetchOverview()
  fetchBannedIps()
}

const handleSearch = () => {
  query.page = 1
  refreshAll()
}

const handleReset = () => {
  query.username = ''
  query.ip = ''
  query.status = undefined
  handleSearch()
}

const switchRange = (range: 'recent' | 'all') => {
  query.range = range
  handleSearch()
}

const openBan = (row: any) => {
  banDialog.ip = row.ip
  banDialog.reason = row.status === 0 ? `登录失败：${row.message}` : '管理员手工封禁'
  banDialog.minutes = 0
  banDialog.visible = true
}

const submitBan = async () => {
  if (!banDialog.ip) {
    ElMessage.warning('请填写要封禁的 IP')
    return
  }
  if (isSelfIp.value) {
    try {
      await ElMessageBox.confirm(
        `${banDialog.ip} 正是你当前使用的 IP，封禁后你将无法访问站点（可在本页解封）。确认继续？`,
        '注意',
        { type: 'warning', confirmButtonText: '仍然封禁', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }
  await createIpBan({
    ip: banDialog.ip,
    reason: banDialog.reason,
    minutes: banDialog.minutes > 0 ? banDialog.minutes : null
  })
  ElMessage.success(`已封禁 ${banDialog.ip}`)
  banDialog.visible = false
  refreshAll()
}

const handleRelease = async (row: any) => {
  try {
    await ElMessageBox.confirm(`确认解封 ${row.ip}？`, '解封确认', { type: 'warning' })
  } catch {
    return
  }
  await releaseIpBan(row.ip)
  ElMessage.success(`已解封 ${row.ip}`)
  refreshAll()
}

const handleClear = async () => {
  try {
    await ElMessageBox.confirm('将删除 30 天前的登录日志，是否继续？', '清理日志', {
      type: 'warning'
    })
  } catch {
    return
  }
  const removed = await clearLoginLogs(30)
  ElMessage.success(`已清理 ${removed} 条 30 天前的日志`)
  refreshAll()
}

onMounted(refreshAll)
</script>

<template>
  <div class="security-login-log">
    <el-card shadow="never" class="search-card">
      <div class="overview">
        <el-radio-group :model-value="query.range" @change="(v: any) => switchRange(v)">
          <el-radio-button value="recent">近期（近 7 天）</el-radio-button>
          <el-radio-button value="all">全部记录</el-radio-button>
        </el-radio-group>
        <div class="stats">
          <el-tag type="danger" effect="plain">今日失败 {{ overview?.todayFailures ?? 0 }} 次</el-tag>
          <el-tag type="warning" effect="plain">封禁中 {{ overview?.activeBans ?? 0 }} 个 IP</el-tag>
          <el-tag type="info" effect="plain">我的 IP：{{ overview?.currentIp || '—' }}</el-tag>
        </div>
      </div>

      <el-form inline class="filters">
        <el-form-item label="登录名">
          <el-input v-model="query.username" placeholder="模糊匹配" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="IP">
          <el-input v-model="query.ip" placeholder="如 114.114" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="danger" plain @click="handleClear">清理 30 天前</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head">
          <span>登录记录（共 {{ total }} 条）</span>
          <el-button link type="primary" @click="refreshAll">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.loginTime) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="登录名" width="130" show-overflow-tooltip />
        <el-table-column label="IP" width="170">
          <template #default="{ row }">
            <span>{{ row.ip }}</span>
            <el-tag v-if="overview && row.ip === overview.currentIp" size="small" type="info" class="ml4">
              当前
            </el-tag>
            <el-tag v-if="bannedIps.has(row.ip)" size="small" type="danger" class="ml4">已封禁</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="region" label="归属地" width="150" show-overflow-tooltip />
        <el-table-column label="客户端" min-width="180">
          <template #default="{ row }">
            <span>{{ row.browser || '—' }}</span>
            <span class="sep">/</span>
            <span>{{ row.os || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="说明" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="bannedIps.has(row.ip)"
              link
              type="primary"
              @click="handleRelease(row)"
            >
              解封
            </el-button>
            <el-button v-else link type="danger" @click="openBan(row)">封禁</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="fetchList"
        @current-change="fetchList"
      />
    </el-card>

    <el-dialog v-model="banDialog.visible" title="封禁 IP" width="460px">
      <el-form label-width="80px">
        <el-form-item label="IP">
          <el-input v-model="banDialog.ip" />
        </el-form-item>
        <el-form-item label="时长">
          <el-select v-model="banDialog.minutes" style="width: 100%">
            <el-option v-for="d in BAN_DURATIONS" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="banDialog.reason" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <el-alert
        v-if="isSelfIp"
        type="warning"
        :closable="false"
        show-icon
        title="这是你当前使用的 IP，封禁后你自己也会被拦截（可在本页解封）"
      />
      <template #footer>
        <el-button @click="banDialog.visible = false">取消</el-button>
        <el-button type="danger" @click="submitBan">确认封禁</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.search-card,
.table-card {
  margin-bottom: 12px;
}
.overview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.filters {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 12px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.ml4 {
  margin-left: 4px;
}
.sep {
  margin: 0 4px;
  color: #c0c4cc;
}
</style>
