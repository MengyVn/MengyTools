<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import type { ToolbarNames } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import {
  getArticle,
  createArticle,
  updateArticle,
  listBlogCategories,
  type ArticleForm,
  type BlogCategory
} from '@/api/blog'
import { uploadImage } from '@/api'

const route = useRoute()
const router = useRouter()
const id = computed(() => {
  const v = route.params.id
  return v === 'new' ? undefined : Number(v)
})
const isEdit = computed(() => id.value !== undefined)

const loading = ref(false)
const submitting = ref(false)
const categories = ref<BlogCategory[]>([])
const form = reactive<ArticleForm>({
  title: '',
  summary: '',
  content: '',
  contentFormat: 'markdown',
  cover: '',
  categoryId: null,
  status: 0,
  isTop: 0,
  publishTime: null
})

const statusOptions = [
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 }
]

const formatOptions = [
  { label: 'Markdown', value: 'markdown' },
  { label: 'HTML', value: 'html' }
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

// HTML 模式预览：直接返回原文走 v-html
const htmlPreview = computed(() => form.content || '')

const fetchData = async () => {
  loading.value = true
  try {
    categories.value = await listBlogCategories()
    if (isEdit.value && id.value) {
      const data = await getArticle(id.value)
      Object.assign(form, {
        title: data.title,
        summary: data.summary,
        content: data.content,
        contentFormat: data.contentFormat || 'markdown',
        cover: data.cover,
        categoryId: data.categoryId,
        status: data.status,
        isTop: data.isTop ?? 0,
        publishTime: data.publishTime
      })
    }
  } finally {
    loading.value = false
  }
}

const handleSubmit = async (status?: number) => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  submitting.value = true
  try {
    const payload = { ...form }
    if (status !== undefined) {
      payload.status = status
    }
    if (isEdit.value && id.value) {
      await updateArticle(id.value, payload)
      ElMessage.success('保存完成')
    } else {
      await createArticle(payload)
      ElMessage.success('创建完成')
      router.push('/blog/article')
    }
  } finally {
    submitting.value = false
  }
}

const handleBack = () => router.push('/blog/article')

// 封面图片上传：成功后回填 URL
const fileInput = ref<HTMLInputElement>()
const uploading = ref(false)
const triggerUpload = () => fileInput.value?.click()
const handleUpload = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const res = await uploadImage(file)
    form.cover = res.url
    ElMessage.success('上传完成')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

onMounted(fetchData)
</script>

<template>
  <div v-loading="loading" class="article-edit">
    <!-- 顶部操作栏 -->
    <el-card shadow="never" class="head-card">
      <div class="head">
        <el-button @click="handleBack"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
        <span class="head-title">{{ isEdit ? '编辑文章' : '新建文章' }}</span>
        <div class="head-actions">
          <el-button @click="handleSubmit(0)" :loading="submitting">存草稿</el-button>
          <el-button type="primary" @click="handleSubmit(1)" :loading="submitting">发布</el-button>
        </div>
      </div>
    </el-card>

    <!-- 表单 -->
    <el-card shadow="never" class="form-card">
      <el-form label-position="top">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="请输入文章标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" placeholder="选择分类" clearable>
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="form.status">
                <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="置顶">
              <el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="封面图">
          <div class="cover-row">
            <el-input v-model="form.cover" placeholder="填写图片 URL，或点击上传保存到服务器" />
            <el-button :loading="uploading" @click="triggerUpload">上传图片</el-button>
            <input ref="fileInput" type="file" accept="image/*" hidden @change="handleUpload" />
          </div>
          <el-image
            v-if="form.cover"
            :src="form.cover"
            fit="cover"
            class="cover-preview"
            :preview-src-list="[form.cover]"
            preview-teleported
          />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="文章摘要（可选）" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 正文编辑区 -->
    <el-card shadow="never" class="editor-card">
      <template #header>
        <div class="card-head">
          <div class="head-left">
            <span class="title-text">正文</span>
            <el-radio-group v-model="form.contentFormat" size="small">
              <el-radio-button v-for="o in formatOptions" :key="o.value" :label="o.label" :value="o.value" />
            </el-radio-group>
          </div>
          <span class="tip">
            {{ form.contentFormat === 'html' ? 'HTML 模式：直接编写 HTML 源码' : 'Markdown 模式：自带工具栏、代码高亮、双栏预览' }}
          </span>
        </div>
      </template>

      <!-- Markdown 模式：md-editor-v3 完整编辑器 -->
      <div v-if="form.contentFormat === 'markdown'" class="md-wrapper">
        <MdEditor
          v-model="form.content"
          :editorId="`article-editor-${id ?? 'new'}`"
          :toolbars="toolbars"
          :noMermaid="true"
          :noKatex="true"
          :preview="true"
          :showCodeRowNumber="true"
          previewTheme="github"
          codeTheme="github"
          language="zh-CN"
          placeholder="在此输入 Markdown 内容..."
          style="height: 620px"
          :onUploadImg="handleUploadImg"
        />
      </div>

      <!-- HTML 模式：双栏 textarea + 预览 -->
      <div v-else class="html-editor">
        <div class="html-col">
          <textarea
            v-model="form.content"
            class="html-input"
            placeholder="在此输入 HTML 源码，如 &lt;h1&gt;标题&lt;/h1&gt;..."
            spellcheck="false"
          />
        </div>
        <div class="html-col">
          <div class="html-preview markdown-body" v-html="htmlPreview" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.article-edit {
  padding: 4px 0;
}
.head-card {
  margin-bottom: 16px;
  .head {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .head-title {
    font-size: 16px;
    font-weight: 600;
  }
  .head-actions {
    margin-left: auto;
    display: flex;
    gap: 8px;
  }
}
.form-card {
  margin-bottom: 16px;
  .cover-row {
    display: flex;
    gap: 8px;
    width: 100%;
    .el-input {
      flex: 1;
    }
  }
  .cover-preview {
    margin-top: 8px;
    width: 240px;
    height: 120px;
    border-radius: 6px;
    border: 1px solid #e5e7eb;
  }
}
.editor-card {
  .card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    .head-left {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .title-text {
      font-weight: 600;
    }
    .tip {
      font-size: 12px;
      color: #999;
    }
  }
  .md-wrapper {
    /* 让 MdEditor 撑满容器 */
    border-radius: 6px;
    overflow: hidden;
  }
}
/* HTML 模式：双栏布局 */
.html-editor {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  min-height: 480px;
}
.html-col {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
}
.html-input {
  width: 100%;
  height: 100%;
  min-height: 480px;
  padding: 16px;
  border: none;
  outline: none;
  resize: none;
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
  font-size: 14px;
  line-height: 1.7;
  background: #fafafa;
}
.html-preview {
  padding: 16px 20px;
  min-height: 480px;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.8;
  background: #fff;
}

/* HTML 预览样式（与前台一致） */
.html-preview :deep(h1),
.html-preview :deep(h2),
.html-preview :deep(h3) {
  margin: 1.2em 0 0.6em;
  font-weight: 600;
}
.html-preview :deep(h1) { font-size: 22px; }
.html-preview :deep(h2) { font-size: 19px; }
.html-preview :deep(h3) { font-size: 17px; }
.html-preview :deep(p) { margin: 0.6em 0; }
.html-preview :deep(a) { color: var(--el-color-primary); }
.html-preview :deep(img) { max-width: 100%; border-radius: 4px; }
.html-preview :deep(code) {
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
}
.html-preview :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 12px 16px;
  border-radius: 6px;
  overflow-x: auto;
}
.html-preview :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}
.html-preview :deep(ul),
.html-preview :deep(ol) { padding-left: 24px; }
.html-preview :deep(blockquote) {
  margin: 0.6em 0;
  padding: 4px 12px;
  border-left: 4px solid #dcdfe6;
  color: #606266;
}

@media (max-width: 1024px) {
  .html-editor {
    grid-template-columns: 1fr;
  }
}
</style>
