/**
 * 社区（轻社区 / 互动型）数据访问层。
 *
 * 与门户（useApi + /v1/blog、/v1/nav、/v1/tools）并行：社区走 /v1/community/**，
 * 两侧共用同一套 useApi（同源代理、cookie 双 Token、401 自动续签）。
 */

/**
 * 站点常量：改为从项目根的 community-site.config.ts 单一来源再导出，
 * 这样前端页面、sitemap.xml、rss.xml 用的是同一份站名/定位。
 */
export { COMMUNITY_SITE, COMMUNITY_BASE } from '~/community-site.config'

/** 分页响应（对齐 MyBatis-Plus IPage） */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface CommunityArticle {
  id: number
  title: string
  summary?: string
  cover?: string
  categoryId?: number
  categoryName?: string
  authorId?: number
  authorName?: string
  isTop?: number
  viewCount?: number
  commentCount?: number
  likeCount?: number
  favoriteCount?: number
  publishTime?: string
  createTime?: string
}

export interface CommunityArticleDetail extends CommunityArticle {
  content?: string
  contentHtml?: string
  contentFormat?: string
  allowComment?: number
  updateTime?: string
}

export interface CommunityComment {
  id: number
  articleId: number
  rootId: number
  parentId: number
  floor?: number
  replyToUserId?: number
  replyToName?: string
  authorId: number
  authorName: string
  authorAvatar?: string
  content: string
  contentHtml?: string
  likeCount?: number
  createTime?: string
  replies?: CommunityComment[]
  replyCount?: number
}

export interface CommunityUserProfile {
  id: number
  nickname: string
  avatar?: string
  signature?: string
  createTime?: string
  commentCount?: number
  articleCount?: number
  receivedLikeCount?: number
}

export interface CommunityUserComment {
  id: number
  articleId: number
  articleTitle?: string
  rootId?: number
  parentId?: number
  content: string
  contentHtml?: string
  likeCount?: number
  createTime?: string
}

export interface CommunityTag {
  id: number
  name: string
  slug: string
  articleCount: number
}

export interface CommunityUserBrief {
  id: number
  nickname: string
  avatar?: string
  commentCount: number
}

export interface CommunitySidebar {
  articleCount: number
  commentCount: number
  userCount: number
  tagCount: number
  hotTags: CommunityTag[]
  activeUsers: CommunityUserBrief[]
}

/** 通知项（字段对齐后端 CommunityNotification 实体） */
export interface CommunityNotificationItem {
  id: number
  type: 'reply' | 'mention' | 'like' | 'system' | string
  actorId: number
  actorName?: string
  actorAvatar?: string
  articleId: number
  commentId: number
  title: string
  content?: string
  isRead: number
  createTime: string
}

/** 通知类型的中文文案与跳转目标 */
export const notificationTarget = (n: CommunityNotificationItem): string =>
  n.articleId ? `/community/posts/${n.articleId}` : '/community'

export const NOTIFICATION_TYPE_TEXT: Record<string, string> = {
  reply: '回复',
  mention: '提及',
  like: '点赞',
  system: '系统'
}

export const useCommunity = () => {
  const api = useApi()

  return {
    /** 文章列表：sort = latest | hot */
    listArticles: (params: {
      page?: number
      size?: number
      categoryId?: number
      tagId?: number
      keyword?: string
      sort?: 'latest' | 'hot'
    } = {}) =>
      api.get<PageResult<CommunityArticle>>('/v1/community/articles', { params }),

    article: (id: number | string) =>
      api.get<CommunityArticleDetail>(`/v1/community/articles/${id}`),

    /** 文章评论：顶层评论分页，每条带楼中楼子回复 */
    comments: (id: number | string, params: { page?: number; size?: number; sort?: 'asc' | 'desc' } = {}) =>
      api.get<PageResult<CommunityComment>>(`/v1/community/articles/${id}/comments`, { params }),

    user: (id: number | string) => api.get<CommunityUserProfile>(`/v1/community/users/${id}`),

    userComments: (id: number | string, params: { page?: number; size?: number } = {}) =>
      api.get<PageResult<CommunityUserComment>>(`/v1/community/users/${id}/comments`, { params }),

    tags: () => api.get<CommunityTag[]>('/v1/community/tags'),

    tagArticles: (slug: string, params: { page?: number; size?: number; sort?: 'latest' | 'hot' } = {}) =>
      api.get<PageResult<CommunityArticle>>(`/v1/community/tags/${slug}/articles`, { params }),

    search: (q: string, params: { page?: number; size?: number } = {}) =>
      api.get<PageResult<CommunityArticle>>('/v1/community/search', { params: { q, ...params } }),

    sidebar: () => api.get<CommunitySidebar>('/v1/community/sidebar'),

    // ==================== 写入链路（需登录，P3） ====================

    /** 发表评论或楼中楼回复（parentId 为空表示发新楼层） */
    createComment: (body: { articleId: number; content: string; parentId?: number }) =>
      api.post<CommunityComment>('/v1/community/comments', body),

    /** 撤回自己的评论 */
    deleteComment: (id: number) => api.delete<void>(`/v1/community/comments/${id}`),

    /** 点赞 / 收藏（开关式幂等） */
    react: (body: { targetType: 'article' | 'comment'; targetId: number; type: 'like' | 'favorite' }) =>
      api.post<{ active: boolean; count: number }>('/v1/community/reactions', body),

    /** 我的点赞/收藏状态 */
    reactionState: (targetType: string, targetId: number) =>
      api.get<{ like: boolean; favorite: boolean }>(
        `/v1/community/reactions/state?targetType=${targetType}&targetId=${targetId}`
      ),

    /** 批量查询「我点过赞/收藏」的对象 id（评论列表初始化用） */
    reactedIds: (targetType: 'article' | 'comment', type: 'like' | 'favorite', ids: number[]) =>
      api.get<number[]>(
        `/v1/community/reactions/state/batch?targetType=${targetType}&type=${type}&ids=${ids.join(',')}`
      ),

    /** 关注 / 取关（用户或标签） */
    follow: (body: { targetType: 'user' | 'tag'; targetId: number }) =>
      api.post<{ active: boolean; count: number }>('/v1/community/follows', body),

    /** 我的关注状态 */
    followState: (targetType: string, targetId: number) =>
      api.get<{ active: boolean; count: number }>(
        `/v1/community/follows/state?targetType=${targetType}&targetId=${targetId}`
      ),

    /** 举报 */
    report: (body: { targetType: 'comment' | 'article' | 'user'; targetId: number; reason: string; detail?: string }) =>
      api.post<void>('/v1/community/reports', body),

    /** 我的通知 */
    notifications: (params: { page?: number; size?: number; unreadOnly?: boolean } = {}) =>
      api.get<PageResult<CommunityNotificationItem>>('/v1/community/notifications', { params }),

    /** 未读数 */
    unreadCount: () => api.get<number>('/v1/community/notifications/unread-count'),

    /** 全部标记已读 */
    markAllRead: () => api.post<number>('/v1/community/notifications/read')
  }
}

/** 相对时间：3 分钟前 / 2 小时前 / 5 天前 / 2026-09-01 */
export const formatRelativeTime = (input?: string | null): string => {
  if (!input) return ''
  const t = new Date(String(input).replace(' ', 'T')).getTime()
  if (Number.isNaN(t)) return String(input)
  const diff = Date.now() - t
  const min = 60 * 1000
  const hour = 60 * min
  const day = 24 * hour
  if (diff < min) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / min)} 分钟前`
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`
  if (diff < 30 * day) return `${Math.floor(diff / day)} 天前`
  return new Date(t).toLocaleDateString('zh-CN')
}

/** 数字缩写：1234 → 1.2k */
export const formatCount = (n?: number | null): string => {
  const v = Number(n ?? 0)
  if (v < 1000) return String(v)
  if (v < 10000) return `${(v / 1000).toFixed(1)}k`
  return `${(v / 10000).toFixed(1)}w`
}
