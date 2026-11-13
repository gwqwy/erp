import { del, get, post } from './http'

const PREFIX = '/admin-api'

// 销售退货
export const saleReturnApi = {
  page: (params: any) => get<any>(`${PREFIX}/sal/return/page`, params),
  create: (data: any) => post(`${PREFIX}/sal/return/create`, data),
  delete: (id: string) => del(`${PREFIX}/sal/return/delete`, { id }),
  audit: (id: string) => post(`${PREFIX}/sal/return/audit`, null, { id }),
  voidOrder: (id: string) => post(`${PREFIX}/sal/return/void`, null, { id })
}

// 采购退货
export const purchaseReturnApi = {
  page: (params: any) => get<any>(`${PREFIX}/pur/return/page`, params),
  create: (data: any) => post(`${PREFIX}/pur/return/create`, data),
  delete: (id: string) => del(`${PREFIX}/pur/return/delete`, { id }),
  audit: (id: string) => post(`${PREFIX}/pur/return/audit`, null, { id }),
  voidOrder: (id: string) => post(`${PREFIX}/pur/return/void`, null, { id })
}

// 费用登记
export const expenseApi = {
  page: (params: any) => get<any>(`${PREFIX}/fin/expense/page`, params),
  create: (params: any) => post(`${PREFIX}/fin/expense/create`, null, params),
  delete: (id: string) => del(`${PREFIX}/fin/expense/delete`, { id }),
  audit: (id: string) => post(`${PREFIX}/fin/expense/audit`, null, { id }),
  voidOrder: (id: string) => post(`${PREFIX}/fin/expense/void`, null, { id }),
  accountList: () => get<any[]>(`${PREFIX}/fin/account/list`),
  aging: () => get<any[]>(`${PREFIX}/fin/report/aging`)
}

// 操作日志
export const operLogApi = {
  page: (params: any) => get<any>(`${PREFIX}/system/oper-log/page`, params)
}

// 公告
export const noticeApi = {
  list: () => get<any[]>(`${PREFIX}/infra/notice/list`),
  create: (params: any) => post(`${PREFIX}/infra/notice/create`, null, params),
  withdraw: (id: string) => put(`${PREFIX}/infra/notice/withdraw`, null, { id }),
  delete: (id: string) => del(`${PREFIX}/infra/notice/delete`, { id })
}

import { put } from './http'
