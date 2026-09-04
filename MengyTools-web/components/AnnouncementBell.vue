<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Bell, Loader2, ArrowRight, Megaphone } from 'lucide-vue-next'
import { useAnnouncement, type AnnouncementItem } from '~/composables/useAnnouncement'

const announcement = useAnnouncement()
const router = useRouter()

const open = ref(false)
const loading = ref(false)
const items = ref<AnnouncementItem[]>([])
const error = ref<string | null>(null)

/** 进度条 tick 触发器：每 30s 自增以驱动 recompute */
const nowTick = ref(Date.now())
let progressTimer: ReturnType<typeof setInterval> | null = null

/** 已读标记：存储最近一次打开下拉时见到的最大 publishTime 毫秒值 */
const LAST_READ_KEY = 'announcement-last-read-ms'
const hasUnread = ref(false)

/** 计算单条非常驻公告的进度百分比（0~100），防御性处理 null 时间 */
const progressOf = (item: AnnouncementItem): number => {
  if (item.isPersistent === 1) return 0
  const start = item.publishTime ? Date.parse(item.publishTime) : NaN
  const end = item.expireTime ? Date.parse(item.expireTime) : NaN
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) return 0
  const now = nowTick.value
  if (now <= start) return 0
  if (now >= end) return 100
  return Math.min(100, Math.max(0, ((now - start) / (end - start)) * 100))
}

/** 格式化"将于 YYYY-MM-DD HH:mm 消失" */
const expireLabel = (item: AnnouncementItem): string => {
  if (!item.expireTime) return ''
  const d = new Date(item.expireTime)
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `将于 ${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())} 消失`
}

const publishLabel = (item: AnnouncementItem): string => {
  const t = item.publishTime || item.createTime
  if (!t) return ''
  return t.slice(0, 16).replace('T', ' ')
}

/** 判断是否有未读：任一 active 公告 publishTime 晚于本地已读标记 */
const recomputeUnread = () => {
  if (!items.value.length) {
    hasUnread.value = false
    return
  }
  let maxMs = 0
  for (const it of items.value) {
    if (it.publishTime) {
      const ms = Date.parse(it.publishTime)
      if (Number.isFinite(ms) && ms > maxMs) maxMs = ms
    }
  }
  let lastRead = 0
  if (import.meta.client) {
    const saved = localStorage.getItem(LAST_READ_KEY)
    if (saved) {
      const n = Number(saved)
      if (Number.isFinite(n)) lastRead = n
    }
  }
  hasUnread.value = maxMs > lastRead
}

const loadActive = async () => {
  loading.value = true
  error.value = null
  try {
    const list = await announcement.fetchActive()
    items.value = Array.isArray(list) ? list : []
    recomputeUnread()
  } catch (e: any) {
    error.value = e?.message || '加载失败'
    items.value = []
  } finally {
    loading.value = false
  }
}

/** 切换下拉显隐；首次打开触发拉取并标记已读 */
const toggle = async () => {
  open.value = !open.value
  if (open.value && items.value.length === 0 && !loading.value && !error.value) {
    await loadActive()
  }
  if (open.value) {
    markAllRead()
  }
}

const markAllRead = () => {
  if (!items.value.length) return
  let maxMs = 0
  for (const it of items.value) {
    if (it.publishTime) {
      const ms = Date.parse(it.publishTime)
      if (Number.isFinite(ms) && ms > maxMs) maxMs = ms
    }
  }
  if (maxMs > 0 && import.meta.client) {
    localStorage.setItem(LAST_READ_KEY, String(maxMs))
  }
  hasUnread.value = false
}

/** 点击外部关闭：监听 document click，命中 bell 容器外则关闭 */
const rootEl = ref<HTMLElement | null>(null)
const onDocClick = (e: MouseEvent) => {
  if (!open.value || !rootEl.value) return
  const target = e.target as Node
  if (!rootEl.value.contains(target)) {
    open.value = false
  }
}
const onKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape' && open.value) open.value = false
}

const goDetail = (id: number) => {
  open.value = false
  router.push(`/announcement/${id}`)
}
const goList = () => {
  open.value = false
  router.push('/announcement')
}

onMounted(() => {
  // 首次拉取当前生效列表（客户端，规避 SSR）
  loadActive().catch(() => {})
  document.addEventListener('click', onDocClick, true)
  document.addEventListener('keydown', onKeydown)
  // 每 30s 更新一次当前时间，驱动进度条平滑刷新
  progressTimer = setInterval(() => {
    nowTick.value = Date.now()
  }, 30_000)
})

onBeforeUnmount(() => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
  document.removeEventListener('click', onDocClick, true)
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <div ref="rootEl" class="announcement-bell">
    <button
      class="bell-btn"
      :class="{ active: open }"
      type="button"
      title="公告"
      @click="toggle"
    >
      <Bell :size="18" />
      <span v-if="hasUnread" class="badge" />
    </button>

    <transition name="ann-pop">
      <div v-if="open" class="dropdown" role="dialog" aria-label="公告">
        <div class="dropdown-header">
          <span class="dropdown-title"><Megaphone :size="15" /> 公告</span>
          <span v-if="items.length" class="dropdown-count">{{ items.length }}</span>
        </div>

        <!-- 加载态 -->
        <div v-if="loading && !items.length" class="dropdown-state">
          <Loader2 :size="22" class="spin" />
          <span>加载中…</span>
        </div>

        <!-- 错误态 -->
        <div v-else-if="error && !items.length" class="dropdown-state">
          <span>{{ error }}</span>
        </div>

        <!-- 空状态 -->
        <div v-else-if="!items.length" class="dropdown-state">
          <span>暂无公告</span>
        </div>

        <!-- 公告列表 -->
        <div v-else class="dropdown-list">
          <button
            v-for="item in items"
            :key="item.id"
            type="button"
            class="ann-item"
            @click="goDetail(item.id)"
          >
            <div class="ann-item-head">
              <span v-if="item.isPersistent === 1" class="tag tag-persistent">常驻</span>
              <span class="ann-title">{{ item.title }}</span>
            </div>
            <div class="ann-meta">
              <span class="ann-time">{{ publishLabel(item) }}</span>
              <span v-if="item.viewCount" class="ann-views">阅读 {{ item.viewCount }}</span>
            </div>

            <!-- 进度条（仅非常驻公告） -->
            <template v-if="item.isPersistent !== 1">
              <div class="progress-track">
                <div
                  class="progress-fill"
                  :style="{ width: progressOf(item) + '%' }"
                />
              </div>
              <div v-if="expireLabel(item)" class="progress-hint">{{ expireLabel(item) }}</div>
            </template>
          </button>
        </div>

        <div class="dropdown-footer">
          <button type="button" class="more-link" @click="goList">
            查看更多 <ArrowRight :size="13" />
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.announcement-bell {
  position: relative;
  display: inline-flex;
}

.bell-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid var(--border-color);
  border-radius: 50%;
  background: var(--bg-color);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}
.bell-btn:hover {
  color: var(--primary);
  border-color: var(--primary-light);
  box-shadow: var(--shadow-sm);
}
.bell-btn.active {
  color: var(--primary);
  border-color: var(--primary-light);
  background: var(--bg-hover);
}
/* 未读红点 */
.badge {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent);
  box-shadow: 0 0 0 2px var(--bg-color);
}

/* 下拉窗口 */
.dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 380px;
  max-height: 480px;
  display: flex;
  flex-direction: column;
  background: var(--bg-color);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  z-index: 200;
}
.dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 10px;
  border-bottom: 1px solid var(--border-light);
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color);
}
.dropdown-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.dropdown-count {
  padding: 1px 8px;
  border-radius: 10px;
  background: var(--bg-hover);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 500;
}

.dropdown-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 8px;
}
.dropdown-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px 16px;
  color: var(--text-tertiary);
  font-size: 13px;
}
.spin {
  animation: ann-spin 1s linear infinite;
}
@keyframes ann-spin {
  to { transform: rotate(360deg); }
}

/* 单条公告 */
.ann-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 10px 12px;
  margin: 2px 0;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  cursor: pointer;
  color: inherit;
  transition: background 0.2s;
}
.ann-item:hover {
  background: var(--bg-hover);
}
.ann-item-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}
.tag-persistent {
  flex-shrink: 0;
  padding: 1px 8px;
  border-radius: 4px;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}
.ann-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color);
  line-height: 1.4;
  /* 单行省略 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ann-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--text-tertiary);
}
.ann-views {
  margin-left: auto;
}

/* 进度条 */
.progress-track {
  margin-top: 8px;
  height: 4px;
  border-radius: 2px;
  background: var(--bg-hover);
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  border-radius: 2px;
  background: linear-gradient(90deg, var(--primary-light), var(--primary));
  /* 平滑过渡：每次 tick 用 width 变化触发 transition */
  transition: width 1s var(--ease-out, cubic-bezier(0.22, 1, 0.36, 1));
}
.progress-hint {
  margin-top: 4px;
  font-size: 11px;
  color: var(--text-tertiary);
}

/* 底部 */
.dropdown-footer {
  display: flex;
  justify-content: flex-end;
  padding: 8px 12px;
  border-top: 1px solid var(--border-light);
}
.more-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: none;
  background: transparent;
  color: var(--primary);
  font-size: 13px;
  font-weight: 500;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.2s;
}
.more-link:hover {
  background: var(--bg-hover);
}

/* 弹出过渡 */
.ann-pop-enter-active {
  transition: opacity 0.18s var(--ease-out, cubic-bezier(0.22, 1, 0.36, 1)),
    transform 0.18s var(--ease-out, cubic-bezier(0.22, 1, 0.36, 1));
}
.ann-pop-leave-active {
  transition: opacity 0.12s ease-in, transform 0.12s ease-in;
}
.ann-pop-enter-from {
  opacity: 0;
  transform: translateY(-8px) scale(0.98);
}
.ann-pop-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.98);
}

@media (max-width: 480px) {
  .dropdown {
    width: min(360px, 90vw);
    right: -10px;
  }
}
</style>
