<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  listBlogCategories,
  createBlogCategory,
  updateBlogCategory,
  deleteBlogCategory,
  type BlogCategory
} from '@/api/blog'

const loading = ref(false)
const list = ref<BlogCategory[]>([])

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingId = ref<number | undefined>()
const formRef = ref<FormInstance>()
const form = reactive<BlogCategory>({ id: 0, name: '', slug: '', sort: 0 })

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  slug: [{ required: true, message: '请输入分类别名', trigger: 'blur' }]
}

const fetchList = async () => {
  loading.value = true
  try {
    list.value = await listBlogCategories()
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editingId.value = undefined
  dialogTitle.value = '新增分类'
  Object.assign(form, { id: 0, name: '', slug: '', sort: 0 })
  dialogVisible.value = true
}

const handleEdit = (row: BlogCategory) => {
  editingId.value = row.id
  dialogTitle.value = '编辑分类'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row: BlogCategory) => {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteBlogCategory(row.id)
  ElMessage.success('已删除')
  fetchList()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (editingId.value) {
      await updateBlogCategory(editingId.value, { name: form.name, slug: form.slug, sort: form.sort })
      ElMessage.success('修改完成')
    } else {
      await createBlogCategory({ name: form.name, slug: form.slug, sort: form.sort })
      ElMessage.success('新增完成')
    }
    dialogVisible.value = false
    fetchList()
  })
}

onMounted(fetchList)
</script>

<template>
  <div class="category-list">
    <el-card shadow="never">
      <template #header>
        <div class="card-head">
          <span>博客分类</span>
          <el-button type="primary" @click="handleAdd">+ 新增分类</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" />
        <el-table-column prop="slug" label="别名" />
        <el-table-column prop="sort" label="排序" width="100" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="420px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="分类名称" />
        </el-form-item>
        <el-form-item label="别名" prop="slug">
          <el-input v-model="form.slug" placeholder="英文别名（用于路由）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
</style>
