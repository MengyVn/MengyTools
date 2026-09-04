<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import type { ToolbarNames } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import {
  listAnnouncements,
  getAnnouncement,
  createAnnouncement,
  updateAnnouncement,
  deleteAnnouncement,
  type AnnouncementItem,
  type AnnouncementForm,
  type AnnouncementQuery
} from '@/api/announce'
import { uploadImage } from '@/api'

const loading = ref(false)
const list = ref<AnnouncementItem[]>([])
const total = ref(0)
const query = reactive<AnnouncementQuery>({ page: 1, size: 10, title: '', status: undefined })

// 搜索状态选项（全部用 undefined，便于清空）
const searchStatusOptions = [
  { label: '全部', value: undefined },
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 },
  { label: '定时中', value: 2 },
  { label: '已下线', value: 3 }
]

// 列表状态 tag 颜色映射
const statusTag = (s?: number) => {
  if (s === 0) return { type: 'info', text: '草稿' }
  if (s === 1) return { type: 'success', text: '已发布' }
  if (s === 2) return { type: 'warning', text: '定时中' }
  if (s === 3) return { type: 'danger', text: '已下线' }
  return { type: 'info', text: '-' }
}

const formatTag = (f: string) =>
  f === 'html' ? { type: 'danger', text: 'HTML' } : { type: 'primary', text: 'Markdown' }

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listAnnouncements({ ...query, page: query.page, size: query.size })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  query.page = 1
  fetchList()
}
const handleReset = () => {
  query.title = ''
  query.status = undefined
  handleSearch()
}

// ============== 编辑弹窗 ==============
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingId = ref<number | undefined>()
const submitting = ref(false)

const defaultForm = (): AnnouncementForm => ({
  title: '',
  content: '',
  contentFormat: 'markdown',
  isPersistent: 0,
  publishTime: null,
  expireTime: null,
  status: 0
})

const form = reactive<AnnouncementForm>(defaultForm())

// 表单状态选项（用户只能选草稿或已发布；定时中/已下线由系统自动产生）
const formStatusOptions = [
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 }
]

const formatOptions = [
  { label: 'Markdown', value: 'markdown' as const },
  { label: 'HTML', value: 'html' as const }
]

// MdEditor 工具栏配置：保留常用按钮，关闭 mermaid/katex 以减小体积
const toolbars: ToolbarNames[] = [
  'bold', 'underline', 'italic', '-',
  'strikeThrough', 'title', 'sub', 'sup',
  'quote', 'unorderedList', 'orderedList', '-',
  'codeRow', 'code', 'link', 'image', 'table', '-',
  'revoke', 'next', '=', 'pageFullscreen', 'fullscreen', 'preview', 'htmlPreview'
]

// MdEditor 图片上传：对接后端 FileController，多文件并发上传后回调 URL 数组
const handleUploadImg = async (
  files: File[],
  callback: (urls: string[]) => void
) => {
  try {
    const urls = await Promise.all(
      files.map(async (f) => {
        const res = await uploadImage(f)
        return res.url
      })
    )
    callback(urls)
    ElMessage.success(`已上传 ${urls.length} 张图片`)
  } catch (e) {
    ElMessage.error('图片上传失败')
    callback([])
  }
}

const handleAdd = () => {
  editingId.value = undefined
  dialogTitle.value = '新增公告'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

const handleEdit = async (row: AnnouncementItem) => {
  editingId.value = row.id
  dialogTitle.value = '编辑公告'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
  try {
    const data = await getAnnouncement(row.id)
    Object.assign(form, {
      title: data.title,
      content: data.content,
      contentFormat: (data.contentFormat === 'html' ? 'html' : 'markdown'),
      isPersistent: data.isPersistent,
      publishTime: data.publishTime,
      expireTime: data.expireTime,
      status: data.status === 0 ? 0 : 1
    })
  } catch (e) {
    dialogVisible.value = false
  }
}

const handleDelete = async (row: AnnouncementItem) => {
  await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '提示', { type: 'warning' })
  await deleteAnnouncement(row.id)
  ElMessage.success('已删除')
  fetchList()
}

// 常驻切换：开启常驻时清空消失时间
const handlePersistentChange = (val: boolean | string | number) => {
  if (val === 1) {
    form.expireTime = null
  }
}

const handleSubmit = async () => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  // 常驻公告强制清空消失时间
  if (form.isPersistent === 1) {
    form.expireTime = null
  }
  submitting.value = true
  try {
    const payload: AnnouncementForm = { ...form }
    if (editingId.value) {
      await updateAnnouncement(editingId.value, payload)
      ElMessage.success('修改完成')
    } else {
      await createAnnouncement(payload)
      ElMessage.success('新增完成')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

// 弹窗关闭时清空 form
const handleDialogClosed = () => {
  Object.assign(form, defaultForm())
  editingId.value = undefined
}

const formatTime = (t: string | null) => t?.replace('T', ' ').slice(0, 16) || '-'

onMounted(fetchList)
</script>

<template>
  <div class="announce-list">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="标题">
          <el-input v-model="query.title" placeholder="公告标题" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option
              v-for="o in searchStatusOptions"
              :key="String(o.value)"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head">
          <span>公告列表</span>
          <el-button type="primary" @click="handleAdd">+ 新建公告</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="标题" min-width="220">
          <template #default="{ row }">
            <span class="title-cell">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="格式" width="110">
          <template #default="{ row }">
            <el-tag :type="formatTag(row.contentFormat).type as any" size="small">
              {{ formatTag(row.contentFormat).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="常驻" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isPersistent === 1 ? 'success' : 'info'" size="small">
              {{ row.isPersistent === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column label="消失时间" width="170">
          <template #default="{ row }">{{ formatTime(row.expireTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status).type as any" size="small">
              {{ statusTag(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="阅读量" width="90" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="780px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form label-position="top">
        <el-form-item label="标题" required>
          <el-input
            v-model="form.title"
            placeholder="请输入公告标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="内容格式">
          <el-radio-group v-model="form.contentFormat" size="small">
            <el-radio-button
              v-for="o in formatOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-radio-group>
        </el-form-item>

        <el-form-item label="内容">
          <div class="editor-wrapper">
            <MdEditor
              v-if="form.contentFormat === 'markdown'"
              v-model="form.content"
              :editorId="`announce-editor-${editingId ?? 'new'}`"
              :toolbars="toolbars"
              :noMermaid="true"
              :noKatex="true"
              :preview="true"
              :showCodeRowNumber="true"
              previewTheme="github"
              codeTheme="github"
              language="zh-CN"
              placeholder="在此输入 Markdown 内容..."
              style="height: 480px"
              :onUploadImg="handleUploadImg"
            />
            <el-input
              v-else
              v-model="form.content"
              type="textarea"
              :rows="16"
              placeholder="在此输入 HTML 源码，如 <h1>公告标题</h1>..."
            />
          </div>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="是否常驻">
              <el-switch
                v-model="form.isPersistent"
                :active-value="1"
                :inactive-value="0"
                @change="handlePersistentChange"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option
                  v-for="o in formStatusOptions"
                  :key="o.value"
                  :label="o.label"
                  :value="o.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="发布时间">
              <el-date-picker
                v-model="form.publishTime"
                type="datetime"
                placeholder="留空=立即发布"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item
          v-if="form.isPersistent === 0"
          label="消失时间"
          required
        >
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            placeholder="公告到期时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <div v-if="form.isPersistent === 1" class="tip-text">
          常驻公告不会自动消失，无需设置消失时间。
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.announce-list {
  padding: 4px 0;
}
.search-card {
  margin-bottom: 16px;
}
.table-card {
  .card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: 600;
  }
}
.title-cell {
  font-weight: 500;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.editor-wrapper {
  width: 100%;
  border-radius: 6px;
  overflow: hidden;
}
.tip-text {
  font-size: 12px;
  color: #999;
  margin-top: -8px;
}
</style>
