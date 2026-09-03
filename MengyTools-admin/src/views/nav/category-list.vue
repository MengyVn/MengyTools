<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  listNavCategories,
  createNavCategory,
  updateNavCategory,
  deleteNavCategory,
  type NavCategory
} from '@/api/nav'

const loading = ref(false)
const list = ref<NavCategory[]>([])

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingId = ref<number | undefined>()
const formRef = ref<FormInstance>()
const form = reactive<NavCategory>({ id: 0, parentId: 0, name: '', icon: '', sort: 0 })

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

// 父分类选项（排除自身及其子孙，避免循环引用——简化版：仅排除自身）
const parentOptions = computed(() => {
  return [
    { id: 0, name: '顶级分类' },
    ...list.value.filter(c => c.id !== editingId.value)
  ]
})

// 构建带层级缩进的展示列表
const treeList = computed(() => {
  const build = (parentId: number, depth: number): NavCategory[] => {
    return list.value
      .filter(c => c.parentId === parentId)
      .sort((a, b) => (a.sort || 0) - (b.sort || 0))
      .flatMap(c => [c, ...build(c.id, depth + 1)])
  }
  return build(0, 0).map(c => {
    const depth = list.value.find(p => p.id === c.parentId)
      ? list.value.filter(x => x.id === c.parentId)[0]
      : null
    return { ...c, _depth: depth ? 1 : 0 }
  })
})

// 简单层级展示：用前缀缩进
const displayName = (item: NavCategory) => {
  let depth = 0
  let pid = item.parentId
  while (pid !== 0 && depth < 10) {
    const parent = list.value.find(c => c.id === pid)
    if (!parent) break
    pid = parent.parentId
    depth++
  }
  return '　'.repeat(depth) + (depth > 0 ? '└ ' : '') + item.name
}

const fetchList = async () => {
  loading.value = true
  try {
    list.value = await listNavCategories()
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editingId.value = undefined
  dialogTitle.value = '新增分类'
  Object.assign(form, { id: 0, parentId: 0, name: '', icon: '', sort: 0 })
  dialogVisible.value = true
}

const handleEdit = (row: NavCategory) => {
  editingId.value = row.id
  dialogTitle.value = '编辑分类'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row: NavCategory) => {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？子分类需先自行处理。`, '提示', { type: 'warning' })
  await deleteNavCategory(row.id)
  ElMessage.success('已删除')
  fetchList()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (editingId.value) {
      await updateNavCategory(editingId.value, {
        parentId: form.parentId, name: form.name, icon: form.icon, sort: form.sort
      })
      ElMessage.success('修改完成')
    } else {
      await createNavCategory({
        parentId: form.parentId, name: form.name, icon: form.icon, sort: form.sort
      })
      ElMessage.success('新增完成')
    }
    dialogVisible.value = false
    fetchList()
  })
}

onMounted(fetchList)
</script>

<template>
  <div class="nav-category-list">
    <el-card shadow="never">
      <template #header>
        <div class="card-head">
          <span>导航分类</span>
          <el-button type="primary" @click="handleAdd">+ 新增分类</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" stripe row-key="id">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="分类名称" min-width="200">
          <template #default="{ row }">
            <span :style="{ paddingLeft: '0' }">{{ displayName(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="100" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="父分类">
          <el-select v-model="form.parentId" placeholder="顶级分类" clearable>
            <el-option v-for="o in parentOptions" :key="o.id" :label="o.name" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="分类名称" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="图标（emoji 或 URL，可选）" />
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
