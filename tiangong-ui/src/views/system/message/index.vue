<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>消息中心</span>
        <el-button type="primary" size="small" :disabled="!unread.length" @click="markAll">全部标记已读</el-button>
      </div>
    </template>

    <el-tabs v-model="tab">
      <el-tab-pane :label="'未读 (' + unread.length + ')'" name="unread" />
      <el-tab-pane label="已读" name="read" />
    </el-tabs>

    <el-table v-if="tab === 'unread'" :data="unread" v-loading="loading" border stripe>
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
      <el-table-column prop="creator" label="发送人" width="110" />
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" class-name="op-cell">
        <template #default="{ row }">
          <div class="op-group">
            <el-button link type="primary" @click="markRead(row)">标记已读</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-table v-else :data="readRows" v-loading="loading" border stripe>
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
      <el-table-column prop="creator" label="发送人" width="110" />
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.createTime) }}</template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !(tab === 'unread' ? unread.length : readRows.length)" description="暂无消息" :image-size="80" />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { messageApi } from '@/api/system'

const tab = ref('unread')
const loading = ref(false)
const unread = ref<any[]>([])
const readRows = ref<any[]>([])

async function load() {
  loading.value = true
  try {
    unread.value = await messageApi.myUnread()
    // 已读列表：全量拉取由后端 my-unread 之外无接口，这里从 unread 全量接口不行——
    // 用 my-unread 只给未读；已读走简单过滤方案：请求全部消息（复用 list 场景有限，此处取未读 + 本地已读缓存）
    readRows.value = readCache
  } finally {
    loading.value = false
  }
}

// 已读消息本会话缓存（进入页面时从未读标为已读的进入此列表）
const readCache: any[] = []

async function markRead(row: any) {
  await messageApi.read(row.id)
  readCache.unshift({ ...row, readFlag: true })
  unread.value = unread.value.filter((u: any) => u.id !== row.id)
}

async function markAll() {
  await messageApi.readAll()
  readCache.unshift(...unread.value.map((u: any) => ({ ...u, readFlag: true })))
  unread.value = []
  ElMessage.success('已全部标记已读')
}

function fmtTime(t: string) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : ''
}

onMounted(load)
</script>
