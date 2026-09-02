<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

const engines = [
  { key: 'bing', label: 'Bing', url: 'https://www.bing.com/search?q=' },
  { key: 'google', label: 'Google', url: 'https://www.google.com/search?q=' },
  { key: 'baidu', label: '百度', url: 'https://www.baidu.com/s?wd=' }
]

// 默认 Bing，选择持久化到 localStorage（仅在客户端访问，避免 SSR 报错）
const currentKey = ref('bing')
const current = computed(() => engines.find(e => e.key === currentKey.value) ?? engines[0])

const keyword = ref('')

// 客户端挂载时读取上次选择
onMounted(() => {
  const saved = localStorage.getItem('search-engine')
  if (saved && engines.some(e => e.key === saved)) {
    currentKey.value = saved
  }
})

const selectEngine = (key: string) => {
  currentKey.value = key
  if (import.meta.client) {
    localStorage.setItem('search-engine', key)
  }
}

const handleSearch = () => {
  const kw = keyword.value.trim()
  if (!kw) return
  window.open(current.value.url + encodeURIComponent(kw), '_blank', 'noopener')
}

const handleEnter = () => handleSearch()
</script>

<template>
  <div class="search-box">
    <div class="engine-switch">
      <button
        v-for="e in engines"
        :key="e.key"
        class="engine-btn"
        :class="{ active: e.key === currentKey }"
        @click="selectEngine(e.key)"
      >
        {{ e.label }}
      </button>
    </div>
    <div class="search-input-wrap">
      <input
        v-model="keyword"
        type="text"
        class="search-input"
        :placeholder="`使用 ${current.label} 搜索...`"
        @keyup.enter="handleEnter"
      >
      <button class="search-btn" @click="handleSearch">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="7" />
          <line x1="21" y1="21" x2="16.5" y2="16.5" />
        </svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
.search-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  max-width: 560px;
}

.engine-switch {
  display: flex;
  gap: 4px;
}
.engine-btn {
  padding: 4px 14px;
  font-size: 13px;
  color: var(--engine-text);
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}
.engine-btn:hover {
  color: var(--engine-active-text);
}
.engine-btn.active {
  color: var(--engine-active-text);
  background: var(--engine-active-bg);
  border-color: var(--search-focus-border);
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}
.search-input {
  width: 100%;
  height: 44px;
  padding: 0 56px 0 18px;
  font-size: 15px;
  color: var(--search-text);
  background: var(--search-bg);
  border: 1px solid var(--search-border);
  border-radius: 22px;
  outline: none;
  transition: all 0.25s;
}
.search-input::placeholder {
  color: var(--search-placeholder);
}
.search-input:focus {
  border-color: var(--search-focus-border);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
}
.search-btn {
  position: absolute;
  right: 6px;
  top: 50%;
  transform: translateY(-50%);
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #ec4899);
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.25s;
}
.search-btn:hover {
  transform: translateY(-50%) scale(1.08);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.5);
}
</style>
