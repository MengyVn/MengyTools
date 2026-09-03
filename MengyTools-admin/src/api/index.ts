import request from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
}

// 登录
export const login = (data: LoginParams) =>
  request.post<unknown, LoginResult>('/v1/auth/login', data, {
    headers: { 'X-Skip-Toast': '1' }
  })

// 刷新 Token
export const refresh = (refreshToken: string) =>
  request.post<unknown, LoginResult>('/v1/auth/refresh', { refreshToken })

// 获取当前用户信息
export const getUserInfo = () => request.get<unknown, UserInfo>('/v1/auth/me')

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  roles: string[]
  permissions: string[]
}

// 上传图片（返回相对路径，如 /images/xxx.jpg）
export const uploadImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<unknown, { url: string }>('/v1/admin/files/upload', formData)
}

// ==================== 仪表盘 ====================

export interface DashboardSummary {
  articleTotal: number
  articlePublished: number
  articleDraft: number
  categoryTotal: number
  navSiteTotal: number
  navCategoryTotal: number
  toolTotal: number
  userTotal: number
}

export interface RecentArticle {
  id: number
  title: string
  cover: string
  categoryId: number
  status: number
  isTop: number
  viewCount: number
  publishTime: string | null
  createTime: string
}

export const getDashboardSummary = () =>
  request.get<unknown, DashboardSummary>('/v1/admin/dashboard/summary')

export const getRecentArticles = () =>
  request.get<unknown, { records: RecentArticle[] }>('/v1/admin/dashboard/recent-articles')
