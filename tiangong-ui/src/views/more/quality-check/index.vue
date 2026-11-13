<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { qualityCheckApi } from '@/api/infra'
import { productApi } from '@/api/bas'

const typeOptions = [
  { label: '来料检 IQC', value: 'iqc' },
  { label: '完工检 FQC', value: 'fqc' }
]
const resultOptions = [
  { label: '合格', value: 1 },
  { label: '不合格', value: 2 },
  { label: '让步接收', value: 3 }
]

const config: CrudConfig = {
  title: '质检记录',
  permission: 'mrp:quality',
  bodyAsQuery: true,
  api: { page: qualityCheckApi.page, create: qualityCheckApi.create, update: qualityCheckApi.create, delete: qualityCheckApi.delete },
  fields: [
    { prop: 'no', label: '质检单号', required: true, inSearch: true, width: 140 },
    { prop: 'checkType', label: '检验类型', type: 'select', required: true, width: 110, options: typeOptions },
    {
      prop: 'productId',
      label: '产品',
      type: 'select',
      required: true,
      width: 160,
      options: async () => (await productApi.page({ pageNo: 1, pageSize: 200, status: 0 })).list
        .map((p: any) => ({ label: p.name, value: p.id }))
    },
    { prop: 'checkQty', label: '检验数量', type: 'number', precision: 2, width: 110 },
    { prop: 'qualifiedQty', label: '合格数量', type: 'number', precision: 2, width: 110 },
    { prop: 'result', label: '结论', type: 'select', width: 100, successValue: 1, inSearch: true, options: resultOptions },
    { prop: 'inspector', label: '检验员', width: 100 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}
</script>
