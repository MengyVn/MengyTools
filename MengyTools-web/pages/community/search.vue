<script setup lang="ts">
/**
 * 搜索：标题/摘要关键词匹配（P1 后端为 LIKE，P4 计划换全文检索）。
 */
import { Search } from 'lucide-vue-next'
import { COMMUNITY_SITE } from '~/community-site.config'
import { useCommunity } from '~/composables/useCommunity'
import { useCommunitySite } from '~/composables/useCommunitySite'

definePageMeta({ layout: 'community' })

const route = useRoute()
const router = useRouter()
const { search } = useCommunity()
const { siteName } = useCommunitySite()

const keyword = ref(String(route.query.q || ''))
const q = computed(() => String(route.query.q || '').trim())
const page = computed(() => Math.max(Number(route.query.page) || 1, 1))

const { data } = await useAsyncData(
  () => `community-search-${q.value}-${page.value}`,
  () => (q.value ? search(q.value, { page: page.value, size: 20 }) : Promise.resolve(null)),
  { watch: [q, page] }
)

const submit = () => {
  const kw = keyword.value.trim()
  router.push({ path: '/community/search', query: kw ? { q: kw } : {} })
}
const changePage = (p: number) => {
  router.push({ path: '/community/search', query: { ...route.query, page: p } })
}

watch(
  () => route.query.q,
  (v) => {
    keyword.value = String(v || '')
  }
)

useSeoMeta({
  title: () => (q.value ? `搜索「${q.value}」 - ${siteName.value}` : `搜索 - ${siteName.value}`),
  description: () => `在 ${siteName.value} 中搜索文章`,
  robots: 'noindex'
})

const { absolute } = useCommunitySeo()
useHead(() => ({
  link: [{ rel: 'canonical', href: absolute('/community/search') }]
}))
</script>

<template>
  <div class="search-page">
    <CommunityBreadcrumb :items="[{ label: '社区首页', to: '/community' }, { label: '搜索' }]" />

    <header class="head">
      <h1 class="title">搜索</h1>
      <form class="form" @submit.prevent="submit">
        <Search :size="16" class="form-icon" />
        <input v-model="keyword" type="search" class="form-input" placeholder="输入关键词，回车搜索" />
        <button type="submit" class="form-btn">搜索</button>
      </form>
    </header>

    <div class="layout">
      <div class="main-col">
        <p v-if="q" class="result-hint">
          「{{ q }}」的搜索结果：{{ data?.total ?? 0 }} 篇
        </p>
        <template v-if="q">
          <CommunityArticleList :items="data?.records ?? []" show-category />
          <CommunityPager
            :current="data?.current ?? 1"
            :pages="data?.pages ?? 0"
            :total="data?.total"
            @change="changePage"
          />
        </template>
        <p v-else class="empty">输入关键词开始搜索。</p>
      </div>

      <CommunitySidebar class="side-col" />
    </div>
  </div>
</template>

<style scoped>
.head {
  padding-bottom: 14px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 16px;
}
.title {
  margin: 0 0 12px;
  font-size: 20px;
  font-weight: 600;
}
.form {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 520px;
}
.form-icon {
  position: absolute;
  left: 10px;
  color: var(--c-text-faint);
  pointer-events: none;
}
.form-input {
  flex: 1;
  height: 36px;
  padding: 0 12px 0 32px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text);
  font-size: 14px;
  outline: none;
}
.form-input:focus {
  border-color: var(--c-accent);
}
.form-btn {
  height: 36px;
  padding: 0 18px;
  border: 1px solid var(--c-accent);
  border-radius: var(--c-radius);
  background: var(--c-accent);
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 20px;
  align-items: start;
}
.result-hint {
  margin: 0 0 12px;
  color: var(--c-text-muted);
  font-size: 13.5px;
}
.empty {
  padding: 40px 0;
  text-align: center;
  color: var(--c-text-faint);
  font-size: 14px;
}
@media (max-width: 900px) {
  .layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
