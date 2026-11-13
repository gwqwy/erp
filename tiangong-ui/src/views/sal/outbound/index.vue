<template>
  <OrderPage :config="config" />
</template>

<script setup lang="ts">
import OrderPage, { type OrderConfig } from '@/components/OrderPage.vue'
import { saleOrderApi, saleOutboundApi } from '@/api/sal'

const config: OrderConfig = {
  title: '销售出库单',
  permission: 'sal:outbound',
  bizType: 'sal_outbound',
  headKey: 'outbound',
  reversible: true,
  api: {
    page: saleOutboundApi.page,
    detail: saleOutboundApi.detail,
    create: saleOutboundApi.create,
    delete: saleOutboundApi.delete,
    audit: saleOutboundApi.audit,
    voidOrder: saleOutboundApi.voidOrder,
    submitApproval: saleOutboundApi.submitApproval,
    reverse: saleOutboundApi.reverse
  },
  masterFields: [
    { prop: 'no', label: '单号', type: 'input', width: 150 },
    { prop: 'customerId', label: '客户', type: 'partner-select', partnerType: 'customer', required: true },
    { prop: 'warehouseId', label: '仓库', type: 'warehouse-select', required: true },
    { prop: 'orderId', label: '来源订单', type: 'source-order' },
    { prop: 'outboundDate', label: '出库日期', type: 'date' },
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
    { prop: 'qty', label: '出库数量', type: 'number', width: 140, precision: 2 },
    { prop: 'price', label: '销售单价', type: 'number', width: 140, precision: 2 },
    { prop: 'amount', label: '金额', type: 'number', readonly: true, width: 130 },
    { prop: 'remark', label: '备注', type: 'input' }
  ],
  source: {
    prop: 'orderId',
    label: '销售订单',
    pageApi: saleOrderApi.page,
    itemListApi: saleOrderApi.itemList,
    mapItem: (oi: any) => ({
      orderItemId: oi.id,
      productId: oi.productId,
      unitId: oi.unitId,
      qty: Number(oi.qty) - Number(oi.outboundQty || 0),
      price: oi.price
    })
  }
}
</script>
