import { get, post } from './http'

export const chatterApi = {
  list: (bizType: string, bizId: string) =>
    get<any[]>(`/admin-api/common/chatter/list`, { bizType, bizId }),
  comment: (data: { bizType: string; bizId: string; content: string }) =>
    post<Long>(`/admin-api/common/chatter/comment`, data)
}
