<script setup lang="ts">
/**
 * 分页器（论坛式：上一页 / 页码 / 下一页 + 总数）。
 * 只负责派发页码，由页面自行写回路由 query（保证 SSR 与可分享 URL）。
 */
const props = defineProps<{ current: number; pages: number; total?: number }>()
const emit = defineEmits<{ (e: 'change', page: number): void }>()

const visiblePages = computed(() => {
  const total = Math.max(props.pages, 1)
  const cur = props.current
  const out: number[] = []
  const start = Math.max(1, Math.min(cur - 2, total - 4))
  const end = Math.min(total, start + 4)
  for (let i = start; i <= end; i++) out.push(i)
  return out
})

const go = (page: number) => {
  if (page < 1 || page > props.pages || page === props.current) return
  emit('change', page)
}
</script>

<template>
  <nav v-if="pages > 1" class="pager" aria-label="分页">
    <button type="button" class="pager-btn" :disabled="current <= 1" @click="go(current - 1)">
      上一页
    </button>
    <button
      v-for="p in visiblePages"
      :key="p"
      type="button"
      class="pager-btn"
      :class="{ active: p === current }"
      @click="go(p)"
    >
      {{ p }}
    </button>
    <button type="button" class="pager-btn" :disabled="current >= pages" @click="go(current + 1)">
      下一页
    </button>
    <span v-if="total !== undefined" class="pager-total">共 {{ total }} 条 / {{ pages }} 页</span>
  </nav>
</template>

<style scoped>
.pager {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 16px;
  flex-wrap: wrap;
}
.pager-btn {
  min-width: 32px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text-muted);
  font-size: 13px;
  cursor: pointer;
}
.pager-btn:hover:not(:disabled) {
  border-color: var(--c-accent);
  color: var(--c-accent);
}
.pager-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.pager-btn.active {
  border-color: var(--c-accent);
  background: var(--c-accent);
  color: #fff;
}
.pager-total {
  margin-left: 6px;
  color: var(--c-text-faint);
  font-size: 12.5px;
}
</style>
