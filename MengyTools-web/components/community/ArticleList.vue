<script setup lang="ts">
/**
 * 社区文章列表（表格式，成熟论坛风格）。
 * 用于：社区首页 / 标签页 / 搜索结果。
 */
import { MessageSquare, Eye } from 'lucide-vue-next'
import type { CommunityArticle } from '~/composables/useCommunity'
import { formatCount, formatRelativeTime } from '~/composables/useCommunity'

defineProps<{ items: CommunityArticle[]; showCategory?: boolean }>()
</script>

<template>
  <ul class="al">
    <li v-for="item in items" :key="item.id" class="al-row">
      <div class="al-main">
        <NuxtLink :to="`/community/posts/${item.id}`" class="al-title">
          <span v-if="item.isTop" class="al-badge al-badge-top">置顶</span>
          {{ item.title }}
        </NuxtLink>
        <p v-if="item.summary" class="al-summary">{{ item.summary }}</p>
        <div class="al-meta">
          <NuxtLink v-if="item.authorId" :to="`/community/users/${item.authorId}`" class="al-author">
            {{ item.authorName || '匿名' }}
          </NuxtLink>
          <span v-else class="al-author">{{ item.authorName || '匿名' }}</span>
          <span v-if="showCategory && item.categoryName" class="al-dot">·</span>
          <span v-if="showCategory && item.categoryName">{{ item.categoryName }}</span>
          <span class="al-dot">·</span>
          <time>{{ formatRelativeTime(item.publishTime || item.createTime) }}</time>
        </div>
      </div>
      <div class="al-stats">
        <span class="al-stat" :title="`${item.commentCount ?? 0} 条评论`">
          <MessageSquare :size="14" />{{ formatCount(item.commentCount) }}
        </span>
        <span class="al-stat" :title="`${item.viewCount ?? 0} 次浏览`">
          <Eye :size="14" />{{ formatCount(item.viewCount) }}
        </span>
      </div>
    </li>
    <li v-if="!items.length" class="al-empty">暂无内容</li>
  </ul>
</template>

<style scoped>
.al {
  list-style: none;
  margin: 0;
  padding: 0;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
}
.al-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--c-border);
}
.al-row:last-child {
  border-bottom: 0;
}
.al-row:hover {
  background: var(--c-surface-alt);
}
.al-main {
  min-width: 0;
  flex: 1;
}
.al-title {
  display: inline-block;
  color: var(--c-text);
  font-size: 15px;
  font-weight: 500;
  line-height: 1.5;
}
.al-title:hover {
  color: var(--c-accent);
}
.al-badge {
  display: inline-block;
  margin-right: 6px;
  padding: 0 5px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 500;
  vertical-align: 1px;
}
.al-badge-top {
  background: var(--c-accent-soft);
  color: var(--c-accent);
}
.al-summary {
  margin: 4px 0 0;
  color: var(--c-text-muted);
  font-size: 13px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.al-meta {
  margin-top: 6px;
  color: var(--c-text-faint);
  font-size: 12.5px;
}
.al-author {
  color: var(--c-text-muted);
}
.al-author:hover {
  color: var(--c-accent);
}
.al-dot {
  margin: 0 4px;
}
.al-stats {
  display: flex;
  gap: 14px;
  flex-shrink: 0;
  padding-top: 2px;
  color: var(--c-text-faint);
  font-size: 12.5px;
}
.al-stat {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 42px;
}
.al-empty {
  padding: 40px 16px;
  text-align: center;
  color: var(--c-text-faint);
  font-size: 14px;
}
</style>
