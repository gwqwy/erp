<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { locationApi, warehouseApi } from '@/api/bas'

const statusOptions = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 }
]

const config: CrudConfig = {
  title: '库位',
  permission: 'bas:location',
  bodyAsQuery: true,
  api: { page: pagedList, create: locationApi.create, update: locationApi.update, delete: locationApi.delete },
  fields: [
    { prop: 'warehouseId', label: '仓库', type: 'select', required: true, inSearch: true, width: 150,
      options: async () => (await warehouseApi.list() as any[]).map((w: any) => ({ label: w.name, value: w.id })) },
    { prop: 'no', label: '库位编码', required: true, width: 140 },
    { prop: 'name', label: '库位名称', required: true, width: 180 },
    { prop: 'status', label: '状态', type: 'select', width: 100, successValue: 0, options: statusOptions }
  ]
}

/** 库位接口按仓库过滤、list 形态；未选仓库返回空 */
async function pagedList(params: any) {
  if (!params.warehouseId) return { list: [], total: 0 }
  const list = await locationApi.list(params.warehouseId)
  return { list, total: list.length }
}
</script>
