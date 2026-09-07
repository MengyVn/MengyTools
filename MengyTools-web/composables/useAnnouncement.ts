/**
 * 公告相关 API 封装：当前生效列表、历史分页、详情、阅读量埋点。
 * 与 useApi 一致：服务端走内网、客户端走公网；统一响应 { code, message, data }。
 */

/** 公告列表项（与后端契约一致） */
export interface AnnouncementItem {
  id: number
  title: string
  contentFormat: string
  isPersistent: number
  /** 是否滚动出现在首页顶部跑马灯：1是 0否 */
  isMarquee: number
  /** 跑马灯显示时长(分钟)：0=一直显示直到手动关闭 */
  displayDuration: number
  publishTime: string | null
  expireTime: string | null
  status: number
  viewCount: number
  createTime: string
}

/** 公告详情：在列表项基础上额外含正文 content */
export interface AnnouncementDetail extends AnnouncementItem {
  content: string
}

/** 历史分页响应 */
export interface AnnouncementPage {
  records: AnnouncementItem[]
  total: number
}

export const useAnnouncement = () => {
  const api = useApi()

  return {
    /** 当前生效公告（铃铛下拉用，所有已发布且已到发布时间） */
    fetchActive: () =>
      api.get<AnnouncementItem[]>('/v1/portal/announcements/active'),
    /** 跑马灯公告（首页顶部滚动条用，is_marquee=1 的已发布公告） */
    fetchMarquee: () =>
      api.get<AnnouncementItem[]>('/v1/portal/announcements/marquee'),
    /** 历史公告分页 */
    fetchPage: (page = 1, size = 10) =>
      api.get<AnnouncementPage>('/v1/portal/announcements', {
        params: { page, size }
      }),
    /** 公告详情（含 content） */
    fetchDetail: (id: string | number) =>
      api.get<AnnouncementDetail>(`/v1/portal/announcements/${id}`),
    /** 阅读量埋点：后端按「公告+访客ID」去重计数 */
    trackView: (announcementId: number, visitorId: string) =>
      api.post<boolean>('/v1/track/announcement/view', {
        announcementId,
        visitorId
      })
  }
}
