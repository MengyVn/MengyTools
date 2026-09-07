/**
 * 门户用户认证状态：登录态、个人中心、注册。
 * token 持久化在 cookie（见 useApi），userInfo 走 useState，应用初始化时按需拉取。
 */

export interface PortalUser {
  id: number
  username: string
  nickname: string
  avatar: string
  email: string
  phone: string
  nicknameUpdateTime: string | null
}

export interface RegisterParams {
  username: string
  password: string
  confirmPassword: string
  nickname?: string
  email?: string
  phone?: string
  captchaToken: string
}

export const useAuth = () => {
  const api = useApi()
  const tokenCookie = useCookie<string | null>('portal_token')
  const rtCookie = useCookie<string | null>('portal_rt')

  // useState：SSR 首屏与客户端共享同一份，刷新页面会重置（按需 refetch）
  const user = useState<PortalUser | null>('portal_user', () => null)

  const isLoggedIn = computed(() => !!tokenCookie.value)

  /** 应用初始化时调用：若存在 token 则拉取个人中心信息 */
  const init = async () => {
    if (!tokenCookie.value || user.value) return
    try {
      user.value = await api.get<PortalUser>('/v1/portal/auth/profile')
    } catch {
      // token 失效（刷新也失败）→ 静默清登录态
      tokenCookie.value = null
      rtCookie.value = null
      user.value = null
    }
  }

  const login = async (username: string, password: string) => {
    const res = await api.post<{ accessToken: string; refreshToken: string }>(
      '/v1/auth/login',
      { username, password }
    )
    tokenCookie.value = res.accessToken
    rtCookie.value = res.refreshToken
    await init()
  }

  const register = async (params: RegisterParams) => {
    return api.post<{ username: string }>('/v1/portal/auth/register', params)
  }

  const logout = () => {
    tokenCookie.value = null
    rtCookie.value = null
    user.value = null
  }

  const fetchProfile = async () => {
    user.value = await api.get<PortalUser>('/v1/portal/auth/profile')
    return user.value
  }

  const updateProfile = async (data: {
    nickname?: string
    email?: string
    phone?: string
  }) => {
    await api.put('/v1/portal/auth/profile', data)
    await fetchProfile()
  }

  const uploadAvatar = async (file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post<{ url: string }>('/v1/portal/auth/avatar', fd)
  }

  return {
    user,
    isLoggedIn,
    token: tokenCookie,
    init,
    login,
    register,
    logout,
    fetchProfile,
    updateProfile,
    uploadAvatar
  }
}
