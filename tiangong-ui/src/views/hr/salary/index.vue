<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { salaryApi } from '@/api/hr'
import { employeeApi } from '@/api/hr'

const employeeOptions = async () =>
  (await employeeApi.page({ pageNo: 1, pageSize: 100 })).list
    .map((e: any) => ({ label: e.name, value: e.id }))

const config: CrudConfig = {
  title: '薪资记录',
  permission: 'hr:salary',
  api: { page: salaryApi.page, create: salaryApi.create, delete: salaryApi.delete },
  fields: [
    { prop: 'period', label: '期间', required: true, width: 110, inSearch: true },
    { prop: 'employeeId', label: '员工', type: 'select', required: true, width: 130, options: employeeOptions },
    { prop: 'grossAmount', label: '应发', type: 'number', precision: 2, width: 120 },
    { prop: 'deduction', label: '扣款', type: 'number', precision: 2, width: 120 },
    { prop: 'netAmount', label: '实发', type: 'number', precision: 2, width: 120 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}
</script>
