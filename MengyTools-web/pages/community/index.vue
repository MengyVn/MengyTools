<script setup lang="ts">
/**
 * 社区首页：文章流（最新 / 最热）+ 侧栏。
 * 经典内容社区形态，克制的排版，无动效。
 */
import { COMMUNITY_SITE } from '~/community-site.config'
import { useCommunity } from '~/composables/useCommunity'
import { useCommunitySite } from '~/composables/useCommunitySite'

definePageMeta({ layout: 'community' })

const route = useRoute()
const router = useRouter()
const { listArticles } = useCommunity()
const { siteName, siteTagline } = useCommunitySite()

const page = computed(() => Math.max(Number(route.query.page) || 1, 1))
const sort = computed<'latest' | 'hot'>(() => (route.query.sort === 'hot' ? 'hot' : 'latest'))

const { data } = await useAsyncData(
  () => `community-index-${page.value}-${sort.value}`,
  () => listArticles({ page: page.value, size: 20, sort: sort.value }),
  { watch: [page, sort] }
)

const setSort = (s: 'latest' | 'hot') => {
  router.push({ path: '/community', query: s === 'hot' ? { sort: 'hot' } : {} })
}
const changePage = (p: number) => {
  router.push({ path: '/community', query: { ...route.query, page: p } })
}

useSeoMeta({
  title: () => `${siteName.value} - ${siteTagline.value}`,
  description: () => `${siteTagline.value}。技术文章、经验分享与讨论。`,
  ogTitle: () => siteName.value,
  ogDescription: () => siteTagline.value,
  ogType: 'website'
})

// SEO：canonical 收敛分页/排序参数；结构化数据声明站点与站内搜索
const { absolute, jsonLd } = useCommunitySeo()
useHead(() => ({
  link: [{ rel: 'canonical', href: absolute('/community') }],
  script: [
    jsonLd({
      '@context': 'https://schema.org',
      '@type': 'WebSite',
      name: siteName.value,
      description: COMMUNITY_SITE.description,
      url: absolute('/community'),
      inLanguage: COMMUNITY_SITE.locale,
      potentialAction: {
        '@type': 'SearchAction',
        target: `${absolute('/community/search')}?q={search_term_string}`,
        'query-input': 'required name=search_term_string'
      }
    })
  ]
}))
</script>

<template>
  <div class="home">
    <section class="intro">
      <h1 class="intro-title">{{ siteName }}</h1>
      <p class="intro-desc">{{ siteTagline }}</p>
    </section>

    <div class="layout">
      <div class="main-col">
        <div class="toolbar">
          <div class="tabs">
            <button
              type="button"
              class="tab"
              :class="{ active: sort === 'latest' }"
              @click="setSort('latest')"
            >
              最新
            </button>
            <button
              type="button"
              class="tab"
              :class="{ active: sort === 'hot' }"
              @click="setSort('hot')"
            >
              最热
            </button>
          </div>
          <span class="toolbar-count">共 {{ data?.total ?? 0 }} 篇</span>
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
.intro {
  padding: 4px 0 18px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 18px;
}
.intro-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.2px;
}
.intro-desc {
  margin: 6px 0 0;
  color: var(--c-text-muted);
  font-size: 14px;
}
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 20px;
  align-items: start;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
.tab:hover {
  color: var(--c-text);
}
.tab.active {
  border-color: var(--c-border);
  background: var(--c-surface);
  color: var(--c-accent);
  font-weight: 500;
}
.toolbar-count {
  color: var(--c-text-faint);
  font-size: 12.5px;
}
@media (max-width: 900px) {
  .layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
