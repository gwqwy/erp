<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { categoryApi, productApi, unitApi } from '@/api/bas'

const statusOptions = [
  { label: '启用', value: 0 },
  { label: '停用', value: 1 }
]

const config: CrudConfig = {
  title: '产品',
  permission: 'bas:product',
  api: { page: productApi.page, create: productApi.create, update: productApi.update, delete: productApi.delete },
  fields: [
    { prop: 'no', label: '产品编码', width: 140, inForm: false },
    { prop: 'name', label: '产品名称', required: true, inSearch: true },
    {
      prop: 'categoryId',
      label: '产品分类',
      type: 'select',
      options: async () => (await categoryApi.list({ status: 0 })).map((c: any) => ({ label: c.name, value: c.id }))
    },
    {
      prop: 'unitId',
      label: '计量单位',
      type: 'select',
      options: async () => (await unitApi.list({ status: 0 })).map((u: any) => ({ label: u.name, value: u.id }))
    },
    { prop: 'spec', label: '规格型号' },
    { prop: 'barcode', label: '条码', inTable: false },
    { prop: 'salePrice', label: '销售价', type: 'number', precision: 2, width: 110 },
    { prop: 'purchasePrice', label: '采购价', type: 'number', precision: 2, width: 110, inTable: false },
    { prop: 'taxRate', label: '税率', type: 'number', precision: 4, width: 100, inTable: false },
    { prop: 'stockMin', label: '库存下限', type: 'number', inTable: false },
    { prop: 'stockMax', label: '库存上限', type: 'number', inTable: false },
    { prop: 'isBatch', label: '批次管理', type: 'switch', width: 90 },
    { prop: 'isSerial', label: '序列号', type: 'switch', width: 90 },
    { prop: 'status', label: '状态', type: 'select', options: statusOptions, successValue: 0, inSearch: true, width: 90 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}
</script>
