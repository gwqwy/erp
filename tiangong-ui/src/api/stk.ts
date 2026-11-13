import { get } from './http'

export const stockApi = {
  quantPage: (params: any) => get<any>('/admin-api/stk/quant/page', params),
  movePage: (params: any) => get<any>('/admin-api/stk/move/page', params)
}
