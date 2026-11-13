<template>
  <OrderPage :config="config" />
</template>

<script setup lang="ts">
import OrderPage, { type OrderConfig } from '@/components/OrderPage.vue'
import { saleReturnApi } from '@/api/sal'

const config: OrderConfig = {
  title: '销售退货',
  permission: 'sal:return',
  bizType: 'sal_return',
  headKey: 'ret',
  api: {
    page: saleReturnApi.page,
    detail: saleReturnApi.detail,
    create: saleReturnApi.create,
    delete: saleReturnApi.delete,
    audit: saleReturnApi.audit,
    voidOrder: saleReturnApi.voidOrder
  },
  masterFields: [
    { prop: 'no', label: '单号', type: 'input', width: 150 },
    { prop: 'customerId', label: '客户', type: 'partner-select', partnerType: 'customer', required: true },
    { prop: 'warehouseId', label: '退回仓库', type: 'warehouse-select', required: true },
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
      autofill: { unitId: 'unitId', price: 'salePrice' }
    },
    { prop: 'qty', label: '退货数量', type: 'number', width: 140, precision: 2 },
    { prop: 'price', label: '退货单价', type: 'number', width: 140, precision: 2 },
    { prop: 'remark', label: '备注', type: 'input' }
  ]
}
</script>
