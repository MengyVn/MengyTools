<script setup lang="ts">
/**
 * 板块页（论坛形态）。
 *
 * 术语调整：社区不再使用标签维度，原「分类」承担板块职责——单选、有导航意义，
 * 数据仍是 blog_category（与门户共用，由后台「博客分类管理」维护）。
 */
import { useCommunity, formatRelativeTime } from '~/composables/useCommunity'
import { useCommunitySite } from '~/composables/useCommunitySite'

definePageMeta({ layout: 'community' })

const { boards } = useCommunity()
const { siteName } = useCommunitySite()

const { data } = await useAsyncData('community-boards', () => boards())

const totalPosts = computed(() =>
  (data.value ?? []).reduce((sum, b) => sum + Number(b.postCount ?? 0), 0)
)

useSeoMeta({
  title: () => `板块 - ${siteName.value}`,
  description: () => `${siteName.value} 的板块导航：按主题浏览全部帖子`
})
</script>

<template>
  <div>
    <CommunityBreadcrumb :items="[{ label: '社区首页', to: '/community' }, { label: '板块' }]" />

    <div class="layout">
      <div class="main-col">
        <header class="head">
          <h1 class="title">板块</h1>
          <p class="desc">
            共 {{ data?.length ?? 0 }} 个板块 · {{ totalPosts }} 篇内容，点击板块查看其下帖子。
          </p>
        </header>

        <ul v-if="data?.length" class="boards">
          <li v-for="b in data" :key="b.id" class="board">
            <div class="board-main">
              <NuxtLink :to="`/community?category=${b.id}`" class="board-name">{{ b.name }}</NuxtLink>
              <span class="board-count">{{ b.postCount ?? 0 }} 篇</span>
            </div>
            <div class="board-latest">
              <template v-if="b.latestPostId">
                <NuxtLink :to="`/community/posts/${b.latestPostId}`" class="latest-title">
                  {{ b.latestPostTitle }}
                </NuxtLink>
                <time class="latest-time">{{ formatRelativeTime(b.latestPostTime) }}</time>
              </template>
              <span v-else class="latest-empty">暂无内容</span>
            </div>
          </li>
        </ul>
        <p v-else class="empty">还没有板块。</p>
      </div>

      <CommunitySidebar class="side-col" />
    </div>
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
  padding-bottom: 12px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 12px;
}
.title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}
.desc {
  margin: 6px 0 0;
  color: var(--c-text-muted);
  font-size: 13.5px;
}
.boards {
  list-style: none;
  margin: 0;
  padding: 0;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
}
.board {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 13px 16px;
  border-bottom: 1px solid var(--c-border);
}
.board:last-child {
  border-bottom: 0;
}
.board:hover {
  background: var(--c-surface-alt);
}
.board-main {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
}
.board-name {
  color: var(--c-text);
  font-size: 15px;
  font-weight: 500;
}
.board-name:hover {
  color: var(--c-accent);
}
.board-count {
  flex-shrink: 0;
  padding: 0 6px;
  border: 1px solid var(--c-border);
  border-radius: 3px;
  background: var(--c-surface-alt);
  color: var(--c-text-faint);
  font-size: 12px;
}
.board-latest {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
  max-width: 55%;
}
.latest-title {
  color: var(--c-text-muted);
  font-size: 13.5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.latest-title:hover {
  color: var(--c-accent);
}
.latest-time {
  flex-shrink: 0;
  color: var(--c-text-faint);
  font-size: 12.5px;
}
.latest-empty {
  color: var(--c-text-faint);
  font-size: 13px;
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
  .board {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }
  .board-latest {
    max-width: 100%;
  }
}
</style>
