<script setup lang="ts">
// 服务端渲染拉取文章列表，利于 SEO
const { data: articles } = await useAsyncData('blog-list', () =>
  useApi().get<{
    records: Array<{ id: number; title: string; summary: string; createTime: string }>
  }>('/v1/blog/articles')
)

useSeoMeta({
  title: '博客 - MengyTools',
  description: '技术文章与生活随笔'
})
</script>

<template>
  <div>
    <h1>博客</h1>
    <div v-if="articles?.records?.length" class="article-list">
      <NuxtLink
        v-for="a in articles.records"
        :key="a.id"
        :to="`/blog/${a.id}`"
        class="card article-item"
      >
        <h2>{{ a.title }}</h2>
        <p class="summary">{{ a.summary }}</p>
        <span class="time">{{ a.createTime }}</span>
      </NuxtLink>
    </div>
    <div v-else class="card empty">暂无文章</div>
  </div>
</template>

<style scoped>
.article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.article-item {
  display: block;
  color: inherit;
}
.article-item:hover {
  border-color: #409eff;
}
.article-item h2 {
  margin: 0 0 8px;
  font-size: 20px;
}
.summary {
  color: #666;
  margin: 0 0 8px;
}
.time {
  color: #999;
  font-size: 13px;
}
.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}
</style>
