/**
 * 社区站点信息（站名 / 一句话定位）。
 *
 * 优先取后台「站点设置」里的 site.name / site.tagline（改配置即生效，无需改代码构建），
 * 取不到时回退到 community-site.config.ts 的内置默认值（后端不可用也能正常渲染）。
 *
 * 用 useAsyncData + 固定 key：同一请求内多个组件调用只会发一次请求（Nuxt 按 key 去重）。
 */
import { COMMUNITY_SITE } from '~/community-site.config'

export interface PublicSiteSettings {
  defaultLanding: string
  siteName: string
  siteTagline: string
}

// 模块级 handler：保证多次调用传入同一个函数引用，配合固定 key 去重
const fetchPublicSettings = () => useApi().get<PublicSiteSettings>('/v1/portal/settings')

export const useCommunitySite = () => {
  const { data } = useAsyncData<PublicSiteSettings | null>(
    'community-public-settings',
    fetchPublicSettings,
    { default: () => null }
  )

  const siteName = computed(() => data.value?.siteName || COMMUNITY_SITE.name)
  const siteTagline = computed(() => data.value?.siteTagline || COMMUNITY_SITE.tagline)
  const defaultLanding = computed(() => data.value?.defaultLanding || 'community')

  return { siteName, siteTagline, defaultLanding, settings: data }
}
