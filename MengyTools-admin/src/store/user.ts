import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, getUserInfo, type LoginParams, type UserInfo } from '@/api'
import { usePermissionStore } from '@/store/permission'
import { resetRouter } from '@/router'

export const useUserStore = defineStore(
  'user',
  () => {
    const token = ref<string>('')
    const refreshToken = ref<string>('')
    const userInfo = ref<UserInfo | null>(null)

    const login = async (params: LoginParams) => {
      const res = await loginApi(params)
      token.value = res.accessToken
      refreshToken.value = res.refreshToken
    }

    const fetchUserInfo = async () => {
      userInfo.value = await getUserInfo()
      return userInfo.value
    }

    const logout = () => {
      token.value = ''
      refreshToken.value = ''
      userInfo.value = null
      // 清除动态路由与权限状态，避免切换账号残留
      usePermissionStore().reset()
      resetRouter()
    }

    return { token, refreshToken, userInfo, login, fetchUserInfo, logout }
  },
  {
    // 持久化 token，刷新页面不丢失
    persist: {
      key: 'mengy-admin-user',
      pick: ['token', 'refreshToken']
    }
  }
)
