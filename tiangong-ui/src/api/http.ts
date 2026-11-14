import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/auth'
import router from '@/router'

/**
 * 统一 HTTP 封装：
 * - 自动携带 Bearer Token
 * - CommonResult 解包：code=0 返回 data；401 清 Token 回登录页；其余 toast 后 reject
 */
const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 15000
})

instance.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') {
      return response
    }
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0) {
        return body.data
      }
      if (body.code === 401) {
        removeToken()
        if (router.currentRoute.value.path !== '/login') {
          router.push('/login')
        }
        return Promise.reject(body)
      }
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(body)
    }
    return body
  },
  error => {
    ElMessage.error(error?.message || '网络异常')
    return Promise.reject(error)
  }
)

export function get<T = any>(url: string, params?: any): Promise<T> {
  return instance.get(url, { params }) as Promise<T>
}

export function post<T = any>(url: string, data?: any, params?: any): Promise<T> {
  return instance.post(url, data, { params }) as Promise<T>
}

export function put<T = any>(url: string, data?: any, params?: any): Promise<T> {
  return instance.put(url, data, { params }) as Promise<T>
}

export function del<T = any>(url: string, params?: any): Promise<T> {
  return instance.delete(url, { params }) as Promise<T>
}

/** Blob 下载（带鉴权），用于报表/单据 xlsx 导出 */
export async function downloadFile(url: string, params: any, fileName: string) {
  const res: any = await instance.get(url, { params, responseType: 'blob' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(res.data)
  a.download = fileName
  a.click()
  URL.revokeObjectURL(a.href)
}

export default instance
