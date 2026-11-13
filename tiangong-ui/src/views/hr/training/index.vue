<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { trainingApi } from '@/api/hr'

const config: CrudConfig = {
  title: '培训记录',
  permission: 'hr:training',
  bodyAsQuery: true,
  api: { page: pagedList, create: trainingApi.create, delete: trainingApi.delete },
  fields: [
    { prop: 'no', label: '单号', width: 150, inForm: false },
    { prop: 'title', label: '培训主题', required: true, width: 180 },
    { prop: 'trainer', label: '讲师', width: 130 },
    { prop: 'trainDate', label: '培训日期', type: 'date', width: 130 },
    { prop: 'employees', label: '参训人员', width: 200 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

async function pagedList(_params: any) {
  const list = await trainingApi.list()
  return { list, total: list.length }
}
</script>
