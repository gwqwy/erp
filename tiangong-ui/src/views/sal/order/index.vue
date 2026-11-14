<template>
  <OrderPage :config="config" />
</template>

<script setup lang="ts">
import OrderPage, { type OrderConfig } from '@/components/OrderPage.vue'
import { saleOrderApi } from '@/api/sal'

const config: OrderConfig = {
  title: '销售订单',
  permission: 'sal:order',
  bizType: 'sal_order',
  headKey: 'order',
  deposit: { get: saleOrderApi.depositGet, register: saleOrderApi.depositRegister },
  api: {
    page: saleOrderApi.page,
    detail: saleOrderApi.detail,
    create: saleOrderApi.create,
    update: saleOrderApi.update,
    delete: saleOrderApi.delete,
    audit: saleOrderApi.audit,
    voidOrder: saleOrderApi.voidOrder,
    submitApproval: saleOrderApi.submitApproval
  },
  masterFields: [
    { prop: 'no', label: '单号', type: 'input', width: 150 },
    { prop: 'customerId', label: '客户', type: 'partner-select', partnerType: 'customer', required: true },
    { prop: 'orderDate', label: '订单日期', type: 'date' },
    { prop: 'expectDate', label: '期望交期', type: 'date' },
    { prop: 'status', label: '状态', type: 'input', width: 90 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ],
  itemFields: [
    {
      prop: 'productId',
      label: '产品',
      type: 'product-select',
      width: 240,
      autofill: { unitId: 'unitId', price: 'salePrice' }
    },
    { prop: 'qty', label: '数量', type: 'number', width: 140, precision: 2 },
    { prop: 'price', label: '含税单价', type: 'number', width: 140, precision: 2 },
    { prop: 'amount', label: '金额', type: 'number', readonly: true, width: 130 },
    { prop: 'remark', label: '备注', type: 'input' }
  ]
}
</script>
