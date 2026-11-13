<template>
  <div class="page-card">
    <el-card shadow="never">
      <div class="table-toolbar">
        <span style="font-weight: 600">公告通知</span>
        <el-button v-perm="'infra:notice:create'" type="primary" :icon="Plus" @click="openCreate">发布公告</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="content" label="内容" min-width="260" />
        <el-table-column prop="publishUser" label="发布人" width="110" />
        <el-table-column prop="publishTime" label="发布时间" width="170">
          <template #default="{ row }">{{ (row.publishTime || '').replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? '已发布' : '已撤回' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" v-perm="'infra:notice:delete'" link type="warning"
                       @click="withdraw(row)">撤回</el-button>
            <el-button v-perm="'infra:notice:delete'" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { noticeApi } from '@/api/more'

const loading = ref(false)
const rows = ref<any[]>([])
const visible = ref(false)
const form = reactive({ title: '', content: '' })

async function load() {
  loading.value = true
  try {
    rows.value = (await noticeApi.list()) ?? []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.title = ''
  form.content = ''
  visible.value = true
}

async function save() {
  if (!form.title) {
    ElMessage.warning('标题不能为空')
    return
  }
  await noticeApi.create({ title: form.title, content: form.content })
  ElMessage.success('发布成功')
  visible.value = false
  load()
}

async function withdraw(row: any) {
  await ElMessageBox.confirm(`撤回公告「${row.title}」？`, '提示', { type: 'warning' })
  await noticeApi.withdraw(row.id)
  ElMessage.success('已撤回')
  load()
}

async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该公告？', '提示', { type: 'warning' })
  await noticeApi.delete(row.id)
  ElMessage.success('删除成功')
  load()
}

import { ElMessageBox } from 'element-plus'

onMounted(load)
</script>
