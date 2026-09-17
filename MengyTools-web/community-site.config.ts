/**
 * 社区站点常量（单一来源）。
 *
 * 这里刻意放在项目根目录、零依赖：既被前端 composable 复用，也被 Nitro 服务端路由
 * （sitemap.xml / rss.xml）以相对路径导入，避免站名散落在多处。
 *
 * TODO 待确认：把 name / tagline 换成正式站名后，导航、SEO meta、结构化数据、
 * sitemap 与 RSS 会同时生效。
 */
export const COMMUNITY_SITE = {
  name: 'Mengy 技术社区',
  tagline: '面向技术爱好者的交流社区',
  description: '技术文章、经验分享与讨论：评论、点赞、收藏、关注与消息通知。',
  locale: 'zh-CN',
  nav: [
    { to: '/community', label: '首页' },
    { to: '/community/boards', label: '板块' },
    { to: '/community/search', label: '搜索' }
  ]
}

/** 路由前缀，便于将来整体换路径（如 /bbs） */
export const COMMUNITY_BASE = '/community'
