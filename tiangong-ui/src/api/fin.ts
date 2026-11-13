import { del, get, post, put } from './http'

const PREFIX = '/admin-api/fin'

export const receivableApi = {
  page: (params: any) => get<any>(`${PREFIX}/receivable/page`, params)
}

export const payableApi = {
  page: (params: any) => get<any>(`${PREFIX}/payable/page`, params)
}

export const voucherApi = {
  page: (params: any) => get<any>(`${PREFIX}/voucher/page`, params),
  detail: (id: string) => get<any>(`${PREFIX}/voucher/detail`, { id }),
  subjectList: () => get<any[]>(`${PREFIX}/subject/list`),
  accountList: () => get<any[]>(`${PREFIX}/account/list`)
}

export const receiptApi = {
  page: (params: any) => get<any>(`${PREFIX}/receipt/page`, params),
  detail: (id: string) => get<any>(`${PREFIX}/receipt/detail`, { id }),
  openReceivables: (partnerId?: string) => get<any[]>(`${PREFIX}/receipt/open-receivables`, { partnerId }),
  create: (data: any) => post(`${PREFIX}/receipt/create`, data),
  delete: (id: string) => del(`${PREFIX}/receipt/delete`, { id }),
  audit: (id: string) => post(`${PREFIX}/receipt/audit`, null, { id }),
  voidOrder: (id: string) => post(`${PREFIX}/receipt/void`, null, { id })
}

export const paymentApi = {
  page: (params: any) => get<any>(`${PREFIX}/payment/page`, params),
  detail: (id: string) => get<any>(`${PREFIX}/payment/detail`, { id }),
  openPayables: (partnerId?: string) => get<any[]>(`${PREFIX}/payment/open-payables`, { partnerId }),
  create: (data: any) => post(`${PREFIX}/payment/create`, data),
  delete: (id: string) => del(`${PREFIX}/payment/delete`, { id }),
  audit: (id: string) => post(`${PREFIX}/payment/audit`, null, { id }),
  voidOrder: (id: string) => post(`${PREFIX}/payment/void`, null, { id })
}

export const templateApi = {
  page: (params: any) => get<any>(`${PREFIX}/template/page`, params),
  create: (params: any) => post(`${PREFIX}/template/create`, null, params),
  update: (params: any) => put(`${PREFIX}/template/update`, null, params),
  delete: (id: string) => del(`${PREFIX}/template/delete`, { id })
}

// ========== 发票 / 回款计划 / 期末结转（V11 fin 扩展端点） ==========
export const invoiceApi = {
  list: (params?: any) => get<any[]>('/admin-api/fin/invoice/list', params),
  create: (params: any) => post('/admin-api/fin/invoice/create', null, params),
  delete: (id: string) => del('/admin-api/fin/invoice/delete', { id })
}

export const receivablePlanApi = {
  list: (params?: any) => get<any[]>('/admin-api/fin/receivable-plan/list', params),
  create: (params: any) => post('/admin-api/fin/receivable-plan/create', null, params),
  done: (id: string) => post('/admin-api/fin/receivable-plan/done', null, { id }),
  delete: (id: string) => del('/admin-api/fin/receivable-plan/delete', { id })
}

export const periodCloseApi = {
  list: () => get<any[]>('/admin-api/fin/period/list'),
  isClosed: (period: string) => get<boolean>('/admin-api/fin/period/is-closed', { period }),
  close: (period: string) => post<string>('/admin-api/fin/period/close', null, { period })
}
