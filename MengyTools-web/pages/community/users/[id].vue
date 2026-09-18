<script setup lang="ts">
/**
 * 社区用户公开主页：资料 + 统计 + TA 的评论 / 关注 / 粉丝。
 * 后端不返回登录名（username），避免账户枚举。
 *
 * 关注/粉丝列表走公开接口，登录后每行带 followed 标记（我是否已关注），
 * 因此列表内可以直接关注/取关，不必逐行再查一次状态。
 */
import { COMMUNITY_SITE } from '~/community-site.config'
import { useCommunity, formatRelativeTime, type PageResult, type CommunityUserComment, type CommunityFollowUser } from '~/composables/useCommunity'
import { useCommunitySite } from '~/composables/useCommunitySite'
import { useAuth } from '~/composables/useAuth'

definePageMeta({ layout: 'community' })

/** 空分页（未激活的 tab 用它占位，避免多打一次接口，也避免 useAsyncData 返回 null） */
const emptyComments = (): PageResult<CommunityUserComment> => ({ records: [], total: 0, size: 20, current: 1, pages: 0 })
const emptyUsers = (): PageResult<CommunityFollowUser> => ({ records: [], total: 0, size: 20, current: 1, pages: 0 })

const TABS = [
  { key: 'comments', label: '评论' },
  { key: 'following', label: '关注' },
  { key: 'followers', label: '粉丝' }
] as const
type TabKey = (typeof TABS)[number]['key']

const route = useRoute()
const router = useRouter()
const { user: fetchUser, userComments, following, followers, follow, followState } = useCommunity()
const { isLoggedIn, user: me, init } = useAuth()
const { siteName } = useCommunitySite()

const showLogin = ref(false)
const followActive = ref(false)
const followCount = ref(0)
const followBusy = ref(false)
/** 列表内关注按钮的忙碌态（按用户 id 记录） */
const rowBusy = ref<number | null>(null)

onMounted(() => init())

const id = computed(() => String(route.params.id))
const page = computed(() => Math.max(Number(route.query.page) || 1, 1))
const tab = computed<TabKey>(() => {
  const t = String(route.query.tab || 'comments')
  return (TABS.some((x) => x.key === t) ? t : 'comments') as TabKey
})

const loadFollow = async () => {
  if (!id.value) return
  try {
    if (isLoggedIn.value) {
      const s = await followState('user', Number(id.value))
      followActive.value = s.active
      followCount.value = s.count
    } else {
      followActive.value = false
    }
  } catch {
    /* 忽略 */
  }
}

const toggleFollow = async () => {
  if (!isLoggedIn.value) {
    showLogin.value = true
    return
  }
  followBusy.value = true
  try {
    const res = await follow({ targetType: 'user', targetId: Number(id.value) })
    followActive.value = res.active
    followCount.value = res.count
  } finally {
    followBusy.value = false
  }
}

const isSelf = computed(() => isLoggedIn.value && me.value?.id === Number(id.value))

const { data: profile, error } = await useAsyncData(
  () => `community-user-${id.value}`,
  () => fetchUser(id.value),
  { watch: [id] }
)

const { data: comments } = await useAsyncData(
  () => `community-user-${id.value}-comments-${page.value}`,
  () =>
    tab.value === 'comments'
      ? userComments(id.value, { page: page.value, size: 20 })
      : Promise.resolve(emptyComments()),
  { watch: [id, page, tab] }
)

/** 关注/粉丝共用一份「当前 tab 的列表」数据（key 必须与评论那份区分开，否则同名会报警） */
const { data: followList } = await useAsyncData(
  () => `community-user-${id.value}-${tab.value}-list-${page.value}`,
  () => {
    if (tab.value === 'following') return following(id.value, { page: page.value, size: 20 })
    if (tab.value === 'followers') return followers(id.value, { page: page.value, size: 20 })
    return Promise.resolve(emptyUsers())
  },
  { watch: [id, page, tab] }
)

if (error.value || !profile.value) {
  throw createError({ statusCode: 404, statusMessage: '用户不存在', fatal: true })
}

const switchTab = (key: TabKey) => {
  router.push({ path: route.path, query: key === 'comments' ? {} : { tab: key } })
}

const changePage = (p: number) => {
  router.push({ path: route.path, query: { ...route.query, page: p } })
}

/** 列表内关注/取关：就地更新该行状态，并同步自己主页的「关注数」 */
const toggleRowFollow = async (row: CommunityFollowUser) => {
  if (!isLoggedIn.value) {
    showLogin.value = true
    return
  }
  if (me.value?.id === row.id) return
  rowBusy.value = row.id
  try {
    const res = await follow({ targetType: 'user', targetId: row.id })
    row.followed = res.active
    if (isSelf.value && profile.value) {
      const cur = Number(profile.value.followingCount ?? 0)
      profile.value.followingCount = Math.max(cur + (res.active ? 1 : -1), 0)
    }
  } finally {
    rowBusy.value = null
  }
}

// 登录态或用户切换时刷新关注状态（必须放在 id 定义之后：immediate 会同步执行）
watch([isLoggedIn, id], loadFollow, { immediate: true })

const initials = computed(() => (profile.value?.nickname || '?').slice(0, 1).toUpperCase())

const joinedAt = computed(() => {
  const t = profile.value?.createTime
  return t ? new Date(String(t).replace(' ', 'T')).toLocaleDateString('zh-CN') : ''
})

const stats = computed(() => [
  { key: 'comments' as TabKey, label: '评论', value: profile.value?.commentCount ?? 0 },
  { key: 'following' as TabKey, label: '关注', value: profile.value?.followingCount ?? 0 },
  { key: 'followers' as TabKey, label: '粉丝', value: profile.value?.followerCount ?? 0 }
])

const activeTotal = computed(() =>
  tab.value === 'comments' ? comments.value?.total ?? 0 : followList.value?.total ?? 0
)

useSeoMeta({
  title: () => `${profile.value?.nickname ?? '用户'} - ${siteName.value}`,
  description: () => profile.value?.signature || `${profile.value?.nickname} 在 ${siteName.value} 的主页`
})

const { absolute, jsonLd } = useCommunitySeo()
useHead(() => ({
  link: [{ rel: 'canonical', href: absolute(`/community/users/${id.value}`) }],
  script: [
    jsonLd({
      '@context': 'https://schema.org',
      '@type': 'ProfilePage',
      url: absolute(`/community/users/${id.value}`),
      inLanguage: COMMUNITY_SITE.locale,
      mainEntity: {
        '@type': 'Person',
        name: profile.value?.nickname,
        description: profile.value?.signature || undefined,
        image: profile.value?.avatar || undefined,
        interactionStatistic: {
          '@type': 'InteractionCounter',
          interactionType: 'https://schema.org/CommentAction',
          userInteractionCount: profile.value?.commentCount ?? 0
        }
      }
    })
  ]
}))
</script>

<template>
  <div v-if="profile" class="profile">
    <CommunityBreadcrumb :items="[{ label: '社区首页', to: '/community' }, { label: profile.nickname }]" />

    <header class="card user-head">
      <img v-if="profile.avatar" :src="profile.avatar" :alt="profile.nickname" class="user-avatar" />
      <span v-else class="user-avatar user-avatar-text">{{ initials }}</span>
      <div class="user-info">
        <h1 class="user-name">{{ profile.nickname }}</h1>
        <p class="user-signature">{{ profile.signature || '这个人很低调，还没有写签名。' }}</p>
        <p v-if="joinedAt" class="user-joined">加入于 {{ joinedAt }}</p>
      </div>
      <div class="user-actions">
        <button v-if="!isSelf" type="button" class="follow-btn" :class="{ active: followActive }" :disabled="followBusy" @click="toggleFollow">
          {{ followActive ? '已关注' : '关注' }}
        </button>
        <span v-if="followCount" class="follow-count">{{ followCount }} 粉丝</span>
      </div>

      <ul class="user-stats">
        <li v-for="s in stats" :key="s.key">
          <button type="button" class="stat-btn" :class="{ active: tab === s.key }" @click="switchTab(s.key)">
            <strong>{{ s.value }}</strong>
            <span>{{ s.label }}</span>
          </button>
        </li>
        <li>
          <span class="stat-plain">
            <strong>{{ profile.articleCount ?? 0 }}</strong>
            <span>文章</span>
          </span>
        </li>
        <li>
          <span class="stat-plain">
            <strong>{{ profile.receivedLikeCount ?? 0 }}</strong>
            <span>获赞</span>
          </span>
        </li>
      </ul>
    </header>

    <section class="card list-card">
      <div class="card-head">
        <h2 class="card-title">{{ tab === 'comments' ? 'TA 的评论' : tab === 'following' ? 'TA 关注的人' : 'TA 的粉丝' }}</h2>
        <nav class="tabs">
          <button
            v-for="t in TABS"
            :key="t.key"
            type="button"
            class="tab"
            :class="{ active: tab === t.key }"
            @click="switchTab(t.key)"
          >
            {{ t.label }}
            <em v-if="t.key === 'comments'">{{ profile.commentCount ?? 0 }}</em>
            <em v-else-if="t.key === 'following'">{{ profile.followingCount ?? 0 }}</em>
            <em v-else>{{ profile.followerCount ?? 0 }}</em>
          </button>
        </nav>
      </div>

      <!-- 评论列表 -->
      <template v-if="tab === 'comments'">
        <ul v-if="comments?.records?.length" class="comment-list">
          <li v-for="c in comments.records" :key="c.id" class="comment-row">
            <div class="comment-context">
              <NuxtLink :to="`/community/posts/${c.articleId}`" class="comment-article">
                {{ c.articleTitle || `文章 #${c.articleId}` }}
              </NuxtLink>
              <span v-if="c.parentId" class="comment-tag">回复</span>
            </div>
            <p class="comment-content">{{ c.content }}</p>
            <div class="comment-meta">
              <time>{{ formatRelativeTime(c.createTime) }}</time>
              <span v-if="c.likeCount">· 赞 {{ c.likeCount }}</span>
            </div>
          </li>
        </ul>
        <p v-else class="empty">TA 还没有发表过评论。</p>
      </template>

      <!-- 关注 / 粉丝列表 -->
      <template v-else>
        <ul v-if="followList?.records?.length" class="user-list">
          <li v-for="u in followList.records" :key="u.id" class="user-row">
            <NuxtLink :to="`/community/users/${u.id}`" class="row-avatar-link">
              <img v-if="u.avatar" :src="u.avatar" :alt="u.nickname" class="row-avatar" />
              <span v-else class="row-avatar row-avatar-text">{{ (u.nickname || '?').slice(0, 1).toUpperCase() }}</span>
            </NuxtLink>
            <div class="row-info">
              <NuxtLink :to="`/community/users/${u.id}`" class="row-name">{{ u.nickname }}</NuxtLink>
              <p class="row-signature">{{ u.signature || '这个人很低调，还没有写签名。' }}</p>
              <time v-if="u.followTime" class="row-time">{{ formatRelativeTime(u.followTime) }}</time>
            </div>
            <button
              v-if="me?.id !== u.id"
              type="button"
              class="follow-btn small"
              :class="{ active: u.followed }"
              :disabled="rowBusy === u.id"
              @click="toggleRowFollow(u)"
            >
              {{ u.followed ? '已关注' : '关注' }}
            </button>
          </li>
        </ul>
        <p v-else class="empty">
          {{ tab === 'following' ? 'TA 还没有关注任何人。' : 'TA 还没有粉丝。' }}
        </p>
      </template>

      <CommunityPager
        :current="(tab === 'comments' ? comments?.current : followList?.current) ?? 1"
        :pages="(tab === 'comments' ? comments?.pages : followList?.pages) ?? 0"
        :total="activeTotal"
        @change="changePage"
      />
    </section>

    <CommunityAuthModal v-model:visible="showLogin" @logged-in="showLogin = false" />
  </div>
</template>

<style scoped>
.card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  padding: 18px;
}
.user-head {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}
.user-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.user-avatar-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--c-accent-soft);
  color: var(--c-accent);
  font-size: 26px;
  font-weight: 600;
}
.user-info {
  flex: 1;
  min-width: 200px;
}
.user-name {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
.user-signature {
  margin: 6px 0 0;
  color: var(--c-text-muted);
  font-size: 14px;
}
.user-joined {
  margin: 4px 0 0;
  color: var(--c-text-faint);
  font-size: 12.5px;
}
.user-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}
.follow-btn {
  height: 30px;
  padding: 0 16px;
  border: 1px solid var(--c-accent);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-accent);
  font-size: 13.5px;
  cursor: pointer;
}
.follow-btn.active {
  background: var(--c-accent-soft);
}
.follow-btn.small {
  height: 28px;
  padding: 0 14px;
  font-size: 13px;
  flex-shrink: 0;
}
.follow-btn:disabled {
  opacity: 0.6;
  cursor: default;
}
.follow-count {
  color: var(--c-text-faint);
  font-size: 12px;
}

.user-stats {
  display: flex;
  gap: 24px;
  list-style: none;
  margin: 0;
  padding: 0;
}
.user-stats li {
  display: flex;
}
.stat-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
  padding: 4px 8px;
  border: 1px solid transparent;
  border-radius: var(--c-radius);
  background: transparent;
  cursor: pointer;
  font: inherit;
  color: inherit;
}
.stat-btn:hover {
  border-color: var(--c-border);
}
.stat-btn.active {
  border-color: var(--c-accent);
  background: var(--c-accent-soft);
}
.stat-plain {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 8px;
}
.user-stats strong {
  font-size: 18px;
  font-weight: 600;
}
.user-stats span {
  color: var(--c-text-faint);
  font-size: 12.5px;
}

.list-card {
  margin-top: 16px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--c-border);
}
.card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}
.tabs {
  display: flex;
  gap: 6px;
}
.tab {
  height: 28px;
  padding: 0 12px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text-muted);
  font-size: 13px;
  cursor: pointer;
}
.tab.active {
  border-color: var(--c-accent);
  color: var(--c-accent);
  background: var(--c-accent-soft);
}
.tab em {
  margin-left: 4px;
  color: var(--c-text-faint);
  font-style: normal;
  font-size: 12px;
}

.comment-list,
.user-list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.comment-row {
  padding: 12px 0;
  border-bottom: 1px solid var(--c-border);
}
.comment-row:last-child {
  border-bottom: 0;
}
.comment-context {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.comment-article {
  color: var(--c-text-muted);
}
.comment-article:hover {
  color: var(--c-accent);
}
.comment-tag {
  padding: 0 5px;
  border-radius: 3px;
  background: var(--c-surface-alt);
  border: 1px solid var(--c-border);
  color: var(--c-text-faint);
  font-size: 11.5px;
}
.comment-content {
  margin: 6px 0 0;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: break-word;
}
.comment-meta {
  margin-top: 6px;
  color: var(--c-text-faint);
  font-size: 12.5px;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--c-border);
}
.user-row:last-child {
  border-bottom: 0;
}
.row-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.row-avatar-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--c-accent-soft);
  color: var(--c-accent);
  font-size: 16px;
  font-weight: 600;
}
.row-info {
  flex: 1;
  min-width: 0;
}
.row-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--c-text);
}
.row-name:hover {
  color: var(--c-accent);
}
.row-signature {
  margin: 2px 0 0;
  color: var(--c-text-muted);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row-time {
  display: inline-block;
  margin-top: 2px;
  color: var(--c-text-faint);
  font-size: 12px;
}

.empty {
  padding: 24px 0;
  text-align: center;
  color: var(--c-text-faint);
  font-size: 13.5px;
}
</style>
