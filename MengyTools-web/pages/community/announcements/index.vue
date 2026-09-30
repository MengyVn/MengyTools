<script setup lang="ts">
/**
 * 社区公告列表：站点公告的历史记录（分页）。
 * 与门户 /announcement 用的是同一套公开接口，只是换成社区版式与暗色主题令牌。
 */
import { COMMUNITY_SITE } from '~/community-site.config'
import { useAnnouncement, type AnnouncementItem } from '~/composables/useAnnouncement'
import { useCommunitySite } from '~/composables/useCommunitySite'
import { formatRelativeTime } from '~/composables/useCommunity'

definePageMeta({ layout: 'community' })

const route = useRoute()
const router = useRouter()
const { fetchPage } = useAnnouncement()
const { siteName } = useCommunitySite()

const page = computed(() => Math.max(Number(route.query.page) || 1, 1))
const SIZE = 10

const { data } = await useAsyncData(
  () => `community-announcements-${page.value}`,
  () => fetchPage(page.value, SIZE),
  { watch: [page] }
)

const records = computed<AnnouncementItem[]>(() => data.value?.records ?? [])
const total = computed(() => data.value?.total ?? 0)
const pages = computed(() => Math.ceil(total.value / SIZE))

const changePage = (p: number) => {
  router.push({ path: '/community/announcements', query: p > 1 ? { page: p } : {} })
}

const publishLabel = (it: AnnouncementItem) => {
  const t = it.publishTime || it.createTime
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}

useSeoMeta({
  title: () => `公告 - ${siteName.value}`,
  description: () => `${siteName.value} 的站点公告历史记录`
})

const { absolute, jsonLd } = useCommunitySeo()
useHead(() => ({
  link: [{ rel: 'canonical', href: absolute('/community/announcements') }],
  script: [
    jsonLd({
      '@context': 'https://schema.org',
      '@type': 'CollectionPage',
      url: absolute('/community/announcements'),
      inLanguage: COMMUNITY_SITE.locale,
      name: `公告 - ${siteName.value}`
    })
  ]
}))
</script>

<template>
  <div>
    <CommunityBreadcrumb :items="[{ label: '社区首页', to: '/community' }, { label: '公告' }]" />

    <section class="card">
      <div class="head">
        <h1 class="title">公告</h1>
        <span class="count">共 {{ total }} 条</span>
      </div>

      <ul v-if="records.length" class="list">
        <li v-for="it in records" :key="it.id" class="row">
          <NuxtLink :to="`/community/announcements/${it.id}`" class="row-title">
            <span v-if="it.isPersistent === 1" class="tag tag-persistent">常驻</span>
            <span v-else class="tag">公告</span>
            <span class="row-text">{{ it.title }}</span>
          </NuxtLink>
          <div class="row-meta">
            <time :title="publishLabel(it)">{{ formatRelativeTime(it.publishTime || it.createTime) }}</time>
            <span v-if="it.viewCount">· 阅读 {{ it.viewCount }}</span>
          </div>
        </li>
      </ul>
      <p v-else class="empty">还没有发布过公告。</p>

      <CommunityPager :current="data?.current ?? page" :pages="pages" :total="total" @change="changePage" />
    </section>
  </div>
</template>

<style scoped>
.card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  padding: 18px;
}
.head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--c-border);
}
.title {
  margin: 0;
  font-size: 19px;
  font-weight: 600;
}
.count {
  color: var(--c-text-faint);
  font-size: 13px;
}
.list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--c-border);
}
.row:last-child {
  border-bottom: 0;
}
.row-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: var(--c-text);
  font-size: 14.5px;
}
.row-title:hover .row-text {
  color: var(--c-accent);
}
.row-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tag {
  flex-shrink: 0;
  padding: 1px 6px;
  border: 1px solid var(--c-border);
  border-radius: 3px;
  color: var(--c-text-muted);
  font-size: 11.5px;
}
.tag-persistent {
  border-color: var(--c-accent);
  color: var(--c-accent);
}
.row-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  color: var(--c-text-faint);
  font-size: 12.5px;
}
.empty {
  padding: 32px 0;
  text-align: center;
  color: var(--c-text-faint);
  font-size: 13.5px;
}
@media (max-width: 640px) {
  .row {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>
