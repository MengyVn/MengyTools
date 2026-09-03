<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  listRoles,
  createRole,
  updateRole,
  deleteRole,
  getMenuTree,
  getRoleMenuIds,
  assignRoleMenus,
  type Role,
  type RoleForm,
  type MenuNode
} from '@/api/system'

const loading = ref(false)
const list = ref<Role[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, roleName: '' })

// 编辑弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingId = ref<number | undefined>()
const formRef = ref<FormInstance>()
const form = reactive<RoleForm>({
  roleName: '',
  roleKey: '',
  sort: 0,
  status: 1,
  remark: ''
})

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }]
}

const statusTag = (s?: number) =>
  s === 1 ? { type: 'success', text: '正常' } : { type: 'info', text: '停用' }

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listRoles({ ...query })
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
  query.roleName = ''
  handleSearch()
}

const handleAdd = () => {
  editingId.value = undefined
  dialogTitle.value = '新增角色'
  Object.assign(form, { roleName: '', roleKey: '', sort: 0, status: 1, remark: '' })
  dialogVisible.value = true
}

const handleEdit = (row: Role) => {
  editingId.value = row.id
  dialogTitle.value = '编辑角色'
  Object.assign(form, {
    roleName: row.roleName,
    roleKey: row.roleKey,
    sort: row.sort,
    status: row.status,
    remark: row.remark
  })
  dialogVisible.value = true
}

const handleDelete = async (row: Role) => {
  await ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '提示', { type: 'warning' })
  await deleteRole(row.id)
  ElMessage.success('已删除')
  fetchList()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async valid => {
    if (!valid) return
    if (editingId.value) {
      await updateRole(editingId.value, { ...form })
      ElMessage.success('修改完成')
    } else {
      await createRole({ ...form })
      ElMessage.success('新增完成')
    }
    dialogVisible.value = false
    fetchList()
  })
}

// 分配菜单弹窗：自定义布局（一级纵向 / 二级横向 / 三级横向）
const menuDialogVisible = ref(false)
const menuTreeData = ref<MenuNode[]>([])
const currentRoleId = ref<number>()
// 用 reactive 包装 Set，确保 has/add/delete 触发响应式
const checkedIds = reactive(new Set<number>())

// 递归收集节点自身 + 所有子孙 id
const collectSelfAndDescendants = (n: MenuNode): number[] => {
  const ids = [n.id]
  n.children?.forEach(c => ids.push(...collectSelfAndDescendants(c)))
  return ids
}

const isGroupAllChecked = (m: MenuNode): boolean =>
  collectSelfAndDescendants(m).every(id => checkedIds.has(id))
const isGroupPartialChecked = (m: MenuNode): boolean => {
  const all = collectSelfAndDescendants(m)
  const hit = all.filter(id => checkedIds.has(id)).length
  return hit > 0 && hit < all.length
}
const toggleGroup = (m: MenuNode, val: boolean | string | number) => {
  const ids = collectSelfAndDescendants(m)
  if (val) ids.forEach(id => checkedIds.add(id))
  else ids.forEach(id => checkedIds.delete(id))
}

const isItemAllChecked = (c: MenuNode): boolean =>
  collectSelfAndDescendants(c).every(id => checkedIds.has(id))
const isItemPartialChecked = (c: MenuNode): boolean => {
  const all = collectSelfAndDescendants(c)
  const hit = all.filter(id => checkedIds.has(id)).length
  return hit > 0 && hit < all.length
}
const toggleItem = (c: MenuNode, val: boolean | string | number) => {
  const ids = collectSelfAndDescendants(c)
  if (val) ids.forEach(id => checkedIds.add(id))
  else ids.forEach(id => checkedIds.delete(id))
}

const toggleOne = (id: number, val: boolean | string | number) => {
  if (val) checkedIds.add(id)
  else checkedIds.delete(id)
}

const handleAssignMenus = async (row: Role) => {
  currentRoleId.value = row.id
  if (!menuTreeData.value.length) {
    menuTreeData.value = await getMenuTree()
  }
  const ids = await getRoleMenuIds(row.id)
  checkedIds.clear()
  ids.forEach(id => checkedIds.add(id))
  menuDialogVisible.value = true
}

const handleSaveMenus = async () => {
  if (!currentRoleId.value) return
  await assignRoleMenus(currentRoleId.value, Array.from(checkedIds))
  ElMessage.success('分配完成')
  menuDialogVisible.value = false
}

onMounted(fetchList)
</script>

<template>
  <div class="role-list">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="角色名">
          <el-input v-model="query.roleName" placeholder="角色名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-head">
          <span>角色列表</span>
          <el-button v-permission="'role:add'" type="primary" @click="handleAdd">+ 新建角色</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleName" label="角色名称" width="160" />
        <el-table-column prop="roleKey" label="标识" width="160" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status).type as any" size="small">
              {{ statusTag(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'role:edit'" text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'role:edit'" text type="primary" size="small" @click="handleAssignMenus(row)">分配菜单</el-button>
            <el-button v-permission="'role:delete'" text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 角色编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="角色名" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如：编辑员" />
        </el-form-item>
        <el-form-item label="标识" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="英文标识，如 editor" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单弹窗：固定尺寸长方形，内容超出滚动；一级纵向、二级横向、三级横向 -->
    <el-dialog
      v-model="menuDialogVisible"
      title="分配菜单"
      width="720px"
      class="menu-assign-dialog"
    >
      <div class="menu-assign-body">
        <div v-for="m in menuTreeData" :key="m.id" class="menu-group">
          <!-- 一级 M：纵向排列 -->
          <div class="menu-group-head">
            <el-checkbox
              :model-value="isGroupAllChecked(m)"
              :indeterminate="isGroupPartialChecked(m)"
              @change="val => toggleGroup(m, val)"
            >
              <span class="group-name">{{ m.menuName }}</span>
            </el-checkbox>
          </div>
          <!-- 二级 C：横向铺开吃满空间 -->
          <div v-if="m.children?.length" class="menu-group-body">
            <div v-for="c in m.children" :key="c.id" class="menu-item">
              <el-checkbox
                :model-value="isItemAllChecked(c)"
                :indeterminate="isItemPartialChecked(c)"
                @change="val => toggleItem(c, val)"
              >
                {{ c.menuName }}
              </el-checkbox>
              <!-- 三级 F：横向铺开（小尺寸） -->
              <div v-if="c.children?.length" class="menu-children">
                <el-checkbox
                  v-for="f in c.children"
                  :key="f.id"
                  :model-value="checkedIds.has(f.id)"
                  size="small"
                  @change="val => toggleOne(f.id, val)"
                >
                  {{ f.menuName }}
                </el-checkbox>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveMenus">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.role-list {
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
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

// 分配菜单弹窗内部布局
.menu-assign-body {
  .menu-group {
    padding: 12px;
    margin-bottom: 12px;
    border: 1px solid #ebeef5;
    border-radius: 4px;
  }
  .menu-group-head {
    margin-bottom: 8px;
    .group-name {
      font-weight: 600;
    }
  }
  // 二级 C：横向铺开，吃满空间
  .menu-group-body {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;
    gap: 8px 20px;
  }
  .menu-item {
    flex: 1 1 auto;
    min-width: 180px;
    max-width: 100%;
  }
  // 三级 F：横向铺开（小尺寸）
  .menu-children {
    display: flex;
    flex-wrap: wrap;
    gap: 4px 12px;
    margin-top: 6px;
    padding-left: 24px;
  }
}
</style>

<!-- 全局样式：el-dialog teleport 到 body 后 scoped 失效，需用全局块控制弹窗尺寸/滚动 -->
<style lang="scss">
.menu-assign-dialog {
  // 固定长方形：宽 720px 由 width 属性控制，body 高度固定 480px 并可纵向滚动
  .el-dialog__body {
    height: 480px;
    overflow-y: auto;
    padding: 16px 20px;
  }
}
</style>
