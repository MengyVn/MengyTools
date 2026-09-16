<script setup lang="ts">
/**
 * 标签下的文章列表（slug 构造 SEO 友好 URL）。
 */
import { COMMUNITY_SITE } from '~/community-site.config'
import { useCommunity } from '~/composables/useCommunity'
import { useCommunitySite } from '~/composables/useCommunitySite'

definePageMeta({ layout: 'community' })

const route = useRoute()
const router = useRouter()
const { tagArticles, tags } = useCommunity()
const { siteName } = useCommunitySite()

const slug = computed(() => String(route.params.slug))
const page = computed(() => Math.max(Number(route.query.page) || 1, 1))
const sort = computed<'latest' | 'hot'>(() => (route.query.sort === 'hot' ? 'hot' : 'latest'))

// 标签列表用于取中文名（同时被其他页面缓存复用）
const { data: allTags } = await useAsyncData('community-tags', () => tags())
const tagName = computed(() => allTags.value?.find((t) => t.slug === slug.value)?.name || slug.value)

const { data, error } = await useAsyncData(
  () => `community-tag-${slug.value}-${page.value}-${sort.value}`,
  () => tagArticles(slug.value, { page: page.value, size: 20, sort: sort.value }),
  { watch: [slug, page, sort] }
)

if (error.value) {
  throw createError({ statusCode: 404, statusMessage: '标签不存在', fatal: true })
}

const setSort = (s: 'latest' | 'hot') => {
  router.push({ path: route.path, query: s === 'hot' ? { sort: 'hot' } : {} })
}
const changePage = (p: number) => {
  router.push({ path: route.path, query: { ...route.query, page: p } })
}

useSeoMeta({
  title: () => `${tagName.value} - 标签 - ${siteName.value}`,
  description: () => `${siteName.value} 中「${tagName.value}」标签下的文章`
})

const { absolute, jsonLd } = useCommunitySeo()
useHead(() => ({
  link: [{ rel: 'canonical', href: absolute(`/community/tags/${slug.value}`) }],
  script: [
    jsonLd({
      '@context': 'https://schema.org',
      '@type': 'CollectionPage',
      name: `${tagName.value} - 标签`,
      url: absolute(`/community/tags/${slug.value}`),
      inLanguage: COMMUNITY_SITE.locale
    }),
    jsonLd({
      '@context': 'https://schema.org',
      '@type': 'BreadcrumbList',
      itemListElement: [
        { '@type': 'ListItem', position: 1, name: '社区首页', item: absolute('/community') },
        { '@type': 'ListItem', position: 2, name: '标签', item: absolute('/community/tags') },
        { '@type': 'ListItem', position: 3, name: tagName.value }
      ]
    })
  ]
}))
</script>

<template>
  <div>
    <CommunityBreadcrumb
      :items="[
        { label: '社区首页', to: '/community' },
        { label: '标签', to: '/community/tags' },
        { label: tagName }
      ]"
    />

    <div class="layout">
      <div class="main-col">
        <header class="head">
          <h1 class="title">{{ tagName }}</h1>
          <p class="desc">共 {{ data?.total ?? 0 }} 篇文章</p>
        </header>

        <div class="toolbar">
          <div class="tabs">
            <button type="button" class="tab" :class="{ active: sort === 'latest' }" @click="setSort('latest')">
              最新
            </button>
            <button type="button" class="tab" :class="{ active: sort === 'hot' }" @click="setSort('hot')">
              最热
            </button>
          </div>
        </div>

        <CommunityArticleList :items="data?.records ?? []" show-category />
        <CommunityPager
          :current="data?.current ?? 1"
          :pages="data?.pages ?? 0"
          :total="data?.total"
          @change="changePage"
        />
      </div>

      <CommunitySidebar class="side-col" />
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 20px;
  align-items: start;
}
.head {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 12px;
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
.toolbar {
  margin-bottom: 12px;
}
.tabs {
  display: flex;
  gap: 4px;
}
.tab {
  padding: 5px 12px;
  border: 1px solid transparent;
  border-radius: var(--c-radius);
  background: transparent;
  color: var(--c-text-muted);
  font-size: 14px;
  cursor: pointer;
}
.tab.active {
  border-color: var(--c-border);
  background: var(--c-surface);
  color: var(--c-accent);
  font-weight: 500;
}
@media (max-width: 900px) {
  .layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
