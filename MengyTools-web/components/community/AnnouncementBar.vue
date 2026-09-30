<script setup lang="ts">
/**
 * 社区顶部公告条。
 *
 * 只展示后台勾了「跑马灯」的公告（is_marquee=1），与门户 MarqueeBar 行为一致：
 *   · 关闭状态与门户共用同一套 storage key，在任一端关掉，另一端也不再打扰；
 *   · is_persistent=1 存 sessionStorage（每次会话重新提醒），否则存 localStorage；
 *   · display_duration > 0 到时自动消失；多条公告每 8 秒轮换。
 *
 * 样式刻意不用门户的渐变底色：社区布局只保留「中性灰阶 + 单一强调色」，
 * 这里用强调色浅底 + 左侧竖条表达「公告」，暗色主题由令牌自动适配。
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Megaphone, X } from 'lucide-vue-next'
import { useAnnouncement, type AnnouncementItem } from '~/composables/useAnnouncement'

const announcement = useAnnouncement()

const items = ref<AnnouncementItem[]>([])
const hiddenIds = ref<Set<number>>(new Set())
const currentIndex = ref(0)

/** 与门户 MarqueeBar 保持一致的关闭态 key */
const CLOSED_KEY_PREFIX = 'marquee-closed-'

const getStorage = (isPersistent: number): Storage | null => {
  if (!import.meta.client) return null
  return isPersistent === 1 ? sessionStorage : localStorage
}

const isClosed = (item: AnnouncementItem) => hiddenIds.value.has(item.id)

const closeItem = (item: AnnouncementItem) => {
  const storage = getStorage(item.isPersistent)
  storage?.setItem(CLOSED_KEY_PREFIX + item.id, '1')
  hiddenIds.value = new Set([...hiddenIds.value, item.id])
}

const loadClosedStates = () => {
  const ids = new Set<number>()
  if (!import.meta.client) {
    hiddenIds.value = ids
    return
  }
  const scan = (storage: Storage) => {
    for (let i = 0; i < storage.length; i++) {
      const key = storage.key(i)
      if (key && key.startsWith(CLOSED_KEY_PREFIX)) {
        const id = Number(key.slice(CLOSED_KEY_PREFIX.length))
        if (Number.isFinite(id)) ids.add(id)
      }
    }
  }
  scan(sessionStorage)
  scan(localStorage)
  hiddenIds.value = ids
}

const autoHideTimers = new Map<number, ReturnType<typeof setTimeout>>()

const visibleItems = computed(() => items.value.filter((i) => !isClosed(i)))

const currentItem = computed(() => {
  const list = visibleItems.value
  if (!list.length) return null
  return list[currentIndex.value % list.length]
})

let rotateTimer: ReturnType<typeof setInterval> | null = null

const startRotate = () => {
  if (rotateTimer || visibleItems.value.length <= 1) return
  rotateTimer = setInterval(() => {
    if (visibleItems.value.length > 1) {
      currentIndex.value = (currentIndex.value + 1) % visibleItems.value.length
    }
  }, 8000)
}

/** display_duration 分钟到时自动关闭（并标记，避免刷新后又出现） */
const setupAutoHide = (item: AnnouncementItem) => {
  if (!item.displayDuration || item.displayDuration <= 0 || isClosed(item)) return
  autoHideTimers.set(
    item.id,
    setTimeout(() => closeItem(item), item.displayDuration * 60 * 1000)
  )
}

const clearTimers = () => {
  autoHideTimers.forEach((t) => clearTimeout(t))
  autoHideTimers.clear()
  if (rotateTimer) {
    clearInterval(rotateTimer)
    rotateTimer = null
  }
}

onMounted(async () => {
  try {
    const list = await announcement.fetchMarquee()
    items.value = Array.isArray(list) ? list : []
  } catch {
    return // 拉不到就不显示，绝不阻塞页面
  }
  loadClosedStates()
  visibleItems.value.forEach(setupAutoHide)
  startRotate()
})

onBeforeUnmount(clearTimers)
</script>

<template>
  <div v-if="currentItem" class="c-notice">
    <div class="c-container c-notice-inner">
      <Megaphone :size="15" class="c-notice-icon" />
      <NuxtLink :to="`/community/announcements/${currentItem.id}`" class="c-notice-track">
        <span class="c-notice-tag">公告</span>
        <span :key="currentItem.id" class="c-notice-text">{{ currentItem.title }}</span>
      </NuxtLink>
      <NuxtLink to="/community/announcements" class="c-notice-all">全部公告</NuxtLink>
      <button type="button" class="c-notice-close" title="关闭" @click="closeItem(currentItem)">
        <X :size="14" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.c-notice {
  background: var(--c-accent-soft);
  border-bottom: 1px solid var(--c-border);
  flex-shrink: 0;
}
.c-notice-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
}
.c-notice-icon {
  flex-shrink: 0;
  color: var(--c-accent);
}
.c-notice-track {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
  color: var(--c-text);
  font-size: 13px;
}
.c-notice-track:hover .c-notice-text {
  color: var(--c-accent);
}
.c-notice-tag {
  flex-shrink: 0;
  padding: 0 6px;
  border: 1px solid var(--c-accent);
  border-radius: 3px;
  color: var(--c-accent);
  font-size: 11.5px;
  line-height: 17px;
}
.c-notice-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.c-notice-all {
  flex-shrink: 0;
  color: var(--c-text-muted);
  font-size: 12.5px;
}
.c-notice-all:hover {
  color: var(--c-accent);
}
.c-notice-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--c-text-faint);
  cursor: pointer;
  flex-shrink: 0;
}
.c-notice-close:hover {
  background: var(--c-surface);
  color: var(--c-text);
}
@media (max-width: 640px) {
  .c-notice-all {
    display: none;
  }
}
</style>
