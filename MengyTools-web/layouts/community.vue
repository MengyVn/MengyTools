<script setup lang="ts">
/**
 * 社区布局（轻社区 / 大众向）。
 *
 * 与门户布局（layouts/default.vue）完全独立：
 *  - 门户保持冻结，本布局不引用门户任何 class 或组件样式；
 *  - 自带一套设计令牌（中性灰阶 + 单一强调色、8pt 间距、无动效），刻意去掉门户的
 *    渐变/跑马灯/滚动渐入等个性化元素；
 *  - 仅复用「功能型」资产：useAuth（登录态）、SlideCaptcha（滑动验证）、ThemeToggle（明暗切换）；
 *  登录/注册用社区自己的 CommunityAuthModal（社区风格），不再使用门户的 LoginModal。
 */
import { Menu, Search, User, X, Bell, PenLine } from 'lucide-vue-next'
import { COMMUNITY_SITE, useCommunity } from '~/composables/useCommunity'
import { imageUrl } from '~/utils/image'
import { useCommunitySite } from '~/composables/useCommunitySite'

const { user, isLoggedIn, init, logout } = useAuth()
const { unreadCount } = useCommunity()
// 站名/定位优先取后台站点设置（改配置即生效），取不到回退内置默认值
const { siteName, siteTagline } = useCommunitySite()

/** 未读消息数（顶栏红点）；登录后拉取，退出即清零 */
const unread = ref(0)
const loadUnread = async () => {
  if (!isLoggedIn.value) {
    unread.value = 0
    return
  }
  try {
    unread.value = await unreadCount()
  } catch {
    unread.value = 0
  }
}

const showLogin = ref(false)
const showUserMenu = ref(false)
const mobileNavOpen = ref(false)
const keyword = ref('')
const router = useRouter()
const route = useRoute()

onMounted(() => {
  init()
  loadUnread()
  document.addEventListener('click', closeUserMenu)
})
watch(isLoggedIn, loadUnread)
onBeforeUnmount(() => document.removeEventListener('click', closeUserMenu))

const closeUserMenu = () => {
  showUserMenu.value = false
}

const toggleUserMenu = (e: MouseEvent) => {
  e.stopPropagation()
  showUserMenu.value = !showUserMenu.value
}

const onSearch = () => {
  const q = keyword.value.trim()
  if (!q) return
  router.push({ path: '/community/search', query: { q } })
}

const doLogout = () => {
  logout()
  showUserMenu.value = false
  if (route.path.startsWith('/community')) {
    router.push('/community')
  }
}

const initials = computed(() => {
  const name = user.value?.nickname || user.value?.username || '?'
  return name.slice(0, 1).toUpperCase()
})

const year = new Date().getFullYear()
</script>

<template>
  <div class="community">
    <header class="c-header">
      <div class="c-container c-header-inner">
        <NuxtLink to="/community" class="c-brand">
          <span class="c-brand-mark">M</span>
          <span class="c-brand-name">{{ siteName }}</span>
        </NuxtLink>

        <nav class="c-nav" :class="{ open: mobileNavOpen }">
          <NuxtLink
            v-for="link in COMMUNITY_SITE.nav"
            :key="link.to"
            :to="link.to"
            class="c-nav-link"
            @click="mobileNavOpen = false"
          >
            {{ link.label }}
          </NuxtLink>
        </nav>

        <div class="c-header-right">
          <form class="c-search" @submit.prevent="onSearch">
            <Search :size="15" class="c-search-icon" />
            <input v-model="keyword" type="search" placeholder="搜索文章" aria-label="搜索文章" />
          </form>

          <NuxtLink to="/community/new" class="c-post-btn" title="发布新帖">
            <PenLine :size="15" />
            <span>发帖</span>
          </NuxtLink>

          <NuxtLink to="/community/notifications" class="c-bell" title="我的消息" aria-label="我的消息">
            <Bell :size="17" />
            <span v-if="unread > 0" class="c-bell-badge">{{ unread > 99 ? '99+' : unread }}</span>
          </NuxtLink>

          <ThemeToggle />

          <template v-if="isLoggedIn">
            <button class="c-user-trigger" type="button" @click="toggleUserMenu">
              <img v-if="user?.avatar" :src="imageUrl(user.avatar)" :alt="user.nickname" class="c-avatar" />
              <span v-else class="c-avatar c-avatar-text">{{ initials }}</span>
              <span class="c-user-name">{{ user?.nickname || user?.username }}</span>
            </button>
            <div v-if="showUserMenu" class="c-user-menu" @click.stop>
              <NuxtLink v-if="user?.id" :to="`/community/users/${user.id}`" class="c-user-menu-item">
                我的主页
              </NuxtLink>
              <NuxtLink to="/community/my-posts" class="c-user-menu-item">我的帖子</NuxtLink>
              <NuxtLink to="/community/settings" class="c-user-menu-item">个人设置</NuxtLink>
              <button type="button" class="c-user-menu-item c-user-menu-danger" @click="doLogout">
                退出登录
              </button>
            </div>
          </template>
          <button v-else class="c-login-btn" type="button" @click="showLogin = true">
            <User :size="15" />
            <span>登录</span>
          </button>

          <button
            class="c-mobile-toggle"
            type="button"
            :aria-expanded="mobileNavOpen"
            aria-label="菜单"
            @click="mobileNavOpen = !mobileNavOpen"
          >
            <component :is="mobileNavOpen ? X : Menu" :size="18" />
          </button>
        </div>
      </div>
    </header>

    <main class="c-main">
      <div class="c-container">
        <slot />
      </div>
    </main>

    <footer class="c-footer">
      <div class="c-container c-footer-inner">
        <div class="c-footer-about">
          <strong>{{ siteName }}</strong>
          <span>{{ siteTagline }}</span>
        </div>
        <div class="c-footer-links">
          <NuxtLink to="/community">首页</NuxtLink>
          <NuxtLink to="/community/boards">板块</NuxtLink>
          <NuxtLink to="/community/search">搜索</NuxtLink>
        </div>
        <p class="c-copyright">© {{ year }} {{ siteName }}</p>
      </div>
    </footer>

    <CommunityAuthModal v-model:visible="showLogin" @logged-in="showLogin = false" />
  </div>
</template>

<style scoped>
/* ============ 设计令牌：中性灰阶 + 单一强调色，无渐变/无动效 ============ */
.community {
  --c-bg: #f6f7f9;
  --c-surface: #ffffff;
  --c-surface-alt: #fafbfc;
  --c-border: #e4e6eb;
  --c-border-strong: #d2d6dd;
  --c-text: #1b1f24;
  --c-text-muted: #5c6472;
  --c-text-faint: #8b93a1;
  --c-accent: #2563eb;
  --c-accent-soft: #eef3ff;
  --c-radius: 4px;

  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--c-bg);
  color: var(--c-text);
  font-size: 15px;
  line-height: 1.65;
}



.c-container {
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 20px;
}

/* ============ 顶部导航 ============ */
.c-header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: var(--c-surface);
  border-bottom: 1px solid var(--c-border);
}
.c-header-inner {
  display: flex;
  align-items: center;
  gap: 20px;
  height: 56px;
}
.c-brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--c-text);
  font-weight: 600;
  font-size: 16px;
  flex-shrink: 0;
}
.c-brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: var(--c-radius);
  background: var(--c-accent);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}
.c-nav {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: 1;
}
.c-nav-link {
  padding: 6px 12px;
  border-radius: var(--c-radius);
  color: var(--c-text-muted);
  font-size: 14px;
}
.c-nav-link:hover {
  color: var(--c-text);
  background: var(--c-surface-alt);
}
.c-nav-link.router-link-exact-active {
  color: var(--c-accent);
  font-weight: 500;
}

.c-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  position: relative;
}
.c-search {
  position: relative;
  display: flex;
  align-items: center;
}
.c-search-icon {
  position: absolute;
  left: 9px;
  color: var(--c-text-faint);
  pointer-events: none;
}
.c-search input {
  width: 180px;
  height: 32px;
  padding: 0 10px 0 28px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface-alt);
  color: var(--c-text);
  font-size: 14px;
  outline: none;
}
.c-search input:focus {
  border-color: var(--c-accent);
  background: var(--c-surface);
}
.c-search input::placeholder {
  color: var(--c-text-faint);
}

.c-post-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 13px;
  border: 1px solid var(--c-accent);
  border-radius: var(--c-radius);
  background: var(--c-accent);
  color: #fff;
  font-size: 13.5px;
}
.c-post-btn:hover {
  opacity: 0.92;
}

.c-bell {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text-muted);
}
.c-bell:hover {
  border-color: var(--c-accent);
  color: var(--c-accent);
}
.c-bell-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #d14343;
  color: #fff;
  font-size: 11px;
  line-height: 16px;
  text-align: center;
}

.c-login-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--c-border-strong);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text);
  font-size: 14px;
  cursor: pointer;
}
.c-login-btn:hover {
  border-color: var(--c-accent);
  color: var(--c-accent);
}

.c-user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 6px 0 4px;
  border: 1px solid transparent;
  border-radius: var(--c-radius);
  background: transparent;
  color: var(--c-text);
  font-size: 14px;
  cursor: pointer;
}
.c-user-trigger:hover {
  border-color: var(--c-border);
  background: var(--c-surface-alt);
}
.c-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.c-avatar-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--c-accent-soft);
  color: var(--c-accent);
  font-size: 12px;
  font-weight: 600;
}
.c-user-name {
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.c-user-menu {
  position: absolute;
  top: 40px;
  right: 0;
  min-width: 132px;
  padding: 4px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.08);
  z-index: 60;
}
.c-user-menu-item {
  display: block;
  width: 100%;
  padding: 7px 10px;
  border: 0;
  border-radius: var(--c-radius);
  background: transparent;
  color: var(--c-text);
  font-size: 14px;
  text-align: left;
  cursor: pointer;
}
.c-user-menu-item:hover {
  background: var(--c-surface-alt);
}
.c-user-menu-danger {
  color: #d14343;
}

.c-mobile-toggle {
  display: none;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text);
  cursor: pointer;
}

/* ============ 主体 / 页脚 ============ */
.c-main {
  flex: 1;
  padding: 24px 0 48px;
}
.c-footer {
  border-top: 1px solid var(--c-border);
  background: var(--c-surface);
  padding: 24px 0;
}
.c-footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.c-footer-about {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 13px;
}
.c-footer-about span {
  color: var(--c-text-muted);
}
.c-footer-links {
  display: flex;
  gap: 16px;
  font-size: 13px;
}
.c-footer-links a {
  color: var(--c-text-muted);
}
.c-footer-links a:hover {
  color: var(--c-accent);
}
.c-copyright {
  margin: 0;
  font-size: 12px;
  color: var(--c-text-faint);
}

@media (max-width: 860px) {
  .c-nav {
    display: none;
    position: absolute;
    top: 56px;
    left: 0;
    right: 0;
    flex-direction: column;
    align-items: stretch;
    gap: 0;
    padding: 8px 20px 12px;
    background: var(--c-surface);
    border-bottom: 1px solid var(--c-border);
  }
  .c-nav.open {
    display: flex;
  }
  .c-nav-link {
    padding: 10px 4px;
  }
  .c-search input {
    width: 120px;
  }
  .c-user-name {
    display: none;
  }
  .c-mobile-toggle {
    display: inline-flex;
  }
  .c-footer-inner {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

<!--
  深色主题令牌必须放在非 scoped 块里：
  在 scoped 块中写 :global([data-theme='dark']) .community，Vue 的 scoped 编译器会把选择器
  压成 [data-theme='dark']（丢掉 .community），变量就落在 <html> 上；而 .community 自身又
  声明了一套浅色变量——元素自身声明优先于继承，于是切换主题完全没反应。
  这里用 html[data-theme='dark'] .community：特异性 (0,2,1) 高于 .community[data-v-x] (0,2,0)，
  仍然读取 <html> 上的 data-theme（nuxt.config 的防闪烁脚本在 hydration 前就写好了），因此既生效又不会闪。
  只以 .community 限定，门户没有该元素，故对门户零影响。
-->
<style>
html[data-theme='dark'] .community {
  --c-bg: #0e1116;
  --c-surface: #161a20;
  --c-surface-alt: #1a1f26;
  --c-border: #262c35;
  --c-border-strong: #333b46;
  --c-text: #e6e9ee;
  --c-text-muted: #9aa3b0;
  --c-text-faint: #6f7886;
  --c-accent: #6ea8fe;
  --c-accent-soft: #1b2434;
}
</style>
