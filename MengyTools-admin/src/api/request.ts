import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 15000
})

// 是否跳过自动 ElMessage 提示（由调用方自行处理）
function shouldSkipToast(config?: InternalAxiosRequestConfig): boolean {
  return config?.headers?.['X-Skip-Toast'] === '1'
}

// 请求拦截：附加 JWT
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：统一处理 { code, message, data }
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    // 业务错误：除非显式标记 skipToast，否则统一弹消息
    if (!shouldSkipToast(response.config)) {
      ElMessage.error(res.message || '请求失败')
    }
    // 401：Token 失效，跳登录
    if (res.code === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
    }
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    const status = error.response?.status
    const skipToast = shouldSkipToast(error.response?.config)
    if (status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
      if (!skipToast) ElMessage.error('登录已过期，请重新登录')
    } else if (status === 403) {
      // 403 有两种成因，给可操作的提示：
      //   1) 后端 CORS 白名单拒绝了当前访问地址（响应体为 Invalid CORS request）
      //   2) 当前账号缺少该操作所需的权限点
      const raw = String(error.response?.data?.message || error.response?.data || '')
      const corsBlocked = /invalid cors request/i.test(raw)
      const msg = corsBlocked
        ? `跨域来源被后端拒绝：请把当前访问地址（${window.location.origin}）加入后端 ALLOWED_ORIGINS 白名单并重启后端`
        : '无权访问：当前账号缺少该操作所需的权限点'
      if (!skipToast) ElMessage.error(msg)
      return Promise.reject(new Error(msg))
    } else if (!skipToast) {
      // 优先展示后端业务错误消息（Result{code,message}）
      const backendMsg = error.response?.data?.message
      ElMessage.error(backendMsg || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default service
