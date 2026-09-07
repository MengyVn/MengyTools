/**
 * 应用启动时按需拉取个人中心信息。
 * SSR 阶段：从请求 cookie 读取 token，拉取后写入 useState，首屏即可渲染头像；
 * 客户端 hydrate：若已存在则跳过，避免重复请求。
 */
export default defineNuxtPlugin(async () => {
  const auth = useAuth()
  if (auth.isLoggedIn.value) {
    await auth.init()
  }
})
