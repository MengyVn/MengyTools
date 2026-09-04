<script setup lang="ts">
import { FileText, Eye } from 'lucide-vue-next'
// 服务端渲染拉取已发布文章列表，利于 SEO
const { data: articles } = await useAsyncData('blog-list', () =>
  useApi().get<Array<{
    id: number
    title: string
    summary: string
    cover: string
    categoryName: string | null
    viewCount: number
    isTop: number
    publishTime: string
    createTime: string
  }>>('/v1/blog/articles')
)

useSeoMeta({
  title: '博客 - MengyTools',
  description: '技术文章与生活随笔，Markdown 驱动'
})

const list = computed(() => articles.value ?? [])
</script>

<template>
  <div class="container">
    <h1 class="page-title animate-fade-up">博客</h1>
    <p class="page-subtitle animate-fade-up delay-1">技术文章与生活随笔 · 共 {{ list.length }} 篇</p>

    <div v-if="list.length" class="article-list">
      <NuxtLink
        v-for="(a, i) in list"
        :key="a.id"
        v-reveal="{ delay: Math.min(i, 8) * 60 }"
        :to="`/blog/${a.id}`"
        class="card article-item"
      >
        <div v-if="a.cover" class="zoom-wrap article-thumb-wrap">
          <img
            :src="a.cover"
            :alt="a.title"
            class="article-thumb"
            loading="lazy"
          >
        </div>
        <div class="article-body">
          <div class="article-meta">
            <span v-if="a.isTop" class="tag tag-top">置顶</span>
            <span v-if="a.categoryName" class="tag">{{ a.categoryName }}</span>
            <span class="time">{{ (a.publishTime || a.createTime || '').slice(0, 10) }}</span>
            <span v-if="a.viewCount" class="views"><Eye :size="13" /> {{ a.viewCount }}</span>
          </div>
          <h2 class="article-title">{{ a.title }}</h2>
          <p class="article-summary">{{ a.summary || '暂无摘要' }}</p>
        </div>
      </NuxtLink>
    </div>
    <div v-else class="empty-state card">
      <FileText :size="40" class="empty-icon" />
      <p>还没有文章，敬请期待</p>
    </div>
  </div>
</template>

<style scoped>
.article-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}
.article-item {
  display: flex;
  gap: 16px;
  color: inherit;
}
/* 封面缩略图：外层裁剪容器 + 内层图片 hover 缩放 */
.article-thumb-wrap {
  width: 132px;
  height: 96px;
  flex-shrink: 0;
  align-self: center;
}
.article-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.article-item:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary-light);
}
.article-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 12px;
}
.tag {
  padding: 2px 10px;
  border-radius: 4px;
  background: rgba(99, 102, 241, 0.1);
  color: var(--primary);
  font-weight: 500;
}
.tag-top {
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: #fff;
}
.time {
  color: var(--text-tertiary);
}
.views {
  color: var(--text-tertiary);
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.article-title {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}
.article-summary {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
