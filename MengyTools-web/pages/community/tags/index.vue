<script setup lang="ts">
/**
 * 标签总览：标签云式的标签列表（含文章数）。
 */
import { COMMUNITY_SITE } from '~/community-site.config'
import { useCommunity } from '~/composables/useCommunity'
import { useCommunitySite } from '~/composables/useCommunitySite'

definePageMeta({ layout: 'community' })

const { tags } = useCommunity()
const { siteName } = useCommunitySite()
const { data } = await useAsyncData('community-tags', () => tags())

useSeoMeta({
  title: () => `标签 - ${siteName.value}`,
  description: () => `按标签浏览 ${siteName.value} 的内容`
})

const { absolute, jsonLd } = useCommunitySeo()
useHead(() => ({
  link: [{ rel: 'canonical', href: absolute('/community/tags') }],
  script: [
    jsonLd({
      '@context': 'https://schema.org',
      '@type': 'CollectionPage',
      name: `标签 - ${siteName.value}`,
      url: absolute('/community/tags'),
      inLanguage: COMMUNITY_SITE.locale,
      hasPart: (data.value ?? []).map((t) => ({
        '@type': 'DefinedTerm',
        name: t.name,
        url: absolute(`/community/tags/${t.slug}`)
      }))
    })
  ]
}))
</script>

<template>
  <div>
    <CommunityBreadcrumb :items="[{ label: '社区首页', to: '/community' }, { label: '标签' }]" />

    <header class="head">
      <h1 class="title">标签</h1>
      <p class="desc">共 {{ data?.length ?? 0 }} 个标签，点击查看该标签下的文章。</p>
    </header>

    <ul v-if="data?.length" class="tag-grid">
      <li v-for="tag in data" :key="tag.id">
        <NuxtLink :to="`/community/tags/${tag.slug}`" class="tag-card">
          <span class="tag-name">{{ tag.name }}</span>
          <span class="tag-slug">{{ tag.slug }}</span>
          <span class="tag-count">{{ tag.articleCount }} 篇</span>
        </NuxtLink>
      </li>
    </ul>
    <p v-else class="empty">还没有标签。</p>
  </div>
</template>

<style scoped>
.head {
  padding-bottom: 14px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 16px;
}
.title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
.desc {
  margin: 6px 0 0;
  color: var(--c-text-muted);
  font-size: 13.5px;
}
.tag-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  list-style: none;
  margin: 0;
  padding: 0;
}
.tag-card {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 12px 14px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  color: var(--c-text);
}
.tag-card:hover {
  border-color: var(--c-accent);
}
.tag-name {
  font-size: 15px;
  font-weight: 500;
}
.tag-slug {
  color: var(--c-text-faint);
  font-size: 12px;
}
.tag-count {
  margin-left: auto;
  color: var(--c-text-muted);
  font-size: 12.5px;
}
.empty {
  padding: 40px 0;
  text-align: center;
  color: var(--c-text-faint);
  font-size: 14px;
}
</style>
