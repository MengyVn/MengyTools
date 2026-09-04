<script setup lang="ts">
import { PenSquare, Wrench, Compass, ArrowRight, FileText } from 'lucide-vue-next'

// Hero 标语轮播：5 句话循环切换，10 分钟切换一次
const heroLines = [
  '一切准备就绪',
  '探索你的数字资产',
  '让创作更高效',
  '聚合博客 · 工具 · 导航',
  '欢迎来到 MengyTools'
]
const heroIndex = ref(0)
let heroTimer: ReturnType<typeof setInterval> | undefined
onMounted(() => {
  heroTimer = setInterval(() => {
    heroIndex.value = (heroIndex.value + 1) % heroLines.length
  }, 600000)
})
onBeforeUnmount(() => heroTimer && clearInterval(heroTimer))

useSeoMeta({
  title: 'MengyTools - 数字资产平台',
  description: '个人博客 · 效率工具 · 网址导航，一站式管理你的数字资产'
})

// 拉最新 5 篇文章用于首页展示
const { data: latestArticles } = await useAsyncData('home-articles', () =>
  useApi().get<Array<{
    id: number
    title: string
    summary: string
    cover: string
    categoryName: string | null
    publishTime: string
  }>>('/v1/blog/articles')
)

const featuredArticles = computed(() => (latestArticles.value ?? []).slice(0, 5))

const sections = [
  { to: '/blog', title: '博客', desc: '技术文章与生活随笔，Markdown 驱动', icon: PenSquare, color: 'linear-gradient(135deg, #6366f1, #818cf8)' },
  { to: '/tools', title: '工具', desc: '常用效率工具卡片，即开即用', icon: Wrench, color: 'linear-gradient(135deg, #ec4899, #f472b6)' },
  { to: '/nav', title: '导航', desc: '精选网址导航，多级分类', icon: Compass, color: 'linear-gradient(135deg, #06b6d4, #22d3ee)' }
]
</script>

<template>
  <div>
    <!-- Hero -->
    <section class="hero">
      <div class="hero-blob hero-blob-1" aria-hidden="true"></div>
      <div class="hero-blob hero-blob-2" aria-hidden="true"></div>
      <div class="container hero-inner">
        <h1 class="hero-title animate-fade-up">
          <Transition name="hero-line" mode="out-in">
            <span :key="heroIndex" class="hero-line-text">{{ heroLines[heroIndex] }}</span>
          </Transition>
        </h1>
        <p class="hero-desc animate-fade-up delay-1">个人数字资产平台 · 博客 / 工具 / 导航</p>
        <div class="hero-search animate-fade-up delay-2">
          <SearchBox />
        </div>
        <div class="hero-actions animate-fade-up delay-3">
          <NuxtLink to="/blog" class="btn btn-primary">浏览博客</NuxtLink>
          <NuxtLink to="/nav" class="btn btn-ghost">网址导航</NuxtLink>
        </div>
      </div>
    </section>

    <!-- 模块入口 -->
    <section class="container sections">
      <NuxtLink
        v-for="(s, i) in sections"
        :key="s.to"
        v-reveal="{ delay: i * 90 }"
        :to="s.to"
        class="card section-card"
      >
        <div class="section-icon" :style="{ background: s.color }"><component :is="s.icon" :size="22" color="#fff" /></div>
        <h3>{{ s.title }}</h3>
        <p>{{ s.desc }}</p>
        <span class="section-arrow"><ArrowRight :size="16" /></span>
      </NuxtLink>
    </section>

    <!-- 最新文章 -->
    <section class="container latest">
      <div class="section-head" v-reveal>
        <h2 class="section-title">最新文章</h2>
        <NuxtLink to="/blog" class="more-link">查看全部 <ArrowRight :size="14" /></NuxtLink>
      </div>
      <div v-if="featuredArticles.length" class="article-list">
        <NuxtLink
          v-for="(a, i) in featuredArticles"
          :key="a.id"
          v-reveal="{ delay: i * 70 }"
          :to="`/blog/${a.id}`"
          class="card article-item"
        >
          <div v-if="a.cover" class="zoom-wrap article-thumb-wrap">
            <img
              :src="a.cover"
              :alt="a.title"
              class="article-thumb"
              loading="lazy"
            >
          </div>
          <div class="article-body">
            <div class="article-meta">
              <span v-if="a.categoryName" class="tag">{{ a.categoryName }}</span>
              <span class="time">{{ a.publishTime?.slice(0, 10) }}</span>
            </div>
            <h3 class="article-title">{{ a.title }}</h3>
            <p class="article-summary">{{ a.summary || '暂无摘要' }}</p>
          </div>
        </NuxtLink>
      </div>
      <div v-else class="empty-state card" v-reveal>
        <FileText :size="40" class="empty-icon" />
        <p>还没有文章，敬请期待</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* Hero */
.hero {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(ellipse at top, rgba(99, 102, 241, 0.15), transparent 60%),
    radial-gradient(ellipse at bottom right, rgba(236, 72, 153, 0.1), transparent 50%),
    var(--bg-page);
  padding: 80px 0 60px;
  margin-bottom: 40px;
}
.hero-inner {
  position: relative;
  text-align: center;
  z-index: 1;
}
.hero-title {
  font-size: 42px;
  font-weight: 800;
  margin: 0 0 16px;
  letter-spacing: -0.5px;
  /* 为轮播过渡预留高度，避免切换时跳动 */
  min-height: 1.3em;
  display: flex;
  align-items: center;
  justify-content: center;
}
/* 标语轮播：淡入上移 / 淡出下移 */
.hero-line-text {
  display: inline-block;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.hero-line-enter-active,
.hero-line-leave-active {
  transition: opacity 0.45s var(--ease-out), transform 0.45s var(--ease-out);
}
.hero-line-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.hero-line-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}
.hero-desc {
  font-size: 18px;
  color: var(--text-secondary);
  margin: 0 0 28px;
}
.hero-search {
  display: flex;
  justify-content: center;
  margin: 0 auto 28px;
}
.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}
.btn {
  display: inline-flex;
  align-items: center;
  padding: 12px 28px;
  border-radius: var(--radius);
  font-weight: 500;
  font-size: 15px;
  transition: all 0.25s;
}
.btn-primary {
  background: linear-gradient(135deg, var(--primary), var(--accent));
  background-size: 200% 200%;
  color: #fff;
  box-shadow: var(--shadow-primary);
}
.btn-primary:hover {
  background-position: 100% 100%;
  transform: translateY(-2px);
  color: #fff;
}
.btn-ghost {
  background: var(--bg-color);
  border: 1px solid var(--border-color);
  color: var(--text-color);
}
.btn-ghost:hover {
  border-color: var(--primary);
  color: var(--primary);
}

/* 模块入口 */
.sections {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  margin-bottom: 56px;
}
.section-card {
  position: relative;
  display: block;
  color: inherit;
  padding: 28px 24px;
}
.section-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: transparent;
}
.section-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
}
.section-card h3 {
  margin: 0 0 6px;
  font-size: 18px;
}
.section-card p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}
.section-arrow {
  position: absolute;
  top: 28px;
  right: 24px;
  color: var(--text-tertiary);
  display: inline-flex;
  transition: transform 0.25s, color 0.25s;
}
.section-card:hover .section-arrow {
  transform: translateX(4px);
  color: var(--primary);
}

/* 最新文章 */
.latest {
  margin-bottom: 40px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.section-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}
.more-link {
  font-size: 14px;
  font-weight: 500;
}
.article-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
.article-item {
  display: flex;
  gap: 14px;
  color: inherit;
}
/* 封面缩略图：外层裁剪容器 + 内层图片 hover 缩放 */
.article-thumb-wrap {
  width: 116px;
  height: 84px;
  flex-shrink: 0;
  align-self: center;
}
.article-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.article-item:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow);
  border-color: var(--primary-light);
}
.article-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 12px;
}
.tag {
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(99, 102, 241, 0.1);
  color: var(--primary);
  font-weight: 500;
}
.time {
  color: var(--text-tertiary);
}
.article-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.article-summary {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

@media (max-width: 640px) {
  .hero-title { font-size: 30px; }
  .hero-desc { font-size: 15px; }
  .hero { padding: 50px 0 40px; }
}
</style>
