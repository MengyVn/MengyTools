/**
 * 统一 API 请求封装：服务端渲染走内网地址，客户端走公网地址。
 * need.md：纯 RESTful API，统一响应 { code, message, data }
 */
export const useApi = () => {
  const config = useRuntimeConfig()
  // 服务端渲染时使用 serverApiBase，浏览器端使用 public.apiBase
  const baseURL =
    typeof window === 'undefined' ? config.serverApiBase : config.public.apiBase

  const request = <T>(url: string, options?: any): Promise<T> => {
    return $fetch<{ code: number; message: string; data: T }>(url, {
      baseURL,
      ...options
    }).then((res) => {
      if (res.code === 200) {
        return res.data
      }
      throw new Error(res.message || '请求失败')
    })
  }

  return {
    get: <T>(url: string, options?: any) => request<T>(url, { method: 'GET', ...options }),
    post: <T>(url: string, body?: any, options?: any) =>
      request<T>(url, { method: 'POST', body, ...options }),
    put: <T>(url: string, body?: any, options?: any) =>
      request<T>(url, { method: 'PUT', body, ...options }),
    delete: <T>(url: string, options?: any) =>
      request<T>(url, { method: 'DELETE', ...options })
  }
}
