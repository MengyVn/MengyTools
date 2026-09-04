<script setup lang="ts">
import { marked } from 'marked'
import { Eye, FileQuestion, ArrowLeft } from 'lucide-vue-next'

const route = useRoute()
const id = route.params.id

const { data: article } = await useAsyncData(`blog-${id}`, () =>
  useApi().get<{
    id: number
    title: string
    content: string
    contentFormat: string
    summary: string
    cover: string
    categoryName: string | null
    viewCount: number
    publishTime: string
    createTime: string
  }>(`/v1/blog/articles/${id}`)
)

const htmlContent = computed(() => {
  if (!article.value?.content) return ''
  // 兼容旧数据：未标注格式的默认当 markdown 处理
  const fmt = article.value.contentFormat || 'markdown'
  if (fmt === 'html') return article.value.content
  try {
    return marked.parse(article.value.content, { async: false }) as string
  } catch {
    return ''
  }
})

// 阅读量埋点：仅在浏览器端上报，后端按「文章+访客ID」24h 去重计数
const api = useApi()
const visitorId = useCookie<string | undefined>('visitor_id', {
  maxAge: 60 * 60 * 24 * 365,
  sameSite: 'lax'
})
// 生成访客 ID：crypto.randomUUID 在非 HTTPS（http://localhost）下不可用，需兼容回退
const genVisitorId = () => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  // 回退：手动拼 UUID v4
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}
onMounted(() => {
  if (!article.value) return
  if (!visitorId.value) {
    visitorId.value = genVisitorId()
  }
  api
    .post<boolean>('/v1/track/view', {
      articleId: article.value.id,
      visitorId: visitorId.value
    })
    .then((counted) => {
      // 本次有效计数时本地即时 +1，无需刷新
      if (counted && article.value) {
        article.value.viewCount = (article.value.viewCount || 0) + 1
      }
    })
    .catch(() => {})
})

useSeoMeta({
  title: () => article.value?.title ? `${article.value.title} - MengyTools` : '文章详情',
  description: () => article.value?.summary || ''
})
</script>

<template>
  <div class="container">
    <article v-if="article" class="card article">
      <div class="article-head animate-fade-up">
        <div class="article-meta">
          <span v-if="article.categoryName" class="tag">{{ article.categoryName }}</span>
          <span class="time">{{ (article.publishTime || article.createTime || '').slice(0, 10) }}</span>
          <span v-if="article.viewCount" class="views"><Eye :size="14" /> {{ article.viewCount }} 阅读</span>
        </div>
        <h1 class="article-title">{{ article.title }}</h1>
        <p v-if="article.summary" class="article-summary">{{ article.summary }}</p>
      </div>

      <!-- 封面图 -->
      <div v-if="article.cover" class="zoom-wrap article-cover-wrap animate-fade-up delay-1">
        <img
          :src="article.cover"
          :alt="article.title"
          class="article-cover"
        />
      </div>

      <div class="article-content markdown-body animate-fade-up delay-2" v-html="htmlContent" />
    </article>
    <div v-else class="empty-state card">
      <FileQuestion :size="40" class="empty-icon" />
      <p>文章不存在或已被删除</p>
      <NuxtLink to="/blog" class="back-link"><ArrowLeft :size="14" /> 返回博客列表</NuxtLink>
    </div>
  </div>
</template>

<style scoped>
.article {
  padding: 40px 48px;
}
.article-head {
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 24px;
  margin-bottom: 32px;
}
.article-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  font-size: 13px;
}
.tag {
  padding: 3px 10px;
  border-radius: 4px;
  background: rgba(99, 102, 241, 0.1);
  color: var(--primary);
  font-weight: 500;
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
.article-title {
  margin: 0 0 12px;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.3;
}
.article-summary {
  margin: 0;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.7;
}

/* 封面图：外层裁剪容器 + 内层图片 hover 缩放 */
.article-cover-wrap {
  width: 100%;
  max-height: 420px;
  margin-bottom: 24px;
  border-radius: var(--radius);
}
.article-cover {
  display: block;
  width: 100%;
  max-height: 420px;
  object-fit: cover;
}

/* Markdown 渲染样式 */
.article-content {
  line-height: 1.8;
  font-size: 15px;
}
.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3),
.article-content :deep(h4) {
  margin: 1.6em 0 0.8em;
  font-weight: 600;
  line-height: 1.4;
}
.article-content :deep(h1) { font-size: 26px; }
.article-content :deep(h2) {
  font-size: 22px;
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 6px;
}
.article-content :deep(h3) { font-size: 19px; }
.article-content :deep(p) { margin: 0.8em 0; }
.article-content :deep(a) { color: var(--primary); }
.article-content :deep(img) {
  max-width: 100%;
  border-radius: var(--radius);
  margin: 16px 0;
}
.article-content :deep(code) {
  background: var(--bg-hover);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
}
.article-content :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 16px 20px;
  border-radius: var(--radius);
  overflow-x: auto;
  margin: 16px 0;
}
.article-content :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}
.article-content :deep(blockquote) {
  border-left: 4px solid var(--primary-light);
  padding: 8px 16px;
  margin: 16px 0;
  color: var(--text-secondary);
  background: rgba(99, 102, 241, 0.04);
}
.article-content :deep(ul),
.article-content :deep(ol) {
  padding-left: 24px;
  margin: 0.8em 0;
}
.article-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}
.article-content :deep(th),
.article-content :deep(td) {
  border: 1px solid var(--border-color);
  padding: 8px 12px;
  text-align: left;
}
.article-content :deep(th) {
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
  .article { padding: 24px 20px; }
  .article-title { font-size: 22px; }
}
</style>
