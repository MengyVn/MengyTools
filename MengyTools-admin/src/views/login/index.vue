<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'admin123'
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async (formEl?: FormInstance) => {
  if (!formEl) return
  await formEl.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login(form)
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/'
      router.replace(redirect)
    } catch {
      // 错误已由拦截器统一提示
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="login-page">
    <!-- 背景光斑 -->
    <div class="blob blob-1" />
    <div class="blob blob-2" />
    <div class="blob blob-3" />

    <div class="login-card">
      <!-- 头部 -->
      <div class="card-header">
        <div class="logo">
          <span class="logo-icon">M</span>
        </div>
        <h1 class="title">MengyTools</h1>
        <p class="subtitle">个人数字资产管理后台</p>
      </div>

      <!-- 表单 -->
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin(formRef)"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin(formRef)"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="card-footer">
        <span>© 2026 MengyTools</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  position: relative;
  height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: linear-gradient(135deg, #1e3a8a 0%, #4c1d95 50%, #831843 100%);
}

/* 背景光斑 */
.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: float 18s ease-in-out infinite;
}
.blob-1 {
  width: 480px;
  height: 480px;
  background: #6366f1;
  top: -120px;
  left: -80px;
}
.blob-2 {
  width: 420px;
  height: 420px;
  background: #ec4899;
  bottom: -100px;
  right: -60px;
  animation-delay: -6s;
}
.blob-3 {
  width: 360px;
  height: 360px;
  background: #06b6d4;
  top: 40%;
  left: 50%;
  animation-delay: -12s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(40px, -30px) scale(1.05); }
  66% { transform: translate(-30px, 40px) scale(0.95); }
}

.login-card {
  position: relative;
  z-index: 1;
  width: 420px;
  padding: 40px 36px 28px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.35);
}

.card-header {
  text-align: center;
  margin-bottom: 28px;

  .logo {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    border-radius: 16px;
    background: linear-gradient(135deg, #6366f1, #ec4899);
    box-shadow: 0 8px 24px rgba(99, 102, 241, 0.4);
  }
  .logo-icon {
    font-size: 26px;
    font-weight: 700;
    color: #fff;
    letter-spacing: -1px;
  }
  .title {
    margin: 14px 0 4px;
    font-size: 22px;
    font-weight: 600;
    color: #fff;
    letter-spacing: 0.5px;
  }
  .subtitle {
    margin: 0;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.65);
  }
}

/* Element Plus 暗背景下的输入框样式覆盖 */
:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  box-shadow: none !important;
  transition: all 0.25s;

  &:hover {
    border-color: rgba(255, 255, 255, 0.3);
  }
  &.is-focus {
    border-color: #6366f1;
    background: rgba(255, 255, 255, 0.12);
  }
}
:deep(.el-input__inner) {
  color: #fff;
  height: 44px;
  &::placeholder {
    color: rgba(255, 255, 255, 0.45);
  }
}
:deep(.el-input__prefix-inner) {
  color: rgba(255, 255, 255, 0.55);
  font-size: 16px;
}
:deep(.el-input__suffix-inner) {
  color: rgba(255, 255, 255, 0.55);
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 4px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #6366f1 0%, #ec4899 100%);
  background-size: 200% 200%;
  background-position: 0% 0%;
  transition: background-position 0.4s, transform 0.15s, box-shadow 0.25s;

  &:hover {
    background-position: 100% 100%;
    box-shadow: 0 10px 28px rgba(99, 102, 241, 0.45);
  }
  &:active {
    transform: scale(0.98);
  }
}

.card-footer {
  margin-top: 18px;
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

/* 小屏适配 */
@media (max-width: 480px) {
  .login-card {
    width: calc(100% - 32px);
    padding: 32px 24px 20px;
  }
}
</style>
