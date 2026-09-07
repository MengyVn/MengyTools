import request from './request'

// ==================== 公告管理 ====================

export interface AnnouncementItem {
  id: number
  title: string
  /** 内容格式 markdown / html */
  contentFormat: string
  /** 1常驻 0非常驻 */
  isPersistent: number
  /** 是否滚动出现在首页顶部跑马灯：1是 0否 */
  isMarquee: number
  /** 跑马灯显示时长(分钟)：0=一直显示直到手动关闭 */
  displayDuration: number
  /** 'yyyy-MM-dd HH:mm:ss' 或 null */
  publishTime: string | null
  expireTime: string | null
  /** 0草稿 1已发布 2定时中 3已下线 */
  status: number
  viewCount: number
  createTime: string
}

export interface AnnouncementDetail extends AnnouncementItem {
  content: string
}

export interface AnnouncementForm {
  title: string
  content: string
  contentFormat: 'markdown' | 'html'
  /** 0 或 1 */
  isPersistent: number
  /** 是否滚动出现在首页顶部跑马灯：1是 0否 */
  isMarquee: number
  /** 跑马灯显示时长(分钟)：0=一直显示直到手动关闭 */
  displayDuration: number
  publishTime: string | null
  expireTime: string | null
  status: number
}

export interface AnnouncementQuery {
  page?: number
  size?: number
  title?: string
  status?: number
}

// 分页列表
export const listAnnouncements = (params: AnnouncementQuery) =>
  request.get<unknown, { records: AnnouncementItem[]; total: number }>('/v1/admin/announcements', { params })

// 详情
export const getAnnouncement = (id: number) =>
  request.get<unknown, AnnouncementDetail>(`/v1/admin/announcements/${id}`)

// 新增
export const createAnnouncement = (data: AnnouncementForm) =>
  request.post<unknown, number>('/v1/admin/announcements', data)

// 修改
export const updateAnnouncement = (id: number, data: AnnouncementForm) =>
  request.put<unknown, void>(`/v1/admin/announcements/${id}`, data)

// 删除
export const deleteAnnouncement = (id: number) =>
  request.delete<unknown, void>(`/v1/admin/announcements/${id}`)
