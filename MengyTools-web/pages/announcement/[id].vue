<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import { Eye, FileQuestion, ArrowLeft, Megaphone } from 'lucide-vue-next'
import { useAnnouncement, type AnnouncementDetail } from '~/composables/useAnnouncement'

const route = useRoute()
const id = route.params.id as string

const announcement = useAnnouncement()

const { data: detail } = await useAsyncData(`announcement-${id}`, () =>
  announcement.fetchDetail(id)
)

const item = computed<AnnouncementDetail | null>(() => detail.value ?? null)

const htmlContent = computed(() => {
  if (!item.value?.content) return ''
  // 兼容旧数据：未标注格式的默认按 markdown 处理
  const fmt = item.value.contentFormat || 'markdown'
  if (fmt === 'html') return item.value.content
  try {
    return marked.parse(item.value.content, { async: false }) as string
  } catch {
    return ''
  }
})

const publishLabel = computed(() => {
  const t = item.value?.publishTime || item.value?.createTime
  if (!t) return ''
  return t.slice(0, 16).replace('T', ' ')
})

// 阅读量埋点：仅在浏览器端上报，后端按「公告+访客ID」去重计数
const visitorId = useCookie<string | undefined>('visitor_id', {
  maxAge: 60 * 60 * 24 * 365,
  sameSite: 'lax'
})
// crypto.randomUUID 在非 HTTPS（http://localhost）下不可用，需兼容回退
const genVisitorId = () => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

onMounted(() => {
  if (!item.value) return
  if (!visitorId.value) {
    visitorId.value = genVisitorId()
  }
  announcement
    .trackView(item.value.id, visitorId.value as string)
    .then((counted) => {
      if (counted && item.value) {
        item.value.viewCount = (item.value.viewCount || 0) + 1
      }
    })
    .catch(() => {})
})

useSeoMeta({
  title: () => item.value?.title ? `${item.value.title} - MengyTools` : '公告详情',
  description: () => `MengyTools 公告：${item.value?.title || ''}`
})
</script>

<template>
  <div class="container">
    <article v-if="item" class="card ann">
      <div class="ann-head animate-fade-up">
        <div class="ann-meta">
          <span v-if="item.isPersistent === 1" class="tag tag-persistent">常驻</span>
          <span class="time">{{ publishLabel }}</span>
          <span v-if="item.viewCount" class="views">
            <Eye :size="14" /> {{ item.viewCount }} 阅读
          </span>
        </div>
        <h1 class="ann-title">
          <Megaphone :size="24" /> {{ item.title }}
        </h1>
      </div>

      <div
        class="ann-content markdown-body animate-fade-up delay-1"
        v-html="htmlContent"
      />
    </article>

    <div v-else class="empty-state card">
      <FileQuestion :size="40" class="empty-icon" />
      <p>公告不存在或已被删除</p>
      <NuxtLink to="/announcement" class="back-link">
        <ArrowLeft :size="14" /> 返回公告列表
      </NuxtLink>
    </div>
  </div>
</template>

<style scoped>
.ann {
  padding: 40px 48px;
}
.ann-head {
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 24px;
  margin-bottom: 32px;
}
.ann-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  font-size: 13px;
}
.tag-persistent {
  padding: 3px 10px;
  border-radius: 4px;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: #fff;
  font-weight: 600;
}
.time,
.views {
  color: var(--text-tertiary);
}
.views {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.ann-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.3;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

/* Markdown 渲染样式（与 blog/[id].vue 保持一致） */
.ann-content {
  line-height: 1.8;
  font-size: 15px;
}
.ann-content :deep(h1),
.ann-content :deep(h2),
.ann-content :deep(h3),
.ann-content :deep(h4) {
  margin: 1.6em 0 0.8em;
  font-weight: 600;
  line-height: 1.4;
}
.ann-content :deep(h1) { font-size: 26px; }
.ann-content :deep(h2) {
  font-size: 22px;
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 6px;
}
.ann-content :deep(h3) { font-size: 19px; }
.ann-content :deep(p) { margin: 0.8em 0; }
.ann-content :deep(a) { color: var(--primary); }
.ann-content :deep(img) {
  max-width: 100%;
  border-radius: var(--radius);
  margin: 16px 0;
}
.ann-content :deep(code) {
  background: var(--bg-hover);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}
.ann-content :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 16px 20px;
  border-radius: var(--radius);
  overflow-x: auto;
  margin: 16px 0;
}
.ann-content :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}
.ann-content :deep(blockquote) {
  border-left: 4px solid var(--primary-light);
  padding: 8px 16px;
  margin: 16px 0;
  color: var(--text-secondary);
  background: rgba(99, 102, 241, 0.04);
}
.ann-content :deep(ul),
.ann-content :deep(ol) {
  padding-left: 24px;
  margin: 0.8em 0;
}
.ann-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}
.ann-content :deep(th),
.ann-content :deep(td) {
  border: 1px solid var(--border-color);
  padding: 8px 12px;
  text-align: left;
}
.ann-content :deep(th) {
  background: var(--bg-hover);
  font-weight: 600;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 16px;
  font-size: 14px;
  font-weight: 500;
}

@media (max-width: 640px) {
  .ann { padding: 24px 20px; }
  .ann-title { font-size: 22px; }
}
</style>
