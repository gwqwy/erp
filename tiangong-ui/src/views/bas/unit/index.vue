<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { unitApi } from '@/api/bas'

const statusOptions = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 }
]

const config: CrudConfig = {
  title: '计量单位',
  permission: 'bas:unit',
  bodyAsQuery: true,
  api: { page: pagedList, create: unitApi.create, update: unitApi.update, delete: unitApi.delete },
  fields: [
    { prop: 'name', label: '单位名称', required: true },
    { prop: 'status', label: '状态', type: 'select', options: statusOptions, successValue: 0, width: 90 }
  ]
}

/** 单位接口为 list 形态，包装为分页结构 */
async function pagedList(_params: any) {
  const list = await unitApi.list()
  return { list, total: list.length }
}
</script>
