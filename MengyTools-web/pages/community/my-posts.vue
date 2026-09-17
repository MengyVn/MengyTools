<script setup lang="ts">
/**
 * 我的帖子：含待审与已屏蔽（公开列表只显示已发布，作者需要在这里看到自己的全部帖子）。
 */
import { Plus, Pencil, Trash2 } from 'lucide-vue-next'
import { useCommunity, POST_STATUS_TEXT, formatRelativeTime, type CommunityPostItem } from '~/composables/useCommunity'
import { useCommunitySite } from '~/composables/useCommunitySite'

definePageMeta({ layout: 'community' })

const route = useRoute()
const router = useRouter()
const { myPosts, deletePost } = useCommunity()
const { siteName } = useCommunitySite()
const { isLoggedIn, init } = useAuth()

const showLogin = ref(false)
const errMsg = ref('')
const okMsg = ref('')
const page = computed(() => Math.max(Number(route.query.page) || 1, 1))
const justCreatedPending = computed(() => route.query.created === 'pending')

onMounted(() => init())

const { data, refresh } = await useAsyncData(
  () => `my-posts-${page.value}-${isLoggedIn.value}`,
  () =>
    isLoggedIn.value
      ? myPosts({ page: page.value, size: 20 })
      : Promise.resolve({ records: [], total: 0, size: 20, current: 1, pages: 0 }),
  { watch: [page, isLoggedIn] }
)

const changePage = (p: number) => {
  router.push({ path: '/community/my-posts', query: { page: p } })
}

const remove = async (post: CommunityPostItem) => {
  if (!confirm(`确定删除《${post.title}》吗？删除后不可恢复。`)) return
  errMsg.value = ''
  try {
    await deletePost(post.id)
    okMsg.value = '已删除'
    await refresh()
  } catch (e: any) {
    errMsg.value = e?.data?.message || e?.message || '删除失败'
  }
}

const statusClass = (status: number) => (status === 1 ? 'ok' : status === 0 ? 'pending' : 'blocked')

useSeoMeta({
  title: () => `我的帖子 - ${siteName.value}`,
  robots: 'noindex'
})
</script>

<template>
  <div>
    <CommunityBreadcrumb :items="[{ label: '社区首页', to: '/community' }, { label: '我的帖子' }]" />

    <div class="head">
      <h1 class="title">我的帖子</h1>
      <NuxtLink to="/community/new" class="new-btn"><Plus :size="14" />发布新帖</NuxtLink>
    </div>

    <p v-if="justCreatedPending" class="banner">
      帖子已提交，因包含链接或账号较新，需审核通过后才会公开显示。
    </p>
    <p v-if="errMsg" class="msg err">{{ errMsg }}</p>
    <p v-if="okMsg" class="msg ok">{{ okMsg }}</p>

    <div v-if="!isLoggedIn" class="card empty">
      <p>登录后可以查看并管理自己的帖子。</p>
      <button type="button" class="btn primary" @click="showLogin = true">登录 / 注册</button>
    </div>

    <template v-else>
      <ul v-if="data?.records?.length" class="list">
        <li v-for="post in data.records" :key="post.id" class="row">
          <div class="row-main">
            <div class="row-title">
              <span class="badge" :class="statusClass(post.status)">{{ POST_STATUS_TEXT[post.status] || post.status }}</span>
              <NuxtLink v-if="post.status === 1" :to="`/community/posts/${post.id}`" class="link">
                {{ post.title }}
              </NuxtLink>
              <span v-else class="plain">{{ post.title }}</span>
            </div>
            <p v-if="post.summary" class="row-summary">{{ post.summary }}</p>
            <div class="row-meta">
              <time>{{ formatRelativeTime(post.createTime) }}</time>
              <span v-if="post.status === 1">· {{ post.commentCount ?? 0 }} 评论</span>
              <span v-if="post.status === 1">· {{ post.viewCount ?? 0 }} 浏览</span>
            </div>
          </div>
          <div class="row-actions">
            <NuxtLink :to="`/community/edit/${post.id}`" class="icon-btn"><Pencil :size="13" />编辑</NuxtLink>
            <button type="button" class="icon-btn danger" @click="remove(post)"><Trash2 :size="13" />删除</button>
          </div>
        </li>
      </ul>
      <p v-else class="card empty">还没有发布过帖子，点右上角「发布新帖」开始吧。</p>

      <CommunityPager
        :current="data?.current ?? 1"
        :pages="data?.pages ?? 0"
        :total="data?.total"
        @change="changePage"
      />
    </template>

    <CommunityAuthModal v-model:visible="showLogin" @logged-in="refresh" />
  </div>
</template>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 14px;
}
.title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
.new-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 14px;
  border: 1px solid var(--c-accent);
  border-radius: var(--c-radius);
  background: var(--c-accent);
  color: #fff;
  font-size: 13.5px;
}
.banner {
  margin: 0 0 12px;
  padding: 10px 12px;
  border: 1px solid var(--c-border);
  border-left: 3px solid var(--c-accent);
  border-radius: var(--c-radius);
  background: var(--c-accent-soft);
  color: var(--c-text-muted);
  font-size: 13px;
  line-height: 1.7;
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
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--c-border);
}
.row:last-child {
  border-bottom: 0;
}
.row-main {
  min-width: 0;
  flex: 1;
}
.row-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
}
.badge {
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 400;
  border: 1px solid var(--c-border);
}
.badge.ok {
  border-color: var(--c-accent);
  color: var(--c-accent);
  background: var(--c-accent-soft);
}
.badge.pending {
  border-color: #e6a23c;
  color: #b8791f;
}
.badge.blocked {
  border-color: #d14343;
  color: #d14343;
}
.link {
  color: var(--c-text);
}
.link:hover {
  color: var(--c-accent);
}
.plain {
  color: var(--c-text-muted);
}
.row-summary {
  margin: 6px 0 0;
  color: var(--c-text-muted);
  font-size: 13px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.row-meta {
  margin-top: 6px;
  color: var(--c-text-faint);
  font-size: 12.5px;
}
.row-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}
.icon-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--c-text-muted);
  font-size: 13px;
  cursor: pointer;
}
.icon-btn:hover {
  color: var(--c-accent);
}
.icon-btn.danger:hover {
  color: #d14343;
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
.msg {
  margin: 0 0 10px;
  font-size: 13px;
}
.msg.err {
  color: #d14343;
}
.msg.ok {
  color: #2f9e44;
}
.btn {
  height: 32px;
  padding: 0 16px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text-muted);
  font-size: 13.5px;
  cursor: pointer;
}
.btn.primary {
  border-color: var(--c-accent);
  background: var(--c-accent);
  color: #fff;
}
</style>
