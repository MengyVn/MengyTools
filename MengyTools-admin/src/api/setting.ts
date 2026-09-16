import request from './request'

// ==================== 站点配置 ====================

export interface SettingItem {
  settingKey: string
  settingValue: string
  remark: string
  updateBy: string
  createTime: string
  updateTime: string
}

/** 全部配置项 */
export const listSettings = () => request.get<unknown, SettingItem[]>('/v1/admin/settings')

/** 批量更新（只传需要改的键） */
export const updateSettings = (values: Record<string, string>) =>
  request.put<unknown, void>('/v1/admin/settings', { values })

/** 已知配置键（与后端 SettingService 的白名单保持一致） */
export const SETTING_KEYS = {
  defaultLanding: 'site.default_landing',
  siteName: 'site.name',
  siteTagline: 'site.tagline'
} as const

/** 默认落地端可选值 */
export const LANDING_OPTIONS = [
  { label: '技术社区（/community）', value: 'community' },
  { label: '个人门户（/）', value: 'portal' }
]
