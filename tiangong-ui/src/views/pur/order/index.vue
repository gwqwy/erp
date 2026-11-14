<template>
  <OrderPage :config="config" />
</template>

<script setup lang="ts">
import OrderPage, { type OrderConfig } from '@/components/OrderPage.vue'
import { purchaseOrderApi } from '@/api/pur'

const config: OrderConfig = {
  title: '采购订单',
  permission: 'pur:order',
  bizType: 'pur_order',
  headKey: 'order',
  deposit: { get: purchaseOrderApi.depositGet, register: purchaseOrderApi.depositRegister },
  api: {
    page: purchaseOrderApi.page,
    detail: purchaseOrderApi.detail,
    create: purchaseOrderApi.create,
    update: purchaseOrderApi.update,
    delete: purchaseOrderApi.delete,
    audit: purchaseOrderApi.audit,
    voidOrder: purchaseOrderApi.voidOrder,
    submitApproval: purchaseOrderApi.submitApproval
  },
  masterFields: [
    { prop: 'no', label: '单号', type: 'input', width: 150 },
    { prop: 'supplierId', label: '供应商', type: 'partner-select', partnerType: 'supplier', required: true },
    { prop: 'orderDate', label: '订单日期', type: 'date' },
    { prop: 'expectDate', label: '期望到货', type: 'date' },
    { prop: 'status', label: '状态', type: 'input', width: 90 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ],
  itemFields: [
    {
      prop: 'productId',
      label: '产品',
      type: 'product-select',
      width: 240,
      autofill: { unitId: 'unitId', price: 'purchasePrice' }
    },
    { prop: 'qty', label: '数量', type: 'number', width: 140, precision: 2 },
    { prop: 'price', label: '含税单价', type: 'number', width: 140, precision: 2 },
    { prop: 'amount', label: '金额', type: 'number', readonly: true, width: 130 },
    { prop: 'remark', label: '备注', type: 'input' }
  ]
}
</script>
