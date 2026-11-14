import { del, get, post, put } from './http'

const PREFIX = '/admin-api/sal'

export const saleOrderApi = {
  depositGet: (id: string) => get<any>('/admin-api/sal/order/deposit', { id }),
  depositRegister: (id: string, amount: number) =>
    post<number>('/admin-api/sal/order/deposit', null, { id, amount }),
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

export const saleOutboundApi = {
  page: (params: any) => get<any>(`${PREFIX}/outbound/page`, params),
  detail: (id: string) => get<any>(`${PREFIX}/outbound/detail`, { id }),
  create: (data: any) => post(`${PREFIX}/outbound/create`, data),
  delete: (id: string) => del(`${PREFIX}/outbound/delete`, { id }),
  audit: (id: string) => post(`${PREFIX}/outbound/audit`, null, { id }),
  voidOrder: (id: string) => post(`${PREFIX}/outbound/void`, null, { id }),
  submitApproval: (id: string) => post(`${PREFIX}/outbound/submit-approval`, null, { id }),
  reverse: (id: string) => post(`${PREFIX}/outbound/reverse`, null, { id })
}

// 销售退货
export const saleReturnApi = {
  page: (params: any) => get<any>(`${PREFIX}/return/page`, params),
  detail: (id: string) => get<any>(`${PREFIX}/return/detail`, { id }),
  create: (data: any) => post(`${PREFIX}/return/create`, data),
  delete: (id: string) => del(`${PREFIX}/return/delete`, { id }),
  audit: (id: string) => post(`${PREFIX}/return/audit`, null, { id }),
  voidOrder: (id: string) => post(`${PREFIX}/return/void`, null, { id })
}
