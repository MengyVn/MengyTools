<script setup lang="ts">
/**
 * 社区侧栏：站点统计 + 热门标签 + 活跃用户。
 * 数据来自单次聚合接口 /v1/community/sidebar，避免并发多个小请求。
 */
import { useCommunity, formatCount } from '~/composables/useCommunity'

const { sidebar } = useCommunity()

// key 固定：同一请求内多页复用，SSR 只取一次
const { data } = await useAsyncData('community-sidebar', () => sidebar())

const stats = computed(() => {
  const d = data.value
  return [
    { label: '文章', value: formatCount(d?.articleCount) },
    { label: '评论', value: formatCount(d?.commentCount) },
    { label: '成员', value: formatCount(d?.userCount) },
    { label: '标签', value: formatCount(d?.tagCount) }
  ]
})

const initials = (name?: string) => (name || '?').slice(0, 1).toUpperCase()
</script>

<template>
  <aside class="sb">
    <section class="sb-card">
      <h3 class="sb-title">社区数据</h3>
      <ul class="sb-stats">
        <li v-for="s in stats" :key="s.label">
          <strong>{{ s.value }}</strong>
          <span>{{ s.label }}</span>
        </li>
      </ul>
    </section>

    <section v-if="data?.hotTags?.length" class="sb-card">
      <h3 class="sb-title">热门标签</h3>
      <div class="sb-tags">
        <NuxtLink
          v-for="tag in data.hotTags"
          :key="tag.id"
          :to="`/community/tags/${tag.slug}`"
          class="sb-tag"
        >
          {{ tag.name }}<em>{{ tag.articleCount }}</em>
        </NuxtLink>
      </div>
      <NuxtLink to="/community/tags" class="sb-more">全部标签 →</NuxtLink>
    </section>

    <section v-if="data?.activeUsers?.length" class="sb-card">
      <h3 class="sb-title">活跃成员</h3>
      <ul class="sb-users">
        <li v-for="u in data.activeUsers" :key="u.id">
          <NuxtLink :to="`/community/users/${u.id}`" class="sb-user">
            <img v-if="u.avatar" :src="u.avatar" :alt="u.nickname" class="sb-avatar" />
            <span v-else class="sb-avatar sb-avatar-text">{{ initials(u.nickname) }}</span>
            <span class="sb-user-name">{{ u.nickname }}</span>
            <span class="sb-user-count">{{ u.commentCount }}</span>
          </NuxtLink>
        </li>
      </ul>
    </section>
  </aside>
</template>

<style scoped>
.sb {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.sb-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  padding: 14px;
}
.sb-title {
  margin: 0 0 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--c-border);
  color: var(--c-text);
  font-size: 14px;
  font-weight: 600;
}
.sb-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px 8px;
  list-style: none;
  margin: 0;
  padding: 0;
}
.sb-stats li {
  display: flex;
  flex-direction: column;
}
.sb-stats strong {
  font-size: 17px;
  font-weight: 600;
  line-height: 1.3;
}
.sb-stats span {
  color: var(--c-text-faint);
  font-size: 12.5px;
}
.sb-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.sb-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border: 1px solid var(--c-border);
  border-radius: 3px;
  background: var(--c-surface-alt);
  color: var(--c-text-muted);
  font-size: 12.5px;
}
.sb-tag:hover {
  border-color: var(--c-accent);
  color: var(--c-accent);
}
.sb-tag em {
  color: var(--c-text-faint);
  font-style: normal;
}
.sb-more {
  display: inline-block;
  margin-top: 10px;
  color: var(--c-accent);
  font-size: 12.5px;
}
.sb-users {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.sb-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 4px;
  border-radius: var(--c-radius);
  color: var(--c-text);
  font-size: 13px;
}
.sb-user:hover {
  background: var(--c-surface-alt);
}
.sb-avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.sb-avatar-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--c-accent-soft);
  color: var(--c-accent);
  font-size: 11px;
  font-weight: 600;
}
.sb-user-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sb-user-count {
  color: var(--c-text-faint);
  font-size: 12px;
}
</style>
