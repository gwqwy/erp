<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { partnerApi } from '@/api/bas'

const statusOptions = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 }
]

const config: CrudConfig = {
  title: '往来单位',
  permission: 'bas:partner',
  api: { page: partnerApi.page, create: partnerApi.create, update: partnerApi.update, delete: partnerApi.delete },
  fields: [
    { prop: 'no', label: '编码', width: 130, inForm: false },
    { prop: 'name', label: '单位名称', required: true, inSearch: true },
    { prop: 'shortName', label: '简称', inTable: false },
    { prop: 'isCustomer', label: '客户', type: 'switch', width: 80 },
    { prop: 'isSupplier', label: '供应商', type: 'switch', width: 90 },
    { prop: 'contact', label: '联系人' },
    { prop: 'phone', label: '电话', width: 130 },
    { prop: 'taxNo', label: '税号', inTable: false },
    { prop: 'address', label: '地址', span: 24, inTable: false },
    { prop: 'creditLimit', label: '信用额度', type: 'number', precision: 2, inTable: false },
    { prop: 'status', label: '状态', type: 'select', options: statusOptions, successValue: 0, inSearch: true, width: 90 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}
</script>
