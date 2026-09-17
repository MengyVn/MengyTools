/**
 * 图片地址归一化。
 *
 * 背景：历史数据里封面图存在两种写法
 *   1) 相对路径        /images/xxx.png      ← 正确：dev 走 Nuxt 代理，生产走同源 nginx
 *   2) 绝对局域网地址  http://10.67.181.190:1096/images/xxx.png  ← 换域名/外网访问就会裂
 * 这里统一把「指向本机图片服务的绝对地址」收敛成相对路径，
 * 使同一份数据在 localhost、局域网 IP、正式域名下都能显示。
 * 其它外部图片地址（图床等）原样保留。
 */
export const IMAGE_PATH_PATTERN = /^https?:\/\/[^/]+:(?:1096|80|443)?(\/images\/.*)$/i

export const imageUrl = (url?: string | null): string => {
  if (!url) return ''
  const matched = IMAGE_PATH_PATTERN.exec(url.trim())
  return matched ? matched[1] : url.trim()
}

/** 是否为可用的图片地址（空串 / 空白视为无图） */
export const hasImage = (url?: string | null): boolean => !!url && url.trim().length > 0
