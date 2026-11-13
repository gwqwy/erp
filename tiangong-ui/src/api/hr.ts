import { del, get, post, put } from './http'

// 员工档案（人事各页面公共依赖，缺失会导致白屏——勿删）
export const employeeApi = {
  page: (params: any) => get<any>('/admin-api/hr/employee/page', params),
  create: (data: any) => post('/admin-api/hr/employee/create', data),
  update: (data: any) => put('/admin-api/hr/employee/update', data),
  delete: (id: string) => del('/admin-api/hr/employee/delete', { id })
}

export const attendanceApi = {
  page: (params: any) => get<any>('/admin-api/hr/attendance/page', params),
  checkIn: (employeeId: string) => post('/admin-api/hr/attendance/check-in', null, { employeeId }),
  checkOut: (employeeId: string) => post('/admin-api/hr/attendance/check-out', null, { employeeId }),
  save: (params: any) => post('/admin-api/hr/attendance/save', null, params),
  summary: (period: string) => get<any[]>('/admin-api/hr/attendance/summary', { period })
}

export const overtimeApi = {
  page: (params: any) => get<any>('/admin-api/hr/overtime/page', params),
  create: (params: any) => post('/admin-api/hr/overtime/create', null, params),
  submitApproval: (id: string) => post('/admin-api/hr/overtime/submit-approval', null, { id }),
  audit: (id: string) => post('/admin-api/hr/overtime/audit', null, { id }),
  delete: (id: string) => del('/admin-api/hr/overtime/delete', { id })
}

export const tripApi = {
  page: (params: any) => get<any>('/admin-api/hr/trip/page', params),
  create: (params: any) => post('/admin-api/hr/trip/create', null, params),
  submitApproval: (id: string) => post('/admin-api/hr/trip/submit-approval', null, { id }),
  audit: (id: string) => post('/admin-api/hr/trip/audit', null, { id }),
  delete: (id: string) => del('/admin-api/hr/trip/delete', { id })
}

export const resignationApi = {
  page: (params: any) => get<any>('/admin-api/hr/resignation/page', params),
  create: (params: any) => post('/admin-api/hr/resignation/create', null, params),
  submitApproval: (id: string) => post('/admin-api/hr/resignation/submit-approval', null, { id }),
  audit: (id: string) => post('/admin-api/hr/resignation/audit', null, { id }),
  delete: (id: string) => del('/admin-api/hr/resignation/delete', { id })
}

export const rewardPenaltyApi = {
  page: (params: any) => get<any>('/admin-api/hr/reward-penalty/page', params),
  create: (params: any) => post('/admin-api/hr/reward-penalty/create', null, params),
  delete: (id: string) => del('/admin-api/hr/reward-penalty/delete', { id })
}

export const leaveApi = {
  page: (params: any) => get<any>('/admin-api/hr/leave/page', params),
  create: (params: any) => post('/admin-api/hr/leave/create', null, params),
  audit: (id: string) => post('/admin-api/hr/leave/audit', null, { id }),
  submitApproval: (id: string) => post('/admin-api/hr/leave/submit-approval', null, { id }),
  delete: (id: string) => del('/admin-api/hr/leave/delete', { id })
}

export const salaryApi = {
  page: (params: any) => get<any>('/admin-api/hr/salary/page', params),
  create: (params: any) => post('/admin-api/hr/salary/create', null, params),
  delete: (id: string) => del('/admin-api/hr/salary/delete', { id })
}

export const contractApi = {
  page: (params: any) => get<any>('/admin-api/hr/contract/page', params),
  create: (params: any) => post('/admin-api/hr/contract/create', null, params),
  delete: (id: string) => del('/admin-api/hr/contract/delete', { id })
}

export const payrollApi = {
  calculate: (period: string, employeeId: string) =>
    get<any>('/admin-api/hr/payroll/calculate', { period, employeeId }),
  generate: (period: string, employeeId: string, remark?: string) =>
    post('/admin-api/hr/payroll/generate', null, { period, employeeId, remark })
}

// ========== 招聘 / 培训 / 绩效（Hr 扩展端点） ==========
export const recruitApi = {
  list: () => get<any[]>('/admin-api/hr/recruit/list'),
  create: (params: any) => post('/admin-api/hr/recruit/create', null, params),
  delete: (id: string) => del('/admin-api/hr/recruit/delete', { id })
}

export const trainingApi = {
  list: () => get<any[]>('/admin-api/hr/training/list'),
  create: (params: any) => post('/admin-api/hr/training/create', null, params),
  delete: (id: string) => del('/admin-api/hr/training/delete', { id })
}

export const performanceApi = {
  list: (params?: any) => get<any[]>('/admin-api/hr/performance/list', params),
  create: (params: any) => post('/admin-api/hr/performance/create', null, params),
  delete: (id: string) => del('/admin-api/hr/performance/delete', { id })
}
