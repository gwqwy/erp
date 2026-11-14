import { get } from './http'

export const dashboardApi = {
  summary: (months = 6) => get<any>('/admin-api/rpt/dashboard/summary', { months })
}

// ========== 经营分析（V14）：排行 / 库存 ABC / 客户对账单 ==========
export const analysisApi = {
  saleRank: (params?: any) => get<any[]>('/admin-api/rpt/sale-rank', params),
  purchaseRank: (params?: any) => get<any[]>('/admin-api/rpt/purchase-rank', params),
  stockAbc: () => get<any[]>('/admin-api/rpt/stock-abc'),
  customerStatement: (params: { partnerId: string; from: string; to: string }) =>
    get<any>('/admin-api/rpt/customer-statement', params)
}

// ========== V16：收发存汇总 / 供应商对账单 ==========
export const stockFlowApi = {
  summary: (params: { from: string; to: string }) => get<any[]>('/admin-api/rpt/stock-flow', params)
}

export const supplierStatementApi = {
  detail: (params: { partnerId: string; from: string; to: string }) =>
    get<any>('/admin-api/rpt/supplier-statement', params)
}
