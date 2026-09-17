<script setup lang="ts">
/**
 * 社区风格的登录/注册弹窗。
 *
 * 与门户的 LoginModal 的区别：仅外观（使用社区设计令牌，无渐变/动效），
 * 校验规则与后端契约完全一致（复用 useAuth 与 SlideCaptcha，不重复实现顺序）。
 */
import { X } from 'lucide-vue-next'

const props = defineProps<{ visible: boolean; initialMode?: 'login' | 'register' }>()
const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void
  (e: 'logged-in'): void
}>()

const auth = useAuth()

const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const errMsg = ref('')
const okMsg = ref('')
const captchaToken = ref('')
const showCaptcha = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', confirmPassword: '', nickname: '' })

/** 与后端一致的校验规则 */
const PWD_RE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{8,32}$/
const NICK_RE = /^[\u4e00-\u9fa5A-Za-z]*$/

watch(
  () => props.visible,
  (v) => {
    if (v) {
      mode.value = props.initialMode || 'login'
      errMsg.value = ''
      okMsg.value = ''
      showCaptcha.value = false
      captchaToken.value = ''
      loading.value = false
    }
  },
  { immediate: true }
)

watch(mode, () => {
  errMsg.value = ''
  okMsg.value = ''
})

const close = () => emit('update:visible', false)

const switchMode = (m: 'login' | 'register') => {
  mode.value = m
}

const submitLogin = async () => {
  errMsg.value = ''
  if (!loginForm.username || !loginForm.password) {
    errMsg.value = '请输入用户名与密码'
    return
  }
  loading.value = true
  try {
    await auth.login(loginForm.username, loginForm.password)
    emit('logged-in')
    close()
  } catch (e: any) {
    errMsg.value = e?.data?.message || e?.message || '登录失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

const validateRegister = (): string | null => {
  if (!/^[A-Za-z0-9_]{3,32}$/.test(registerForm.username)) {
    return '用户名需 3-32 位字母、数字或下划线'
  }
  if (!PWD_RE.test(registerForm.password)) {
    return '密码需 8-32 位且包含大写字母、小写字母、数字与特殊符号'
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    return '两次输入的密码不一致'
  }
  if (registerForm.nickname && !NICK_RE.test(registerForm.nickname)) {
    return '昵称只能是中文或字母'
  }
  if (!captchaToken.value) {
    return '请先完成真人验证'
  }
  return null
}

const submitRegister = async () => {
  errMsg.value = ''
  const invalid = validateRegister()
  if (invalid) {
    errMsg.value = invalid
    return
  }
  loading.value = true
  try {
    await auth.register({
      username: registerForm.username,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      nickname: registerForm.nickname || undefined,
      captchaToken: captchaToken.value
    })
    // 注册成功直接登录，省去二次输入
    await auth.login(registerForm.username, registerForm.password)
    emit('logged-in')
    close()
  } catch (e: any) {
    errMsg.value = e?.data?.message || e?.message || '注册失败，请稍后再试'
    captchaToken.value = ''
    showCaptcha.value = false
  } finally {
    loading.value = false
  }
}

const onVerified = (token: string) => {
  captchaToken.value = token
  showCaptcha.value = false
  okMsg.value = '真人验证通过'
}

const onCaptchaFail = (msg: string) => {
  errMsg.value = msg || '验证失败，请重试'
}
</script>

<template>
  <div v-if="visible" class="auth-mask" @click.self="close">
    <div class="auth-card" role="dialog" aria-modal="true">
      <button type="button" class="auth-close" aria-label="关闭" @click="close">
        <X :size="16" />
      </button>

      <div class="auth-tabs">
        <button
          type="button"
          class="auth-tab"
          :class="{ active: mode === 'login' }"
          @click="switchMode('login')"
        >
          登录
        </button>
        <button
          type="button"
          class="auth-tab"
          :class="{ active: mode === 'register' }"
          @click="switchMode('register')"
        >
          注册
        </button>
      </div>

      <!-- 登录 -->
      <form v-if="mode === 'login'" class="auth-form" @submit.prevent="submitLogin">
        <label class="auth-label" for="c-login-username">用户名</label>
        <input
          id="c-login-username"
          v-model.trim="loginForm.username"
          class="auth-input"
          autocomplete="username"
          placeholder="用户名"
        />

        <label class="auth-label" for="c-login-password">密码</label>
        <input
          id="c-login-password"
          v-model="loginForm.password"
          class="auth-input"
          type="password"
          autocomplete="current-password"
          placeholder="密码"
        />

        <p v-if="errMsg" class="auth-msg err">{{ errMsg }}</p>
        <p v-if="okMsg" class="auth-msg ok">{{ okMsg }}</p>

        <button class="auth-submit" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登录' }}
        </button>

        <p class="auth-foot">
          还没有账号？
          <button type="button" class="auth-link" @click="switchMode('register')">立即注册</button>
        </p>
      </form>

      <!-- 注册 -->
      <form v-else class="auth-form" @submit.prevent="submitRegister">
        <label class="auth-label" for="c-reg-username">用户名</label>
        <input
          id="c-reg-username"
          v-model.trim="registerForm.username"
          class="auth-input"
          autocomplete="username"
          placeholder="3-32 位字母、数字或下划线"
        />

        <label class="auth-label" for="c-reg-password">密码</label>
        <input
          id="c-reg-password"
          v-model="registerForm.password"
          class="auth-input"
          type="password"
          autocomplete="new-password"
          placeholder="8-32 位，含大小写字母、数字与符号"
        />

        <label class="auth-label" for="c-reg-confirm">确认密码</label>
        <input
          id="c-reg-confirm"
          v-model="registerForm.confirmPassword"
          class="auth-input"
          type="password"
          autocomplete="new-password"
          placeholder="再次输入密码"
        />

        <label class="auth-label" for="c-reg-nickname">昵称（可选）</label>
        <input
          id="c-reg-nickname"
          v-model.trim="registerForm.nickname"
          class="auth-input"
          placeholder="中文或字母，不填则使用用户名"
        />

        <div class="auth-captcha">
          <div class="auth-captcha-head">
            <span class="auth-label inline">真人验证</span>
            <span v-if="captchaToken" class="auth-passed">已通过</span>
          </div>
          <button
            v-if="!captchaToken && !showCaptcha"
            type="button"
            class="auth-captcha-btn"
            @click="showCaptcha = true"
          >
            点击完成滑动验证
          </button>
          <SlideCaptcha v-if="showCaptcha" class="auth-captcha-body" @verified="onVerified" @fail="onCaptchaFail" />
        </div>

        <p v-if="errMsg" class="auth-msg err">{{ errMsg }}</p>
        <p v-if="okMsg" class="auth-msg ok">{{ okMsg }}</p>

        <button class="auth-submit" type="submit" :disabled="loading">
          {{ loading ? '注册中…' : '注册并登录' }}
        </button>

        <p class="auth-foot">
          已有账号？
          <button type="button" class="auth-link" @click="switchMode('login')">去登录</button>
        </p>
      </form>
    </div>
  </div>
</template>

<style scoped>
.auth-mask {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.42);
}
.auth-card {
  position: relative;
  width: 100%;
  max-width: 400px;
  padding: 20px 22px 18px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.16);
}
.auth-close {
  position: absolute;
  top: 12px;
  right: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 0;
  border-radius: var(--c-radius);
  background: transparent;
  color: var(--c-text-faint);
  cursor: pointer;
}
.auth-close:hover {
  background: var(--c-surface-alt);
  color: var(--c-text);
}

.auth-tabs {
  display: flex;
  gap: 18px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 16px;
}
.auth-tab {
  position: relative;
  padding: 0 0 9px;
  border: 0;
  background: transparent;
  color: var(--c-text-muted);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
}
.auth-tab.active {
  color: var(--c-text);
}
.auth-tab.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: var(--c-accent);
}

.auth-form {
  display: flex;
  flex-direction: column;
}
.auth-label {
  margin: 10px 0 5px;
  color: var(--c-text-muted);
  font-size: 13px;
}
.auth-label.inline {
  margin: 0;
}
.auth-input {
  height: 36px;
  padding: 0 10px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text);
  font-size: 14px;
  font-family: inherit;
  outline: none;
}
.auth-input:focus {
  border-color: var(--c-accent);
}
.auth-input::placeholder {
  color: var(--c-text-faint);
}

.auth-captcha {
  margin-top: 12px;
}
.auth-captcha-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.auth-passed {
  color: #2f9e44;
  font-size: 12.5px;
}
.auth-captcha-btn {
  width: 100%;
  height: 36px;
  border: 1px dashed var(--c-border-strong);
  border-radius: var(--c-radius);
  background: var(--c-surface-alt);
  color: var(--c-text-muted);
  font-size: 13.5px;
  cursor: pointer;
}
.auth-captcha-btn:hover {
  border-color: var(--c-accent);
  color: var(--c-accent);
}
.auth-captcha-body {
  margin-top: 4px;
}

.auth-msg {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.6;
}
.auth-msg.err {
  color: #d14343;
}
.auth-msg.ok {
  color: #2f9e44;
}

.auth-submit {
  height: 38px;
  margin-top: 16px;
  border: 1px solid var(--c-accent);
  border-radius: var(--c-radius);
  background: var(--c-accent);
  color: #fff;
  font-size: 14.5px;
  font-weight: 500;
  cursor: pointer;
}
.auth-submit:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.auth-foot {
  margin: 12px 0 0;
  color: var(--c-text-muted);
  font-size: 13px;
  text-align: center;
}
.auth-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--c-accent);
  font-size: 13px;
  cursor: pointer;
}
</style>
