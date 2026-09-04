<script setup lang="ts">
import { Search, ArrowUpRight, Compass } from 'lucide-vue-next'
const { data: categories } = await useAsyncData('nav-list', () =>
  useApi().get<Array<{
    id: number
    name: string
    icon: string
    sort: number
    sites: Array<{
      id: number
      name: string
      url: string
      description: string
      icon: string
    }>
  }>>('/v1/nav')
)

useSeoMeta({
  title: '网址导航 - MengyTools',
  description: '精选网址导航，多级分类'
})

// 客户端搜索（仅过滤已渲染数据）
const keyword = ref('')
const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return categories.value ?? []
  return (categories.value ?? [])
    .map(c => ({
      ...c,
      sites: c.sites.filter(s =>
        s.name.toLowerCase().includes(kw) ||
        s.url.toLowerCase().includes(kw) ||
        (s.description || '').toLowerCase().includes(kw)
      )
    }))
    .filter(c => c.sites.length > 0)
})

// 取站点首字母/中文首字作为图标占位
const initial = (name: string) => (name || '?').charAt(0)
</script>

<template>
  <div class="container">
    <h1 class="page-title animate-fade-up">网址导航</h1>
    <p class="page-subtitle animate-fade-up delay-1">精选网址 · 多级分类</p>

    <div class="search-bar animate-fade-up delay-2">
      <Search :size="16" class="search-icon" />
      <input
        v-model="keyword"
        type="text"
        placeholder="搜索站点名称、网址或描述..."
        class="search-input"
      >
    </div>

    <div v-if="filtered.length">
      <section v-for="(cat, ci) in filtered" :key="cat.id" v-reveal="{ delay: Math.min(ci, 6) * 80 }" class="cat-block">
        <h2 class="cat-title">
          <span class="cat-bar"></span>
          {{ cat.name }}
          <span class="cat-count">{{ cat.sites.length }}</span>
        </h2>
        <div class="sites">
          <a
            v-for="site in cat.sites"
            :key="site.id"
            :href="site.url"
            target="_blank"
            rel="noopener noreferrer"
            class="card site-card"
          >
            <div class="site-icon" :style="{ background: cat.color || 'linear-gradient(135deg, #6366f1, #818cf8)' }">
              {{ initial(site.name) }}
            </div>
            <div class="site-info">
              <span class="site-name">{{ site.name }}</span>
              <span class="site-desc">{{ site.description || site.url }}</span>
            </div>
            <span class="site-arrow"><ArrowUpRight :size="16" /></span>
          </a>
        </div>
      </section>
    </div>
    <div v-else class="empty-state card">
      <Compass :size="40" class="empty-icon" />
      <p>{{ keyword ? '没有匹配的站点' : '暂无导航数据' }}</p>
    </div>
  </div>
</template>

<style scoped>
.search-bar {
  position: relative;
  margin-bottom: 32px;
}
.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-tertiary);
  pointer-events: none;
}
.search-input {
  width: 100%;
  padding: 12px 18px 12px 42px;
  font-size: 15px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  background: var(--bg-color);
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.search-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.cat-block {
  margin-bottom: 40px;
}
.cat-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 18px;
}
.cat-bar {
  width: 4px;
  height: 18px;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  border-radius: 2px;
}
.cat-count {
  padding: 1px 8px;
  border-radius: 10px;
  background: var(--bg-hover);
  color: var(--text-tertiary);
  font-size: 12px;
  font-weight: 500;
}

.sites {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}
.site-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  color: inherit;
  position: relative;
}
.site-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow);
  border-color: var(--primary-light);
}
.site-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
  font-size: 16px;
  flex-shrink: 0;
}
.site-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}
.site-name {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.site-desc {
  font-size: 12px;
  color: var(--text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.site-arrow {
  color: var(--text-tertiary);
  display: inline-flex;
  opacity: 0;
  transition: opacity 0.2s, transform 0.2s;
}
.site-card:hover .site-arrow {
  opacity: 1;
  transform: translate(2px, -2px);
}
</style>
