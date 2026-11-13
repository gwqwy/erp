<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>文件中心（分片上传 · 秒传 · 断点续传）</span>
        <div style="display:flex;gap:12px;align-items:center">
          <el-input v-model="keyword" placeholder="按文件名搜索" clearable style="width:220px" @keyup.enter="load" />
          <el-button @click="load">查询</el-button>
          <el-upload :show-file-list="false" :auto-upload="false" accept="*/*"
                     :on-change="pickFile">
            <el-button type="primary" :loading="uploading">
              {{ uploading ? progressText : '上传文件' }}
            </el-button>
          </el-upload>
        </div>
      </div>
    </template>
    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="fileName" label="文件名" min-width="220" show-overflow-tooltip />
      <el-table-column label="大小" width="110">
        <template #default="{ row }">{{ fmtSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="md5" label="MD5" width="200" show-overflow-tooltip />
      <el-table-column prop="bizType" label="业务类型" width="100" />
      <el-table-column prop="uploaderName" label="上传人" width="110" />
      <el-table-column label="上传时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right" class-name="op-cell">
        <template #default="{ row }">
          <div class="op-group">
            <el-button link type="primary" @click="download(row)">下载</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SparkMD5 from 'spark-md5'
import { fileApi } from '@/api/file'

const CHUNK_SIZE = 5 * 1024 * 1024

const list = ref<any[]>([])
const loading = ref(false)
const uploading = ref(false)
const progressText = ref('')
const keyword = ref('')

async function load() {
  loading.value = true
  try {
    list.value = await fileApi.list({ fileName: keyword.value || undefined })
  } finally {
    loading.value = false
  }
}

function fmtSize(n: number) {
  if (!n && n !== 0) return ''
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  if (n < 1024 * 1024 * 1024) return (n / 1024 / 1024).toFixed(1) + ' MB'
  return (n / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

function fmtTime(t: string) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : ''
}

/** 选择文件 → MD5 秒传检测 → 分片上传 → 合并 */
async function pickFile(file: any) {
  const raw: File = file.raw
  if (!raw) return
  uploading.value = true
  try {
    progressText.value = '计算指纹…'
    const md5 = await md5OfFile(raw)
    // 1) 秒传
    const hit = await fileApi.checkMd5(md5)
    if (hit) {
      ElMessage.success('秒传成功：服务器已存在相同文件')
      await load()
      return
    }
    // 2) 初始化分片会话（支持断点续传）
    const totalChunks = Math.max(1, Math.ceil(raw.size / CHUNK_SIZE))
    const init = await fileApi.chunkInit({
      fileName: raw.name,
      md5,
      totalChunks,
      totalSize: raw.size
    })
    if (init.mode === 'instant') {
      ElMessage.success('秒传成功')
      await load()
      return
    }
    const uploadId = init.uploadIdOrName
    const received: number[] = init.mode === 'resume' ? init.received || [] : []
    // 3) 逐片上传（跳过已收分片）
    for (let i = 0; i < totalChunks; i++) {
      if (received.includes(i)) continue
      progressText.value = '上传中 ' + Math.round(((i + 1) / totalChunks) * 100) + '%'
      const start = i * CHUNK_SIZE
      await fileApi.chunkUpload(uploadId, i, raw.slice(start, Math.min(start + CHUNK_SIZE, raw.size)))
    }
    // 4) 合并
    progressText.value = '合并中…'
    await fileApi.chunkMerge({ uploadId, fileName: raw.name, md5, totalChunks })
    ElMessage.success('上传完成')
    await load()
  } finally {
    uploading.value = false
    progressText.value = ''
  }
}

function md5OfFile(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const spark = new SparkMD5.ArrayBuffer()
    const reader = new FileReader()
    let index = 0
    const step = 2 * 1024 * 1024
    const next = () => {
      const blob = file.slice(index, Math.min(index + step, file.size))
      reader.onload = e => {
        spark.append(e.target?.result as ArrayBuffer)
        index += step
        if (index < file.size) next()
        else resolve(spark.end())
      }
      reader.onerror = reject
      reader.readAsArrayBuffer(blob)
    }
    next()
  })
}

async function download(row: any) {
  await fileApi.download(row.id, row.fileName)
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确认删除该文件记录吗？', '提示', { type: 'warning' })
  await fileApi.delete(row.id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(load)
</script>
