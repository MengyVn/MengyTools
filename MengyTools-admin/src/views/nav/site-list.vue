<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  listNavSites,
  createNavSite,
  updateNavSite,
  deleteNavSite,
  listNavCategories,
  type NavSite,
  type NavCategory,
  type NavSiteQuery
} from '@/api/nav'

const loading = ref(false)
const list = ref<NavSite[]>([])
const total = ref(0)
const categories = ref<NavCategory[]>([])
const query = reactive<NavSiteQuery>({ page: 1, size: 10, name: '', categoryId: undefined, status: undefined })

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

// 分类 id -> name 映射，方便表格展示
const categoryName = computed(() => {
  const map = new Map<number, string>()
  categories.value.forEach(c => map.set(c.id, c.name))
  return map
})

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingId = ref<number | undefined>()
const formRef = ref<FormInstance>()
const form = reactive<NavSite>({
  id: 0, categoryId: 0, name: '', url: '', description: '', icon: '', sort: 0, clickCount: 0, status: 1
})

const rules = {
  name: [{ required: true, message: '请输入站点名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入站点 URL', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listNavSites({ ...query, page: query.page, size: query.size })
    list.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  categories.value = await listNavCategories()
}

const handleSearch = () => {
  query.page = 1
  fetchList()
}
const handleReset = () => {
  query.name = ''
  query.categoryId = undefined
  query.status = undefined
  handleSearch()
}

const handleAdd = () => {
  editingId.value = undefined
  dialogTitle.value = '新增站点'
  Object.assign(form, {
    id: 0, categoryId: categories.value[0]?.id ?? 0, name: '', url: '',
    description: '', icon: '', sort: 0, clickCount: 0, status: 1
  })
  dialogVisible.value = true
}

const handleEdit = (row: NavSite) => {
  editingId.value = row.id
  dialogTitle.value = '编辑站点'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row: NavSite) => {
  await ElMessageBox.confirm(`确定删除站点「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteNavSite(row.id)
  ElMessage.success('删除成功')
  fetchList()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (editingId.value) {
      await updateNavSite(editingId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createNavSite({ ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchList()
  })
}

onMounted(() => {
  fetchCategories()
  fetchList()
})
</script>

<template>
  <div class="nav-site-list">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="站点名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.categoryId" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px">
            <el-option v-for="o in statusOptions" :key="String(o.value)" :label="o.label" :value="o.value" />
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
          <span>导航站点</span>
          <el-button type="primary" @click="handleAdd">+ 新增站点</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="站点" min-width="200">
          <template #default="{ row }">
            <div class="site-cell">
              <span v-if="row.icon" class="site-icon">{{ row.icon }}</span>
              <span class="site-name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">{{ categoryName.get(row.categoryId) || '-' }}</template>
        </el-table-column>
        <el-table-column label="URL" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <a :href="row.url" target="_blank" class="url-link">{{ row.url }}</a>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="clickCount" label="点击量" width="90" />
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
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择分类" filterable>
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="站点名称" />
        </el-form-item>
        <el-form-item label="URL" prop="url">
          <el-input v-model="form.url" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="站点描述（可选）" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="图标（emoji 或 URL，可选）" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.search-card { margin-bottom: 16px; }
.table-card .card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}
.site-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  .site-icon { font-size: 18px; }
  .site-name { font-weight: 500; }
}
.url-link {
  color: var(--el-color-primary);
  &:hover { text-decoration: underline; }
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
