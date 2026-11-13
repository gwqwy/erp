<template>
  <OrderPage :config="config">
    <template #actions="{ row }">
      <el-tag size="small" :type="row.otherType === 'other_in' ? 'success' : 'danger'">
        {{ row.otherType === 'other_in' ? '其他入库' : '其他出库' }}
      </el-tag>
    </template>
  </OrderPage>
</template>

<script setup lang="ts">
import OrderPage, { type OrderConfig } from '@/components/OrderPage.vue'
import { otherStockApi } from '@/api/stock'

const config: OrderConfig = {
  title: '其他出入库',
  permission: 'stk:other',
  bizType: 'stock_other',
  headKey: 'other',
  api: {
    page: otherStockApi.page,
    detail: otherStockApi.detail,
    create: otherStockApi.create,
    delete: otherStockApi.delete,
    audit: otherStockApi.audit,
    voidOrder: otherStockApi.voidOrder
  },
  masterFields: [
    { prop: 'no', label: '单号', type: 'input', width: 150 },
    { prop: 'otherType', label: '类型', type: 'input', width: 110 },
    { prop: 'warehouseId', label: '仓库', type: 'warehouse-select', required: true },
    { prop: 'otherDate', label: '日期', type: 'date' },
    { prop: 'status', label: '状态', type: 'input', width: 90 },
    { prop: 'remark', label: '备注（赠品/报废/领用…）', type: 'textarea', span: 24, inTable: false }
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
    { prop: 'price', label: '入库成本', type: 'number', width: 140, precision: 2 },
    { prop: 'remark', label: '备注', type: 'input' }
  ]
}
</script>
