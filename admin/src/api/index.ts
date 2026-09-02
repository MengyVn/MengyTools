import request from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
}

// 登录
export const login = (data: LoginParams) =>
  request.post<unknown, LoginResult>('/v1/auth/login', data)

// 刷新 Token
export const refresh = (refreshToken: string) =>
  request.post<unknown, LoginResult>('/v1/auth/refresh', { refreshToken })

// 获取当前用户信息
export const getUserInfo = () => request.get<unknown, UserInfo>('/v1/auth/me')

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  roles: string[]
  permissions: string[]
}
