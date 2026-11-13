<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { templateApi, voucherApi } from '@/api/fin'

const config: CrudConfig = {
  title: '凭证模板',
  permission: 'fin:template',
  bodyAsQuery: true,
  api: { page: templateApi.page, create: templateApi.create, update: templateApi.update, delete: templateApi.delete },
  fields: [
    {
      prop: 'bizType',
      label: '业务类型',
      type: 'select',
      required: true,
      width: 130,
      disabledOnUpdate: true,
      options: [
        { label: '销售出库', value: 'sale_out' },
        { label: '采购入库', value: 'purchase_in' },
        { label: '收款', value: 'receipt' },
        { label: '付款', value: 'payment' }
      ]
    },
    {
      prop: 'event',
      label: '触发事件',
      type: 'select',
      required: true,
      width: 100,
      disabledOnUpdate: true,
      options: [{ label: '审核', value: 'audit' }]
    },
    {
      prop: 'subjectId',
      label: '会计科目',
      type: 'select',
      required: true,
      width: 160,
      options: async () => (await voucherApi.subjectList()).map(s => ({ label: `${s.no} ${s.name}`, value: s.id }))
    },
    {
      prop: 'direction',
      label: '借贷方向',
      type: 'select',
      required: true,
      width: 100,
      options: [
        { label: '借方', value: 'debit' },
        { label: '贷方', value: 'credit' }
      ]
    },
    { prop: 'summary', label: '分录摘要' },
    {
      prop: 'enabled',
      label: '启用',
      type: 'select',
      width: 80,
      successValue: 1,
      options: [
        { label: '启用', value: 1 },
        { label: '停用', value: 0 }
      ]
    }
  ]
}
</script>
