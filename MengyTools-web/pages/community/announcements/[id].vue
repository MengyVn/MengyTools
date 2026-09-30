<script setup lang="ts">
/**
 * 社区公告详情。
 *
 * 内容由管理员发布（可信来源，与门户公告同一张表），渲染沿用门户已验证的 marked 方案：
 * contentFormat=html 直接输出，其余按 markdown 解析。
 * 阅读量埋点与文章一致：cookie 存访客ID，后端按「公告+访客ID」去重。
 */
import { marked } from 'marked'
import { COMMUNITY_SITE } from '~/community-site.config'
import { useAnnouncement, type AnnouncementDetail } from '~/composables/useAnnouncement'
import { useCommunitySite } from '~/composables/useCommunitySite'

definePageMeta({ layout: 'community' })

/** 与门户公告铃铛共用的已读标记 key：在社区读过，门户铃铛就不再提示未读 */
const LAST_READ_KEY = 'announcement-last-read-ms'

const route = useRoute()
const id = String(route.params.id)
const { fetchDetail, trackView } = useAnnouncement()
const { siteName } = useCommunitySite()

const { data: detail, error } = await useAsyncData(`community-announcement-${id}`, () =>
  fetchDetail(id)
)

if (error.value || !detail.value) {
  throw createError({ statusCode: 404, statusMessage: '公告不存在', fatal: true })
}

const item = computed<AnnouncementDetail | null>(() => detail.value ?? null)

const htmlContent = computed(() => {
  const it = item.value
  if (!it?.content) return ''
  const fmt = it.contentFormat || 'markdown'
  if (fmt === 'html') return it.content
  try {
    return marked.parse(it.content, { async: false }) as string
  } catch {
    return ''
  }
})

const publishLabel = computed(() => {
  const t = item.value?.publishTime || item.value?.createTime
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
})

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
  const it = item.value
  if (!it) return
  // 已读标记
  const ms = Date.parse(String(it.publishTime || it.createTime || ''))
  if (Number.isFinite(ms)) {
    const prev = Number(localStorage.getItem(LAST_READ_KEY) || 0)
    if (!Number.isFinite(prev) || ms > prev) {
      localStorage.setItem(LAST_READ_KEY, String(ms))
    }
  }
  // 阅读量
  if (!visitorId.value) {
    visitorId.value = genVisitorId()
  }
  trackView(it.id, visitorId.value as string)
    .then((counted) => {
      if (counted && item.value) {
        item.value.viewCount = (item.value.viewCount || 0) + 1
      }
    })
    .catch(() => {})
})

useSeoMeta({
  title: () => (item.value?.title ? `${item.value.title} - ${siteName.value}` : `公告 - ${siteName.value}`),
  description: () => `${siteName.value} 公告：${item.value?.title || ''}`
})

const { absolute, jsonLd } = useCommunitySeo()
useHead(() => ({
  link: [{ rel: 'canonical', href: absolute(`/community/announcements/${id}`) }],
  script: [
    jsonLd({
      '@context': 'https://schema.org',
      '@type': 'Article',
      headline: item.value?.title,
      url: absolute(`/community/announcements/${id}`),
      inLanguage: COMMUNITY_SITE.locale,
      datePublished: item.value?.publishTime || item.value?.createTime,
      publisher: { '@type': 'Organization', name: siteName.value }
    })
  ]
}))
</script>

<template>
  <div v-if="item">
    <CommunityBreadcrumb
      :items="[
        { label: '社区首页', to: '/community' },
        { label: '公告', to: '/community/announcements' },
        { label: item.title }
      ]"
    />

    <article class="card">
      <header class="head">
        <h1 class="title">
          <span v-if="item.isPersistent === 1" class="tag">常驻</span>
          {{ item.title }}
        </h1>
        <div class="meta">
          <time>{{ publishLabel }}</time>
          <span v-if="item.viewCount">· 阅读 {{ item.viewCount }}</span>
          <NuxtLink to="/community/announcements" class="back">返回公告列表</NuxtLink>
        </div>
      </header>

      <div class="content" v-html="htmlContent" />
    </article>
  </div>
</template>

<style scoped>
.card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  padding: 22px;
}
.head {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 16px;
}
.title {
  margin: 0;
  font-size: 21px;
  line-height: 1.45;
  font-weight: 600;
}
.tag {
  display: inline-block;
  margin-right: 6px;
  padding: 1px 6px;
  border: 1px solid var(--c-accent);
  border-radius: 3px;
  color: var(--c-accent);
  font-size: 12px;
  font-weight: 400;
  vertical-align: 2px;
}
.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
  color: var(--c-text-faint);
  font-size: 12.5px;
}
.back {
  margin-left: auto;
  color: var(--c-accent);
}

/* 正文排版：与社区帖子保持一致的中性风格 */
.content {
  color: var(--c-text);
  font-size: 15px;
  line-height: 1.85;
  overflow-wrap: break-word;
}
.content :deep(h1),
.content :deep(h2),
.content :deep(h3),
.content :deep(h4) {
  margin: 22px 0 10px;
  font-weight: 600;
  line-height: 1.4;
}
.content :deep(h2) {
  font-size: 18px;
}
.content :deep(h3) {
  font-size: 16px;
}
.content :deep(p) {
  margin: 12px 0;
}
.content :deep(a) {
  color: var(--c-accent);
  text-decoration: underline;
}
.content :deep(ul),
.content :deep(ol) {
  margin: 12px 0;
  padding-left: 22px;
}
.content :deep(li) {
  margin: 5px 0;
}
.content :deep(blockquote) {
  margin: 12px 0;
  padding: 8px 14px;
  border-left: 3px solid var(--c-border-strong);
  background: var(--c-surface-alt);
  color: var(--c-text-muted);
}
.content :deep(code) {
  padding: 1px 5px;
  border: 1px solid var(--c-border);
  border-radius: 3px;
  background: var(--c-surface-alt);
  font-size: 13.5px;
}
.content :deep(pre) {
  margin: 12px 0;
  padding: 12px 14px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface-alt);
  overflow-x: auto;
}
.content :deep(pre code) {
  padding: 0;
  border: 0;
  background: transparent;
}
.content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--c-radius);
}
.content :deep(table) {
  width: 100%;
  margin: 12px 0;
  border-collapse: collapse;
  font-size: 14px;
}
.content :deep(th),
.content :deep(td) {
  padding: 7px 10px;
  border: 1px solid var(--c-border);
  text-align: left;
}
.content :deep(hr) {
  margin: 20px 0;
  border: 0;
  border-top: 1px solid var(--c-border);
}
</style>
