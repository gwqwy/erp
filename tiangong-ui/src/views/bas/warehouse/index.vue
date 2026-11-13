<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { warehouseApi } from '@/api/bas'

const statusOptions = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 }
]

const config: CrudConfig = {
  title: '仓库',
  permission: 'bas:warehouse',
  api: { page: pagedList, create: warehouseApi.create, update: warehouseApi.update, delete: warehouseApi.delete },
  fields: [
    { prop: 'no', label: '编码', width: 120, inForm: false },
    { prop: 'name', label: '仓库名称', required: true },
    { prop: 'address', label: '地址' },
    { prop: 'isQualityCheck', label: '收货质检', type: 'switch', width: 100 },
    { prop: 'status', label: '状态', type: 'select', options: statusOptions, successValue: 0, width: 90 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

/** 仓库接口为 list 形态，包装为分页结构 */
async function pagedList(params: any) {
  const list = await warehouseApi.list()
  return { list, total: list.length }
}
</script>
