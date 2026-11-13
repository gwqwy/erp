<template>
  <TreeCrudPage :config="config" />
</template>

<script setup lang="ts">
import TreeCrudPage, { type TreeCrudConfig } from '@/components/TreeCrudPage.vue'
import { menuApi } from '@/api/system'

const statusOptions = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 }
]
const typeOptions = [
  { label: '目录', value: 1 },
  { label: '菜单', value: 2 },
  { label: '按钮', value: 3 }
]

const config: TreeCrudConfig = {
  title: '菜单',
  permission: 'system:menu',
  api: {
    list: menuApi.list,
    create: menuApi.create,
    update: menuApi.update,
    delete: menuApi.delete
  },
  fields: [
    { prop: 'name', label: '菜单名称', required: true },
    { prop: 'type', label: '类型', type: 'select', options: typeOptions, required: true, width: 90 },
    { prop: 'permission', label: '权限标识', width: 210 },
    { prop: 'path', label: '路由路径', width: 130 },
    { prop: 'component', label: '组件路径', width: 190 },
    { prop: 'parentId', label: '上级菜单' },
    { prop: 'sort', label: '排序', type: 'number', width: 80 },
    { prop: 'status', label: '状态', type: 'select', options: statusOptions, successValue: 0, width: 90 }
  ]
}
</script>
