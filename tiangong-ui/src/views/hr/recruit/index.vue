<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { recruitApi } from '@/api/hr'

const statusOptions = [
  { label: '招聘中', value: 0 },
  { label: '已完成', value: 1 },
  { label: '已关闭', value: 2 }
]

const config: CrudConfig = {
  title: '招聘',
  permission: 'hr:recruit',
  bodyAsQuery: true,
  api: { page: pagedList, create: recruitApi.create, delete: recruitApi.delete },
  fields: [
    { prop: 'no', label: '单号', width: 150, inForm: false },
    { prop: 'position', label: '岗位', required: true, width: 160 },
    { prop: 'headcount', label: '人数', type: 'number', width: 90 },
    { prop: 'department', label: '部门', width: 140 },
    { prop: 'status', label: '状态', type: 'select', width: 100, successValue: 0, options: statusOptions },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

async function pagedList(_params: any) {
  const list = await recruitApi.list()
  return { list, total: list.length }
}
</script>
