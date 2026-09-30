<script setup lang="ts">
/**
 * 社区侧栏：站点统计 + 最近公告 + 板块 + 活跃用户。
 * 统计数据来自单次聚合接口 /v1/community/sidebar；公告单独取一次（同一请求内按 key 缓存）。
 */
import { useCommunity, formatCount } from '~/composables/useCommunity'
import { useAnnouncement, type AnnouncementItem } from '~/composables/useAnnouncement'
import { imageUrl } from '~/utils/image'

const { sidebar } = useCommunity()
const { fetchActive } = useAnnouncement()

// key 固定：同一请求内多页复用，SSR 只取一次
const { data } = await useAsyncData('community-sidebar', () => sidebar())

/** 最近公告：取当前生效的前 4 条（与门户公告同源） */
const { data: notices } = await useAsyncData('community-sidebar-notices', async () => {
  try {
    const list = await fetchActive()
    return (Array.isArray(list) ? list : []).slice(0, 4) as AnnouncementItem[]
  } catch {
    return [] as AnnouncementItem[]
  }
})

const noticeDate = (it: AnnouncementItem) => {
  const t = it.publishTime || it.createTime
  return t ? String(t).replace('T', ' ').slice(5, 16) : ''
}

const stats = computed(() => {
  const d = data.value
  return [
    { label: '文章', value: formatCount(d?.articleCount) },
    { label: '评论', value: formatCount(d?.commentCount) },
    { label: '成员', value: formatCount(d?.userCount) },

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

    <section v-if="notices?.length" class="sb-card">
      <h3 class="sb-title">公告</h3>
      <ul class="sb-notices">
        <li v-for="n in notices" :key="n.id">
          <NuxtLink :to="`/community/announcements/${n.id}`" class="sb-notice">
            <span class="sb-notice-title">{{ n.title }}</span>
            <span class="sb-notice-time">{{ noticeDate(n) }}</span>
          </NuxtLink>
        </li>
      </ul>
      <NuxtLink to="/community/announcements" class="sb-more">全部公告 →</NuxtLink>
    </section>

    <section v-if="data?.boards?.length" class="sb-card">
      <h3 class="sb-title">板块</h3>
      <ul class="sb-boards">
        <li v-for="b in data.boards" :key="b.id">
          <NuxtLink :to="`/community?category=${b.id}`" class="sb-board">
            <span class="sb-board-name">{{ b.name }}</span>
            <span class="sb-board-count">{{ b.postCount }}</span>
          </NuxtLink>
        </li>
      </ul>
      <NuxtLink to="/community/boards" class="sb-more">全部板块 →</NuxtLink>
    </section>

    <section v-if="data?.activeUsers?.length" class="sb-card">
      <h3 class="sb-title">活跃成员</h3>
      <ul class="sb-users">
        <li v-for="u in data.activeUsers" :key="u.id">
          <NuxtLink :to="`/community/users/${u.id}`" class="sb-user">
            <img v-if="u.avatar" :src="imageUrl(u.avatar)" :alt="u.nickname" class="sb-avatar" />
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
.sb-boards {
  list-style: none;
  margin: 0;
  padding: 0;
}
.sb-board {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 5px 4px;
  border-radius: var(--c-radius);
  color: var(--c-text-muted);
  font-size: 13.5px;
}
.sb-board:hover {
  background: var(--c-surface-alt);
  color: var(--c-accent);
}
.sb-board-count {
  color: var(--c-text-faint);
  font-size: 12px;
}
.sb-more {
  display: inline-block;
  margin-top: 10px;
  color: var(--c-accent);
  font-size: 12.5px;
}
.sb-notices {
  list-style: none;
  margin: 0;
  padding: 0;
}
.sb-notice {
  display: block;
  padding: 6px 4px;
  border-radius: var(--c-radius);
}
.sb-notice:hover {
  background: var(--c-surface-alt);
}
.sb-notice-title {
  display: block;
  color: var(--c-text);
  font-size: 13.5px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.sb-notice:hover .sb-notice-title {
  color: var(--c-accent);
}
.sb-notice-time {
  display: block;
  margin-top: 2px;
  color: var(--c-text-faint);
  font-size: 12px;
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
