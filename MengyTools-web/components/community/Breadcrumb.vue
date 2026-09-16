<script setup lang="ts">
/** 面包屑：成熟站点的基本导航元素。 */
defineProps<{ items: Array<{ label: string; to?: string }> }>()
</script>

<template>
  <nav class="bc" aria-label="面包屑">
    <template v-for="(item, i) in items" :key="i">
      <NuxtLink v-if="item.to && i < items.length - 1" :to="item.to" class="bc-link">
        {{ item.label }}
      </NuxtLink>
      <span v-else class="bc-current">{{ item.label }}</span>
      <span v-if="i < items.length - 1" class="bc-sep">/</span>
    </template>
  </nav>
</template>

<style scoped>
.bc {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  color: var(--c-text-faint);
  font-size: 13px;
  flex-wrap: wrap;
}
.bc-link {
  color: var(--c-text-muted);
}
.bc-link:hover {
  color: var(--c-accent);
}
.bc-current {
  color: var(--c-text);
}
.bc-sep {
  color: var(--c-border-strong);
}
</style>
