import instance, { del, get, post } from './http'

/** 文件中心：分片上传（秒传/断点续传）/ 列表 / 下载 / 删除 */
export const fileApi = {
  list: (params?: any) => get<any[]>('/admin-api/file/list', params),
  delete: (id: string) => del('/admin-api/file/delete', { id }),
  /** 秒传检测：命中返回已有文件记录 */
  checkMd5: (md5: string) => get<any>('/admin-api/file/check-md5', { md5 }),
  /** 初始化分片会话：mode = instant | resume | new */
  chunkInit: (params: { fileName: string; md5: string; totalChunks: number; totalSize: number }) =>
    post<any>('/admin-api/file/chunk/init', null, params),
  /** 逐片上传（5MB 一片，FormData） */
  chunkUpload: (uploadId: string, chunkIndex: number, blob: Blob) => {
    const form = new FormData()
    form.append('file', blob)
    return post<boolean>(
      '/admin-api/file/chunk/upload',
      form,
      { uploadId, chunkIndex }
    )
  },
  /** 合并分片 */
  chunkMerge: (params: { uploadId: string; fileName: string; md5: string; totalChunks: number }) =>
    post<any>('/admin-api/file/chunk/merge', null, params),
  /** 直传小文件并关联业务单据（附件） */
  uploadTo: (file: File, bizType: string, bizId: string) => {
    const form = new FormData()
    form.append('file', file)
    return post<any>('/admin-api/file/upload', form, { bizType, bizId })
  },
  /** 下载（带 Authorization，blob 落地） */
  download: async (id: string, fileName: string) => {
    const res: any = await instance.get('/admin-api/file/download', {
      params: { id },
      responseType: 'blob'
    })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    a.click()
    URL.revokeObjectURL(url)
  }
}
