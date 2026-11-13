<template>
  <OrderPage :config="config" />
</template>

<script setup lang="ts">
import OrderPage, { type OrderConfig } from '@/components/OrderPage.vue'
import { purchaseReturnApi } from '@/api/pur'

const config: OrderConfig = {
  title: '采购退货',
  permission: 'pur:return',
  bizType: 'pur_return',
  headKey: 'ret',
  api: {
    page: purchaseReturnApi.page,
    detail: purchaseReturnApi.detail,
    create: purchaseReturnApi.create,
    delete: purchaseReturnApi.delete,
    audit: purchaseReturnApi.audit,
    voidOrder: purchaseReturnApi.voidOrder
  },
  masterFields: [
    { prop: 'no', label: '单号', type: 'input', width: 150 },
    { prop: 'supplierId', label: '供应商', type: 'partner-select', partnerType: 'supplier', required: true },
    { prop: 'warehouseId', label: '退出仓库', type: 'warehouse-select', required: true },
    { prop: 'returnDate', label: '退货日期', type: 'date' },
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
    { prop: 'qty', label: '退货数量', type: 'number', width: 140, precision: 2 },
    { prop: 'price', label: '退货单价', type: 'number', width: 140, precision: 2 },
    { prop: 'remark', label: '备注', type: 'input' }
  ]
}
</script>
