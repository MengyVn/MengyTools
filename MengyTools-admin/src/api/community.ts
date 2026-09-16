import request from './request'

// ==================== 社区（轻社区 / 互动型）管理 ====================

export interface PageResult<T> {
  records: T[]
  total: number
  size?: number
  current?: number
  pages?: number
}

/** 评论审核列表项 */
export interface CommunityCommentAdmin {
  id: number
  articleId: number
  articleTitle: string | null
  rootId: number
  parentId: number
  floor: number
  authorId: number
  authorName: string
  content: string
  /** 0待审 1已发布 2已屏蔽 */
  status: number
  likeCount: number
  ip: string
  createTime: string
}

/** 举报列表项 */
export interface CommunityReport {
  id: number
  reporterId: number
  reporterName: string
  /** comment / article / user */
  targetType: string
  targetId: number
  targetExcerpt: string | null
  reason: string
  detail: string
  /** 0待处理 1举报成立 2已驳回 */
  status: number
  handlerId: number | null
  handlerName: string | null
  handleNote: string | null
  handleTime: string | null
  createTime: string
}

/** 用户治理列表项 */
export interface CommunityUserAdmin {
  id: number
  username: string
  nickname: string
  avatar: string
  signature: string
  /** 1启用 0禁用（封禁即置 0） */
  status: number
  muteUntil: string | null
  commentCount: number
  createTime: string
}

/** 治理操作日志 */
export interface CommunityAuditLog {
  id: number
  operatorId: number
  operatorName: string
  action: string
  targetType: string
  targetId: number
  beforeValue: string
  afterValue: string
  note: string
  ip: string
  createTime: string
}

export interface CommunityStats {
  commentPending: number
  commentPublished: number
  commentBlocked: number
  reportPending: number
}

/** 评论列表（可按状态/文章/关键词过滤） */
export const listCommunityComments = (params: {
  page?: number
  size?: number
  status?: number
  articleId?: number
  keyword?: string
}) => request.get<unknown, PageResult<CommunityCommentAdmin>>('/v1/admin/community/comments', { params })

/** 举报列表（可按状态过滤） */
export const listCommunityReports = (params: { page?: number; size?: number; status?: number }) =>
  request.get<unknown, PageResult<CommunityReport>>('/v1/admin/community/reports', { params })

/** 用户治理列表 */
export const listCommunityUsers = (params: {
  page?: number
  size?: number
  keyword?: string
  onlyCommented?: boolean
}) => request.get<unknown, PageResult<CommunityUserAdmin>>('/v1/admin/community/users', { params })

/** 治理操作日志 */
export const listCommunityAuditLogs = (params: {
  page?: number
  size?: number
  action?: string
  targetType?: string
  targetId?: number
}) => request.get<unknown, PageResult<CommunityAuditLog>>('/v1/admin/community/audit-logs', { params })

/** 待办徽标统计 */
export const getCommunityStats = () => request.get<unknown, CommunityStats>('/v1/admin/community/stats')

// ==================== 治理动作（P3，全部写操作都会留痕） ====================

/** 评论审核：0待审 1已发布 2已屏蔽 */
export const auditComment = (id: number, status: number, note?: string) =>
  request.put<unknown, void>(`/v1/admin/community/comments/${id}/status`, { status, note })

/** 删除评论（逻辑删除，顶层评论连带子回复） */
export const deleteCommunityComment = (id: number, note?: string) =>
  request.delete<unknown, void>(`/v1/admin/community/comments/${id}`, { params: { note } })

/** 举报处理：status 1=成立 2=驳回；blockComment=成立时连带屏蔽被举报评论 */
export const handleCommunityReport = (id: number, status: number, note: string, blockComment: boolean) =>
  request.put<unknown, void>(`/v1/admin/community/reports/${id}/handle`, { status, note, blockComment })

/** 禁言：minutes<=0 表示解除禁言 */
export const muteCommunityUser = (id: number, minutes: number, note?: string) =>
  request.put<unknown, string>(`/v1/admin/community/users/${id}/mute`, { minutes, note })

/** 封禁 / 解封 */
export const banCommunityUser = (id: number, banned: boolean, note?: string) =>
  request.put<unknown, string>(`/v1/admin/community/users/${id}/ban`, { banned, note })

/** 治理动作常量（下拉与标签复用） */
export const AUDIT_ACTIONS = [
  { label: '评论审核', value: 'comment.audit' },
  { label: '评论删除', value: 'comment.delete' },
  { label: '举报处理', value: 'report.handle' },
  { label: '用户禁言', value: 'user.mute' },
  { label: '用户封禁', value: 'user.ban' }
]
