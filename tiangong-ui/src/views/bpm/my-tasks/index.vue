<template>
  <div class="page-card">
    <el-card shadow="never">
      <div class="table-toolbar">
        <span style="font-weight: 600">我的待办审批</span>
        <el-button :icon="Refresh" circle @click="load" />
      </div>
      <el-table :data="tasks" v-loading="loading" border stripe>
        <el-table-column label="业务类型" width="140">
          <template #default="{ row }">{{ bizLabel(row.bizType) }}</template>
        </el-table-column>
        <el-table-column prop="bizNo" label="单号" width="160" />
        <el-table-column prop="level" label="级次" width="80" align="center" />
        <el-table-column prop="createTime" label="提交时间" width="170">
          <template #default="{ row }">{{ (row.createTime || '').replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handle(row, true)">通过</el-button>
            <el-button type="danger" size="small" @click="handle(row, false)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!tasks.length && !loading" description="暂无待办 🎉" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { bpmApi } from '@/api/bpm'

const BIZ_LABELS: Record<string, string> = {
  pur_order: '采购订单',
  pur_inbound: '采购入库',
  sal_order: '销售订单',
  sal_outbound: '销售出库',
  hr_leave: '请假申请'
}

const loading = ref(false)
const tasks = ref<any[]>([])

function bizLabel(t: string) {
  return BIZ_LABELS[t] ?? t
}

async function load() {
  loading.value = true
  try {
    tasks.value = (await bpmApi.myTasks()) ?? []
  } finally {
    loading.value = false
  }
}

async function handle(row: any, pass: boolean) {
  const action = pass ? '通过' : '驳回'
  const { value } = await ElMessageBox.prompt(`确认${action}「${row.bizNo}」第 ${row.level} 级审批？`, action, {
    inputPlaceholder: '审批意见（可空）',
    inputValue: ''
  })
  if (pass) await bpmApi.approve(row.id, value)
  else await bpmApi.reject(row.id, value)
  ElMessage.success(`已${action}`)
  load()
}

onMounted(load)
</script>
