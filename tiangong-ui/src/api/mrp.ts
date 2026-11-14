import { del, get, post, put } from './http'

const PREFIX = '/admin-api/mrp'

export const bomApi = {
  page: (params: any) => get<any>(`${PREFIX}/bom/page`, params),
  detail: (id: string) => get<any>(`${PREFIX}/bom/detail`, { id }),
  create: (data: any) => post(`${PREFIX}/bom/create`, data),
  update: (data: any) => put(`${PREFIX}/bom/update`, data),
  delete: (id: string) => del(`${PREFIX}/bom/delete`, { id }),
  enable: (id: string) => post(`${PREFIX}/bom/enable`, null, { id })
}

export const workOrderApi = {
  page: (params: any) => get<any>(`${PREFIX}/work-order/page`, params),
  detail: (id: string) => get<any>(`${PREFIX}/work-order/detail`, { id }),
  itemList: (orderId: string) => get<any[]>(`${PREFIX}/work-order/item-list`, { orderId }),
  create: (params: any) => post(`${PREFIX}/work-order/create`, null, params),
  delete: (id: string) => del(`${PREFIX}/work-order/delete`, { id }),
  audit: (id: string) => post(`${PREFIX}/work-order/audit`, null, { id }),
  voidOrder: (id: string) => post(`${PREFIX}/work-order/void`, null, { id }),
  finish: (params: { id: string; qty: number; warehouseId: string }) =>
    post<any>(`${PREFIX}/work-order/finish`, null, params)
}

export const materialReqApi = {
  page: (params: any) => get<any>(`${PREFIX}/material-req/page`, params),
  detail: (id: string) => get<any>(`${PREFIX}/material-req/detail`, { id }),
  create: (data: any) => post(`${PREFIX}/material-req/create`, data),
  delete: (id: string) => del(`${PREFIX}/material-req/delete`, { id }),
  audit: (id: string) => post(`${PREFIX}/material-req/audit`, null, { id }),
  voidOrder: (id: string) => post(`${PREFIX}/material-req/void`, null, { id })
}

export const mrpCalcApi = {
  run: (data: { warehouseId: string; demands: Array<{ productId: string; qty: number }> }) =>
    post<any>(`${PREFIX}/calc/run`, data)
}

export const mrpPlanApi = {
  runAndSave: (data: { warehouseId: string; demands: Array<{ productId: string; qty: number }>; remark?: string }) =>
    post<any>(`${PREFIX}/plan/run-and-save`, data),
  page: (params: any) => get<any>(`${PREFIX}/plan/page`, params),
  toPurchaseOrder: (params: { id: string; supplierId: string }) =>
    post<any>(`${PREFIX}/plan/to-purchase-order`, null, params)
}

// ========== V17：BOM 成本卷算 ==========
export const costApi = {
  rollup: (productId: string) => get<any>(`${PREFIX}/calc/cost`, { productId })
}
