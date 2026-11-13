import { del, get, post, put } from './http'

const PREFIX = '/admin-api/bas'

// 产品（请求体风格）
export const productApi = {
  page: (params: any) => get<any>(`${PREFIX}/product/page`, params),
  create: (data: any) => post(`${PREFIX}/product/create`, data),
  update: (data: any) => put(`${PREFIX}/product/update`, data),
  delete: (id: string) => del(`${PREFIX}/product/delete`, { id })
}

// 往来单位（请求体风格）
export const partnerApi = {
  page: (params: any) => get<any>(`${PREFIX}/partner/page`, params),
  create: (data: any) => post(`${PREFIX}/partner/create`, data),
  update: (data: any) => put(`${PREFIX}/partner/update`, data),
  delete: (id: string) => del(`${PREFIX}/partner/delete`, { id })
}

// 仓库（请求体风格）
export const warehouseApi = {
  list: (params?: any) => get<any[]>(`${PREFIX}/warehouse/list`, params),
  create: (data: any) => post(`${PREFIX}/warehouse/create`, data),
  update: (data: any) => put(`${PREFIX}/warehouse/update`, data),
  delete: (id: string) => del(`${PREFIX}/warehouse/delete`, { id })
}

// 产品分类（@RequestParam 风格）
export const categoryApi = {
  list: (params?: any) => get<any[]>(`${PREFIX}/product-category/list`, params),
  create: (params: any) => post(`${PREFIX}/product-category/create`, null, params),
  update: (params: any) => put(`${PREFIX}/product-category/update`, null, params),
  delete: (id: string) => del(`${PREFIX}/product-category/delete`, { id })
}

// 计量单位（@RequestParam 风格）
export const unitApi = {
  list: (params?: any) => get<any[]>(`${PREFIX}/unit/list`, params),
  create: (params: any) => post(`${PREFIX}/unit/create`, null, params),
  update: (params: any) => put(`${PREFIX}/unit/update`, null, params),
  delete: (id: string) => del(`${PREFIX}/unit/delete`, { id })
}

// 单号规则（@RequestParam 风格）
export const serialRuleApi = {
  list: () => get<any[]>(`${PREFIX}/serial-rule/list`),
  preview: (bizType: string) => get<string>(`${PREFIX}/serial-rule/preview`, { bizType }),
  create: (params: any) => post(`${PREFIX}/serial-rule/create`, null, params),
  update: (params: any) => put(`${PREFIX}/serial-rule/update`, null, params),
  delete: (id: string) => del(`${PREFIX}/serial-rule/delete`, { id })
}

// ========== 库位 / 价格策略（V11 bas 扩展端点） ==========
export const locationApi = {
  list: (warehouseId: string) => get<any[]>('/admin-api/bas/location/list', { warehouseId }),
  create: (params: any) => post('/admin-api/bas/location/create', null, params),
  update: (params: any) => put('/admin-api/bas/location/update', null, params),
  delete: (id: string) => del('/admin-api/bas/location/delete', { id })
}

export const partnerPriceApi = {
  list: (params?: any) => get<any[]>('/admin-api/bas/price/list', params),
  create: (params: any) => post('/admin-api/bas/price/create', null, params),
  delete: (id: string) => del('/admin-api/bas/price/delete', { id })
}
