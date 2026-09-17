<script setup lang="ts">
/**
 * 社区发帖/编辑编辑器（BBS 级：Markdown 编辑、实时预览、正文插图、链接、表格、代码块）。
 *
 * 编辑器复用后台管理端同款 md-editor-v3，但：
 *   - 仅在编辑页通过 defineAsyncComponent 懒加载，普通访客不会下载编辑器代码与样式；
 *   - 图片上传走社区专用接口 /v1/community/files/image（登录 + 限频 + 类型大小校验），
 *     不复用需要 article:edit 权限的管理端上传接口。
 */
import { ImagePlus, Send, X } from 'lucide-vue-next'
import 'md-editor-v3/lib/style.css'
import type { ToolbarNames } from 'md-editor-v3'
import { useCommunity, type PostForm } from '~/composables/useCommunity'
import { imageUrl, hasImage } from '~/utils/image'

// 懒加载：只在进入编辑页时才拉取编辑器
const MdEditor = defineAsyncComponent(async () => {
  const mod = await import('md-editor-v3')
  return mod.MdEditor
})

const props = defineProps<{
  mode: 'create' | 'edit'
  postId?: number
  initial?: {
    title?: string
    summary?: string
    content?: string
    cover?: string
    categoryId?: number | null
    tags?: string[]
    allowComment?: number
  } | null
}>()

const emit = defineEmits<{ (e: 'submitted', payload: { id: number; status: number }): void }>()

const { categories, createPost, updatePost, uploadImage } = useCommunity()

const { data: categoryList } = await useAsyncData('community-categories', () => categories())

const form = reactive({
  title: '',
  summary: '',
  content: '',
  cover: '',
  categoryId: null as number | null,
  allowComment: 1
})

const submitting = ref(false)
const uploadingCover = ref(false)
const errMsg = ref('')
const coverInput = ref<HTMLInputElement | null>(null)

// 回填（编辑模式）
if (props.initial) {
  form.title = props.initial.title || ''
  form.summary = props.initial.summary || ''
  form.content = props.initial.content || ''
  form.cover = props.initial.cover || ''
  form.categoryId = props.initial.categoryId ?? null
  form.allowComment = props.initial.allowComment ?? 1
}

/** 工具栏：与后台一致，保留插入图片/链接/表格/预览等常用能力 */
const toolbars: ToolbarNames[] = [
  'bold', 'underline', 'italic', '-',
  'strikeThrough', 'title', 'sub', 'sup',
  'quote', 'unorderedList', 'orderedList', '-',
  'codeRow', 'code', 'link', 'image', 'table', '-',
  'revoke', 'next', '=', 'pageFullscreen', 'fullscreen', 'preview', 'htmlPreview'
]

/** 编辑器内插图：上传后由编辑器插入 Markdown 图片语法 */
const handleUploadImg = async (files: File[], callback: (urls: string[]) => void) => {
  errMsg.value = ''
  try {
    const urls: string[] = []
    for (const f of files) {
      if (f.size > 5 * 1024 * 1024) {
        errMsg.value = `图片 ${f.name} 超过 5MB`
        continue
      }
      const res = await uploadImage(f)
      urls.push(res.url)
    }
    callback(urls)
  } catch (e: any) {
    errMsg.value = e?.data?.message || e?.message || '图片上传失败'
    callback([])
  }
}

const pickCover = () => coverInput.value?.click()

const onCoverChange = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    errMsg.value = '封面请选择图片文件'
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    errMsg.value = '封面不能超过 5MB'
    return
  }
  uploadingCover.value = true
  errMsg.value = ''
  try {
    const res = await uploadImage(file)
    form.cover = res.url
  } catch (e2: any) {
    errMsg.value = e2?.data?.message || e2?.message || '封面上传失败'
  } finally {
    uploadingCover.value = false
  }
}

const removeCover = () => {
  form.cover = ''
}

const submit = async () => {
  errMsg.value = ''
  if (form.title.trim().length < 2) {
    errMsg.value = '标题至少 2 个字'
    return
  }
  if (form.content.trim().length < 10) {
    errMsg.value = '正文至少 10 个字'
    return
  }
  const payload: PostForm = {
    title: form.title.trim(),
    summary: form.summary.trim(),
    content: form.content,
    cover: form.cover,
    categoryId: form.categoryId,
    allowComment: form.allowComment
  }
  submitting.value = true
  try {
    if (props.mode === 'edit' && props.postId) {
      await updatePost(props.postId, payload)
      emit('submitted', { id: props.postId, status: 1 })
    } else {
      const res = await createPost(payload)
      emit('submitted', { id: res.id, status: res.status })
    }
  } catch (e: any) {
    errMsg.value = e?.data?.message || e?.message || '提交失败，请稍后再试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <form class="editor" @submit.prevent="submit">
    <!-- 标题 -->
    <input
      v-model.trim="form.title"
      class="title-input"
      maxlength="100"
      placeholder="标题（2-100 字）"
      aria-label="标题"
    />

    <!-- 摘要 + 分类 + 标签 -->
    <div class="meta-grid">
      <div class="meta-item">
        <label class="label" for="p-category">板块</label>
        <select id="p-category" v-model="form.categoryId" class="input">
          <option :value="null">未选择板块</option>
          <option v-for="c in categoryList ?? []" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </div>
      <div class="meta-item">
        <label class="label" for="p-allow">评论</label>
        <select id="p-allow" v-model="form.allowComment" class="input">
          <option :value="1">允许评论</option>
          <option :value="0">关闭评论</option>
        </select>
      </div>
    </div>

    <label class="label" for="p-summary">摘要（可选，不填自动取正文前 150 字）</label>
    <input id="p-summary" v-model.trim="form.summary" class="input" maxlength="200" placeholder="一句话概括" />

    <!-- 封面 -->
    <div class="cover-row">
      <div class="label-row">
        <span class="label">封面图（可选）</span>
        <button type="button" class="link-btn" :disabled="uploadingCover" @click="pickCover">
          <ImagePlus :size="14" />{{ uploadingCover ? '上传中…' : '上传封面' }}
        </button>
      </div>
      <div v-if="hasImage(form.cover)" class="cover-preview">
        <img :src="imageUrl(form.cover)" alt="封面预览" />
        <button type="button" class="cover-remove" aria-label="移除封面" @click="removeCover">
          <X :size="14" />
        </button>
      </div>
      <input ref="coverInput" type="file" accept="image/*" class="file-input" @change="onCoverChange" />
    </div>

    <!-- Markdown 编辑器（客户端渲染） -->
    <div class="editor-wrap">
      <ClientOnly>
        <MdEditor
          v-model="form.content"
          :toolbars="toolbars"
          :preview="true"
          language="zh-CN"
          :style="{ height: '460px' }"
          placeholder="支持 Markdown：标题、列表、代码块、表格、链接与图片；工具栏可直接插入图片或链接"
          @on-upload-img="handleUploadImg"
        />
        <template #fallback>
          <textarea
            v-model="form.content"
            class="fallback-editor"
            rows="16"
            placeholder="编辑器加载中…（此浏览器环境不支持富编辑器时，可直接输入 Markdown）"
          />
        </template>
      </ClientOnly>
    </div>

    <p v-if="errMsg" class="msg err">{{ errMsg }}</p>

    <div class="actions">
      <span class="hint">发布后：新用户或含链接的内容会先进入待审，通过后公开可见。</span>
      <button type="submit" class="submit-btn" :disabled="submitting">
        <Send :size="14" />{{ submitting ? '提交中…' : mode === 'edit' ? '保存修改' : '发布帖子' }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.editor {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  padding: 18px;
}
.title-input {
  width: 100%;
  height: 44px;
  padding: 0 12px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text);
  font-size: 17px;
  font-weight: 500;
  outline: none;
}
.title-input:focus {
  border-color: var(--c-accent);
}
.meta-grid {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 14px;
  margin-top: 14px;
}
.label {
  display: block;
  margin: 12px 0 5px;
  color: var(--c-text-muted);
  font-size: 13px;
}
.label-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.label-row .label {
  margin-bottom: 5px;
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
.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--c-accent);
  font-size: 13px;
  cursor: pointer;
}
.link-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.cover-row {
  margin-top: 6px;
}
.cover-preview {
  position: relative;
  display: inline-block;
  margin-top: 6px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  overflow: hidden;
}
.cover-preview img {
  display: block;
  max-width: 320px;
  max-height: 160px;
  object-fit: cover;
}
.cover-remove {
  position: absolute;
  top: 6px;
  right: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: var(--c-radius);
  background: rgba(15, 23, 42, 0.6);
  color: #fff;
  cursor: pointer;
}
.file-input {
  display: none;
}
.editor-wrap {
  margin-top: 16px;
}
.fallback-editor {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 13.5px;
}
.msg {
  margin: 12px 0 0;
  font-size: 13px;
}
.msg.err {
  color: #d14343;
}
.actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 16px;
  flex-wrap: wrap;
}
.check {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--c-text-muted);
  font-size: 13.5px;
}
.hint {
  color: var(--c-text-faint);
  font-size: 12.5px;
}
.submit-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  margin-left: auto;
  padding: 0 20px;
  border: 1px solid var(--c-accent);
  border-radius: var(--c-radius);
  background: var(--c-accent);
  color: #fff;
  font-size: 14.5px;
  cursor: pointer;
}
.submit-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

@media (max-width: 720px) {
  .meta-grid {
    grid-template-columns: minmax(0, 1fr);
  }
  .submit-btn {
    margin-left: 0;
    width: 100%;
    justify-content: center;
  }
}
</style>
