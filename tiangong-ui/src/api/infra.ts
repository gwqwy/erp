import { del, get, post } from './http'

export const formApi = {
  defList: () => get<any[]>('/admin-api/infra/form/def/list'),
  defCreate: (params: any) => post('/admin-api/infra/form/def/create', null, params),
  defUpdate: (params: any) => (post('/admin-api/infra/form/def/update', null, params)),
  defDelete: (id: string) => del('/admin-api/infra/form/def/delete', { id }),
  dataPage: (params: any) => get<any>('/admin-api/infra/form/data/page', params),
  dataCreate: (params: any) => post('/admin-api/infra/form/data/create', null, params),
  dataDelete: (id: string) => del('/admin-api/infra/form/data/delete', { id })
}

export const qualityCheckApi = {
  page: (params: any) => get<any>('/admin-api/mrp/quality-check/page', params),
  create: (params: any) => post('/admin-api/mrp/quality-check/create', null, params),
  delete: (id: string) => del('/admin-api/mrp/quality-check/delete', { id })
}

// ========== 低代码表单数据填报 ==========
export const formDataApi = {
  defList: () => get<any[]>('/admin-api/infra/form/def/list'),
  defCreate: (params: any) => post('/admin-api/infra/form/def/create', null, params),
  defDelete: (id: string) => del('/admin-api/infra/form/def/delete', { id }),
  dataPage: (params: any) => get<any>('/admin-api/infra/form/data/page', params),
  dataCreate: (params: any) => post('/admin-api/infra/form/data/create', null, params),
  dataDelete: (id: string) => del('/admin-api/infra/form/data/delete', { id })
}

// ========== CSV 数据导入（V14） ==========
export const bulkImportApi = {
  upload: (kind: 'products' | 'partners' | 'stock-openings', file: File) => {
    const form = new FormData()
    form.append('file', file)
    return post('/admin-api/infra/import/' + kind, form)
  }
}
