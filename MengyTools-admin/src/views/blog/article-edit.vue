<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getArticle,
  createArticle,
  updateArticle,
  listBlogCategories,
  type ArticleForm,
  type BlogCategory
} from '@/api/blog'

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

// Markdown 预览（简易渲染，避免引入额外依赖）
const preview = computed(() => {
  let html = form.content || ''
  // 标题
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')
  // 粗体/斜体
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')
  // 代码块
  html = html.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
  // 行内代码
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>')
  // 图片
  html = html.replace(/!\[(.*?)\]\((.+?)\)/g, '<img src="$2" alt="$1">')
  // 链接
  html = html.replace(/\[(.+?)\]\((.+?)\)/g, '<a href="$2" target="_blank">$1</a>')
  // 列表
  html = html.replace(/^- (.+)$/gm, '<li>$1</li>')
  html = html.replace(/(<li>[\s\S]*?<\/li>)/g, '<ul>$1</ul>')
  // 段落
  html = html
    .split(/\n\n+/)
    .map(block =>
      /^<(h\d|ul|pre|img)/.test(block.trim()) ? block.trim() : `<p>${block.trim()}</p>`
    )
    .join('\n')
  return html
})

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
      ElMessage.success('保存成功')
    } else {
      await createArticle(payload)
      ElMessage.success('创建成功')
      router.push('/content/blog')
    }
  } finally {
    submitting.value = false
  }
}

const handleBack = () => router.push('/content/blog')

onMounted(fetchData)
</script>

<template>
  <div v-loading="loading" class="article-edit">
    <!-- 顶部操作栏 -->
    <el-card shadow="never" class="head-card">
      <div class="head">
        <el-button @click="handleBack">← 返回</el-button>
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
        <el-form-item label="封面图 URL">
          <el-input v-model="form.cover" placeholder="封面图地址（可选）" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="文章摘要（可选）" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Markdown 编辑区 + 预览 -->
    <el-card shadow="never" class="editor-card">
      <template #header>
        <div class="card-head">
          <span>正文（Markdown）</span>
          <span class="tip">支持 Markdown 语法</span>
        </div>
      </template>
      <div class="editor-row">
        <div class="editor-col">
          <textarea
            v-model="form.content"
            class="md-input"
            placeholder="在此输入 Markdown 内容..."
            spellcheck="false"
          />
        </div>
        <div class="preview-col">
          <div class="md-preview markdown-body" v-html="preview" />
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
}
.editor-card {
  .card-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 600;
    .tip {
      font-size: 12px;
      color: #999;
      font-weight: normal;
    }
  }
}
.editor-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  min-height: 480px;
}
.editor-col,
.preview-col {
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  overflow: hidden;
}
.md-input {
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
.md-preview {
  padding: 16px 20px;
  min-height: 480px;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.8;
  background: #fff;
}

/* Markdown 渲染样式（与前台一致） */
.md-preview :deep(h1),
.md-preview :deep(h2),
.md-preview :deep(h3) {
  margin: 1.2em 0 0.6em;
  font-weight: 600;
}
.md-preview :deep(h1) { font-size: 22px; }
.md-preview :deep(h2) { font-size: 19px; }
.md-preview :deep(h3) { font-size: 17px; }
.md-preview :deep(p) { margin: 0.6em 0; }
.md-preview :deep(a) { color: var(--el-color-primary); }
.md-preview :deep(img) { max-width: 100%; border-radius: 4px; }
.md-preview :deep(code) {
  background: #f3f4f6;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'SFMono-Regular', Consolas, Menlo, monospace;
}
.md-preview :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 12px 16px;
  border-radius: 6px;
  overflow-x: auto;
}
.md-preview :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}
.md-preview :deep(ul) { padding-left: 24px; }

@media (max-width: 1024px) {
  .editor-row {
    grid-template-columns: 1fr;
  }
}
</style>
