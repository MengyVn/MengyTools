<script setup lang="ts">
const route = useRoute()
const id = route.params.id

const { data: article } = await useAsyncData(`blog-${id}`, () =>
  useApi().get<{
    id: number
    title: string
    content: string
    summary: string
    createTime: string
  }>(`/v1/blog/articles/${id}`)
)

useSeoMeta({
  title: () => article.value?.title || '文章详情',
  description: () => article.value?.summary || ''
})
</script>

<template>
  <article class="card" v-if="article">
    <h1>{{ article.title }}</h1>
    <div class="meta">{{ article.createTime }}</div>
    <div class="content" v-html="article.content" />
  </article>
  <div v-else class="card empty">文章不存在或已被删除</div>
</template>

<style scoped>
.meta {
  color: #999;
  font-size: 14px;
  margin-bottom: 20px;
}
.content {
  line-height: 1.8;
}
.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}
</style>
