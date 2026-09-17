<script setup lang="ts">
/**
 * 发帖页。编辑器组件按页懒加载（普通访客不会下载 md-editor-v3）。
 */
import { useCommunitySite } from '~/composables/useCommunitySite'

definePageMeta({ layout: 'community' })

const { isLoggedIn, init } = useAuth()
const { siteName } = useCommunitySite()
const router = useRouter()
const showLogin = ref(false)

onMounted(() => init())

const onSubmitted = (payload: { id: number; status: number }) => {
  if (payload.status === 0) {
    // 待审帖未公开，详情页不可见，引导到「我的帖子」
    router.push({ path: '/community/my-posts', query: { created: 'pending' } })
  } else {
    router.push(`/community/posts/${payload.id}`)
  }
}

useSeoMeta({
  title: () => `发布帖子 - ${siteName.value}`,
  robots: 'noindex'
})
</script>

<template>
  <div>
    <CommunityBreadcrumb
      :items="[
        { label: '社区首页', to: '/community' },
        { label: '发布帖子' }
      ]"
    />

    <div v-if="!isLoggedIn" class="card empty">
      <p>登录后即可发布帖子。</p>
      <button type="button" class="btn primary" @click="showLogin = true">登录 / 注册</button>
    </div>

    <CommunityPostEditor v-else mode="create" @submitted="onSubmitted" />

    <CommunityAuthModal v-model:visible="showLogin" />
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
