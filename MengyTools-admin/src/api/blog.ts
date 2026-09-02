import request from './request'

// ==================== 文章管理 ====================

export interface ArticleListItem {
  id: number
  title: string
  summary: string
  cover: string
  categoryId: number | null
  categoryName: string | null
  viewCount: number
  isTop: number
  publishTime: string | null
  createTime: string
}

export interface ArticleDetail {
  id: number
  title: string
  summary: string
  content: string
  cover: string
  categoryId: number | null
  categoryName: string | null
  viewCount: number
  isTop: number | null
  status: number
  publishTime: string | null
  createTime: string
}

export interface ArticleForm {
  title: string
  summary: string
  content: string
  cover: string
  categoryId: number | null
  status: number
  isTop: number
  publishTime: string | null
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

export interface ArticleQuery {
  page?: number
  size?: number
  title?: string
  status?: number
  categoryId?: number
}

// 分页列表
export const listArticles = (params: ArticleQuery) =>
  request.get<unknown, PageResult<ArticleListItem>>('/v1/admin/articles', { params })

// 详情
export const getArticle = (id: number) =>
  request.get<unknown, ArticleDetail>(`/v1/admin/articles/${id}`)

// 新增
export const createArticle = (data: ArticleForm) =>
  request.post<unknown, number>('/v1/admin/articles', data)

// 修改
export const updateArticle = (id: number, data: ArticleForm) =>
  request.put<unknown, void>(`/v1/admin/articles/${id}`, data)

// 删除
export const deleteArticle = (id: number) =>
  request.delete<unknown, void>(`/v1/admin/articles/${id}`)

// 切换置顶
export const toggleArticleTop = (id: number, top: boolean) =>
  request.put<unknown, void>(`/v1/admin/articles/${id}/top`, null, { params: { top } })

// ==================== 博客分类 ====================

export interface BlogCategory {
  id: number
  name: string
  slug: string
  sort: number
}

export const listBlogCategories = () =>
  request.get<unknown, BlogCategory[]>('/v1/admin/blog-categories')

export const createBlogCategory = (data: Partial<BlogCategory>) =>
  request.post<unknown, number>('/v1/admin/blog-categories', data)

export const updateBlogCategory = (id: number, data: Partial<BlogCategory>) =>
  request.put<unknown, void>(`/v1/admin/blog-categories/${id}`, data)

export const deleteBlogCategory = (id: number) =>
  request.delete<unknown, void>(`/v1/admin/blog-categories/${id}`)
