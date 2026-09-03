<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  getMenuTree,
  createMenu,
  updateMenu,
  deleteMenu,
  type MenuNode,
  type MenuForm
} from '@/api/system'

const loading = ref(false)
const list = ref<MenuNode[]>([])

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingId = ref<number | undefined>()
const formRef = ref<FormInstance>()
const form = reactive<MenuForm>({
  parentId: 0,
  menuName: '',
  menuType: 'C',
  permission: '',
  path: '',
  component: '',
  icon: '',
  sort: 0,
  visible: 1,
  status: 1
})

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  menuType: [{ required: true, message: '请选择菜单类型', trigger: 'change' }]
}

// 父菜单选项：把当前树形数据转成 el-tree-select 可用结构
// 顶级根 parentId = 0，无父
const treeSelectData = computed<MenuNode[]>(() => [
  { id: 0, parentId: -1, menuName: '顶级', menuType: 'M', permission: '', path: '', component: '', icon: '', sort: 0, visible: 1, status: 1, children: list.value }
])

const treeProps = { label: 'menuName', children: 'children' }

const menuTypeOptions = [
  { label: '目录', value: 'M' },
  { label: '菜单', value: 'C' },
  { label: '按钮/权限', value: 'F' }
]

const menuTypeTag = (t: string) =>
  t === 'M' ? { type: 'primary', text: '目录' }
    : t === 'C' ? { type: 'success', text: '菜单' }
      : { type: 'info', text: '按钮' }

const fetchList = async () => {
  loading.value = true
  try {
    list.value = await getMenuTree()
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, {
    parentId: 0,
    menuName: '',
    menuType: 'C',
    permission: '',
    path: '',
    component: '',
    icon: '',
    sort: 0,
    visible: 1,
    status: 1
  })
}

const handleAdd = (row?: MenuNode) => {
  editingId.value = undefined
  dialogTitle.value = '新增菜单'
  resetForm()
  if (row) form.parentId = row.id
  dialogVisible.value = true
}

const handleEdit = (row: MenuNode) => {
  editingId.value = row.id
  dialogTitle.value = '编辑菜单'
  resetForm()
  Object.assign(form, {
    parentId: row.parentId,
    menuName: row.menuName,
    menuType: row.menuType,
    permission: row.permission,
    path: row.path,
    component: row.component,
    icon: row.icon,
    sort: row.sort,
    visible: row.visible,
    status: row.status
  })
  dialogVisible.value = true
}

const handleDelete = async (row: MenuNode) => {
  await ElMessageBox.confirm(`确定删除菜单「${row.menuName}」吗？子菜单也会一并删除。`, '提示', { type: 'warning' })
  await deleteMenu(row.id)
  ElMessage.success('已删除')
  fetchList()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async valid => {
    if (!valid) return
    if (editingId.value) {
      await updateMenu(editingId.value, { ...form })
      ElMessage.success('修改完成')
    } else {
      await createMenu({ ...form })
      ElMessage.success('新增完成')
    }
    dialogVisible.value = false
    fetchList()
  })
}

onMounted(fetchList)
</script>

<template>
  <div class="menu-list">
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head">
          <span>菜单管理</span>
          <el-button v-permission="'menu:add'" type="primary" @click="handleAdd()">+ 新增顶级菜单</el-button>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="list"
        row-key="id"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        default-expand-all
        border
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="180" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="menuTypeTag(row.menuType).type as any" size="small">
              {{ menuTypeTag(row.menuType).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="120">
          <template #default="{ row }">
            <span v-if="row.icon">
              <el-icon><component :is="row.icon" /></el-icon>
              {{ row.icon }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路径" width="180" />
        <el-table-column prop="component" label="组件" width="180" />
        <el-table-column prop="permission" label="权限标识" width="160" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="可见" width="80">
          <template #default="{ row }">
            <el-tag :type="row.visible === 1 ? 'success' : 'info'" size="small">
              {{ row.visible === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'menu:add'" text type="primary" size="small" @click="handleAdd(row)">新增子级</el-button>
            <el-button v-permission="'menu:edit'" text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'menu:delete'" text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="父菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="treeSelectData"
            node-key="id"
            :props="treeProps"
            check-strictly
            default-expand-all
            placeholder="顶级"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单类型" prop="menuType">
          <el-radio-group v-model="form.menuType">
            <el-radio-button v-for="o in menuTypeOptions" :key="o.value" :value="o.value">{{ o.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" placeholder="菜单/目录/按钮名称" />
        </el-form-item>
        <template v-if="form.menuType !== 'F'">
          <el-form-item label="路由路径">
            <el-input v-model="form.path" placeholder="相对 Layout 的路径，如 blog/article" />
          </el-form-item>
          <el-form-item label="组件路径" v-if="form.menuType === 'C'">
            <el-input v-model="form.component" placeholder="相对 views 的路径，如 blog/article-list" />
          </el-form-item>
          <el-form-item label="图标">
            <el-input v-model="form.icon" placeholder="ElementPlus 图标名，如 Document" />
          </el-form-item>
        </template>
        <el-form-item label="权限标识" v-if="form.menuType === 'F' || form.permission">
          <el-input v-model="form.permission" placeholder="如 article:add" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="是否可见" v-if="form.menuType !== 'F'">
          <el-radio-group v-model="form.visible">
            <el-radio :value="1">显示</el-radio>
            <el-radio :value="0">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
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
.menu-list {
  padding: 4px 0;
}
.table-card {
  .card-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-weight: 600;
  }
}
</style>
