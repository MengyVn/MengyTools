<script setup lang="ts">
/**
 * 站点设置：默认落地端 + 站名与一句话定位。
 *
 * 默认落地端只影响「首次访问（无 cookie）」时 `/` 的落点：
 *   - 技术社区：访问 / 会 302 到 /community
 *   - 个人门户：访问 / 保持原来的门户首页
 * 用户访问过某一端后会被 cookie 记住（最近使用优先），不会被后台默认值反复改变。
 */
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listSettings,
  updateSettings,
  SETTING_KEYS,
  LANDING_OPTIONS,
  type SettingItem
} from '@/api/setting'

const loading = ref(false)
const saving = ref(false)
const rows = ref<SettingItem[]>([])

const form = reactive({
  defaultLanding: 'community',
  siteName: '',
  siteTagline: ''
})

const fetchSettings = async () => {
  loading.value = true
  try {
    const list = await listSettings()
    rows.value = list
    const map = new Map(list.map((r) => [r.settingKey, r.settingValue]))
    form.defaultLanding = map.get(SETTING_KEYS.defaultLanding) === 'portal' ? 'portal' : 'community'
    form.siteName = map.get(SETTING_KEYS.siteName) || ''
    form.siteTagline = map.get(SETTING_KEYS.siteTagline) || ''
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!form.siteName.trim()) {
    ElMessage.warning('站点名称不能为空')
    return
  }
  saving.value = true
  try {
    await updateSettings({
      [SETTING_KEYS.defaultLanding]: form.defaultLanding,
      [SETTING_KEYS.siteName]: form.siteName.trim(),
      [SETTING_KEYS.siteTagline]: form.siteTagline.trim()
    })
    ElMessage.success('已保存。前端最多 30 秒后生效（服务端缓存）')
    await fetchSettings()
  } finally {
    saving.value = false
  }
}

onMounted(fetchSettings)
</script>

<template>
  <div class="site-setting">
    <el-card v-loading="loading" shadow="never" class="form-card">
      <template #header>
        <div class="card-head">
          <span>站点设置</span>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </div>
      </template>

      <el-form label-width="130px" class="form">
        <el-form-item label="默认落地端">
          <el-radio-group v-model="form.defaultLanding">
            <el-radio v-for="o in LANDING_OPTIONS" :key="o.value" :value="o.value">
              {{ o.label }}
            </el-radio>
          </el-radio-group>
          <p class="hint">
            只影响首次访问（浏览器无 cookie）时访问 <code>/</code> 的落点：
            选「技术社区」会 302 跳转到 <code>/community</code>。
            用户访问过某一端后由 cookie 记住（最近使用优先），后台改默认值不会打断他们当前的浏览习惯。
            门户内部页面（<code>/blog</code>、<code>/tools</code> 等）始终正常访问，不受影响。
          </p>
        </el-form-item>

        <el-form-item label="站点名称">
          <el-input v-model="form.siteName" maxlength="60" show-word-limit style="max-width: 420px" />
          <p class="hint">用于社区导航、SEO 标题、sitemap 与 RSS。</p>
        </el-form-item>

        <el-form-item label="一句话定位">
          <el-input v-model="form.siteTagline" maxlength="80" show-word-limit style="max-width: 420px" />
          <p class="hint">社区首页副标题与 SEO 描述。</p>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head"><span>配置明细</span></div>
      </template>
      <el-table :data="rows" stripe>
        <el-table-column prop="settingKey" label="配置键" width="220" />
        <el-table-column prop="settingValue" label="当前值" min-width="200" show-overflow-tooltip />
        <el-table-column prop="remark" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column prop="updateBy" label="最后更新人" width="120" />
        <el-table-column prop="updateTime" label="更新时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.form-card,
.table-card {
  margin-bottom: 12px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.form {
  max-width: 820px;
}
.hint {
  margin: 6px 0 0;
  color: #909399;
  font-size: 12px;
  line-height: 1.7;
}
.hint code {
  padding: 0 4px;
  background: #f5f7fa;
  border-radius: 3px;
}
</style>
