<script setup lang="ts">
useSeoMeta({
  title: 'MengyTools - 个人数字资产平台',
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
  { to: '/blog', title: '博客', desc: '技术文章与生活随笔，Markdown 驱动', icon: '✍️', color: 'linear-gradient(135deg, #6366f1, #818cf8)' },
  { to: '/tools', title: '工具', desc: '常用效率工具卡片，即开即用', icon: '🛠️', color: 'linear-gradient(135deg, #ec4899, #f472b6)' },
  { to: '/nav', title: '导航', desc: '精选网址导航，多级分类', icon: '🧭', color: 'linear-gradient(135deg, #06b6d4, #22d3ee)' }
]
</script>

<template>
  <div>
    <!-- Hero -->
    <section class="hero">
      <div class="container hero-inner">
        <h1 class="hero-title">
          欢迎来到 <span class="gradient-text">MengyTools</span>
        </h1>
        <p class="hero-desc">个人数字资产平台 · 博客 / 工具 / 导航</p>
        <div class="hero-search">
          <SearchBox />
        </div>
        <div class="hero-actions">
          <NuxtLink to="/blog" class="btn btn-primary">浏览博客</NuxtLink>
          <NuxtLink to="/nav" class="btn btn-ghost">网址导航</NuxtLink>
        </div>
      </div>
    </section>

    <!-- 模块入口 -->
    <section class="container sections">
      <NuxtLink
        v-for="s in sections"
        :key="s.to"
        :to="s.to"
        class="card section-card"
      >
        <div class="section-icon" :style="{ background: s.color }">{{ s.icon }}</div>
        <h3>{{ s.title }}</h3>
        <p>{{ s.desc }}</p>
        <span class="section-arrow">→</span>
      </NuxtLink>
    </section>

    <!-- 最新文章 -->
    <section class="container latest">
      <div class="section-head">
        <h2 class="section-title">最新文章</h2>
        <NuxtLink to="/blog" class="more-link">查看全部 →</NuxtLink>
      </div>
      <div v-if="featuredArticles.length" class="article-list">
        <NuxtLink
          v-for="a in featuredArticles"
          :key="a.id"
          :to="`/blog/${a.id}`"
          class="card article-item"
        >
          <img
            v-if="a.cover"
            :src="a.cover"
            :alt="a.title"
            class="article-thumb"
            loading="lazy"
          />
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
      <div v-else class="empty-state card">
        <div class="emoji">📝</div>
        <p>还没有文章，敬请期待</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* Hero */
.hero {
  background:
    radial-gradient(ellipse at top, rgba(99, 102, 241, 0.15), transparent 60%),
    radial-gradient(ellipse at bottom right, rgba(236, 72, 153, 0.1), transparent 50%),
    var(--bg-page);
  padding: 80px 0 60px;
  margin-bottom: 40px;
}
.hero-inner {
  text-align: center;
}
.hero-title {
  font-size: 42px;
  font-weight: 800;
  margin: 0 0 16px;
  letter-spacing: -0.5px;
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
  font-size: 22px;
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
  font-size: 18px;
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
/* 封面缩略图 */
.article-thumb {
  width: 116px;
  height: 84px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
  align-self: center;
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
