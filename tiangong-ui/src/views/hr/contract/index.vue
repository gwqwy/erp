<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { contractApi } from '@/api/hr'
import { employeeApi } from '@/api/hr'

const statusOptions = [
  { label: '履行中', value: 0 },
  { label: '已到期', value: 1 },
  { label: '已解除', value: 2 }
]

const config: CrudConfig = {
  title: '劳动合同',
  permission: 'hr:contract',
  bodyAsQuery: true,
  api: { page: contractApi.page, create: contractApi.create, delete: contractApi.delete },
  fields: [
    { prop: 'no', label: '合同号', width: 150, inForm: false },
    { prop: 'employeeId', label: '员工', type: 'select', required: true, width: 130,
      options: async () => (await employeeApi.page({ pageNo: 1, pageSize: 100 })).list
        .map((e: any) => ({ label: e.name, value: e.id })) },
    { prop: 'startDate', label: '起始', type: 'date', width: 110 },
    { prop: 'endDate', label: '到期', type: 'date', width: 110 },
    { prop: 'status', label: '状态', type: 'select', width: 100, successValue: 0, options: statusOptions },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}
</script>
