<script setup lang="ts">
const { data: tools } = await useAsyncData('tools-list', () =>
  useApi().get<Array<{ id: number; name: string; description: string; link: string }>>(
    '/v1/tools'
  )
)

useSeoMeta({
  title: '效率工具 - MengyTools',
  description: '常用效率工具卡片'
})
</script>

<template>
  <div>
    <h1>效率工具</h1>
    <div class="grid">
      <a
        v-for="t in tools ?? []"
        :key="t.id"
        :href="t.link"
        target="_blank"
        rel="noopener"
        class="card tool-card"
      >
        <h3>{{ t.name }}</h3>
        <p>{{ t.description }}</p>
      </a>
    </div>
  </div>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.tool-card {
  display: block;
  color: inherit;
  transition: transform 0.2s;
}
.tool-card:hover {
  transform: translateY(-4px);
}
.tool-card h3 {
  margin: 0 0 6px;
  color: #409eff;
}
.tool-card p {
  margin: 0;
  color: #777;
  font-size: 14px;
}
</style>
