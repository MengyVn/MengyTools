<script setup lang="ts">
import { ref, computed } from 'vue'
import { FileText, Eye, Megaphone, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { useAnnouncement, type AnnouncementItem } from '~/composables/useAnnouncement'

useSeoMeta({
  title: '公告 - MengyTools',
  description: '查看所有公告历史记录'
})

const announcement = useAnnouncement()

const page = ref(1)
const size = 10
const total = ref(0)
const loading = ref(false)
const records = ref<AnnouncementItem[]>([])
const errorMsg = ref<string | null>(null)

const totalPages = computed(() =>
  Math.max(1, Math.ceil(total.value / size))
)

const fetchPage = async (p: number) => {
  if (p < 1 || (total.value && p > totalPages.value)) return
  loading.value = true
  errorMsg.value = null
  try {
    const res = await announcement.fetchPage(p, size)
    records.value = res?.records ?? []
    total.value = res?.total ?? 0
    page.value = p
  } catch (e: any) {
    errorMsg.value = e?.message || '加载失败'
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// SSR 首屏拉取第一页
await useAsyncData('announcement-list-page1', async () => {
  await fetchPage(1)
  return { ok: true }
})

const goPrev = () => fetchPage(page.value - 1)
const goNext = () => fetchPage(page.value + 1)

const publishLabel = (it: AnnouncementItem) => {
  const t = it.publishTime || it.createTime
  if (!t) return ''
  return t.slice(0, 16).replace('T', ' ')
}
</script>

<template>
  <div class="container">
    <h1 class="page-title animate-fade-up">
      <Megaphone :size="26" /> 公告历史
    </h1>
    <p class="page-subtitle animate-fade-up delay-1">系统公告 · 共 {{ total }} 条</p>

    <div v-if="loading && !records.length" class="loading-state card">
      <span>加载中…</span>
    </div>

    <div v-else-if="errorMsg" class="empty-state card">
      <FileText :size="40" class="empty-icon" />
      <p>{{ errorMsg }}</p>
    </div>

    <div v-else-if="records.length" class="ann-list">
      <NuxtLink
        v-for="(it, i) in records"
        :key="it.id"
        v-reveal="{ delay: Math.min(i, 8) * 60 }"
        :to="`/announcement/${it.id}`"
        class="card ann-item"
      >
        <div class="ann-meta">
          <span v-if="it.isPersistent === 1" class="tag tag-persistent">常驻</span>
          <span v-else class="tag tag-active">生效中</span>
          <span class="time">{{ publishLabel(it) }}</span>
          <span v-if="it.viewCount" class="views">
            <Eye :size="13" /> {{ it.viewCount }}
          </span>
        </div>
        <h2 class="ann-title">{{ it.title }}</h2>
      </NuxtLink>
    </div>

    <div v-else class="empty-state card">
      <FileText :size="40" class="empty-icon" />
      <p>暂无公告</p>
    </div>

    <!-- 分页 -->
    <div v-if="totalPages > 1" class="pagination">
      <button
        class="page-btn"
        :disabled="page <= 1 || loading"
        @click="goPrev"
      >
        <ChevronLeft :size="16" /> 上一页
      </button>
      <span class="page-info">{{ page }} / {{ totalPages }}</span>
      <button
        class="page-btn"
        :disabled="page >= totalPages || loading"
        @click="goNext"
      >
        下一页 <ChevronRight :size="16" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.loading-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-tertiary);
  font-size: 14px;
}

.ann-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}
.ann-item {
  display: block;
  color: inherit;
  cursor: pointer;
}
.ann-item:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary-light);
}
.ann-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 12px;
}
.tag {
  padding: 2px 10px;
  border-radius: 4px;
  font-weight: 500;
}
.tag-persistent {
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: #fff;
}
.tag-active {
  background: rgba(99, 102, 241, 0.1);
  color: var(--primary);
}
.time {
  color: var(--text-tertiary);
}
.views {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--text-tertiary);
}
.ann-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}
.page-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--bg-color);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.page-btn:hover:not(:disabled) {
  color: var(--primary);
  border-color: var(--primary-light);
}
.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.page-info {
  font-size: 14px;
  color: var(--text-secondary);
}
</style>
