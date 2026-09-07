<script setup lang="ts">
import { User as UserIcon, UserCircle, LogOut, ChevronDown } from 'lucide-vue-next'

const auth = useAuth()
const router = useRouter()

const showLogin = ref(false)
const showDropdown = ref(false)
const menuRef = ref<HTMLElement | null>(null)

const avatarSrc = computed(() => auth.user.value?.avatar || '')
// 昵称首字作为无图占位
const initial = computed(() => {
  const n = auth.user.value?.nickname || auth.user.value?.username || '?'
  return n.charAt(0).toUpperCase()
})

const goProfile = () => {
  showDropdown.value = false
  router.push('/profile')
}

const onLogout = () => {
  showDropdown.value = false
  auth.logout()
  router.push('/')
}

const handleClickOutside = (e: MouseEvent) => {
  if (menuRef.value && !menuRef.value.contains(e.target as Node)) {
    showDropdown.value = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onBeforeUnmount(() => document.removeEventListener('click', handleClickOutside))
</script>

<template>
  <div class="user-menu" ref="menuRef">
    <!-- 未登录：登录按钮 -->
    <button v-if="!auth.user.value" class="login-btn" @click="showLogin = true">
      <UserIcon :size="18" />
      <span class="login-text">登录</span>
    </button>

    <!-- 已登录：头像 + 下拉 -->
    <div v-else class="avatar-wrap" @click="goProfile">
      <div class="avatar">
        <img v-if="avatarSrc" :src="avatarSrc" :alt="auth.user.value.nickname" />
        <span v-else class="fallback">{{ initial }}</span>
      </div>
      <ChevronDown :size="14" class="caret" @click.stop="showDropdown = !showDropdown" />

      <Transition name="dropdown">
        <div v-if="showDropdown" class="dropdown" @click.stop>
          <div class="dropdown-header">
            <div class="dropdown-avatar">
              <img v-if="avatarSrc" :src="avatarSrc" :alt="auth.user.value.nickname" />
              <span v-else class="fallback">{{ initial }}</span>
            </div>
            <div class="dropdown-name">
              <strong>{{ auth.user.value.nickname }}</strong>
              <small>{{ auth.user.value.username }}</small>
            </div>
          </div>
          <button class="dropdown-item" @click="goProfile">
            <UserCircle :size="16" /> 个人中心
          </button>
          <button class="dropdown-item danger" @click="onLogout">
            <LogOut :size="16" /> 退出登录
          </button>
        </div>
      </Transition>
    </div>

    <LoginModal v-model:visible="showLogin" />
  </div>
</template>

<style scoped>
.user-menu {
  position: relative;
}
.login-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  background: var(--bg-color);
  color: var(--text-color);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.login-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: rgba(99, 102, 241, 0.06);
}
.avatar-wrap {
  display: flex;
  align-items: center;
  gap: 2px;
  cursor: pointer;
  padding: 2px;
  border-radius: 30px;
  transition: background 0.2s;
}
.avatar-wrap:hover {
  background: var(--bg-hover);
}
.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  flex-shrink: 0;
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.fallback {
  color: #fff;
}
.caret {
  color: var(--text-tertiary);
  transition: transform 0.2s;
}
.dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 220px;
  background: var(--bg-color);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  box-shadow: var(--shadow-lg);
  padding: 6px;
  z-index: 200;
}
.dropdown-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-bottom: 1px solid var(--border-light);
  margin-bottom: 4px;
}
.dropdown-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}
.dropdown-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.dropdown-name {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.dropdown-name strong {
  font-size: 14px;
  color: var(--text-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dropdown-name small {
  font-size: 12px;
  color: var(--text-tertiary);
}
.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 10px;
  border: none;
  background: transparent;
  color: var(--text-color);
  font-size: 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.15s;
}
.dropdown-item:hover {
  background: var(--bg-hover);
}
.dropdown-item.danger:hover {
  background: rgba(239, 68, 68, 0.08);
  color: #ef4444;
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.18s, transform 0.18s;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
