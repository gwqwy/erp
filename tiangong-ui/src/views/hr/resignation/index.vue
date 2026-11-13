<template>
  <ApplyPage :config="config" />
</template>

<script setup lang="ts">
import ApplyPage from '../apply-page.vue'
import { resignationApi } from '@/api/hr'
import { employeeApi } from '@/api/hr'

const employeeOptions = async () =>
  (await employeeApi.page({ pageNo: 1, pageSize: 100, status: 0 })).list
    .map((e: any) => ({ label: e.name, value: e.id }))

const statusOptions = [
  { label: '草稿', value: 10 },
  { label: '审批中', value: 20 },
  { label: '已通过', value: 30 },
  { label: '已驳回', value: 50 }
]

const config = {
  title: '离职申请',
  permission: 'hr:resign',
  api: {
    page: resignationApi.page,
    create: resignationApi.create,
    delete: resignationApi.delete,
    submitApproval: resignationApi.submitApproval,
    audit: resignationApi.audit
  },
  fields: [
    { prop: 'no', label: '单号', width: 150, inForm: false },
    { prop: 'employeeId', label: '员工', type: 'select', required: true, options: employeeOptions },
    { prop: 'planLeaveDate', label: '计划离职日', type: 'date', width: 130 },
    { prop: 'status', label: '状态', type: 'select', options: statusOptions, inSearch: true, width: 100 },
    { prop: 'reason', label: '原因', type: 'textarea', span: 24, inTable: false }
  ]
}
</script>
