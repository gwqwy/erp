<template>
  <OrderPage :config="config" />
</template>

<script setup lang="ts">
import OrderPage, { type OrderConfig } from '@/components/OrderPage.vue'
import { materialReqApi, workOrderApi } from '@/api/mrp'
import { productApi } from '@/api/bas'

const config: OrderConfig = {
  title: '领料单',
  permission: 'mrp:req',
  bizType: 'mrp_material_req',
  headKey: 'req',
  api: {
    page: materialReqApi.page,
    detail: materialReqApi.detail,
    create: materialReqApi.create,
    delete: materialReqApi.delete,
    audit: materialReqApi.audit,
    voidOrder: materialReqApi.voidOrder
  },
  masterFields: [
    { prop: 'no', label: '单号', type: 'input', width: 150 },
    { prop: 'workOrderId', label: '生产工单', type: 'source-order', required: true },
    { prop: 'warehouseId', label: '发料仓库', type: 'warehouse-select', required: true },
    { prop: 'status', label: '状态', type: 'input', width: 100 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ],
  itemFields: [
    {
      prop: 'productId',
      label: '材料',
      type: 'product-select',
      width: 240,
      autofill: { unitId: 'unitId' }
    },
    { prop: 'qty', label: '领料数量', type: 'number', width: 140, precision: 2 },
    { prop: 'remark', label: '备注', type: 'input' }
  ],
  source: {
    prop: 'workOrderId',
    label: '生产工单',
    pageApi: workOrderApi.page,
    itemListApi: workOrderApi.itemList,
    mapItem: (oi: any) => ({
      workOrderItemId: oi.id,
      productId: oi.productId,
      unitId: oi.unitId,
      qty: Number(oi.qty) - Number(oi.issuedQty || 0)
    })
  }
}
</script>
