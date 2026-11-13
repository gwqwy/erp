<template>
  <OrderPage :config="config" />
</template>

<script setup lang="ts">
import OrderPage, { type OrderConfig } from '@/components/OrderPage.vue'
import { receiptApi, voucherApi } from '@/api/fin'

const config: OrderConfig = {
  title: '收款单',
  permission: 'fin:receipt',
  bizType: 'finance_receipt',
  headKey: 'receipt',
  api: {
    page: receiptApi.page,
    detail: receiptApi.detail,
    create: receiptApi.create,
    delete: receiptApi.delete,
    audit: receiptApi.audit,
    voidOrder: receiptApi.voidOrder
  },
  masterFields: [
    { prop: 'no', label: '单号', type: 'input', width: 150 },
    { prop: 'partnerId', label: '客户', type: 'partner-select', partnerType: 'customer', required: true },
    {
      prop: 'accountId',
      label: '结算账户',
      type: 'select',
      required: true,
      options: async () => (await voucherApi.accountList()).map(a => ({ label: a.name, value: a.id }))
    },
    { prop: 'receiptDate', label: '收款日期', type: 'date' },
    { prop: 'status', label: '状态', type: 'input', width: 90 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ],
  itemFields: [
    {
      prop: 'receivableId',
      label: '核销应收',
      type: 'select',
      width: 280,
      options: async () => (await receiptApi.openReceivables()).map(r => ({
        label: `${r.bizNo}（未核销 ${Number(r.unreceivedAmount)}）`,
        value: r.id,
        raw: r
      }))
    },
    { prop: 'amount', label: '本次核销金额', type: 'number', width: 160, precision: 2,
      autofill: { amount: 'unreceivedAmount' } },
    { prop: 'remark', label: '备注', type: 'input' }
  ]
}
</script>
