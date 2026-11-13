<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>期末结转（损益结转本年利润 + 锁账）</span>
        <el-input v-model="newPeriod" placeholder="如 2026-11" style="width:160px;margin-right:12px" />
        <el-button type="primary" :loading="closing" @click="handleClose">执行结转</el-button>
      </div>
    </template>
    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="period" label="会计期间" width="140" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.closed ? 'success' : 'info'">{{ row.closed ? '已结转锁账' : '未结转' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="closeUser" label="操作人" width="140" />
      <el-table-column prop="closeTime" label="结转时间" min-width="180" />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { periodCloseApi } from '@/api/fin'

const list = ref<any[]>([])
const loading = ref(false)
const closing = ref(false)
const newPeriod = ref('')

async function load() {
  loading.value = true
  try {
    list.value = await periodCloseApi.list()
  } finally {
    loading.value = false
  }
}

async function handleClose() {
  const period = newPeriod.value.trim()
  if (!/^\d{4}-\d{2}$/.test(period)) {
    ElMessage.warning('请输入期间，格式如 2026-11')
    return
  }
  await ElMessageBox.confirm(
    '结转将把当期损益科目转入本年利润并锁账，期间内单据将无法再审核。确认执行？',
    '期末结转',
    { type: 'warning' }
  )
  closing.value = true
  try {
    const msg = await periodCloseApi.close(period)
    ElMessage.success(msg || '结转完成')
    await load()
  } finally {
    closing.value = false
  }
}

onMounted(load)
</script>
