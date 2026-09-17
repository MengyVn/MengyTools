<script setup lang="ts">
/**
 * 社区用户公开主页：资料 + 统计 + TA 的评论（带所属文章，可跳回上下文）。
 * 后端不返回登录名（username），避免账户枚举。
 */
import { COMMUNITY_SITE } from '~/community-site.config'
import { useCommunity, formatRelativeTime } from '~/composables/useCommunity'
import { useCommunitySite } from '~/composables/useCommunitySite'
import { useAuth } from '~/composables/useAuth'

definePageMeta({ layout: 'community' })

const route = useRoute()
const router = useRouter()
const { user: fetchUser, userComments, follow, followState } = useCommunity()
const { isLoggedIn, user: me, init } = useAuth()
const { siteName } = useCommunitySite()

const showLogin = ref(false)
const followActive = ref(false)
const followCount = ref(0)
const followBusy = ref(false)

onMounted(() => init())

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

const id = computed(() => String(route.params.id))
const page = computed(() => Math.max(Number(route.query.page) || 1, 1))

const { data: profile, error } = await useAsyncData(
  () => `community-user-${id.value}`,
  () => fetchUser(id.value),
  { watch: [id] }
)

const { data: comments } = await useAsyncData(
  () => `community-user-${id.value}-comments-${page.value}`,
  () => userComments(id.value, { page: page.value, size: 20 }),
  { watch: [id, page] }
)

if (error.value || !profile.value) {
  throw createError({ statusCode: 404, statusMessage: '用户不存在', fatal: true })
}

const changePage = (p: number) => {
  router.push({ path: route.path, query: { page: p } })
}

// 登录态或用户切换时刷新关注状态（必须放在 id 定义之后：immediate 会同步执行）
watch([isLoggedIn, id], loadFollow, { immediate: true })

const initials = computed(() => (profile.value?.nickname || '?').slice(0, 1).toUpperCase())

const joinedAt = computed(() => {
  const t = profile.value?.createTime
  return t ? new Date(String(t).replace(' ', 'T')).toLocaleDateString('zh-CN') : ''
})

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
        <span v-if="followCount" class="follow-count">{{ followCount }} 人关注</span>
      </div>

      <ul class="user-stats">
        <li>
          <strong>{{ profile.commentCount ?? 0 }}</strong>
          <span>评论</span>
        </li>
        <li>
          <strong>{{ profile.articleCount ?? 0 }}</strong>
          <span>文章</span>
        </li>
        <li>
          <strong>{{ profile.receivedLikeCount ?? 0 }}</strong>
          <span>获赞</span>
        </li>
      </ul>
    </header>

    <section class="card comments-card">
      <h2 class="card-title">TA 的评论 <em>{{ comments?.total ?? 0 }}</em></h2>

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

      <CommunityPager
        :current="comments?.current ?? 1"
        :pages="comments?.pages ?? 0"
        :total="comments?.total"
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
.follow-count {
  color: var(--c-text-faint);
  font-size: 12px;
}

.user-stats {
  display: flex;
  gap: 28px;
  list-style: none;
  margin: 0;
  padding: 0;
}
.user-stats li {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.user-stats strong {
  font-size: 18px;
  font-weight: 600;
}
.user-stats span {
  color: var(--c-text-faint);
  font-size: 12.5px;
}

.comments-card {
  margin-top: 16px;
}
.card-title {
  margin: 0 0 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--c-border);
  font-size: 15px;
  font-weight: 600;
}
.card-title em {
  margin-left: 4px;
  color: var(--c-text-faint);
  font-style: normal;
  font-size: 13px;
}
.comment-list {
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
.empty {
  padding: 24px 0;
  text-align: center;
  color: var(--c-text-faint);
  font-size: 13.5px;
}
</style>
