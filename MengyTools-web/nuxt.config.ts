// 后端地址：服务端渲染走 SERVER_API_BASE；开发代理默认指 8120，
// 可用 DEV_API_TARGET 覆盖（便于同时跑多个后端实例联调）
const DEV_API_TARGET = process.env.DEV_API_TARGET || 'http://127.0.0.1:8120/api'

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },

  // 监听 0.0.0.0，允许通过 IP+端口 访问（默认仅 localhost）
  devServer: {
    host: '0.0.0.0',
    port: 9560
  },

  // SSR 服务端渲染（need.md：博客 SEO）
  ssr: true,

  modules: ['@pinia/nuxt'],

  app: {
    head: {
      htmlAttrs: { lang: 'zh-CN' },
      title: 'MengyTools - 个人数字资产平台',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'description', content: '个人博客 · 效率工具 · 网址导航' }
      ],
      // 防主题闪烁：SSR 后、hydration 前在 <html> 设置 data-theme
      script: [
        {
          innerHTML: `(function(){try{var t=localStorage.getItem('theme')||'system';var d=t==='system'?(window.matchMedia&&window.matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light'):t;document.documentElement.setAttribute('data-theme',d);}catch(e){}})();`,
          tagPosition: 'head'
        }
      ],
      // JS 禁用时让滚动渐入元素直接可见，避免内容隐藏
      noscript: [
        { innerHTML: '<style>.reveal{opacity:1!important;transform:none!important}</style>' }
      ]
    }
  },

  css: ['~/assets/css/main.css', '~/assets/css/animations.css'],

  runtimeConfig: {
    // 后端 API 地址：服务端渲染走内网，浏览器走公网
    serverApiBase: process.env.SERVER_API_BASE || 'http://127.0.0.1:8120/api',
    public: {
      apiBase: process.env.PUBLIC_API_BASE || '/api'
    }
  },

  // 开发环境代理后端，避免跨域
  nitro: {
    devProxy: {
      '/api': {
        target: DEV_API_TARGET,
        changeOrigin: true
      },
      // 图片：转发到本机 nginx（devProxy 会剥掉 /images 前缀，故 target 需带 /images）
      '/images': {
        target: 'http://127.0.0.1:1096/images',
        changeOrigin: true
      }
    }
  }
})
