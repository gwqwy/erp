<template>
  <OrderPage :config="config" />
</template>

<script setup lang="ts">
import OrderPage, { type OrderConfig } from '@/components/OrderPage.vue'
import { paymentApi, voucherApi } from '@/api/fin'

const config: OrderConfig = {
  title: '付款单',
  permission: 'fin:payment',
  bizType: 'finance_payment',
  headKey: 'payment',
  api: {
    page: paymentApi.page,
    detail: paymentApi.detail,
    create: paymentApi.create,
    delete: paymentApi.delete,
    audit: paymentApi.audit,
    voidOrder: paymentApi.voidOrder
  },
  masterFields: [
    { prop: 'no', label: '单号', type: 'input', width: 150 },
    { prop: 'partnerId', label: '供应商', type: 'partner-select', partnerType: 'supplier', required: true },
    {
      prop: 'accountId',
      label: '结算账户',
      type: 'select',
      required: true,
      options: async () => (await voucherApi.accountList()).map(a => ({ label: a.name, value: a.id }))
    },
    { prop: 'paymentDate', label: '付款日期', type: 'date' },
    { prop: 'status', label: '状态', type: 'input', width: 90 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ],
  itemFields: [
    {
      prop: 'payableId',
      label: '核销应付',
      type: 'select',
      width: 280,
      options: async () => (await paymentApi.openPayables()).map(p => ({
        label: `${p.bizNo}（未核销 ${Number(p.unpaidAmount)}）`,
        value: p.id,
        raw: p
      }))
    },
    { prop: 'amount', label: '本次核销金额', type: 'number', width: 160, precision: 2,
      autofill: { amount: 'unpaidAmount' } },
    { prop: 'remark', label: '备注', type: 'input' }
  ]
}
</script>
