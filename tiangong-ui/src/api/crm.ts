import { del, get, post, put } from './http'

const PREFIX = '/admin-api/crm'

export const leadApi = {
  page: (params: any) => get<any>(`${PREFIX}/lead/page`, params),
  create: (data: any) => post(`${PREFIX}/lead/create`, data),
  update: (data: any) => put(`${PREFIX}/lead/update`, data),
  delete: (id: string) => del(`${PREFIX}/lead/delete`, { id }),
  convert: (params: { id: string; partnerId: string; amount?: number; expectDate?: string }) =>
    post<any>(`${PREFIX}/lead/convert`, null, params)
}

export const businessApi = {
  page: (params: any) => get<any>(`${PREFIX}/business/page`, params),
  stageStats: () => get<Record<string, long[]>>(`${PREFIX}/business/stage-stats`),
  create: (data: any) => post(`${PREFIX}/business/create`, data),
  update: (data: any) => put(`${PREFIX}/business/update`, data),
  updateStage: (id: string, stage: number) => put(`${PREFIX}/business/stage`, null, { id, stage }),
  delete: (id: string) => del(`${PREFIX}/business/delete`, { id })
}
