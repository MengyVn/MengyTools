<script setup lang="ts">
import { X, Eye, EyeOff, User, Lock, Mail, Phone, Check } from 'lucide-vue-next'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void
  (e: 'logged-in'): void
}>()

const auth = useAuth()

type Mode = 'login' | 'register'
const mode = ref<Mode>('login')
const loading = ref(false)
const errMsg = ref('')
const okMsg = ref('')

// 登录表单
const loginForm = reactive({ username: '', password: '' })
// 注册表单
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',
  phone: ''
})
const captchaToken = ref('')

// 密码可见性
const showPwd = ref(false)
const showPwd2 = ref(false)

const close = () => {
  emit('update:visible', false)
}

const switchMode = (m: Mode) => {
  mode.value = m
  errMsg.value = ''
  okMsg.value = ''
}

// 前端校验
const PWD_RE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{8,32}$/
const NICK_RE = /^[\u4e00-\u9fa5A-Za-z]*$/

const validateRegister = (): string | null => {
  if (!registerForm.username.trim()) return '请输入用户名'
  if (!/^[A-Za-z0-9_]{3,32}$/.test(registerForm.username))
    return '用户名仅支持英文、数字、下划线，3~32 位'
  if (!PWD_RE.test(registerForm.password))
    return '密码必须包含大小写字母、数字与符号，8~32 位'
  if (registerForm.password !== registerForm.confirmPassword)
    return '两次密码不一致'
  if (registerForm.nickname && !NICK_RE.test(registerForm.nickname))
    return '昵称仅支持中文与英文，不可含符号或空格'
  if (!captchaToken.value) return '请先完成真人验证'
  return null
}

const onCaptchaVerified = (token: string) => {
  captchaToken.value = token
}
const onCaptchaFail = () => {
  captchaToken.value = ''
}

// 密码强度提示
const strength = computed(() => {
  const p = registerForm.password
  if (!p) return { level: 0, text: '' }
  let score = 0
  if (p.length >= 8) score++
  if (/[a-z]/.test(p)) score++
  if (/[A-Z]/.test(p)) score++
  if (/\d/.test(p)) score++
  if (/[^A-Za-z0-9\s]/.test(p)) score++
  if (p.length >= 12) score++
  if (score <= 2) return { level: 1, text: '弱' }
  if (score <= 4) return { level: 2, text: '中' }
  return { level: 3, text: '强' }
})

const submit = async () => {
  errMsg.value = ''
  okMsg.value = ''
  if (mode.value === 'login') {
    if (!loginForm.username || !loginForm.password) {
      errMsg.value = '请输入用户名和密码'
      return
    }
    loading.value = true
    try {
      await auth.login(loginForm.username, loginForm.password)
      okMsg.value = '登录成功'
      emit('logged-in')
      setTimeout(close, 400)
    } catch (e: any) {
      errMsg.value = e?.message || '登录失败'
    } finally {
      loading.value = false
    }
    return
  }

  // 注册
  const err = validateRegister()
  if (err) {
    errMsg.value = err
    return
  }
  loading.value = true
  try {
    await auth.register({
      username: registerForm.username,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      nickname: registerForm.nickname || undefined,
      email: registerForm.email || undefined,
      phone: registerForm.phone || undefined,
      captchaToken: captchaToken.value
    })
    okMsg.value = '注册成功，请登录'
    switchMode('login')
    loginForm.username = registerForm.username
    loginForm.password = ''
  } catch (e: any) {
    errMsg.value = e?.message || '注册失败'
    // 验证码失效：通过 key 重置 SlideCaptcha
    captchaToken.value = ''
    captchaResetKey.value++
  } finally {
    loading.value = false
  }
}

// 用于重置验证码组件
const captchaResetKey = ref(0)
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay" @click.self="close">
        <div class="modal-card">
          <button class="close-btn" @click="close"><X :size="18" /></button>

          <div class="tabs">
            <button :class="{ active: mode === 'login' }" @click="switchMode('login')">登录</button>
            <button :class="{ active: mode === 'register' }" @click="switchMode('register')">注册</button>
          </div>

          <!-- 登录 -->
          <form v-if="mode === 'login'" class="form" @submit.prevent="submit">
            <label class="field">
              <User :size="16" class="ico" />
              <input v-model="loginForm.username" type="text" placeholder="用户名" autocomplete="username" />
            </label>
            <label class="field">
              <Lock :size="16" class="ico" />
              <input
                v-model="loginForm.password"
                :type="showPwd ? 'text' : 'password'"
                placeholder="密码"
                autocomplete="current-password"
              />
              <button type="button" class="eye" @click="showPwd = !showPwd">
                <Eye v-if="!showPwd" :size="16" /><EyeOff v-else :size="16" />
              </button>
            </label>

            <p v-if="errMsg" class="err">{{ errMsg }}</p>
            <p v-if="okMsg" class="ok">{{ okMsg }}</p>

            <button class="submit" :disabled="loading">{{ loading ? '登录中…' : '登录' }}</button>

            <p class="switch-tip">
              还未注册？<a @click="switchMode('register')">点击此处注册账号</a>
            </p>
          </form>

          <!-- 注册 -->
          <form v-else class="form" @submit.prevent="submit">
            <label class="field">
              <User :size="16" class="ico" />
              <input v-model="registerForm.username" type="text" placeholder="用户名 *（英文/数字/下划线 3~32）" />
            </label>
            <label class="field">
              <Lock :size="16" class="ico" />
              <input
                v-model="registerForm.password"
                :type="showPwd ? 'text' : 'password'"
                placeholder="密码 *（大小写字母+数字+符号 8~32）"
                autocomplete="new-password"
              />
              <button type="button" class="eye" @click="showPwd = !showPwd">
                <Eye v-if="!showPwd" :size="16" /><EyeOff v-else :size="16" />
              </button>
            </label>
            <div v-if="registerForm.password" class="strength">
              <div class="bar" :class="['l' + strength.level]"></div>
              <span>强度：{{ strength.text }}</span>
            </div>
            <label class="field">
              <Lock :size="16" class="ico" />
              <input
                v-model="registerForm.confirmPassword"
                :type="showPwd2 ? 'text' : 'password'"
                placeholder="再次确认密码 *"
                autocomplete="new-password"
              />
              <button type="button" class="eye" @click="showPwd2 = !showPwd2">
                <Eye v-if="!showPwd2" :size="16" /><EyeOff v-else :size="16" />
              </button>
            </label>
            <label class="field">
              <Check :size="16" class="ico" />
              <input v-model="registerForm.nickname" type="text" placeholder="昵称（选填，仅中文/英文）" />
            </label>
            <label class="field">
              <Mail :size="16" class="ico" />
              <input v-model="registerForm.email" type="email" placeholder="邮箱（选填）" />
            </label>
            <label class="field">
              <Phone :size="16" class="ico" />
              <input v-model="registerForm.phone" type="tel" placeholder="手机号（选填）" />
            </label>

            <div class="captcha-wrap">
              <SlideCaptcha :key="captchaResetKey" @verified="onCaptchaVerified" @fail="onCaptchaFail" />
            </div>

            <p v-if="errMsg" class="err">{{ errMsg }}</p>
            <p v-if="okMsg" class="ok">{{ okMsg }}</p>

            <button class="submit" :disabled="loading || !captchaToken">
              {{ loading ? '注册中…' : '注册' }}
            </button>

            <p class="switch-tip">
              已有账号？<a @click="switchMode('login')">直接登录</a>
            </p>
          </form>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 16px;
}
.modal-card {
  position: relative;
  width: 380px;
  max-width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  background: var(--bg-color);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: 24px 22px 20px;
}
.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
}
.close-btn:hover {
  color: var(--text-color);
  background: var(--bg-hover);
}
.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 18px;
  border-bottom: 1px solid var(--border-light);
}
.tabs button {
  flex: 1;
  padding: 8px 0;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  border-bottom: 2px solid transparent;
}
.tabs button.active {
  color: var(--primary);
  border-bottom-color: var(--primary);
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
.field .ico {
  color: var(--text-tertiary);
  flex-shrink: 0;
}
.field input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  padding: 10px 8px;
  font-size: 14px;
  color: var(--text-color);
  min-width: 0;
}
.eye {
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 4px;
  display: flex;
}
.strength {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-tertiary);
  padding: 0 2px;
}
.bar {
  flex: 1;
  height: 4px;
  border-radius: 2px;
  background: var(--bg-hover);
  position: relative;
}
.bar.l1 {
  background: linear-gradient(90deg, #ef4444 33%, var(--bg-hover) 33%);
}
.bar.l2 {
  background: linear-gradient(90deg, #f59e0b 66%, var(--bg-hover) 66%);
}
.bar.l3 {
  background: #10b981;
}
.err {
  margin: 0;
  font-size: 12px;
  color: #ef4444;
}
.ok {
  margin: 0;
  font-size: 12px;
  color: #10b981;
}
.submit {
  margin-top: 4px;
  padding: 11px;
  border: none;
  border-radius: var(--radius);
  background: linear-gradient(135deg, var(--primary), var(--accent));
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.1s;
}
.submit:hover:not(:disabled) {
  opacity: 0.92;
}
.submit:active:not(:disabled) {
  transform: scale(0.99);
}
.submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.switch-tip {
  text-align: center;
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}
.switch-tip a {
  color: var(--primary);
  cursor: pointer;
  text-decoration: none;
  font-weight: 500;
}
.switch-tip a:hover {
  text-decoration: underline;
}
.captcha-wrap {
  display: flex;
  justify-content: center;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s;
}
.modal-enter-active .modal-card,
.modal-leave-active .modal-card {
  transition: transform 0.22s var(--ease-out, ease), opacity 0.22s;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-from .modal-card,
.modal-leave-to .modal-card {
  transform: translateY(12px) scale(0.98);
  opacity: 0;
}
</style>
