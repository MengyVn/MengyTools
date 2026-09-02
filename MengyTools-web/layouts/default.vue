<script setup lang="ts">
const navLinks = [
  { to: '/', label: '首页' },
  { to: '/blog', label: '博客' },
  { to: '/tools', label: '工具' },
  { to: '/nav', label: '导航' }
]
</script>

<template>
  <div class="site">
    <header class="site-header">
      <div class="container header-inner">
        <NuxtLink to="/" class="logo">MengyTools</NuxtLink>
        <nav class="nav">
          <NuxtLink v-for="link in navLinks" :key="link.to" :to="link.to" class="nav-link">
            {{ link.label }}
          </NuxtLink>
        </nav>
      </div>
    </header>

    <main class="site-main">
      <div class="container">
        <slot />
      </div>
    </main>

    <footer class="site-footer">
      <div class="container">
        <p>© {{ new Date().getFullYear() }} MengyTools · 个人数字资产平台</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.site {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
.site-header {
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 10;
}
.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
}
.logo {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  text-decoration: none;
}
.nav {
  display: flex;
  gap: 24px;
}
.nav-link {
  color: #555;
  text-decoration: none;
  font-size: 15px;
}
.nav-link:hover,
.nav-link.router-link-exact-active {
  color: #409eff;
}
.site-main {
  flex: 1;
  padding: 32px 0;
}
.site-footer {
  background: #fafafa;
  border-top: 1px solid #eee;
  padding: 20px 0;
  text-align: center;
  color: #999;
  font-size: 14px;
}
</style>
