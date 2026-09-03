<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import {
  listUsers,
  createUser,
  updateUser,
  deleteUser,
  listAllRoles,
  getUserRoleIds,
  type UserItem,
  type UserForm,
  type Role
} from '@/api/system'

const loading = ref(false)
const list = ref<UserItem[]>([])
const total = ref(0)
const roles = ref<Role[]>([])
const query = reactive({ page: 1, size: 10, username: '', nickname: '' })

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingId = ref<number | undefined>()
const formRef = ref<FormInstance>()
const form = reactive<UserForm>({
  username: '',
  nickname: '',
  password: '',
  email: '',
  phone: '',
  status: 1,
  roleIds: []
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }]
}

const statusTag = (s?: number) =>
  s === 1 ? { type: 'success', text: '正常' } : { type: 'info', text: '停用' }

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listUsers({ ...query })
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
  query.username = ''
  query.nickname = ''
  handleSearch()
}

const resetForm = () => {
  Object.assign(form, {
    username: '',
    nickname: '',
    password: '',
    email: '',
    phone: '',
    status: 1,
    roleIds: []
  })
}

const handleAdd = () => {
  editingId.value = undefined
  dialogTitle.value = '新增用户'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row: UserItem) => {
  editingId.value = row.id
  dialogTitle.value = '编辑用户'
  resetForm()
  Object.assign(form, {
    username: row.username,
    nickname: row.nickname,
    email: row.email,
    phone: row.phone,
    status: row.status,
    roleIds: []
  })
  // 拉当前用户角色
  getUserRoleIds(row.id).then(ids => (form.roleIds = ids))
  dialogVisible.value = true
}

const handleDelete = async (row: UserItem) => {
  await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '提示', { type: 'warning' })
  await deleteUser(row.id)
  ElMessage.success('已删除')
  fetchList()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async valid => {
    if (!valid) return
    const payload: UserForm = { ...form }
    // 编辑时不重置密码（空则不传）
    if (editingId.value && !payload.password) delete payload.password
    if (editingId.value) {
      await updateUser(editingId.value, payload)
      ElMessage.success('修改完成')
    } else {
      await createUser(payload)
      ElMessage.success('新增完成')
    }
    dialogVisible.value = false
    fetchList()
  })
}

onMounted(async () => {
  roles.value = await listAllRoles()
  fetchList()
})
</script>

<template>
  <div class="user-list">
    <el-card shadow="never" class="search-card">
      <el-form inline>
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="用户名" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="query.nickname" placeholder="昵称" clearable @keyup.enter="handleSearch" />
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
          <span>用户列表</span>
          <el-button v-permission="'user:add'" type="primary" @click="handleAdd">+ 新建用户</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <template v-if="row.roles?.length">
              <el-tag
                v-for="r in row.roles"
                :key="r.id"
                size="small"
                class="role-tag"
              >
                {{ r.roleName }}
              </el-tag>
            </template>
            <span v-else class="muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status).type as any" size="small">
              {{ statusTag(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" width="170">
          <template #default="{ row }">
            {{ row.loginTime?.replace('T', ' ').slice(0, 16) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'user:edit'" text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button v-permission="'user:delete'" text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item v-if="!editingId" label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item v-else label="用户名">
          <el-input :model-value="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="显示昵称" />
        </el-form-item>
        <el-form-item :label="editingId ? '重置密码' : '密码'" :required="!editingId">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="editingId ? '留空则不修改' : '登录密码'"
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="邮箱（可选）" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="手机号（可选）" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="分配角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
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
.user-list {
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
.role-tag {
  margin: 2px 6px 2px 0;
}
.muted {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
