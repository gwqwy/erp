<template>
  <OrderPage :config="config" />
</template>

<script setup lang="ts">
import OrderPage, { type OrderConfig } from '@/components/OrderPage.vue'
import { transferApi } from '@/api/stock'
import { warehouseApi } from '@/api/bas'

const warehouseOptions = async () =>
  (await warehouseApi.list({ status: 0 })).map((w: any) => ({ label: w.name, value: w.id }))

const config: OrderConfig = {
  title: '库存调拨',
  permission: 'stk:transfer',
  bizType: 'stock_transfer',
  headKey: 'transfer',
  api: {
    page: transferApi.page,
    detail: transferApi.detail,
    create: transferApi.create,
    delete: transferApi.delete,
    audit: transferApi.audit,
    voidOrder: transferApi.voidOrder
  },
  masterFields: [
    { prop: 'no', label: '单号', type: 'input', width: 150 },
    { prop: 'fromWarehouseId', label: '调出仓库', type: 'select', required: true, options: warehouseOptions },
    { prop: 'toWarehouseId', label: '调入仓库', type: 'select', required: true, options: warehouseOptions },
    { prop: 'transferDate', label: '调拨日期', type: 'date' },
    { prop: 'status', label: '状态', type: 'input', width: 90 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ],
  itemFields: [
    {
      prop: 'productId',
      label: '产品',
      type: 'product-select',
      width: 240,
      autofill: { unitId: 'unitId' }
    },
    { prop: 'qty', label: '调拨数量', type: 'number', width: 140, precision: 2 },
    { prop: 'remark', label: '备注', type: 'input' }
  ]
}
</script>
