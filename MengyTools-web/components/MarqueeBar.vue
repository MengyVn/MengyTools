<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { X, Megaphone } from 'lucide-vue-next'
import { useAnnouncement, type AnnouncementItem } from '~/composables/useAnnouncement'

const announcement = useAnnouncement()
const router = useRouter()

const items = ref<AnnouncementItem[]>([])
const hiddenIds = ref<Set<number>>(new Set())

/** 关闭态存储 key 前缀 */
const CLOSED_KEY_PREFIX = 'marquee-closed-'

/** 根据 is_persistent 决定存储方式：
 *  is_persistent=1(常驻)：存 sessionStorage（会话级，每次登录/新标签都重新弹出）
 *  is_persistent=0(非常驻)：存 localStorage（持久，跨登录保持关闭）
 */
const getStorage = (isPersistent: number): Storage | null => {
  if (!import.meta.client) return null
  return isPersistent === 1 ? sessionStorage : localStorage
}

/** 读取某条公告的关闭态 */
const isClosed = (item: AnnouncementItem): boolean => {
  return hiddenIds.value.has(item.id)
}

/** 关闭单条公告：写入对应存储 */
const closeItem = (item: AnnouncementItem) => {
  const storage = getStorage(item.isPersistent)
  if (storage) {
    storage.setItem(CLOSED_KEY_PREFIX + item.id, '1')
  }
  hiddenIds.value.add(item.id)
}

/** 加载所有跑马灯公告的关闭态 */
const loadClosedStates = () => {
  hiddenIds.value = new Set()
  if (!import.meta.client) return
  // 扫描 sessionStorage 和 localStorage 中的关闭标记
  const scan = (storage: Storage) => {
    for (let i = 0; i < storage.length; i++) {
      const key = storage.key(i)
      if (key && key.startsWith(CLOSED_KEY_PREFIX)) {
        const idStr = key.slice(CLOSED_KEY_PREFIX.length)
        const id = Number(idStr)
        if (Number.isFinite(id)) {
          hiddenIds.value.add(id)
        }
      }
    }
  }
  scan(sessionStorage)
  scan(localStorage)
}

/** display_duration > 0 时，到时自动隐藏 */
const autoHideTimers = new Map<number, ReturnType<typeof setTimeout>>()

const setupAutoHide = (item: AnnouncementItem) => {
  if (item.displayDuration && item.displayDuration > 0 && !isClosed(item)) {
    const ms = item.displayDuration * 60 * 1000
    const timer = setTimeout(() => {
      closeItem(item)
      // 从可见列表中移除
      visibleItems.value = visibleItems.value.filter(i => i.id !== item.id)
    }, ms)
    autoHideTimers.set(item.id, timer)
  }
}

const clearAutoHideTimers = () => {
  autoHideTimers.forEach(t => clearTimeout(t))
  autoHideTimers.clear()
}

/** 过滤掉已关闭的公告 */
const visibleItems = computed(() => items.value.filter(i => !isClosed(i)))

/** 当前显示的索引（用于轮播多条公告） */
const currentIndex = ref(0)

const currentItem = computed(() => {
  const list = visibleItems.value
  if (!list.length) return null
  return list[currentIndex.value % list.length]
})

let rotateTimer: ReturnType<typeof setInterval> | null = null

const goDetail = (id: number) => {
  router.push(`/announcement/${id}`)
}

const handleClose = (e: Event) => {
  e.stopPropagation()
  if (currentItem.value) {
    closeItem(currentItem.value)
  }
}

const loadMarquee = async () => {
  try {
    const list = await announcement.fetchMarquee()
    items.value = Array.isArray(list) ? list : []
    loadClosedStates()
    // 为可见公告设置自动隐藏
    visibleItems.value.forEach(item => setupAutoHide(item))
    // 多条公告轮播（每条展示 8 秒）
    if (visibleItems.value.length > 1 && !rotateTimer) {
      rotateTimer = setInterval(() => {
        if (visibleItems.value.length > 1) {
          currentIndex.value = (currentIndex.value + 1) % visibleItems.value.length
        }
      }, 8000)
    }
  } catch {
    // 静默失败，跑马灯不阻塞页面
  }
}

onMounted(() => {
  loadMarquee()
})

onBeforeUnmount(() => {
  clearAutoHideTimers()
  if (rotateTimer) {
    clearInterval(rotateTimer)
    rotateTimer = null
  }
})
</script>

<template>
  <div v-if="currentItem" class="marquee-bar">
    <div class="marquee-inner">
      <span class="marquee-icon"><Megaphone :size="14" /></span>
      <div class="marquee-track" @click="goDetail(currentItem!.id)">
        <transition name="marquee-fade" mode="out-in">
          <span :key="currentItem.id" class="marquee-text">{{ currentItem.title }}</span>
        </transition>
      </div>
      <button class="marquee-close" type="button" title="关闭" @click="handleClose">
        <X :size="14" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.marquee-bar {
  background: linear-gradient(90deg, var(--primary, #6366f1), var(--accent, #8b5cf6));
  color: #fff;
  font-size: 13px;
  line-height: 1;
  overflow: hidden;
  flex-shrink: 0;
}

.marquee-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  max-width: var(--container-max, 1200px);
  margin: 0 auto;
}

.marquee-icon {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  opacity: 0.9;
}

.marquee-track {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  cursor: pointer;
  font-weight: 500;
}

.marquee-text {
  display: inline-block;
}

.marquee-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s;
}

.marquee-close:hover {
  background: rgba(255, 255, 255, 0.4);
}

/* 轮播淡入淡出 */
.marquee-fade-enter-active {
  transition: opacity 0.3s ease;
}
.marquee-fade-leave-active {
  transition: opacity 0.2s ease;
}
.marquee-fade-enter-from,
.marquee-fade-leave-to {
  opacity: 0;
}

@media (max-width: 640px) {
  .marquee-inner {
    padding: 8px 12px;
  }
}
</style>
