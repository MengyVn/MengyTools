/**
 * v-reveal 指令：元素滚动进入视口时渐入上移。
 * 用法：
 *   <div v-reveal>...</div>
 *   <div v-reveal="{ delay: 100 }">...</div>  // 延迟 ms，用于 stagger
 *   <div v-reveal="{ y: 40, duration: 800 }">...</div>
 *
 * 实现要点（参考高性能滚动动效经验）：
 * - 用 IntersectionObserver 触发，避免 scroll 监听占用主线程
 * - 只动 transform / opacity（GPU 合成层），不触发 layout
 * - 元素只观察一次，进入后 unobserve
 * - 仅客户端注册（.client.ts），SSR 不影响首屏内容直出
 */
export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.directive('reveal', {
    mounted(el: HTMLElement, binding) {
      const opts = (binding.value || {}) as {
        delay?: number
        y?: number
        duration?: number
      }
      const delay = opts.delay ?? 0
      const y = opts.y ?? 24
      const duration = opts.duration ?? 700

      el.classList.add('reveal')
      el.style.setProperty('--reveal-y', `${y}px`)
      el.style.transitionDuration = `${duration}ms`
      if (delay) el.style.transitionDelay = `${delay}ms`

      // 不支持 IO 时直接显示，保证内容可见
      if (typeof IntersectionObserver === 'undefined') {
        el.classList.add('reveal-visible')
        return
      }

      const io = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (entry.isIntersecting) {
              el.classList.add('reveal-visible')
              io.unobserve(el)
            }
          })
        },
        { threshold: 0.08, rootMargin: '0px 0px -8% 0px' }
      )
      io.observe(el)
    }
  })
})
