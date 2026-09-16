/**
 * 动态 rss.xml（Nitro 服务端路由）：技术博客的订阅入口。
 * 收录社区最新的已发布文章（正文摘要作为 description）。
 */
import { COMMUNITY_SITE } from '../../community-site.config'

interface ArticleItem {
  id: number
  title: string
  summary?: string
  authorName?: string
  categoryName?: string
  publishTime?: string
  createTime?: string
}

const escapeXml = (s: string): string =>
  s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')

export default defineEventHandler(async (event) => {
  const config = useRuntimeConfig(event)
  const api = config.serverApiBase as string

  const headers = getRequestHeaders(event)
  const proto = (headers['x-forwarded-proto'] as string) || 'http'
  const host = (headers['x-forwarded-host'] as string) || (headers.host as string) || 'localhost:9560'
  const origin = `${proto}://${host}`

  let items: ArticleItem[] = []
  try {
    const res = await $fetch<{ data: { records: ArticleItem[] } }>(
      `${api}/v1/community/articles?page=1&size=30`
    )
    items = res.data?.records ?? []
  } catch {
    /* 后端不可用时返回空频道，而不是整体报错 */
  }

  const toRfc822 = (s?: string) => {
    const t = s ? new Date(String(s).replace(' ', 'T')) : new Date()
    return Number.isNaN(t.getTime()) ? new Date().toUTCString() : t.toUTCString()
  }

  const body =
    '<?xml version="1.0" encoding="UTF-8"?>\n' +
    '<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">\n' +
    '  <channel>\n' +
    `    <title>${escapeXml(COMMUNITY_SITE.name)}</title>\n` +
    `    <link>${origin}/community</link>\n` +
    `    <description>${escapeXml(COMMUNITY_SITE.description)}</description>\n` +
    `    <language>${COMMUNITY_SITE.locale}</language>\n` +
    `    <lastBuildDate>${new Date().toUTCString()}</lastBuildDate>\n` +
    `    <atom:link href="${origin}/rss.xml" rel="self" type="application/rss+xml"/>\n` +
    items
      .map((a) => {
        const link = `${origin}/community/posts/${a.id}`
        return (
          '    <item>\n' +
          `      <title>${escapeXml(a.title || '')}</title>\n` +
          `      <link>${link}</link>\n` +
          `      <guid isPermaLink="true">${link}</guid>\n` +
          `      <pubDate>${toRfc822(a.publishTime || a.createTime)}</pubDate>\n` +
          (a.categoryName ? `      <category>${escapeXml(a.categoryName)}</category>\n` : '') +
          `      <description>${escapeXml(a.summary || '')}</description>\n` +
          '    </item>'
        )
      })
      .join('\n') +
    '\n  </channel>\n</rss>\n'

  setHeader(event, 'Content-Type', 'application/rss+xml; charset=utf-8')
  setHeader(event, 'Cache-Control', 'public, max-age=600')
  return body
})
