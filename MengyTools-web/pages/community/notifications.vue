<script setup lang="ts">
/**
 * 我的消息：被回复 / 被提及 / 被点赞 / 系统通知。
 * 未登录时提示登录（页面受后端保护，匿名请求会 401）。
 */
import { COMMUNITY_SITE } from '~/community-site.config'
import { useCommunity, formatRelativeTime, notificationTarget, NOTIFICATION_TYPE_TEXT } from '~/composables/useCommunity'
import { useCommunitySite } from '~/composables/useCommunitySite'
import { useAuth } from '~/composables/useAuth'

definePageMeta({ layout: 'community' })

const route = useRoute()
const router = useRouter()
const { notifications, markAllRead } = useCommunity()
const { isLoggedIn, init } = useAuth()
const { siteName } = useCommunitySite()

const page = computed(() => Math.max(Number(route.query.page) || 1, 1))
const unreadOnly = computed(() => route.query.unread === '1')
const showLogin = ref(false)
const marking = ref(false)

onMounted(() => init())

const { data, refresh } = await useAsyncData(
  () => `community-notifications-${page.value}-${unreadOnly.value}`,
  () =>
    isLoggedIn.value
      ? notifications({ page: page.value, size: 20, unreadOnly: unreadOnly.value })
      : // 未登录返回空分页结构（useAsyncData 不允许返回 null，否则会有告警并可能在客户端重复请求）
        Promise.resolve({ records: [], total: 0, size: 20, current: 1, pages: 0 }),
  { watch: [page, unreadOnly] }
)

watch(isLoggedIn, () => refresh())

const changePage = (p: number) => {
  router.push({ path: '/community/notifications', query: { ...route.query, page: p } })
}

const toggleUnread = () => {
  router.push({
    path: '/community/notifications',
    query: unreadOnly.value ? {} : { unread: '1' }
  })
}

const doMarkAllRead = async () => {
  marking.value = true
  try {
    await markAllRead()
    await refresh()
  } finally {
    marking.value = false
  }
}

useSeoMeta({
  title: () => `我的消息 - ${siteName.value}`,
  robots: 'noindex'
})
</script>

<template>
  <div>
    <CommunityBreadcrumb :items="[{ label: '社区首页', to: '/community' }, { label: '我的消息' }]" />

    <div class="layout">
      <div class="main-col">
        <div class="head">
          <h1 class="title">我的消息</h1>
          <div class="head-actions">
            <button type="button" class="tab" :class="{ active: unreadOnly }" @click="toggleUnread">
              {{ unreadOnly ? '只看未读' : '全部' }}
            </button>
            <button type="button" class="btn" :disabled="marking" @click="doMarkAllRead">全部标为已读</button>
          </div>
        </div>

        <template v-if="!isLoggedIn">
          <div class="card empty">
            <p>登录后可以查看回复、提及与点赞消息。</p>
            <button type="button" class="btn primary" @click="showLogin = true">登录 / 注册</button>
          </div>
        </template>

        <template v-else>
          <ul v-if="data?.records?.length" class="list">
            <li v-for="n in data.records" :key="n.id" class="row" :class="{ unread: !n.isRead }">
              <span class="type">{{ NOTIFICATION_TYPE_TEXT[n.type] || n.type }}</span>
              <div class="body">
                <NuxtLink :to="notificationTarget(n)" class="row-title">
                  {{ n.title }}
                  <span v-if="!n.isRead" class="dot" />
                </NuxtLink>
                <p v-if="n.content" class="row-content">{{ n.content }}</p>
              </div>
              <time class="time">{{ formatRelativeTime(n.createTime) }}</time>
            </li>
          </ul>
          <p v-else class="card empty">暂无消息。</p>

          <CommunityPager
            :current="data?.current ?? 1"
            :pages="data?.pages ?? 0"
            :total="data?.total"
            @change="changePage"
          />
        </template>
      </div>

      <CommunitySidebar class="side-col" />
    </div>

    <LoginModal v-model:visible="showLogin" @logged-in="showLogin = false" />
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 12px;
}
.title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.tab {
  padding: 4px 10px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text-muted);
  font-size: 12.5px;
  cursor: pointer;
}
.tab.active {
  border-color: var(--c-accent);
  color: var(--c-accent);
}
.btn {
  height: 28px;
  padding: 0 12px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text-muted);
  font-size: 12.5px;
  cursor: pointer;
}
.btn.primary {
  border-color: var(--c-accent);
  color: var(--c-accent);
}
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
}
.row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--c-border);
}
.row:last-child {
  border-bottom: 0;
}
.row.unread {
  background: var(--c-accent-soft);
}
.type {
  flex-shrink: 0;
  padding: 1px 7px;
  border: 1px solid var(--c-border);
  border-radius: 3px;
  background: var(--c-surface);
  color: var(--c-text-muted);
  font-size: 12px;
}
.body {
  flex: 1;
  min-width: 0;
}
.row-title {
  color: var(--c-text);
  font-size: 14px;
  font-weight: 500;
}
.row-title:hover {
  color: var(--c-accent);
}
.dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-left: 6px;
  border-radius: 50%;
  background: #d14343;
  vertical-align: 1px;
}
.row-content {
  margin: 4px 0 0;
  color: var(--c-text-muted);
  font-size: 13px;
  white-space: pre-wrap;
  overflow-wrap: break-word;
}
.time {
  flex-shrink: 0;
  color: var(--c-text-faint);
  font-size: 12.5px;
}
.card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  padding: 20px;
}
.empty {
  text-align: center;
  color: var(--c-text-muted);
  font-size: 13.5px;
}
.empty p {
  margin: 0 0 12px;
}
@media (max-width: 900px) {
  .layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
