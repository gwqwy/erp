<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { performanceApi, employeeApi } from '@/api/hr'

const config: CrudConfig = {
  title: '绩效考核',
  permission: 'hr:performance',
  bodyAsQuery: true,
  api: { page: pagedList, create: performanceApi.create, delete: performanceApi.delete },
  fields: [
    { prop: 'no', label: '单号', width: 150, inForm: false },
    { prop: 'employeeId', label: '员工', type: 'select', required: true, inSearch: true, width: 130,
      options: async () => (await employeeApi.page({ pageNo: 1, pageSize: 200 })).list
        .map((e: any) => ({ label: e.name, value: e.id })) },
    { prop: 'period', label: '考核期', required: true, inSearch: true, width: 120 },
    { prop: 'score', label: '得分', type: 'number', required: true, width: 100 },
    { prop: 'grade', label: '等级', width: 90 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

async function pagedList(params: any) {
  const list = await performanceApi.list({ employeeId: params.employeeId, period: params.period })
  return { list, total: list.length }
}
</script>
