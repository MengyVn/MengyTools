import request from './request'

// ==================== 安全审计（登录日志 / IP 封禁） ====================

export interface PageResult<T> {
  records: T[]
  total: number
  size?: number
  current?: number
  pages?: number
}

/** 登录日志（一次登录尝试一行） */
export interface LoginLog {
  id: number
  username: string
  userId?: number | null
  ip: string
  /** IP 归属地：离线库解析；内网/本机单独标记 */
  region: string
  userAgent: string
  browser: string
  os: string
  /** 1成功 0失败 */
  status: number
  message: string
  loginTime: string
}

/** IP 封禁记录（人工封禁，区别于 Redis 的临时锁定） */
export interface IpBan {
  id: number
  ip: string
  region: string
  reason: string
  operatorId?: number | null
  operatorName: string
  /** null=永久封禁 */
  expireTime?: string | null
  /** 1封禁中 0已解除 */
  status: number
  banTime: string
  releaseTime?: string | null
  createTime: string
  updateTime: string
}

/** Redis 临时锁定（连续输错密码触发） */
export interface IpLock {
  ip: string
  region: string
  failCount: number
  locked: boolean
  remainSeconds: number
}

export interface SecurityOverview {
  /** 今日登录失败次数 */
  todayFailures: number
  /** 封禁中的 IP 数 */
  activeBans: number
  /** 当前访问者 IP（前端用来提示「别把自己封了」） */
  currentIp: string
}

/**
 * 概览统计
 *
 * 统一用 request.get<unknown, T> 的写法：拦截器已经 return response.data，
 * 第二个泛型才是真正的返回类型（与 api/community.ts 保持一致）。
 */
export const securityOverview = () =>
  request.get<unknown, SecurityOverview>('/v1/admin/security/overview')

/**
 * 登录日志分页
 * @param range recent=最近 7 天，all=全部
 */
export const listLoginLogs = (params: {
  page?: number
  size?: number
  range?: 'recent' | 'all'
  username?: string
  ip?: string
  status?: number
}) => request.get<unknown, PageResult<LoginLog>>('/v1/admin/security/login-logs', { params })

/** 清理 N 天前的登录日志，返回删除条数 */
export const clearLoginLogs = (days = 30) =>
  request.delete<unknown, number>('/v1/admin/security/login-logs', { params: { days } })

/** 封禁名单分页 */
export const listIpBans = (params: {
  page?: number
  size?: number
  state?: 'active' | 'released'
  keyword?: string
}) => request.get<unknown, PageResult<IpBan>>('/v1/admin/security/ip-bans', { params })

/** 封禁 IP；minutes 留空或 0 表示永久 */
export const createIpBan = (data: { ip: string; reason?: string; minutes?: number | null }) =>
  request.post<unknown, IpBan>('/v1/admin/security/ip-bans', data)

/** 解除封禁（同时清掉该 IP 的临时锁定） */
export const releaseIpBan = (ip: string) =>
  request.delete<unknown, void>('/v1/admin/security/ip-bans', { params: { ip } })

/** 临时锁定中的 IP 列表 */
export const listIpLocks = () => request.get<unknown, IpLock[]>('/v1/admin/security/ip-locks')

/** 解锁某个 IP 的临时锁定 */
export const unlockIp = (ip: string) =>
  request.delete<unknown, void>('/v1/admin/security/ip-locks', { params: { ip } })

/** 封禁时长选项（分钟） */
export const BAN_DURATIONS = [
  { label: '永久', value: 0 },
  { label: '1 小时', value: 60 },
  { label: '1 天', value: 1440 },
  { label: '7 天', value: 10080 },
  { label: '30 天', value: 43200 }
]

/** 后端时间格式 2026-09-30T10:03:13 → 2026-09-30 10:03:13 */
export const fmtTime = (t?: string | null) => (t ? String(t).replace('T', ' ').slice(0, 19) : '—')
