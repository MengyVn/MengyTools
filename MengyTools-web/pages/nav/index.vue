<script setup lang="ts">
const { data: categories } = await useAsyncData('nav-list', () =>
  useApi().get<
    Array<{
      id: number
      name: string
      sites: Array<{ id: number; name: string; url: string; icon: string }>
    }>
  >('/v1/nav')
)

useSeoMeta({
  title: '网址导航 - MengyTools',
  description: '精选网址导航'
})
</script>

<template>
  <div>
    <h1>网址导航</h1>
    <section v-for="cat in categories ?? []" :key="cat.id" class="cat-block">
      <h2>{{ cat.name }}</h2>
      <div class="sites">
        <a
          v-for="site in cat.sites"
          :key="site.id"
          :href="site.url"
          target="_blank"
          rel="noopener"
          class="card site-card"
        >
          <span class="icon">{{ site.name.charAt(0) }}</span>
          <span class="name">{{ site.name }}</span>
        </a>
      </div>
    </section>
  </div>
</template>

<style scoped>
.cat-block {
  margin-bottom: 32px;
}
.cat-block h2 {
  font-size: 20px;
  border-left: 4px solid #409eff;
  padding-left: 10px;
  margin-bottom: 16px;
}
.sites {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}
.site-card {
  display: flex;
  align-items: center;
  gap: 10px;
  color: inherit;
}
.site-card:hover {
  border-color: #409eff;
}
.icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
}
.name {
  font-size: 14px;
}
</style>
