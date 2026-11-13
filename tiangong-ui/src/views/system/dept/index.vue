<template>
  <TreeCrudPage :config="config" />
</template>

<script setup lang="ts">
import TreeCrudPage, { type TreeCrudConfig } from '@/components/TreeCrudPage.vue'
import { deptApi } from '@/api/system'

const statusOptions = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 }
]

const config: TreeCrudConfig = {
  title: '部门',
  permission: 'system:dept',
  bodyAsQuery: true,
  api: { list: deptApi.list, create: deptApi.create, update: deptApi.update, delete: deptApi.delete },
  fields: [
    { prop: 'name', label: '部门名称', required: true },
    { prop: 'parentId', label: '上级部门' },
    { prop: 'sort', label: '排序', type: 'number', width: 90 },
    { prop: 'status', label: '状态', type: 'select', options: statusOptions, successValue: 0, width: 90 }
  ]
}
</script>
