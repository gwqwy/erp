<template>
  <OrderPage :config="config">
    <template #actions="{ row }">
      <el-button v-if="Number(row.status) === 0" v-perm="'mrp:bom:update'" link type="warning"
                 @click="handleEnable(row)">启用</el-button>
    </template>
  </OrderPage>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import OrderPage, { type OrderConfig } from '@/components/OrderPage.vue'
import { bomApi } from '@/api/mrp'
import { productApi } from '@/api/bas'

const productOptions = async () =>
  (await productApi.page({ pageNo: 1, pageSize: 200, status: 0 })).list.map((p: any) => ({
    label: `${p.name}${p.spec ? ' / ' + p.spec : ''}`,
    value: p.id
  }))

const config: OrderConfig = {
  title: 'BOM',
  permission: 'mrp:bom',
  bizType: 'mrp_bom',
  headKey: 'bom',
  api: {
    page: bomApi.page,
    detail: bomApi.detail,
    create: bomApi.create,
    update: bomApi.update,
    delete: bomApi.delete
    // 无审批流：不提供 audit/void
  },
  masterFields: [
    { prop: 'no', label: 'BOM 编号', type: 'input', width: 150 },
    { prop: 'productId', label: '成品', type: 'select', required: true, options: productOptions },
    { prop: 'version', label: '版本', type: 'input', width: 80 },
    { prop: 'qty', label: '产出基数', type: 'input', width: 100 },
    {
      prop: 'status',
      label: '状态',
      type: 'select',
      width: 100,
      inForm: false,
      options: [
        { label: '草稿', value: 0 },
        { label: '已启用', value: 1 }
      ]
    },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ],
  itemFields: [
    {
      prop: 'productId',
      label: '组件产品',
      type: 'product-select',
      width: 240,
      autofill: { unitId: 'unitId' }
    },
    { prop: 'qty', label: '单耗', type: 'number', width: 140, precision: 4 },
    { prop: 'lossRate', label: '损耗率 %', type: 'number', width: 130, precision: 2 },
    { prop: 'remark', label: '备注', type: 'input' }
  ]
}

async function handleEnable(row: any) {
  await bomApi.enable(row.id)
  ElMessage.success('已启用并设为默认 BOM')
  location.reload()
}
</script>
