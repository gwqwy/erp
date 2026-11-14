import { del, get, post, put } from './http'

const PREFIX = '/admin-api/stk'

// 即时库存/流水查询（quant/move 页面与预警页共用）
export const stockApi = {
  quantPage: (params: any) => get<any>(`${PREFIX}/quant/page`, params),
  movePage: (params: any) => get<any>(`${PREFIX}/move/page`, params)
}

// 调拨
export const transferApi = {
  page: (params: any) => get<any>(`${PREFIX}/transfer/page`, params),
  detail: (id: string) => get<any>(`${PREFIX}/transfer/detail`, { id }),
  create: (data: any) => post(`${PREFIX}/transfer/create`, data),
  delete: (id: string) => del(`${PREFIX}/transfer/delete`, { id }),
  audit: (id: string) => post(`${PREFIX}/transfer/audit`, null, { id }),
  voidOrder: (id: string) => post(`${PREFIX}/transfer/void`, null, { id })
}

// 盘点
export const checkApi = {
  page: (params: any) => get<any>(`${PREFIX}/check/page`, params),
  detail: (id: string) => get<any>(`${PREFIX}/check/detail`, { id }),
  create: (params: any) => post(`${PREFIX}/check/create`, null, params),
  updateRealQty: (itemId: string, realQty: number) => put(`${PREFIX}/check/update-real-qty`, null, { itemId, realQty }),
  delete: (id: string) => del(`${PREFIX}/check/delete`, { id }),
  audit: (id: string) => post(`${PREFIX}/check/audit`, null, { id })
}

// 其他出入库
export const otherStockApi = {
  page: (params: any) => get<any>(`${PREFIX}/other/page`, params),
  detail: (id: string) => get<any>(`${PREFIX}/other/detail`, { id }),
  create: (data: any) => post(`${PREFIX}/other/create`, data),
  delete: (id: string) => del(`${PREFIX}/other/delete`, { id }),
  audit: (id: string) => post(`${PREFIX}/other/audit`, null, { id }),
  voidOrder: (id: string) => post(`${PREFIX}/other/void`, null, { id })
}

// ========== 库存预警扫描（V14） ==========
export const alertScanApi = {
  scanNow: () => post<any>('/admin-api/stk/alert/scan'),
  lowList: () => get<any[]>('/admin-api/stk/alert/low-list')
}

// ========== V17：序列号追溯 ==========
export const serialApi = {
  trace: (serialNo: string) => get<any[]>('/admin-api/stk/serial/trace', { serialNo }),
  inStock: (productId: string) => get<any[]>('/admin-api/stk/serial/in-stock', { productId }),
  register: (params: any) => post('/admin-api/stk/serial/register', null, params),
  consume: (params: any) => post('/admin-api/stk/serial/consume', null, params)
}
