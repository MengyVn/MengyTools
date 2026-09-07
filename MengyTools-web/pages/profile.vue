<script setup lang="ts">
import { Camera, User as UserIcon, Mail, Phone, Check, Lock } from 'lucide-vue-next'

definePageMeta({ title: '个人中心' })
useSeoMeta({ title: '个人中心 - MengyTools' })

const auth = useAuth()

const showLogin = ref(false)
const loading = ref(false)
const saving = ref(false)
const msg = ref('')
const msgType = ref<'ok' | 'err'>('ok')
const fileInput = ref<HTMLInputElement | null>(null)

// 编辑表单（独立于 store，避免未保存改动污染全局）
const form = reactive({
  nickname: '',
  email: '',
  phone: ''
})

const COOLDOWN_DAYS = 3
const cooldownEndsAt = computed(() => {
  const t = auth.user.value?.nicknameUpdateTime
  if (!t) return null
  return new Date(t).getTime() + COOLDOWN_DAYS * 24 * 3600 * 1000
})
const nicknameLocked = computed(() => {
  return !!cooldownEndsAt.value && cooldownEndsAt.value > Date.now()
})
const nicknameHint = computed(() => {
  if (!cooldownEndsAt.value) return '昵称 3 天可改一次'
  const ms = cooldownEndsAt.value - Date.now()
  if (ms <= 0) return '可修改'
  const h = Math.ceil(ms / 3600000)
  if (h < 24) return `昵称冷却中，剩余约 ${h} 小时`
  const d = Math.ceil(h / 24)
  return `昵称冷却中，剩余约 ${d} 天`
})

const fillForm = () => {
  const u = auth.user.value
  form.nickname = u?.nickname || ''
  form.email = u?.email || ''
  form.phone = u?.phone || ''
}

onMounted(async () => {
  if (!auth.isLoggedIn.value) return
  loading.value = true
  try {
    await auth.fetchProfile()
    fillForm()
  } finally {
    loading.value = false
  }
})

const onAvatarClick = () => fileInput.value?.click()
const onFileChange = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  msg.value = ''
  loading.value = true
  try {
    await auth.uploadAvatar(file)
    await auth.fetchProfile()
    msgType.value = 'ok'
    msg.value = '头像已更新'
  } catch (err: any) {
    msgType.value = 'err'
    msg.value = err?.message || '头像上传失败'
  } finally {
    loading.value = false
    input.value = ''
  }
}

const save = async () => {
  msg.value = ''
  msgType.value = 'ok'
  // 昵称：未改动或锁定时不传
  const payload: { nickname?: string; email?: string; phone?: string } = {
    email: form.email,
    phone: form.phone
  }
  if (!nicknameLocked.value && form.nickname && form.nickname !== auth.user.value?.nickname) {
    payload.nickname = form.nickname
  }
  saving.value = true
  try {
    await auth.updateProfile(payload)
    fillForm()
    msgType.value = 'ok'
    msg.value = '保存完成'
  } catch (err: any) {
    msgType.value = 'err'
    msg.value = err?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

const avatarSrc = computed(() => auth.user.value?.avatar || '')
const initial = computed(() => {
  const n = auth.user.value?.nickname || auth.user.value?.username || '?'
  return n.charAt(0).toUpperCase()
})
</script>

<template>
  <div class="container profile-page">
    <!-- 未登录 -->
    <div v-if="!auth.user.value && !auth.isLoggedIn.value" class="card login-cta">
      <Lock :size="40" class="empty-icon" />
      <h2>请先登录</h2>
      <p>登录后即可管理你的个人资料</p>
      <button class="btn-primary" @click="showLogin = true">去登录</button>
      <LoginModal v-model:visible="showLogin" />
    </div>

    <!-- 加载中 -->
    <div v-else-if="loading && !auth.user.value" class="card login-cta">
      <p>加载中…</p>
    </div>

    <!-- 个人中心 -->
    <div v-else-if="auth.user.value" class="card profile-card">
      <h1 class="page-title">个人中心</h1>

      <div class="avatar-block">
        <div class="avatar" @click="onAvatarClick">
          <img v-if="avatarSrc" :src="avatarSrc" :alt="auth.user.value.nickname" />
          <span v-else class="fallback">{{ initial }}</span>
          <span class="cam"><Camera :size="18" /></span>
        </div>
        <p class="avatar-tip">点击头像修改</p>
        <input
          ref="fileInput"
          type="file"
          accept="image/png,image/jpeg,image/gif,image/webp,image/bmp"
          hidden
          @change="onFileChange"
        />
      </div>

      <div class="form">
        <label class="field readonly">
          <UserIcon :size="16" class="ico" />
          <input :value="auth.user.value.username" readonly placeholder="用户名" />
          <span class="badge">不可修改</span>
        </label>

        <label class="field" :class="{ disabled: nicknameLocked }">
          <Check :size="16" class="ico" />
          <input
            v-model="form.nickname"
            type="text"
            placeholder="昵称（仅中文/英文）"
            :disabled="nicknameLocked"
          />
        </label>
        <p class="hint">{{ nicknameHint }}</p>

        <label class="field">
          <Mail :size="16" class="ico" />
          <input v-model="form.email" type="email" placeholder="邮箱" />
        </label>

        <label class="field">
          <Phone :size="16" class="ico" />
          <input v-model="form.phone" type="tel" placeholder="手机号" />
        </label>

        <p v-if="msg" class="msg" :class="msgType">{{ msg }}</p>

        <button class="btn-primary" :disabled="saving">{{ saving ? '保存中…' : '保存' }}</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 560px;
}
.login-cta {
  text-align: center;
  padding: 48px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.login-cta h2 {
  margin: 4px 0 0;
  font-size: 18px;
}
.login-cta p {
  margin: 0 0 8px;
  color: var(--text-secondary);
  font-size: 14px;
}
.profile-card {
  padding: 28px 28px 24px;
}
.avatar-block {
  text-align: center;
  margin-bottom: 24px;
}
.avatar {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 auto 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: #fff;
  font-size: 40px;
  font-weight: 700;
  cursor: pointer;
  border: 3px solid var(--bg-color);
  box-shadow: var(--shadow);
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cam {
  position: absolute;
  right: 2px;
  bottom: 2px;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--bg-color);
}
.avatar-tip {
  margin: 0;
  font-size: 12px;
  color: var(--text-tertiary);
}
.form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.field {
  position: relative;
  display: flex;
  align-items: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  background: var(--bg-color);
  padding: 0 12px;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.field:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}
.field.readonly {
  background: var(--bg-hover);
}
.field.disabled {
  opacity: 0.6;
}
.field .ico {
  color: var(--text-tertiary);
  flex-shrink: 0;
}
.field input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  padding: 11px 8px;
  font-size: 14px;
  color: var(--text-color);
  min-width: 0;
}
.field input:read-only {
  cursor: default;
}
.badge {
  font-size: 11px;
  color: var(--text-tertiary);
  background: var(--bg-hover);
  padding: 2px 8px;
  border-radius: 4px;
  flex-shrink: 0;
}
.hint {
  margin: -6px 2px 0;
  font-size: 12px;
  color: var(--text-tertiary);
}
.msg {
  margin: 0;
  font-size: 13px;
}
.msg.ok {
  color: #10b981;
}
.msg.err {
  color: #ef4444;
}
.btn-primary {
  margin-top: 4px;
  padding: 12px;
  border: none;
  border-radius: var(--radius);
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}
.btn-primary:hover:not(:disabled) {
  opacity: 0.92;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
