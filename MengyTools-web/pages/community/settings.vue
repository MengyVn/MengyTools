<script setup lang="ts">
/**
 * 社区 - 个人设置（社区风格）。
 *
 * 与门户 /profile 的分工：
 *   - 本页属于社区，使用社区布局与设计令牌；
 *   - 账号级字段（昵称/邮箱/手机/头像）复用门户账号接口（同一套 sys_user），
 *     社区独有字段（个人签名）走 /v1/community/profile；
 *   - 门户的 /profile 页面保持原样，不做改动。
 */
import { Camera, Save, MessageSquare, User } from 'lucide-vue-next'
import { useCommunity } from '~/composables/useCommunity'
import { useCommunitySite } from '~/composables/useCommunitySite'
import { imageUrl } from '~/utils/image'

definePageMeta({ layout: 'community' })

const { user, isLoggedIn, init, fetchProfile, updateProfile, uploadAvatar } = useAuth()
const { siteName } = useCommunitySite()
const { user: fetchPublicProfile, updateSignature } = useCommunity()

const showLogin = ref(false)
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const errMsg = ref('')
const okMsg = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

const form = reactive({ nickname: '', email: '', phone: '', signature: '' })
const meta = reactive<{ username: string; createTime: string; nicknameUpdateTime: string | null }>({
  username: '',
  createTime: '',
  nicknameUpdateTime: null
})

onMounted(async () => {
  await init()
  if (isLoggedIn.value) await load()
})

const load = async () => {
  loading.value = true
  try {
    const profile = await fetchProfile()
    form.nickname = profile?.nickname || ''
    form.email = profile?.email || ''
    form.phone = profile?.phone || ''
    meta.username = profile?.username || ''
    meta.nicknameUpdateTime = profile?.nicknameUpdateTime || null
    // 签名来自社区公开资料
    if (user.value?.id) {
      const pub = await fetchPublicProfile(user.value.id)
      form.signature = pub?.signature || ''
      meta.createTime = pub?.createTime || ''
    }
  } catch {
    errMsg.value = '资料加载失败，请刷新重试'
  } finally {
    loading.value = false
  }
}

/** 昵称冷却剩余天数（后端规则：3 天可改一次） */
const nicknameCooldown = computed(() => {
  if (!meta.nicknameUpdateTime) return 0
  const last = new Date(String(meta.nicknameUpdateTime).replace(' ', 'T')).getTime()
  if (Number.isNaN(last)) return 0
  const remainMs = last + 3 * 24 * 3600 * 1000 - Date.now()
  return remainMs > 0 ? Math.ceil(remainMs / (24 * 3600 * 1000)) : 0
})

const pickAvatar = () => fileInput.value?.click()

const onAvatarChange = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    errMsg.value = '请选择图片文件'
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    errMsg.value = '头像不能超过 5MB'
    return
  }
  uploading.value = true
  errMsg.value = ''
  okMsg.value = ''
  try {
    await uploadAvatar(file)
    await fetchProfile()
    okMsg.value = '头像已更新'
  } catch (e: any) {
    errMsg.value = e?.data?.message || e?.message || '头像上传失败'
  } finally {
    uploading.value = false
  }
}

const save = async () => {
  errMsg.value = ''
  okMsg.value = ''
  if (!form.nickname.trim()) {
    errMsg.value = '昵称不能为空'
    return
  }
  saving.value = true
  try {
    await updateProfile({
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      phone: form.phone.trim()
    })
    await updateSignature(form.signature.trim())
    await fetchProfile()
    okMsg.value = '已保存'
  } catch (e: any) {
    errMsg.value = e?.data?.message || e?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

useSeoMeta({
  title: () => `个人设置 - ${siteName.value}`,
  robots: 'noindex'
})
</script>

<template>
  <div>
    <CommunityBreadcrumb :items="[{ label: '社区首页', to: '/community' }, { label: '个人设置' }]" />

    <div v-if="!isLoggedIn" class="card empty">
      <p>登录后即可修改个人资料。</p>
      <button type="button" class="btn primary" @click="showLogin = true">登录 / 注册</button>
    </div>

    <div v-else class="layout">
      <div class="main-col">
        <!-- 头像 + 账号 -->
        <section class="card">
          <h2 class="card-title">头像与账号</h2>
          <div class="avatar-row">
            <img v-if="user?.avatar" :src="imageUrl(user.avatar)" :alt="user.nickname" class="avatar" />
            <span v-else class="avatar avatar-text">{{ (user?.nickname || '?').slice(0, 1) }}</span>
            <div class="avatar-actions">
              <button type="button" class="btn" :disabled="uploading" @click="pickAvatar">
                <Camera :size="14" />{{ uploading ? '上传中…' : '更换头像' }}
              </button>
              <p class="hint">支持 jpg / png / gif / webp，建议 256×256，不超过 5MB。</p>
            </div>
            <input ref="fileInput" type="file" accept="image/*" class="file-input" @change="onAvatarChange" />
          </div>

          <dl class="meta-list">
            <div class="meta-item">
              <dt>用户名</dt>
              <dd>{{ meta.username }}</dd>
            </div>
            <div class="meta-item">
              <dt>注册时间</dt>
              <dd>{{ meta.createTime || '—' }}</dd>
            </div>
            <div class="meta-item">
              <dt>昵称修改</dt>
              <dd>
                <template v-if="nicknameCooldown > 0">剩余 {{ nicknameCooldown }} 天可再次修改</template>
                <template v-else>可修改（3 天限改一次）</template>
              </dd>
            </div>
          </dl>
        </section>

        <!-- 资料 -->
        <section class="card">
          <h2 class="card-title">基本资料</h2>
          <label class="label" for="s-nickname">昵称</label>
          <input id="s-nickname" v-model.trim="form.nickname" class="input" maxlength="32" />
          <p v-if="nicknameCooldown > 0" class="hint">昵称 3 天可改一次，当前不可修改。</p>

          <div class="grid2">
            <div>
              <label class="label" for="s-email">邮箱</label>
              <input id="s-email" v-model.trim="form.email" class="input" placeholder="用于接收通知" />
            </div>
            <div>
              <label class="label" for="s-phone">手机号</label>
              <input id="s-phone" v-model.trim="form.phone" class="input" placeholder="可选" />
            </div>
          </div>

          <label class="label" for="s-signature">个人签名</label>
          <textarea
            id="s-signature"
            v-model="form.signature"
            class="input textarea"
            rows="3"
            maxlength="100"
            placeholder="展示在你的公开主页上，最多 100 字"
          />
          <p class="hint">{{ form.signature.length }}/100</p>

          <p v-if="errMsg" class="msg err">{{ errMsg }}</p>
          <p v-if="okMsg" class="msg ok">{{ okMsg }}</p>

          <div class="actions">
            <button type="button" class="btn primary" :disabled="saving || loading" @click="save">
              <Save :size="14" />{{ saving ? '保存中…' : '保存修改' }}
            </button>
          </div>
        </section>
      </div>

      <aside class="side-col">
        <section class="card">
          <h2 class="card-title">快捷入口</h2>
          <NuxtLink v-if="user?.id" :to="`/community/users/${user.id}`" class="quick">
            <User :size="14" />我的公开主页
          </NuxtLink>
          <NuxtLink to="/community/notifications" class="quick">
            <MessageSquare :size="14" />我的消息
          </NuxtLink>
        </section>
      </aside>
    </div>

    <CommunityAuthModal v-model:visible="showLogin" @logged-in="load" />
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 20px;
  align-items: start;
}
.card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  padding: 18px;
}
.card + .card {
  margin-top: 16px;
}
.card-title {
  margin: 0 0 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--c-border);
  font-size: 15px;
  font-weight: 600;
}
.empty {
  text-align: center;
  color: var(--c-text-muted);
  font-size: 13.5px;
}
.empty p {
  margin: 0 0 12px;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
.avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--c-border);
  flex-shrink: 0;
}
.avatar-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--c-accent-soft);
  color: var(--c-accent);
  font-size: 28px;
  font-weight: 600;
}
.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.file-input {
  display: none;
}

.meta-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
  margin: 18px 0 0;
  padding-top: 14px;
  border-top: 1px solid var(--c-border);
}
.meta-item dt {
  color: var(--c-text-faint);
  font-size: 12.5px;
}
.meta-item dd {
  margin: 2px 0 0;
  font-size: 14px;
}

.label {
  display: block;
  margin: 12px 0 5px;
  color: var(--c-text-muted);
  font-size: 13px;
}
.input {
  width: 100%;
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
.input:focus {
  border-color: var(--c-accent);
}
.textarea {
  height: auto;
  padding: 8px 10px;
  line-height: 1.6;
  resize: vertical;
}
.grid2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 4px;
}
.hint {
  margin: 6px 0 0;
  color: var(--c-text-faint);
  font-size: 12.5px;
}
.msg {
  margin: 10px 0 0;
  font-size: 13px;
}
.msg.err {
  color: #d14343;
}
.msg.ok {
  color: #2f9e44;
}
.actions {
  margin-top: 16px;
}
.btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 14px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text-muted);
  font-size: 13.5px;
  cursor: pointer;
}
.btn:hover:not(:disabled) {
  border-color: var(--c-accent);
  color: var(--c-accent);
}
.btn.primary {
  border-color: var(--c-accent);
  background: var(--c-accent);
  color: #fff;
}
.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.quick {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 4px;
  color: var(--c-text-muted);
  font-size: 13.5px;
}
.quick:hover {
  color: var(--c-accent);
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: minmax(0, 1fr);
  }
  .grid2 {
    grid-template-columns: 1fr;
  }
}
</style>
