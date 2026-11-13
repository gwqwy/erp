import { get } from './http'

export const reportApi = {
  balance: (period: string) => get<any[]>('/admin-api/fin/report/balance', { period }),
  profit: (period: string) => get<any>('/admin-api/fin/report/profit', { period })
}
