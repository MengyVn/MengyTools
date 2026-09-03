<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, WarningFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const errorMsg = ref('')
const form = reactive({
  username: '',
  password: ''
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
    errorMsg.value = ''
    try {
      await userStore.login(form)
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/'
      router.replace(redirect)
    } catch (e: unknown) {
      // 由登录页统一在表单下方红字提示，不弹右上角消息
      const err = e as { message?: string }
      errorMsg.value = err?.message || '登录失败，请稍后重试'
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="login-page">
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
        <!-- 错误提示：连续输错 5 次将锁定 IP -->
        <div v-if="errorMsg" class="error-tip">
          <el-icon><WarningFilled /></el-icon>
          <span>{{ errorMsg }}</span>
        </div>
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
  background: linear-gradient(135deg, #eef2f7 0%, #e3e9f2 100%);
}

.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  padding: 40px 36px 24px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #ebeef5;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.06);
}

.card-header {
  text-align: center;
  margin-bottom: 28px;

  .logo {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 52px;
    height: 52px;
    border-radius: 12px;
    background: #409eff;
  }
  .logo-icon {
    font-size: 26px;
    font-weight: 700;
    color: #fff;
  }
  .title {
    margin: 14px 0 4px;
    font-size: 22px;
    font-weight: 600;
    color: #303133;
    letter-spacing: 0.5px;
  }
  .subtitle {
    margin: 0;
    font-size: 13px;
    color: #909399;
  }
}

.error-tip {
  margin: 0 0 16px;
  padding: 9px 12px;
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  border-radius: 6px;
  color: #f56c6c;
  font-size: 13px;
  line-height: 1.4;
  display: flex;
  align-items: center;
  gap: 6px;
  .el-icon {
    flex-shrink: 0;
    font-size: 16px;
  }
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 4px;
  border: none;
  border-radius: 8px;
}

.card-footer {
  margin-top: 16px;
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
}

/* 小屏适配 */
@media (max-width: 480px) {
  .login-card {
    width: calc(100% - 32px);
    padding: 32px 24px 20px;
  }
}
</style>

