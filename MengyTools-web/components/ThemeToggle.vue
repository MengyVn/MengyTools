<script setup lang="ts">
import { useTheme, type ThemeMode } from '~/composables/useTheme'

const { mode, setTheme } = useTheme()

const options: Array<{ key: ThemeMode; icon: string; label: string }> = [
  { key: 'light', icon: '☀️', label: '浅色' },
  { key: 'dark', icon: '🌙', label: '暗色' },
  { key: 'system', icon: '💻', label: '跟随系统' }
]
</script>

<template>
  <div class="theme-toggle" :title="`当前：${options.find(o => o.key === mode)?.label}`">
    <button
      v-for="o in options"
      :key="o.key"
      class="toggle-btn"
      :class="{ active: mode === o.key }"
      :title="o.label"
      @click="setTheme(o.key)"
    >
      <span class="icon">{{ o.icon }}</span>
    </button>
  </div>
</template>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 3px;
  background: var(--bg-hover);
  border: 1px solid var(--border-color);
  border-radius: 20px;
}
.toggle-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
  line-height: 1;
  transition: all 0.2s;
}
.toggle-btn:hover {
  background: var(--bg-color);
}
.toggle-btn.active {
  background: var(--bg-color);
  box-shadow: var(--shadow-sm);
}
</style>
