import { ref, onMounted, onBeforeUnmount } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'system'
type Resolved = 'light' | 'dark'

// 全局共享状态（模块级单例，多个组件实例共用同一份）
const mode = ref<ThemeMode>('system')
const resolved = ref<Resolved>('light')
let mediaQuery: MediaQueryList | null = null
let listener: (() => void) | null = null
let inited = false

function resolveTheme(m: ThemeMode): Resolved {
  if (m === 'system') {
    return mediaQuery?.matches ? 'dark' : 'light'
  }
  return m
}

function apply(m: ThemeMode) {
  const actual = resolveTheme(m)
  resolved.value = actual
  if (import.meta.client) {
    document.documentElement.setAttribute('data-theme', actual)
  }
}

export function useTheme() {
  const setTheme = (m: ThemeMode) => {
    mode.value = m
    if (import.meta.client) {
      localStorage.setItem('theme', m)
      apply(m)
    }
  }

  onMounted(() => {
    if (!inited) {
      inited = true
      mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      // 读取上次选择
      const saved = localStorage.getItem('theme') as ThemeMode | null
      if (saved === 'light' || saved === 'dark' || saved === 'system') {
        mode.value = saved
      }
      apply(mode.value)
      // 监听系统主题变化（仅 system 模式下生效）
      listener = () => {
        if (mode.value === 'system') apply('system')
      }
      mediaQuery.addEventListener('change', listener)
    }
  })

  onBeforeUnmount(() => {
    // 模块级单例，不在此移除监听（生命周期内常驻）
  })

  return { mode, resolved, setTheme }
}
