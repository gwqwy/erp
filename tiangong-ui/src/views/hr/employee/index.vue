<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { employeeApi } from '@/api/hr'
import { get } from '@/api/http'

const deptOptions = async () =>
  (await get<any[]>('/admin-api/system/dept/list')).map((d: any) => ({ label: d.name, value: d.id }))

const statusOptions = [
  { label: '在职', value: 0 },
  { label: '离职', value: 1 }
]

const config: CrudConfig = {
  title: '员工',
  permission: 'hr:employee',
  api: { page: employeeApi.page, create: employeeApi.create, update: employeeApi.update, delete: employeeApi.delete },
  fields: [
    { prop: 'no', label: '工号', width: 110 },
    { prop: 'name', label: '姓名', required: true, inSearch: true },
    { prop: 'deptId', label: '部门', type: 'select', options: deptOptions, inSearch: true },
    { prop: 'post', label: '岗位' },
    { prop: 'baseSalary', label: '基本工资', type: 'number', precision: 2, width: 120 },
    { prop: 'phone', label: '手机', width: 130 },
    { prop: 'email', label: '邮箱', inTable: false },
    { prop: 'entryDate', label: '入职日期', type: 'date', width: 120 },
    { prop: 'leaveDate', label: '离职日期', type: 'date', width: 120, inTable: false },
    { prop: 'status', label: '状态', type: 'select', options: statusOptions, successValue: 0, inSearch: true, width: 90 }
  ]
}
</script>
