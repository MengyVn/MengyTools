<script setup lang="ts">
/**
 * 安全审计 - IP 封禁
 *
 * 两个来源分区展示：
 *   1. 封禁名单（sys_ip_ban）：管理员手工封禁，可永久或定时，可一键解封；
 *   2. 临时锁定（Redis）：连续输错密码 5 次自动锁 30 分钟，可一键解锁，也可直接升级为封禁。
 */
import { reactive, ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listIpBans,
  createIpBan,
  releaseIpBan,
  listIpLocks,
  unlockIp,
  securityOverview,
  BAN_DURATIONS,
  fmtTime,
  type IpBan,
  type IpLock,
  type SecurityOverview
} from '@/api/security'

const loading = ref(false)
const banList = ref<IpBan[]>([])
const banTotal = ref(0)
const lockList = ref<IpLock[]>([])
const lockLoading = ref(false)
const overview = ref<SecurityOverview | null>(null)

const query = reactive({
  page: 1,
  size: 20,
  state: undefined as 'active' | 'released' | undefined,
  keyword: ''
})

const form = reactive({
  ip: '',
  reason: '',
  minutes: 0
})

const isSelfIp = computed(
  () => !!overview.value && !!form.ip && form.ip === overview.value.currentIp
)

const fetchOverview = async () => {
  try {
    overview.value = await securityOverview()
  } catch {
    /* 忽略 */
  }
}

const fetchBans = async () => {
  loading.value = true
  try {
    const res = await listIpBans({
      page: query.page,
      size: query.size,
      state: query.state,
      keyword: query.keyword || undefined
    })
    banList.value = res.records
    banTotal.value = res.total
  } finally {
    loading.value = false
  }
}

const fetchLocks = async () => {
  lockLoading.value = true
  try {
    lockList.value = await listIpLocks()
  } finally {
    lockLoading.value = false
  }
}

const refreshAll = () => {
  fetchBans()
  fetchLocks()
  fetchOverview()
}

const handleSearch = () => {
  query.page = 1
  fetchBans()
}

const handleReset = () => {
  query.state = undefined
  query.keyword = ''
  handleSearch()
}

const submitBan = async () => {
  if (!form.ip) {
    ElMessage.warning('请填写要封禁的 IP')
    return
  }
  if (isSelfIp.value) {
    try {
      await ElMessageBox.confirm(
        `${form.ip} 是你当前使用的 IP，封禁后你自己也会被拦截（可在本页解封）。确认继续？`,
        '注意',
        { type: 'warning', confirmButtonText: '仍然封禁', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }
  await createIpBan({
    ip: form.ip,
    reason: form.reason,
    minutes: form.minutes > 0 ? form.minutes : null
  })
  ElMessage.success(`已封禁 ${form.ip}`)
  form.ip = ''
  form.reason = ''
  form.minutes = 0
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

const handleUnlock = async (row: any) => {
  await unlockIp(row.ip)
  ElMessage.success(`已解锁 ${row.ip}`)
  refreshAll()
}

const banFromLock = async (row: any) => {
  try {
    await ElMessageBox.confirm(`将 ${row.ip} 直接加入封禁名单（永久）？`, '封禁确认', {
      type: 'warning'
    })
  } catch {
    return
  }
  await createIpBan({ ip: row.ip, reason: `临时锁定升级：失败 ${row.failCount} 次`, minutes: null })
  ElMessage.success(`已封禁 ${row.ip}`)
  refreshAll()
}

/** 剩余锁定时间：秒 → 分秒 */
const remainText = (row: any) => {
  if (!row.locked || !row.remainSeconds) return '—'
  const m = Math.floor(row.remainSeconds / 60)
  const s = row.remainSeconds % 60
  return m > 0 ? `${m} 分 ${s} 秒` : `${s} 秒`
}

const banStateText = (row: any) => {
  if (row.status === 0) return '已解除'
  if (row.expireTime && new Date(String(row.expireTime).replace('T', ' ')) < new Date()) return '已到期'
  return '封禁中'
}

const banStateTag = (row: any) => {
  const t = banStateText(row)
  if (t === '封禁中') return 'danger'
  if (t === '已到期') return 'info'
  return 'success'
}

onMounted(refreshAll)
</script>

<template>
  <div class="security-ip-ban">
    <el-card shadow="never" class="form-card">
      <template #header>
        <div class="card-head">
          <span>手工封禁 IP</span>
          <span class="hint">
            封禁中的 IP 会被拦在站点之外（返回 403），并在登录日志里留痕
          </span>
        </div>
      </template>
      <el-form inline>
        <el-form-item label="IP">
          <el-input v-model="form.ip" placeholder="如 203.0.113.9" style="width: 180px" />
        </el-form-item>
        <el-form-item label="时长">
          <el-select v-model="form.minutes" style="width: 130px">
            <el-option v-for="d in BAN_DURATIONS" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="form.reason" placeholder="选填，便于日后追溯" style="width: 260px" />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" @click="submitBan">封禁</el-button>
          <el-button @click="refreshAll">刷新</el-button>
        </el-form-item>
      </el-form>
      <div class="stats">
        <el-tag type="warning" effect="plain">封禁中 {{ overview?.activeBans ?? 0 }} 个 IP</el-tag>
        <el-tag type="danger" effect="plain">今日登录失败 {{ overview?.todayFailures ?? 0 }} 次</el-tag>
        <el-tag type="info" effect="plain">我的 IP：{{ overview?.currentIp || '—' }}</el-tag>
      </div>
      <el-alert
        v-if="isSelfIp"
        type="warning"
        :closable="false"
        show-icon
        title="你正在封禁自己当前使用的 IP，请确认不是误操作"
      />
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head">
          <span>封禁名单（共 {{ banTotal }} 条）</span>
          <div class="head-filters">
            <el-input
              v-model="query.keyword"
              placeholder="IP / 归属地 / 原因"
              clearable
              style="width: 180px"
              @keyup.enter="handleSearch"
            />
            <el-select v-model="query.state" placeholder="全部状态" clearable style="width: 130px">
              <el-option label="封禁中" value="active" />
              <el-option label="已解除" value="released" />
            </el-select>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="banList" stripe>
        <el-table-column label="IP" width="170">
          <template #default="{ row }">
            <span>{{ row.ip }}</span>
            <el-tag v-if="overview && row.ip === overview.currentIp" size="small" type="info" class="ml4">
              当前
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="region" label="归属地" width="150" show-overflow-tooltip />
        <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
        <el-table-column prop="operatorName" label="操作人" width="110" show-overflow-tooltip />
        <el-table-column label="封禁时间" width="170">
          <template #default="{ row }">{{ fmtTime(row.banTime) }}</template>
        </el-table-column>
        <el-table-column label="到期时间" width="170">
          <template #default="{ row }">{{ row.expireTime ? fmtTime(row.expireTime) : '永久' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="banStateTag(row) as any">{{ banStateText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1 && banStateText(row) !== '已到期'"
              link
              type="primary"
              @click="handleRelease(row)"
            >
              解封
            </el-button>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="banTotal"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="fetchBans"
        @current-change="fetchBans"
      />
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head">
          <span>临时锁定中（连续输错密码 5 次自动锁 30 分钟）</span>
          <el-button link type="primary" @click="fetchLocks">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="lockLoading" :data="lockList" stripe>
        <el-table-column prop="ip" label="IP" width="180" />
        <el-table-column prop="region" label="归属地" width="170" show-overflow-tooltip />
        <el-table-column prop="failCount" label="失败次数" width="100" />
        <el-table-column label="锁定状态" width="140">
          <template #default="{ row }">
            <el-tag size="small" :type="row.locked ? 'danger' : 'info'">
              {{ row.locked ? '已锁定' : '仅计数' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="剩余锁定" width="140">
          <template #default="{ row }">{{ remainText(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleUnlock(row)">解锁</el-button>
            <el-button link type="danger" @click="banFromLock(row)">永久封禁</el-button>
          </template>
        </el-table-column>
      </el-table>
      <p v-if="!lockList.length" class="empty">当前没有被临时锁定的 IP。</p>
    </el-card>
  </div>
</template>

<style scoped>
.form-card,
.table-card {
  margin-bottom: 12px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.head-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 4px;
}
.ml4 {
  margin-left: 4px;
}
.muted {
  color: #c0c4cc;
}
.empty {
  margin: 12px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
