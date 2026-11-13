import { get, post } from './http'

export interface LoginResult {
  userId: string
  accessToken: string
}

export interface MenuNode {
  id: string
  parentId: string
  name: string
  path: string
  component: string
  type: number
  sort: number
  children: MenuNode[]
}

export interface PermissionInfo {
  user: { id: string; username: string; nickname: string; deptId: string }
  roles: string[]
  permissions: string[]
  menus: MenuNode[]
}

export const authApi = {
  login: (data: { username: string; password: string }) =>
    post<LoginResult>('/admin-api/system/auth/login', data),
  logout: () => post<boolean>('/admin-api/system/auth/logout'),
  getPermissionInfo: () => get<PermissionInfo>('/admin-api/system/auth/get-permission-info')
}
