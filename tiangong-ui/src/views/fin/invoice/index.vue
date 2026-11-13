<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { invoiceApi } from '@/api/fin'

const typeOptions = [
  { label: '采购发票', value: 'PUR' },
  { label: '销售发票', value: 'SAL' }
]

const config: CrudConfig = {
  title: '发票',
  permission: 'fin:invoice',
  bodyAsQuery: true,
  api: { page: pagedList, create: invoiceApi.create, delete: invoiceApi.delete },
  fields: [
    { prop: 'type', label: '类型', type: 'select', required: true, inSearch: true, width: 110,
      options: typeOptions,
      formatter: (v: string) => typeOptions.find(o => o.value === v)?.label || v },
    { prop: 'invoiceNo', label: '发票号', required: true, inSearch: true, width: 160 },
    { prop: 'invoiceDate', label: '开票日期', type: 'date', width: 120 },
    { prop: 'amount', label: '金额', type: 'number', required: true, width: 120 },
    { prop: 'taxAmount', label: '税额', type: 'number', width: 110 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

async function pagedList(params: any) {
  const list = await invoiceApi.list({ type: params.type, invoiceNo: params.invoiceNo })
  return { list, total: list.length }
}
</script>
