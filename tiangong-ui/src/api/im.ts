import { get, post } from './http'
import { getToken } from '@/utils/auth'

/** IM 消息：会话 / 历史 / REST 发送 */
export const imApi = {
  sessions: () => get<any[]>('/admin-api/im/sessions'),
  history: (sessionKey: string) => get<any[]>('/admin-api/im/history', { sessionKey }),
  send: (receiverId: string, content: string) =>
    post<any>('/admin-api/im/send', null, { receiverId, content })
}

/** IM WebSocket 地址（同源代理 /ws → 后端） */
export function imWsUrl(): string {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  return proto + '://' + location.host + '/ws/im/' + getToken()
}
