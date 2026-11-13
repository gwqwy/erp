import { get, post, put } from './http'

const PREFIX = '/admin-api/bpm'

export const bpmApi = {
  myTasks: () => get<any[]>(`${PREFIX}/my-tasks`),
  approve: (taskId: string, comment?: string) => post(`${PREFIX}/approve`, { taskId, comment }),
  reject: (taskId: string, comment?: string) => post(`${PREFIX}/reject`, { taskId, comment }),
  taskPage: (params: any) => get<any>(`${PREFIX}/task/page`, params),
  configPage: (params: any) => get<any>(`${PREFIX}/config/page`, params),
  configCreate: (params: any) => post(`${PREFIX}/config/create`, null, params),
  configUpdate: (params: any) => put(`${PREFIX}/config/update`, null, params),
  configDelete: (id: string) => (del(`${PREFIX}/config/delete`, { id }))
}

import { del } from './http'
