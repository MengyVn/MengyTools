<script setup lang="ts">
/**
 * 编辑自己的帖子（路径用扁平的 /community/edit/{id}）。
 *
 * 说明：一开始放在 pages/community/posts/[id]/edit.vue，与 posts/[id].vue
 * 形成「同一路由段既是文件又是目录」，实测该写法下 /posts/{id}/edit 会被详情页抢先匹配，
 * 因此改为独立路径，避免歧义。
 *
 * 数据在客户端加载：这是私有页面（noindex），不需要 SSR；
 * 用 SSR 抓私有数据还会引入「服务端 cookie 时序」这类无谓复杂度。
 * 权限由后端按 author_id 校验，非作者得到 404。
 */
import {useCommunity} from '~/composables/useCommunity'
import { useCommunitySite } from '~/composables/useCommunitySite'
import type { CommunityArticleDetail } from '~/composables/useCommunity'

definePageMeta({ layout: 'community' })

const route = useRoute()
const router = useRouter()
const { myPostDetail } = useCommunity()
const { siteName } = useCommunitySite()
const { isLoggedIn, init } = useAuth()

const showLogin = ref(false)
const postId = computed(() => Number(route.params.id))

const post = ref<CommunityArticleDetail | null>(null)
const loading = ref(true)
const loadErr = ref('')

const loadPost = async () => {
  loading.value = true
  loadErr.value = ''
  try {
    await init()
    if (!isLoggedIn.value) {
      return
    }
    post.value = await myPostDetail(postId.value)
  } catch (e: any) {
    loadErr.value = e?.data?.message || e?.message || '帖子不存在或不属于你'
    post.value = null
  } finally {
    loading.value = false
  }
}

onMounted(loadPost)
watch(postId, loadPost)

const initial = computed(() => {
  const p = post.value
  if (!p) return null
  return {
    title: p.title,
    summary: p.summary || '',
    content: p.content || '',
    cover: p.cover || '',
    categoryId: p.categoryId ?? null,
    allowComment: p.allowComment ?? 1
  }
})

const onSubmitted = (payload: { id: number }) => {
  router.push(`/community/posts/${payload.id}`)
}

useSeoMeta({
  title: () => `编辑帖子 - ${siteName.value}`,
  robots: 'noindex'
})
</script>

<template>
  <div>
    <CommunityBreadcrumb
      :items="[
        { label: '社区首页', to: '/community' },
        { label: '我的帖子', to: '/community/my-posts' },
        { label: '编辑帖子' }
      ]"
    />

    <div v-if="loading" class="card empty">加载中…</div>

    <div v-else-if="!isLoggedIn" class="card empty">
      <p>请先登录后再编辑自己的帖子。</p>
      <button type="button" class="btn primary" @click="showLogin = true">登录 / 注册</button>
    </div>

    <div v-else-if="loadErr" class="card empty">
      <p>{{ loadErr }}</p>
      <NuxtLink to="/community/my-posts" class="btn primary">返回我的帖子</NuxtLink>
    </div>

    <CommunityPostEditor
      v-else-if="initial"
      mode="edit"
      :post-id="postId"
      :initial="initial"
      @submitted="onSubmitted"
    />

    <CommunityAuthModal v-model:visible="showLogin" @logged-in="loadPost" />
  </div>
</template>

<style scoped>
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
.btn {
  display: inline-block;
  height: 32px;
  line-height: 30px;
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
