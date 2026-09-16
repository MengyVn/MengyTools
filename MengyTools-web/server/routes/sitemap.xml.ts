/**
 * 动态 sitemap.xml（Nitro 服务端路由）。
 *
 * 背景：门户的 public/robots.txt 里已经写了 `Sitemap: /sitemap.xml`，但该文件此前并不存在。
 * 本路由一次性补齐：同时收录门户页面与社区页面（社区内容来自后端接口，故必须动态生成）。
 *
 * 说明：
 *  - 站点域名从请求头推导（x-forwarded-proto / host），无需硬编码生产域名；
 *  - 单个 sitemap 上限按 500 条截断，避免文章/用户变多后文件膨胀（后续可拆分 sitemap index）。
 */
interface PageResult<T> {
  records: T[]
  total: number
}

interface ArticleItem {
  id: number
  publishTime?: string
  createTime?: string
}

interface TagItem {
  slug: string
}

interface UserBrief {
  id: number
}

const MAX_ENTRIES = 500

export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const api = config.serverApiBase as string

  const headers = getRequestHeaders(event)
  const proto = (headers['x-forwarded-proto'] as string) || 'http'
  const host = (headers['x-forwarded-host'] as string) || (headers.host as string) || 'localhost:9560'
  const origin = `${proto}://${host}`

  const urls: Array<{ loc: string; lastmod?: string; changefreq?: string; priority?: string }> = [
    { loc: '/', changefreq: 'daily', priority: '1.0' },
    { loc: '/blog', changefreq: 'daily', priority: '0.8' },
    { loc: '/tools', changefreq: 'weekly', priority: '0.6' },
    { loc: '/nav', changefreq: 'weekly', priority: '0.6' },
    { loc: '/announcement', changefreq: 'weekly', priority: '0.5' },
    { loc: '/community', changefreq: 'hourly', priority: '0.9' },
    { loc: '/community/tags', changefreq: 'weekly', priority: '0.6' }
  ]

  try {
    const articles = await $fetch<{ data: PageResult<ArticleItem> }>(
      `${api}/v1/community/articles?page=1&size=200`
    )
    for (const a of articles.data?.records ?? []) {
      urls.push({
        loc: `/community/posts/${a.id}`,
        lastmod: a.publishTime || a.createTime,
        changefreq: 'weekly',
        priority: '0.7'
      })
    }
  } catch {
    /* 后端不可用时仍返回静态部分，避免 sitemap 整体 500 */
  }

  try {
    const tags = await $fetch<{ data: TagItem[] }>(`${api}/v1/community/tags`)
    for (const t of tags.data ?? []) {
      urls.push({ loc: `/community/tags/${t.slug}`, changefreq: 'weekly', priority: '0.5' })
    }
  } catch {
    /* 同上 */
  }

  try {
    const sidebar = await $fetch<{ data: { activeUsers?: UserBrief[] } }>(`${api}/v1/community/sidebar`)
    for (const u of sidebar.data?.activeUsers ?? []) {
      urls.push({ loc: `/community/users/${u.id}`, changefreq: 'weekly', priority: '0.4' })
    }
  } catch {
    /* 同上 */
  }

  const body =
    '<?xml version="1.0" encoding="UTF-8"?>\n' +
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n' +
    urls
      .slice(0, MAX_ENTRIES)
      .map((u) => {
        const lastmod = u.lastmod ? `    <lastmod>${new Date(String(u.lastmod).replace(' ', 'T')).toISOString().slice(0, 10)}</lastmod>\n` : ''
        return (
          '  <url>\n' +
          `    <loc>${origin}${u.loc}</loc>\n` +
          lastmod +
          (u.changefreq ? `    <changefreq>${u.changefreq}</changefreq>\n` : '') +
          (u.priority ? `    <priority>${u.priority}</priority>\n` : '') +
          '  </url>'
        )
      })
      .join('\n') +
    '\n</urlset>\n'

  setHeader(event, 'Content-Type', 'application/xml; charset=utf-8')
  setHeader(event, 'Cache-Control', 'public, max-age=600')
  return body
})
