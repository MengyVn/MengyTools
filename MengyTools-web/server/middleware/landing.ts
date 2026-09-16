/**
 * 默认落地端中间件（Nitro 服务端，SSR 阶段 302）。
 *
 * 规则（cookie 记忆「最近使用的端」，站点配置决定首次访问的默认端）：
 *   1. /?portal=1        → 强制回门户，并记住「门户」
 *   2. /?community=1     → 强制去社区，并记住「社区」
 *   3. 访问 /community/* → 记住「社区」
 *   4. 访问门户页面      → 记住「门户」（这样门户自己的「首页」链接不会被弹回社区）
 *   5. 访问 / 且无 cookie → 按后台 site.default_landing 落地
 *
 * 注意：故意不在「按默认值跳转」时写 cookie —— 否则用户会被永久钉在当时的默认端，
 *       后台后续改默认值就对这些人生效不了了。
 *
 * 用 302（临时）而非 301：默认端是后台随时可改的配置，不能被浏览器永久缓存。
 */
const LANDING_COOKIE = 'landing_pref'
const COOKIE_MAX_AGE = 60 * 60 * 24 * 180

/** 门户侧路径：访问这些说明用户在用门户 */
const PORTAL_PREFIXES = ['/blog', '/tools', '/nav', '/announcement', '/profile']

/** 站点配置的进程内缓存（后端侧还有 30 秒缓存，这里同样 30 秒，避免每个请求都打后端） */
let cacheAt = 0
let cachedLanding = 'community'

const loadDefaultLanding = async (event: any): Promise<string> => {
  if (Date.now() - cacheAt < 30_000) return cachedLanding
  try {
    const api = useRuntimeConfig(event).serverApiBase as string
    const res = await $fetch<{ data?: { defaultLanding?: string } }>(`${api}/v1/portal/settings`)
    cachedLanding = res?.data?.defaultLanding === 'portal' ? 'portal' : 'community'
  } catch {
    /* 后端不可用时保持上一次的值（默认 community），不影响页面本身 */
  }
  cacheAt = Date.now()
  return cachedLanding
}

const remember = (event: any, value: 'portal' | 'community') => {
  setCookie(event, LANDING_COOKIE, value, { path: '/', maxAge: COOKIE_MAX_AGE, sameSite: 'lax' })
}

export default defineEventHandler(async (event) => {
  const url = getRequestURL(event)
  const path = url.pathname

  // 只处理 HTML 页面请求：接口、构建产物、静态文件（含 .xml/.txt/.png）一律放行
  if (path.startsWith('/api') || path.startsWith('/_') || path.startsWith('/images') || /\.[a-z0-9]+$/i.test(path)) {
    return
  }

  // 1) 显式切换
  if (url.searchParams.get('portal') === '1') {
    remember(event, 'portal')
    return
  }
  if (url.searchParams.get('community') === '1') {
    remember(event, 'community')
    return sendRedirect(event, '/community', 302)
  }

  // 2) 记录最近使用的端
  if (path === '/community' || path.startsWith('/community/')) {
    remember(event, 'community')
    return
  }
  if (PORTAL_PREFIXES.some((p) => path === p || path.startsWith(`${p}/`))) {
    remember(event, 'portal')
    return
  }

  // 3) 只有根路径需要决定落地端
  if (path !== '/') return

  const pref = getCookie(event, LANDING_COOKIE)
  const landing = pref || (await loadDefaultLanding(event))

  if (landing === 'community') {
    return sendRedirect(event, '/community', 302)
  }
})
