import request from './request'

// ==================== 导航分类 ====================

export interface NavCategory {
  id: number
  parentId: number
  name: string
  icon: string
  sort: number
}

export const listNavCategories = () =>
  request.get<unknown, NavCategory[]>('/v1/admin/nav-categories')

export const createNavCategory = (data: Partial<NavCategory>) =>
  request.post<unknown, number>('/v1/admin/nav-categories', data)

export const updateNavCategory = (id: number, data: Partial<NavCategory>) =>
  request.put<unknown, void>(`/v1/admin/nav-categories/${id}`, data)

export const deleteNavCategory = (id: number) =>
  request.delete<unknown, void>(`/v1/admin/nav-categories/${id}`)

// ==================== 导航站点 ====================

export interface NavSite {
  id: number
  categoryId: number
  name: string
  url: string
  description: string
  icon: string
  sort: number
  clickCount: number
  status: number
}

export interface NavSiteQuery {
  page?: number
  size?: number
  name?: string
  categoryId?: number
  status?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export const listNavSites = (params: NavSiteQuery) =>
  request.get<unknown, PageResult<NavSite>>('/v1/admin/nav-sites', { params })

export const getNavSite = (id: number) =>
  request.get<unknown, NavSite>(`/v1/admin/nav-sites/${id}`)

export const createNavSite = (data: Partial<NavSite>) =>
  request.post<unknown, number>('/v1/admin/nav-sites', data)

export const updateNavSite = (id: number, data: Partial<NavSite>) =>
  request.put<unknown, void>(`/v1/admin/nav-sites/${id}`, data)

export const deleteNavSite = (id: number) =>
  request.delete<unknown, void>(`/v1/admin/nav-sites/${id}`)
