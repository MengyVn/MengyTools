<script setup lang="ts">
useSeoMeta({
  title: 'MengyTools - 个人数字资产平台',
  description: '个人博客 · 效率工具 · 网址导航，一站式管理你的数字资产'
})

const sections = [
  { to: '/blog', title: '博客', desc: '技术文章与生活随笔，Markdown 驱动' },
  { to: '/tools', title: '工具', desc: '常用效率工具卡片，即开即用' },
  { to: '/nav', title: '导航', desc: '精选网址导航，多级分类' }
]
</script>

<template>
  <div>
    <section class="hero">
      <h1>MengyTools</h1>
      <p>个人数字资产平台 · 博客 / 工具 / 导航</p>
    </section>

    <section class="grid">
      <NuxtLink v-for="s in sections" :key="s.to" :to="s.to" class="card section-card">
        <h3>{{ s.title }}</h3>
        <p>{{ s.desc }}</p>
      </NuxtLink>
    </section>
  </div>
</template>

<style scoped>
.hero {
  text-align: center;
  padding: 60px 0 40px;
}
.hero h1 {
  font-size: 42px;
  margin: 0 0 12px;
  color: #2c3e50;
}
.hero p {
  color: #666;
  font-size: 18px;
  margin: 0;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 20px;
  margin-top: 20px;
}
.section-card {
  display: block;
  color: inherit;
  transition: transform 0.2s, box-shadow 0.2s;
}
.section-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}
.section-card h3 {
  margin: 0 0 8px;
  color: #409eff;
}
.section-card p {
  margin: 0;
  color: #777;
  font-size: 14px;
}
</style>
