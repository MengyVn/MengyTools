/**
 * 统一 API 请求封装：服务端渲染走内网地址，客户端走公网地址。
 * need.md：纯 RESTful API，统一响应 { code, message, data }
 * 门户登录后自动注入 Authorization（从 cookie 读取，SSR 友好），
 * 401 时尝试用 refreshToken 续签并重试一次。
 */
export const useApi = () => {
  const config = useRuntimeConfig()
  // 服务端渲染时使用 serverApiBase，浏览器端使用 public.apiBase
  const baseURL =
    typeof window === 'undefined' ? config.serverApiBase : config.public.apiBase

  // token 存 cookie：SSR/客户端一致，刷新页面不丢失
  const tokenCookie = useCookie<string | null>('portal_token', {
    maxAge: 60 * 30, // 与后端 Access Token TTL 对齐
    sameSite: 'lax'
  })
  const rtCookie = useCookie<string | null>('portal_rt', {
    maxAge: 60 * 60 * 24 * 7, // 与后端 Refresh Token TTL 对齐
    sameSite: 'lax'
  })

  // 用 refreshToken 续签（不走 request 包装，避免递归）
  const doRefresh = async (): Promise<boolean> => {
    if (!rtCookie.value) return false
    try {
      const res = await $fetch<{ code: number; message: string; data: { accessToken: string; refreshToken: string } }>(
        '/v1/auth/refresh',
        { baseURL, method: 'POST', body: { refreshToken: rtCookie.value } }
      )
      if (res.code === 200 && res.data?.accessToken) {
        tokenCookie.value = res.data.accessToken
        rtCookie.value = res.data.refreshToken
        return true
      }
    } catch {
      // 续签失败：清登录态，交给上层处理
      tokenCookie.value = null
      rtCookie.value = null
    }
    return false
  }

  const request = async <T>(url: string, options?: any): Promise<T> => {
    const headers: Record<string, string> = { ...(options?.headers || {}) }
    if (tokenCookie.value) headers.Authorization = `Bearer ${tokenCookie.value}`

    try {
      const res = await $fetch<{ code: number; message: string; data: T }>(url, {
        baseURL,
        ...options,
        headers
      }).then((res) => res)
      if (res.code === 200) {
        return res.data
      }
      throw new Error(res.message || '请求失败')
    } catch (e: any) {
      // 401：Access Token 过期，尝试续签后重试一次
      const status = e?.response?.status || e?.statusCode
      if (status === 401 && !options?._retried) {
        const ok = await doRefresh()
        if (ok) {
          return request<T>(url, { ...options, _retried: true })
        }
      }
      throw e
    }
  }

  return {
    get: <T>(url: string, options?: any) => request<T>(url, { method: 'GET', ...options }),
    post: <T>(url: string, body?: any, options?: any) =>
      request<T>(url, { method: 'POST', body, ...options }),
    put: <T>(url: string, body?: any, options?: any) =>
      request<T>(url, { method: 'PUT', body, ...options }),
    delete: <T>(url: string, options?: any) =>
      request<T>(url, { method: 'DELETE', options })
  }
}
