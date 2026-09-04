<script setup lang="ts">
import { Wrench, ArrowUpRight } from 'lucide-vue-next'
const { data: tools } = await useAsyncData('tools-list', () =>
  useApi().get<Array<{
    id: number
    name: string
    description: string
    link: string
    componentRoute: string
    icon: string
  }>>('/v1/tools')
)

useSeoMeta({
  title: '效率工具 - MengyTools',
  description: '常用效率工具卡片'
})

const list = computed(() => tools.value ?? [])
</script>

<template>
  <div class="container">
    <h1 class="page-title animate-fade-up">效率工具</h1>
    <p class="page-subtitle animate-fade-up delay-1">常用工具卡片 · 即开即用</p>

    <div v-if="list.length" class="grid">
      <a
        v-for="(t, i) in list"
        :key="t.id"
        v-reveal="{ delay: Math.min(i, 8) * 50 }"
        :href="t.link || t.componentRoute"
        target="_blank"
        rel="noopener noreferrer"
        class="card tool-card"
      >
        <div class="tool-icon"><Wrench :size="22" /></div>
        <div class="tool-body">
          <h3>{{ t.name }}</h3>
          <p>{{ t.description || '暂无描述' }}</p>
        </div>
        <span class="tool-arrow"><ArrowUpRight :size="16" /></span>
      </a>
    </div>
    <div v-else class="empty-state card">
      <Wrench :size="40" class="empty-icon" />
      <p>暂无工具</p>
    </div>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
}
.tool-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 22px;
  color: inherit;
  position: relative;
}
.tool-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary-light);
}
.tool-icon {
  font-size: 28px;
  line-height: 1;
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1), rgba(236, 72, 153, 0.1));
}
.tool-body {
  flex: 1;
  min-width: 0;
}
.tool-body h3 {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
}
.tool-body p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.tool-arrow {
  position: absolute;
  top: 22px;
  right: 22px;
  color: var(--text-tertiary);
  display: inline-flex;
  opacity: 0;
  transition: opacity 0.2s, transform 0.2s;
}
.tool-card:hover .tool-arrow {
  opacity: 1;
  transform: translate(2px, -2px);
  color: var(--primary);
}
</style>
