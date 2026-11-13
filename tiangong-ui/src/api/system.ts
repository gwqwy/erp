import { del, get, post, put } from './http'

const PREFIX = '/admin-api/system'

// ========== 用户 ==========
export const userApi = {
  page: (params: any) => get<any>(`${PREFIX}/user/page`, params),
  create: (data: any) => post(`${PREFIX}/user/create`, data),
  update: (data: any) => put(`${PREFIX}/user/update`, data),
  delete: (id: string) => del(`${PREFIX}/user/delete`, { id }),
  updatePassword: (data: { id: string; password: string }) => put(`${PREFIX}/user/update-password`, data),
  assignRole: (data: { id: string; roleIds: string[] }) => put(`${PREFIX}/user/assign-role`, data),
  getRoleIds: (id: string) => get<string[]>(`${PREFIX}/user/get-role-ids`, { id })
}

// ========== 角色 ==========
export const roleApi = {
  page: (params: any) => get<any>(`${PREFIX}/role/page`, params),
  listSimple: () => get<any[]>(`${PREFIX}/role/list-simple`),
  create: (data: any) => post(`${PREFIX}/role/create`, data),
  update: (data: any) => put(`${PREFIX}/role/update`, data),
  delete: (id: string) => del(`${PREFIX}/role/delete`, { id }),
  getMenuIds: (roleId: string) => get<string[]>(`${PREFIX}/role/get-menu-ids`, { roleId }),
  assignMenu: (data: { roleId: string; menuIds: string[] }) => put(`${PREFIX}/role/assign-menu`, data)
}

// ========== 菜单 ==========
export const menuApi = {
  list: () => get<any[]>(`${PREFIX}/menu/list`),
  create: (data: any) => post(`${PREFIX}/menu/create`, data),
  update: (data: any) => put(`${PREFIX}/menu/update`, data),
  delete: (id: string) => del(`${PREFIX}/menu/delete`, { id })
}

// ========== 部门（后端为 @RequestParam 风格）==========
export const deptApi = {
  list: () => get<any[]>(`${PREFIX}/dept/list`),
  create: (params: any) => post(`${PREFIX}/dept/create`, null, params),
  update: (params: any) => put(`${PREFIX}/dept/update`, null, params),
  delete: (id: string) => del(`${PREFIX}/dept/delete`, { id })
}

// ========== 字典 ==========
export const dictApi = {
  typePage: (params: any) => get<any>(`${PREFIX}/dict/type/page`, params),
  typeCreate: (params: any) => post(`${PREFIX}/dict/type/create`, null, params),
  typeUpdate: (params: any) => put(`${PREFIX}/dict/type/update`, null, params),
  typeDelete: (id: string) => del(`${PREFIX}/dict/type/delete`, { id }),
  dataList: (dictType: string) => get<any[]>(`${PREFIX}/dict/data/list`, { dictType }),
  dataCreate: (params: any) => post(`${PREFIX}/dict/data/create`, null, params),
  dataUpdate: (params: any) => put(`${PREFIX}/dict/data/update`, null, params),
  dataDelete: (id: string) => del(`${PREFIX}/dict/data/delete`, { id })
}
