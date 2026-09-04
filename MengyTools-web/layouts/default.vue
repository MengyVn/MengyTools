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
        <NuxtLink to="/" class="logo">
          <span class="logo-badge">M</span>
          <span class="logo-text">MengyTools</span>
        </NuxtLink>
        <nav class="nav">
          <NuxtLink
            v-for="link in navLinks"
            :key="link.to"
            :to="link.to"
            class="nav-link"
          >
            {{ link.label }}
          </NuxtLink>
        </nav>
        <div class="header-right">
          <AnnouncementBell />
          <ThemeToggle />
        </div>
      </div>
    </header>

    <main class="site-main">
      <slot />
    </main>

    <footer class="site-footer">
      <div class="container footer-inner">
        <div class="footer-brand">
          <span class="footer-logo">MengyTools</span>
          <span class="footer-desc">个人数字资产平台 · 博客 / 工具 / 导航</span>
        </div>
        <p class="copyright">© {{ new Date().getFullYear() }} MengyTools. All rights reserved.</p>
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

/* 顶部导航 */
.site-header {
  background: var(--header-bg);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-light);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: var(--header-height);
}
.header-right {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-color);
  font-weight: 700;
}
.logo:hover {
  color: var(--text-color);
}
.logo-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  box-shadow: var(--shadow-primary);
}
.logo-text {
  font-size: 18px;
  letter-spacing: 0.3px;
}
.nav {
  display: flex;
  gap: 4px;
}
.nav-link {
  padding: 8px 16px;
  color: var(--text-secondary);
  font-size: 15px;
  font-weight: 500;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
}
.nav-link:hover {
  color: var(--primary);
  background: var(--bg-hover);
}
.nav-link.router-link-exact-active {
  color: var(--primary);
  background: rgba(99, 102, 241, 0.08);
}

/* 主体 */
.site-main {
  flex: 1;
  padding: 40px 0;
}

/* 底部 */
.site-footer {
  background: #0f172a;
  color: rgba(255, 255, 255, 0.7);
  padding: 40px 0 24px;
  margin-top: auto;
}
.footer-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.footer-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.footer-logo {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
}
.footer-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}
.copyright {
  margin: 12px 0 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

@media (max-width: 640px) {
  .logo-text {
    display: none;
  }
  .nav-link {
    padding: 8px 12px;
    font-size: 14px;
  }
}
</style>
