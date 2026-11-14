import { del, get, post, put } from './http'

const PREFIX = '/admin-api/pur'

export const purchaseOrderApi = {
  depositGet: (id: string) => get<any>('/admin-api/pur/order/deposit', { id }),
  depositRegister: (id: string, amount: number) =>
    post<number>('/admin-api/pur/order/deposit', null, { id, amount }),
  page: (params: any) => get<any>(`${PREFIX}/order/page`, params),
  detail: (id: string) => get<any>(`${PREFIX}/order/detail`, { id }),
  itemList: (orderId: string) => get<any[]>(`${PREFIX}/order/item-list`, { orderId }),
  create: (data: any) => post(`${PREFIX}/order/create`, data),
  update: (data: any) => put(`${PREFIX}/order/update`, data),
  delete: (id: string) => del(`${PREFIX}/order/delete`, { id }),
  audit: (id: string) => post(`${PREFIX}/order/audit`, null, { id }),
  voidOrder: (id: string) => post(`${PREFIX}/order/void`, null, { id }),
  submitApproval: (id: string) => post(`${PREFIX}/order/submit-approval`, null, { id })
}

export const purchaseInboundApi = {
  page: (params: any) => get<any>(`${PREFIX}/inbound/page`, params),
  detail: (id: string) => get<any>(`${PREFIX}/inbound/detail`, { id }),
  create: (data: any) => post(`${PREFIX}/inbound/create`, data),
  delete: (id: string) => del(`${PREFIX}/inbound/delete`, { id }),
  audit: (id: string) => post(`${PREFIX}/inbound/audit`, null, { id }),
  voidOrder: (id: string) => post(`${PREFIX}/inbound/void`, null, { id }),
  submitApproval: (id: string) => post(`${PREFIX}/inbound/submit-approval`, null, { id }),
  reverse: (id: string) => post(`${PREFIX}/inbound/reverse`, null, { id })
}

// 采购退货
export const purchaseReturnApi = {
  page: (params: any) => get<any>(`${PREFIX}/return/page`, params),
  detail: (id: string) => get<any>(`${PREFIX}/return/detail`, { id }),
  create: (data: any) => post(`${PREFIX}/return/create`, data),
  delete: (id: string) => del(`${PREFIX}/return/delete`, { id }),
  audit: (id: string) => post(`${PREFIX}/return/audit`, null, { id }),
  voidOrder: (id: string) => post(`${PREFIX}/return/void`, null, { id })
}
