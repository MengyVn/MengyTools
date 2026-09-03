import request from './request'

// ==================== 菜单/权限 ====================

export interface MenuNode {
  id: number
  parentId: number
  menuName: string
  /** M目录 C菜单 F按钮/权限 */
  menuType: 'M' | 'C' | 'F'
  permission: string
  path: string
  component: string
  icon: string
  sort: number
  visible: number
  status: number
  children?: MenuNode[]
}

export interface MenuForm {
  parentId?: number
  menuName: string
  menuType: 'M' | 'C' | 'F'
  permission?: string
  path?: string
  component?: string
  icon?: string
  sort?: number
  visible?: number
  status?: number
}

// 当前登录用户菜单树（侧边栏 + 动态路由）
export const getUserMenus = () =>
  request.get<unknown, MenuNode[]>('/v1/auth/menus')

// 全量菜单树（菜单管理页）
export const getMenuTree = () =>
  request.get<unknown, MenuNode[]>('/v1/admin/menus/tree')

export const createMenu = (data: MenuForm) =>
  request.post<unknown, number>('/v1/admin/menus', data)

export const updateMenu = (id: number, data: MenuForm) =>
  request.put<unknown, void>(`/v1/admin/menus/${id}`, data)

export const deleteMenu = (id: number) =>
  request.delete<unknown, void>(`/v1/admin/menus/${id}`)

// ==================== 角色 ====================

export interface Role {
  id: number
  roleName: string
  roleKey: string
  sort: number
  status: number
  remark: string
}

export interface RoleForm {
  roleName: string
  roleKey: string
  sort?: number
  status?: number
  remark?: string
}

export const listRoles = (params: { page: number; size: number; roleName?: string }) =>
  request.get<unknown, { records: Role[]; total: number }>('/v1/admin/roles', { params })

export const listAllRoles = () =>
  request.get<unknown, Role[]>('/v1/admin/roles/all')

export const createRole = (data: RoleForm) =>
  request.post<unknown, number>('/v1/admin/roles', data)

export const updateRole = (id: number, data: RoleForm) =>
  request.put<unknown, void>(`/v1/admin/roles/${id}`, data)

export const deleteRole = (id: number) =>
  request.delete<unknown, void>(`/v1/admin/roles/${id}`)

export const getRoleMenuIds = (id: number) =>
  request.get<unknown, number[]>(`/v1/admin/roles/${id}/menus`)

export const assignRoleMenus = (id: number, menuIds: number[]) =>
  request.put<unknown, void>(`/v1/admin/roles/${id}/menus`, { menuIds })

// ==================== 用户 ====================

export interface UserItem {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  status: number
  remark: string
  loginIp: string
  loginTime: string
  createTime: string
  /** 用户角色列表（list 接口返回） */
  roles?: { id: number; roleName: string; roleKey: string }[]
}

export interface UserForm {
  username?: string
  nickname?: string
  password?: string
  email?: string
  phone?: string
  avatar?: string
  status?: number
  remark?: string
  roleIds?: number[]
}

export const listUsers = (params: { page: number; size: number; username?: string; nickname?: string }) =>
  request.get<unknown, { records: UserItem[]; total: number }>('/v1/admin/users', { params })

export const createUser = (data: UserForm) =>
  request.post<unknown, number>('/v1/admin/users', data)

export const updateUser = (id: number, data: UserForm) =>
  request.put<unknown, void>(`/v1/admin/users/${id}`, data)

export const deleteUser = (id: number) =>
  request.delete<unknown, void>(`/v1/admin/users/${id}`)

export const getUserRoleIds = (id: number) =>
  request.get<unknown, number[]>(`/v1/admin/users/${id}/roles`)

export const assignUserRoles = (id: number, roleIds: number[]) =>
  request.put<unknown, void>(`/v1/admin/users/${id}/roles`, { roleIds })
