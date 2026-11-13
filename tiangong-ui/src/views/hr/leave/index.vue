<template>
  <CrudPage :config="config">
    <template #actions="{ row }">
      <el-button v-if="row.status === 10" v-perm="'hr:leave:audit'" link type="warning"
                 @click="submit(row)">提交审批</el-button>
      <el-button v-if="row.status === 10" v-perm="'hr:leave:audit'" link type="success"
                 @click="audit(row)">直接审核</el-button>
    </template>
  </CrudPage>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { leaveApi } from '@/api/hr'
import { employeeApi } from '@/api/hr'

const statusOptions = [
  { label: '草稿', value: 10 },
  { label: '审批中', value: 20 },
  { label: '已通过', value: 30 },
  { label: '已驳回', value: 50 }
]

const employeeOptions = async () =>
  (await employeeApi.page({ pageNo: 1, pageSize: 100, status: 0 })).list
    .map((e: any) => ({ label: `${e.name}（${e.no || ''}）`, value: e.id }))

const config: CrudConfig = {
  title: '请假申请',
  permission: 'hr:leave',
  api: { page: leaveApi.page, create: leaveApi.create, delete: leaveApi.delete },
  fields: [
    { prop: 'no', label: '单号', width: 150, inForm: false },
    { prop: 'employeeId', label: '员工', type: 'select', required: true, options: employeeOptions },
    { prop: 'leaveType', label: '类型', required: true, width: 90 },
    { prop: 'startDate', label: '开始日期', type: 'date', width: 120 },
    { prop: 'endDate', label: '结束日期', type: 'date', width: 120 },
    { prop: 'status', label: '状态', type: 'select', options: statusOptions, inSearch: true, width: 100 },
    { prop: 'reason', label: '事由', type: 'textarea', span: 24, inTable: false }
  ]
}

async function submit(row: any) {
  await ElMessageBox.confirm('提交到审批流（未配置审批流会提示改用直接审核）？', '提示', { type: 'warning' })
  await leaveApi.submitApproval(row.id)
  ElMessage.success('已提交审批')
  location.reload()
}

async function audit(row: any) {
  await ElMessageBox.confirm('确认直接通过该请假申请吗？', '提示', { type: 'warning' })
  await leaveApi.audit(row.id)
  ElMessage.success('审核通过')
  location.reload()
}
</script>
