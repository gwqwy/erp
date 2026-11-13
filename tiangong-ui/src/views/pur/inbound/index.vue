<template>
  <OrderPage :config="config" />
</template>

<script setup lang="ts">
import OrderPage, { type OrderConfig } from '@/components/OrderPage.vue'
import { purchaseInboundApi, purchaseOrderApi } from '@/api/pur'

const config: OrderConfig = {
  title: '采购入库单',
  permission: 'pur:inbound',
  bizType: 'pur_inbound',
  headKey: 'inbound',
  reversible: true,
  api: {
    page: purchaseInboundApi.page,
    detail: purchaseInboundApi.detail,
    create: purchaseInboundApi.create,
    delete: purchaseInboundApi.delete,
    audit: purchaseInboundApi.audit,
    voidOrder: purchaseInboundApi.voidOrder,
    submitApproval: purchaseInboundApi.submitApproval,
    reverse: purchaseInboundApi.reverse
  },
  masterFields: [
    { prop: 'no', label: '单号', type: 'input', width: 150 },
    { prop: 'supplierId', label: '供应商', type: 'partner-select', partnerType: 'supplier', required: true },
    { prop: 'warehouseId', label: '仓库', type: 'warehouse-select', required: true },
    { prop: 'orderId', label: '来源订单', type: 'source-order' },
    { prop: 'inboundDate', label: '入库日期', type: 'date' },
    { prop: 'status', label: '状态', type: 'input', width: 90 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ],
  itemFields: [
    {
      prop: 'productId',
      label: '产品',
      type: 'product-select',
      width: 240,
      autofill: { unitId: 'unitId', price: 'price' }
    },
    { prop: 'qty', label: '入库数量', type: 'number', width: 140, precision: 2 },
    { prop: 'price', label: '入库成本', type: 'number', width: 140, precision: 2 },
    { prop: 'amount', label: '金额', type: 'number', readonly: true, width: 130 },
    { prop: 'remark', label: '备注', type: 'input' }
  ],
  source: {
    prop: 'orderId',
    label: '采购订单',
    pageApi: purchaseOrderApi.page,
    itemListApi: purchaseOrderApi.itemList,
    mapItem: (oi: any) => ({
      orderItemId: oi.id,
      productId: oi.productId,
      unitId: oi.unitId,
      qty: Number(oi.qty) - Number(oi.inboundQty || 0),
      price: oi.price
    })
  }
}
</script>
