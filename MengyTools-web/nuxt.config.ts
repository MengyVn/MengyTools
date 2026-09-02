// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },

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
      ]
    }
  },

  css: ['~/assets/css/main.css'],

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
        target: 'http://127.0.0.1:8120/api',
        changeOrigin: true
      }
    }
  }
})
