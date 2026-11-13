<template>
  <TreeCrudPage :config="config" />
</template>

<script setup lang="ts">
import TreeCrudPage, { type TreeCrudConfig } from '@/components/TreeCrudPage.vue'
import { categoryApi } from '@/api/bas'

const statusOptions = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 }
]

const config: TreeCrudConfig = {
  title: '产品分类',
  permission: 'bas:product-category',
  bodyAsQuery: true,
  api: { list: categoryApi.list, create: categoryApi.create, update: categoryApi.update, delete: categoryApi.delete },
  fields: [
    { prop: 'name', label: '分类名称', required: true },
    { prop: 'parentId', label: '上级分类' },
    { prop: 'sort', label: '排序', type: 'number', width: 90 },
    { prop: 'status', label: '状态', type: 'select', options: statusOptions, successValue: 0, width: 90 }
  ]
}
</script>
