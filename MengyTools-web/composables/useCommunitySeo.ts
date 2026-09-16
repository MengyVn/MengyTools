/**
 * 社区 SEO 工具：canonical 链接与 JSON-LD 结构化数据。
 *
 * 为什么需要：社区是面向搜索引擎的公开内容，需要
 *   1) canonical 收敛重复 URL（分页参数、排序参数）；
 *   2) 结构化数据（WebSite/BlogPosting/BreadcrumbList/ProfilePage）帮助富摘要展示；
 *   3) 分页/搜索页不被索引（见各页 useSeoMeta 的 robots）。
 */
export const useCommunitySeo = () => {
  const url = useRequestURL()

  /** 生成本站绝对 URL；传入 path 可覆盖（用于分页/排序参数收敛到规范路径） */
  const absolute = (path?: string): string => {
    const p = path ?? url.pathname
    const normalized = p.length > 1 ? p.replace(/\/+$/, '') : p
    return `${url.origin}${normalized}`
  }

  /** JSON-LD 脚本节点 */
  const jsonLd = (data: Record<string, unknown>) => ({
    type: 'application/ld+json',
    innerHTML: JSON.stringify(data)
  })

  /** ISO 时间（'2026-09-02 14:29:57' 这类后端格式 → ISO） */
  const toIso = (s?: string | null): string | undefined => {
    if (!s) return undefined
    const t = new Date(String(s).replace(' ', 'T'))
    return Number.isNaN(t.getTime()) ? undefined : t.toISOString()
  }

  return { absolute, jsonLd, toIso }
}
